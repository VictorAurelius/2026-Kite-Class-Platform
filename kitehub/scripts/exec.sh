#!/bin/bash
# Execute command in KiteHub container
# Usage: ./scripts/exec.sh <service> [command]
#
# Examples:
#   ./scripts/exec.sh postgres            # Open psql shell
#   ./scripts/exec.sh redis               # Open redis-cli
#   ./scripts/exec.sh gateway sh          # Open shell in gateway
#   ./scripts/exec.sh frontend sh         # Open shell in frontend

set -e
cd "$(dirname "$0")/.."

if [ $# -eq 0 ]; then
    echo "Usage: ./scripts/exec.sh <service> [command]"
    echo ""
    echo "Services: postgres, redis, gateway, subscription, branding, email, admin, frontend"
    echo ""
    echo "Examples:"
    echo "  ./scripts/exec.sh postgres        # Open psql shell"
    echo "  ./scripts/exec.sh redis           # Open redis-cli"
    echo "  ./scripts/exec.sh gateway sh      # Open shell"
    exit 1
fi

SERVICE=$1
shift

# Add kitehub- prefix if not present
if [[ "$SERVICE" != kitehub-* ]]; then
    SERVICE="kitehub-$SERVICE"
fi

# Default commands for common services
if [ $# -eq 0 ]; then
    case $SERVICE in
        kitehub-postgres)
            docker exec -it $SERVICE psql -U kitehub -d kitehub
            ;;
        kitehub-redis)
            docker exec -it $SERVICE redis-cli
            ;;
        kitehub-rabbitmq)
            docker exec -it $SERVICE rabbitmqctl status
            ;;
        *)
            docker exec -it $SERVICE sh
            ;;
    esac
else
    docker exec -it $SERVICE "$@"
fi
