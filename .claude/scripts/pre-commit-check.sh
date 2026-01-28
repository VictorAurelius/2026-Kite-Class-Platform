#!/bin/bash
# Pre-commit Skills Compliance Check
# Auto-checks code compliance with project skills before committing
#
# Usage: Run this script before `git commit`
#   ./pre-commit-check.sh
#
# Or install as git hook:
#   ln -s ../../.claude/scripts/pre-commit-check.sh .git/hooks/pre-commit

set -e

echo "🔍 Running Skills Compliance Check..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

VIOLATIONS=0

# ==============================================================================
# 1. Check JavaDoc for Public Methods
# ==============================================================================
echo "📝 Checking JavaDoc compliance..."

# Find Java files with public methods missing JavaDoc
MISSING_JAVADOC=$(git diff --cached --name-only | grep '\.java$' | xargs -I {} sh -c '
  if [ -f "{}" ]; then
    grep -B 5 "^\s*public.*(" {} | grep -B 5 -v "^\s*/\*\*" | grep "^\s*public" || true
  fi
' | wc -l)

if [ "$MISSING_JAVADOC" -gt 0 ]; then
    echo -e "${RED}❌ Found $MISSING_JAVADOC public methods potentially missing JavaDoc${NC}"
    echo "   Review: code-style.md lines 110-158"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ JavaDoc compliance OK${NC}"
fi
echo ""

# ==============================================================================
# 2. Check Error Code Usage
# ==============================================================================
echo "🚨 Checking error code usage..."

# Check for hardcoded error messages in ServiceImpl
HARDCODED_ERRORS=$(git diff --cached | grep -E '(throw new .*(Exception|Error)\("(?!.*_.*)")' | wc -l)

if [ "$HARDCODED_ERRORS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Found $HARDCODED_ERRORS potential hardcoded error messages${NC}"
    echo "   Expected: throw new Exception(\"ERROR_CODE\", args)"
    echo "   Review: error-logging.md lines 24-157"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Error code usage OK${NC}"
fi
echo ""

# ==============================================================================
# 3. Check @since Annotations
# ==============================================================================
echo "📅 Checking @since annotations..."

# Get current branch to determine PR version
BRANCH=$(git branch --show-current)

# Check if new Java files have @since annotation
NEW_JAVA_FILES=$(git diff --cached --name-only --diff-filter=A | grep '\.java$' || true)

if [ -n "$NEW_JAVA_FILES" ]; then
    MISSING_SINCE=0
    for file in $NEW_JAVA_FILES; do
        if ! grep -q "@since" "$file"; then
            echo -e "${RED}❌ Missing @since in $file${NC}"
            MISSING_SINCE=$((MISSING_SINCE + 1))
        fi
    done

    if [ "$MISSING_SINCE" -gt 0 ]; then
        echo "   Review: code-style.md lines 111-169"
        VIOLATIONS=$((VIOLATIONS + 1))
    else
        echo -e "${GREEN}✅ @since annotations OK${NC}"
    fi
else
    echo -e "${GREEN}✅ No new Java files to check${NC}"
fi
echo ""

# ==============================================================================
# 4. Check Import Ordering
# ==============================================================================
echo "📦 Checking import ordering..."

# Check for wildcard imports
WILDCARD_IMPORTS=$(git diff --cached | grep -E '^\+import .*\.\*;' | wc -l)

if [ "$WILDCARD_IMPORTS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Found $WILDCARD_IMPORTS wildcard imports${NC}"
    echo "   Avoid: import java.util.*"
    echo "   Use: import java.util.List"
    echo "   Review: code-style.md lines 286-309"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Import ordering OK${NC}"
fi
echo ""

# ==============================================================================
# 5. Check Sensitive Data
# ==============================================================================
echo "🔐 Checking for sensitive data..."

# Check for common sensitive patterns
SENSITIVE_PATTERNS=(
    "password.*=.*['\"]"
    "api[_-]?key.*=.*['\"]"
    "secret.*=.*['\"]"
    "token.*=.*['\"]"
    "jdbc:.*//.*:.*@"
)

SENSITIVE_FOUND=0
for pattern in "${SENSITIVE_PATTERNS[@]}"; do
    MATCHES=$(git diff --cached | grep -iE "$pattern" | grep -v "^-" | wc -l)
    if [ "$MATCHES" -gt 0 ]; then
        echo -e "${RED}❌ Potential sensitive data found: $pattern${NC}"
        SENSITIVE_FOUND=$((SENSITIVE_FOUND + 1))
    fi
done

if [ "$SENSITIVE_FOUND" -gt 0 ]; then
    echo "   Review staged files and remove sensitive data"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ No sensitive data detected${NC}"
fi
echo ""

# ==============================================================================
# 6. Check Messages.properties Updates
# ==============================================================================
echo "🌐 Checking messages.properties..."

# If exception classes changed, check if messages.properties updated
EXCEPTION_CHANGES=$(git diff --cached --name-only | grep -E '(Exception|Error)\.java$' | wc -l)
MESSAGES_CHANGES=$(git diff --cached --name-only | grep 'messages.*\.properties' | wc -l)

if [ "$EXCEPTION_CHANGES" -gt 0 ] && [ "$MESSAGES_CHANGES" -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Exception classes changed but messages.properties not updated${NC}"
    echo "   Did you add new error codes to messages.properties?"
    echo "   Review: error-logging.md lines 24-106"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}✅ Messages.properties check OK${NC}"
fi
echo ""

# ==============================================================================
# Summary
# ==============================================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! Safe to commit.${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Stage your changes: git add -A"
    echo "2. Commit with proper format: git commit -m \"type(scope): description\""
    echo "3. Include Co-Authored-By line"
    exit 0
else
    echo -e "${RED}❌ Found $VIOLATIONS compliance issue(s)${NC}"
    echo ""
    echo "Please fix the issues above before committing."
    echo ""
    echo "Resources:"
    echo "- Full checklist: .claude/skills/skills-compliance-checklist.md"
    echo "- Code style: .claude/skills/code-style.md"
    echo "- Error logging: .claude/skills/error-logging.md"
    echo ""
    echo "To skip this check (not recommended):"
    echo "  git commit --no-verify"
    exit 1
fi
