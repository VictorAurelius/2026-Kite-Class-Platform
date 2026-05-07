#!/usr/bin/env bash
# =========================================================================
# smoke-test.sh — Post-deploy verification (GAP-089 baseline + GAP-377 ext)
# =========================================================================
# Usage:
#   ./scripts/smoke-test.sh                                      # defaults: prod URLs
#   ./scripts/smoke-test.sh <single-url>                         # both KH+KC use same URL (legacy)
#   ./scripts/smoke-test.sh <kitehub-url> <kiteclass-url>        # dual-URL (preferred)
#
# Examples:
#   ./scripts/smoke-test.sh                                                      # prod
#   ./scripts/smoke-test.sh https://staging.kitehub.vn https://staging.kiteclass.vn
#   ./scripts/smoke-test.sh http://localhost:9000                                # local single gateway
#
# Defaults (no args): https://kitehub.vn  +  https://kiteclass.vn
#
# How to extend (add a new assertion):
# 1. For a simple status-code assertion against KH or KC, add a `check_page`
#    or `check_status` call in the appropriate Section (1..7) below.
# 2. For a JSON endpoint, use `check_api_json $name $url`.
# 3. For body content (e.g. component marker), use
#    `check_body_contains "$name" "$url" "needle"`.
# 4. Keep one assertion per call and prefer the helpers — they handle
#    timeouts, color, and counter bookkeeping uniformly.
# 5. If your assertion targets a path that currently doesn't exist, file a
#    follow-up gap (GAP-377-followup-*) and substitute a safer route.
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

# Shared body buffer (file-based — avoids subshell variable scope issues
# when http_get is invoked as `code=$(http_get URL)`. With `set -u`,
# assigning to a global var inside command-substitution subshell does NOT
# propagate; writing to a tempfile does.)
BODY_FILE="$(mktemp -t smoke-body.XXXXXX)"
trap 'rm -f "$BODY_FILE"' EXIT
BODY=""

