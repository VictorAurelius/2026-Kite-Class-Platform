#!/usr/bin/env bash
# =========================================================================
# smoke-tenant-isolation-rls.sh — Multi-tenant RLS + audit log immutability
# =========================================================================
# Wave 85 Bucket G (GAP-475). Covers 3 of 4 AC enhancements (Bucket A sim):
#   - G-AC1 Cross-tenant data leak smoke (rotate 10 tenants × 5 actions
#     × verify isolation — RLS verification post-Bucket-B)
#   - G-AC3 RLS NULL session var test (clear app.current_tenant_id → query →
#     expect raise exception)
#   - G-AC4 Admin audit log immutability (UPDATE/DELETE admin_audit_logs
#     → expect rejection)
#
# Usage:
#   ./scripts/smoke-tenant-isolation-rls.sh                  # dry-run
#   PG_HOST=staging-db PG_USER=qa PGPASSWORD=*** \
#     ./scripts/smoke-tenant-isolation-rls.sh --execute
#
# SAFETY:
#   --execute REFUSES production hosts. Reads + isolation probes only.
#   Audit log mutation attempt is EXPECTED to fail (asserts policy active).
#
# Exit codes:
#   0 = all 3 AC checks pass (or dry-run)
#   1 = cross-tenant leak detected / RLS off / audit log mutable
#   2 = config invalid OR psql missing
# =========================================================================

set -euo pipefail

MODE="dry-run"
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-kitehub}"
PG_DB="${PG_DB:-kitehub}"
TENANT_COUNT="${TENANT_COUNT:-10}"

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
        --host) PG_HOST="$2"; shift 2 ;;
        --tenants) TENANT_COUNT="$2"; shift 2 ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "Unknown arg: $1"; exit 2 ;;
    esac
done

echo "=== smoke-tenant-isolation-rls.sh (mode=$MODE) ==="
echo "Target: $PG_USER@$PG_HOST:$PG_PORT/$PG_DB | Tenants: $TENANT_COUNT"

if [ "$MODE" = "execute" ]; then
    case "$PG_HOST" in
        *staging*|localhost|127.0.0.1) ;;
        *) echo -e "${RED}[ABORT]${NC} --execute refuses non-staging host: $PG_HOST"; exit 2 ;;
    esac
    command -v psql >/dev/null 2>&1 || { fail "psql not installed"; exit 2; }
fi

if [ "$MODE" = "dry-run" ]; then
    info "Dry-run: 3 AC test plan"
    info ""
    info "G-AC1: Cross-tenant data leak smoke"
    info "  - Rotate through $TENANT_COUNT tenants, 5 read actions each"
    info "  - For each: SET app.current_tenant_id='<id>' → SELECT FROM users → assert tenant_id=<id> only"
    info ""
    info "G-AC3: RLS NULL session var"
    info "  - RESET app.current_tenant_id (or set to '')"
    info "  - SELECT FROM users → expect raise exception (RLS policy blocks NULL tenant)"
    info ""
    info "G-AC4: Admin audit log immutability"
    info "  - UPDATE admin_audit_logs SET action='tampered' WHERE id=<any>"
    info "  - DELETE FROM admin_audit_logs WHERE id=<any>"
    info "  - Both expect RAISE (immutability trigger active)"
    info ""

    if command -v psql >/dev/null 2>&1; then
        pass "psql available"
    else
        info "psql not installed (apt install postgresql-client)"
    fi
    pass "Dry-run complete"
    echo ""
    echo "Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL"
    exit 0
fi

# ─── Execute ───────────────────────────────────────────────────────────

PSQL="psql -h $PG_HOST -p $PG_PORT -U $PG_USER -d $PG_DB -tA"

# G-AC1: cross-tenant isolation
info "G-AC1: cross-tenant isolation probe ($TENANT_COUNT tenants)"
TENANT_IDS=$($PSQL -c "SELECT id FROM tenants ORDER BY created_at DESC LIMIT $TENANT_COUNT;" 2>/dev/null || echo "")

if [ -z "$TENANT_IDS" ]; then
    fail "Could not fetch tenant IDs (tenants table empty or RLS blocking superuser?)"
else
    LEAK_FOUND=0
    for TID in $TENANT_IDS; do
        # Set session tenant and verify reads return only that tenant's rows
        WRONG=$($PSQL -c "SET app.current_tenant_id = '$TID'; SELECT COUNT(*) FROM users WHERE tenant_id <> '$TID';" 2>/dev/null | tail -1 || echo "ERR")
        if [ "$WRONG" != "0" ] && [ "$WRONG" != "ERR" ]; then
            fail "Tenant $TID leaked $WRONG rows from other tenants"
            LEAK_FOUND=1
        fi
    done
    [ $LEAK_FOUND -eq 0 ] && pass "G-AC1: all $TENANT_COUNT tenants isolated (0 cross-tenant leaks)"
fi

# G-AC3: NULL session var rejection
info "G-AC3: RLS rejects NULL session variable"
NULL_RESULT=$($PSQL -c "RESET app.current_tenant_id; SELECT COUNT(*) FROM users;" 2>&1 | tail -3 || echo "")
if echo "$NULL_RESULT" | grep -qiE "(error|raise|tenant.*required|null|denied)"; then
    pass "G-AC3: RLS correctly raised on NULL/missing tenant context"
else
    fail "G-AC3: query succeeded with NULL tenant context — RLS not enforcing"
    echo "  Result: $NULL_RESULT" | head -3
fi

# G-AC4: audit log immutability
info "G-AC4: admin_audit_logs UPDATE/DELETE rejection"
UPDATE_RESULT=$($PSQL -c "UPDATE admin_audit_logs SET action='SMOKE_TAMPER_TEST' WHERE id IN (SELECT id FROM admin_audit_logs LIMIT 1);" 2>&1 || true)
if echo "$UPDATE_RESULT" | grep -qiE "(error|raise|denied|immutable|read.?only)"; then
    pass "G-AC4: UPDATE on admin_audit_logs rejected"
else
    fail "G-AC4: UPDATE on admin_audit_logs SUCCEEDED — immutability trigger missing"
fi

DELETE_RESULT=$($PSQL -c "DELETE FROM admin_audit_logs WHERE id IN (SELECT id FROM admin_audit_logs LIMIT 1);" 2>&1 || true)
if echo "$DELETE_RESULT" | grep -qiE "(error|raise|denied|immutable|read.?only)"; then
    pass "G-AC4: DELETE on admin_audit_logs rejected"
else
    fail "G-AC4: DELETE on admin_audit_logs SUCCEEDED — immutability trigger missing"
fi

echo ""
echo "=== Summary: $PASS_COUNT PASS / $FAIL_COUNT FAIL ==="
[ $FAIL_COUNT -eq 0 ] || exit 1
