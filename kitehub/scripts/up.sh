#!/bin/bash
# Start KiteHub stack
# Usage: ./scripts/up.sh [service...]

set -e
cd "$(dirname "$0")/.."

if [ $# -eq 0 ]; then
    echo "Starting all KiteHub services..."
    docker-compose -f docker-compose.kitehub.yml up -d
else
    echo "Starting: $@"
    docker-compose -f docker-compose.kitehub.yml up -d "$@"
fi

echo ""
echo "Waiting for services to be ready..."
sleep 3
docker-compose -f docker-compose.kitehub.yml ps
