#!/bin/bash
# Execute command in Kite Platform container
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

# Map short names to container names
# Shared infra uses kite- prefix, KiteHub services use kitehub- prefix
case $SERVICE in
    postgres)    SERVICE="kite-postgres" ;;
    redis)       SERVICE="kite-redis" ;;
    rabbitmq)    SERVICE="kite-rabbitmq" ;;
    minio)       SERVICE="kite-minio" ;;
    mailhog)     SERVICE="kite-mailhog" ;;
    gateway)     SERVICE="kite-gateway" ;;
    subscription) SERVICE="kitehub-subscription" ;;
    branding)    SERVICE="kitehub-branding" ;;
    email)       SERVICE="kitehub-email" ;;
    admin)       SERVICE="kitehub-admin" ;;
    frontend)    SERVICE="kitehub-frontend" ;;
    # If already has prefix, keep as-is
    kite-*|kitehub-*|kiteclass-*) ;;
    # Default: try kitehub- prefix
    *)           SERVICE="kitehub-$SERVICE" ;;
esac

# Default commands for common services
if [ $# -eq 0 ]; then
    case $SERVICE in
        kite-postgres)
            docker exec -it $SERVICE psql -U kitehub -d kitehub
            ;;
        kite-redis)
            docker exec -it $SERVICE redis-cli
            ;;
        kite-rabbitmq)
            docker exec -it $SERVICE rabbitmqctl status
            ;;
        *)
            docker exec -it $SERVICE sh
            ;;
    esac
else
    docker exec -it $SERVICE "$@"
fi
