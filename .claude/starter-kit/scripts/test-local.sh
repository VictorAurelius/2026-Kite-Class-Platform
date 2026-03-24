#!/bin/bash
# Local Test Runner — Run before pushing
# Auto-detects changed files and runs appropriate tests
# NOTE: chmod +x this file before use
#
# Usage:
#   ./scripts/test-local.sh                    # Auto-detect changed files
#   ./scripts/test-local.sh backend            # Test backend only
#   ./scripts/test-local.sh frontend           # Test frontend only
#   ./scripts/test-local.sh all                # Test everything
#   ./scripts/test-local.sh --quick            # Compile/lint only (no full tests)

set -e

# =============================================================================
# CONFIGURE THESE FOR YOUR PROJECT
# =============================================================================
# Each entry: "label:path:type" where type is java|node|python
# Example: "api:services/api:java" "web:frontend:node" "ml:ml-service:python"
PROJECT_DIRS=(
    # "backend:my-backend:java"
    # "frontend:my-frontend:node"
    # "ml:ml-service:python"
)

# Patterns to detect which project dir changed (glob matched against file paths)
# Maps to PROJECT_DIRS by index. If empty, all dirs are tested.
# Example: "my-backend/*" "my-frontend/*" "ml-service/*"
DETECT_PATTERNS=(
    # "my-backend/*"
    # "my-frontend/*"
    # "ml-service/*"
)

# Files that count as "docs only" (no tests needed)
DOCS_PATTERNS="documents/*|*.md|*.txt|*.rst|LICENSE|CHANGELOG"
# =============================================================================

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$ROOT_DIR"

QUICK_MODE=false
PASSED=0
FAILED=0
SKIPPED=0

# Parse arguments
if [[ "$1" == "--quick" ]]; then
    QUICK_MODE=true
    shift
fi

EXPLICIT_TARGET="${1:-}"

echo -e "${BLUE}==========================================================${NC}"
echo -e "${BLUE}  Local Test Runner (pre-push)${NC}"
echo -e "${BLUE}==========================================================${NC}"
echo ""

# Helper: run a test step
run_step() {
    local name="$1"
    local cmd="$2"
    local dir="$3"

    echo -e "${BLUE}> $name${NC}"
    if (cd "$dir" && eval "$cmd") 2>&1 | tail -5; then
        echo -e "${GREEN}  [OK] $name passed${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  [!!] $name FAILED${NC}"
        ((FAILED++))
    fi
    echo ""
}

# Run tests for a project dir by type
run_tests_for() {
    local label="$1"
    local dir="$2"
    local type="$3"

    if [ ! -d "$dir" ]; then
        echo -e "${YELLOW}  [SKIP] $label: directory not found ($dir)${NC}"
        ((SKIPPED++))
        return
    fi

    case "$type" in
        java)
            if [ -f "$dir/mvnw" ]; then
                if $QUICK_MODE; then
                    run_step "$label: Compile" "./mvnw compile -q 2>&1" "$dir"
                else
                    run_step "$label: Compile" "./mvnw compile -q 2>&1" "$dir"
                    run_step "$label: Unit Tests" "./mvnw test -q 2>&1" "$dir"
                fi
            elif [ -f "$dir/gradlew" ]; then
                if $QUICK_MODE; then
                    run_step "$label: Compile" "./gradlew compileJava 2>&1" "$dir"
                else
                    run_step "$label: Build" "./gradlew build 2>&1" "$dir"
                fi
            else
                echo -e "${YELLOW}  [SKIP] $label: no mvnw or gradlew found${NC}"
                ((SKIPPED++))
            fi
            ;;
        node)
            if [ -f "$dir/package.json" ]; then
                if $QUICK_MODE; then
                    # Try lint first, fall back to tsc
                    if grep -q '"lint"' "$dir/package.json"; then
                        run_step "$label: Lint" "npm run lint 2>&1" "$dir"
                    elif grep -q '"tsc"' "$dir/package.json" || [ -f "$dir/tsconfig.json" ]; then
                        run_step "$label: Type Check" "npx tsc --noEmit 2>&1" "$dir"
                    fi
                else
                    # Try vitest, jest, or generic test script
                    if grep -q '"vitest"' "$dir/package.json"; then
                        run_step "$label: Vitest" "npx vitest run --reporter=verbose 2>&1" "$dir"
                    elif grep -q '"jest"' "$dir/package.json"; then
                        run_step "$label: Jest" "npx jest --verbose 2>&1" "$dir"
                    elif grep -q '"test"' "$dir/package.json"; then
                        run_step "$label: Tests" "npm test 2>&1" "$dir"
                    else
                        echo -e "${YELLOW}  [SKIP] $label: no test runner found${NC}"
                        ((SKIPPED++))
                    fi
                fi
            else
                echo -e "${YELLOW}  [SKIP] $label: no package.json found${NC}"
                ((SKIPPED++))
            fi
            ;;
        python)
            if [ -f "$dir/pyproject.toml" ] || [ -f "$dir/setup.py" ] || [ -f "$dir/requirements.txt" ]; then
                if $QUICK_MODE; then
                    if command -v ruff &>/dev/null; then
                        run_step "$label: Ruff Lint" "ruff check . 2>&1" "$dir"
                    elif command -v flake8 &>/dev/null; then
                        run_step "$label: Flake8" "flake8 . 2>&1" "$dir"
                    fi
                else
                    if command -v pytest &>/dev/null; then
                        run_step "$label: Pytest" "pytest -v 2>&1" "$dir"
                    elif [ -f "$dir/manage.py" ]; then
                        run_step "$label: Django Tests" "python manage.py test 2>&1" "$dir"
                    else
                        echo -e "${YELLOW}  [SKIP] $label: no test runner found${NC}"
                        ((SKIPPED++))
                    fi
                fi
            else
                echo -e "${YELLOW}  [SKIP] $label: no Python project found${NC}"
                ((SKIPPED++))
            fi
            ;;
        *)
            echo -e "${YELLOW}  [SKIP] $label: unknown type '$type'${NC}"
            ((SKIPPED++))
            ;;
    esac
}

