#!/bin/bash
# Rebuild and restart KiteHub service(s)
# Usage: ./scripts/rebuild.sh <service|all> [--no-cache] [--rebuild-base]
#
# Examples:
#   ./scripts/rebuild.sh gateway              # Rebuild gateway (uses cached base)
#   ./scripts/rebuild.sh frontend             # Rebuild frontend
#   ./scripts/rebuild.sh all                  # Rebuild all (uses build-all.sh)
#   ./scripts/rebuild.sh gateway --no-cache   # Full no-cache rebuild (includes base)
#   ./scripts/rebuild.sh branding --rebuild-base  # Force base rebuild then build service

set -e
cd "$(dirname "$0")/.."

if [ $# -eq 0 ]; then
    echo "Usage: ./scripts/rebuild.sh <service|all> [--no-cache] [--rebuild-base]"
    echo ""
    echo "KiteHub:    subscription, branding, email, admin, gateway, frontend"
    echo "KiteClass:  kiteclass-core, kiteclass-frontend"
    echo "All:        all"
    echo ""
    echo "Flags:"
    echo "  --no-cache      Full rebuild without Docker cache (includes base image)"
    echo "  --rebuild-base  Rebuild kitehub-base (re-downloads Maven deps) then rebuild service"
    exit 1
fi

SERVICE=$1
NO_CACHE=""
REBUILD_BASE=false

for arg in "$@"; do
    if [ "$arg" = "--no-cache" ]; then
        NO_CACHE="--no-cache"
        REBUILD_BASE=true
    fi
    if [ "$arg" = "--rebuild-base" ]; then
        REBUILD_BASE=true
    fi
done

# Handle 'all' case
if [ "$SERVICE" = "all" ]; then
    echo "Rebuilding all services..."
    ./scripts/build-all.sh $NO_CACHE
    echo ""
    echo "Restarting all services..."
    # NOTE: every service in docker-compose.kitehub.yml carries a `profiles:` tag,
    # so a bare `up -d` selects nothing ("no service selected"). Must pass the
    # full profile to bring the whole stack up. (Fixed 2026-06-08 — see GAP filed.)
    docker-compose -f docker-compose.kitehub.yml --profile full up -d
    exit 0
fi

# Special case: gateway compose service is 'kite-gateway' (not 'kitehub-gateway')
if [ "$SERVICE" = "gateway" ] || [ "$SERVICE" = "kitehub-gateway" ]; then
    SERVICE="kite-gateway"
# Add kitehub- prefix if not present (skip kiteclass-* and kite-* services)
elif [[ "$SERVICE" != kitehub-* ]] && [[ "$SERVICE" != kiteclass-* ]] && [[ "$SERVICE" != kite-* ]]; then
    SERVICE="kitehub-$SERVICE"
fi

echo "=============================================="
echo "  Rebuilding $SERVICE"
echo "=============================================="

# Rebuild base image if requested (--no-cache or --rebuild-base)
# Use case: new Maven dependencies added to pom.xml need to be downloaded into base
if [ "$REBUILD_BASE" = true ] && [ "$SERVICE" != "kitehub-frontend" ]; then
    echo ""
    echo "[1/3] Rebuilding kitehub-base (Maven dependency cache)..."
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
