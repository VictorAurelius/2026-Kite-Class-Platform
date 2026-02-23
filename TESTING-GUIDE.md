# Testing Guide - KiteClass Platform

Hướng dẫn chạy tests local với Testcontainers cleanup tự động.

---

## 🎯 Quick Start

### Chạy tất cả tests (Recommended)

```bash
# Chạy cả Core + Gateway tests với auto-cleanup
./scripts/test-local.sh all
```

### Chạy từng service riêng

```bash
# Chỉ test Core service
./scripts/test-local.sh core

# Chỉ test Gateway service
./scripts/test-local.sh gateway
```

### Manual cleanup (nếu cần)

```bash
# Cleanup Testcontainers containers
./scripts/cleanup-testcontainers.sh
```

---

## 🐳 Testcontainers là gì?

**Testcontainers** là library Java tạo **ephemeral containers** (containers tạm thời) để run integration tests.

### Ví dụ:

Khi chạy tests:
```bash
cd kiteclass/kiteclass-core
./mvnw clean test
```

Testcontainers sẽ:
1. ✅ Tự động start PostgreSQL container
2. ✅ Chạy tests với database này
3. ⚠️ **Đôi khi KHÔNG tự cleanup** container sau khi test xong

---

## 🚨 Vấn đề với Testcontainers

### Khi nào containers KHÔNG tự cleanup?

- 🛑 **Interrupt tests** (Ctrl+C giữa chừng)
- 💥 **JVM crash** trong lúc test
- 🐛 **Debug breakpoints** (JVM vẫn running, không shutdown)
- 🔧 **IDE run tests** (IDE đôi khi giữ containers)

### Hậu quả:

```bash
docker ps -a
```

Sẽ thấy nhiều **leftover containers**:
```
CONTAINER ID   NAMES               IMAGE                  STATUS
fa9c516c774e   crazy_jemison       postgres:15-alpine     Up 2 hours
06d1d6fa0ce9   infallible_agnesi   postgres:15-alpine     Exited (0) 9 days ago
e9820bfc6537   beautiful_bassi     postgres:15-alpine     Exited (0) 10 days ago
```

**Vấn đề:**
- 💾 Chiếm RAM/CPU (running containers)
- 💽 Chiếm disk space (stopped containers)
- 🔌 Port conflicts (random ports)
- 😵 Confusing (containers nào đang active?)

---

## ✅ Giải pháp: Automated Cleanup

### 1. Sử dụng `test-local.sh` (Recommended)

Script này **tự động cleanup** sau khi tests xong (dù pass hay fail):

```bash
./scripts/test-local.sh all
```

**Cách hoạt động:**
```bash
# Start tests
→ Core Service tests running...
→ Testcontainers creates postgres container
→ Tests complete (pass/fail)

# Auto cleanup (trap EXIT)
→ 🧹 Running post-test cleanup...
→ ✅ Stopped 1 containers
→ ✅ Removed 1 containers
→ 🎉 Cleanup complete!
```

### 2. Manual cleanup script

Nếu quên cleanup hoặc tests bị interrupt:

```bash
./scripts/cleanup-testcontainers.sh
```

**Output:**
```
🧹 Cleaning up Testcontainers...

Found Testcontainers:
  - Running: 1
  - Stopped: 2

Stopping running Testcontainers...
✅ Stopped 1 containers

Removing stopped Testcontainers...
✅ Removed 3 containers

🎉 Cleanup complete!
```

---

## 🔍 Cách nhận biết Testcontainers

### List tất cả Testcontainers:

```bash
docker ps -a --filter "label=org.testcontainers=true"
```

### Đặc điểm nhận biết:

| Đặc điểm | Testcontainers | KiteClass Dev |
|----------|----------------|---------------|
| **Container name** | Random (crazy_jemison, beautiful_bassi) | kiteclass-postgres |
| **Image version** | postgres:**15**-alpine | postgres:**16**-alpine |
| **Port** | Random (57472, 49153, ...) | Fixed (**5432**) |
| **Labels** | org.testcontainers=true | com.docker.compose.service=postgres |

### Example:

```bash
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}"
```

