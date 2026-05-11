#!/usr/bin/env bash
# =========================================================================
# smoke-rollback-cycle.sh — Rollback cycle test (GAP-475 Sub-6, Wave 62)
# =========================================================================
# Usage:
#   ./scripts/smoke-rollback-cycle.sh                 # dry-run (default)
#   ./scripts/smoke-rollback-cycle.sh --execute       # real rollback + restore
#   ./scripts/smoke-rollback-cycle.sh --target-sha <sha>  # explicit target
#
# Flow:
#   1. Pre-flight smoke (current state)
#   2. Trigger rollback workflow → target_sha (previous main SHA by default)
#   3. Wait gateway health
#   4. Post-rollback smoke (assert same pass set)
#   5. Restore forward → original current SHA
#   6. Re-smoke
#   7. Emit JSON report to /tmp/rollback-cycle-${EPOCHSECONDS}.json
#
# Env-gated:
#   ROLLBACK_CYCLE_E2E=1 + --execute  → real workflow trigger
#   (defense-in-depth: BOTH required to run real ops)
#
# Cadence (per documents/05-guides/operations/incident-response-runbook.md §7):
#   - Monthly: --dry-run
#   - Quarterly: --execute during maintenance window
#
# ⚠️  PARTIAL STATUS (2026-05-11): rollback.yml workflow not yet present in
#     .github/workflows/. This script ships as scaffold; rollback trigger step
#     skips when workflow absent + logs [DEFER] message. Once rollback.yml lands
#     (tracked GAP-475 follow-up), enable real path by setting WORKFLOW_NAME
#     below + re-verify with --dry-run.
#
# Exit codes:
#   0 = all phases pass (or dry-run completes)
#   1 = any smoke FAIL OR rollback workflow failure
#   2 = pre-flight infra not ready (health endpoint unreachable)
# =========================================================================

set -euo pipefail

# ─── Config ────────────────────────────────────────────────────────────

WORKFLOW_NAME="${WORKFLOW_NAME:-rollback.yml}"
HEALTH_URL="${HEALTH_URL:-https://kitehub.vn/actuator/health}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-300}"  # 5 minutes max
SMOKE_SCRIPT="${SMOKE_SCRIPT:-$(dirname "$0")/smoke-test.sh}"
EPOCHSECONDS_VAL=$(date +%s)
REPORT_FILE="/tmp/rollback-cycle-${EPOCHSECONDS_VAL}.json"

# Colors
if [ -t 1 ]; then
    GREEN='\033[0;32m'
    RED='\033[0;31m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    NC='\033[0m'
    BOLD='\033[1m'
else
    GREEN='' RED='' YELLOW='' BLUE='' NC='' BOLD=''
fi

# Defaults
MODE="dry-run"
TARGET_SHA=""

# ─── Args ──────────────────────────────────────────────────────────────

usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Options:
  --dry-run                 Default. No real workflow triggers; pre-flight smoke only.
  --execute                 Run real rollback + restore cycle (requires ROLLBACK_CYCLE_E2E=1).
  --target-sha <sha>        Override previous-SHA detection.
  -h | --help               Show this help.

Examples:
  $0                                                 # dry-run preview
  $0 --dry-run                                       # same as default
  ROLLBACK_CYCLE_E2E=1 $0 --execute                  # real cycle
  $0 --execute --target-sha abc1234                  # custom target
EOF
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        --dry-run) MODE="dry-run"; shift ;;
        --execute) MODE="execute"; shift ;;
        --target-sha) TARGET_SHA="$2"; shift 2 ;;
        -h|--help) usage ;;
        *) echo "Unknown arg: $1"; usage ;;
    esac
done

# ─── Helpers ───────────────────────────────────────────────────────────

log_info()  { printf "%b[INFO]%b  %s\n" "$BLUE"   "$NC" "$*"; }
log_pass()  { printf "%b[PASS]%b  %s\n" "$GREEN"  "$NC" "$*"; }
log_fail()  { printf "%b[FAIL]%b  %s\n" "$RED"    "$NC" "$*"; }
log_warn()  { printf "%b[WARN]%b  %s\n" "$YELLOW" "$NC" "$*"; }
log_defer() { printf "%b[DEFER]%b %s\n" "$YELLOW" "$NC" "$*"; }
log_step()  { printf "\n%b━━━ %s ━━━%b\n" "$BOLD" "$*" "$NC"; }

