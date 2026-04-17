#!/usr/bin/env bash
# =========================================================================
# smoke-test.sh — Post-deploy verification (GAP-089)
# =========================================================================
# Usage: ./scripts/smoke-test.sh <base-url>
# Example: ./scripts/smoke-test.sh https://api.kiteclass.com
#          ./scripts/smoke-test.sh http://localhost:9000
#
# Exit codes:
#   0 = all checks pass
#   1 = at least one FAIL
#   2 = no FAILs, but at least one WARN
# =========================================================================

set -euo pipefail

# ─── Config ────────────────────────────────────────────────────────────

TIMEOUT=10          # curl timeout in seconds
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

# Colors (disabled if not a terminal)
if [ -t 1 ]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    YELLOW='\033[1;33m'
    NC='\033[0m'
    BOLD='\033[1m'
else
    GREEN='' RED='' YELLOW='' NC='' BOLD=''
fi

# ─── Helpers ───────────────────────────────────────────────────────────

usage() {
    echo "Usage: $0 <base-url>"
    echo ""
    echo "  base-url  Root URL of the gateway (e.g. http://localhost:9000)"
    echo ""
    echo "Exit codes: 0=pass  1=fail  2=warn-only"
    exit 1
}

pass() {
    PASS_COUNT=$((PASS_COUNT + 1))
    printf "  ${GREEN}PASS${NC}  %s (%s)\n" "$1" "$2"
}

fail() {
    FAIL_COUNT=$((FAIL_COUNT + 1))
    printf "  ${RED}FAIL${NC}  %s (%s)\n" "$1" "$2"
}

warn() {
    WARN_COUNT=$((WARN_COUNT + 1))
    printf "  ${YELLOW}WARN${NC}  %s (%s)\n" "$1" "$2"
}

# GET request — returns HTTP status code; body stored in $BODY
http_get() {
    local url="$1"
    BODY=$(curl -sS --max-time "$TIMEOUT" -o - -w '\n%{http_code}' "$url" 2>/dev/null) || {
        BODY=""
        echo "000"
        return
    }
    local code
    code=$(echo "$BODY" | tail -n1)
    BODY=$(echo "$BODY" | sed '$d')
    echo "$code"
}

# POST request — returns HTTP status code; body stored in $BODY
http_post() {
    local url="$1"
    local data="${2:-}"
    local content_type="${3:-application/json}"
    BODY=$(curl -sS --max-time "$TIMEOUT" -X POST \
        -H "Content-Type: $content_type" \
        -d "$data" \
        -o - -w '\n%{http_code}' "$url" 2>/dev/null) || {
        BODY=""
        echo "000"
        return
    }
    local code
    code=$(echo "$BODY" | tail -n1)
    BODY=$(echo "$BODY" | sed '$d')
    echo "$code"
}

# ─── Checks ───────────────────────────────────────────────────────────

check_health() {
    local name="$1"
    local path="$2"
    local url="${BASE_URL}${path}"

    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "Health: $name" "connection refused / timeout"
    elif [ "$code" = "200" ]; then
        # Check for "UP" in body if it's an actuator endpoint
        if echo "$BODY" | grep -q '"UP"' 2>/dev/null; then
            pass "Health: $name" "${code} OK, status: UP"
        else
            pass "Health: $name" "${code} OK"
        fi
    elif [ "$code" = "503" ]; then
        # Service unhealthy but endpoint reachable
        warn "Health: $name" "${code} Service Unavailable"
    elif [ "$code" = "502" ] || [ "$code" = "504" ]; then
        fail "Health: $name" "${code} Bad Gateway"
    else
        warn "Health: $name" "HTTP ${code}"
    fi
}

check_page() {
    local name="$1"
    local path="$2"
    local expected_text="$3"

    local url="${BASE_URL}${path}"
    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "Page: $name" "connection refused / timeout"
    elif [ "$code" = "200" ]; then
        if [ -n "$expected_text" ] && ! echo "$BODY" | grep -qi "$expected_text" 2>/dev/null; then
            warn "Page: $name" "200 OK but missing expected text '${expected_text}'"
        else
            pass "Page: $name" "200 OK, contains '${expected_text}'"
        fi
    else
        fail "Page: $name" "HTTP ${code}"
    fi
}

