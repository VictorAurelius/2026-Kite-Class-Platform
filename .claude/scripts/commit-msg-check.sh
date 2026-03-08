#!/bin/bash
# Commit Message Validation Hook
# Validates commit message format and length
#
# Usage: Install as git hook:
#   ln -s ../../.claude/scripts/commit-msg-check.sh .git/hooks/commit-msg

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

COMMIT_MSG_FILE=$1
COMMIT_MSG=$(head -n 1 "$COMMIT_MSG_FILE")

echo "📏 Validating commit message..."

# Extract subject line (first line)
SUBJECT_LINE=$(echo "$COMMIT_MSG" | head -n 1)
MSG_LENGTH=${#SUBJECT_LINE}

VIOLATIONS=0

# ==============================================================================
# Check 1: Message Length (25-50 characters)
# ==============================================================================
if [ "$MSG_LENGTH" -lt 25 ]; then
    echo -e "${RED}❌ Commit message too short: $MSG_LENGTH characters (minimum 25)${NC}"
    echo "   Current: \"$SUBJECT_LINE\""
    echo ""
    echo "   Tips:"
    echo "   - Be specific about what changed"
    echo "   - Use active voice: 'Add feature' not 'Added feature'"
    echo "   - Include scope: 'feat(api): add user endpoint'"
    VIOLATIONS=$((VIOLATIONS + 1))
elif [ "$MSG_LENGTH" -gt 50 ]; then
    echo -e "${RED}❌ Commit message too long: $MSG_LENGTH characters (maximum 50)${NC}"
    echo "   Current: \"$SUBJECT_LINE\""
    echo ""
    echo "   Tips:"
    echo "   - Keep subject line concise"
    echo "   - Move details to commit body (after blank line)"
    echo "   - Example:"
    echo "     fix(auth): resolve token expiration bug"
    echo "     "
    echo "     - Update token validation logic"
    echo "     - Add refresh token rotation"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Commit message length OK ($MSG_LENGTH characters)${NC}"
fi

# ==============================================================================
# Check 2: Format (type(scope): description)
# ==============================================================================
if ! echo "$SUBJECT_LINE" | grep -qE '^(feat|fix|docs|style|refactor|test|chore|perf|ci|build|revert)\(.+\):'; then
    echo -e "${YELLOW}⚠️  Commit message doesn't follow format: type(scope): description${NC}"
    echo "   Current: \"$SUBJECT_LINE\""
    echo ""
    echo "   Expected format:"
    echo "   feat(api): add user authentication"
    echo "   fix(db): resolve connection timeout"
    echo ""
    echo "   Types: feat, fix, docs, style, refactor, test, chore"
fi

# ==============================================================================
# Check 3: No AI/Claude in Co-Authored-By or Author
# ==============================================================================
FULL_MSG=$(cat "$COMMIT_MSG_FILE")
if echo "$FULL_MSG" | grep -iE "Co-Authored-By.*(claude|AI Assistant|noreply@anthropic\.com)"; then
    echo ""
    echo -e "${RED}❌ Commit message contains AI attribution in Co-Authored-By line${NC}"
    echo "   Found: Claude / AI Assistant / noreply@anthropic.com"
    echo "   This violates authorship policy"
    echo ""
    echo "   Remove or replace Co-Authored-By line:"
    echo "   - Remove: Delete the Co-Authored-By line completely"
    echo "   - Replace: Use only human collaborators"
    VIOLATIONS=$((VIOLATIONS + 1))
fi

# ==============================================================================
# Summary
# ==============================================================================
if [ "$VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}✅ Commit message validated successfully${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}❌ Commit message validation failed${NC}"
    echo ""
    echo "To skip this check (not recommended):"
    echo "  git commit --no-verify"
    exit 1
fi
