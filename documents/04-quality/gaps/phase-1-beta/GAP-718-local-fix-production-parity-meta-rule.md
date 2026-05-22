---
id: GAP-718
title: META rule — local code/config fix MUST check production-equivalent surface trong cùng PR
status: OPEN
priority: P0
phase: phase-1-beta
audience: dev
found: 2026-05-22
related: [GAP-717, GAP-156]
---

# GAP-718 — META rule local-fix-production-parity-check.md

## Problem

Wave 104.5 close-loop session 2026-05-22 surfaced concrete instance of systemic meta-gap: **fix LOCAL config (`docker-compose.kitehub.yml` added `JWT_CHALLENGE_SECRET` env) WITHOUT checking production-equivalent surface (AWS Secrets Manager + Terraform IAM + deploy script env injection)**. Concrete bug GAP-717.

Per outside-in audit Wave 104.5 follow-up: no existing rule mandates this check. Existing rules cover related-but-different patterns:

| Rule | Direction | Covers Wave 104.5 case? |
|---|---|---|
| `audit-to-gap-pipeline.md` §2.5 filing-time state-check | Code-state at gap-filing | ❌ — different boundary |
| `audit-to-gap-pipeline.md` §2.6 wave-plan state-check | Symbol presence at plan-time | ❌ |
| `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync | Decision-doc → code sweep | ❌ inverse direction |
| `audit-to-gap-pipeline.md` §2.8 fix-time state-check | Verify symptom present | ❌ different question |
| `production-env-config-registry.md` §11 audit scripts | Pre-release one-off | ❌ not per-fix |
| `release-deploy-standard.md` §3.1 Secrets management | Release-time checklist | ❌ not per-fix |
| `pre-mutation-state-check.md` | PRE-mutation investigation | ❌ before-mutation, not post-fix |
| `concurrent-production-mutation-ops.md` | Concurrent op conflict | ❌ different concern |

→ **Gap confirmed.** No rule covers "local fix → production env sweep" direction.

Per `meta-gap-priority.md` §3: meta gaps fixing skills/rules/workflow get HIGHEST priority. This is META P0 — fix once → force-multiplier mọi future local fix.

## Proposed rule body

`.claude/rules/local-fix-production-parity-check.md` v1.0.0:

```markdown
# Local-Fix Production-Parity Check — code/config fix must sweep production env

**Priority:** 🟠 MANDATORY — production deploy completeness governance
**Version:** 1.0.0
**Applies to:** Every PR that introduces or modifies config-shape artifacts:
  - `application*.yml` ${VAR:default} new entries OR default value changes
  - `docker-compose*.yml` env var additions
  - `Dockerfile` new ENV/ARG directives
  - `@Value("${...}")` annotation new in Java source
  - New AWS Secrets Manager reference in code
  - New external service URL (SMTP / Resend / VietQR / OAuth provider)

## 1. The Rule

> Khi PR touches config-shape artifact LOCAL (docker-compose / application.yml / Dockerfile),
> MUST verify production-equivalent surface (terraform-aws / helm / deploy workflow / scripts)
> shipped trong CÙNG PR OR file follow-up gap với explicit completion deadline + GAP-612-style
> blocker reference nếu unblock dependency exists.

## 2. Production-equivalent surfaces

| Local source | Production equivalent | Required parity artifact |
|---|---|---|
| `docker-compose*.yml environment:` | EC2 systemd env / Helm values / Workflow env | `scripts/deploy-prod.sh` OR `infrastructure/helm/values-production.yaml` |
| `application.yml ${VAR:default}` | Spring profile `application-production.yml` OR env var injection | Per `production-env-config-registry.md` registry |
| New secret reference (e.g., new `@Value("${jwt.something}")`) | AWS Secrets Manager + Terraform IAM grant | `infrastructure/terraform-aws/secrets.tf` + `iam-deploy.tf` |
| New env var trong Dockerfile | Production deploy artifact mirror | Same as compose row |
| New external service URL config | Production endpoint configured + secret if needed | Per service: vendor-specific config docs |

## 3. Required artifacts when rule fires

Per PR touching trigger paths:

1. **Production parity table** trong PR description showing 4 columns: Local surface | Prod surface required | Same-PR? | Follow-up gap if deferred
2. **OR** follow-up gap filed với explicit deadline + acceptance criteria
3. **OR** override trailer `LOCAL_FIX_PROD_PARITY_DEFER: <reason + follow-up gap link>`

## 4. Banned shortcuts

- ❌ "Production has env var set explicitly → default doesn't matter" without documenting WHERE env set
- ❌ "Fix code only, deploy chain figure out later" without follow-up gap
- ❌ Adding `JWT_CHALLENGE_SECRET` to compose alone without Secrets Manager pathway (the 2026-05-22 Wave 104.5 self-test failure)
- ❌ "AWS suspended (GAP-612) so prod parity doesn't matter now" — code change can still ship; verification deferred

## 5. Enforcement

### 5.1 Reviewer-checklist

Pre-merge any PR touching trigger paths, reviewer asks:
- Production-equivalent surface checked?
- If new env var/secret → terraform + deploy script updated?
- If deferred → follow-up gap link present?

