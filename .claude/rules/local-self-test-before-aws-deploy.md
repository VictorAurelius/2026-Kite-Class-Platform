---
paths:
  - ".github/workflows/deploy-production.yml"
  - ".github/workflows/terraform-apply.yml"
  - ".github/workflows/rollback.yml"
  - "scripts/deploy-prod.sh"
  - "kitehub/scripts/up.sh"
  - "scripts/local/**"
---

# Local Self-Test Before AWS Deploy

**Priority:** 🟠 MANDATORY — pre-deploy gate
**Version:** 1.0.0
**Created:** 2026-05-21
**Last-Reviewed:** 2026-05-21
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (memory mirror + reviewer-checklist + worked self-test trên Wave 71b admin-login retroactive) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies local-self-test mandate Phase 0C following GAP-694 Phase 0A+0B groundwork)
**Applies to:** Mọi PR / session trigger AWS deploy workflow (deploy-production.yml, terraform-apply.yml, rollback.yml, SSM SendCommand prod scope). Out-of-scope: pure docs PR, internal config refactor, local dev iteration không touch deploy.

---

## 1. The Rule

> **Trước mọi trigger AWS deploy workflow** (`deploy-production.yml` / `terraform-apply.yml dry_run=false` / `rollback.yml` / `aws ssm send-command` prod scope), **`bash kitehub/scripts/up.sh --profile <profile>` + `bash scripts/local/smoke-e2e.sh` PHẢI PASS local first**. Mandate full local stack health + admin-flow walk per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g) trước khi production touch.

Wave 102.8 Bucket A (2026-05-21) đã ship `kitehub/scripts/check-docker.sh` preflight + integrate vào `up.sh`/`setup.sh`. Wave 102.8 Bucket B đã ship env-reference tooling. Phase 0C này codify rule mandate **local-pass-first → AWS-trigger** để 3-stack groundwork (Bucket A + B + C) compound thành actionable gate.

Rationale: bug class invisible to inside-out CI tests (H2 + Mockito stub không match Postgres + production secrets behavior) chỉ surface post-deploy trên real infra. Rule này push surfacing point từ "post-deploy 500" về "pre-trigger local stack walk" — eliminate user round-trip cost + recover from prod incident retroactively.

---

## 2. Scope clarification

### 2.1 In scope (rule fires)

| Trigger | Why |
|---|---|
| `gh workflow run deploy-production.yml` | Production EC2 + RDS mutation |
| `gh workflow run terraform-apply.yml dry_run=false` | Production infra mutation per `aws-observability-first.md` + `pre-mutation-state-check.md` |
| `gh workflow run rollback.yml confirm=APPLY` | Production rollback service swap |
| `aws ssm send-command` prod scope (per `agent-aws-access.md` §4.3 Tier 3 banned-without-confirm) | Production fleet mutation |

### 2.2 Out of scope (rule N/A)

| Case | Why |
|---|---|
| Tier 1 read-only AWS ops per `agent-aws-access.md` §2.1 | No mutation, no local-gate need |
| Docs-only PR per `docs-only-pr-auto-merge.md` §2 | No deploy effect |
| Local dev iteration (no AWS touch) | Loop runs locally already |
| `terraform-apply.yml dry_run=true` | Plan-only, no mutation |
| Hotfix CVE incident response | Per §5 override mechanism, `LOCAL_SMOKE_SKIP` trailer required với retro per `release-fix-retry-budget.md` §5 |

---

## 3. Required local gate sequence

Trước mọi §2.1 trigger, three-step sequence MUST complete với evidence:

### Step 1 — Stack up

```bash
bash kitehub/scripts/up.sh --profile infra-only
# Minimum 4 infra services healthy (kite-postgres + kite-redis + kite-rabbitmq + kite-minio)
# Verify: docker ps → 4+ services UP healthy
```

Use `--profile full` nếu FE/BE code changed (boots all microservices + frontends per Wave 37 GAP-407 profile system).

### Step 2 — Smoke E2E

