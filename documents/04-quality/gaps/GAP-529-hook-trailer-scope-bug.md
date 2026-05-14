# GAP-529: Hook trailer scope bug — `_has_trailer()` reads HEAD allowing trailer leak across PRs

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Meta (governance hook)
**Found:** 2026-05-14 (Wave 74 Bucket C — Agent C empirical test discovery)
**Affects:** `pre-tool-guard.py` `check_admin_merge` + `check_aws_tier3` + `check_terraform_retry` (any rule using `_has_trailer()`); likely same class affects `audit-gate.py` `has_audit_override` + `has_domain_milestone_defer`

## Problem

`pre-tool-guard.py` `_has_trailer(trailer)` reads HEAD commit body via `git log -1 --format=%B` on current branch. After a PR with a legitimate override trailer (`ADMIN_MERGE_OVERRIDE:`, `AUDIT_OVERRIDE:`, etc.) merges, the trailer can persist as branch HEAD on:

1. **Main HEAD** — until next non-trailer commit moves HEAD past it (transient; resolved by next merge)
2. **Feature branches derived from trailer commit** — until those branches rebase / merge / get abandoned (per-branch; can be long-lived)

Symptom: in any agent session whose current branch HEAD body contains a stale `ADMIN_MERGE_OVERRIDE:` trailer from a previous PR, calling `gh pr merge --admin` will be silently ALLOWED — bypassing the rule the trailer was originally a one-time override for.

### Worked incident (2026-05-14)

PR #1320 (Wave 74 plan) merged with legitimate `ADMIN_MERGE_OVERRIDE: Vercel deployment rate-limited...` trailer (valid per `admin-merge-discipline.md` §2 row "Trivial docs PR"). Squash commit landed on main as `c09b675d`.

Agent C (Bucket C, branch `wave-74-bucket-c-edge-tests` derived from `c09b675d`) ran existing test `test_admin_merge_blocked` which expects BLOCK on `gh pr merge --admin`. Test FAILED — hook found trailer on its branch HEAD → ALLOW.

Agent C added bonus test `test_admin_merge_trailer_scope_bug_documentation` to track the bug + relaxed the existing test to accept either deny (clean HEAD) or allow (HEAD-trailer-leak).

Main HEAD `45efea57` (Bucket A merge) does NOT have trailer → hook blocks correctly on main NOW. But the bug class persists for any future PR with legitimate override that lands and derives downstream branches.

## Root Cause

Trailer scope wrong:
- **Current behavior (BUG):** `_has_trailer()` reads HEAD commit body on current branch — leaks across PRs, branches, sessions
- **Correct behavior:** scope to the SPECIFIC PR being merged. When `check_admin_merge` fires on `gh pr merge <N> --admin`, hook should fetch THAT PR's body or its commits' bodies via `gh pr view <N> --json body,commits`

Same class affects:
- `pre-tool-guard.py` `check_admin_merge` (`ADMIN_MERGE_OVERRIDE`)
- `pre-tool-guard.py` `check_aws_tier3` (potential override trailer)
- `pre-tool-guard.py` `check_terraform_retry` (potential override trailer)
- `audit-gate.py` `has_audit_override` (`AUDIT_OVERRIDE`)
- `audit-gate.py` `has_domain_milestone_defer` (`AUDIT_DEFER_DOMAIN_MILESTONE`)
- Any future hook using `_has_trailer()` helper

## Proposed Fix

### Phase 1 — Per-PR trailer scoping in `pre-tool-guard.py`

1. Detect PR number from command pattern: `gh pr merge <N> --admin` → extract `<N>`
2. Read PR body via `gh pr view <N> --json body,commits` (cached per session if perf concern)
3. Search for trailer in PR body OR first commit body of PR (not HEAD of current branch)
4. Fallback to current `_commit_body()` ONLY if PR number not extractable (rare; backward compat)

### Phase 2 — Helper refactor: `_has_trailer_in_pr(pr_num, trailer)` shared across hooks

Replace `_has_trailer()` with `_has_trailer_in_pr()` accepting PR number context. Update all callers:
- `pre-tool-guard.py` 3 rule functions
- `audit-gate.py` 2 trailer detector functions

### Phase 3 — Regression test using monkey-patched `_commit_body`

Existing test `test_admin_merge_blocked` should not depend on HEAD environmental state. Stub `_commit_body` / `_has_trailer_in_pr` to return controlled values per test case.

## Acceptance Criteria

- [ ] `_has_trailer_in_pr(pr_num, trailer)` helper added to `pre-tool-guard.py` (or shared module)
- [ ] `check_admin_merge` uses PR-scoped trailer detection
- [ ] `check_aws_tier3` + `check_terraform_retry` same migration if they use trailer
- [ ] `audit-gate.py` `has_audit_override` + `has_domain_milestone_defer` migrated
- [ ] `test_admin_merge_blocked` refactored to stub trailer detection (no HEAD dependency)
- [ ] New tests cover: PR body has trailer / first commit body has trailer / no trailer / PR not found
- [ ] Worked self-test: replay PR #1320 incident — branch derived from trailer commit + clean PR body → BLOCK correctly
- [ ] Update `admin-merge-discipline.md` §4 to clarify "trailer applied to PR body" (current text says "applied to SQUASH commit" — same intent, different mechanism)

## Related

- Wave 74 Bucket C audit artifact: `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md` (CRITICAL class #3 → coverage measurement; trailer scoping is sub-class)
- Hook coverage skill: `.claude/skills/quality/hook-review/SKILL.md` (8 rubric points — this bug = point #3 "Override trailer recognition" failure)
- Rule: `.claude/rules/admin-merge-discipline.md` §4 (text says "trailer on SQUASH commit" — semantic mismatch with implementation reading HEAD pre-merge)
- Incident: Wave 74 Bucket C test FAIL → Agent C documented bug → bonus test `test_admin_merge_trailer_scope_bug_documentation` in `.claude/hooks/tests/test-pre-tool-guard.py`

## Log

- **2026-05-14:** Gap filed during Wave 74 closure. Discovered Bucket C 2026-05-14 ~07:43 UTC when existing test `test_admin_merge_blocked` failed unexpectedly. Bonus test added documenting bug. Severity downgraded P0 → P1 after verification: main HEAD `45efea57` does NOT carry trailer (Bucket A + B merges since `c09b675d` moved main past it) — bug per-branch-derivation, not "stuck-forever". Fix Wave 75 candidate; meanwhile main hook works correctly.