### 5.2 CI grep detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days)

Future heuristic: scan PR diff for additions to `docker-compose*.yml environment:` block; require co-edit of `infrastructure/terraform-aws/secrets.tf` OR `scripts/deploy-prod.sh` OR `infrastructure/helm/values-production.yaml` OR `LOCAL_FIX_PROD_PARITY_DEFER:` trailer.

### 5.3 Override mechanism

```
git commit -m "...
LOCAL_FIX_PROD_PARITY_DEFER: <which surface deferred — reason + follow-up gap link>"
```

## 6. Worked self-test — Wave 104.5 GAP-713 + JWT_CHALLENGE_SECRET incident

Apply rule retroactively to PR #1715 commit `b45f9b28`:

**Diff changed (config-shape artifacts):**
- `kitehub/kitehub-subscription/src/main/resources/application.yml` line 249 default `localhost:8083` → `kitehub-email:8080`
- `kitehub/kitehub-subscription/.../EmailConsumer.java` @Value default change
- `kitehub/kitehub-subscription/.../EmailServiceClient.java` @Value default change
- `kitehub/docker-compose.kitehub.yml` add `JWT_CHALLENGE_SECRET` env (×2 services)

**Production parity table (retroactive):**

| Local surface | Prod surface required | Same-PR? | Follow-up gap |
|---|---|---|---|
| application.yml `email.service.url` default | Prod likely has `EMAIL_SERVICE_URL` env set explicit | ⚠️ unchecked | None — likely OK but unverified |
| EmailConsumer @Value default | Same as above | ⚠️ | None |
| EmailServiceClient @Value default | Same | ⚠️ | None |
| **docker-compose `JWT_CHALLENGE_SECRET`** | **AWS Secrets Manager + IAM + deploy script** | ❌ **MISSING** | ❌ **MISSING** |

Verdict: rule fires correctly — would have caught the gap at PR review time → reviewer asks "production deploy chain JWT_CHALLENGE_SECRET injection?" → either fix same-PR OR file GAP-717 explicit. Counterfactual: ~2 weeks saved when production cutover hits 401, OR avoided silent prod-2FA failure post-AWS-restore.

## 7. Relationship to other rules

- `audit-to-gap-pipeline.md` §2.7 — inverse direction (decision-doc → code sweep); this rule covers fix-direction (code → prod env sweep)
- `production-env-config-registry.md` §11 — pre-release audit scripts; this rule per-PR check
- `release-deploy-standard.md` §3 — release artifact checklist; this rule per-fix check
- `meta-gap-priority.md` §3 — META P0 force-multiplier (this rule applies to mọi future config fix)
- `gap-done-discipline.md` §3 — PARTIAL exit ramp if deferred follow-up gap mandatory
- `rule-change-process.md` §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test ship same PR
- `incident-to-rule-pipeline.md` 5-stage — this rule = direct output 2026-05-22 user-flagged meta gap
- Memory `feedback_local_fix_production_parity.md` (paired same-PR)

## 8. Self-test verification

Wave 105 Bucket E0 — concrete fix GAP-717 + ship this rule. Self-test §6 retroactive on Wave 104.5. Rule applies prospectively to Wave 105+ all config fixes.

## 9. Log

- **2026-05-22 (v1.0.0):** Rule created in response to user-flagged meta gap Wave 104.5 close-loop 2026-05-22: "ngoài ra check meta có audit cho việc fix local thì phải check fix env cho cả production chưa?". Concrete bug GAP-717 (JWT_CHALLENGE_SECRET local-only) surfaced same session as instantiation. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ → Classify ✓ (no existing rule covers code-fix → prod-env-sweep direction) → Rule+Enforce ✓ (this rule + reviewer-checklist + GAP-717 paired same Wave 105) → Self-Test ✓ (§6 retroactive Wave 104.5 — rule fires correctly) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint, no constraint loosening; existing PRs grandfathered; rule applies prospectively từ Wave 105+).
```

## Acceptance Criteria

- [ ] `.claude/rules/local-fix-production-parity-check.md` v1.0.0 shipped với 9 sections
- [ ] Memory `feedback_local_fix_production_parity.md` paired same-PR (per Enforcement Parity Mandate)
- [ ] §3 production-equivalent surfaces table covers 5 trigger paths
- [ ] §6 worked self-test retroactive on Wave 104.5 GAP-717 incident
- [ ] `output-review-mandate.md` §3 matrix row added cho "Local fix production parity"
- [ ] `rules-index.csv` row added
- [ ] Cross-link updates trong related rules (`audit-to-gap-pipeline.md` §2.7 + `production-env-config-registry.md` §11)
- [ ] Wave 105 Bucket E0 ship cùng PR với GAP-717 fix

## Related

- Concrete sister: GAP-717 (instance of pattern this rule prevents)
- Sister rule precedent: `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync (inverse direction)
- Meta-priority: `meta-gap-priority.md` §3 — META P0 force-multiplier
- Enforcement parity: `rule-change-process.md` §6.5 — rule + memory + self-test same PR
- Triggered by: user-flagged meta question Wave 104.5 close-loop 2026-05-22
- Wave 105 plan: `documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md` — amended add Bucket E0 priority
