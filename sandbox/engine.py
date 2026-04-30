import ctypes
import json
import os
import select
import signal
import socket
import threading
import uuid
import shutil
import pyroute2
from ctypes import CDLL, CFUNCTYPE, c_int, c_char_p, c_void_p, Structure, pointer, cast

libc = CDLL("libc.so.6", use_errno=True)
try:
    import seccomp
except ImportError:
    seccomp = None

# Constants
LOWER_DIR = "/opt/container-rootfs"
BASE_DIR = "/tmp/sandbox"
CGROUP_ROOT = "/sys/fs/cgroup/sandbox"
BRIDGE_NAME = "sandbox-br"
BRIDGE_IP = "10.0.0.1"
CONTAINER_IP_PREFIX = "10.0.0."
CONTAINER_NET_MASK = 24
UID_INSIDE = 1000
GID_INSIDE = 1000
MEMORY_LIMIT = 256 * 1024 * 1024
PIDS_MAX = 64

CLONE_NEWPID    = 0x20000000
CLONE_NEWUTS    = 0x04000000
CLONE_NEWNS     = 0x00020000
CLONE_NEWNET    = 0x40000000
CLONE_NEWUSER   = 0x10000000
CLONE_NEWCGROUP = 0x02000000
CLONE_NEWIPC    = 0x08000000
CLONE_FLAGS = (
    CLONE_NEWPID | CLONE_NEWUTS | CLONE_NEWNS |
    CLONE_NEWNET | CLONE_NEWUSER | CLONE_NEWCGROUP |
    CLONE_NEWIPC | signal.SIGCHLD
)

PR_SET_NO_NEW_PRIVS = 57

class ChildData(Structure):
    _fields_ = [
        ("command", c_char_p),
        ("merged_dir", c_char_p),
        ("lower_dir", c_char_p),
        ("upper_dir", c_char_p),
        ("work_dir", c_char_p),
        ("child_pipe_fd", c_int),   # sync pipe (deprecated)
        ("container_id", c_char_p),
        ("veth_cont_name", c_char_p),
        ("run_as_user", c_int),
        ("host_socket_fd", c_int),  # fd of host side of socketpair
    ]

def install_seccomp():
    if seccomp is None:
        return
    try:
        f = seccomp.SyscallFilter(defaction=seccomp.ALLOW)
        for name in ("reboot", "kexec_load", "kexec_file_load",
                     "init_module", "finit_module", "delete_module"):
            try:
                sc = seccomp.resolve_syscall(seccomp.Arch(), name)
                f.add_rule(seccomp.ERRNO(1), sc)
            except:
                pass
        f.load()
    except Exception:
        pass

def setup_user_mapping(pid, host_uid, host_gid):
    """Map 0 inside -> host_uid outside, and UID_INSIDE -> UID_INSIDE if possible."""
    with open(f"/proc/{pid}/setgroups", 'w') as f:
        f.write("deny")
    if host_uid == UID_INSIDE:
        with open(f"/proc/{pid}/uid_map", 'w') as f:
            f.write(f"0 {host_uid} 1000\n")
    else:
        with open(f"/proc/{pid}/uid_map", 'w') as f:
            f.write(f"0 {host_uid} 1\n")
            f.write(f"{UID_INSIDE} {UID_INSIDE} 1\n")
    if host_gid == GID_INSIDE:
        with open(f"/proc/{pid}/gid_map", 'w') as f:
            f.write(f"0 {host_gid} 1000\n")
    else:
        with open(f"/proc/{pid}/gid_map", 'w') as f:
            f.write(f"0 {host_gid} 1\n")
            f.write(f"{GID_INSIDE} {GID_INSIDE} 1\n")

def setup_network_inside(veth_name):
    ipr = pyroute2.IPRoute()
    lo_idx = ipr.link_lookup(ifname="lo")[0]
    ipr.link("set", index=lo_idx, state="up")
    idx = ipr.link_lookup(ifname=veth_name)[0]
    ipr.addr("add", index=idx, address=f"{CONTAINER_IP_PREFIX}2",
             mask=CONTAINER_NET_MASK)
    ipr.link("set", index=idx, state="up")
    ipr.route("add", dst="0.0.0.0/0", gateway=BRIDGE_IP)