# --- Auto-detect or explicit target ---
if [ -n "$EXPLICIT_TARGET" ] && [ "$EXPLICIT_TARGET" != "all" ]; then
    # Run specific target
    FOUND=false
    for entry in "${PROJECT_DIRS[@]}"; do
        IFS=: read -r label dir type <<< "$entry"
        if [ "$label" = "$EXPLICIT_TARGET" ]; then
            run_tests_for "$label" "$dir" "$type"
            FOUND=true
            break
        fi
    done
    if ! $FOUND; then
        echo -e "${RED}Unknown target: $EXPLICIT_TARGET${NC}"
        echo "Available targets:"
        for entry in "${PROJECT_DIRS[@]}"; do
            IFS=: read -r label dir type <<< "$entry"
            echo "  $label ($type) -> $dir"
        done
        exit 1
    fi
elif [ "$EXPLICIT_TARGET" = "all" ]; then
    # Run all
    for entry in "${PROJECT_DIRS[@]}"; do
        IFS=: read -r label dir type <<< "$entry"
        run_tests_for "$label" "$dir" "$type"
    done
else
    # Auto-detect based on changed files
    CHANGED=$(git diff --cached --name-only 2>/dev/null || git diff HEAD --name-only 2>/dev/null || echo "")

    if [ -z "$CHANGED" ]; then
        echo -e "${YELLOW}No changed files detected. Run with explicit target:${NC}"
        echo "  $0 <target>    # Test specific project"
        echo "  $0 all         # Test everything"
        echo ""
        echo "Available targets:"
        for entry in "${PROJECT_DIRS[@]}"; do
            IFS=: read -r label dir type <<< "$entry"
            echo "  $label ($type) -> $dir"
        done
        exit 0
    fi

    # Check if docs only
    HAS_CODE=false
    while IFS= read -r file; do
        [[ -z "$file" ]] && continue
        IS_DOC=false
        IFS='|' read -ra PATS <<< "$DOCS_PATTERNS"
        for pat in "${PATS[@]}"; do
            # shellcheck disable=SC2254
            case "$file" in $pat) IS_DOC=true; break ;; esac
        done
        if ! $IS_DOC; then
            HAS_CODE=true
            break
        fi
    done <<< "$CHANGED"

    if ! $HAS_CODE; then
        echo -e "${GREEN}[OK] Only documentation changes -- no tests needed${NC}"
        exit 0
    fi

    # Detect which project dirs have changes
    echo -e "${BLUE}Auto-detecting changes...${NC}"
    TESTED_ANY=false

    for i in "${!PROJECT_DIRS[@]}"; do
        IFS=: read -r label dir type <<< "${PROJECT_DIRS[$i]}"
        PATTERN="${DETECT_PATTERNS[$i]:-$dir/*}"

        HAS_CHANGES=false
        while IFS= read -r file; do
            [[ -z "$file" ]] && continue
            # shellcheck disable=SC2254
            case "$file" in $PATTERN) HAS_CHANGES=true; break ;; esac
        done <<< "$CHANGED"

        if $HAS_CHANGES; then
            echo -e "  Changed: ${BLUE}$label${NC} ($dir)"
            run_tests_for "$label" "$dir" "$type"
            TESTED_ANY=true
        fi
    done

    if ! $TESTED_ANY; then
        echo -e "${YELLOW}No project dirs matched changed files.${NC}"
        echo "Configure PROJECT_DIRS and DETECT_PATTERNS in this script."
    fi
fi

# --- Summary ---
echo -e "${BLUE}==========================================================${NC}"
echo -e "  Results: ${GREEN}[OK] $PASSED passed${NC}  ${RED}[!!] $FAILED failed${NC}  ${YELLOW}[--] $SKIPPED skipped${NC}"
echo -e "${BLUE}==========================================================${NC}"

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}FIX FAILURES BEFORE PUSHING!${NC}"
    exit 1
fi

echo -e "${GREEN}All tests passed -- safe to push${NC}"
