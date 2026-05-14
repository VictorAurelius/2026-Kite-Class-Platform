# Release Fix Retry Budget — pivot to "remove the gate" at retry #2

**Priority:** 🔴 CRITICAL — release iteration discipline
**Version:** 1.1.0
**Created:** 2026-05-08
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every release-tag retry sequence (e.g. `v0.9.0-beta-staging.N`, `v1.0.0-rc.N`) where a CI gate (Trivy / Lighthouse / E2E / smoke / strict-warnings) blocks tag promotion **AND** every deploy/mutation-op retry sequence where the prior attempt failed due to insufficient observability of underlying failure. Scope explicitly excludes fix-cycles inside a single PR (those follow normal review).

---

## 1. The Rule

> **When a CI gate fails the same release-candidate-tag class for the 2nd consecutive retry, STOP fixing and immediately pivot to "remove or relax the gate" decision instead.** Continued patching past retry #2 produces a sunk-cost spiral with progressively narrower wins; the gate's design or scope is the actual problem.

Concrete trigger:
- `*-staging.N` or `*-rc.N` where N ≥ 3, AND
- The PRECEDING 2 retry-fix PRs targeted the SAME CI gate (Trivy / Lighthouse / E2E test / smoke / strict-warnings / lint), AND
- Each retry merged a "patch the symptom" PR (add to ignore-list, bump 1 dep, document 1 exception) rather than a "redesign the gate" PR

When all 3 conditions hold → the next session MUST apply §3 decision flow before tagging staging.N+1.

---

## 2. Why this rule exists — Phase 3 staging.1-7 saga (2026-05-08)

Phase 3 first OIDC trigger sequence:

| Tag | Failure | Fix PR | Decision quality |
|---|---|---|---|
| staging.1 | Multi-arch base image manifest amd64-only | #1004 amd64-only workflow | ✅ root-cause — 1 retry justified |
| staging.2 | IAM ARN `your-product-a-*` vs `kite/*` mismatch | #1005 ARN pattern fix | ✅ root-cause — 1 retry justified |
| staging.3 | Trivy 6 HIGH+CRITICAL Java CVE (your-service-email scan) | #1009 `.trivyignore` 6 CVEs | ⚠️ patch-symptom — should retry only if confidence high |
| staging.4 | Trivy 48 unique HIGH+ across all 10 services | #1011 conditional severity CRITICAL-only | 🚨 **retry #2 — should have triggered §3 STOP-AND-REDESIGN** |
| staging.5 | Trivy CRITICAL gnutls (alpine 3.23) — 1 missed in ignore | #1012 add CVE-2026-33845 | ❌ rule violation — kept patching past budget |
| staging.6 | Trivy CRITICAL — additional unidentified | (none — went directly to redesign) | ❌ wasted iteration |
| staging.7 | Redesigned: staging.* exit-code 0 (info-only) | #1014 non-blocking gate | ✅ correct pivot at last |

