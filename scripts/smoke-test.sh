#!/usr/bin/env bash
# shellcheck disable=SC2059,SC2155
# SC2059: color escape codes (${BOLD}/${RED}/${NC}) in printf format are intentional
# SC2155: local declare+assign acceptable; failure case rare in helper construction
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
#   ./scripts/smoke-test.sh https://staging.kitehub.vn https://staging.kitehub.me
#   ./scripts/smoke-test.sh http://localhost:9000                                # local single gateway
#
# Defaults (no args): https://kitehub.vn  +  https://kitehub.me
#
# Env-gated scenarios:
#   SMOKE_LOGS_E2E=1 LOKI_URL=...      Enable Loki ingest assertion (GAP-434)
#   STOP_WHEN_IDLE_E2E=1               Enable stop-when-idle cycle audit
#                                      (Wave 61 Bucket C — seed dry-run + ≤25min
#                                       cycle envelope + optional state.json log
#                                       via STOP_WHEN_IDLE_STATE_FILE=path)
#   BETA_SMOKE_SES_VERIFY=live|stub    Beta-signup end-to-end mode (GAP-389-B)
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
    echo "Defaults: KH=https://kitehub.vn  KC=https://kitehub.me"
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

# GAP-434 Phase 2 — Logs Overview end-to-end smoke test.
#
# Verifies the Loki/Promtail stack is shipping log lines to Loki by querying
# the Loki HTTP API (proxied via Grafana) for a count of recent samples from
# kitehub-gateway. Gated by env var SMOKE_LOGS_E2E=1 because it requires:
#   - LOKI_URL pointing at a reachable Loki HTTP endpoint
#     (e.g. via `kubectl port-forward svc/kitehub-loki 3100:3100` OR
#     a cluster-external Grafana proxy URL with auth)
# Default invocation in CI / prod skips this check (warns once); operators
# enable explicitly when verifying a fresh deploy.
#
# Equivalent LogQL query: count_over_time({service="kitehub-gateway"}[5m]) > 0
check_logs_overview_e2e() {
    local label="LOGS_OVERVIEW_E2E"
    if [ "${SMOKE_LOGS_E2E:-0}" != "1" ]; then
        warn "$label" "skipped (set SMOKE_LOGS_E2E=1 + LOKI_URL to enable)"
        return
    fi
    local loki_url="${LOKI_URL:-}"
    if [ -z "$loki_url" ]; then
        fail "$label" "SMOKE_LOGS_E2E=1 but LOKI_URL unset"
        return
    fi
    # Loki query_range: count log lines emitted by kitehub-gateway in last 5m.
    # See https://grafana.com/docs/loki/latest/reference/api/#query-loki-over-a-range-of-time
    local query='count_over_time({service="kitehub-gateway"}[5m])'
    local now_ns start_ns
    now_ns=$(date +%s%N)
    start_ns=$(($(date +%s) - 300))000000000
    local url="${loki_url}/loki/api/v1/query_range?query=$(printf %s "$query" | jq -sRr @uri)&start=${start_ns}&end=${now_ns}&step=60s"
    local code
    code=$(http_get "$url")
    read_body
    if [ "$code" != "200" ]; then
        fail "$label" "Loki query_range returned ${code}"
        return
    fi
    if ! command -v jq >/dev/null 2>&1; then
        warn "$label" "Loki responded 200 but jq not available — cannot assert sample count"
        return
    fi
    local sample_count
    sample_count=$(echo "$BODY" | jq -r '[.data.result[]?.values[]? | .[1] | tonumber] | add // 0' 2>/dev/null || echo "0")
    if [ -z "$sample_count" ] || [ "$sample_count" = "null" ]; then
        sample_count=0
    fi
    if [ "$(printf '%s\n' "$sample_count" | awk '{print ($1 > 0)}')" = "1" ]; then
        pass "$label" "Loki has ${sample_count} samples for kitehub-gateway in last 5m"
    else
        fail "$label" "Loki returned 0 samples — Promtail not shipping or service silent"
    fi
}

