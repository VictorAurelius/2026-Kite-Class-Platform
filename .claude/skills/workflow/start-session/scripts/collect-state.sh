#!/usr/bin/env bash
# collect-state.sh — gather session-start context for /start-session skill
# Output: human-readable summary to stdout; errors to stderr
# Usage: ./collect-state.sh [--quick] [--json]
#
# Accuracy fix 2026-04-24 per GAP-206:
#   - Wave + blockers parsed from ROADMAP.md (not filename mtime / alphabetical)
#   - Recent merges from git log
#   - /repo-status integration for full health
#   - Scratchpad awareness for documents/action-2.md

set -u
MODE="${1:-full}"

TS="$(date -Iseconds)"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || exit 1

BRANCH="$(git branch --show-current 2>/dev/null || echo unknown)"

# Dirty check — with scratchpad awareness
DIRTY_FILES="$(git diff --name-only 2>/dev/null; git diff --cached --name-only 2>/dev/null)"
DIRTY_FILES="$(echo "$DIRTY_FILES" | sort -u | grep -v '^$' || true)"
DIRTY_COUNT="$(echo "$DIRTY_FILES" | grep -c '.' 2>/dev/null || echo 0)"

if [ "$DIRTY_COUNT" = "0" ]; then
  BRANCH_STATE="clean"
elif [ "$DIRTY_COUNT" = "1" ] && [ "$DIRTY_FILES" = "documents/action-2.md" ]; then
  BRANCH_STATE="clean (scratchpad only: documents/action-2.md)"
else
  BRANCH_STATE="dirty ($DIRTY_COUNT file(s))"
fi

# PRs (gh required — gracefully skip if not authed)
OPEN_PRS="?"
TOP_PRS=""
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  OPEN_PRS="$(gh pr list --state open --json number --jq 'length' 2>/dev/null || echo '?')"
  TOP_PRS="$(gh pr list --state open --limit 3 --json number,title \
    --jq '.[] | "#\(.number) \(.title[0:60])"' 2>/dev/null | tr '\n' ';' || echo '')"
fi

# Repo-status — full multi-factor health (replaces minimal CI check)
RS_LEVEL="unknown"
RS_CI="?"
RS_CVE_HIGH="?"
RS_CVE_CRIT="?"
RS_STALE_BRANCHES="?"
RS_AUDIT_P0="?"
if [ -f scripts/repo-status.sh ]; then
  RS_JSON="$(bash scripts/repo-status.sh --json 2>/dev/null || echo '{}')"
  if command -v jq >/dev/null 2>&1; then
    RS_LEVEL="$(echo "$RS_JSON" | jq -r '.level // "unknown"' 2>/dev/null || echo unknown)"
    RS_CI="$(echo "$RS_JSON" | jq -r '.ci.status // "unknown"' 2>/dev/null || echo unknown)"
    RS_CVE_HIGH="$(echo "$RS_JSON" | jq -r '(.security.high // 0) + (.security.code_scan_errors // 0)' 2>/dev/null || echo 0)"
    RS_CVE_CRIT="$(echo "$RS_JSON" | jq -r '.security.critical // 0' 2>/dev/null || echo 0)"
    RS_STALE_BRANCHES="$(echo "$RS_JSON" | jq -r '.branches.stale_branches // 0' 2>/dev/null || echo 0)"
    RS_AUDIT_P0="$(echo "$RS_JSON" | jq -r '.audit.p0 // 0' 2>/dev/null || echo 0)"
  fi
fi

# Current wave — parse ROADMAP.md "Next recommended wave" (fallback to mtime)
CURRENT_WAVE=""
ROADMAP="documents/04-quality/gaps/ROADMAP.md"
if [ -f "$ROADMAP" ]; then
  # Look for line like: "**Next recommended wave:** Wave 5 **GAP-047** document generation..."
  CURRENT_WAVE="$(grep -m1 'Next recommended wave' "$ROADMAP" 2>/dev/null \
    | sed -E 's/.*Next recommended wave[:\*]*\s*//; s/\*\*//g' \
    | head -c 120 | xargs 2>/dev/null || echo '')"
