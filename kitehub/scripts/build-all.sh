#!/bin/bash
# Build all KiteHub Docker images
# Usage: ./scripts/build-all.sh [--no-cache]

set -e

cd "$(dirname "$0")/.."

CACHE_FLAG=""
if [ "$1" = "--no-cache" ]; then
    CACHE_FLAG="--no-cache"
    echo "Building without cache..."
fi

echo "=============================================="
echo "  Kite Platform Docker Build"
echo "=============================================="

# Step 1: Build base image first (contains Maven dependencies)
echo ""
echo "[1/5] Building kitehub-base image..."
docker build $CACHE_FLAG -t kitehub-base:latest -f kitehub-base/Dockerfile .

# Step 2: Build KiteHub backend services (use base image)
echo ""
echo "[2/5] Building KiteHub backend services..."
docker-compose -f docker-compose.kitehub.yml build $CACHE_FLAG \
    kitehub-subscription \
    kitehub-branding \
    kitehub-email \
    kitehub-admin \
    kite-gateway

# Step 3: Build KiteHub frontend
echo ""
echo "[3/5] Building kitehub-frontend..."
docker-compose -f docker-compose.kitehub.yml build $CACHE_FLAG kitehub-frontend

# Step 4: Build KiteClass core (self-contained context — does not use kitehub-base)
echo ""
echo "[4/5] Building kiteclass-core..."
docker-compose -f docker-compose.kitehub.yml build $CACHE_FLAG kiteclass-core

# Step 5: Build KiteClass frontend
echo ""
echo "[5/5] Building kiteclass-frontend..."
docker-compose -f docker-compose.kitehub.yml build $CACHE_FLAG kiteclass-frontend

echo ""
echo "=============================================="
echo "  Build Complete!"
echo "=============================================="
echo ""
echo "To start: docker-compose -f docker-compose.kitehub.yml up -d"
echo ""
docker images | grep -E "kite-|kitehub-|kiteclass-"
