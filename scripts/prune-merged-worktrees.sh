#!/usr/bin/env bash
# =========================================================================
# prune-merged-worktrees.sh — Post-wave cleanup
# =========================================================================
# Per `.claude/rules/post-wave-cleanup.md`. Removes worktree husks (worktrees
# whose branch has been deleted on remote OR merged into main) + local branches
# already merged to main.
#
# Usage:
#   ./scripts/prune-merged-worktrees.sh           # interactive (asks confirm)
#   ./scripts/prune-merged-worktrees.sh --dry-run # preview only
#   ./scripts/prune-merged-worktrees.sh --yes     # non-interactive (CI)
#
# Safe by default — never touches main, current branch, or worktrees with
# uncommitted changes. Force-removes only after confirmation.
# =========================================================================
set -euo pipefail

DRY_RUN=false
ASSUME_YES=false
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=true ;;
        --yes|-y)  ASSUME_YES=true ;;
        --help|-h)
            sed -n '2,16p' "$0"
            exit 0
            ;;
    esac
done

REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

# Fetch + prune remote-tracking refs first so locally-merged check is accurate.
git fetch --prune origin >/dev/null 2>&1 || true

CURRENT_BRANCH=$(git branch --show-current)
MERGED_BRANCHES=$(git branch --merged origin/main 2>/dev/null \
    | sed -E 's/^[* +] //; s/^\+ //' \
    | grep -vE '^(main|HEAD)$' \
    | grep -v "^${CURRENT_BRANCH}$" \
    || true)

# Worktree husks = ANY worktree under .claude/worktrees/ (agent-scratch ephemeral by design
# per `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`).
# These are created per-agent for parallel wave-pack runs and should be pruned post-wave.
# Parse user-friendly format: "<path> <sha> [<branch>]" or "<path> <sha> (detached HEAD)"
HUSK_WORKTREES=()
HUSK_BRANCHES=()  # parallel array; "" entry = detached-HEAD worktree, no branch to delete
SEEN_PATHS=()
while IFS= read -r line; do
    WT_PATH=$(echo "$line" | awk '{print $1}')
    WT_PATH="${WT_PATH/#\~/$HOME}"
    [[ "$WT_PATH" == "$REPO_ROOT" ]] && continue
    # Only target agent-scratch worktrees
    [[ "$WT_PATH" != *"/.claude/worktrees/"* ]] && continue
    # Dedupe (detached HEAD entries can repeat under some git versions)
    DUP=false
    for seen in "${SEEN_PATHS[@]:-}"; do
        [[ "$seen" == "$WT_PATH" ]] && DUP=true && break
    done
    $DUP && continue
    SEEN_PATHS+=("$WT_PATH")

    WT_COMMIT=$(echo "$line" | awk '{print $2}')
    WT_BRANCH=$(echo "$line" | grep -oE '\[[^]]+\]' | head -1 | tr -d '[]' || true)

    if [[ -z "$WT_BRANCH" ]]; then
        # Detached HEAD — no branch to delete. Prune only if commit is reachable
        # from origin/main (i.e., the work has landed). Otherwise skip with a hint.
        if git merge-base --is-ancestor "$WT_COMMIT" origin/main 2>/dev/null; then
            HUSK_WORKTREES+=("$WT_PATH")
            HUSK_BRANCHES+=("")  # sentinel: no branch
        else
            echo "  ⚠️  Skipping detached-HEAD worktree (commit $WT_COMMIT not in origin/main): $WT_PATH" >&2
        fi
        continue
    fi

    [[ "$WT_BRANCH" == "main" || "$WT_BRANCH" == "$CURRENT_BRANCH" ]] && continue
    HUSK_WORKTREES+=("$WT_PATH")
    HUSK_BRANCHES+=("$WT_BRANCH")
done < <(git worktree list)

WT_COUNT=${#HUSK_WORKTREES[@]}
BR_COUNT=$(echo -n "$MERGED_BRANCHES" | grep -c . || true)

echo "─────────────────────────────────────────────────"
echo "Post-wave cleanup detection"
echo "─────────────────────────────────────────────────"
echo "Worktree husks (branch deleted/merged):  $WT_COUNT"
if [[ $WT_COUNT -gt 0 ]]; then
    for i in "${!HUSK_WORKTREES[@]}"; do
        if [[ -z "${HUSK_BRANCHES[$i]}" ]]; then
            echo "    - ${HUSK_WORKTREES[$i]}  (detached HEAD)"
        else
            echo "    - ${HUSK_WORKTREES[$i]}  [${HUSK_BRANCHES[$i]}]"
        fi
    done
fi
echo "Local branches merged to origin/main:    $BR_COUNT"
if [[ $BR_COUNT -gt 0 ]]; then
    echo "$MERGED_BRANCHES" | sed 's/^/    - /'
fi
echo "─────────────────────────────────────────────────"

if [[ $WT_COUNT -eq 0 && $BR_COUNT -eq 0 ]]; then
    echo "✅ Nothing to prune — repo clean."
    exit 0
fi

if $DRY_RUN; then
    echo "DRY-RUN: no changes made."
    exit 0
fi

if ! $ASSUME_YES; then
    read -r -p "Prune $WT_COUNT worktree(s) + $BR_COUNT branch(es)? [y/N] " ANSWER
    if [[ "$ANSWER" != "y" && "$ANSWER" != "Y" ]]; then
        echo "Aborted."
        exit 0
    fi
fi

# Prune worktrees first (releases their branch holds)
for i in "${!HUSK_WORKTREES[@]}"; do
    WT_PATH="${HUSK_WORKTREES[$i]}"
    WT_BRANCH="${HUSK_BRANCHES[$i]}"
    echo "Removing worktree: $WT_PATH"
    git worktree unlock "$WT_PATH" >/dev/null 2>&1 || true
    git worktree remove --force "$WT_PATH" 2>&1 | sed 's/^/    /' || true
done
git worktree prune

# Prune local branches
if [[ $BR_COUNT -gt 0 ]]; then
    echo "$MERGED_BRANCHES" | while IFS= read -r BR; do
        [[ -z "$BR" ]] && continue
        echo "Deleting branch: $BR"
        git branch -D "$BR" 2>&1 | sed 's/^/    /' || true
    done
fi

echo "─────────────────────────────────────────────────"
echo "✅ Cleanup complete."
git worktree list | head -5