fi
# Fallback: mtime newest (legacy behavior, labeled explicitly)
if [ -z "$CURRENT_WAVE" ] && [ -d documents/03-planning/waves ]; then
  CURRENT_WAVE="(mtime fallback) $(ls -t documents/03-planning/waves/*.md 2>/dev/null | head -1 \
    | xargs -I{} basename {} .md 2>/dev/null || echo '')"
fi

# Session locks
LOCK_DIR=".claude/session-locks"
ACTIVE_LOCKS=0
LOCK_LIST=""
if [ -d "$LOCK_DIR" ]; then
  ACTIVE_LOCKS="$(find "$LOCK_DIR" -name 'session-*.lock' -mmin -240 2>/dev/null | wc -l | tr -d ' ')"
  LOCK_LIST="$(find "$LOCK_DIR" -name 'session-*.lock' -mmin -240 2>/dev/null \
    -exec basename {} \; | tr '\n' ';')"
  # Auto-purge stale locks (>4h = 240 min)
  find "$LOCK_DIR" -name 'session-*.lock' -mmin +240 -delete 2>/dev/null || true
fi

# Blocker gaps — parse ROADMAP "GA Blockers" table (not alphabetical grep)
BLOCKERS=""
if [ -f "$ROADMAP" ]; then
  # Extract GAP-XXX entries from the "GA Blockers remaining" section
  BLOCKERS="$(awk '/GA Blockers remaining/,/Priority rule|Epics fully closed/' "$ROADMAP" 2>/dev/null \
    | grep -oE 'GAP-[0-9]+' | sort -u | head -6 | tr '\n' ';' || echo '')"
fi
# Fallback to old behavior if ROADMAP missing
if [ -z "$BLOCKERS" ] && [ -d documents/04-quality/gaps ]; then
  BLOCKERS="(fallback) $(grep -l '^\*\*Status:\*\* 🔵 OPEN' documents/04-quality/gaps/GAP-*.md 2>/dev/null \
    | xargs grep -l '^\*\*Priority:\*\* 🔴 P0\|^\*\*Priority:\*\* 🟠 P1' 2>/dev/null \
    | head -3 | xargs -I{} basename {} .md 2>/dev/null | tr '\n' ';' || echo '')"
fi

# Recent merges — last 5 squash-merges on main in past 3 days
RECENT_MERGES=""
if command -v git >/dev/null 2>&1; then
  RECENT_MERGES="$(git log main --since='3 days ago' --oneline 2>/dev/null \
    | grep -E '#[0-9]+\)?$' | head -5 | tr '\n' '§' || echo '')"
fi

# MCP servers — per .claude/rules/mcp-first-with-fallback.md prefer MCP if connected.
# Surface status here so session start sees it (avoid the 2026-04-26 anti-pattern of
# defaulting to gh CLI all session because MCP availability was never checked).
MCP_TOTAL=0
MCP_CONNECTED=0
MCP_FAILED=""
if command -v claude >/dev/null 2>&1; then
  MCP_RAW="$(claude mcp list 2>/dev/null || true)"
  # Lines look like: "name: docker run ... - ✓ Connected" or "✗ Failed to connect" or "✗ Needs authentication"
  MCP_TOTAL="$(echo "$MCP_RAW" | grep -cE '^[a-zA-Z0-9_-]+:.*-\s*[✓✗]' || echo 0)"
  MCP_CONNECTED="$(echo "$MCP_RAW" | grep -cE '✓\s*Connected' || echo 0)"
  MCP_FAILED="$(echo "$MCP_RAW" | grep -E '✗' \
    | sed -E 's/^([a-zA-Z0-9_-]+):.*$/\1/' | tr '\n' ',' | sed 's/,$//' || echo '')"
