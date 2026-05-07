# GAP-383: prune-merged-worktrees.sh bails on detached-HEAD worktree

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟡 P2 (cleanup tooling — recurrence-class but low blast radius)
**Domain:** DevOps / scripts
**Found:** 2026-05-07 (Wave 33 closure cleanup attempt)
**Affects:** `scripts/prune-merged-worktrees.sh` invocation in any closure PR when repo has detached-HEAD worktree

## Problem

`scripts/prune-merged-worktrees.sh --yes` exits 1 silently when iterating over a worktree in detached-HEAD state. Repro:

```
git worktree list | grep "(detached HEAD)"
# .claude/worktrees/fix-883  cbf7385e (detached HEAD)
# .claude/worktrees/fix-bucket-d-2  cbf7385e (detached HEAD)
```

Script line that fails (per `bash -x` trace):

```bash
WT_BRANCH=$(echo "$line" | grep -oE '\[[^]]+\]' | head -1 | tr -d '[]')
# For detached HEAD line: $WT_BRANCH = "" (no brackets present)
+ [[ -z "" ]]
# Test passes; loop continues but later check `[[ "$WT_BRANCH" == "main" ]]` etc fails to skip → script bails
```

Effect: closure cleanup step in `post-wave-cleanup.md` §2 cannot complete; husks accumulate. Wave 33 closure (PR #900) had to defer worktree prune; husks tracked manually for next session.

## Root Cause

Script assumes every worktree has a branch name extractable from `[branch-name]` brackets in `git worktree list` output. Detached-HEAD worktrees show `(detached HEAD)` instead — no brackets — empty `WT_BRANCH`. Script's downstream logic (`grep --merged origin/main`, branch deletion) doesn't tolerate empty.

Existing detached-HEAD worktrees in repo as of 2026-05-07:
- `.claude/worktrees/fix-883` (Wave 32 v1 leftover)
- `.claude/worktrees/fix-bucket-d-2` (Wave 32 v1 fix-attempt leftover)

## Proposed Fix

Add empty-WT_BRANCH guard early in loop:

```bash
if [[ -z "$WT_BRANCH" ]]; then
    # Detached HEAD — no branch to delete; just remove worktree if commit is in main
    WT_COMMIT=$(echo "$line" | awk '{print $2}')
    if git merge-base --is-ancestor "$WT_COMMIT" origin/main 2>/dev/null; then
        HUSK_WORKTREES+=("$WT_PATH")
        # No branch to delete, skip HUSK_BRANCHES
    fi
    continue
fi
```

This treats detached-HEAD worktree at commit X as prunable IF X is in `origin/main`'s ancestry (i.e., already merged).

Plus add deduplication: detached-HEAD entries can show same commit multiple times (e.g., 2× `cbf7385e`) — only prune unique paths.

## Acceptance Criteria

- [x] Script handles detached-HEAD worktree: prune if commit in `origin/main` ancestry, skip otherwise (don't bail)
- [x] Self-test: synthetic fixture with detached-HEAD worktree at commit-in-main → script prunes; at commit-not-in-main → script skips with message
- [x] Run on current main: pruned `.claude/worktrees/fix-883` + `fix-bucket-d-2` (manual workaround pre-fix on 2026-05-07; post-fix script verified on `/tmp/gap383-test` fixture covers same case)
- [x] Existing branch-named husks still pruned correctly (regression check: fixture `merged-feat` branch + `branch-merged` worktree both pruned cleanly)
- [x] Script exit code 0 on success, 1 on real failures only (verified `EXIT=0` on fixture run)

## Out of scope

- Refactor worktree listing to use `git worktree list --porcelain` (cleaner parsing) — bigger scope, defer

## Related

- `scripts/prune-merged-worktrees.sh` — script being fixed
- `.claude/rules/post-wave-cleanup.md` — rule that mandates running this script
- Wave 33 closure (PR #900) — incident where bug surfaced
- `feedback_post_merge_doc_sync.md` — closure cleanup discipline

## Log

- **2026-05-07:** Filed at Wave 33 closure retro. Bug surfaced when `scripts/prune-merged-worktrees.sh --yes` bailed on `fix-883` detached-HEAD worktree. Closure PR #900 deferred prune; husks tracked manually for next-session cleanup.
- **2026-05-07 (fix shipped):** Script updated to detect detached-HEAD lines in `git worktree list` output, treat empty `WT_BRANCH` as sentinel for "no branch to delete", and prune the worktree only if `git merge-base --is-ancestor $commit origin/main` succeeds. Otherwise the entry is preserved and a warning printed to stderr. Path-dedup added to handle repeated entries some git versions emit. Verified on `/tmp/gap383-test` fixture with three worktree variants (branch-merged / detached-in-main / detached-not-in-main): `EXIT=0`, prune count 2, skip count 1 with warning. Real-repo run earlier today (manual detached-HEAD removal then full prune) confirmed root scenario.
