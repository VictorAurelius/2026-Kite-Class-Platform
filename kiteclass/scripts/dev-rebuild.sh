#!/bin/bash
# Development rebuild script with proper cache clearing
# Usage: ./kiteclass/scripts/dev-rebuild.sh [service]
# Example: ./kiteclass/scripts/dev-rebuild.sh frontend
#          ./kiteclass/scripts/dev-rebuild.sh core
#          ./kiteclass/scripts/dev-rebuild.sh all

set -e

# Get script directory and navigate to kiteclass root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SERVICE=${1:-all}

echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo -e "${BLUE}   KiteClass Development Rebuild Script${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════${NC}"
echo ""

# Function to clear Redis cache
clear_redis_cache() {
    echo -e "${YELLOW}🧹 Clearing Redis cache...${NC}"
    docker exec kiteclass-redis redis-cli FLUSHALL > /dev/null 2>&1 || {
        echo -e "${YELLOW}⚠️  Redis not running or already clear${NC}"
    }
    echo -e "${GREEN}✅ Redis cache cleared${NC}"
}

# Function to rebuild and restart a service
rebuild_service() {
    local service=$1
    echo ""
    echo -e "${BLUE}🔨 Rebuilding $service...${NC}"
    docker-compose -f docker-compose.dev.yml build $service

    echo -e "${BLUE}🔄 Restarting $service...${NC}"
    docker-compose -f docker-compose.dev.yml up -d $service

    echo -e "${GREEN}✅ $service rebuilt and restarted${NC}"
}

# Function to wait for service health
wait_for_service() {
    local service=$1
    local max_wait=30
    local count=0

    echo -e "${YELLOW}⏳ Waiting for $service to be ready...${NC}"
    while [ $count -lt $max_wait ]; do
        if docker ps --filter "name=kiteclass-$service" --filter "health=healthy" | grep -q "kiteclass-$service" 2>/dev/null; then
            echo -e "${GREEN}✅ $service is ready${NC}"
            return 0
        fi
        if docker ps --filter "name=kiteclass-$service" --filter "status=running" | grep -q "kiteclass-$service" 2>/dev/null; then
            # Service running but no health check, consider it ready after 5 seconds
            if [ $count -gt 5 ]; then
                echo -e "${GREEN}✅ $service is running${NC}"
                return 0
            fi
        fi
        sleep 1
        count=$((count + 1))
    done
    echo -e "${YELLOW}⚠️  $service may still be starting...${NC}"
}

# Main rebuild logic
case $SERVICE in
    frontend)
        clear_redis_cache
        rebuild_service "frontend"
        wait_for_service "frontend"
        ;;

    core)
        clear_redis_cache
        rebuild_service "core"
        wait_for_service "core"
        ;;

    gateway)
        clear_redis_cache
        rebuild_service "gateway"
        wait_for_service "gateway"
        ;;

    all)
        clear_redis_cache
        echo -e "${BLUE}🔨 Rebuilding all services...${NC}"
        docker-compose -f docker-compose.dev.yml build

        echo -e "${BLUE}🔄 Restarting all services...${NC}"
        docker-compose -f docker-compose.dev.yml up -d

        echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"
        sleep 5
        wait_for_service "core"
        wait_for_service "gateway"
        wait_for_service "frontend"
        ;;

    *)
        echo -e "${RED}❌ Unknown service: $SERVICE${NC}"
        echo ""
        echo "Usage: $0 [service]"
        echo "Services: frontend, core, gateway, all"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
echo -e "${GREEN}   ✅ Rebuild complete!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}📊 Service Status:${NC}"
docker-compose -f docker-compose.dev.yml ps