fi

if [ "$MODE" = "--json" ]; then
  cat <<EOF
{
  "timestamp": "$TS",
  "branch": "$BRANCH",
  "branch_state": "$BRANCH_STATE",
  "open_prs": "$OPEN_PRS",
  "top_prs": "$TOP_PRS",
  "repo_status_level": "$RS_LEVEL",
  "ci_main": "$RS_CI",
  "cve_critical": "$RS_CVE_CRIT",
  "cve_high": "$RS_CVE_HIGH",
  "stale_branches": "$RS_STALE_BRANCHES",
  "audit_p0": "$RS_AUDIT_P0",
  "current_wave": "$CURRENT_WAVE",
  "active_locks": $ACTIVE_LOCKS,
  "lock_files": "$LOCK_LIST",
  "blocker_gaps": "$BLOCKERS",
  "recent_merges": "$RECENT_MERGES",
  "mcp_total": $MCP_TOTAL,
  "mcp_connected": $MCP_CONNECTED,
  "mcp_failed": "$MCP_FAILED"
}
EOF
  exit 0
fi

if [ "$MODE" = "--quick" ]; then
  echo "Mức: $RS_LEVEL · Nhánh: $BRANCH ($BRANCH_STATE) · PRs: $OPEN_PRS · CVE H/C: $RS_CVE_HIGH/$RS_CVE_CRIT · MCP: $MCP_CONNECTED/$MCP_TOTAL · Wave: ${CURRENT_WAVE:-chưa rõ}"
  exit 0
fi

# Full output — tiếng Việt per CLAUDE.md §CRITICAL Communication Language (GAP-207)
cat <<EOF
# Trạng thái session @ $TS

Nhánh:             $BRANCH ($BRANCH_STATE)
Mức repo:          $RS_LEVEL
  · CI main:       $RS_CI
  · CVE:           $RS_CVE_CRIT critical, $RS_CVE_HIGH high
  · Branches cũ:   $RS_STALE_BRANCHES
  · Audit P0:      $RS_AUDIT_P0
PRs đang mở:       $OPEN_PRS  ${TOP_PRS:+— $TOP_PRS}
MCP servers:       $MCP_CONNECTED/$MCP_TOTAL connected${MCP_FAILED:+ (FAILED: $MCP_FAILED — see hint below)}
Wave hiện tại:     ${CURRENT_WAVE:-<chưa rõ — check ROADMAP.md thủ công>}
Gaps blocker:      ${BLOCKERS:-<none>}
Session locks:     $ACTIVE_LOCKS  [$LOCK_LIST]

Merges gần đây (3 ngày):
$(echo "${RECENT_MERGES:-<none>}" | tr '§' '\n' | sed 's/^/  · /')

Ghi chú:
  · Wave + blockers parse từ documents/04-quality/gaps/ROADMAP.md
  · Mức repo qua scripts/repo-status.sh --json (4 yếu tố)
  · Lock dir: $LOCK_DIR (auto-purge sau 4h stale)
  · Các field cần gh — đảm bảo 'gh auth status' OK
  · MCP failed → 'docker ps' + 'docker pull ghcr.io/github/github-mcp-server' + 'claude mcp list' để reconnect.
    Per .claude/rules/mcp-first-with-fallback.md §3: nếu MCP unavailable thì fallback CLI; nhưng phải biết để swap khi fix xong.
  · ⚠️ Wave-eligibility: trước khi /continue, Claude PHẢI check action sắp tới có ≥3 sub-tasks disjoint không.
    Nếu YES → tạo wave plan + spawn 4-5 parallel agents thay vì serial PRs.
    Refs: feedback_wave_plan_before_serial_prs.md + feedback_parallel_agent_strategy.md
    Anti-pattern 2026-04-26: GAP-229 chạy 3 phases serial (~90min) thay vì parallel (~30min).
EOF