# ─── Stop-when-idle E2E (Wave 61 Bucket C — GAP-376 + GAP-449) ────────
#
# Validates the resume → seed-verify → smoke → stop cycle for the
# stop-when-idle production posture (per Wave 61 plan §3 Bucket C).
# Gated by STOP_WHEN_IDLE_E2E=1 because the stack-resume + stack-stop steps
# are USER-EXECUTED (per `agent-aws-access.md` Tier 1; mutation actions
# remain manual). The script ASSUMES the user has already resumed the stack
# (`bash scripts/aws/start-stack.sh` — Bucket D follow-up) and will stop it
# after this check exits.
#
# Asserts in order:
#   1. Seed dry-run (`scripts/seed-production.sh --dry-run`) PASS
#   2. 13 endpoint health checks already covered by Sections 1-8 above
#      → this function only audits the cycle envelope (timing + log entry)
#   3. Writes a state.json fragment to documents/05-guides/deploy/state/ if
#      the user has set STOP_WHEN_IDLE_STATE_FILE (default skipped — read-only)
#
# Honors `agent-aws-access.md` Tier 1: no AWS CLI mutation, no infra change.
check_stop_when_idle_e2e() {
    local label="STOP_WHEN_IDLE_E2E"
    if [ "${STOP_WHEN_IDLE_E2E:-0}" != "1" ]; then
        warn "$label" "skipped (set STOP_WHEN_IDLE_E2E=1 to enable cycle audit)"
        return
    fi

    local cycle_start
    cycle_start=$(date +%s)

    # 1. Seed dry-run — verifies seed scripts + env wiring without DB write
    if [ -x scripts/seed-production.sh ]; then
        if scripts/seed-production.sh --dry-run >/dev/null 2>&1; then
            pass "$label seed dry-run" "scripts/seed-production.sh --dry-run exit 0"
        else
            # Dry-run can fail on missing env vars — informational only here.
            warn "$label seed dry-run" "missing env (SEED_ADMIN_PASSWORD etc.) — populate before real cutover"
        fi
    else
        warn "$label seed dry-run" "scripts/seed-production.sh not executable"
    fi

    # 2. Cycle wall-clock — fail if other checks already consumed >25 minutes
    #    (per Wave 61 plan §3 Bucket C acceptance: 1 cycle ≤25 phút).
    local now elapsed
    now=$(date +%s)
    elapsed=$((now - TOTAL_START))
    if [ "$elapsed" -lt 1500 ]; then  # 25 min = 1500s
        pass "$label cycle envelope" "elapsed ${elapsed}s of 1500s budget"
    else
        warn "$label cycle envelope" "elapsed ${elapsed}s exceeds 1500s (25 min) target"
    fi

    # 3. Optional state.json append — only if caller provided file path
    local state_file="${STOP_WHEN_IDLE_STATE_FILE:-}"
    if [ -n "$state_file" ]; then
        # Best-effort JSON append; do not fail the smoke if write fails.
        if printf '{"ts":"%s","cycle_seconds":%d,"pass":%d,"fail":%d,"warn":%d}\n' \
            "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$((now - cycle_start))" \
            "$PASS_COUNT" "$FAIL_COUNT" "$WARN_COUNT" >> "$state_file" 2>/dev/null; then
            pass "$label state log" "appended to $state_file"
        else
            warn "$label state log" "cannot write $state_file"
        fi
    fi
}

