#!/bin/bash

# KiteClass - View Service Logs Script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  KiteClass - View Service Logs${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Navigate to kiteclass directory
cd "$PROJECT_ROOT"

# Parse arguments
SERVICE="${1:-all}"

if [ "$SERVICE" = "all" ]; then
    echo "Showing logs for ALL services (Ctrl+C to exit)..."
    echo ""
    docker-compose -f docker-compose.dev.yml logs -f
else
    echo "Showing logs for: $SERVICE (Ctrl+C to exit)..."
    echo ""
    docker-compose -f docker-compose.dev.yml logs -f "$SERVICE"
fi
