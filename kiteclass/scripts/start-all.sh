#!/bin/bash

# KiteClass - Start All Services Script
# Starts all services: PostgreSQL, Redis, RabbitMQ, MinIO, Backend, Gateway, Frontend

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
echo -e "${BLUE}  KiteClass - Start All Services${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print colored messages
info() {
    echo -e "${BLUE}ℹ ${NC} $1"
}

success() {
    echo -e "${GREEN}✓${NC} $1"
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

error() {
    echo -e "${RED}✗${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    error "Docker is not running. Please start Docker Desktop."
    exit 1
fi

success "Docker is running"

# Navigate to kiteclass directory
cd "$PROJECT_ROOT"

# Parse arguments
START_MODE="${1:-all}"

case "$START_MODE" in
    "infra"|"infrastructure")
        info "Starting infrastructure services only (PostgreSQL, Redis, RabbitMQ, MinIO)..."
        docker-compose -f docker-compose.dev.yml up -d postgres redis rabbitmq minio minio-init
        ;;

    "backend"|"core")
        info "Starting backend services (Core + Gateway)..."
        docker-compose -f docker-compose.dev.yml up -d postgres redis rabbitmq core gateway
        ;;

    "frontend")
        info "Starting frontend service..."
        docker-compose -f docker-compose.dev.yml up -d frontend
        ;;

    "all"|*)
        info "Starting ALL services..."
        docker-compose -f docker-compose.dev.yml up -d
        ;;
esac

echo ""
info "Waiting for services to be healthy..."
sleep 5

# Check service health
echo ""
echo -e "${BLUE}Service Status:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_service() {
    local service=$1
    local port=$2
    local name=$3

    if docker ps | grep -q "$service"; then
        local health=$(docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null || echo "no-health-check")
        if [ "$health" = "healthy" ] || [ "$health" = "no-health-check" ]; then
            success "$name is running on port $port"
        else
            warning "$name is starting (health: $health)..."
        fi
    else
        error "$name is not running"
    fi
}

check_service "kiteclass-postgres" "5432" "PostgreSQL"
check_service "kiteclass-redis" "6379" "Redis"
check_service "kiteclass-rabbitmq" "5672/15672" "RabbitMQ"
check_service "kiteclass-minio" "9000/9001" "MinIO"

if [ "$START_MODE" = "all" ] || [ "$START_MODE" = "backend" ] || [ "$START_MODE" = "core" ]; then
    check_service "kiteclass-core" "8081" "Backend (Core)"
fi

if [ "$START_MODE" = "all" ] || [ "$START_MODE" = "frontend" ]; then
    check_service "kiteclass-frontend" "3000" "Frontend"
    check_service "kiteclass-nginx" "80/443" "Nginx"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Show service URLs
echo ""
echo -e "${BLUE}Service URLs:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  PostgreSQL:        localhost:5432"
echo "  Redis:             localhost:6379"
echo "  RabbitMQ UI:       http://localhost:15672 (kiteclass/kiteclass123)"
echo "  MinIO Console:     http://localhost:9001 (minioadmin/minioadmin)"

if [ "$START_MODE" = "all" ] || [ "$START_MODE" = "backend" ] || [ "$START_MODE" = "core" ]; then
    echo "  Backend API:       http://localhost:8081"
    echo "  Backend Health:    http://localhost:8081/actuator/health"
    echo "  Swagger UI:        http://localhost:8081/swagger-ui.html"
    echo "  API Gateway:       http://localhost:8080"
fi

if [ "$START_MODE" = "all" ] || [ "$START_MODE" = "frontend" ]; then
    echo "  Frontend:          http://localhost:3000"
    echo "  Nginx:             http://localhost"
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Show logs command
echo ""
info "Useful commands:"
echo "  View logs:         docker-compose -f docker-compose.dev.yml logs -f [service]"
echo "  Stop all:          docker-compose -f docker-compose.dev.yml down"
echo "  Restart service:   docker-compose -f docker-compose.dev.yml restart [service]"
echo "  View status:       docker-compose -f docker-compose.dev.yml ps"

echo ""
success "All services started successfully!"
echo ""
echo -e "${YELLOW}Note: Services may take a few minutes to fully initialize.${NC}"
echo -e "${YELLOW}Check logs with: docker-compose -f docker-compose.dev.yml logs -f${NC}"
