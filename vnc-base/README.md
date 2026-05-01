# BashLab - Headless VNC Desktop (Debian XFCE)

Dự án này cung cấp một môi trường Desktop Linux (Debian 12) chạy bên trong Docker, cho phép truy cập từ xa qua trình duyệt web. Đây là phiên bản đã được tối ưu hóa ("cắt gọt") đặc biệt dành cho các bài tập thực hành Bash (BashLab), loại bỏ các thành phần dư thừa từ OpenShift và Kubernetes để đảm bảo tính nhẹ nhàng và bảo mật.

## 1. Phân tích các thành phần chính

Hệ thống được xây dựng dựa trên sự phối hợp của các thành phần sau:

### A. Hệ điều hành & Giao diện (OS & UI)
- **Debian 12 (Bookworm):** Nền tảng hệ điều hành ổn định, bảo mật và phổ biến.
- **XFCE4 Desktop:** Một môi trường đồ họa cực kỳ nhẹ, tiêu tốn ít RAM, rất phù hợp để chạy trong container mà vẫn đảm bảo trải nghiệm người dùng mượt mà.
- **Xterm & Xfce4-terminal:** Các công cụ dòng lệnh chính cho bài tập Bash.

### B. Hiển thị từ xa (Remote Display Stack)
- **TigerVNC Server:** Tạo ra một "Virtual Display" (:1) bên trong container. Nó đóng vai trò là máy chủ truyền hình ảnh màn hình.
- **noVNC:** Một client VNC viết bằng HTML5. Nó cho phép bạn truy cập Desktop chỉ bằng trình duyệt web (Chrome, Firefox, Edge...) mà không cần cài đặt phần mềm VNC viewer.
- **Websockify:** Cầu nối trung gian để chuyển đổi dữ liệu VNC sang giao thức WebSocket mà trình duyệt có thể hiểu được.

### C. Công cụ bổ trợ
- **Firefox & Chromium:** Được cài đặt sẵn để học viên có thể xem tài liệu, tra cứu hướng dẫn trực tiếp trong môi trường Lab.
- **Vim, Wget, Net-tools:** Các tiện ích dòng lệnh cơ bản phục vụ học tập.

## 2. Cấu trúc dự án (Đã tối ưu hóa)

```text
.
├── Dockerfile.debian-xfce-vnc  # Tệp cấu hình build image chính
├── src
│   ├── common
│   │   ├── install/           # Các script cài đặt dùng chung (Firefox, noVNC)
│   │   ├── scripts/           # Script khởi động hệ thống (vnc_startup.sh)
│   │   └── xfce/              # Cấu hình giao diện và Window Manager
│   └── debian
│       └── install/           # Các script cài đặt riêng cho Debian (Chrome, TigerVNC, UI)
```

## 3. Điểm nhấn An toàn Bảo mật (ATBM)

- **Non-root User:** Container mặc định chạy dưới quyền user `1000` (không có quyền root). Điều này ngăn chặn việc học viên vô tình hoặc cố ý can thiệp vào kernel của máy chủ host.
- **Isolation:** Đã loại bỏ hoàn toàn các thành phần liên quan đến `nss-wrapper` và `generate_container_user` (vốn dùng cho OpenShift), giúp giảm thiểu diện tấn công (attack surface).
- **Password Protection:** Truy cập web được bảo vệ bằng mật khẩu (mặc định là `vncpassword`).
- **Resource Control:** Môi trường được thiết kế để dễ dàng giới hạn CPU/RAM thông qua tham số Docker khi chạy.

## 4. Hướng dẫn sử dụng

### Build Image
```bash
docker build -t bashlab -f Dockerfile.debian-xfce-vnc .
```

### Chạy Container
```bash
docker run -d -p 6901:6901 -p 5901:5901 --name my-bashlab bashlab
```

### Truy cập
- **Địa chỉ:** `http://localhost:6901`
- **Mật khẩu:** `vncpassword`

## 5. Các biến môi trường tùy chỉnh
Bạn có thể thay đổi cấu hình khi chạy bằng cách thêm tham số `-e`:
- `VNC_PW`: Thay đổi mật khẩu truy cập.
- `VNC_RESOLUTION`: Thay đổi độ phân giải màn hình (mặc định `1280x1024`).
- `VNC_COL_DEPTH`: Thay đổi độ sâu màu (mặc định `24`).


