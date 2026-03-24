#!/bin/bash
# Check Kite Platform stack status with health checks and resource usage
# Usage: ./scripts/status.sh [--simple]

set -e
cd "$(dirname "$0")/.."

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=============================================="
echo "  Kite Platform Stack Status"
echo "=============================================="
echo ""

# Show container status
docker-compose -f docker-compose.kitehub.yml ps -a

echo ""
echo "=============================================="
echo "  Health Checks"
echo "=============================================="
echo ""

# Health check function with color
check_health() {
    local service=$1
    local port=$2
    local endpoint=${3:-/actuator/health}

    echo -n "  $service: "
    if curl -s -f "http://localhost:$port$endpoint" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ healthy${NC}"
        return 0
    else
        echo -e "${RED}❌ unhealthy${NC}"
        return 1
    fi
}

# Check all services
check_health "kite-gateway         " 9000
check_health "kitehub-subscription" 8081
check_health "kitehub-branding    " 8083
check_health "kitehub-email       " 8084
check_health "kitehub-admin       " 8085
check_health "kitehub-frontend    " 3001 "/"

echo ""

# Resource usage (only if not --simple)
if [ "$1" != "--simple" ] && [ "$1" != "-s" ]; then
    echo "=============================================="
    echo "  Resource Usage"
    echo "=============================================="
    echo ""

    # Get stats for kite platform containers only
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" $(docker-compose -f docker-compose.kitehub.yml ps -q 2>/dev/null) 2>/dev/null || echo "  No running containers"

    echo ""
    echo "=============================================="
    echo "  Recent Errors (last 5 min)"
    echo "=============================================="
    echo ""

    # Show recent errors from logs
    ERROR_COUNT=$(docker-compose -f docker-compose.kitehub.yml logs --since 5m 2>/dev/null | grep -iE "error|exception|failed" | wc -l)

    if [ "$ERROR_COUNT" -gt 0 ]; then
        echo -e "  ${YELLOW}⚠️  Found $ERROR_COUNT error(s) in logs${NC}"
        echo "  Run './scripts/logs.sh' to see details"
    else
        echo -e "  ${GREEN}✅ No errors found${NC}"
    fi

    echo ""
fi

echo "=============================================="
echo "  Service URLs"
echo "=============================================="
echo ""
echo "  Frontend:      http://localhost:3001"
echo "  Gateway API:   http://localhost:9000"
echo "  Subscription:  http://localhost:8081"
echo "  Branding:      http://localhost:8083"
echo "  Email:         http://localhost:8084"
echo "  Admin:         http://localhost:8085"
echo ""
echo "  PostgreSQL:    localhost:5433"
echo "  Redis:         localhost:6380"
echo "  RabbitMQ:      http://localhost:15673"
echo "  MinIO Console: http://localhost:9191"
echo "  MailHog UI:    http://localhost:8025"
echo ""
