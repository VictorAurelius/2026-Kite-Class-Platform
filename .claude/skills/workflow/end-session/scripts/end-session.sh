#!/usr/bin/env bash
# end-session.sh — archive the current session's lock file + emit 1-line summary.
# Phase 2 of GAP-193. Pair with /start-session (Phase 1).
#
# Usage:
#   ./end-session.sh              # Step 0a gate → archive lock + print summary
#   ./end-session.sh --check-only # run Step 0a gate report only (exit 2 if dirty)
#   ./end-session.sh --allow-dirty # bypass Step 0a dirty-block (note reason)
#   ./end-session.sh --keep-lock  # print summary only, leave lock in place
#   ./end-session.sh --summary-only  # alias of --keep-lock
#
# Step 0a gate (per SKILL.md): blocks end if main tree OR any worktree has
# uncommitted changes (next session would inherit a dirty/disoriented state).
#
# Exit codes:
#   0 — success (or no active lock; not an error)
#   1 — internal error (filesystem / git unavailable)
#   2 — Step 0a gate FAIL (working tree dirty; resolve or --allow-dirty)

set -u

KEEP_LOCK=0
CHECK_ONLY=0
ALLOW_DIRTY=0
for arg in "$@"; do
    case "$arg" in
        --keep-lock|--summary-only) KEEP_LOCK=1 ;;
        --check-only) CHECK_ONLY=1 ;;
        --allow-dirty) ALLOW_DIRTY=1 ;;
        -h|--help)
            sed -n '2,18p' "$0"; exit 0 ;;
    esac
done

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || { echo "FAIL: not in a git repo" >&2; exit 1; }

# ── Step 0a gate: working-tree clean + sync (per SKILL.md Step 0a) ──
# Blocks end-session if main tree / any worktree has uncommitted changes
# (next session would pickup a dirty/disoriented state). --allow-dirty overrides.
GATE_FAIL=0
GATE_REPORT=""
_dirty_main="$(git status --porcelain 2>/dev/null)"
if [[ -n "$_dirty_main" ]]; then
    GATE_FAIL=1
    GATE_REPORT+="  🔴 Main tree DIRTY ($(printf '%s\n' "$_dirty_main" | grep -c .) uncommitted file)"$'\n'
fi
while IFS= read -r wt; do
    [[ -z "$wt" || "$wt" == "$REPO_ROOT" ]] && continue
    _dw="$(git -C "$wt" status --porcelain 2>/dev/null)"
    if [[ -n "$_dw" ]]; then
        GATE_FAIL=1
        GATE_REPORT+="  🔴 Worktree DIRTY: $wt ($(printf '%s\n' "$_dw" | grep -c .) file)"$'\n'
    fi
done < <(git worktree list --porcelain 2>/dev/null | awk '/^worktree /{print $2}')
git fetch origin main -q 2>/dev/null || true
BEHIND="$(git rev-list --count main..origin/main 2>/dev/null || echo 0)"
if [[ "${BEHIND:-0}" -gt 0 ]]; then
    GATE_REPORT+="  🟡 local main behind origin/main by $BEHIND commit(s) — fast-forward (nếu clean) hoặc handoff branch"$'\n'
fi
[[ "$GATE_FAIL" -eq 0 && -z "$GATE_REPORT" ]] && GATE_REPORT="  ✅ Working tree clean + synced (clean-slate)"
echo "── Step 0a gate: working-tree clean + sync ──"
printf '%s\n' "$GATE_REPORT"
if [[ "$CHECK_ONLY" -eq 1 ]]; then
    [[ "$GATE_FAIL" -eq 1 ]] && exit 2 || exit 0
fi
if [[ "$GATE_FAIL" -eq 1 && "$ALLOW_DIRTY" -eq 0 ]]; then
    echo "🛑 end-session BLOCKED: working tree dirty. Commit/push/PR HOẶC handoff branch trước (SKILL.md Step 0a §Decision). Override: --allow-dirty (note lý do)." >&2
    exit 2
fi

LOCK_DIR=".claude/session-locks"
SESSION_ID="${CLAUDE_SESSION_ID:-$(whoami)@$(hostname):ppid-$PPID}"

