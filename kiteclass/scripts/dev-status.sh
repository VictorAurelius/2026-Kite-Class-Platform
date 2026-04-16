#!/bin/bash
#
# Check status of all development services
# Kiểm tra trạng thái các services
#
# Usage: ./scripts/dev-status.sh
#

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}📊 KiteClass Services Status${NC}"
echo -e "${BLUE}================================${NC}\n"

check_service() {
    local url=$1
    local name=$2

    if curl -s "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $name${NC} - Running"
    else
        echo -e "${RED}❌ $name${NC} - Not running"
    fi
}

check_docker() {
    local container=$1
    local name=$2

    if docker ps | grep -q "$container"; then
        echo -e "${GREEN}✅ $name${NC} - Running"
    else
        echo -e "${RED}❌ $name${NC} - Not running"
    fi
}

echo -e "${BLUE}🌐 Web Services:${NC}"
check_service "http://localhost:4700" "Frontend (4700)    "
check_service "http://localhost:8080/actuator/health" "Gateway (8080)    "
check_service "http://localhost:8081/actuator/health" "Core (8081)       "

echo -e "\n${BLUE}🐳 Docker Services:${NC}"
check_docker "kiteclass-postgres" "PostgreSQL (5432)  "
check_docker "kiteclass-redis" "Redis (6379)       "

echo -e "\n${BLUE}📝 Logs Location:${NC}"
LOGS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.log"
if [ -d "$LOGS_DIR" ]; then
    echo -e "  ${YELLOW}$LOGS_DIR${NC}"
    ls -lh "$LOGS_DIR"/*.log 2>/dev/null || echo -e "  ${YELLOW}No log files${NC}"
else
    echo -e "  ${YELLOW}No logs directory${NC}"
fi

echo -e "\n${BLUE}💡 Commands:${NC}"
echo -e "  Start:  ${GREEN}./scripts/dev-start.sh${NC}"
echo -e "  Stop:   ${RED}./scripts/dev-stop.sh${NC}"
echo -e "  Logs:   ${YELLOW}tail -f .log/frontend.log${NC}"
echo ""
