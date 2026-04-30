import asyncio
import json
import os
import signal
import threading
from typing import Dict, Set

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from pydantic import BaseModel
import uvicorn

from container_engine import create_container, stop_container

app = FastAPI()

# In-memory container store
containers: Dict[str, dict] = {}
# subscribers[container_id] = {"logs": set(), "exec": set()}
subscribers: Dict[str, Dict[str, Set[WebSocket]]] = {}
# To safely access from threads
import asyncio
main_loop = None

class CreateRequest(BaseModel):
    command: str
    run_as_user: bool = True

@app.on_event("startup")
def startup():
    global main_loop
    main_loop = asyncio.get_event_loop()
    if os.geteuid() != 0:
        raise SystemExit("Must run as root!")

def container_io_handler(cid: str, host_sock):
    """Thread to read JSON messages from container and broadcast to WebSocket subscribers."""
    try:
        buffer = b""
        while True:
            chunk = host_sock.recv(4096)
            if not chunk:
                break
            buffer += chunk
            while b"\n" in buffer:
                line, buffer = buffer.split(b"\n", 1)
                try:
                    msg = json.loads(line.decode())
                except:
                    continue
                # Broadcast to logs and exec subscribers
                for client_type in ("logs", "exec"):
                    if cid in subscribers and client_type in subscribers[cid]:
                        for ws in list(subscribers[cid][client_type]):
                            try:
                                asyncio.run_coroutine_threadsafe(
                                    ws.send_text(json.dumps(msg)),
                                    main_loop
                                )
                            except:
                                pass
                # If exit message, we can signal cleanup
                if msg.get("type") == "exit":
                    break
    except Exception:
        pass
    finally:
        # Cleanup after exit
        host_sock.close()
        # Remove container from tracking after delay
        def cleanup():
            if cid in containers:
                info = containers.pop(cid)
                stop_container(cid, info["base"], info["cgroup"], info["veth_host"],
                               info["host_sock"], info["pid"])
        # Schedule after broadcast
        asyncio.run_coroutine_threadsafe(_delayed_cleanup(cid), main_loop)

async def _delayed_cleanup(cid: str):
    await asyncio.sleep(1)
    if cid in containers:
        info = containers.pop(cid, None)
        if info:
            try:
                stop_container(cid, info["base"], info["cgroup"], info["veth_host"],
                               info["host_sock"], info["pid"])
            except:
                pass

@app.post("/containers")
async def create_container_endpoint(req: CreateRequest):
    try:
        cid, host_sock, pid, base, cgroup_path, veth_host = create_container(req.command, req.run_as_user)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    containers[cid] = {
        "pid": pid,
        "host_sock": host_sock,
        "base": base,
        "cgroup": cgroup_path,
        "veth_host": veth_host,
        "status": "running"
    }
    subscribers[cid] = {"logs": set(), "exec": set()}

    # Start IO thread
    t = threading.Thread(target=container_io_handler, args=(cid, host_sock), daemon=True)
    t.start()

    return {"container_id": cid}

@app.get("/containers/{cid}")
async def get_container(cid: str):
    if cid not in containers:
        raise HTTPException(status_code=404, detail="Container not found")
    info = containers[cid]
    # Determine status: check if process exists
    try:
        os.kill(info["pid"], 0)
        status = "running"
    except ProcessLookupError:
        status = "stopped"
    info["status"] = status
    return {
        "id": cid,
        "status": status,
        "pid": info["pid"]
        # could add IP, memory etc.
    }

@app.delete("/containers/{cid}")
async def delete_container(cid: str):
    if cid not in containers:
        raise HTTPException(status_code=404, detail="Container not found")
    info = containers.pop(cid)
    # Stop container
    stop_container(cid, info["base"], info["cgroup"], info["veth_host"],
                   info["host_sock"], info["pid"])
    # Notify subscribers
    if cid in subscribers:
        for ws_set in subscribers[cid].values():
            for ws in ws_set:
                try:
                    await ws.send_text(json.dumps({"type": "exit", "code": -9}))
                except:
                    pass
        del subscribers[cid]
    return {"message": f"Container {cid} terminated and cleaned up"}

@app.websocket("/containers/{cid}/logs")
async def ws_logs(websocket: WebSocket, cid: str):
    await websocket.accept()
    if cid not in containers:
        await websocket.close(code=1008, reason="Container not found")
        return
    # Register as log subscriber
    subscribers[cid]["logs"].add(websocket)
    try:
        while True:
            # We just keep connection open, data pushed by IO thread
            await websocket.receive_text()  # ignore any client messages
    except WebSocketDisconnect:
        pass
    finally:
        subscribers[cid]["logs"].discard(websocket)

@app.websocket("/containers/{cid}/exec")
async def ws_exec(websocket: WebSocket, cid: str):
    await websocket.accept()
    if cid not in containers:
        await websocket.close(code=1008, reason="Container not found")
        return
    # Register as exec subscriber
    subscribers[cid]["exec"].add(websocket)
    host_sock = containers[cid]["host_sock"]
    try:
        while True:
            data = await websocket.receive_text()
            # Send stdin to container via host_sock
            msg = json.dumps({"type": "stdin", "data": data}) + "\n"
            # Send in a thread-safe manner (we'll use asyncio's run_in_executor to avoid blocking)
            loop = asyncio.get_event_loop()
            await loop.run_in_executor(None, lambda: host_sock.sendall(msg.encode()))
    except WebSocketDisconnect:
        pass
    finally:
        subscribers[cid]["exec"].discard(websocket)