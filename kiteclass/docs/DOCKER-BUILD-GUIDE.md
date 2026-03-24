# Docker Build Guide - KiteClass Platform

Hướng dẫn build và quản lý Docker images với version tracking để sync giữa nhiều máy.

## 🎯 Tính năng

- ✅ **Auto version tagging**: Tự động tag images với commit hash và PR number
- ✅ **Build history logging**: Lưu lịch sử build vào `.docker-build-logs/`
- ✅ **Version tracking**: Luôn biết image hiện tại build từ PR/commit nào
- ✅ **Multi-machine sync**: Dễ dàng sync giữa nhiều máy phát triển
- ✅ **Docker labels**: Metadata embedded trong image

## 📋 Prerequisites

1. **Docker Desktop** đang chạy
2. **WSL 2 integration** enabled (Settings → Resources → WSL Integration)
3. Git repository đã được clone

## 🚀 Quick Start

### 1. Build Docker Images

```bash
# Build tất cả services với version tracking
./scripts/docker-build.sh
```

Script sẽ:
- Detect branch và PR number từ git
- Tag images với format: `pr-{PR}-{commit}` hoặc `{branch}-{commit}`
- Save build info vào `.docker-build-logs/current-version.txt`
- Append build history vào `.docker-build-logs/build-history.log`

**Output ví dụ:**
```
================================================
  KiteClass Platform - Docker Build Script
================================================

📋 Build Information:
  Branch:      feature/PR-3.8-frontend-testing
  PR:          3.8
  Commit:      43f9a72
  Message:     fix(frontend): TypeScript strict errors
  Build Tag:   pr-3.8-43f9a72
  Build Date:  2026-02-23 04:15:00

🔨 Building Docker images...
```

### 2. Check Current Version

```bash
# Xem version hiện tại
./scripts/docker-version.sh
```

**Output:**
```
================================================
  KiteClass Docker Images - Current Version
================================================
Build Date:    2026-02-23 04:15:00
Branch:        feature/PR-3.8-frontend-testing
PR Number:     3.8
Commit Hash:   43f9a72
Build Tag:     pr-3.8-43f9a72

Services Built:
- kiteclass-core:pr-3.8-43f9a72
- kiteclass-gateway:pr-3.8-43f9a72
- kiteclass-frontend:pr-3.8-43f9a72
```

### 3. Start Services

```bash
# Start all services
docker compose -f docker-compose.dev.yml up -d

# View logs
docker compose -f docker-compose.dev.yml logs -f

# Stop services
docker compose -f docker-compose.dev.yml down
```

## 📂 Version Tracking Files

### `.docker-build-logs/current-version.txt`
Chứa thông tin version hiện tại đang chạy trên máy này.

### `.docker-build-logs/build-history.log`
Lịch sử tất cả các lần build (append-only).

**Note:** Folder `.docker-build-logs/` đã được thêm vào `.gitignore` vì:
- Mỗi máy có build history riêng
- Không cần commit vào repo
- Chỉ dùng local để track

## 🔄 Workflow: Sync Giữa Nhiều Máy

### Máy A (Office):
```bash
# Checkout PR branch
git checkout feature/PR-3.9-student-pages

# Build images
./scripts/docker-build.sh
# → Build tag: pr-3.9-a1b2c3d

# Làm việc...
docker compose -f docker-compose.dev.yml up -d
```

### Máy B (Home):
```bash
# Pull latest code
git checkout feature/PR-3.9-student-pages
git pull origin feature/PR-3.9-student-pages

# Build images (cùng commit = cùng tag)
./scripts/docker-build.sh
# → Build tag: pr-3.9-a1b2c3d (giống Máy A)

# Check version để verify
./scripts/docker-version.sh
```

### Khi Cần Rebuild Exact Version:
```bash
# Xem commit hash từ version file
cat .docker-build-logs/current-version.txt
# Commit Hash: a1b2c3d

# Checkout exact commit
git checkout a1b2c3d

# Rebuild
./scripts/docker-build.sh
```

## 🏷️ Image Tagging Convention

