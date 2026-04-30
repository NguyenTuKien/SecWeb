FROM ubuntu:22.04

# Cài đặt các gói phụ thuộc hệ thống
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    iptables \
    bridge-utils \
    libapparmor-dev \
    libseccomp-dev \
    python3-seccomp \
    curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy mã nguồn và cài đặt các gói Python
COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY container_engine.py main.py ./

# Expose cổng REST API
EXPOSE 8000

# Khởi chạy ứng dụng
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]