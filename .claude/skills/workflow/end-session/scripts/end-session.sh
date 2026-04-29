#!/usr/bin/env bash
# end-session.sh — archive the current session's lock file + emit 1-line summary.
# Phase 2 of GAP-193. Pair with /start-session (Phase 1).
#
# Usage:
#   ./end-session.sh              # archive lock + print summary
#   ./end-session.sh --keep-lock  # print summary only, leave lock in place
#   ./end-session.sh --summary-only  # alias of --keep-lock
#
# Exit codes:
#   0 — success (or no active lock; not an error)
#   1 — internal error (filesystem / git unavailable)

set -u

KEEP_LOCK=0
for arg in "$@"; do
    case "$arg" in
        --keep-lock|--summary-only) KEEP_LOCK=1 ;;
        -h|--help)
            sed -n '2,15p' "$0"; exit 0 ;;
    esac
done

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT" || { echo "FAIL: not in a git repo" >&2; exit 1; }

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