# Find the matching lock file (first match wins).
ACTIVE_LOCK=""
if [[ -d "$LOCK_DIR" ]]; then
    while IFS= read -r f; do
        if grep -qF "session_id: $SESSION_ID" "$f" 2>/dev/null \
           || grep -qF "session_id: \"$SESSION_ID\"" "$f" 2>/dev/null; then
            ACTIVE_LOCK="$f"
            break
        fi
    done < <(find "$LOCK_DIR" -maxdepth 1 -type f -name '*.lock' 2>/dev/null)
fi

# Gather summary fields.
BRANCH="$(git branch --show-current 2>/dev/null || echo unknown)"
TURNS="${CLAUDE_TURN_COUNT:-?}"
STARTED=""
if [[ -n "$ACTIVE_LOCK" ]]; then
    STARTED="$(grep -E '^started\s*:' "$ACTIVE_LOCK" 2>/dev/null \
        | head -1 | sed -E 's/^started\s*:\s*//; s/^"//; s/"$//')"
fi

# Elapsed (best-effort — requires ISO date).
ELAPSED="?"
if [[ -n "$STARTED" ]]; then
    START_EPOCH="$(date -d "$STARTED" +%s 2>/dev/null || echo "")"
    if [[ -n "$START_EPOCH" ]]; then
        NOW_EPOCH="$(date +%s)"
        DIFF=$((NOW_EPOCH - START_EPOCH))
        H=$((DIFF / 3600))
        M=$(((DIFF % 3600) / 60))
        ELAPSED="${H}h${M}m"
    fi
fi

# PRs merged since started (best-effort — only inspects local main log).
PRS_MERGED=0
if [[ -n "$STARTED" ]]; then
    PRS_MERGED="$(git log main --since="$STARTED" --oneline 2>/dev/null \
        | grep -cE 'Merge pull request|\(#[0-9]+\)' || echo 0)"
fi

# Gaps touched since started.
GAPS_TOUCHED=0
if [[ -n "$STARTED" ]]; then
    GAPS_TOUCHED="$(git log --since="$STARTED" --name-only \
        -- documents/04-quality/gaps/ 2>/dev/null \
        | grep -cE 'GAP-[0-9]+' || echo 0)"
fi

# Archive (unless --keep-lock).
ARCHIVE_PATH=""
if [[ -n "$ACTIVE_LOCK" && "$KEEP_LOCK" -eq 0 ]]; then
    DATE_TODAY="$(date +%Y-%m-%d)"
    ARCHIVE_DIR="$LOCK_DIR/archived/$DATE_TODAY"
    mkdir -p "$ARCHIVE_DIR"
    # Append closing summary inside the lock content for retro readability.
    {
        echo ""
        echo "# --- session closed by /end-session $(date -Iseconds) ---"
        echo "closed_at: $(date -Iseconds)"
        echo "branch_at_close: $BRANCH"
        echo "prs_merged: $PRS_MERGED"
        echo "gaps_touched: $GAPS_TOUCHED"
        echo "turn_count: $TURNS"
        echo "elapsed: $ELAPSED"
    } >> "$ACTIVE_LOCK"
    BASENAME="$(basename "$ACTIVE_LOCK")"
    ARCHIVE_PATH="$ARCHIVE_DIR/$BASENAME"
    mv "$ACTIVE_LOCK" "$ARCHIVE_PATH"
fi

# Emit 1-line summary (Vietnamese per CLAUDE.md §CRITICAL).
if [[ -z "$ACTIVE_LOCK" ]]; then
    echo "ℹ️  Không có session-lock cho session $SESSION_ID. Tóm tắt: nhánh=$BRANCH, turns=$TURNS."
elif [[ "$KEEP_LOCK" -eq 1 ]]; then
    echo "ℹ️  Lock giữ nguyên (--keep-lock). Tóm tắt: $PRS_MERGED PR merged, $GAPS_TOUCHED gaps touched, $TURNS turns, $ELAPSED elapsed."
else
    echo "✓ Session $SESSION_ID archived → $ARCHIVE_PATH. $PRS_MERGED PR merged, $GAPS_TOUCHED gaps touched, $TURNS turns, $ELAPSED elapsed."
fi

exit 0
