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
echo "  KiteHub Docker Build"
echo "=============================================="

# Step 1: Build base image first (contains Maven dependencies)
echo ""
echo "[1/6] Building kitehub-base image..."
docker build $CACHE_FLAG -t kitehub-base:latest -f kitehub-base/Dockerfile .

# Step 2: Build backend services (in parallel using base image)
echo ""
echo "[2/6] Building backend services..."
docker-compose -f docker-compose.kitehub.yml build $CACHE_FLAG \
    kitehub-subscription \
    kitehub-branding \
    kitehub-email \
    kitehub-admin \
    kitehub-gateway

# Step 3: Build frontend
echo ""
echo "[3/6] Building kitehub-frontend..."
docker-compose -f docker-compose.kitehub.yml build $CACHE_FLAG kitehub-frontend

echo ""
echo "=============================================="
echo "  Build Complete!"
echo "=============================================="
echo ""
echo "To start: docker-compose -f docker-compose.kitehub.yml up -d"
echo ""
docker images | grep kitehub
