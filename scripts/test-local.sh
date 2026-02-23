#!/bin/bash
# Local Test Runner with Auto-cleanup
# Runs tests and automatically cleans up Testcontainers
#
# Usage:
#   ./scripts/test-local.sh [core|gateway|all]

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SERVICE="${1:-all}"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  KiteClass - Local Test Runner${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Cleanup function to run on exit
cleanup() {
    echo ""
    echo -e "${YELLOW}🧹 Running post-test cleanup...${NC}"
    ./scripts/cleanup-testcontainers.sh
}

# Register cleanup to run on script exit (success or failure)
trap cleanup EXIT

# Run tests based on service selection
case "$SERVICE" in
    core)
        echo -e "${BLUE}📦 Testing Core Service...${NC}"
        echo ""
        cd kiteclass/kiteclass-core
        ./mvnw clean test
        ;;
    gateway)
        echo -e "${BLUE}🚪 Testing Gateway Service...${NC}"
        echo ""
        cd kiteclass/kiteclass-gateway
        ./mvnw clean test
        ;;
    all)
        echo -e "${BLUE}📦 Testing Core Service...${NC}"
        echo ""
        cd kiteclass/kiteclass-core
        ./mvnw clean test
        cd ../..

        echo ""
        echo -e "${BLUE}🚪 Testing Gateway Service...${NC}"
        echo ""
        cd kiteclass/kiteclass-gateway
        ./mvnw clean test
        cd ../..
        ;;
    *)
        echo -e "${RED}❌ Invalid service: $SERVICE${NC}"
        echo "Usage: $0 [core|gateway|all]"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}✅ All tests completed successfully!${NC}"
