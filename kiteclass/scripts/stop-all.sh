#!/bin/bash

# KiteClass - Stop All Services Script

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
echo -e "${BLUE}  KiteClass - Stop All Services${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Navigate to kiteclass directory
cd "$PROJECT_ROOT"

# Parse arguments
STOP_MODE="${1:-all}"

case "$STOP_MODE" in
    "down")
        echo -e "${YELLOW}Stopping and removing all containers, networks...${NC}"
        docker-compose -f docker-compose.dev.yml down
        ;;

    "down-v")
        echo -e "${RED}Stopping and removing all containers, networks, AND VOLUMES (DATA WILL BE LOST)...${NC}"
        read -p "Are you sure? (yes/no): " -r
        if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            docker-compose -f docker-compose.dev.yml down -v
            echo -e "${GREEN}✓${NC} All data removed"
        else
            echo "Cancelled."
            exit 0
        fi
        ;;

    "all"|*)
        echo -e "${YELLOW}Stopping all services...${NC}"
        docker-compose -f docker-compose.dev.yml stop
        ;;
esac

echo ""
echo -e "${GREEN}✓${NC} Services stopped successfully!"
echo ""
echo "To start again: ./scripts/start-all.sh"
