# Admin-Merge Discipline — `gh pr merge --admin` is a sharp tool

**Priority:** 🔴 CRITICAL — guardrail against silent main breakage
**Version:** 1.0.0
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every `gh pr merge --admin` invocation in this repo (manual OR automated)

---

## 1. The Rule

> **`--admin` flag bypasses ALL pre-merge CI gates including required status checks, strict-warnings profile, and test compile.** Use it ONLY when you have just-now run the equivalent verification locally on the **exact merge candidate** (HEAD of PR branch rebased onto current main).
>
> If verification cannot run locally (infra-dep test, Lighthouse, smoke), `--admin` requires `ADMIN_MERGE_OVERRIDE:` trailer with reason + link to follow-up gap that schedules the missing verification.

This rule exists because 2026-05-07 GAP-926 incident: PR #920 (Wave 35 Bucket D) had Java compile error in `BetaAccessServiceTest` line 285 — missing 7th param `consentGiven` after Bucket B added it to DTO. Git auto-merge during rebase didn't add conflict markers (different code regions), but the resulting code didn't compile. Coordinator force-pushed rebased branch and **immediately merged with `--admin` bypassing the new CI run**. Broken code landed on main; user caught it via IDE diagnostic on next read.

The `--admin` flag is GitHub's "I know what I'm doing" override for branch protection. It assumes the user has independently verified the merge candidate. When the user hasn't, `--admin` becomes "I don't know what I'm doing but ship anyway."

---

## 2. When `--admin` is acceptable

| Scenario | Acceptable? | Required preconditions |
|---|---|---|
| Local verify just ran clean for diff scope | ✅ | `mvn verify` / `pnpm test --run` / `pnpm build` for affected modules ≤ 5 minutes ago |
| Trivial docs PR (no code), repo-wide checks all pending too long | ✅ | Diff is `*.md` only OR `documents/**` only; no code paths touched |
| CI infrastructure broken (GitHub Actions outage) | ⚠️ | Verify upstream status page; trailer required |
| Merge conflict resolved + force-pushed; CI re-running | ❌ | **WAIT for new CI run** — this is exactly when bypass causes harm |
| "Just want to ship" (impatience) | ❌ | Wait. CI is faster than the bug-fix PR cycle that follows |
| Strict-warnings flagged something non-blocking | ❌ | Fix the warning OR add `@SuppressWarnings` with rationale |

**Hard rule:** after rebase or force-push, `--admin` is BANNED until the new CI run completes (this is the most common harm pattern). The rebase changed the code; previous CI runs validate stale commits.

---

## 3. Required local verification by change scope

If the PR touches these paths, the LISTED command MUST have run clean within 5 minutes of `--admin` merge:

| Path scope | Required local verify |
|---|---|
| `your-product-a/{your-service-admin,branding,email,gateway,platform,subscription}/**` | `cd your-product-a && ./mvnw -pl <module> verify -P strict-warnings` |
| `your-product-b/your-core/**` | `cd your-product-b/your-core && ./mvnw verify -P strict-warnings` |
| `your-product-a/your-frontend-a/**` OR `your-product-b/your-frontend-b/**` | `pnpm -F <pkg> test --run && pnpm -F <pkg> build && pnpm -F <pkg> lint` |
| `infrastructure/helm/**` | `helm lint <chart>` + `helm template <chart>` clean |
| `.github/workflows/*.yml` | `python3 -c "import yaml; yaml.safe_load(open('<file>'))"` |
| `.husky/**`, `scripts/*.sh` | `shellcheck <file>` |
| `documents/**`, `.claude/**`, `*.md` | none needed |
| Mixed multi-layer | ALL applicable verifies must be clean |

**Verify the EXACT merge candidate:** if main moved, rebase locally first, then verify, then merge. Don't verify pre-rebase HEAD.

---

## 4. Override mechanism

For genuine cases where local verify cannot run (infra-dep test, Playwright on remote browser, vendor smoke):

```
git commit -m "...
ADMIN_MERGE_OVERRIDE: <reason — what verify could not run locally>
ADMIN_MERGE_FOLLOWUP: GAP-XXX (<eta>) — schedules the deferred verification"
```

Trailer requirements:
1. Reason cited (be specific — "Lighthouse needs prod-like build"; not "trust me")
2. Follow-up gap link with concrete completion date
3. Quarterly retro reviews override frequency — pattern frequency >5% of admin merges triggers meta-review

Trailer applied to the SQUASH commit (so it lands on main, not PR feature branch).

---

## 5. Concrete examples

### BAD — what 2026-05-07 GAP-926 did

```bash
# Bucket D PR #920 had merge conflict on BetaAccessService constants
git -C /tmp/wt-d rebase origin/main   # auto-merge, no markers, but 6→7 param drift in test
git -C /tmp/wt-d push --force-with-lease
gh pr merge 920 --squash --admin       # ❌ NO local mvn verify; new CI run not yet started
```

Result: broken test compile lands on main; user catches via IDE.

### GOOD — same scenario, properly handled

```bash
git -C /tmp/wt-d rebase origin/main
# Local verify the EXACT rebased HEAD
git -C /tmp/wt-d push --force-with-lease
cd /tmp/wt-d/your-product-a && ./mvnw -pl your-service-subscription verify -P strict-warnings  # ← MUST PASS
gh pr merge 920 --squash --admin       # ✅ now safe; verify just ran clean
```

OR (preferred when no time pressure):