```bash
bash scripts/local/smoke-e2e.sh
# Covers admin login + critical user flow per pre-handoff-self-test-completeness.md §2.4 (a)→(g)
```

**NOTE v1.0.0:** Script `scripts/local/smoke-e2e.sh` chưa tồn tại (tracked follow-up GAP-XXX). Until shipped: manual admin login walk per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g) checklist là acceptable substitute với commit-body evidence cite từng checklist row PASS.

### Step 3 — Document outcome

PR body OR commit body PHẢI include section:

```markdown
## Local self-test evidence

- Profile run: `bash kitehub/scripts/up.sh --profile infra-only`
- Services healthy: 4/4 (kite-postgres + kite-redis + kite-rabbitmq + kite-minio Up <N>s healthy)
- Admin login flow: (a) credential available ✓ (b) curl POST /api/auth/login → 200 + JWT ✓
  (c) Login UI redirects → /admin ✓ (d) Admin role-guard accepts ✓ (e) Nav to /admin/beta-requests ✓
  (f) Page renders với data ✓ (g) Approve action → 200 + UI update ✓
- Smoke script: `bash scripts/local/smoke-e2e.sh` exit 0 (HOẶC: manual walk evidence cited per §2.4 a→g)
```

Banned shortcut: "trust me, ran locally" without evidence in PR body OR commit body. Override per §5 below.

---

## 4. Banned patterns

| ❌ Don't | ✅ Do |
|---|---|
| Trigger `deploy-production.yml` without local stack up | `up.sh --profile <required>` first; verify services healthy |
| "Skipped local because Docker unreachable" | Fix Docker via `kitehub/scripts/check-docker.sh` per Bucket A (Wave 102.8); rule không exempt for tooling-state |
| Smoke E2E "I'll run it in CI" | CI smoke runs AFTER deploy — không phải BEFORE. Local PRE-deploy gate là non-negotiable |
| Trigger `rollback.yml` without verifying old image runs locally first | Pull old image SHA via `docker pull`, run, verify health before rollback trigger |
| Use this rule as excuse to defer feature work | Local self-test = ~3 min wall-clock sau Bucket A landed; không phải blocker |
| Document outcome in chat message only | PR body OR commit body — repo-tracked evidence |
| Combine local gate với mutation trigger trong same PR | Local gate evidence MUST exist trước workflow trigger (sequential gate, không parallel) |

---

## 5. Override mechanism

Genuine exception (vd P0 prod incident, local Docker broken):

