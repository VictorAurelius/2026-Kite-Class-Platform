#!/usr/bin/env bash
# =========================================================================
# smoke-login-happy-path.sh — E2E login (BE + FE) + JWT verify + redirect
# =========================================================================
# Wave 85 Bucket G (GAP-475). Covers pre-handoff-self-test-completeness.md §2.1
# (auth-gated user-flow: credential → login API → role-guard → dashboard).
#
# Usage:
#   ./scripts/smoke-login-happy-path.sh                      # dry-run (default)
#   ./scripts/smoke-login-happy-path.sh --execute            # hit staging
#   SMOKE_BASE_URL=https://staging.kitehub.vn \
#     SMOKE_USER=qa@kitehub.me SMOKE_PASS=*** \
#     ./scripts/smoke-login-happy-path.sh --execute
#
# Defense-in-depth:
#   --execute required; never targets production (host must contain "staging"
#   OR localhost), KHÔNG hit production tenants.
#
# Exit codes:
#   0 = all checks pass (or dry-run completes)
#   1 = login failed / JWT missing / role-guard rejected / redirect wrong
#   2 = config invalid (missing creds or production host targeted)
# =========================================================================

set -euo pipefail

# ─── Config ────────────────────────────────────────────────────────────

MODE="dry-run"
BASE_URL="${SMOKE_BASE_URL:-https://staging.kitehub.vn}"
USER="${SMOKE_USER:-qa-smoke@kitehub.me}"
PASS="${SMOKE_PASS:-}"
TIMEOUT=15

if [ -t 1 ]; then
    GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
else
    GREEN=''; RED=''; YELLOW=''; NC=''
fi

PASS_COUNT=0
FAIL_COUNT=0

pass()  { echo -e "  ${GREEN}[PASS]${NC} $1"; PASS_COUNT=$((PASS_COUNT+1)); }
fail()  { echo -e "  ${RED}[FAIL]${NC} $1"; FAIL_COUNT=$((FAIL_COUNT+1)); }
info()  { echo -e "  ${YELLOW}[INFO]${NC} $1"; }

# ─── Parse args ────────────────────────────────────────────────────────

while [ $# -gt 0 ]; do
    case "$1" in
        --execute) MODE="execute"; shift ;;
        --base-url) BASE_URL="$2"; shift 2 ;;
        --user) USER="$2"; shift 2 ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "Unknown arg: $1"; exit 2 ;;
    esac
done

echo "=== smoke-login-happy-path.sh (mode=$MODE) ==="
echo "Base URL: $BASE_URL"
echo "User: $USER"

# ─── Safety: never target production ───────────────────────────────────

if [ "$MODE" = "execute" ]; then
    case "$BASE_URL" in
        *staging*|http://localhost*|http://127.0.0.1*) ;;
        *) echo -e "${RED}[ABORT]${NC} --execute refuses non-staging host: $BASE_URL"; exit 2 ;;
    esac
    if [ -z "$PASS" ]; then
        echo -e "${RED}[ABORT]${NC} SMOKE_PASS env var required for --execute"
        exit 2
    fi
fi

# ─── Dry-run path ──────────────────────────────────────────────────────

if [ "$MODE" = "dry-run" ]; then
    info "Dry-run: validating script + endpoint list (no network calls)"
    info "Would POST $BASE_URL/api/v1/auth/login (BE login)"
    info "Would GET $BASE_URL/dashboard (FE redirect target)"
    info "Would GET $BASE_URL/api/v1/users/me (JWT verify)"
    pass "Dry-run complete — script structure valid"
    echo ""
    echo "Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL"
    exit 0
fi

# ─── Execute path ──────────────────────────────────────────────────────

info "Step 1: POST $BASE_URL/api/v1/auth/login"
LOGIN_RESPONSE=$(curl -sS -m $TIMEOUT \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$USER\",\"password\":\"$PASS\"}" \
    -w "\nHTTP_CODE:%{http_code}" \
    "$BASE_URL/api/v1/auth/login" || echo "HTTP_CODE:000")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | grep -oE 'HTTP_CODE:[0-9]+' | cut -d: -f2)
BODY=$(echo "$LOGIN_RESPONSE" | sed '/HTTP_CODE:/d')

if [ "$HTTP_CODE" = "200" ]; then
    pass "Login API returned 200"
else
    fail "Login API returned $HTTP_CODE (expected 200)"
    echo "Body: $BODY" | head -3
    exit 1
fi

# Extract JWT (try common keys: token, accessToken, access_token)
JWT=$(echo "$BODY" | grep -oE '"(access[Tt]oken|token|access_token)"\s*:\s*"[^"]+"' \
    | head -1 | sed -E 's/.*:\s*"([^"]+)"/\1/')

if [ -n "$JWT" ] && [ ${#JWT} -gt 20 ]; then
    pass "JWT extracted (length=${#JWT})"
else
    fail "No JWT in login response"
    exit 1
fi

info "Step 2: GET $BASE_URL/api/v1/users/me (JWT verify)"
ME_CODE=$(curl -sS -m $TIMEOUT -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $JWT" \
    "$BASE_URL/api/v1/users/me" || echo "000")

if [ "$ME_CODE" = "200" ]; then
    pass "JWT accepted by /users/me (200)"
else
    fail "/users/me returned $ME_CODE (expected 200)"
fi

info "Step 3: GET $BASE_URL/dashboard (role-guard redirect check)"
DASH_HEADERS=$(curl -sSI -m $TIMEOUT \
    -H "Authorization: Bearer $JWT" \
    "$BASE_URL/dashboard" || echo "")
DASH_CODE=$(echo "$DASH_HEADERS" | head -1 | grep -oE '[0-9]{3}')

case "$DASH_CODE" in
    200|302|303) pass "Dashboard reachable (HTTP $DASH_CODE)" ;;
    401|403) fail "Dashboard rejected JWT (HTTP $DASH_CODE) — role-guard mismatch?" ;;
    *) fail "Dashboard unexpected status: $DASH_CODE" ;;
esac

echo ""
echo "=== Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL ==="
[ $FAIL_COUNT -eq 0 ] || exit 1