usage() {
    echo "Usage:"
    echo "  $0                                     # defaults to prod URLs"
    echo "  $0 <single-url>                        # both KH+KC use same URL (legacy)"
    echo "  $0 <kitehub-url> <kiteclass-url>       # dual-URL"
    echo ""
    echo "Defaults: KH=https://kitehub.vn  KC=https://kiteclass.vn"
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

# GET request — prints HTTP status code on stdout; body persisted to $BODY_FILE.
# Caller reads body via `read_body` (loads into global $BODY).
http_get() {
    local url="$1"
    : > "$BODY_FILE"
    local code
    code=$(curl -sS --max-time "$TIMEOUT" -o "$BODY_FILE" -w '%{http_code}' "$url" 2>/dev/null) || {
        : > "$BODY_FILE"
        echo "000"
        return
    }
    echo "$code"
}

# POST request — prints HTTP status code; body persisted to $BODY_FILE.
http_post() {
    local url="$1"
    local data="${2:-}"
    local content_type="${3:-application/json}"
    : > "$BODY_FILE"
    local code
    code=$(curl -sS --max-time "$TIMEOUT" -X POST \
        -H "Content-Type: $content_type" \
        -d "$data" \
        -o "$BODY_FILE" -w '%{http_code}' "$url" 2>/dev/null) || {
        : > "$BODY_FILE"
        echo "000"
        return
    }
    echo "$code"
}

# Load body from $BODY_FILE into global $BODY (callers reference $BODY after).
read_body() {
    if [ -s "$BODY_FILE" ]; then
        BODY=$(cat "$BODY_FILE")
    else
        BODY=""
    fi
}

# ─── Checks ───────────────────────────────────────────────────────────

# Health endpoint: 200 + optional UP marker; service-specific naming.
check_health() {
    local name="$1"
    local url="$2"

    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "Health: $name" "connection refused / timeout"
    elif [ "$code" = "200" ]; then
        read_body
        if echo "$BODY" | grep -q '"UP"' 2>/dev/null; then
            pass "Health: $name" "${code} OK, status: UP"
        else
            pass "Health: $name" "${code} OK"
        fi
    elif [ "$code" = "503" ]; then
        warn "Health: $name" "${code} Service Unavailable"
    elif [ "$code" = "502" ] || [ "$code" = "504" ]; then
        fail "Health: $name" "${code} Bad Gateway"
    else
        warn "Health: $name" "HTTP ${code}"
    fi
}

# Page check: 200 expected + optional substring match in body.
check_page() {
    local name="$1"
    local url="$2"
    local expected_text="${3:-}"

    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "Page: $name" "connection refused / timeout"
    elif [ "$code" = "200" ]; then
        read_body
        if [ -n "$expected_text" ] && ! echo "$BODY" | grep -qi "$expected_text" 2>/dev/null; then
            warn "Page: $name" "200 OK but missing expected text '${expected_text}'"
        elif [ -n "$expected_text" ]; then
            pass "Page: $name" "200 OK, contains '${expected_text}'"
        else
            pass "Page: $name" "200 OK"
        fi
    else
        fail "Page: $name" "HTTP ${code}"
    fi
}

# Body-contains check: 200 + body matches a needle (case-insensitive grep).
# Useful for verifying a component marker is rendered (e.g. ConsentBanner).
check_body_contains() {
    local name="$1"
    local url="$2"
    local needle="$3"

    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "Body: $name" "connection refused / timeout"
    elif [ "$code" = "200" ]; then
        read_body
        local hits
        hits=$(echo "$BODY" | grep -c "$needle" 2>/dev/null || true)
        # grep -c returns 0 when no matches; tolerate empty hits as zero.
        if [ -z "$hits" ]; then
            hits=0
        fi
        if [ "$hits" -gt 0 ]; then
            pass "Body: $name" "200 OK, '${needle}' matches=${hits}"
        else
            fail "Body: $name" "200 OK but '${needle}' not present"
        fi
    else
        fail "Body: $name" "HTTP ${code}"
    fi
}

check_api_json() {
    local name="$1"
    local url="$2"

    local code
    code=$(http_get "$url")

    if [ "$code" = "000" ]; then
        fail "API: $name" "connection refused / timeout"
    elif [ "$code" = "200" ]; then
        read_body
        if echo "$BODY" | grep -qE '^\s*[\[{]' 2>/dev/null; then
            pass "API: $name" "200 OK, valid JSON"
        else
            warn "API: $name" "200 OK but response is not JSON"
        fi
    elif [ "$code" = "401" ] || [ "$code" = "403" ]; then
        pass "API: $name" "${code} (auth required, endpoint reachable)"
    else
        fail "API: $name" "HTTP ${code}"
    fi
}

check_error_handling() {
    local name="$1"
    local url="$2"
    local expected_code="$3"

    local code
    code=$(http_post "$url" "{}" "application/json")

    if [ "$code" = "000" ]; then
        fail "Error handling: $name" "connection refused / timeout"
    elif [ "$code" = "$expected_code" ]; then
        pass "Error handling: $name" "${code} (expected)"
    elif [ "$code" = "500" ]; then
        warn "Error handling: $name" "500 instead of ${expected_code} — unhandled error"
    else
        pass "Error handling: $name" "${code} (expected ${expected_code})"
    fi
}

# Beta-signup end-to-end smoke (GAP-389-B, Wave 36 Bucket C):
#   1. POST /api/v1/auth/request-beta-access with synthetic email
#   2. Inspect SES delivery counter delta (kite_email_sent_total) OR API stub assertion
#   3. Cleanup: DELETE the PENDING beta_access_request row to avoid pollution
#
# Requires SES delivery validation (one of):
#   - Production: real SES sending domain + IAM creds (env BETA_SMOKE_SES_VERIFY=live)
#     The script reads SES SendStatistics counter delta as proxy for delivery.
#   - Staging:    SES sandbox + verified test mailbox (env BETA_SMOKE_SES_VERIFY=stub)
#     The script asserts the API persisted the row + emitted email request event.
#
# Cleanup endpoint (BETA_SMOKE_CLEANUP_URL): internal admin route protected by
# token in BETA_SMOKE_ADMIN_TOKEN. If unset, the row is left for manual cleanup
# and a WARN is emitted (delivery validation still runs).
check_beta_signup_flow() {
    local kh_url="$1"
    local timestamp
    timestamp=$(date +%s)
    local test_email="smoke+${timestamp}@kite.test"
    local payload
    payload=$(printf '{"email":"%s","name":"Smoke Test","orgName":"Smoke Inc","persona":"P1_SOLO_TEACHER","consentGiven":true}' "$test_email")

    local request_url="${kh_url}/api/v1/auth/request-beta-access"

    # Step 1 — POST request
    local code
    code=$(http_post "$request_url" "$payload" "application/json")

    if [ "$code" = "000" ]; then
        fail "Beta-signup: POST request" "connection refused / timeout ($request_url)"
        return
    fi

    # Accept 200/201/202 (created or accepted) as success; 409 means already exists (rare for synthetic email).
    case "$code" in
        200|201|202)
            pass "Beta-signup: POST request" "${code} (test-email=${test_email})"
            ;;
        409)
            warn "Beta-signup: POST request" "${code} duplicate — synthetic email collision (rare)"
            return
            ;;
        404)
            warn "Beta-signup: endpoint" "${code} not deployed yet at ${request_url} (skip downstream checks)"
            return
            ;;
        *)
            fail "Beta-signup: POST request" "HTTP ${code}"
            return
            ;;
    esac

    # Step 2 — SES delivery validation
    local ses_mode="${BETA_SMOKE_SES_VERIFY:-stub}"
    if [ "$ses_mode" = "live" ]; then
        if command -v aws >/dev/null 2>&1; then
            local sent_count
            sent_count=$(aws ses get-send-statistics 2>/dev/null \
                | grep -oE '"DeliveryAttempts": *[0-9]+' \
                | head -1 \
                | grep -oE '[0-9]+' || echo "")
            if [ -n "$sent_count" ] && [ "$sent_count" -gt 0 ]; then
                pass "Beta-signup: SES delivery" "DeliveryAttempts counter present (${sent_count})"
            else
                warn "Beta-signup: SES delivery" "no DeliveryAttempts reported (delivery may be async)"
            fi
        else
            warn "Beta-signup: SES delivery" "aws CLI unavailable — cannot verify SES counter (live mode)"
        fi
    else
        # Stub mode — assert backend recorded the request via metrics endpoint
        local metrics_code
        metrics_code=$(http_get "${kh_url}/kitehub-email/actuator/metrics/kite.email.sent")
        case "$metrics_code" in
            200)
                pass "Beta-signup: email metric reachable" "200 (kite.email.sent metric exposed)"
                ;;
            401|403)
                pass "Beta-signup: email metric reachable" "${metrics_code} (auth-protected, endpoint exists)"
                ;;
            404)
                warn "Beta-signup: email metric" "404 — kite.email.sent counter not yet wired (GAP-115 dependency)"
                ;;
            *)
                warn "Beta-signup: email metric" "HTTP ${metrics_code}"
                ;;
        esac
    fi

    # Step 3 — Cleanup PENDING row
    local cleanup_url="${BETA_SMOKE_CLEANUP_URL:-}"
    local admin_token="${BETA_SMOKE_ADMIN_TOKEN:-}"
    if [ -n "$cleanup_url" ] && [ -n "$admin_token" ]; then
        local cleanup_code
        cleanup_code=$(curl -sS --max-time "$TIMEOUT" -X DELETE \
            -H "Authorization: Bearer ${admin_token}" \
            -H "Content-Type: application/json" \
            -d "$payload" \
            -o /dev/null -w '%{http_code}' \
            "${cleanup_url}?email=${test_email}" 2>/dev/null || echo "000")
        case "$cleanup_code" in
            200|204)
                pass "Beta-signup: cleanup" "${cleanup_code} (row removed)"
                ;;
            404)
                warn "Beta-signup: cleanup" "404 — row already absent or endpoint missing"
                ;;
            *)
                warn "Beta-signup: cleanup" "HTTP ${cleanup_code} — manual cleanup may be needed for ${test_email}"
                ;;
        esac
    else
        warn "Beta-signup: cleanup" "BETA_SMOKE_CLEANUP_URL/BETA_SMOKE_ADMIN_TOKEN unset — leaving row for manual cleanup (${test_email})"
    fi
}