```
git commit -m "...
LOCAL_SMOKE_SKIP: <gate-step — e.g., 'stack-up' / 'smoke-e2e' / 'admin-walk'> — <reason — e.g., 'P0 prod incident, local Docker broken, fix queued GAP-XXX'>
LOCAL_SMOKE_FOLLOWUP: <gap link scheduling local verify within Ndays>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review (likely scope mis-defined hoặc local stack health regression class). Per `release-fix-retry-budget.md` §5 row "Tooling-fix-then-retry" — nếu local gate skip due to observability gap, fix observability FIRST then retry without skip.

---

## 6. Worked self-test — Wave 71b admin-login incident (retroactive)

**Scenario:** Wave 71b 2026-05-13 closure flipped GAP-509/512/513 → DONE based on `curl POST /api/auth/login` HTTP 200. Live user attempt revealed 3 bugs:

1. No UI button visible — user phải guess URL `/admin/beta-requests`
2. Direct URL → redirect `/login` → no admin credential trong handoff
3. After credential retrieved manually từ AWS, login succeeded → redirects `/dashboard` (NOT `/admin`) → `/admin/*` routes blocked by role-guard mismatch (BE seeded `PLATFORM_ADMIN` vs FE guard literal `'ADMIN'`)

Per `pre-handoff-self-test-completeness.md` §6 worked self-test, all 3 bugs surfaced ONLY at live walkthrough — endpoint-level `curl 200` insufficient.

**Apply rule §3 retroactively:**

| Step | Status retroactive | Outcome |
|---|---|---|
| Step 1 (stack up) | Would have required `bash kitehub/scripts/up.sh --profile full` — agent at the time worked off staging EC2 không local stack | Stack-up gate would have FAILED to find local stack → §5 trailer mandate forces explicit `LOCAL_SMOKE_SKIP: stack-up — agent on staging not local` documentation → reviewer flag |
| Step 2 (smoke E2E with admin walk) | Manual walk per §2.4 (a)→(g) would surface (e) "Nav to /admin/beta-requests" FAIL (no sidebar link per GAP-519) + (d) "Admin role-guard accepts" FAIL (role mismatch per GAP-518) | 2 of 3 Wave 71b bugs caught locally |
| Step 3 (document outcome) | Would have caught absent admin credential trong handoff (1 of 3 bugs caught at evidence-write time) | All 3 bugs surface BEFORE flip DONE |

**Counterfactual:** 3 bugs caught locally pre-deploy → 0 user round-trip + 0 retroactive gap filing (GAP-518/519 prevented). Rule fires correctly on the originating incident. Self-test PASS ✅

**Cost-save:** ~30 min user round-trip × 1 incident + 2 retro gap filings (~45 min) = ~75 min eliminated per pattern recurrence. Compound force-multiplier across every future deploy session.

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho PR triggering §2.1 AWS deploy workflow:

- [ ] PR body OR commit body contains `## Local self-test evidence` section per §3 Step 3?
- [ ] Stack-up evidence cited (profile + services healthy count)?
- [ ] Admin walk evidence cited per `pre-handoff-self-test-completeness.md` §2.4 (a)→(g)?
- [ ] Smoke script OR manual walk evidence (script chưa tồn tại v1.0.0 — manual walk acceptable)?
- [ ] Nếu absent local evidence + no `LOCAL_SMOKE_SKIP:` trailer → flag + block

### 7.2 Memory mirror (paired same-PR)

Memory entry `feedback_local_self_test_before_aws_deploy.md` text shipped trong PR body inline per `post-merge-sync-completeness.md` §7.5 (user copy-paste to user-memory dir + update MEMORY.md index).

### 7.3 Detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày)

Future enhancement — `audit-gate.py` hook scan PR body / commit message for `gh workflow run (deploy-production|terraform-apply|rollback)` invocations → require either `## Local self-test evidence` section trong PR body OR `LOCAL_SMOKE_SKIP:` trailer. Defer until 2nd recurrence per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions:

- **Detector complexity:** scanning PR body + commit message + workflow trigger commands requires multi-source grep + cross-reference logic, NOT trivial <50 LOC bash
- **Recurrence count:** 0 post-merge (rule shipped 2026-05-21, ~0 days)
- **Honest defer:** reviewer-checklist §7.1 + worked self-test §6 + paired memory §7.2 sufficient cho v1.0.0; revisit detector when recurrence-count ≥2 OR after `scripts/local/smoke-e2e.sh` ships (enables auto-verification of evidence claim)

### 7.4 Cross-link with `aws-observability-first.md` v1.0.0

`aws-observability-first.md` mandate CloudTrail BEFORE infra apply (audit baseline). This rule mandates local-pass BEFORE deploy trigger (functional baseline). Compound layered gates: CloudTrail trail-status reachable + local stack healthy = pre-deploy fully gated.

---

## 8. Self-test fixture

3 PR body scenarios + rule verdict trong `_examples/local-self-test-self-test-fixture.md` (paired same-PR per §7.1 reviewer-checklist self-test mandate).

---

## 9. Relationship to other rules

- **`pre-handoff-self-test-completeness.md`** v1.1.1 §2.4 — admin-flow checklist (this rule's local equivalent — enforces ALL §2.4 (a)→(g) trước trigger AWS deploy)
- **`agent-action-bias.md`** v1.0.1 §1 Part A "do it yourself" — this rule extends "verify it locally yourself before AWS"
- **`aws-observability-first.md`** v1.0.0 — CloudTrail BEFORE infra apply; this rule = local-pass BEFORE deploy trigger (compound layered gates)
- **`pre-mutation-state-check.md`** v1.2.0 — pre-mutation audit artifact mandate; this rule adds local-state-check layer
- **`release-deploy-standard.md`** v1.2.0 §3.1 — PRE-RELEASE "Smoke admin-login" mandate; this rule's local-pass-first = pre-trigger version của post-deploy smoke
- **`concurrent-production-mutation-ops.md`** v1.0.0 — serialize mutations; this rule adds local-gate as pre-step trong sequential serialization
- **`release-fix-retry-budget.md`** v1.1.0 — fix discipline + Tooling-fix-then-retry exception; this rule prevents deploy-fix-retry cycle by catching at local
- **`admin-merge-discipline.md`** v1.0.1 — `--admin` BANNED post-rebase; complementary pre-merge gate
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory + worked self-test paired same PR
- **`incident-to-rule-pipeline.md`** v1.1 5-stage applied: Detect ✓ (outside-in synthesis Wave 102.8 surfaced local-self-test mandate gap post Wave 71b admin-login incident) → Classify ✓ (no existing rule mandates local-pass-first → AWS-trigger; sister rules cover post-mutation audit, observability baseline, pre-mutation state-check, retry budget, admin-flow at handoff time — none cover pre-trigger local gate) → Rule+Enforce ✓ (this file + paired same-PR memory entry text in PR body per `post-merge-sync-completeness.md` §7.5 + worked self-test §6 retroactive Wave 71b admin-login per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on Wave 71b incident — rule fires correctly + counterfactual eliminates 1 user round-trip + 2 retro gap filings) → Retro Log ✓ (this entry)

---

## 10. Log

- **2026-05-21 (v1.0.0):** Rule created — Wave 102.8 Bucket C closing GAP-694 Phase 0C following Bucket A (`check-docker.sh` preflight + `kitehub/.env` populate) + Bucket B (env-reference tooling) groundwork. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (outside-in synthesis 2026-05-21 surfaced local-self-test mandate gap post Wave 71b admin-login incident pattern — surface point pushed từ "post-deploy 500" về "pre-trigger local stack walk") → Classify ✓ (no existing rule mandates local-pass-first → AWS-trigger; sister rules cover post-mutation audit (`pre-mutation-state-check.md`), observability baseline (`aws-observability-first.md`), retry budget (`release-fix-retry-budget.md`), admin-flow at handoff time (`pre-handoff-self-test-completeness.md`) — none cover pre-trigger local gate) → Rule+Enforce ✓ (this file + paired same-PR: memory `feedback_local_self_test_before_aws_deploy.md` text inline PR body per `post-merge-sync-completeness.md` §7.5 + worked self-test §6 retroactive Wave 71b admin-login + self-test fixture `_examples/local-self-test-self-test-fixture.md` + rules-index.csv row + GAP-694 closure flip PARTIAL 75% → DONE 100% + git mv to closed/ per `gap-folder-organization.md` v2.0.0 §3.3 + CSV `filename` column sync per `gap-architecture-v2.md` §3 per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on Wave 71b incident — rule fires correctly + counterfactual eliminates 1 user round-trip + 2 retro gap filings GAP-518/519; §8 fixture 3 scenarios PASS/FAIL/WARN demonstrates detector intent) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-implicit local-gate mandate; no constraint loosening for prior deploy PRs; existing deploy PRs grandfathered; rule applies prospectively từ Wave 102.8 forward). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: local-pass before AWS-trigger) + ✅ unique (no overlap với sister rules covering different time-windows) + ✅ widely applicable (every deploy session) + ✅ body discipline §1 has ≤2 "and" conjunctions. Detector wiring (§7.3) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (complexity + recurrence + honest-defer cited inline); reviewer-checklist + worked self-test + memory mirror sufficient cho v1.0.0. Path-scoped per `context-budget-mandate.md` §3.1 — `paths:` frontmatter auto-load only on deploy/local-stack file context (saves session budget khi non-deploy work).
