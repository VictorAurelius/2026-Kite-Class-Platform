#!/bin/bash
# Start KiteHub stack
# Usage: ./scripts/up.sh [--profile PROFILE] [service...]

set -e
cd "$(dirname "$0")/.."

PROFILE=""
SERVICES=()

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        *)
            SERVICES+=("$1")
            shift
            ;;
    esac
done

# Build docker-compose command
CMD="docker-compose -f docker-compose.kitehub.yml"
if [ -n "$PROFILE" ]; then
    CMD="$CMD --profile $PROFILE"
fi

if [ ${#SERVICES[@]} -eq 0 ]; then
    echo "Starting all KiteHub services..."
    if [ -n "$PROFILE" ]; then
        echo "Profile: $PROFILE"
    fi
    $CMD up -d
else
    echo "Starting: ${SERVICES[*]}"
    if [ -n "$PROFILE" ]; then
        echo "Profile: $PROFILE"
    fi
    $CMD up -d "${SERVICES[@]}"
fi

echo ""
echo "Waiting for services to be ready..."
sleep 3
docker-compose -f docker-compose.kitehub.yml ps
