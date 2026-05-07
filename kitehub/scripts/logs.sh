#!/bin/bash
# View Kite Platform logs
# Usage: ./scripts/logs.sh [service] [-f] [--tail N]
#
# Examples:
#   ./scripts/logs.sh                    # All logs (last 100 lines)
#   ./scripts/logs.sh -f                 # Follow all logs
#   ./scripts/logs.sh kite-gateway       # Gateway logs only
#   ./scripts/logs.sh kite-gateway -f    # Follow gateway logs
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
        kite-*|kitehub-*|kiteclass-*)
            SERVICE="$1"
            shift
            ;;
        *)
            # Map short names to container names
            case $1 in
                postgres)    SERVICE="kite-postgres" ;;
                redis)       SERVICE="kite-redis" ;;
                rabbitmq)    SERVICE="kite-rabbitmq" ;;
                minio)       SERVICE="kite-minio" ;;
                mailhog)     SERVICE="kite-mailhog" ;;
                gateway)     SERVICE="kite-gateway" ;;
                *)           SERVICE="kitehub-$1" ;;
            esac
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
