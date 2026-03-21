#!/bin/bash
# Clean KiteHub Docker resources
# Usage: ./scripts/clean.sh [--all]
#
# Options:
#   (default)  Remove containers and networks only
#   --all      Remove containers, networks, volumes, and images

set -e
cd "$(dirname "$0")/.."

echo "=============================================="
echo "  KiteHub Cleanup"
echo "=============================================="

if [ "$1" = "--all" ]; then
    echo ""
    echo "WARNING: This will remove ALL KiteHub data including:"
    echo "  - All containers"
    echo "  - All volumes (database, minio data)"
    echo "  - All images"
    echo ""
    read -p "Are you sure? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "Cancelled."
        exit 0
    fi

    echo ""
    echo "[1/4] Stopping containers..."
    docker-compose -f docker-compose.kitehub.yml down -v

    echo ""
    echo "[2/4] Removing KiteHub images..."
    docker images --filter "reference=kitehub-*" -q | xargs -r docker rmi -f

    echo ""
    echo "[3/4] Removing infrastructure images..."
    docker images --filter "reference=postgres" -q | xargs -r docker rmi -f 2>/dev/null || true
    docker images --filter "reference=redis" -q | xargs -r docker rmi -f 2>/dev/null || true
    docker images --filter "reference=rabbitmq" -q | xargs -r docker rmi -f 2>/dev/null || true
    docker images --filter "reference=minio/*" -q | xargs -r docker rmi -f 2>/dev/null || true

    echo ""
    echo "[4/4] Pruning unused resources..."
    docker system prune -f

    echo ""
    echo "✅ Full cleanup complete."
else
    echo ""
    echo "[1/2] Stopping containers..."
    docker-compose -f docker-compose.kitehub.yml down

    echo ""
    echo "[2/2] Done. Volumes and images preserved."
    echo ""
    echo "To remove everything: ./scripts/clean.sh --all"
fi
