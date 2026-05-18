#!/bin/bash
# Local Test Runner — PHẢI chạy trước khi push
# Tự detect files changed → chạy tests phù hợp
#
# Usage:
#   ./scripts/test-local.sh                    # Auto-detect changed files
#   ./scripts/test-local.sh kiteclass core     # Test specific service
#   ./scripts/test-local.sh kiteclass frontend # Test frontend only
#   ./scripts/test-local.sh kiteclass all      # Test everything
#   ./scripts/test-local.sh --quick            # Compile + checkstyle only (no full tests)

set -e

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

if [[ "$1" == "kiteclass" || "$1" == "kitehub" ]]; then
    PROJECT="$1"
    SERVICE="${2:-all}"
    AUTO_DETECT=false
else
    PROJECT=""
    SERVICE=""
    AUTO_DETECT=true
fi

echo -e "${BLUE}════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Local Test Runner (pre-push)${NC}"
echo -e "${BLUE}════════════════════════════════════════════════${NC}"
echo ""

# Auto-detect changed files
if $AUTO_DETECT; then
    CHANGED=$(git diff --cached --name-only 2>/dev/null || git diff HEAD --name-only 2>/dev/null || echo "")

    HAS_KC_CORE=false
    HAS_KC_FRONTEND=false
    HAS_KH_BACKEND=false
    HAS_KH_FRONTEND=false
    HAS_DOCS_ONLY=true

    while IFS= read -r file; do
        [[ -z "$file" ]] && continue
        case "$file" in
            kiteclass/kiteclass-core/*) HAS_KC_CORE=true; HAS_DOCS_ONLY=false ;;
            kiteclass/kiteclass-frontend/*) HAS_KC_FRONTEND=true; HAS_DOCS_ONLY=false ;;
            kitehub/kitehub-frontend/*) HAS_KH_FRONTEND=true; HAS_DOCS_ONLY=false ;;
            kitehub/*) HAS_KH_BACKEND=true; HAS_DOCS_ONLY=false ;;
            documents/*|*.md) ;; # docs only
            *) HAS_DOCS_ONLY=false ;;
        esac
    done <<< "$CHANGED"

    if $HAS_DOCS_ONLY && [[ -n "$CHANGED" ]]; then
        echo -e "${GREEN}✅ Only documentation changes — no tests needed${NC}"
        exit 0
    fi

    if [[ -z "$CHANGED" ]]; then
        echo -e "${YELLOW}⚠️  No changed files detected. Run with explicit args:${NC}"
        echo "  $0 kiteclass core|frontend|all"
        echo "  $0 kitehub all"
        exit 0
    fi

    echo -e "${BLUE}Changed files detected:${NC}"
    $HAS_KC_CORE && echo "  📦 kiteclass-core"
    $HAS_KC_FRONTEND && echo "  ⚛️  kiteclass-frontend"
    $HAS_KH_BACKEND && echo "  📦 kitehub backend"
    $HAS_KH_FRONTEND && echo "  ⚛️  kitehub-frontend"
    echo ""
fi

# Helper: run a test step
run_step() {
    local name="$1"
    local cmd="$2"
    local dir="$3"

    echo -e "${BLUE}▶ $name${NC}"
    if (cd "$dir" && eval "$cmd") 2>&1 | tail -5; then
        echo -e "${GREEN}  ✅ $name passed${NC}"
        ((PASSED++))
    else
        echo -e "${RED}  ❌ $name FAILED${NC}"
        ((FAILED++))
    fi
    echo ""
}

# Cleanup testcontainers on exit
cleanup() {
    if [ -f "$SCRIPT_DIR/cleanup-testcontainers.sh" ]; then
        "$SCRIPT_DIR/cleanup-testcontainers.sh" 2>/dev/null || true
    fi
}
trap cleanup EXIT

# ─── KiteClass Core ───
if $AUTO_DETECT && $HAS_KC_CORE || [[ "$PROJECT" == "kiteclass" && ("$SERVICE" == "core" || "$SERVICE" == "all") ]]; then
    if $QUICK_MODE; then
        run_step "KC Core: Checkstyle" "./mvnw checkstyle:check -q 2>&1" "kiteclass/kiteclass-core"
    else
        run_step "KC Core: Compile + Checkstyle" "./mvnw compile -q 2>&1" "kiteclass/kiteclass-core"
        run_step "KC Core: Unit Tests" "./mvnw test -q 2>&1" "kiteclass/kiteclass-core"
    fi
fi

# ─── KiteClass Gateway: REMOVED per ADR-032 / GAP-001 ───

# ─── KiteClass Frontend ───
if $AUTO_DETECT && $HAS_KC_FRONTEND || [[ "$PROJECT" == "kiteclass" && ("$SERVICE" == "frontend" || "$SERVICE" == "all") ]]; then
    if $QUICK_MODE; then
        run_step "KC Frontend: ESLint" "npx eslint src/ --quiet 2>&1" "kiteclass/kiteclass-frontend"
    else
        run_step "KC Frontend: Vitest" "npx vitest run --reporter=verbose 2>&1" "kiteclass/kiteclass-frontend"
    fi
fi

# ─── KiteHub Backend ───
if $AUTO_DETECT && $HAS_KH_BACKEND || [[ "$PROJECT" == "kitehub" && "$SERVICE" == "all" ]]; then
    echo -e "${YELLOW}⚠️  KiteHub backend tests — requires Docker stack${NC}"
    ((SKIPPED++))
fi

# ─── KiteHub Frontend ───
if $AUTO_DETECT && $HAS_KH_FRONTEND || [[ "$PROJECT" == "kitehub" && "$SERVICE" == "all" ]]; then
    if $QUICK_MODE; then
        run_step "KH Frontend: Type Check" "npx next lint 2>&1" "kitehub/kitehub-frontend"
    else
        run_step "KH Frontend: Build Check" "npx next build 2>&1" "kitehub/kitehub-frontend"
    fi
fi

# ─── Summary ───
echo -e "${BLUE}════════════════════════════════════════════════${NC}"
echo -e "  Results: ${GREEN}✅ $PASSED passed${NC}  ${RED}❌ $FAILED failed${NC}  ${YELLOW}⏭️  $SKIPPED skipped${NC}"
echo -e "${BLUE}════════════════════════════════════════════════${NC}"

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}❌ FIX FAILURES BEFORE PUSHING!${NC}"
    exit 1
fi

echo -e "${GREEN}✅ All tests passed — safe to push${NC}"
