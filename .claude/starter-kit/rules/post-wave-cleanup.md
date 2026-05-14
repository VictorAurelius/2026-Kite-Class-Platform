# Post-Wave Cleanup — prune worktree husks + merged branches in closure

**Priority:** 🟠 MANDATORY — closure protocol governance
**Version:** 1.0.0
**Created:** 2026-05-06
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every wave closure PR — coordinator must run cleanup as part of closure protocol

---

## 1. The Rule

> **Wave closure PR MUST include a cleanup step that prunes (a) all `.claude/worktrees/agent-*/` worktree husks and (b) local branches merged to `origin/main`. Cleanup runs BEFORE closure PR ships, leaving zero post-wave residue.**

`.claude/worktrees/agent-*/` are agent-scratch ephemeral by design (per `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`). Each parallel wave-pack run creates 3-5 worktrees. Without explicit cleanup in closure protocol, worktrees accumulate forever — Wave 22-26 left 19 worktrees + 28 stale branches over 5 waves before user flagged it 2026-05-06.

This rule closes the silent-leak in closure protocol that surfaced 2026-05-06: each wave's post-merge cleanup was implicit "when convenient" not explicit "in closure checklist," so it never happened.

---

## 2. Mandatory cleanup step

Add this to every wave closure PR (alongside ROADMAP §🚀 Next Action update + wave plan `status:complete` flip + `wave-history.jsonl` append per Rule 15):

```bash
bash scripts/prune-merged-worktrees.sh --yes
```

The script:
- Detects all worktrees under `.claude/worktrees/` (agent-scratch ephemeral)
- Detects local branches merged to `origin/main` (excluding main + current)
- Force-removes worktrees + deletes merged branches
- Safe by default — never touches main, current branch, or non-`.claude/worktrees/` paths

If output shows ≥1 husk OR ≥1 merged branch BEFORE running, that is the violation this rule prevents. After running, output should be `Nothing to prune — repo clean.`.

### When to run

| Phase | Action |
|---|---|
| Pre-closure-PR | Run `--dry-run` first to preview |
| Closure PR coordinator-prep | Run `--yes` after all bucket PRs merged + before drafting closure PR |
| If closure PR fails CI | Re-run after fix; cleanup is idempotent |
| Mid-wave (between buckets) | DO NOT prune until all bucket PRs merged — script protects against current branch but not against in-flight PRs |

### What NOT to prune

- Main worktree (script auto-skips)
- Current branch (script auto-skips)
- Worktrees outside `.claude/worktrees/` (e.g., manual `git worktree add ../sister-repo`)
- Branches with unmerged commits (script uses `git branch --merged origin/main` — strict)

---

## 3. Enforcement

### 3.1 Wave plan template (lands same PR)

`documents/03-planning/waves/_TEMPLATE.md` §7 Closure Protocol — add bullet:
> - Run `bash scripts/prune-merged-worktrees.sh --yes` to prune worktree husks + merged branches per `post-wave-cleanup.md` (paired with closure docs sync)

### 3.2 `start-session` collect-state.sh hint (lands same PR)

When `collect-state.sh` detects ≥3 worktree husks OR ≥3 merged branches, emit hint with exact command:
```
⚠️  Post-wave cleanup needed: 18 husks + 12 merged branches
    Run: bash scripts/prune-merged-worktrees.sh --dry-run
```

### 3.3 Memory cross-link (lands same PR)

`feedback_post_merge_doc_sync.md` — extend cleanup-step row to include this rule.

### 3.4 Override mechanism

Mid-wave incident requires keeping a worktree husk for forensic purposes:

```
git commit -m "...
POST_WAVE_CLEANUP_OVERRIDE: <reason — explain what worktree to keep + how long>"
```

Trailer logged. Worktree manually pruned after forensic window expires.

---

## 4. Self-test (worked example — 2026-05-06)

Repo state at rule creation:
- 19 worktrees under `.claude/worktrees/` (1 main + 18 husks from Wave 22-26)
- 28 local branches (~12 merged to origin/main, ~16 still unmerged)

Apply rule:
```bash
bash scripts/prune-merged-worktrees.sh --yes
# → 18 worktree husks pruned, 10 merged branches deleted
bash scripts/prune-merged-worktrees.sh --yes
# → 0 husks, 3 remaining (delayed merge detection from previous run)
bash scripts/prune-merged-worktrees.sh --yes
# → Nothing to prune — repo clean.
```

Verdict: 19 → 1 worktree, 28 → ~10 branches in 3 idempotent runs. Rule fires correctly + script handles edge cases (deleted branch detection takes a moment after worktree removal).

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Defer cleanup to "quarterly retro" | Cleanup IS the closure step — run before drafting closure PR |
| Run cleanup mid-wave (between bucket merges) | Wait until ALL bucket PRs merged |
| Force-remove worktrees outside `.claude/worktrees/` manually | Script restricts scope; manual `git worktree remove` only with explicit purpose |
| Skip cleanup because "next session start-session will do it" | start-session WARNS but does NOT auto-prune (user-confirm required); coordinator owns closure |
| Delete unmerged branches without `--force` confirmation | Script uses `--merged origin/main` — won't delete unmerged work |

---

## 6. Relationship to other rules

- **`gap-done-discipline.md`** §closure — DONE flip requires AC checked; this rule extends with cleanup mandate (process step)
- **`feedback_post_merge_doc_sync.md`** — sync ROADMAP + wave-plan + wave-history + (NEW) cleanup
- **`agent-background-spawn-default.md`** — establishes worktree as ephemeral scratch; this rule mandates their cleanup
- **`feedback_parallel_agent_strategy.md`** — wave-pack creates worktrees; this rule closes the lifecycle
- **`incident-to-rule-pipeline.md`** — this rule is direct output of 2026-05-06 user-flagged miss "tại sao vẫn miss clean up?"
- **`rule-change-process.md`** §6.5 — this rule + script + closure update + memory all ship same PR per Enforcement Parity Mandate

---

## 7. Log

- **2026-05-06 (v1.0.0):** Rule created in response to user-flagged miss "tại sao vẫn miss clean up? cập nhật workflow?" after Wave 26 closure. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged 17 stale branches + 6 worktree husks accumulated over Wave 22-26), Classify ✓ (no rule covers; closure protocol implicit not explicit), Rule+Enforce ✓ (this file + `scripts/prune-merged-worktrees.sh` + `_TEMPLATE.md` §7 update + `collect-state.sh` hint + memory update — all paired same-PR per `rule-change-process.md` §6.5), Self-Test ✓ (§4 worked example: 19→1 worktree, 28→~10 branches), Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new rule with built-in enforcement, no constraint loosening for prior work; existing accumulated husks fixed by self-test).