| Branch Pattern | PR Detected | Tag Format | Example |
|---|---|---|---|
| `feature/PR-3.8-*` | 3.8 | `pr-{PR}-{commit}` | `pr-3.8-43f9a72` |
| `feature/PR-99-*` | 99 | `pr-{PR}-{commit}` | `pr-99-abc1234` |
| `main` | N/A | `{branch}-{commit}` | `main-ef12cf1` |
| `develop` | N/A | `{branch}-{commit}` | `develop-xyz5678` |

## 🔍 View Image Metadata

```bash
# Inspect image labels
docker inspect kiteclass-core:pr-3.8-43f9a72 --format '{{json .Config.Labels}}' | jq

# Output:
{
  "org.opencontainers.image.title": "KiteClass Core Service",
  "org.opencontainers.image.version": "pr-3.8-43f9a72",
  "com.kiteclass.commit-hash": "43f9a72",
  "com.kiteclass.build-date": "2026-02-23 04:15:00",
  "com.kiteclass.pr-number": "3.8",
  "com.kiteclass.branch": "feature/PR-3.8-frontend-testing"
}
```

## 🧪 Testing Workflow

### Test PR 3.8 (Frontend Testing):
```bash
# Checkout PR branch
git checkout feature/PR-3.8-frontend-testing

# Build images
./scripts/docker-build.sh
# Tag: pr-3.8-43f9a72

# Start services
docker compose -f docker-compose.dev.yml up -d

# Test frontend
# - Frontend: http://localhost:3000
# - Gateway API: http://localhost:8090
# - Core API: http://localhost:8081

# Run frontend tests
cd kiteclass/kiteclass-frontend
pnpm test
pnpm test:e2e
```

### Quay về Main:
```bash
# Stop current services
docker compose -f docker-compose.dev.yml down

# Checkout main
git checkout main

# Rebuild với main version
./scripts/docker-build.sh
# Tag: main-ef12cf1

# Start services
docker compose -f docker-compose.dev.yml up -d
```

## 🗑️ Clean Up Old Images

```bash
# List all kiteclass images
docker images | grep kiteclass

# Remove specific version
docker rmi kiteclass-core:pr-3.7-oldcommit

# Remove all unused images (careful!)
docker image prune -a
```

## 📊 View Build History

```bash
# View all builds
cat .docker-build-logs/build-history.log

# View last 5 builds
tail -n 100 .docker-build-logs/build-history.log

# Search builds by PR
grep "PR-3.8" .docker-build-logs/build-history.log
```

## 🐛 Troubleshooting

### Docker không tìm thấy
```
Error: The command 'docker' could not be found
```
**Fix**: Start Docker Desktop và enable WSL 2 integration

### Build lỗi "manifest unknown"
```bash
# Rebuild from scratch
docker compose -f docker-compose.dev.yml build --no-cache
```

### Version file bị mất
```bash
# Rebuild để recreate
./scripts/docker-build.sh
```

### Images chiếm nhiều disk
```bash
# Check disk usage
docker system df

# Clean up
docker system prune -a --volumes
```

## 📚 Related Files

- `scripts/docker-build.sh` - Build script với version tracking
- `scripts/docker-version.sh` - Show current version
- `docker-compose.dev.yml` - Development compose file
- `.docker-build-logs/` - Version tracking data (local only)
- `kiteclass/kiteclass-core/Dockerfile` - Core service
- `kiteclass/kiteclass-gateway/Dockerfile` - Gateway service
- `kiteclass/kiteclass-frontend/Dockerfile.dev` - Frontend dev

## 💡 Tips

1. **Luôn check version** trước khi start coding:
   ```bash
   ./scripts/docker-version.sh
   ```

2. **Rebuild sau khi pull code**:
   ```bash
   git pull && ./scripts/docker-build.sh
   ```

3. **Xem logs để debug**:
   ```bash
   docker compose -f docker-compose.dev.yml logs -f gateway
   ```

4. **Health check**:
   ```bash
   docker ps  # Check all containers healthy
   ```

---

**Last Updated**: 2026-02-23 (after PR 3.8 merge)