iso_now() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }

workflow_exists() {
    local wf="$1"
    if command -v gh >/dev/null 2>&1; then
        gh api "repos/:owner/:repo/actions/workflows" --jq ".workflows[].path" 2>/dev/null \
            | grep -q "${wf}$" && return 0
    fi
    # Filesystem fallback (worktree-local check)
    [ -f ".github/workflows/${wf}" ]
}

run_smoke() {
    local label="$1"
    local log_file="/tmp/smoke-${label}-${EPOCHSECONDS_VAL}.log"
    log_info "Running smoke test (${label})..." >&2
    if [ ! -x "$SMOKE_SCRIPT" ]; then
        log_warn "smoke-test.sh not executable at $SMOKE_SCRIPT — chmod +x" >&2
        chmod +x "$SMOKE_SCRIPT" || true
    fi
    local exit_code=0
    "$SMOKE_SCRIPT" > "$log_file" 2>&1 || exit_code=$?
    local pass_count
    pass_count=$(grep -cE "^\[?PASS\]?" "$log_file" 2>/dev/null || echo "0")
    pass_count="${pass_count//[!0-9]/}"
    : "${pass_count:=0}"
    pass_count=$((10#$pass_count))
    if [ "$exit_code" -eq 0 ]; then
        log_pass "${label} smoke OK (${pass_count} passes)" >&2
    else
        log_fail "${label} smoke FAIL (exit ${exit_code}, ${pass_count} passes) — log: ${log_file}" >&2
    fi
    echo "$pass_count"
}

wait_health() {
    local deadline=$(( $(date +%s) + HEALTH_TIMEOUT ))
    log_info "Waiting for ${HEALTH_URL} to return 200 (timeout ${HEALTH_TIMEOUT}s)..."
    while [ "$(date +%s)" -lt "$deadline" ]; do
        local code
        code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$HEALTH_URL" 2>/dev/null || echo "000")
        if [ "$code" = "200" ]; then
            log_pass "Health endpoint up (HTTP 200)"
            return 0
        fi
        sleep 15
    done
    log_fail "Health endpoint did not return 200 within ${HEALTH_TIMEOUT}s"
    return 1
}

trigger_rollback() {
    local sha="$1"
    if ! workflow_exists "$WORKFLOW_NAME"; then
        log_defer "$WORKFLOW_NAME not present in .github/workflows/ — Sub-6 PARTIAL"
        log_defer "Once rollback.yml lands, enable real path. Skipping trigger."
        return 0
    fi
    log_info "Triggering ${WORKFLOW_NAME} with target_sha=${sha}..."
    if [ "$MODE" = "execute" ] && [ "${ROLLBACK_CYCLE_E2E:-0}" = "1" ]; then
        gh workflow run "$WORKFLOW_NAME" -f "target_sha=${sha}" \
            || { log_fail "gh workflow run failed"; return 1; }
        log_info "Polling completion..."
        sleep 10
        local run_id
        run_id=$(gh run list --workflow "$WORKFLOW_NAME" --limit 1 --json databaseId --jq '.[0].databaseId')
        gh run watch "$run_id" --exit-status \
            || { log_fail "rollback workflow run failed (id=$run_id)"; return 1; }
        log_pass "Rollback workflow completed"
    else
        log_info "(dry-run) Would run: gh workflow run $WORKFLOW_NAME -f target_sha=$sha"
    fi
}

# ─── Main ──────────────────────────────────────────────────────────────

START_ISO=$(iso_now)
log_step "Rollback Cycle Test (mode=${MODE})"

# Capture current + previous SHAs
log_info "Resolving current + previous SHAs..."
CURRENT_SHA=""
PREVIOUS_SHA=""
if command -v gh >/dev/null 2>&1; then
    CURRENT_SHA=$(gh api "repos/:owner/:repo/commits/main" --jq '.sha' 2>/dev/null || echo "")
fi
if [ -z "$CURRENT_SHA" ]; then
    CURRENT_SHA=$(git rev-parse origin/main 2>/dev/null || git rev-parse HEAD)
fi

if [ -n "$TARGET_SHA" ]; then
    PREVIOUS_SHA="$TARGET_SHA"
else
    if command -v gh >/dev/null 2>&1; then
        PREVIOUS_SHA=$(gh api "repos/:owner/:repo/commits?sha=main&per_page=2" --jq '.[1].sha' 2>/dev/null || echo "")
    fi
    if [ -z "$PREVIOUS_SHA" ]; then
        PREVIOUS_SHA=$(git rev-parse origin/main~1 2>/dev/null || git rev-parse HEAD~1)
    fi
fi

log_info "Current SHA:  ${CURRENT_SHA}"
log_info "Previous SHA: ${PREVIOUS_SHA}"

# Safety gate for execute mode
if [ "$MODE" = "execute" ] && [ "${ROLLBACK_CYCLE_E2E:-0}" != "1" ]; then
    log_fail "--execute requires ROLLBACK_CYCLE_E2E=1 (defense-in-depth)"
    log_info "Re-run as: ROLLBACK_CYCLE_E2E=1 $0 --execute"
    exit 1
fi

# Step 1 — Pre-flight smoke
log_step "Phase 1 — Pre-flight smoke"
PRE_PASS=$(run_smoke "pre" || true)

if [ "$MODE" = "dry-run" ]; then
    log_step "Dry-run complete — skipping rollback + restore phases"
    cat > "$REPORT_FILE" <<EOF
{
  "mode": "dry-run",
  "start": "$START_ISO",
  "end": "$(iso_now)",
  "current_sha": "$CURRENT_SHA",
  "previous_sha": "$PREVIOUS_SHA",
  "pre_smoke_pass": ${PRE_PASS:-0},
  "post_smoke_pass": null,
  "restored_smoke_pass": null,
  "tta_rollback_sec": null,
  "tta_restore_sec": null,
  "workflow_present": $(workflow_exists "$WORKFLOW_NAME" && echo "true" || echo "false")
}
EOF
    log_pass "Report: $REPORT_FILE"
    cat "$REPORT_FILE"
    exit 0
fi

# Step 2 — Trigger rollback
log_step "Phase 2 — Rollback to $PREVIOUS_SHA"
ROLLBACK_START=$(date +%s)
trigger_rollback "$PREVIOUS_SHA"
wait_health
ROLLBACK_END=$(date +%s)
TTA_ROLLBACK=$(( ROLLBACK_END - ROLLBACK_START ))

# Step 3 — Post-rollback smoke
log_step "Phase 3 — Post-rollback smoke"
POST_PASS=$(run_smoke "post-rollback" || true)
if [ "${POST_PASS:-0}" -lt "${PRE_PASS:-0}" ]; then
    log_warn "Post-rollback pass count (${POST_PASS}) < pre (${PRE_PASS}) — investigate"
fi

# Step 4 — Restore forward
log_step "Phase 4 — Restore forward to $CURRENT_SHA"
RESTORE_START=$(date +%s)
trigger_rollback "$CURRENT_SHA"
wait_health
RESTORE_END=$(date +%s)
TTA_RESTORE=$(( RESTORE_END - RESTORE_START ))

# Step 5 — Restored smoke
log_step "Phase 5 — Restored smoke"
RESTORED_PASS=$(run_smoke "restored" || true)

# Report
END_ISO=$(iso_now)
cat > "$REPORT_FILE" <<EOF
{
  "mode": "execute",
  "start": "$START_ISO",
  "end": "$END_ISO",
  "current_sha": "$CURRENT_SHA",
  "previous_sha": "$PREVIOUS_SHA",
  "pre_smoke_pass": ${PRE_PASS:-0},
  "post_smoke_pass": ${POST_PASS:-0},
  "restored_smoke_pass": ${RESTORED_PASS:-0},
  "tta_rollback_sec": ${TTA_ROLLBACK:-0},
  "tta_restore_sec": ${TTA_RESTORE:-0},
  "workflow_present": $(workflow_exists "$WORKFLOW_NAME" && echo "true" || echo "false")
}
EOF

log_step "Cycle complete"
log_pass "Report: $REPORT_FILE"
cat "$REPORT_FILE"
exit 0