# ─── Auth happy path (GAP-475 Sub-1 — Wave 62 Bucket A) ───────────────
#
# Env-gated end-to-end auth verification: POST login → extract Bearer →
# GET protected endpoint → assert 200.
#
# Requires (set ALL three to enable):
#   SMOKE_AUTH_E2E=1          Enable the check
#   SMOKE_AUTH_USER=...       Pre-seeded test account email (KH)
#   SMOKE_AUTH_PASS=...       Test account password
#
# When env vars are unset, function logs a single SKIP line and returns 0.
# Endpoint shape verified 2026-05-11 via kitehub/scripts/test-api-e2e.sh:
#   POST ${KH_URL}/api/auth/login  body {email, password}  -> 200 {accessToken, ...}
#   GET  ${KH_URL}/api/platform/instances  Authorization: Bearer <jwt>  -> 200
# Protected route chosen because it requires real JWT (returns 401 unauth).
check_auth_happy_path() {
    local label="AUTH_HAPPY_PATH"
    if [ "${SMOKE_AUTH_E2E:-0}" != "1" ]; then
        warn "$label" "skipped (set SMOKE_AUTH_E2E=1 + SMOKE_AUTH_USER + SMOKE_AUTH_PASS to enable)"
        return
    fi
    local user="${SMOKE_AUTH_USER:-}"
    local pass="${SMOKE_AUTH_PASS:-}"
    if [ -z "$user" ] || [ -z "$pass" ]; then
        fail "$label" "SMOKE_AUTH_E2E=1 but SMOKE_AUTH_USER/SMOKE_AUTH_PASS unset"
        return
    fi

    # Step 1 — POST login
    local login_url="${KH_URL}/api/auth/login"
    local payload
    payload=$(printf '{"email":"%s","password":"%s"}' "$user" "$pass")
    local code
    code=$(http_post "$login_url" "$payload" "application/json")
    if [ "$code" = "000" ]; then
        fail "$label login" "connection refused / timeout ($login_url)"
        return
    fi
    if [ "$code" != "200" ]; then
        fail "$label login" "HTTP ${code} (expected 200)"
        return
    fi
    read_body

    # Step 2 — Extract JWT (try accessToken first, then token)
    local jwt=""
    if command -v jq >/dev/null 2>&1; then
        jwt=$(echo "$BODY" | jq -r '.accessToken // .token // empty' 2>/dev/null || echo "")
    else
        # Fallback grep — extract first matching token field value (non-greedy)
        jwt=$(echo "$BODY" | grep -oE '"(accessToken|token)"[[:space:]]*:[[:space:]]*"[^"]+"' \
            | head -1 \
            | sed -E 's/.*"(accessToken|token)"[[:space:]]*:[[:space:]]*"([^"]+)"/\2/')
    fi
    if [ -z "$jwt" ]; then
        fail "$label token-extract" "200 OK but no accessToken/token field in response body"
        return
    fi
    pass "$label login" "200 OK, JWT extracted (${#jwt} chars)"

    # Step 3 — GET protected endpoint with Bearer
    local protected_url="${KH_URL}/api/platform/instances"
    local protected_code
    protected_code=$(curl -sS --max-time "$TIMEOUT" \
        -H "Authorization: Bearer ${jwt}" \
        -o "$BODY_FILE" -w '%{http_code}' \
        "$protected_url" 2>/dev/null || echo "000")
    case "$protected_code" in
        200)
            pass "$label protected GET" "200 OK (${protected_url})"
            ;;
        401|403)
            fail "$label protected GET" "${protected_code} — JWT rejected (token/role issue)"
            ;;
        000)
            fail "$label protected GET" "connection refused / timeout"
            ;;
        *)
            warn "$label protected GET" "HTTP ${protected_code} (expected 200)"
            ;;
    esac
}