def _setup_network_host(child_pid, cid, veth_cont):
    veth_host = f"veth{cid}_h"
    ipr = pyroute2.IPRoute()
    if not ipr.link_lookup(ifname=BRIDGE_NAME):
        ipr.link("add", ifname=BRIDGE_NAME, kind="bridge")
        br_idx = ipr.link_lookup(ifname=BRIDGE_NAME)[0]
        ipr.addr("add", index=br_idx, address=BRIDGE_IP, mask=CONTAINER_NET_MASK)
        ipr.link("set", index=br_idx, state="up")
        os.system("sysctl -w net.ipv4.ip_forward=1 >/dev/null")
        if os.system("iptables -t nat -C POSTROUTING -s 10.0.0.0/24 -j MASQUERADE 2>/dev/null") != 0:
            os.system("iptables -t nat -A POSTROUTING -s 10.0.0.0/24 -j MASQUERADE")
    else:
        br_idx = ipr.link_lookup(ifname=BRIDGE_NAME)[0]

    ipr.link("add", ifname=veth_host, peer=veth_cont, kind="veth")
    host_idx = ipr.link_lookup(ifname=veth_host)[0]
    cont_idx = ipr.link_lookup(ifname=veth_cont)[0]

    netns_path = f"/proc/{child_pid}/ns/net"
    netns_fd = os.open(netns_path, os.O_RDONLY)
    ipr.link("set", index=cont_idx, net_ns_fd=netns_fd)
    os.close(netns_fd)

    ipr.link("set", index=host_idx, master=br_idx)
    ipr.link("set", index=host_idx, state="up")
    return veth_host

def container_setup(data: ChildData):
    command = data.command.decode()
    merged = data.merged_dir.decode()
    lower = data.lower_dir.decode()
    upper = data.upper_dir.decode()
    work = data.work_dir.decode()
    pipe_fd = data.child_pipe_fd   # for sync (we'll still use for initial sync)
    cid = data.container_id.decode()
    veth_cont = data.veth_cont_name.decode()
    run_as_user = bool(data.run_as_user)
    host_sock_fd = data.host_socket_fd

    try:
        # Wait for parent (host) to set up user mapping and network
        os.read(pipe_fd, 1)
        os.close(pipe_fd)

        # Hostname
        os.sethostname(cid)

        # Mount OverlayFS
        os.makedirs(merged, exist_ok=True)
        opts = f"lowerdir={lower},upperdir={upper},workdir={work}".encode()
        if libc.mount(b"overlay", merged.encode(), b"overlay", 0, opts) != 0:
            raise OSError(ctypes.get_errno(), "Overlay mount failed")

        # Mount /sys/fs/cgroup for the container
        cg_host = os.path.join(CGROUP_ROOT, cid)
        cg_target = os.path.join(merged, "sys/fs/cgroup")
        os.makedirs(cg_target, exist_ok=True)
        if libc.mount(cg_host.encode(), cg_target.encode(), None, 2, None) != 0:
            os.makedirs(cg_target, exist_ok=True)
            libc.mount(b"none", cg_target.encode(), b"cgroup2", 0, b"")

        # Mount /proc, /run, /tmp
        for d in ["proc", "run", "tmp"]:
            target = os.path.join(merged, d)
            os.makedirs(target, exist_ok=True)
            fstype = b"proc" if d == "proc" else b"tmpfs"
            if libc.mount(fstype, target.encode(), fstype, 0, b"") != 0:
                pass  # non-fatal

        # chroot
        os.chroot(merged)
        os.chdir("/")

        # Setup network
        setup_network_inside(veth_cont)

        # Security
        libc.prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0)
        install_seccomp()
        if not run_as_user:
            # stay as root inside (mapped to host user)
            pass
        else:
            os.setgid(GID_INSIDE)
            os.setuid(UID_INSIDE)

        # Now create communication channel with host
        sock = socket.fromfd(host_sock_fd, socket.AF_UNIX, socket.SOCK_STREAM)
        sock.setblocking(True)

        # Create pty for interactive command
        import pty
        master_fd, slave_fd = pty.openpty()
        pid = os.fork()
        if pid == 0:
            # Child: run the command inside pty
            os.close(master_fd)
            os.setsid()
            os.dup2(slave_fd, 0)
            os.dup2(slave_fd, 1)
            os.dup2(slave_fd, 2)
            if slave_fd > 2:
                os.close(slave_fd)
            os.execvp("/bin/sh", ["/bin/sh", "-c", command])
        else:
            # Parent: relay between pty master and host socket
            os.close(slave_fd)
            # We'll use two threads or select to forward
            # Because we need to read from master and send to sock, and read from sock and write to master
            def forward():
                try:
                    while True:
                        r, _, _ = select.select([master_fd, sock], [], [])
                        if master_fd in r:
                            data = os.read(master_fd, 4096)
                            if not data:
                                break
                            msg = json.dumps({"type": "stdout", "data": data.decode(errors='replace')}) + "\n"
                            sock.sendall(msg.encode())
                        if sock in r:
                            req = sock.recv(4096)
                            if not req:
                                break
                            try:
                                obj = json.loads(req.decode())
                                if obj.get("type") == "stdin":
                                    os.write(master_fd, obj["data"].encode())
                            except:
                                pass
                except Exception:
                    pass
                finally:
                    # Send exit code
                    try:
                        _, status = os.waitpid(pid, 0)
                        code = os.WEXITSTATUS(status) if os.WIFEXITED(status) else -1
                        sock.sendall((json.dumps({"type": "exit", "code": code}) + "\n").encode())
                    except:
                        pass
                    sock.close()
                    os.close(master_fd)
                    os._exit(0)

            threading.Thread(target=forward, daemon=True).start()
            # Keep alive until forward ends (could join, but we want to keep the main process alive)
            # We'll just loop to keep PID 1 alive, signaling termination when forward thread stops.
            # Use a simple event.
            import time
            while True:
                time.sleep(1)

    except Exception as e:
        print(f"Child setup failed: {e}")
        os._exit(1)

