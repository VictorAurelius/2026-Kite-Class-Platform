#!/usr/bin/env bash
# collect-state.sh — gather session-start context for /start-session skill
# Output: human-readable summary to stdout; errors to stderr
# Usage: ./collect-state.sh [--quick] [--json]

set -u
MODE="${1:-full}"

TS="$(date -Iseconds)"
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || exit 1

BRANCH="$(git branch --show-current 2>/dev/null || echo unknown)"

# Dirty check
if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
  BRANCH_STATE="dirty"
else
  BRANCH_STATE="clean"
fi

# PRs (gh required — gracefully skip if not authed)
OPEN_PRS="?"
TOP_PRS=""
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  OPEN_PRS="$(gh pr list --state open --json number --jq 'length' 2>/dev/null || echo '?')"
  TOP_PRS="$(gh pr list --state open --limit 3 --json number,title,statusCheckRollup \
    --jq '.[] | "#\(.number) \(.title[0:50])"' 2>/dev/null | tr '\n' ';' || echo '')"
fi

# CI on main (best-effort)
CI_STATE="unknown"
if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  CI_STATE="$(gh run list --branch main --limit 1 --json conclusion \
    --jq '.[0].conclusion // "pending"' 2>/dev/null || echo 'unknown')"
fi

# Current wave (newest file in waves/)
CURRENT_WAVE=""
if [ -d documents/03-planning/waves ]; then
  CURRENT_WAVE="$(ls -t documents/03-planning/waves/*.md 2>/dev/null | head -1 \
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

# Top 3 blocker gaps (P0/P1 open)
BLOCKERS=""
if [ -d documents/04-quality/gaps ]; then
  BLOCKERS="$(grep -l '^\*\*Status:\*\* 🔵 OPEN' documents/04-quality/gaps/GAP-*.md 2>/dev/null \
    | xargs grep -l '^\*\*Priority:\*\* 🔴 P0\|^\*\*Priority:\*\* 🟠 P1' 2>/dev/null \
    | head -3 | xargs -I{} basename {} .md 2>/dev/null | tr '\n' ';' || echo '')"
fi

if [ "$MODE" = "--json" ]; then
  cat <<EOF
{
  "timestamp": "$TS",
  "branch": "$BRANCH",
  "branch_state": "$BRANCH_STATE",
  "open_prs": "$OPEN_PRS",
  "top_prs": "$TOP_PRS",
  "ci_main": "$CI_STATE",
  "current_wave": "$CURRENT_WAVE",
  "active_locks": $ACTIVE_LOCKS,
  "lock_files": "$LOCK_LIST",
  "blocker_gaps": "$BLOCKERS"
}
EOF
  exit 0
fi

if [ "$MODE" = "--quick" ]; then
  echo "Wave: ${CURRENT_WAVE:-unknown} · Branch: $BRANCH ($BRANCH_STATE) · PRs: $OPEN_PRS · CI main: $CI_STATE · Locks: $ACTIVE_LOCKS"
  exit 0
fi

# Full output
cat <<EOF
# Session State @ $TS

Branch:        $BRANCH ($BRANCH_STATE)
Current wave:  ${CURRENT_WAVE:-<none>}
Open PRs:      $OPEN_PRS
Top PRs:       ${TOP_PRS:-<none>}
CI main:       $CI_STATE
Active locks:  $ACTIVE_LOCKS  [$LOCK_LIST]
Blocker gaps:  ${BLOCKERS:-<none>}

Note: lock dir = $LOCK_DIR (auto-purged >4h stale).
For gh-dependent fields (PRs, CI), ensure 'gh auth status' is OK.
EOF