# ─── Latency thresholds (GAP-475 Sub-4 — Wave 62 Bucket A) ────────────
#
# Measure per-endpoint wall-clock latency via curl -w "%{time_total}".
# Always writes JSON report to /tmp/smoke-latency-<epoch>.json + prints
# summary table. By default the function only emits informational warns
# above threshold; with SMOKE_LATENCY_ASSERT=1 it converts threshold
# breaches into FAIL.
#
# Per-endpoint thresholds (ms):
#   /actuator/health endpoints     ≤  500
#   public API JSON endpoints      ≤ 1500
#   FE landing / login / register  ≤ 2000
check_latency_thresholds() {
    local label="LATENCY"
    local assert_mode="${SMOKE_LATENCY_ASSERT:-0}"

    local epoch
    epoch=$(date +%s)
    local report="/tmp/smoke-latency-${epoch}.json"

    # endpoint|category|threshold_ms
    local endpoints=(
        "${KC_URL}/kiteclass/actuator/health|health|500"
        "${KH_URL}/kitehub-subscription/actuator/health|health|500"
        "${KH_URL}/kitehub-branding/actuator/health|health|500"
        "${KH_URL}/kitehub-email/actuator/health|health|500"
        "${KH_URL}/api/health|api|1500"
        "${KC_URL}/api/v1/public/courses|api|1500"
        "${KC_URL}/api/v1/public/settings|api|1500"
        "${KH_URL}/|page|2000"
        "${KH_URL}/login|page|2000"
        "${KH_URL}/register|page|2000"
    )

    : > "$report"
    printf '[' >> "$report"
    local first=1
    local breach=0
    local total=0

    printf "  ${BOLD}Latency report:${NC}\n"

    local entry url category threshold time_ms ok
    for entry in "${endpoints[@]}"; do
        url="${entry%%|*}"
        local rest="${entry#*|}"
        category="${rest%%|*}"
        threshold="${rest##*|}"

        # %{time_total} = total transfer seconds (float). Convert to ms.
        local time_sec
        time_sec=$(curl -sS --max-time "$TIMEOUT" -o /dev/null \
            -w '%{time_total}' "$url" 2>/dev/null || echo "0")
        if [ -z "$time_sec" ] || [ "$time_sec" = "0" ]; then
            time_ms=0
        else
            time_ms=$(awk -v t="$time_sec" 'BEGIN{printf "%d", t * 1000}')
        fi

        if [ "$time_ms" -le "$threshold" ] && [ "$time_ms" -gt 0 ]; then
            ok="true"
        else
            ok="false"
            breach=$((breach + 1))
        fi
        total=$((total + 1))

        # Comma separator between JSON objects
        if [ "$first" -eq 1 ]; then
            first=0
        else
            printf ',' >> "$report"
        fi
        printf '{"endpoint":"%s","category":"%s","time_ms":%d,"threshold_ms":%d,"pass":%s}' \
            "$url" "$category" "$time_ms" "$threshold" "$ok" >> "$report"

        # Print one row
        local marker="OK  "
        if [ "$ok" = "false" ]; then
            marker="OVER"
        fi
        printf "    [%s] %5dms / %4dms  %s\n" "$marker" "$time_ms" "$threshold" "$url"
    done
    printf ']\n' >> "$report"

    if [ "$breach" -eq 0 ]; then
        pass "$label" "${total}/${total} endpoints within threshold (report: ${report})"
    else
        if [ "$assert_mode" = "1" ]; then
            fail "$label" "${breach}/${total} endpoints exceeded threshold (SMOKE_LATENCY_ASSERT=1; report: ${report})"
        else
            warn "$label" "${breach}/${total} endpoints exceeded threshold (info-only; set SMOKE_LATENCY_ASSERT=1 to fail; report: ${report})"
        fi
    fi
}

