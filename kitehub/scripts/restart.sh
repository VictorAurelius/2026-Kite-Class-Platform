#!/bin/bash
# Restart KiteHub service(s)
# Usage: ./scripts/restart.sh [service...]
#
# Examples:
#   ./scripts/restart.sh                 # Restart all
#   ./scripts/restart.sh gateway         # Restart gateway only
#   ./scripts/restart.sh gateway admin   # Restart gateway and admin

set -e
cd "$(dirname "$0")/.."

if [ $# -eq 0 ]; then
    echo "Restarting all KiteHub services..."
    docker-compose -f docker-compose.kitehub.yml restart
else
    # Add kitehub- prefix if not present
    SERVICES=""
    for svc in "$@"; do
        if [[ "$svc" == kitehub-* ]]; then
            SERVICES="$SERVICES $svc"
        else
            SERVICES="$SERVICES kitehub-$svc"
        fi
    done

    echo "Restarting:$SERVICES"
    docker-compose -f docker-compose.kitehub.yml restart $SERVICES
fi

echo ""
sleep 2
docker-compose -f docker-compose.kitehub.yml ps
