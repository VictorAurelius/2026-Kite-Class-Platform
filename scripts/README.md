# KiteClass Development Scripts

Scripts tự động hóa cho môi trường development. Tương thích với WSL.

## 🚀 Quick Start

### Option 1: Docker Compose (⚡ NHANH - Recommended)

```bash
# Khởi động tất cả services với Docker
./scripts/dev-docker.sh up

# Xem logs
./scripts/dev-docker.sh logs

# Dừng tất cả
./scripts/dev-docker.sh down
```

**Ưu điểm:**
- ✅ **Cực nhanh** - Không cần compile mỗi lần
- ✅ **Stable** - Docker images đã build sẵn
- ✅ **Hot reload** - Frontend tự động reload khi code thay đổi
- ✅ **Production-like** - Giống môi trường production

### Option 2: Native (Chậm hơn)

```bash
# Khởi động tất cả services native
./scripts/dev-start.sh

# Kiểm tra trạng thái
./scripts/dev-status.sh

# Dừng tất cả services
./scripts/dev-stop.sh
```

## 📋 Available Scripts

### 1. `dev-docker.sh` - Docker Compose (⚡ RECOMMENDED)

Khởi động tất cả services bằng Docker Compose - **NHANH NHẤT!**

**Usage:**
```bash
# Start all services
./scripts/dev-docker.sh up

# Stop all services
./scripts/dev-docker.sh down

# Rebuild images
./scripts/dev-docker.sh build

# View logs
./scripts/dev-docker.sh logs

# Check status
./scripts/dev-docker.sh status

# Restart services
./scripts/dev-docker.sh restart

# Clean everything
./scripts/dev-docker.sh clean
```

**Services included:**
- PostgreSQL (port 5432)
- Redis (port 6379)
- Core Service (port 8081) - **Docker image**
- Gateway Service (port 8080) - **Docker image**
- Frontend (port 3000) - **Docker với hot reload**

**Ưu điểm:**
- ⚡ **Nhanh** - Images đã build sẵn, không compile mỗi lần
- 🔄 **Hot reload** - Frontend tự động reload
- 🐳 **Isolated** - Mỗi service trong container riêng
- 🏥 **Health checks** - Tự động check service health
- 📊 **Easy monitoring** - `docker-compose ps` để xem status

### 2. `dev-start.sh` - Native startup (Chậm hơn)

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

# 4. Đăng ký/đăng nhập tài khoản

# 5. (Optional) Tạo dữ liệu mẫu - xem section "📦 Import Dữ Liệu Mẫu" bên dưới
#    Cần lấy accessToken và tenantId từ localStorage trước
./scripts/seed-data.sh YOUR_TOKEN YOUR_TENANT_ID

# 6. Làm việc...

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

## 📦 Import Dữ Liệu Mẫu

### Cách 1: Sử dụng Seed Script (Recommended)

Script `seed-data.sh` sẽ tự động tạo:
- ✅ **5 học viên** với thông tin đầy đủ (tên, email, số điện thoại, địa chỉ)
- ✅ **3 giáo viên** với chuyên môn khác nhau (Computer Science, Math, Chemistry)

**Bước thực hiện:**

```bash
# Bước 1: Truy cập ứng dụng
open http://localhost:3000

# Bước 2: Đăng ký tài khoản mới hoặc đăng nhập

# Bước 3: Mở Developer Tools (F12), vào tab Console, chạy lệnh:
localStorage.getItem('accessToken')    # Copy token
localStorage.getItem('tenantId')       # Copy tenant ID

# Bước 4: Chạy script với token và tenant ID vừa copy
./scripts/seed-data.sh "YOUR_ACCESS_TOKEN" "YOUR_TENANT_ID"

# Ví dụ:
# ./scripts/seed-data.sh "eyJhbGciOiJIUzUxMiJ9..." "550e8400-e29b-41d4-a716-446655440000"
```

**Output mẫu khi thành công:**
```
🌱 Tạo dữ liệu mẫu cho KiteClass...
✅ Student 1 created: Nguyễn Văn An
✅ Student 2 created: Trần Thị Bình
...
✅ Teacher 1 created: Dr. John Smith
...
🎉 Hoàn tất! Đã tạo 5 students và 3 teachers.
```

### Cách 2: Thêm thủ công qua UI

Truy cập các trang quản lý trong ứng dụng:
- Students: http://localhost:3000/students
- Teachers: http://localhost:3000/teachers

Click nút "Add New" và điền thông tin.

### Kiểm tra dữ liệu đã import

```bash
# Kết nối vào PostgreSQL
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass_dev

# Kiểm tra số lượng students
SELECT COUNT(*) FROM students WHERE deleted = false;

# Kiểm tra số lượng teachers
SELECT COUNT(*) FROM teachers WHERE deleted = false;

# Xem danh sách students
SELECT name, email FROM students WHERE deleted = false;

# Thoát
\q
```

## 📊 Database Access

### PostgreSQL

```bash
# Connection info
Host: localhost
Port: 5432
Database: kiteclass_dev
User: kiteclass
Password: kiteclass123

# Connect với psql
docker exec -it kiteclass-postgres psql -U kiteclass -d kiteclass_dev

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
