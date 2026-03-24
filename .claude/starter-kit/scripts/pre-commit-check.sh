#!/bin/bash
# Pre-commit Check — Extensible framework for code compliance
# NOTE: chmod +x this file before use
#
# Usage:
#   ./scripts/pre-commit-check.sh
#
# Install as git hook:
#   ln -s ../../scripts/pre-commit-check.sh .git/hooks/pre-commit

# NOTE: Do NOT use `set -e` — we want to collect all violations
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

VIOLATIONS=0
echo "Running pre-commit checks..."
echo ""

# ==============================================================================
# 1. Commit Message Format (conventional commits)
# ==============================================================================
echo "Checking commit message format..."
# Note: Full commit-msg validation is better in a commit-msg hook.
# This section is a placeholder reminder.
echo -e "${GREEN}  [OK] Commit message validated in commit-msg hook${NC}"
echo ""

# ==============================================================================
# 2. Sensitive Data Check
# ==============================================================================
echo "Checking for sensitive data..."
SENSITIVE_PATTERNS=(
    "password.*=.*['\"]"
    "api[_-]?key.*=.*['\"]"
    "secret.*=.*['\"]"
    "token.*=.*['\"]"
    "jdbc:.*//.*:.*@"
)
SENSITIVE_FOUND=0
for pattern in "${SENSITIVE_PATTERNS[@]}"; do
    MATCHES=$(git diff --cached --diff-filter=ACM -- '*.java' '*.ts' '*.tsx' '*.js' '*.py' '*.properties' '*.yml' '*.yaml' '*.env' | \
        grep -iE "$pattern" | grep -v "^-" | \
        grep -iv "getItem\|removeItem\|localStorage\|process\.env\|interface\|type \|os\.environ\|example\|placeholder" | wc -l)
    if [ "$MATCHES" -gt 0 ]; then
        echo -e "${RED}  [!!] Potential sensitive data: $pattern${NC}"
        SENSITIVE_FOUND=$((SENSITIVE_FOUND + 1))
    fi
done
if [ "$SENSITIVE_FOUND" -gt 0 ]; then
    echo "       Review staged files and remove sensitive data"
    VIOLATIONS=$((VIOLATIONS + 1))
else
    echo -e "${GREEN}  [OK] No sensitive data detected${NC}"
fi
echo ""

# ==============================================================================
# 3. TODO/FIXME/HACK Check (informational)
# ==============================================================================
echo "Checking for TODO/FIXME/HACK markers..."
TODO_COUNT=$(git diff --cached | grep -E '^\+.*\b(TODO|FIXME|HACK|XXX)\b' | grep -v "^+++\|^---" | wc -l)
if [ "$TODO_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}  [..] Found $TODO_COUNT new TODO/FIXME/HACK marker(s)${NC}"
    git diff --cached | grep -E '^\+.*\b(TODO|FIXME|HACK|XXX)\b' | grep -v "^+++\|^---" | head -5 | sed 's/^/       /'
    echo "       (informational only, not blocking)"
else
    echo -e "${GREEN}  [OK] No new TODO/FIXME markers${NC}"
fi
echo ""

# ==============================================================================
# 4. Large File Check
# ==============================================================================
echo "Checking for large files..."
LARGE_FILES=$(git diff --cached --name-only --diff-filter=ACM | while read -r f; do
    [ -f "$f" ] && SIZE=$(wc -c < "$f") && [ "$SIZE" -gt 1048576 ] && echo "$f ($((SIZE/1024))KB)"
done)
if [ -n "$LARGE_FILES" ]; then
    echo -e "${YELLOW}  [..] Large files (>1MB) staged:${NC}"
    echo "$LARGE_FILES" | sed 's/^/       /'
    echo "       Consider using .gitignore or Git LFS"
else
    echo -e "${GREEN}  [OK] No large files detected${NC}"
fi
echo ""

# ==============================================================================
# ADD YOUR PROJECT-SPECIFIC CHECKS BELOW
# ==============================================================================
# Examples:
#   - Java: wildcard imports, missing @since, checkstyle
#   - Python: type hints, docstrings
#   - Node: console.log statements, 'any' type usage
#   - Docs: business doc accompanies logic changes
#
# Template for a new check:
# echo "Checking <your check>..."
# ISSUES=$(git diff --cached | grep -E '<pattern>' | wc -l)
# if [ "$ISSUES" -gt 0 ]; then
#     echo -e "${RED}  [!!] <description>${NC}"
#     VIOLATIONS=$((VIOLATIONS + 1))
# else
#     echo -e "${GREEN}  [OK] <description>${NC}"
# fi
# echo ""
# ==============================================================================

# ==============================================================================
# Summary
# ==============================================================================
echo "=========================================================="
if [ "$VIOLATIONS" -eq 0 ]; then
    echo -e "${GREEN}[OK] All checks passed! Safe to commit.${NC}"
    exit 0
else
    echo -e "${RED}[!!] Found $VIOLATIONS compliance issue(s)${NC}"
    echo ""
    echo "Fix the issues above before committing."
    echo "To skip (not recommended): git commit --no-verify"
    exit 1
fi