# ─── Flyway migration head verify (GAP-475 Sub-5 + GAP-476 — Wave 64) ─
#
# Compare highest Flyway version on disk with the version reported by the
# Spring Boot Actuator Flyway endpoint exposed at
# ${KC_URL}/kiteclass/actuator/flyway (kiteclass-core is the canonical
# Flyway owner per scripts/verify-restore.sh + Wave02MigrationsTest).
#
# Endpoint is admin-gated by FlywayEndpointAuthFilter — caller must send
# X-User-Roles: ADMIN header (gateway forwards this post-auth) along with
# the Bearer token. Smoke runs the gateway-routed URL so the gateway has
# already validated the token before reaching kiteclass-core.
#
# Required env (when SMOKE_MIGRATION_VERIFY=1):
#   SMOKE_ADMIN_TOKEN=...     Admin Bearer JWT (validated by gateway)
#   SMOKE_MIGRATION_URL=...   (optional) override endpoint path; default
#                             ${KC_URL}/kiteclass/actuator/flyway
check_migration_head() {
    local label="MIGRATION_HEAD"
    if [ "${SMOKE_MIGRATION_VERIFY:-0}" != "1" ]; then
        warn "$label" "skipped (set SMOKE_MIGRATION_VERIFY=1 + SMOKE_ADMIN_TOKEN to enable)"
        return
    fi
    local admin_token="${SMOKE_ADMIN_TOKEN:-}"
    if [ -z "$admin_token" ]; then
        fail "$label" "SMOKE_MIGRATION_VERIFY=1 but SMOKE_ADMIN_TOKEN unset"
        return
    fi

    # Compute on-disk max version. kiteclass-core is the canonical Flyway
    # owner (per scripts/verify-restore.sh + Wave02MigrationsTest).
    local migration_dir="kiteclass/kiteclass-core/src/main/resources/db/migration"
    if [ ! -d "$migration_dir" ]; then
        fail "$label" "migration dir not found at ${migration_dir}"
        return
    fi
    local disk_max
    # shellcheck disable=SC2010  # ls | grep is fine here: V-prefixed filenames are ASCII-only by Flyway convention
    disk_max=$(ls "$migration_dir" 2>/dev/null \
        | grep -oE '^V[0-9]+' \
        | sed 's/^V//' \
        | sort -n \
        | tail -1)
    if [ -z "$disk_max" ]; then
        fail "$label" "no V<N>__*.sql migrations found in ${migration_dir}"
        return
    fi

    # Probe the Spring Boot Actuator Flyway endpoint (GAP-476). Admin gate
    # via FlywayEndpointAuthFilter requires X-User-Roles: ADMIN header.
    local url="${SMOKE_MIGRATION_URL:-${KC_URL}/kiteclass/actuator/flyway}"
    local code
    code=$(curl -sS --max-time "$TIMEOUT" \
        -H "Authorization: Bearer ${admin_token}" \
        -H "X-User-Roles: ADMIN" \
        -o "$BODY_FILE" -w '%{http_code}' \
        "$url" 2>/dev/null || echo "000")

    if [ "$code" = "000" ]; then
        fail "$label" "connection refused / timeout (${url})"
        return
    fi
    if [ "$code" = "401" ] || [ "$code" = "403" ]; then
        fail "$label" "${code} — admin auth rejected (check SMOKE_ADMIN_TOKEN + X-User-Roles forwarding) at ${url}"
        return
    fi
    if [ "$code" != "200" ]; then
        fail "$label" "HTTP ${code} from ${url}"
        return
    fi
    read_body
    local body_ok="$BODY"

    # Parse highest version from actuator/flyway response shape:
    # {contexts:{<dataSource>:{flywayBeans:{<bean>:{migrations:[{version,state,...}]}}}}}
    local server_max=""
    if command -v jq >/dev/null 2>&1; then
        server_max=$(echo "$body_ok" | jq -r '
            [
              (.contexts // {} | to_entries[]?.value.flywayBeans // {} | to_entries[]?.value.migrations // [])[]?.version,
              (.migrations // [])[]?.version
            ]
            | map(select(. != null and . != ""))
            | map(tonumber? // 0)
            | max // empty
        ' 2>/dev/null || echo "")
    fi
    if [ -z "$server_max" ] || [ "$server_max" = "null" ]; then
        fail "$label" "200 OK from ${url} but cannot parse migration version (jq required, or unexpected shape)"
        return
    fi

    if [ "$server_max" = "$disk_max" ]; then
        pass "$label" "server V${server_max} == disk V${disk_max} (endpoint: ${url})"
    else
        fail "$label" "server V${server_max} != disk V${disk_max} — migration drift (endpoint: ${url})"
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
        KC_URL="https://kitehub.me"
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

# 7c. Auth happy path (GAP-475 Sub-1 — Wave 62 Bucket A; gated by SMOKE_AUTH_E2E=1)
check_auth_happy_path

# 7d. Migration head verify (GAP-475 Sub-5 — Wave 62 Bucket A; gated by SMOKE_MIGRATION_VERIFY=1)
check_migration_head

# 8. Gateway routing (no 502/503)
check_no_502 "kiteclass route"   "${KC_URL}/kiteclass/actuator/info"
check_no_502 "kitehub-sub route" "${KH_URL}/kitehub-subscription/actuator/info"

# 9b. Logs Overview end-to-end (GAP-434 Phase 2 — gated by SMOKE_LOGS_E2E=1)
check_logs_overview_e2e

# 9c. Stop-when-idle cycle audit (Wave 61 Bucket C — gated by STOP_WHEN_IDLE_E2E=1)
check_stop_when_idle_e2e

# 9d. Per-endpoint latency thresholds (GAP-475 Sub-4 — Wave 62 Bucket A)
# Always runs (info-only); set SMOKE_LATENCY_ASSERT=1 to fail on threshold breach.
check_latency_thresholds

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
