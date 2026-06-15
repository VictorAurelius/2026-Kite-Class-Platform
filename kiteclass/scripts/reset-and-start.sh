#!/bin/bash

# KiteClass - Reset Database and Start Services Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  KiteClass - Reset Database & Start${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Navigate to kiteclass directory
cd "$PROJECT_ROOT"

# Step 1: Stop all application services (keep infrastructure running)
echo -e "${YELLOW}⊙${NC} Stopping application services..."
docker-compose -f docker-compose.dev.yml stop core gateway frontend nginx 2>/dev/null || true

# Step 2: Ensure PostgreSQL is running
echo -e "${YELLOW}⊙${NC} Ensuring PostgreSQL is running..."
docker-compose -f docker-compose.dev.yml up -d postgres
sleep 5

# Step 3: Drop and recreate database
echo -e "${YELLOW}⊙${NC} Resetting database..."
docker exec kiteclass-postgres psql -U kiteclass -d postgres -c "DROP DATABASE IF EXISTS kiteclass_dev;" 2>/dev/null || true
docker exec kiteclass-postgres psql -U kiteclass -d postgres -c "CREATE DATABASE kiteclass_dev;"
echo -e "${GREEN}✓${NC} Database recreated"

# Step 4: Start infrastructure services
echo -e "${YELLOW}⊙${NC} Starting infrastructure services..."
docker-compose -f docker-compose.dev.yml up -d postgres redis rabbitmq minio minio-init
sleep 10

# Step 5: Start Core service (runs Flyway migrations directly — dedicated gateway
# removed per ADR-032 / GAP-001; routing handled by shared kite-gateway per ADR-023)
echo -e "${YELLOW}⊙${NC} Starting Core service..."
docker-compose -f docker-compose.dev.yml up -d core
echo -e "${BLUE}ℹ ${NC} Waiting for Core to be healthy (30 seconds)..."
sleep 30

# Check Core health
if docker ps | grep -q "kiteclass-core.*healthy"; then
    echo -e "${GREEN}✓${NC} Core is healthy"
else
    echo -e "${YELLOW}⚠${NC} Core is still starting..."
fi

# Step 7: Start Frontend and Nginx
echo -e "${YELLOW}⊙${NC} Starting Frontend and Nginx..."
docker-compose -f docker-compose.dev.yml up -d frontend nginx

echo ""
echo -e "${GREEN}✓${NC} All services started successfully!"
echo ""

# Show service status
echo -e "${BLUE}Service URLs:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  PostgreSQL:        localhost:5432"
echo "  Redis:             localhost:6379"
echo "  RabbitMQ UI:       http://localhost:15672 (kiteclass/kiteclass123)"
echo "  MinIO Console:     http://localhost:9001 (minioadmin/minioadmin)"
echo "  Backend API:       http://localhost:8081"
echo "  Backend Health:    http://localhost:8081/actuator/health"
echo "  Swagger UI:        http://localhost:8081/swagger-ui.html"
echo "  API Gateway:       http://localhost:8080"
echo "  Frontend:          http://localhost:3000"
echo "  Nginx:             http://localhost:8090"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${BLUE}ℹ ${NC} View logs: ./scripts/view-logs.sh"
echo -e "${BLUE}ℹ ${NC} Stop all:  ./scripts/stop-all.sh"
echo ""
