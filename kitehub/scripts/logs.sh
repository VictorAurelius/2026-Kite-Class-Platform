#!/bin/bash
# View KiteHub logs
# Usage: ./scripts/logs.sh [service] [-f] [--tail N]
#
# Examples:
#   ./scripts/logs.sh                    # All logs (last 100 lines)
#   ./scripts/logs.sh -f                 # Follow all logs
#   ./scripts/logs.sh kitehub-gateway    # Gateway logs only
#   ./scripts/logs.sh kitehub-gateway -f # Follow gateway logs
#   ./scripts/logs.sh --tail 50          # Last 50 lines

set -e
cd "$(dirname "$0")/.."

# Default options
FOLLOW=""
TAIL="100"
SERVICE=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--follow)
            FOLLOW="-f"
            shift
            ;;
        --tail)
            TAIL="$2"
            shift 2
            ;;
        --tail=*)
            TAIL="${1#*=}"
            shift
            ;;
        kitehub-*)
            SERVICE="$1"
            shift
            ;;
        *)
            SERVICE="kitehub-$1"
            shift
            ;;
    esac
done

if [ -n "$SERVICE" ]; then
    echo "Logs for $SERVICE (last $TAIL lines):"
    docker-compose -f docker-compose.kitehub.yml logs $FOLLOW --tail=$TAIL "$SERVICE"
else
    echo "Logs for all services (last $TAIL lines):"
    docker-compose -f docker-compose.kitehub.yml logs $FOLLOW --tail=$TAIL
fi
