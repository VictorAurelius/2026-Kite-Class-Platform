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
  "recent_merges": "$RECENT_MERGES"
}
EOF
  exit 0
fi

if [ "$MODE" = "--quick" ]; then
  echo "Level: $RS_LEVEL · Branch: $BRANCH ($BRANCH_STATE) · PRs: $OPEN_PRS · CVE H/C: $RS_CVE_HIGH/$RS_CVE_CRIT · Wave: ${CURRENT_WAVE:-unknown}"
  exit 0
fi

# Full output
cat <<EOF
# Session State @ $TS

Branch:          $BRANCH ($BRANCH_STATE)
Repo level:      $RS_LEVEL
  · CI main:     $RS_CI
  · CVE:         $RS_CVE_CRIT critical, $RS_CVE_HIGH high
  · Stale brs:   $RS_STALE_BRANCHES
  · Audit P0:    $RS_AUDIT_P0
Open PRs:        $OPEN_PRS  ${TOP_PRS:+— $TOP_PRS}
Current wave:    ${CURRENT_WAVE:-<none — check ROADMAP.md manually>}
Blocker gaps:    ${BLOCKERS:-<none>}
Active locks:    $ACTIVE_LOCKS  [$LOCK_LIST]

Recent merges (last 3 days):
$(echo "${RECENT_MERGES:-<none>}" | tr '§' '\n' | sed 's/^/  · /')

Notes:
  · Wave + blockers parsed from documents/04-quality/gaps/ROADMAP.md
  · Repo level via scripts/repo-status.sh --json (4 factors)
  · Lock dir: $LOCK_DIR (auto-purged >4h stale)
  · For gh-dependent fields, ensure 'gh auth status' is OK
EOF
