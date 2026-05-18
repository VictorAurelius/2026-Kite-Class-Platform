#!/bin/bash
#
# Docker Compose Development Environment
# Khởi động tất cả services bằng Docker Compose (NHANH HƠN)
#
# Usage: ./scripts/dev-docker.sh [command]
# Commands: up, down, build, logs, restart
#

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Default to kiteclass compose file (can be overridden with environment variable)
COMPOSE_FILE="${COMPOSE_FILE:-$PROJECT_ROOT/kiteclass/docker-compose.dev.yml}"

cd "$PROJECT_ROOT"

# Function để show help
show_help() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}🐳 KiteClass Docker Development${NC}"
    echo -e "${BLUE}================================${NC}\n"

    echo -e "${GREEN}Usage:${NC}"
    echo -e "  ./scripts/dev-docker.sh [command]\n"

    echo -e "${GREEN}Commands:${NC}"
    echo -e "  ${YELLOW}up${NC}       - Start all services (default)"
    echo -e "  ${YELLOW}down${NC}     - Stop all services"
    echo -e "  ${YELLOW}build${NC}    - Build/rebuild Docker images"
    echo -e "  ${YELLOW}rebuild${NC}  - Rebuild and restart all services"
    echo -e "  ${YELLOW}logs${NC}     - Show logs (all services)"
    echo -e "  ${YELLOW}restart${NC}  - Restart all services"
    echo -e "  ${YELLOW}status${NC}   - Show running containers"
    echo -e "  ${YELLOW}clean${NC}    - Remove all containers and volumes"
    echo ""
}

# Parse command
COMMAND="${1:-up}"

case "$COMMAND" in
    up|start)
        echo -e "${BLUE}================================${NC}"
        echo -e "${BLUE}🚀 Starting KiteClass Services${NC}"
        echo -e "${BLUE}================================${NC}\n"

        echo -e "${YELLOW}📦 Building images (first time only)...${NC}"
        docker-compose -f "$COMPOSE_FILE" build

        echo -e "\n${YELLOW}🚀 Starting containers...${NC}"
        docker-compose -f "$COMPOSE_FILE" up -d

        echo -e "\n${GREEN}✅ Services started!${NC}\n"

        echo -e "${BLUE}📍 URLs:${NC}"
        echo -e "  Frontend:  ${GREEN}http://localhost:3000${NC}"
        echo -e "  Gateway:   ${GREEN}http://localhost:8080${NC}"
        echo -e "  Core:      ${GREEN}http://localhost:8081${NC}\n"

        echo -e "${BLUE}📊 Check status:${NC}"
        echo -e "  ${YELLOW}./scripts/dev-docker.sh status${NC}\n"

        echo -e "${BLUE}📝 View logs:${NC}"
        echo -e "  ${YELLOW}./scripts/dev-docker.sh logs${NC}"
        echo -e "  ${YELLOW}docker-compose -f docker-compose.dev.yml logs -f frontend${NC}\n"

        echo -e "${BLUE}🛑 Stop services:${NC}"
        echo -e "  ${YELLOW}./scripts/dev-docker.sh down${NC}\n"
        ;;

    down|stop)
        echo -e "${YELLOW}🛑 Stopping services...${NC}"
        docker-compose -f "$COMPOSE_FILE" down
        echo -e "${GREEN}✅ Services stopped${NC}"
        ;;

    build)
        echo -e "${YELLOW}🔨 Building Docker images...${NC}"
        docker-compose -f "$COMPOSE_FILE" build "$@"
        echo -e "${GREEN}✅ Build complete${NC}"
        ;;

    rebuild)
        echo -e "${YELLOW}🔨 Rebuilding and restarting...${NC}"
        docker-compose -f "$COMPOSE_FILE" down
        docker-compose -f "$COMPOSE_FILE" build
        docker-compose -f "$COMPOSE_FILE" up -d
        echo -e "${GREEN}✅ Rebuild complete${NC}"
        ;;

    logs)
        echo -e "${BLUE}📝 Showing logs (Ctrl+C to exit)...${NC}\n"
        docker-compose -f "$COMPOSE_FILE" logs -f
        ;;

    restart)
        echo -e "${YELLOW}🔄 Restarting services...${NC}"
        docker-compose -f "$COMPOSE_FILE" restart
        echo -e "${GREEN}✅ Services restarted${NC}"
        ;;

    status|ps)
        echo -e "${BLUE}📊 Running containers:${NC}\n"
        docker-compose -f "$COMPOSE_FILE" ps

        echo -e "\n${BLUE}🏥 Health status:${NC}"
        for container in kiteclass-postgres kiteclass-redis kiteclass-core kiteclass-frontend; do
            if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
                health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "no healthcheck")
                if [ "$health" = "healthy" ]; then
                    echo -e "  ${GREEN}✅ $container${NC} - healthy"
                elif [ "$health" = "no healthcheck" ]; then
                    echo -e "  ${BLUE}ℹ️  $container${NC} - running (no healthcheck)"
                else
                    echo -e "  ${YELLOW}⚠️  $container${NC} - $health"
                fi
            else
                echo -e "  ${RED}❌ $container${NC} - not running"
            fi
        done
        ;;

    clean)
        echo -e "${RED}⚠️  This will remove all containers and volumes!${NC}"
        read -p "Are you sure? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}🧹 Cleaning up...${NC}"
            docker-compose -f "$COMPOSE_FILE" down -v
            echo -e "${GREEN}✅ Cleanup complete${NC}"
        else
            echo -e "${BLUE}Cancelled${NC}"
        fi
        ;;

    help|--help|-h)
        show_help
        ;;

    *)
        echo -e "${RED}Unknown command: $COMMAND${NC}\n"
        show_help
        exit 1
        ;;
esac
