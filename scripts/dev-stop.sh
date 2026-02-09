#!/bin/bash
#
# Stop all development services
# Dừng tất cả services đang chạy
#
# Usage: ./scripts/dev-stop.sh
#

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS_DIR="$PROJECT_ROOT/.log"
PIDS_FILE="$LOGS_DIR/dev-pids.txt"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}🛑 Dừng KiteClass Development${NC}"
echo -e "${BLUE}================================${NC}\n"

# Stop processes from PID file
if [ -f "$PIDS_FILE" ]; then
    echo -e "${YELLOW}📋 Đang dừng processes...${NC}"
    while IFS= read -r pid; do
        if ps -p "$pid" > /dev/null 2>&1; then
            kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || true
            echo -e "${GREEN}✅ Đã dừng process $pid${NC}"
        else
            echo -e "${YELLOW}⚠️  Process $pid đã dừng rồi${NC}"
        fi
    done < "$PIDS_FILE"
    rm "$PIDS_FILE"
else
    echo -e "${YELLOW}⚠️  Không tìm thấy file PIDs${NC}"
fi

# Stop Docker containers
echo -e "\n${YELLOW}🐳 Đang dừng Docker containers...${NC}"
docker stop kiteclass-postgres kiteclass-redis 2>/dev/null || true
echo -e "${GREEN}✅ Đã dừng PostgreSQL và Redis${NC}"

# Kill any remaining Java processes on ports 8080, 8081
echo -e "\n${YELLOW}🔍 Kiểm tra ports 8080, 8081, 3000...${NC}"
for port in 8080 8081 3000; do
    pid=$(lsof -ti:$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        kill -9 "$pid" 2>/dev/null || true
        echo -e "${GREEN}✅ Đã giải phóng port $port${NC}"
    fi
done

echo -e "\n${GREEN}================================${NC}"
echo -e "${GREEN}✅ Đã dừng tất cả services!${NC}"
echo -e "${GREEN}================================${NC}\n"

echo -e "${BLUE}💡 Để khởi động lại:${NC}"
echo -e "  ${YELLOW}./scripts/dev-start.sh${NC}\n"