check_api_json() {
    local name="$1"
    local path="$2"

    local url="${BASE_URL}${path}"
    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "API: $name" "connection refused / timeout"
    elif [ "$code" = "200" ]; then
        # Validate JSON: check for { or [
        if echo "$BODY" | grep -qE '^\s*[\[{]' 2>/dev/null; then
            pass "API: $name" "200 OK, valid JSON"
        else
            warn "API: $name" "200 OK but response is not JSON"
        fi
    elif [ "$code" = "401" ] || [ "$code" = "403" ]; then
        # Auth-protected endpoints — expected if no token
        pass "API: $name" "${code} (auth required, endpoint reachable)"
    else
        fail "API: $name" "HTTP ${code}"
    fi
}

check_error_handling() {
    local name="$1"
    local path="$2"
    local expected_code="$3"

    local url="${BASE_URL}${path}"
    local code
    code=$(http_post "$url" "{}" "application/json")

    if [ "$code" = "000" ]; then
        fail "Error handling: $name" "connection refused / timeout"
    elif [ "$code" = "$expected_code" ]; then
        pass "Error handling: $name" "${code} (expected)"
    elif [ "$code" = "500" ]; then
        warn "Error handling: $name" "500 instead of ${expected_code} — unhandled error"
    else
        # Different code but not 500 — acceptable
        pass "Error handling: $name" "${code} (expected ${expected_code})"
    fi
}

check_no_502() {
    local name="$1"
    local path="$2"

    local url="${BASE_URL}${path}"
    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "Gateway: $name" "connection refused / timeout"
    elif [ "$code" = "502" ] || [ "$code" = "503" ]; then
        fail "Gateway: $name" "${code} — service unreachable behind gateway"
    else
        pass "Gateway: $name" "${code} (route resolves)"
    fi
}

# ─── Main ─────────────────────────────────────────────────────────────

if [ $# -lt 1 ]; then
    usage
fi

BASE_URL="${1%/}"  # strip trailing slash

TOTAL_START=$(date +%s)

echo ""
printf "${BOLD}Smoke Test Results — %s${NC}\n" "$BASE_URL"
echo "════════════════════════════════════════════════════════════════"

# 1. Health endpoints
check_health "kiteclass-core"       "/kiteclass/actuator/health"
check_health "kitehub-subscription" "/kitehub-subscription/actuator/health"
check_health "kitehub-branding"     "/kitehub-branding/actuator/health"
check_health "kitehub-email"        "/kitehub-email/actuator/health"

# 2. Public pages
check_page "landing page" "/" "html"

# 3. Public API
check_api_json "public courses"  "/api/v1/public/courses"
check_api_json "public settings" "/api/v1/public/settings"

# 4. Error handling (malformed requests should get 400, not 500)
check_error_handling "register (empty body)" "/api/auth/register" "400"
check_error_handling "login (empty body)"    "/api/auth/login"    "400"

# 5. Gateway routing (no 502/503)
check_no_502 "kiteclass route"      "/kiteclass/actuator/info"
check_no_502 "kitehub-sub route"    "/kitehub-subscription/actuator/info"

TOTAL=$((PASS_COUNT + FAIL_COUNT + WARN_COUNT))
TOTAL_END=$(date +%s)
DURATION=$((TOTAL_END - TOTAL_START))

echo "════════════════════════════════════════════════════════════════"
printf "  Result: %d/%d PASS" "$PASS_COUNT" "$TOTAL"
[ "$FAIL_COUNT" -gt 0 ] && printf ", ${RED}%d FAIL${NC}" "$FAIL_COUNT"
[ "$WARN_COUNT" -gt 0 ] && printf ", ${YELLOW}%d WARN${NC}" "$WARN_COUNT"
printf " (%ds)\n" "$DURATION"

if [ "$FAIL_COUNT" -gt 0 ]; then
    printf "  Exit: ${RED}1${NC} (has failures)\n"
    echo ""
    exit 1
elif [ "$WARN_COUNT" -gt 0 ]; then
    printf "  Exit: ${YELLOW}2${NC} (warnings only)\n"
    echo ""
    exit 2
else
    printf "  Exit: ${GREEN}0${NC} (all pass)\n"
    echo ""
    exit 0
fi