def create_container(command: str, run_as_user: bool = True):
    cid = str(uuid.uuid4())[:8]
    base = os.path.join(BASE_DIR, cid)
    upper = os.path.join(base, "upper")
    work = os.path.join(base, "work")
    merged = os.path.join(base, "merged")
    for d in (upper, work, merged):
        os.makedirs(d, exist_ok=True)

    cgroup_path = os.path.join(CGROUP_ROOT, cid)
    os.makedirs(cgroup_path, exist_ok=True)
    with open(os.path.join(cgroup_path, "memory.max"), 'w') as f:
        f.write(str(MEMORY_LIMIT))
    with open(os.path.join(cgroup_path, "pids.max"), 'w') as f:
        f.write(str(PIDS_MAX))

    # Socketpair for host-container communication
    host_sock, child_sock = socket.socketpair(socket.AF_UNIX, socket.SOCK_STREAM)
    child_sock_fd = child_sock.fileno()

    # Sync pipe (still used for initial sync)
    parent_pipe, child_pipe = os.pipe()

    data = ChildData()
    data.command = c_char_p(command.encode())
    data.merged_dir = c_char_p(merged.encode())
    data.lower_dir = c_char_p(LOWER_DIR.encode())
    data.upper_dir = c_char_p(upper.encode())
    data.work_dir = c_char_p(work.encode())
    data.child_pipe_fd = child_pipe
    data.container_id = c_char_p(cid.encode())
    veth_cont = f"veth{cid}_c"
    data.veth_cont_name = c_char_p(veth_cont.encode())
    data.run_as_user = c_int(1 if run_as_user else 0)
    data.host_socket_fd = child_sock_fd

    child_func_type = CFUNCTYPE(c_int, c_void_p)
    def child_func_c(arg_ptr):
        d = cast(arg_ptr, pointer(ChildData)).contents
        container_setup(d)
        os._exit(0)
    child_cb = child_func_type(child_func_c)

    stack_size = 1024 * 1024
    stack = (ctypes.c_char * stack_size)()
    stack_top = c_void_p(ctypes.addressof(stack) + stack_size)

    child_pid = libc.clone(child_cb, stack_top, CLONE_FLAGS, pointer(data))
    if child_pid == -1:
        child_sock.close()
        host_sock.close()
        shutil.rmtree(base, ignore_errors=True)
        os.rmdir(cgroup_path)
        raise OSError(ctypes.get_errno(), "clone() failed")

    # Setup user mapping
    host_uid = os.getuid()
    host_gid = os.getgid()
    setup_user_mapping(child_pid, host_uid, host_gid)

    # Add to cgroup
    with open(os.path.join(cgroup_path, "cgroup.procs"), 'w') as f:
        f.write(str(child_pid))

    veth_host = _setup_network_host(child_pid, cid, veth_cont)

    # Signal child to continue
    os.write(parent_pipe, b'1')
    os.close(parent_pipe)
    os.close(child_pipe)

    # Close child end of socket in host
    child_sock.close()

    return cid, host_sock, child_pid, base, cgroup_path, veth_host

def stop_container(cid, base, cgroup_path, veth_host, host_sock, child_pid):
    """Kill container process and clean up."""
    try:
        os.kill(child_pid, signal.SIGKILL)
    except ProcessLookupError:
        pass
    # Cleanup network
    try:
        ipr = pyroute2.IPRoute()
        idx = ipr.link_lookup(ifname=veth_host)
        if idx:
            ipr.link("del", index=idx[0])
    except Exception:
        pass
    # Unmount
    merged = os.path.join(base, "merged")
    for mnt in [os.path.join(merged, "proc"), os.path.join(merged, "sys/fs/cgroup"),
                os.path.join(merged, "run"), os.path.join(merged, "tmp"), merged]:
        try:
            os.system(f"umount -l {mnt} 2>/dev/null")
        except:
            pass
    # Remove cgroup
    try:
        os.rmdir(cgroup_path)
    except Exception:
        pass
    # Remove temp dirs
    shutil.rmtree(base, ignore_errors=True)
    host_sock.close()