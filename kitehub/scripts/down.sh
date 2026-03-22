#!/bin/bash
# Stop KiteHub stack
# Usage: ./scripts/down.sh [--volumes]

set -e
cd "$(dirname "$0")/.."

if [ "$1" = "--volumes" ] || [ "$1" = "-v" ]; then
    echo "Stopping KiteHub and removing volumes..."
    docker-compose -f docker-compose.kitehub.yml down -v
else
    echo "Stopping KiteHub (preserving volumes)..."
    docker-compose -f docker-compose.kitehub.yml down
fi

echo "Done."
