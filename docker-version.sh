#!/bin/bash
# Hiển thị version của Docker images hiện tại

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

VERSION_FILE=".docker-build-logs/current-version.txt"
LOG_FILE=".docker-build-logs/build-history.log"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  KiteClass Docker Images - Current Version${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

if [ -f "$VERSION_FILE" ]; then
    cat "$VERSION_FILE"
else
    echo -e "${YELLOW}⚠️  No build version found.${NC}"
    echo -e "   Run ${GREEN}./docker-build.sh${NC} to build images first."
fi

echo ""
echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  Running Containers${NC}"
echo -e "${BLUE}================================================${NC}"
docker ps --filter "name=kiteclass" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || true

echo ""
echo -e "${YELLOW}💡 Quick commands:${NC}"
echo -e "  View build history: ${GREEN}cat $LOG_FILE${NC}"
echo -e "  View last 5 builds: ${GREEN}tail -n 100 $LOG_FILE${NC}"
echo -e "  Rebuild:            ${GREEN}./docker-build.sh${NC}"
echo ""