**Testcontainers** (SAI - cần cleanup):
```
crazy_jemison        postgres:15-alpine   0.0.0.0:57472->5432/tcp
```

**KiteClass Dev** (ĐÚNG - cần cho development):
```
kiteclass-postgres   postgres:16-alpine   0.0.0.0:5432->5432/tcp
```

---

## 📋 Best Practices

### ✅ DO

1. **Luôn dùng `test-local.sh`** để chạy tests
   ```bash
   ./scripts/test-local.sh all
   ```

2. **Cleanup trước khi push code**
   ```bash
   ./scripts/cleanup-testcontainers.sh
   git push origin main
   ```

3. **Check containers định kỳ**
   ```bash
   docker ps -a --filter "label=org.testcontainers=true"
   ```

4. **Cleanup ngay khi thấy leftover containers**
   ```bash
   ./scripts/cleanup-testcontainers.sh
   ```

### ❌ DON'T

1. **ĐỪNG ignore leftover containers** - cleanup ngay!

2. **ĐỪNG manually delete KiteClass dev containers**
   ```bash
   # ❌ WRONG - Xóa nhầm dev database
   docker rm -f kiteclass-postgres

   # ✅ CORRECT - Chỉ xóa Testcontainers
   ./scripts/cleanup-testcontainers.sh
   ```

3. **ĐỪNG Ctrl+C giữa chừng tests** nếu có thể tránh

4. **ĐỪNG để tests chạy lâu với debug breakpoints** - cleanup sau khi debug xong

---

## 🔄 Workflow: Test trước khi Push

```bash
# 1. Run tests với auto-cleanup
./scripts/test-local.sh all

# 2. Verify không còn leftover containers
docker ps -a --filter "label=org.testcontainers=true"
# Should show: no containers

# 3. Push code
git push origin main
```

---

## 🐛 Troubleshooting

### ❓ Tests fail với "port already in use"

**Nguyên nhân:** Testcontainers cũ vẫn chạy và đang dùng port

**Giải pháp:**
```bash
./scripts/cleanup-testcontainers.sh
./scripts/test-local.sh all
```

### ❓ Docker chiếm nhiều disk space

**Check disk usage:**
```bash
docker system df
```

**Cleanup all (CAREFUL - removes all unused containers/images):**
```bash
docker system prune -a --volumes
```

### ❓ Không thấy script `test-local.sh`

**Verify scripts exist:**
```bash
ls -la scripts/
```

**Nếu thiếu, pull latest code:**
```bash
git pull origin main
```

---

## 📚 Related Scripts

### Test Scripts

| Script | Mô tả | Usage |
|--------|-------|-------|
| `scripts/test-local.sh` | Run tests + auto-cleanup | `./scripts/test-local.sh [core\|gateway\|all]` |
| `scripts/cleanup-testcontainers.sh` | Manual cleanup only | `./scripts/cleanup-testcontainers.sh` |

### Docker Scripts

| Script | Mô tả | Usage |
|--------|-------|-------|
| `docker-build.sh` | Build Docker images with version tracking | `./docker-build.sh` |
| `docker-version.sh` | Show current Docker build version | `./docker-version.sh` |

### Development

```bash
# Start dev environment
docker compose -f docker-compose.dev.yml up -d

# Stop dev environment
docker compose -f docker-compose.dev.yml down

# View logs
docker compose -f docker-compose.dev.yml logs -f
```

---

## 💡 Tips

### Check system resources

```bash
# Memory usage
docker stats --no-stream

# Container count
docker ps -q | wc -l

# Disk usage
docker system df
```

### Cleanup all old stopped containers

```bash
# Remove ALL stopped containers (not just Testcontainers)
docker container prune

# Remove ALL unused images
docker image prune -a
```

### Scheduled cleanup (optional)

Add to cron to cleanup daily:
```bash
# Edit crontab
crontab -e

# Add line (cleanup every day at 2 AM)
0 2 * * * /path/to/project/scripts/cleanup-testcontainers.sh
```

---

**Last Updated**: 2026-02-23 (after migration centralization)
**Related**: DOCKER-BUILD-GUIDE.md, MEMORY.md
