#!/bin/bash
# Docker Build Script với Version Tracking
# Tự động tag images với commit hash và PR number để track version giữa các máy

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Log file
LOG_DIR=".docker-build-logs"
LOG_FILE="$LOG_DIR/build-history.log"
CURRENT_VERSION_FILE="$LOG_DIR/current-version.txt"

# Tạo log directory nếu chưa có
mkdir -p "$LOG_DIR"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  KiteClass Platform - Docker Build Script${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Lấy thông tin Git
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")
COMMIT_HASH=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
COMMIT_MSG=$(git log -1 --pretty=%B 2>/dev/null | head -1 || echo "unknown")
BUILD_DATE=$(date '+%Y-%m-%d %H:%M:%S')

# Detect PR number from branch name
PR_NUMBER=""
if [[ $BRANCH =~ feature/PR-([0-9]+\.[0-9]+) ]]; then
    PR_NUMBER="${BASH_REMATCH[1]}"
elif [[ $BRANCH =~ PR-([0-9]+) ]]; then
    PR_NUMBER="${BASH_REMATCH[1]}"
fi

# Build tag
if [ -n "$PR_NUMBER" ]; then
    BUILD_TAG="pr-${PR_NUMBER}-${COMMIT_HASH}"
    VERSION_LABEL="PR ${PR_NUMBER} (${COMMIT_HASH})"
else
    BUILD_TAG="${BRANCH}-${COMMIT_HASH}"
    VERSION_LABEL="${BRANCH} (${COMMIT_HASH})"
fi

echo -e "${YELLOW}📋 Build Information:${NC}"
echo -e "  Branch:      ${GREEN}${BRANCH}${NC}"
echo -e "  PR:          ${GREEN}${PR_NUMBER:-N/A}${NC}"
echo -e "  Commit:      ${GREEN}${COMMIT_HASH}${NC}"
echo -e "  Message:     ${COMMIT_MSG}"
echo -e "  Build Tag:   ${GREEN}${BUILD_TAG}${NC}"
echo -e "  Build Date:  ${BUILD_DATE}"
echo ""

# Log build info
cat >> "$LOG_FILE" <<EOF
================================================================================
Build Date:    $BUILD_DATE
Branch:        $BRANCH
PR Number:     ${PR_NUMBER:-N/A}
Commit:        $COMMIT_HASH
Build Tag:     $BUILD_TAG
Commit Msg:    $COMMIT_MSG
================================================================================

EOF

# Save current version
cat > "$CURRENT_VERSION_FILE" <<EOF
KiteClass Platform - Current Docker Build Version
==================================================
Build Date:    $BUILD_DATE
Branch:        $BRANCH
PR Number:     ${PR_NUMBER:-N/A}
Commit Hash:   $COMMIT_HASH
Build Tag:     $BUILD_TAG
Commit Msg:    $COMMIT_MSG

Services Built:
- kiteclass-core:${BUILD_TAG}
- kiteclass-frontend:${BUILD_TAG}

To rebuild this exact version, checkout commit: $COMMIT_HASH
EOF

echo -e "${YELLOW}🔨 Building Docker images...${NC}"
echo ""

# Build với labels và tags
docker compose -f docker-compose.dev.yml build \
    --build-arg BUILD_TAG="$BUILD_TAG" \
    --build-arg COMMIT_HASH="$COMMIT_HASH" \
    --build-arg BUILD_DATE="$BUILD_DATE" \
    --build-arg PR_NUMBER="${PR_NUMBER:-N/A}" \
    --build-arg BRANCH="$BRANCH"

echo ""
echo -e "${GREEN}✅ Build completed successfully!${NC}"
echo ""

# Tag images with version
echo -e "${YELLOW}🏷️  Tagging images with version: ${BUILD_TAG}${NC}"
docker tag kiteclass-core "kiteclass-core:${BUILD_TAG}"
docker tag kiteclass-frontend "kiteclass-frontend:${BUILD_TAG}"

echo ""
echo -e "${GREEN}📦 Images tagged:${NC}"
echo -e "  - kiteclass-core:${BUILD_TAG}"
echo -e "  - kiteclass-frontend:${BUILD_TAG}"
echo ""

# Show current version
echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  Current Version (saved to ${CURRENT_VERSION_FILE})${NC}"
echo -e "${BLUE}================================================${NC}"
cat "$CURRENT_VERSION_FILE"
echo ""

echo -e "${YELLOW}💡 Next steps:${NC}"
echo -e "  1. Start services:    ${GREEN}docker compose -f docker-compose.dev.yml up -d${NC}"
echo -e "  2. View logs:         ${GREEN}docker compose -f docker-compose.dev.yml logs -f${NC}"
echo -e "  3. Check version:     ${GREEN}cat $CURRENT_VERSION_FILE${NC}"
echo -e "  4. View build history: ${GREEN}cat $LOG_FILE${NC}"
echo ""
