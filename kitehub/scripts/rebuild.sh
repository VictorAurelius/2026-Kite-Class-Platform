#!/bin/bash
# Rebuild and restart KiteHub service(s)
# Usage: ./scripts/rebuild.sh <service|all> [--no-cache]
#
# Examples:
#   ./scripts/rebuild.sh gateway          # Rebuild gateway
#   ./scripts/rebuild.sh frontend         # Rebuild frontend
#   ./scripts/rebuild.sh all              # Rebuild all (uses build-all.sh)
#   ./scripts/rebuild.sh gateway --no-cache

set -e
cd "$(dirname "$0")/.."

if [ $# -eq 0 ]; then
    echo "Usage: ./scripts/rebuild.sh <service|all> [--no-cache]"
    echo ""
    echo "KiteHub:    subscription, branding, email, admin, gateway, frontend"
    echo "KiteClass:  kiteclass-core, kiteclass-frontend"
    echo "All:        all"
    exit 1
fi

SERVICE=$1
NO_CACHE=""

if [ "$2" = "--no-cache" ]; then
    NO_CACHE="--no-cache"
fi

# Handle 'all' case
if [ "$SERVICE" = "all" ]; then
    echo "Rebuilding all services..."
    ./scripts/build-all.sh $NO_CACHE
    echo ""
    echo "Restarting all services..."
    docker-compose -f docker-compose.kitehub.yml up -d
    exit 0
fi

# Add kitehub- prefix if not present (skip kiteclass-* services)
if [[ "$SERVICE" != kitehub-* ]] && [[ "$SERVICE" != kiteclass-* ]]; then
    SERVICE="kitehub-$SERVICE"
fi

echo "=============================================="
echo "  Rebuilding $SERVICE"
echo "=============================================="

# For backend services, need to rebuild base first if --no-cache
if [ "$NO_CACHE" = "--no-cache" ] && [ "$SERVICE" != "kitehub-frontend" ]; then
    echo ""
    echo "[1/3] Rebuilding kitehub-base (dependencies)..."
    docker build $NO_CACHE -t kitehub-base:latest -f kitehub-base/Dockerfile .
fi

echo ""
echo "[2/3] Building $SERVICE..."
docker-compose -f docker-compose.kitehub.yml build $NO_CACHE "$SERVICE"

echo ""
echo "[3/3] Restarting $SERVICE..."
docker-compose -f docker-compose.kitehub.yml up -d "$SERVICE"

echo ""
echo "Done. Waiting for health check..."
sleep 5
docker-compose -f docker-compose.kitehub.yml ps "$SERVICE"