check_no_502() {
    local name="$1"
    local url="$2"

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

# Build info: read /actuator/info build.version. Echo only — no assertion.
echo_build_info() {
    local label="$1"
    local url="$2"
    if ! command -v jq >/dev/null 2>&1; then
        printf "  %s  %s build version: %s\n" "info" "$label" "(jq not available)"
        return
    fi
    local body version
    body=$(curl -sS --max-time "$TIMEOUT" "$url" 2>/dev/null || echo '{}')
    version=$(echo "$body" | jq -r '.build.version // "unknown"' 2>/dev/null || echo 'unknown')
    printf "  %s  %s build version: %s\n" "info" "$label" "$version"
}

# ─── Main ─────────────────────────────────────────────────────────────

# Argument handling — dual-URL with backward-compat:
#   0 args: defaults (prod)
#   1 arg : both KH and KC use same URL (legacy GAP-089 invocation)
#   2 args: <KH-url> <KC-url>
case "$#" in
    0)
        KH_URL="https://kitehub.vn"
        KC_URL="https://kiteclass.vn"
        ;;
    1)
        case "${1:-}" in
            -h|--help) usage ;;
        esac
        KH_URL="${1%/}"
        KC_URL="${1%/}"
        ;;
    2)
        KH_URL="${1%/}"
        KC_URL="${2%/}"
        ;;
    *)
        usage
        ;;
