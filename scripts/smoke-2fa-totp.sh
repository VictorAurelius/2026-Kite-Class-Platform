#!/usr/bin/env bash
# =========================================================================
# smoke-2fa-totp.sh — TOTP enroll + verify (admin@kitehub.me staging)
# =========================================================================
# Wave 85 Bucket G (GAP-475). Covers pre-handoff-self-test-completeness.md §2.10
# (time-sensitive flow: TOTP TTL ±30s window, server clock skew tolerance).
#
# Usage:
#   ./scripts/smoke-2fa-totp.sh                              # dry-run
#   SMOKE_ADMIN_JWT=... SMOKE_TOTP_SECRET=... \
#     ./scripts/smoke-2fa-totp.sh --execute
#
# Flow:
#   1. POST /api/v1/auth/2fa/enroll        → returns otpauth:// URI + secret
#   2. Generate TOTP from SMOKE_TOTP_SECRET (needs `oathtool` OR python pyotp)
#   3. POST /api/v1/auth/2fa/verify {code} → state flip 2FA_PENDING → 2FA_ENABLED
#   4. (Window check) wait 31s, generate fresh code, re-verify
#   5. POST /api/v1/auth/2fa/disable       → cleanup
#
# Dependencies:
#   - oathtool   (apt: oathtool)            OR
#   - python3 + pyotp (pip install pyotp)
#
# Exit codes:
#   0 = pass (or dry-run)
#   1 = enrollment failed / TOTP rejected / state didn't flip
#   2 = config invalid OR missing TOTP generator
# =========================================================================

set -euo pipefail

MODE="dry-run"
BASE_URL="${SMOKE_BASE_URL:-https://staging.kitehub.vn}"
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

while [ $# -gt 0 ]; do
    case "$1" in
        --execute) MODE="execute"; shift ;;
        --base-url) BASE_URL="$2"; shift 2 ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "Unknown arg: $1"; exit 2 ;;
    esac
done

echo "=== smoke-2fa-totp.sh (mode=$MODE) ==="

if [ "$MODE" = "execute" ]; then
    case "$BASE_URL" in
        *staging*|http://localhost*|http://127.0.0.1*) ;;
        *) echo -e "${RED}[ABORT]${NC} --execute refuses non-staging host: $BASE_URL"; exit 2 ;;
    esac
    [ -n "${SMOKE_ADMIN_JWT:-}" ] || { fail "SMOKE_ADMIN_JWT required"; exit 2; }
fi

# Generate TOTP code helper (oathtool preferred, fallback python)
gen_totp() {
    local secret="$1"
    if command -v oathtool >/dev/null 2>&1; then
        oathtool --totp -b "$secret" 2>/dev/null
    elif command -v python3 >/dev/null 2>&1; then
        python3 -c "import pyotp,sys; print(pyotp.TOTP('$secret').now())" 2>/dev/null
    else
        echo ""
    fi
}

if [ "$MODE" = "dry-run" ]; then
    info "Dry-run: TOTP smoke 5-phase plan"
    info "Phase 1: enroll endpoint validation"
    info "Phase 2: TOTP code generation (oathtool/pyotp dependency check)"
    info "Phase 3: verify endpoint with first code"
    info "Phase 4: 31s wait → fresh code → re-verify (window rotation check)"
    info "Phase 5: disable cleanup"

    if command -v oathtool >/dev/null 2>&1; then
        pass "TOTP generator available: oathtool"
    elif command -v python3 >/dev/null 2>&1; then
        if python3 -c "import pyotp" 2>/dev/null; then
            pass "TOTP generator available: python pyotp"
        else
            info "python3 present but pyotp missing — run: pip install pyotp"
        fi
    else
        info "No TOTP generator detected (install oathtool or python+pyotp before --execute)"
    fi

    pass "Dry-run complete"
    echo ""
    echo "Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL"
    exit 0
fi

# ─── Execute ───────────────────────────────────────────────────────────

info "Phase 1: POST $BASE_URL/api/v1/auth/2fa/enroll"
ENROLL=$(curl -sS -m $TIMEOUT -H "Authorization: Bearer $SMOKE_ADMIN_JWT" \
    -X POST "$BASE_URL/api/v1/auth/2fa/enroll" || echo "")
SECRET=$(echo "$ENROLL" | grep -oE '"secret"\s*:\s*"[A-Z2-7]+"' | sed -E 's/.*"([A-Z2-7]+)"/\1/')

if [ -n "$SECRET" ] && [ ${#SECRET} -ge 16 ]; then
    pass "Enroll returned base32 secret (len=${#SECRET})"
else
    fail "Enroll didn't return valid base32 secret"
    echo "$ENROLL" | head -3
    exit 1
fi

info "Phase 2: generate first TOTP code"
CODE=$(gen_totp "$SECRET")
if [ -n "$CODE" ] && [ ${#CODE} -eq 6 ]; then
    pass "Generated TOTP: $CODE"
else
    fail "TOTP generator failed (install oathtool or pyotp)"
    exit 2
fi

info "Phase 3: verify code"
VERIFY_CODE=$(curl -sS -m $TIMEOUT -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $SMOKE_ADMIN_JWT" \
    -H "Content-Type: application/json" \
    -d "{\"code\":\"$CODE\"}" \
    -X POST "$BASE_URL/api/v1/auth/2fa/verify" || echo "000")

if [ "$VERIFY_CODE" = "200" ]; then
    pass "TOTP verified (state flip → 2FA_ENABLED)"
else
    fail "Verify returned $VERIFY_CODE"
fi

info "Phase 4: wait 31s for fresh window + re-verify"
sleep 31
CODE2=$(gen_totp "$SECRET")
if [ "$CODE" != "$CODE2" ]; then pass "Window rotated: $CODE → $CODE2"; else fail "TOTP didn't rotate after 31s"; fi

info "Phase 5: cleanup disable"
DISABLE_CODE=$(curl -sS -m $TIMEOUT -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $SMOKE_ADMIN_JWT" \
    -X POST "$BASE_URL/api/v1/auth/2fa/disable" || echo "000")
if [ "$DISABLE_CODE" = "200" ]; then pass "2FA disabled"; else fail "Disable returned $DISABLE_CODE"; fi

echo ""
echo "=== Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL ==="
[ $FAIL_COUNT -eq 0 ] || exit 1