```bash
git -C /tmp/wt-d push --force-with-lease
# Wait for CI to re-run on new HEAD; check status
until gh pr checks 920 --json state --jq '...' ; do sleep 30; done
gh pr merge 920 --squash       # ✅ no --admin; CI is the gate
```

---

## 6. Enforcement

### 6.1 Reviewer-checklist (manual)

When reviewing a PR that was merged with `--admin` (verify via `gh pr view <n> --json mergeStateStatus`), reviewer asks:
- Did the author cite local verify in PR description / commit body?
- If not, was there an `ADMIN_MERGE_OVERRIDE:` trailer with valid reason + follow-up gap?
- If neither, file an incident gap referencing this rule §6.4

### 6.2 Memory auto-load (per-session enforcement)

Memory entry `feedback_admin_merge_bypass_test_compile.md` (paired same-PR) loads at session start. Includes the GAP-926 worked example as cautionary tale + 5-bullet checklist before any `--admin` invocation.

### 6.3 Detector (deferred — follow-up gap)

Tracked separately: scan recent commits for squash messages where merge time < 60s after force-push and check that author also ran local verify (heuristic via shell history or CI pre-merge state). Cost-benefit per `incident-to-rule-pipeline.md` §3 advisory-rule guard — defer until 2nd recurrence.

### 6.4 Quarterly audit

`quality-audit` skill samples last 90 days of squash-merge commits for `--admin` indicators. Verify each has either:
- Local verify mention in commit body / PR description, OR
- `ADMIN_MERGE_OVERRIDE:` trailer with valid reason + landed follow-up gap

Pattern frequency >5% of admin merges per quarter → meta-review of this rule.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| `gh pr merge --admin` immediately after `git push --force-with-lease` | Wait for new CI run OR run local verify on exact rebased HEAD |
| "CI is slow, --admin will save 5 min" | The fix-the-bug-PR cycle costs 30+ min; CI wait wins |
| Use `--admin` because rebase auto-merge "looked fine" | Auto-merge ≠ compile check. Always run `mvn verify` / `pnpm build` |
| Use `--admin` for "trivial" code change | Trivial isn't trivial when DTO signatures shifted underneath |
| Use `--admin` "to unblock the team" | If you must ship now, use `ADMIN_MERGE_OVERRIDE:` trailer with explicit reason |
| Skip local verify because "tests took 10min" | 10min beats hours of follow-up debugging on broken main |

---

## 8. Relationship to other rules

- **`feedback_coordinator_ci_fix_pattern.md`** (memory) — establishes "fix on agent branch + force-push" pattern; this rule extends it: after force-push, **wait for CI** before merging
- **`gap-done-discipline.md`** §2 — DONE flip requires AC verified; if AC includes "tests pass", `--admin` bypass means DONE flip is invalid
- **`output-review-mandate.md`** §3 — every output requires review process executed; `--admin` bypasses the test-execution layer of review
- **`rule-change-process.md`** §6.5 — Enforcement Parity Mandate: this rule + its memory + worked self-test all land same PR
- **`incident-to-rule-pipeline.md`** — this rule is direct output of GAP-926 incident applied through 5-stage pipeline
- **`feedback_admin_merge_bypass_test_compile.md`** (memory, paired same PR) — per-session reminder + 5-bullet checklist

---

## 9. Worked self-test — apply rule to GAP-926 incident

**Scenario:** Coordinator just rebased Bucket D (PR #920) on main + force-pushed.

**Apply §3 matrix:** Diff touches `your-product-a/your-service-subscription/**` → required `cd your-product-a && ./mvnw -pl your-service-subscription verify -P strict-warnings`.

**At decision time** (post-force-push, pre-merge):
- Did `mvn verify` run on exact rebased HEAD? ❌ NO
- Is there an override trailer? ❌ NO (just `gh pr merge --admin`)
- Verdict per §2 row "Merge conflict resolved + force-pushed; CI re-running": **`--admin` BANNED** in this scenario

**Counterfactual:** if rule existed at decision time, coordinator would have:
1. Run `cd your-product-a && ./mvnw -pl your-service-subscription verify -P strict-warnings`
2. Hit Java compile error at `BetaAccessServiceTest:285`
3. Fixed before merge
4. GAP-926 fix-PR cycle eliminated; saved ~10min + zero broken-main exposure

**Verdict:** rule fires correctly on the original incident. Self-test PASS ✅.

---

## 10. Open Items

- [ ] Detector implementation (§6.3) — scan squash-commit metadata for force-push-then-immediate-merge pattern. Defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard. Track in follow-up gap if 2nd recurrence happens.
- [ ] Quarterly audit (§6.4) integration into existing `quality-audit` skill — small wiring task, batch with next quality-audit skill update.

---

## 11. Log

- **2026-05-07 (v1.0.0):** Rule created. Triggered by GAP-926 incident (PR #920 Bucket D rebased + force-pushed + immediately `--admin` merged → Java compile error in `BetaAccessServiceTest:285` landed on main; user caught via IDE diagnostic). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no existing rule prohibits `--admin` post-rebase; `feedback_coordinator_ci_fix_pattern.md` covers force-push pattern but not post-merge gate) → Rule+Enforce ✓ (this file + memory `feedback_admin_merge_bypass_test_compile.md` paired same PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§9 worked example) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds new constraint, no constraint loosening). Detector deferred per §10 Open Items per premature-rule guard.