**Cost of the miss:**
- 5 retry attempts (staging.3 → staging.7) for SAME gate (Trivy)
- ~50 CI minutes wasted (staging.4-6 build + Trivy + abandoned)
- 4 fix-PRs (#1009, #1011, #1012 — all later superseded by #1014's redesign)
- Reviewer fatigue + context-switching cost
- Retro: at retry #2 (staging.4 fail), the data already showed "48 CVEs across all services, per-service drift, .trivyignore management not scaling" — sufficient signal to pivot. Continued patching produced 3 more fail iterations.

If this rule had existed at staging.4 fail (retry #2) → §3 decision flow → "remove HIGH gate for staging.* tags" decision in 1 PR, not 4.

---

## 3. Decision flow at retry #2

When CI gate fails the same release-tag class for the 2nd time:

```
1. STOP. Do not draft another fix PR yet.
2. Quantify the gate's scope:
   - How many distinct findings are in scope? (Trivy: count CVEs; E2E: count failed specs)
   - How many DIFFERENT fixes were needed across retry #1 + #2?
   - Is the count GROWING per retry? (e.g., staging.3 = 6 CVEs → staging.4 = 48 CVEs)
3. Apply the §4 pivot matrix (below) to choose path.
4. Document the choice in the next PR's body — even "retry #3 IS the right call" needs explicit rationale.
```

---

## 4. Pivot matrix (apply at retry #2)

| Signal | Action |
|---|---|
| **Finding count GROWING per retry** (e.g., 6 → 48 CVE) | 🚨 **STOP patching.** Gate's blast radius unbounded; redesign gate (relax severity / scope per tag class / make non-blocking for pre-release). |
| **Same finding class but in different sub-scope** (e.g., per-service drift, per-test environment drift) | 🚨 **STOP patching individual sub-scope.** Find the ROOT scope (parent pom, shared base image, central config) and fix once. |
| **Each retry adds 1 entry to an ignore-list** | ⚠️ **Pause.** Are you using the ignore-list as designed (documented exception per Trivy/lint best practice) or as a band-aid? If 3+ entries with same root cause → fix root cause OR remove the gate. |
| **Fixes are in workflow YAML, not in code/dep** | ⚠️ Workflow gate may be over-spec'd. Consider relaxing for tag class. |
| **The standard cited justifies "recommended không mandatory"** | ✅ Lower the gate. `release-deploy-standard.md` §3.1 explicitly allows softer bars for `*-staging.*` / `*-rc.*` pre-release tags. |
| **2 retries, 1 root cause, fix landed but wasn't deployed yet** | ✅ retry #3 is acceptable — root cause is bounded. Document explicitly. |
| **Tooling visibility gap** (workflow poll status disagrees with underlying op state — e.g. SSM `InProgress` while command actually `Failed`) | 🚨 **STOP retrying.** Fix observability FIRST (add CloudWatch streaming / log tail / output config). Retry without observability = bug-fix loop. See §5 row "Tooling-fix-then-retry" for required follow-up gap. |

---

## 5. Allowed exceptions (genuine retry #3+ acceptable)

| Case | Why exempt | Required commit trailer |
|---|---|---|
| Different gate fails each retry (gate-A retry #1, gate-B retry #2, gate-A again retry #3) | Each gate's retry budget independent | None — natural multi-gate sequence |
| External dependency just released a fix; retry #3 = use the fix | Bounded root-cause known fixed | `RELEASE_RETRY_EXTERNAL_FIX: <dep-version + URL>` |
| Hotfix CVE/regulator deadline; gate is the right shape, just need to fix the actual issue | No alternative; production-quality gate is the requirement | `RELEASE_RETRY_DEADLINE_OVERRIDE: <regulator/incident + ETA>` |
| Test environment flake (transient network, runner image bug) | Genuinely transient; not a gate-design issue | `RELEASE_RETRY_TRANSIENT: <evidence — runner ID + error pattern>` |
| **Tooling-fix-then-retry** (visibility gap fixed in same session, retry now possible with observability) | Underlying op was failing without diagnostics; tooling fixed → retry sees actual failure modes | `RELEASE_RETRY_TOOLING_FIXED: <observability-gap-fix-PR + follow-up-gap-ID>` — fix PR landed + follow-up gap closes deferred work |

If the situation doesn't match one of these → §3 decision flow is mandatory.

---

## 6. Concrete examples (good vs bad)

### ❌ BAD — what Phase 3 actually did (staging.3 → staging.7)

```
staging.3 fails Trivy 6 HIGH+CRITICAL Java CVE
→ Fix: .trivyignore 6 entries (PR #1009)         [retry #1 — patch]
staging.4 fails Trivy 48 HIGH+ across all services
→ Fix: severity CRITICAL-only (PR #1011)         [retry #2 — patch — RULE VIOLATED]
staging.5 fails Trivy CRITICAL gnutls
→ Fix: .trivyignore add gnutls (PR #1012)        [retry #3 — patch — wasted]
staging.6 fails Trivy CRITICAL unidentified
→ Fix: <skipped, went to redesign>                [retry #4 — wasted]
staging.7 redesign: staging.* exit-code 0
→ Fix: non-blocking gate (PR #1014)              [retry #5 — finally correct]
```

5 retry attempts. 4 fix-PRs. Should have been 2 fix-PRs (#1004 + #1005 root-cause) + 1 redesign-PR (#1014 equivalent at staging.4).

### ✅ GOOD — what should have happened

```
staging.3 fails Trivy 6 HIGH+CRITICAL
→ §3 decision flow: 1 retry budget. .trivyignore 6 entries.       [retry #1]
staging.4 fails Trivy 48 HIGH+ across all services
→ §3 decision flow STOP. Apply §4 pivot matrix:
  - Finding count GROWING (6 → 48): 🚨 STOP patching
  - Decision: redesign gate. staging.* exit-code 0 (info-only).
→ Fix: non-blocking staging gate (1 PR, equivalent to #1014)      [retry #2 redesign]
staging.5 → 10/10 push success.
```

3 retries total. 1 patch-PR + 1 redesign-PR. ~3 hours saved.

---

## 7. Enforcement

### 7.1 PR-template checkbox (lands same PR via §10 below)

Add to `.github/pull_request_template.md` Output Review Checklist:
> - [ ] **Release retry budget** — if PR body mentions "staging.N" / "rc.N" with N ≥ 3 OR fixes the same CI gate as the prior 2 release-fix PRs, §3 decision flow was applied; PR body cites either §4 pivot matrix outcome OR §5 exemption trailer

### 7.2 Memory auto-load (per session)

Memory entry `feedback_release_fix_retry_budget.md` (paired same-PR) loads at session start. 4-bullet checklist before drafting any fix-PR following a release-tag failure:
1. What tag is this targeting? Is N ≥ 3?
2. What gate is failing? Same as prior 2 PRs?
3. Apply §3 decision flow before drafting fix.
4. If §4 pivot matrix says STOP → draft redesign-PR, not patch-PR.

### 7.3 Override mechanism (commit trailer)

For genuine retry #3+ exemption per §5:
```
git commit -m "...
RELEASE_RETRY_EXTERNAL_FIX: <dep-version + URL>
# OR
RELEASE_RETRY_DEADLINE_OVERRIDE: <regulator/incident + ETA>
# OR
RELEASE_RETRY_TRANSIENT: <evidence>"
```

Trailer logged in quarterly retro. Pattern frequency >5% per quarter triggers meta-review.

### 7.4 Self-test (worked example — §2 saga)

Apply rule §3 + §4 retroactively to staging.4 fail timestamp (the moment of rule violation):

- staging.3 retry #1: 6 Java CVE → .trivyignore 6 entries → patch
- staging.4 retry #2: 48 CVE across all services → §3 STOP
  - §4 pivot signal "Finding count GROWING" 6→48 → 🚨 STOP patching
  - §4 pivot signal "release-deploy-standard.md §3.1 allows softer bar for staging.*" → ✅ Lower gate
  - Decision: ship `exit-code: '0'` for staging.* (1 PR).
- Predicted outcome: staging.5 = 10/10 push success.
- Actual outcome (without rule): 3 more wasted iterations + 4 patch PRs.

→ Self-test PASS. Rule fires correctly on the originating saga. ✅

### 7.5 Detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

Future enhancement: `audit-gate.py` AUDIT_RULES rule `release-retry-budget` that:
1. Reads recent git log for last 3 release-tag commits (`v*-staging.*` / `v*-rc.*`)
2. If most-recent fix-PR + prior PR + PR-before-that all touch SAME workflow file OR SAME `.trivyignore` / similar ignore-config
3. → BLOCK new fix-PR unless commit body has `RELEASE_RETRY_*_OVERRIDE:` trailer

Defer to 2nd recurrence per premature-rule guard. Reviewer-checklist + memory auto-load + worked example sufficient for v1.0.0.

---

## 8. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Patch each new CVE/finding as it surfaces in retry #3+ | At retry #2, count findings + apply §4 pivot matrix |
| Add 1 more entry to `.trivyignore` because "it's the same pattern" | If 3+ entries with same root cause → fix root cause OR remove gate |
| Bump 1 dep version to fix 1 CVE, retry, repeat | Bump the parent BOM (Spring Boot, alpine base) once; or relax the gate for pre-release |
| Justify retry #3 with "we're so close, 1 more should do it" | Sunk-cost fallacy. The fact you're at retry #3 means initial scoping was wrong. |
| Treat `*-staging.*` and `*-rc.*` like production-grade gates | `release-deploy-standard.md` §3.1: pre-release tags get softer bars. Use them. |
| Skip §3 decision flow because "we know what the fix is this time" | At retry #2 the data ALWAYS supports redesign over patch. Run §3 anyway. |

---

## 9. Relationship to other rules

- **`release-deploy-standard.md`** §3.1 — defines per-bump-type artifact gates; this rule operationalizes "recommended không mandatory" for pre-release tags
- **`incident-to-rule-pipeline.md`** — this rule = direct output of Phase 3 staging.1-7 retro applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory + PR template + worked self-test all ship same PR
- **`gap-done-discipline.md`** — fix-PRs that would have been blocked by this rule are still valid PRs once redesign-PR ships; their gap-closure remains valid
- **`terraform-apply-retry-reconfirm.md`** — sibling rule for terraform apply retries; both belong to "retry-budget" rule family
- **`feedback_release_fix_retry_budget.md`** (memory pointer to this rule, paired same PR)

---

## 10. Log

- **2026-05-12 (v1.1.0):** MINOR — added §4 pivot matrix row "Tooling visibility gap" + §5 exception row "Tooling-fix-then-retry" with override trailer `RELEASE_RETRY_TOOLING_FIXED`. Scope extended in **Applies to** to cover deploy/mutation-op retry sequences where prior attempt failed due to observability gap. Triggered by 2026-05-12 Wave 65 deploy incident: `deploy-production.yml` poll loop showed `Status=InProgress` for ~15 attempts (2.5 min) while SSM command was actually `Failed` with `Terminated / exit status 143` at 7.88s (terraform `user_data` stop-modify-start cycle killed SSM-running deploy-prod.sh — sister rule `concurrent-production-mutation-ops.md` v1.0.0 covers the conflict cause; this v1.1.0 covers the visibility-gap retry-discipline). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged "đã có rule fix-before-retry chưa, nếu không sẽ bị fix bug vòng lặp") → Classify ✓ (existing v1.0.0 §4 covered CI gate visibility/scope; no row for workflow-poll-vs-underlying-state mismatch) → Rule+Enforce ✓ (this v1.1.0 + sister rule + paired memory + GAP-491 follow-up for CloudWatch streaming + audit artifact extension per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (today's incident — Tooling visibility gap fires; before re-trigger deploy, GAP-491 Path A CloudWatch streaming MUST land + use `RELEASE_RETRY_TOOLING_FIXED:` trailer citing fix PR + GAP-491 link) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds previously-uncovered visibility-gap trigger to existing pivot matrix; no constraint loosening for prior work; existing deploy retries grandfathered, rule applies prospectively from this PR).
- **2026-05-08 (v1.0.0):** Rule created at user request "rút kinh nghiệm cần đặt tiêu chí release, đối với những lỗi fix retry 2 lần trở lên của release thì ngay lập tức phải nghĩ đến phương án loại bỏ" — direct response to Phase 3 staging.1-7 saga (5 retries on Trivy gate; should have pivoted at retry #2 = staging.4 fail). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged retry waste pattern) → Classify ✓ (no existing rule covers retry-budget for release-tag CI gates; closest analog `terraform-apply-retry-reconfirm.md` is single-apply scope, not multi-tag-retry scope) → Rule+Enforce ✓ (this file + paired memory + PR template checkbox + worked self-test in §7.4 per §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7.4 worked example on the originating staging.4 timestamp — rule fires correctly) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint, no constraint loosening for prior work; existing fix-PRs grandfathered, rule applies prospectively). Detector wiring (§7.5) deferred per `incident-to-rule-pipeline.md` premature-rule guard until 2nd recurrence.
