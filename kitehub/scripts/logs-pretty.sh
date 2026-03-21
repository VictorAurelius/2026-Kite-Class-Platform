#!/bin/bash
# Pretty-print docker compose logs with color highlighting and filtering
# Usage: ./scripts/logs-pretty.sh [service] [--errors] [--follow]

cd "$(dirname "$0")/.."

# Colors
RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

SERVICE=""
ERRORS_ONLY=false
FOLLOW=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --errors|-e)
            ERRORS_ONLY=true
            shift
            ;;
        --follow|-f)
            FOLLOW=true
            shift
            ;;
        -*)
            echo "Unknown option: $1"
            echo "Usage: $0 [service] [--errors] [--follow]"
            exit 1
            ;;
        *)
            SERVICE=$1
            shift
            ;;
    esac
done

# Build docker-compose logs command
CMD="docker-compose -f docker-compose.kitehub.yml logs"

if [ "$FOLLOW" = true ]; then
    CMD="$CMD -f"
fi

if [ -n "$SERVICE" ]; then
    CMD="$CMD $SERVICE"
fi

# Execute and colorize
echo "=============================================="
if [ -n "$SERVICE" ]; then
    echo "  Logs for: $SERVICE"
else
    echo "  All Service Logs"
fi
if [ "$ERRORS_ONLY" = true ]; then
    echo "  (Errors only)"
fi
echo "=============================================="
echo ""

if [ "$ERRORS_ONLY" = true ]; then
    # Show only errors and exceptions
    $CMD 2>&1 | grep -iE "error|exception|failed|fatal|warn" | while IFS= read -r line; do
        if echo "$line" | grep -iqE "error|exception|failed|fatal"; then
            echo -e "${RED}$line${NC}"
        elif echo "$line" | grep -iqE "warn"; then
            echo -e "${YELLOW}$line${NC}"
        else
            echo "$line"
        fi
    done
else
    # Show all logs with highlighting
    $CMD 2>&1 | while IFS= read -r line; do
        if echo "$line" | grep -iqE "error|exception|failed|fatal"; then
            echo -e "${RED}$line${NC}"
        elif echo "$line" | grep -iqE "warn"; then
            echo -e "${YELLOW}$line${NC}"
        elif echo "$line" | grep -iqE "success|started|ready"; then
            echo -e "${GREEN}$line${NC}"
        elif echo "$line" | grep -iqE "info"; then
            echo -e "${CYAN}$line${NC}"
        else
            echo "$line"
        fi
    done
fi
