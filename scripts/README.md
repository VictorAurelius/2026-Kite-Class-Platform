# KiteClass Development Scripts

Scripts tự động hóa cho môi trường development. Tương thích với WSL.

## 🚀 Quick Start

```bash
# Khởi động tất cả services
./scripts/dev-start.sh

# Kiểm tra trạng thái
./scripts/dev-status.sh

# Dừng tất cả services
./scripts/dev-stop.sh
```

## 📋 Available Scripts

### 1. `dev-start.sh` - Khởi động môi trường dev

Tự động khởi động:
- ✅ PostgreSQL (Docker)
- ✅ Redis (Docker)
- ✅ Core Service (port 8081)
- ✅ Gateway Service (port 8080)
- ✅ Frontend (port 3000)

**Usage:**
```bash
./scripts/dev-start.sh
```

**Output:**
- Frontend: http://localhost:3000
- Gateway: http://localhost:8080
- Core: http://localhost:8081
- Logs: `.log/` directory

**Dừng services:**
- Nhấn `Ctrl+C` hoặc chạy `./scripts/dev-stop.sh`

### 2. `dev-stop.sh` - Dừng tất cả services

Dừng toàn bộ:
- Backend processes (Gateway + Core)
- Frontend process
- Docker containers (PostgreSQL + Redis)

**Usage:**
```bash
./scripts/dev-stop.sh
```

### 3. `dev-status.sh` - Kiểm tra trạng thái

Hiển thị trạng thái của:
- Frontend (port 3000)
- Gateway (port 8080)
- Core (port 8081)
- PostgreSQL container
- Redis container
- Log files

**Usage:**
```bash
./scripts/dev-status.sh
```

### 4. `seed-data.sh` - Tạo dữ liệu mẫu

Tạo:
- 5 học viên mẫu
- 3 giáo viên mẫu

**Usage:**
```bash
# Bước 1: Login vào http://localhost:3000/login
# Bước 2: Lấy accessToken và tenantId từ localStorage (F12)
# Bước 3: Chạy script
./scripts/seed-data.sh YOUR_ACCESS_TOKEN YOUR_TENANT_ID
```

**Hoặc chạy không tham số để xem hướng dẫn:**
```bash
./scripts/seed-data.sh
```

## 📝 Logs

Tất cả logs được lưu tại `.log/`:

```bash
# Xem logs Frontend
tail -f .log/frontend.log

# Xem logs Gateway
tail -f .log/gateway.log

# Xem logs Core
tail -f .log/core.log

# Xem tất cả logs
tail -f .log/*.log
```

## 🔧 Prerequisites

Scripts sẽ tự động kiểm tra các tool cần thiết:

- ✅ **Docker** - Để chạy PostgreSQL và Redis
- ✅ **Java 21+** - Để chạy Spring Boot
- ✅ **Node.js 20+** - Để chạy Next.js
- ✅ **pnpm** - Package manager cho frontend

### Cài đặt trên WSL/Ubuntu

```bash
# Docker
sudo apt update
sudo apt install docker.io
sudo usermod -aG docker $USER
# Logout và login lại

# Java 21
sudo apt install openjdk-21-jdk

# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# pnpm
npm install -g pnpm
```

## 🐛 Troubleshooting

### Lỗi: Port already in use

```bash
# Kiểm tra port đang được sử dụng
lsof -i :3000  # Frontend
lsof -i :8080  # Gateway
lsof -i :8081  # Core

# Kill process
kill -9 <PID>

# Hoặc dùng script stop
./scripts/dev-stop.sh
```

### Lỗi: Docker containers không khởi động

```bash
# Kiểm tra Docker service
sudo service docker status
sudo service docker start

# Xóa containers cũ và khởi động lại
docker rm -f kiteclass-postgres kiteclass-redis
./scripts/dev-start.sh
```

### Lỗi: Maven build fails

```bash
# Clean và rebuild
cd kiteclass/kiteclass-gateway
./mvnw clean install

cd ../kiteclass-core
./mvnw clean install
```

### Lỗi: Frontend dependencies

```bash
cd kiteclass/kiteclass-frontend
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

## 🎯 Workflow Thông Thường

### Development Session

```bash
# 1. Khởi động môi trường
./scripts/dev-start.sh

# 2. Chờ tất cả services sẵn sàng (khoảng 2-3 phút)

# 3. Truy cập http://localhost:3000

# 4. Sau khi login, tạo dữ liệu mẫu (optional)
./scripts/seed-data.sh YOUR_TOKEN YOUR_TENANT_ID

# 5. Làm việc...

# 6. Khi xong, dừng services
# Nhấn Ctrl+C hoặc:
./scripts/dev-stop.sh
```

### Kiểm tra nhanh

```bash
# Kiểm tra services đang chạy
./scripts/dev-status.sh

# Xem logs real-time
tail -f .log/frontend.log
```

## 📊 Database Access

### PostgreSQL

```bash
# Connection info
Host: localhost
Port: 5432
Database: kiteclass
User: kiteclass
Password: kiteclass123

# Connect với psql
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass

# Hoặc dùng GUI tools (DBeaver, pgAdmin)
```

### Redis

```bash
# Connect với redis-cli
docker exec -it kiteclass-redis redis-cli

# Test connection
PING  # Should return PONG

# List all keys
KEYS *
```

## 🔐 Security Note

- Database credentials chỉ dùng cho development
- Không commit `.env.local` vào git
- Scripts này KHÔNG dùng cho production

## 📚 More Info

- Frontend README: `kiteclass/kiteclass-frontend/README.md`
- Backend Gateway: `kiteclass/kiteclass-gateway/README.md`
- Backend Core: `kiteclass/kiteclass-core/README.md`
- Implementation Plan: `documents/03-planning/implementation/kiteclass-implementation-plan.md`