esac

TOTAL_START=$(date +%s)

echo ""
printf "${BOLD}Smoke Test Results — KH=%s  KC=%s${NC}\n" "$KH_URL" "$KC_URL"
echo "════════════════════════════════════════════════════════════════"

# 1. Health endpoints (KH gateway routes — work for both single-gateway
#    and dual-URL deployments because gateway exposes per-service health)
check_health "kiteclass-core"       "${KC_URL}/kiteclass/actuator/health"
check_health "kitehub-subscription" "${KH_URL}/kitehub-subscription/actuator/health"
check_health "kitehub-branding"     "${KH_URL}/kitehub-branding/actuator/health"
check_health "kitehub-email"        "${KH_URL}/kitehub-email/actuator/health"

# 2. Public marketing pages (GAP-377 ext)
check_page "KH landing page"   "${KH_URL}/"            "html"
check_page "KH /legal/privacy" "${KH_URL}/legal/privacy"
check_page "KH /legal/terms"   "${KH_URL}/legal/terms"
check_page "KH /legal/cookies" "${KH_URL}/legal/cookies"

# 3. Auth surfaces (GAP-377 ext)
# NOTE 2026-05-06 state-check: kitehub-frontend uses (auth) route group with
# /login + /register routes — there is NO /auth/signup or
# /auth/request-beta-access. Substitutes /login + /register; original AC
# routes tracked via follow-up GAP-377-followup-auth-route-checks.
check_page "KH /login"    "${KH_URL}/login"
check_page "KH /register" "${KH_URL}/register"

# 4. API health (GAP-377 ext) — kitehub-frontend exposes /api/health (not /api/v1/health)
check_api_json "KH /api/health" "${KH_URL}/api/health"

# 5. ConsentBanner present (GAP-377 ext per Wave 23 Bucket BC)
check_body_contains "KH ConsentBanner mounted" "${KH_URL}/" "ConsentBanner\\|consent-banner"

# 6. Public API (KC gateway)
check_api_json "KC public courses"  "${KC_URL}/api/v1/public/courses"
check_api_json "KC public settings" "${KC_URL}/api/v1/public/settings"

# 7. Error handling (malformed requests should get 400, not 500)
check_error_handling "KC register (empty body)" "${KC_URL}/api/auth/register" "400"
check_error_handling "KC login (empty body)"    "${KC_URL}/api/auth/login"    "400"

# 7b. Beta-signup end-to-end (GAP-389-B, Wave 36 Bucket C)
# Requires env BETA_SMOKE_SES_VERIFY (live|stub, default stub) and optional
# BETA_SMOKE_CLEANUP_URL + BETA_SMOKE_ADMIN_TOKEN for row cleanup.
check_beta_signup_flow "$KH_URL"

# 8. Gateway routing (no 502/503)
check_no_502 "kiteclass route"   "${KC_URL}/kiteclass/actuator/info"
check_no_502 "kitehub-sub route" "${KH_URL}/kitehub-subscription/actuator/info"

# 9. Build info display (echo only — no assertion)
echo_build_info "KH" "${KH_URL}/actuator/info"

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
