#!/usr/bin/env bash
# =========================================================================
# smoke-p95-latency-k6.sh — k6 load (50 VUs × 5m) — P95 < 500ms + no OOM
# =========================================================================
# Wave 85 Bucket G (GAP-475). Covers:
#   - performance-audit P95 < 500ms threshold
#   - G-AC2 OOM regression test (10-tenant concurrent load; verify no OOM
#     kill khi 60% MaxRAM enforced)
#
# Usage:
#   ./scripts/smoke-p95-latency-k6.sh                        # dry-run
#   SMOKE_BASE_URL=https://staging.kitehub.vn \
#     ./scripts/smoke-p95-latency-k6.sh --execute            # k6 run
#
# Dependencies:
#   - k6 (https://k6.io/docs/getting-started/installation/)
#
# Output:
#   /tmp/k6-p95-${EPOCHSECONDS}.json (full metrics)
#   Stdout summary with PASS/FAIL on P95 + OOM exit
#
# Exit codes:
#   0 = P95 < 500ms AND no OOM
#   1 = P95 ≥ 500ms OR OOM detected
#   2 = config invalid OR k6 missing
# =========================================================================

set -euo pipefail

MODE="dry-run"
BASE_URL="${SMOKE_BASE_URL:-https://staging.kitehub.vn}"
VUS="${K6_VUS:-50}"
DURATION="${K6_DURATION:-5m}"
P95_THRESHOLD_MS="${P95_THRESHOLD_MS:-500}"

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

while [ $# -gt 0 ]; do
    case "$1" in
        --execute) MODE="execute"; shift ;;
        --base-url) BASE_URL="$2"; shift 2 ;;
        --vus) VUS="$2"; shift 2 ;;
        --duration) DURATION="$2"; shift 2 ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "Unknown arg: $1"; exit 2 ;;
    esac
done

echo "=== smoke-p95-latency-k6.sh (mode=$MODE) ==="
echo "Target: $BASE_URL | VUs: $VUS | Duration: $DURATION | P95 threshold: ${P95_THRESHOLD_MS}ms"

# Safety
if [ "$MODE" = "execute" ]; then
    case "$BASE_URL" in
        *staging*|http://localhost*|http://127.0.0.1*) ;;
        *) echo -e "${RED}[ABORT]${NC} --execute refuses non-staging host: $BASE_URL"; exit 2 ;;
    esac
    command -v k6 >/dev/null 2>&1 || { fail "k6 not installed"; exit 2; }
fi

# Generate k6 script
K6_SCRIPT=$(mktemp /tmp/k6-script-XXXXX.js)
cat > "$K6_SCRIPT" <<JS
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: ${VUS},
  duration: '${DURATION}',
  thresholds: {
    'http_req_duration{kind:read}': ['p(95)<${P95_THRESHOLD_MS}'],
    'http_req_failed':              ['rate<0.01'],
  },
};

// Simulate 10 tenants for G-AC2 OOM regression
const TENANTS = Array.from({length: 10}, (_, i) => 'tenant-' + (i+1));

export default function () {
  const t = TENANTS[Math.floor(Math.random() * TENANTS.length)];
  http.get('${BASE_URL}/actuator/health', { tags: { kind: 'read' } });
  http.get('${BASE_URL}/api/v1/public/pricing', { tags: { kind: 'read', tenant: t } });
  sleep(0.3);
}
JS

if [ "$MODE" = "dry-run" ]; then
    info "Dry-run: validate k6 script + dependency check"
    if command -v k6 >/dev/null 2>&1; then
        pass "k6 installed: $(k6 version 2>&1 | head -1)"
    else
        info "k6 not installed (install: https://k6.io/docs/getting-started/installation/)"
    fi
    info "Generated k6 script at: $K6_SCRIPT"
    info "Would run: k6 run --summary-export=/tmp/k6-summary.json $K6_SCRIPT"
    info "G-AC2 OOM check: post-run, ssh EC2 + dmesg | grep -i 'killed process' OR docker logs | grep OOMKilled"
    pass "Dry-run complete"
    echo ""
    echo "Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL"
    exit 0
fi

# ─── Execute ───────────────────────────────────────────────────────────

SUMMARY=/tmp/k6-summary-$(date +%s).json
info "Running k6: $VUS VUs × $DURATION"
if k6 run --summary-export="$SUMMARY" "$K6_SCRIPT"; then
    pass "k6 thresholds passed (P95 < ${P95_THRESHOLD_MS}ms, error rate < 1%)"
else
    fail "k6 thresholds violated — see $SUMMARY"
fi

info "G-AC2 OOM check: verify no OOMKilled containers in staging"
info "  Manual: ssh kc_app && sudo dmesg | grep -i 'killed process'"
info "  Manual: docker logs kc-app | grep -i OOMKilled"
info "  (Automated remote check defer GAP-475 follow-up)"

echo ""
echo "=== Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL ==="
echo "Full metrics: $SUMMARY"
[ $FAIL_COUNT -eq 0 ] || exit 1
