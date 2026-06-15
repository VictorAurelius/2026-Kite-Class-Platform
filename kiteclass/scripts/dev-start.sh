#!/bin/bash
#
# Development Environment Startup Script
# Khởi động toàn bộ môi trường dev: PostgreSQL, Redis, Backend (Gateway + Core), Frontend
#
# Usage: ./scripts/dev-start.sh
# WSL Compatible: Yes
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/.log"
PIDS_FILE="$LOGS_DIR/dev-pids.txt"

# Tạo thư mục logs
mkdir -p "$LOGS_DIR"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}🚀 KiteClass Development Setup${NC}"
echo -e "${BLUE}================================${NC}\n"

# Function để check command tồn tại
check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo -e "${RED}❌ $1 không được cài đặt!${NC}"
        echo -e "${YELLOW}Cài đặt: $2${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ $1 đã cài đặt${NC}"
}

# Function để wait cho HTTP service sẵn sàng
wait_for_http() {
    local url=$1
    local name=$2
    local max_attempts=60
    local attempt=0

    echo -e "${YELLOW}⏳ Đợi $name khởi động...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $name đã sẵn sàng!${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
        echo -n "."
    done

    echo -e "\n${RED}❌ $name không khởi động được sau 2 phút${NC}"
    return 1
}

# Function để wait cho TCP port
wait_for_port() {
    local port=$1
    local name=$2
    local max_attempts=30
    local attempt=0

    echo -e "${YELLOW}⏳ Đợi $name khởi động...${NC}"

    while [ $attempt -lt $max_attempts ]; do
        if nc -z localhost "$port" 2>/dev/null || timeout 1 bash -c "cat < /dev/null > /dev/tcp/localhost/$port" 2>/dev/null; then
            echo -e "${GREEN}✅ $name đã sẵn sàng!${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
        echo -n "."
    done

    echo -e "\n${RED}❌ $name không khởi động được sau 1 phút${NC}"
    return 1
}

# Function để cleanup khi thoát
cleanup() {
    echo -e "\n${YELLOW}🛑 Đang dừng tất cả services...${NC}"

    if [ -f "$PIDS_FILE" ]; then
        while IFS= read -r pid; do
            if ps -p "$pid" > /dev/null 2>&1; then
                kill "$pid" 2>/dev/null || true
                echo -e "${GREEN}✅ Đã dừng process $pid${NC}"
            fi
        done < "$PIDS_FILE"
        rm "$PIDS_FILE"
    fi

    # Stop Docker containers
    echo -e "${YELLOW}🐳 Đang dừng Docker containers...${NC}"
    docker stop kiteclass-postgres kiteclass-redis 2>/dev/null || true

    echo -e "${GREEN}✅ Đã dừng tất cả services${NC}"
}

trap cleanup EXIT INT TERM

# 1. Kiểm tra prerequisites
echo -e "\n${BLUE}📋 Kiểm tra prerequisites...${NC}"
check_command "docker" "sudo apt install docker.io"
check_command "java" "sudo apt install openjdk-21-jdk"
check_command "node" "curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash - && sudo apt install -y nodejs"
check_command "pnpm" "npm install -g pnpm"

# 2. Start PostgreSQL
echo -e "\n${BLUE}🐘 Khởi động PostgreSQL...${NC}"
if docker ps -a | grep -q kiteclass-postgres; then
    docker start kiteclass-postgres
else
    docker run -d \
        --name kiteclass-postgres \
        -e POSTGRES_DB=kiteclass_dev \
        -e POSTGRES_USER=kiteclass \
        -e POSTGRES_PASSWORD=kiteclass123 \
        -p 5432:5432 \
        postgres:16-alpine
fi
wait_for_port 5432 "PostgreSQL"

# 3. Start Redis
echo -e "\n${BLUE}🔴 Khởi động Redis...${NC}"
if docker ps -a | grep -q kiteclass-redis; then
    docker start kiteclass-redis
else
    docker run -d \
        --name kiteclass-redis \
        -p 6379:6379 \
        redis:7-alpine
fi
wait_for_port 6379 "Redis"

