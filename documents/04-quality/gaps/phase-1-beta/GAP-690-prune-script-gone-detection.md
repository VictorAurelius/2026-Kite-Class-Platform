# GAP-690: prune-merged-worktrees.sh missing `[gone]` + worktree-agent-* detection

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta (tooling — post-wave cleanup script)
**Found:** 2026-05-20 (session housekeeping post Wave 102.5)
**Affects:** `post-wave-cleanup.md` §2 enforcement effectiveness; future wave closures

---

## Problem

`scripts/prune-merged-worktrees.sh` reports `Nothing to prune — repo clean.` despite 39 deletable local branches (31 `[gone]` + 8 `worktree-agent-*` orphans) accumulating post Wave 84-102.

Root cause line 39: `git branch --merged origin/main` only catches **ancestry-merged** branches. This repo uses **squash-merge** universally → original branch commits never become ancestors of `main` (squash creates new commit with different SHA) → script misses ~100% of merged branches.

Session 2026-05-20 manual cleanup deleted:
- 31 `[gone]` branches (remote ref absent, PR squash-merged)
- 8 `worktree-agent-*` orphans (Wave 99B+ scratch, no upstream, commits all squash-merged via bucket PRs)
- 30 additional `wave/*` / `chore/*` / `feat/*` branches with stale upstream (repo had `deleteBranchOnMerge: false` until 2026-05-20)

Without script enhancement, this cleanup will recur every 2-3 weeks as buckets accumulate.

---

## Root Cause

| Detection method | Catches squash-merge? | Currently in script? |
|---|:---:|:---:|
| `git branch --merged origin/main` (commit ancestry) | ❌ NO | ✅ Line 39 |
| `[gone]` upstream tracking (`%(upstream:track)`) | ✅ YES | ❌ Missing |
| `worktree-agent-*` prefix pattern | ✅ YES (agent scratch convention) | ❌ Missing |
| Match against `gh pr list --state merged --head <branch>` | ✅ YES | ❌ Missing (expensive; ~1s/branch) |

`[gone]` detection is the cheap + reliable signal. Set via `git fetch --prune` (which the script already runs at line 36).

---

## Proposed Fix

### Phase 1: Add `[gone]` detection (5-10 min)

```bash
# After line 43 (MERGED_BRANCHES detection)
GONE_BRANCHES=$(git for-each-ref --format='%(refname:short) %(upstream:track)' refs/heads/ \
    | awk '$2 == "[gone]" {print $1}' \
    | grep -vE '^(main|HEAD)$' \
    | grep -v "^${CURRENT_BRANCH}$" \
    || true)

# Merge into total deletion list
ALL_TO_DELETE=$(printf "%s\n%s\n" "$MERGED_BRANCHES" "$GONE_BRANCHES" | sort -u | grep -v '^$' || true)
```

### Phase 2: Add `worktree-agent-*` orphan detection (5 min)

```bash
# Worktree-agent-* branches with no upstream + commits unreachable from main
# = orphan scratch from wave-pack runs whose worktree was already removed
AGENT_ORPHANS=$(git for-each-ref --format='%(refname:short) %(upstream:short)' refs/heads/ \
    | awk '$1 ~ /^worktree-agent-/ && $2 == "" {print $1}' \
    || true)

# Optional: verify commit is squash-merged (more expensive but safer)
# Use `gh pr list --state merged --search "<short-sha>"` to confirm
```

### Phase 3: Self-test (3 fixtures)

Per `incident-to-rule-pipeline.md` Stage 4 mandate:
1. Create test branch with squash-merge simulation → script should detect
2. Create `worktree-agent-test123` with no upstream → script should detect
3. Create un-merged feature branch → script should SKIP (false positive guard)

### Phase 4: Update `post-wave-cleanup.md` §2 (5 min)

Update rule to reference new detection logic + add §4 worked example showing 39-branch cleanup.

---

## Acceptance Criteria

- [ ] `scripts/prune-merged-worktrees.sh --dry-run` reports both ancestry-merged + `[gone]` + `worktree-agent-*` orphan branches
- [ ] Self-test fixtures (3) pass — true positives + true negatives validated
- [ ] `post-wave-cleanup.md` §2 updated with new detection mechanism
- [ ] Self-test PASS evidence quoted in fix PR description
- [ ] No regression on existing safety guards (current branch + main + non-`.claude/worktrees/` paths still skipped)

---

## Why this matters

`post-wave-cleanup.md` is 🟠 MANDATORY governance — but enforcement only fires when script detects cleanup candidates. Current ~100% miss rate on squash-merged means rule effectively unenforceable post-Wave-84 (when 4-bucket parallel wave pattern emerged).

**Recurrence cost without fix:**
- ~13 buckets/wave × 4-5 waves/week × 2 weeks = ~100+ stale branches accumulate before manual triage
- Reader velocity hit (collect-state.sh always reports "RED" due to stale branch count)
- Cognitive load on Claude session start (every `/start-session` sees stale count, has to decide if real signal)

**With fix:** post-wave-cleanup runs at wave closure, catches squash-merged within 1 cycle. Recurrence count = 0.

---

## Related

- **Rule:** `.claude/rules/post-wave-cleanup.md` §2 (script invocation mandate)
- **Rule:** `.claude/rules/incident-to-rule-pipeline.md` Stage 4 (self-test mandate)
- **Script:** `scripts/prune-merged-worktrees.sh` lines 39-43 (current detection)
- **Session:** 2026-05-20 housekeeping (manual cleanup of 39 + 30 = 69 branches)
- **Wave:** post Wave 102.5 closure cleanup
- **Repo setting:** `deleteBranchOnMerge=true` enabled 2026-05-20 — future squash-merges auto-delete remote, so `[gone]` detection will be primary signal post-2026-05-20

---

## Log

- **2026-05-20:** Gap filed during session housekeeping post Wave 102.5 closure. Manual cleanup deleted 39 branches the script missed. Repo setting `deleteBranchOnMerge=true` enabled same session — script enhancement still needed because `[gone]` detection runs locally without `gh` API call, faster + offline-friendly.
