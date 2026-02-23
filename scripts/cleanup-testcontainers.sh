#!/bin/bash
# Cleanup Testcontainers
# Removes all Testcontainers leftovers after running tests
#
# Usage:
#   ./scripts/cleanup-testcontainers.sh

set -e

echo "🧹 Cleaning up Testcontainers..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Find all Testcontainers (running and stopped)
RUNNING=$(docker ps -q --filter "label=org.testcontainers=true" 2>/dev/null || true)
STOPPED=$(docker ps -aq --filter "label=org.testcontainers=true" 2>/dev/null || true)

RUNNING_COUNT=$(echo "$RUNNING" | grep -v '^$' | wc -l || echo 0)
STOPPED_COUNT=$(echo "$STOPPED" | grep -v '^$' | wc -l || echo 0)

if [ "$RUNNING_COUNT" -eq 0 ] && [ "$STOPPED_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✅ No Testcontainers to clean up${NC}"
    exit 0
fi

echo "Found Testcontainers:"
echo "  - Running: $RUNNING_COUNT"
echo "  - Stopped: $STOPPED_COUNT"
echo ""

if [ -n "$RUNNING" ] && [ "$RUNNING_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}Stopping running Testcontainers...${NC}"
    echo "$RUNNING" | xargs docker stop 2>/dev/null || true
    echo -e "${GREEN}✅ Stopped $RUNNING_COUNT containers${NC}"
fi

if [ -n "$STOPPED" ] && [ "$STOPPED_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}Removing stopped Testcontainers...${NC}"
    echo "$STOPPED" | xargs docker rm 2>/dev/null || true
    echo -e "${GREEN}✅ Removed $STOPPED_COUNT containers${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Cleanup complete!${NC}"