# Defaults for required env vars so the stack boots out-of-the-box on a fresh
# clone. Production sets these via real secrets management; here they're
# loud-default values that are safe because dev never accepts external traffic.
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export INTERNAL_API_SECRET="${INTERNAL_API_SECRET:-dev-internal-secret-not-for-prod-32chars}"
SPRING_PROFILE_FOR_RUN="$SPRING_PROFILES_ACTIVE"

# 4. Start Core Service
# Activate the dev profile so application-dev.yml (BrandingDataSeeder + dev-only
# logging) takes effect — see GAP-235 Sub-PR F notes. Schema runs through Flyway
# + ddl-auto: validate same as prod (GAP-244 V46 closed the audit-column drift).
echo -e "\n${BLUE}⚙️  Khởi động Core Service (port 8081, profile=$SPRING_PROFILE_FOR_RUN)...${NC}"
cd "$PROJECT_ROOT/kiteclass-core"
./mvnw spring-boot:run -Dspring-boot.run.profiles="$SPRING_PROFILE_FOR_RUN" \
    > "$LOGS_DIR/core.log" 2>&1 &
CORE_PID=$!
echo "$CORE_PID" >> "$PIDS_FILE"
echo -e "${GREEN}Core PID: $CORE_PID${NC}"
wait_for_http "http://localhost:8081/actuator/health" "Core Service"

# 5. Setup frontend environment
echo -e "\n${BLUE}⚛️  Setup Frontend...${NC}"
cd "$PROJECT_ROOT/kiteclass-frontend"

if [ ! -f ".env.local" ]; then
    echo -e "${YELLOW}📝 Tạo .env.local...${NC}"
    cat > .env.local << 'EOF'
# API Backend URL
NEXT_PUBLIC_API_URL=http://localhost:8080

# Feature flags
NEXT_PUBLIC_ENABLE_ANALYTICS=false
EOF
    echo -e "${GREEN}✅ Đã tạo .env.local${NC}"
fi

# Install dependencies nếu chưa có
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}📦 Cài đặt dependencies...${NC}"
    pnpm install
fi

# 7. Start Frontend
echo -e "\n${BLUE}🎨 Khởi động Frontend (port 4700)...${NC}"
pnpm dev > "$LOGS_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" >> "$PIDS_FILE"
echo -e "${GREEN}Frontend PID: $FRONTEND_PID${NC}"
wait_for_http "http://localhost:4700" "Frontend"

# 8. Tạo dữ liệu mẫu (optional)
echo -e "\n${BLUE}📊 Tạo dữ liệu mẫu...${NC}"
if [ -f "$PROJECT_ROOT/scripts/seed-data.sh" ]; then
    bash "$PROJECT_ROOT/scripts/seed-data.sh" || echo -e "${YELLOW}⚠️  Không thể tạo dữ liệu mẫu (có thể cần login trước)${NC}"
else
    echo -e "${YELLOW}⚠️  Script seed-data.sh không tồn tại${NC}"
fi

# 9. Summary
echo -e "\n${GREEN}================================${NC}"
echo -e "${GREEN}✅ Tất cả services đã khởi động!${NC}"
echo -e "${GREEN}================================${NC}\n"

echo -e "${BLUE}📍 URLs:${NC}"
echo -e "  Frontend:  ${GREEN}http://localhost:4700${NC}"
echo -e "  Gateway:   ${GREEN}http://localhost:8080${NC}"
echo -e "  Core:      ${GREEN}http://localhost:8081${NC}"
echo -e "  PostgreSQL: ${GREEN}localhost:5432${NC} (user: kiteclass, pass: kiteclass123)"
echo -e "  Redis:     ${GREEN}localhost:6379${NC}\n"

echo -e "${BLUE}📝 Logs:${NC}"
echo -e "  Frontend: ${YELLOW}tail -f $LOGS_DIR/frontend.log${NC}"
echo -e "  Gateway:  ${YELLOW}tail -f $LOGS_DIR/gateway.log${NC}"
echo -e "  Core:     ${YELLOW}tail -f $LOGS_DIR/core.log${NC}\n"

echo -e "${BLUE}🛑 Để dừng:${NC}"
echo -e "  Nhấn ${RED}Ctrl+C${NC} hoặc chạy: ${YELLOW}./scripts/dev-stop.sh${NC}\n"

echo -e "${YELLOW}⏳ Services đang chạy... (Ctrl+C để dừng)${NC}\n"

# Keep script running
wait
