---
paths:
  - "**/application*.yml"
  - "**/docker-compose*.yml"
  - "**/Dockerfile*"
  - "infrastructure/terraform-aws/**"
  - "infrastructure/helm/**"
  - "scripts/fetch-secrets.sh"
  - "**/.env*.template"
---

# Local-Fix Production-Parity Check — code/config fix must sweep production env

**Priority:** 🟠 MANDATORY — production deploy completeness governance
**Version:** 1.1.0
**Created:** 2026-05-22
**Last-Reviewed:** 2026-06-19
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.1.0 MINOR self-approve per `rule-change-process.md` §5; adds §2.5 "value-resolution dimension — multi-host topology config" closing config-bug campaign #2492-2496 gap (G2★ local đơn-host KHÔNG tái hiện topology 3-EC2 → cross-host config-bug rơi vào no-gate vùng → chỉ lòi khi deploy AWS). Paired same-PR với G3-config gate split trong `flow-verification-campaign.md` §1 + registry `documents/05-guides/deploy/prod-deploy-config-registry.md` + smoke gate `scripts/smoke-prod-config.sh` per §6.5 Enforcement Parity Mandate; no constraint loosening — adds deploy-time value-resolution layer complementary to existing §2 PR-time shape-parity. META P1 force-multiplier per `meta-gap-priority.md` §3. v1.0.1 (kept): new rule với built-in enforcement (reviewer-checklist + memory mirror inline PR body + worked self-test retroactive Wave 81 + Wave 104.5 same incident class) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "local code/config fix → production-equivalent surface sweep" complementary to `audit-to-gap-pipeline.md` §2.7 inverse direction)

**Applies to:** Every PR introducing or modifying config-shape artifacts:
- `application*.yml` `${VAR:default}` new entries OR default value changes
- `docker-compose*.yml` `environment:` block additions
- `Dockerfile*` new `ENV`/`ARG` directives
- `@Value("${...}")` annotation new in Java source
- New AWS Secrets Manager reference in code
- New external service URL config (SMTP / Resend / VietQR / OAuth provider)
- New `.env*.template` env var declaration

---

## 1. The Rule

> **Khi PR touches config-shape artifact LOCAL (docker-compose / application.yml / Dockerfile / @Value / .env.template), PHẢI verify production-equivalent surface (terraform-aws / helm / deploy workflow / scripts / docker-compose.production.yml / Secrets Manager IaC) shipped trong CÙNG PR HOẶC follow-up gap với explicit completion deadline + GAP-blocker reference nếu unblock dependency exists.**

Sister direction của `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync. That rule covers `decision-doc → code-sweep` direction (e.g., domain decision lands → grep stale code refs). This rule covers `code/config-fix → prod-env-surface-sweep` direction (e.g., new `JWT_CHALLENGE_SECRET` env var lands locally → check AWS Secrets Manager + Terraform IaC + deploy script + Helm).

Force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn check → mọi future code/config fix subsequent auto-comply → eliminate retroactive "code shipped local fine, deploy chain broken" round-trip cost.

---

## 2. Production-equivalent surfaces

For each local config-shape artifact class, the production-equivalent surface PHẢI be checked + parity ship same-PR:

| Local source | Production equivalent surface | Required parity artifact |
|---|---|---|
| `kitehub/docker-compose.kitehub.yml` `environment:` block addition | EC2 systemd env / Helm values / Workflow env / `docker-compose.production.yml` | `scripts/deploy-prod.sh` + `scripts/fetch-secrets.sh` + `docker-compose.production.yml` (root) + `infrastructure/helm/values-production.yaml` |
| `application.yml ${VAR:default}` new entry | Spring profile `application-production.yml` OR runtime env var injection | Per `production-env-config-registry.md` §3 registry update |
| New `@Value("${jwt.something}")` annotation in Java source | AWS Secrets Manager secret + Terraform IaC declaration + IAM grant | `infrastructure/terraform-aws/secrets.tf` (`random_password` + `aws_secretsmanager_secret` + `aws_secretsmanager_secret_version`) + IAM grant via `iam.tf` (wildcard pattern OR explicit ARN) + `scripts/fetch-secrets.sh` fetch line + env-vars-registry row |
| New `ENV` directive in `Dockerfile*` | Production deploy artifact mirror | Same as docker-compose row above |
| New external service URL config (Resend / VietQR / OAuth) | Production endpoint configured + secret if needed | Per service: vendor-specific config docs + Secrets Manager IaC if API key required |
| New `.env*.template` declared env var | Production secret/config provisioning path | Either §1 + §2 above per nature (secret vs public config) |

### 2.1 Banned surface gap pattern

Most common missed surface = **AWS Secrets Manager IaC declaration when secret was created manually via runbook**. Example (Wave 81 incident): secret `kitehub/production/jwt-challenge-secret` created manually 2026-05-15 via Wave 81 jwt-secret-fix-runbook; `scripts/fetch-secrets.sh` updated to fetch it; `env-vars-registry.md` documented it; BUT `infrastructure/terraform-aws/secrets.tf` NEVER got the resource declaration → IaC drift latent until next teardown/restore.

Rule explicitly flags this: **manual secret creation per runbook = local-fix-equivalent**; terraform IaC declaration MUST be paired same-PR OR file follow-up gap (Wave 105 GAP-717 pattern).

---

## 2.5 Value-resolution dimension — multi-host topology config (added v1.1.0)

§2 above kiểm **config-SHAPE parity** tại PR-time: artifact production-equivalent CÓ tồn tại không (terraform/helm/compose/fetch-secrets). NHƯNG shape đúng ≠ value đúng: env var `KITECLASS_CORE_URL` CÓ trong compose (shape OK) nhưng VALUE `http://10.0.0.155:8081` chỉ resolve đúng trên **topology 3-EC2 thật** — single-host Docker local KHÔNG tái hiện được cross-host routing. Class bug này (config-bug campaign #2492-2496) chỉ manifest lúc deploy AWS, KHÔNG bắt được bằng shape-parity ở PR-time.

> **Khi PR touches config phụ-thuộc-topology (cross-host URL trỏ private IP / SG self-ref / nginx per-tenant proxy / S3 IAM-role / secret-fetch `/etc/kite/.env` keys / EC2 bash quirk / prod-profile env), PHẢI: (a) đăng ký config-điểm đó vào registry `documents/05-guides/deploy/prod-deploy-config-registry.md` (single source of truth: artifact / local-value / prod-value / derive-method / smoke-check), VÀ (b) đảm bảo có 1 smoke-check tương ứng trong `scripts/smoke-prod-config.sh` (G3-config gate).**

Khác biệt 2 tầng:

| Tầng | Kiểm gì | Khi nào | Cơ chế |
|---|---|---|---|
| **§2 shape-parity** | Artifact production-equivalent có tồn tại (terraform/helm IaC declaration) | PR-time | Reviewer-checklist + grep |
| **§2.5 value-resolution** | Value resolve đúng trên topology 3-EC2 thật (cross-host reach / route / IAM / secret) | Deploy-time (sau `terraform apply` + push) | Smoke gate `scripts/smoke-prod-config.sh` (G3-config per `flow-verification-campaign.md` §1) + registry |

Force-multiplier: 1 registry + smoke gate → biến "fix config từng cái khi deploy lòi" thành "smoke → bảng PASS/FAIL → batch fix". ⚠️ Class này TÁI DIỄN mỗi lần redev `terraform apply` (instance-ID/private-IP/RDS-endpoint MỚI) → registry + smoke trả lãi ngay lần redev kế.

---

## 3. Required artifacts when rule fires

For PR touching §Applies-to trigger paths, ONE of three outcomes MUST hold:

### 3.1 Same-PR parity table (preferred)

PR description (or commit body) contains a `## Production parity` table showing each local surface + production equivalent + ship status:

```markdown
## Production parity

| Local surface | Prod surface required | Same-PR? | Follow-up gap if deferred |
|---|---|---|---|
| application.yml `email.service.url` default | EMAIL_SERVICE_URL env in docker-compose.production.yml | ✅ Wave 104.5 PR #1715 | — |
| docker-compose.kitehub.yml JWT_CHALLENGE_SECRET env | terraform-aws secrets.tf jwt_challenge_secret resource | ✅ this PR Bucket E0 | — |
| fetch-secrets.sh fetch_secret line | IAM grant + AWS Secrets Manager resource | ✅ pre-existing (wildcard IAM) | — |
```

### 3.2 Follow-up gap filed same PR

When parity can't ship same-PR (vendor delay, AWS account suspended per GAP-612, scope split):

- File follow-up gap với explicit:
  - Deadline (date OR "post-<blocker-gap> unblock")
  - Acceptance criteria mirroring missing parity items
  - Block dependency reference if applicable
- Link gap from current PR description

Example (Wave 104.5 → Wave 105 cascade):
- Wave 104.5 PR #1715 added `JWT_CHALLENGE_SECRET` to local compose
- Missing: terraform IaC declaration in `secrets.tf`
- Filed: GAP-717 (P1) Wave 105 Bucket E0 ship target

### 3.3 Override trailer

Genuine exception (regulator deadline, P0 hotfix, vendor-only path):

```
git commit -m "...
LOCAL_FIX_PROD_PARITY_DEFER: <which surface deferred — reason + follow-up gap link>"
```

Trailer logged in quarterly retro. Pattern frequency >5%/quarter triggers meta-review.

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| "Production has env var set explicitly via runbook → default doesn't matter" without documenting WHERE env set | Document override mechanism per `production-env-config-registry.md` §4 + ship IaC equivalent |
| Fix code only, "deploy chain figure out later" without follow-up gap | File follow-up gap per §3.2 with explicit acceptance criteria |
| Add `JWT_CHALLENGE_SECRET` to local compose alone without Secrets Manager IaC pathway | Pair §2 row 3 artifacts (terraform secret + IAM + fetch script + env-vars-registry) |
| "AWS suspended (GAP-612) so prod parity doesn't matter now" — skip IaC silently | Ship IaC code change ngay (no AWS access required for IaC source edit); verification deferred to post-restore — explicit blocker reference |
| Create secret manually via AWS console / runbook without filing terraform IaC follow-up | Manual creation = same-PR follow-up gap OR same-PR IaC declaration with `terraform import` runbook |
| Trust that "env_file passthrough handles new env vars automatically" without verifying compose actually uses `env_file:` directive | Verify production compose `env_file: /etc/kite/.env` covers new var OR add explicit `environment:` block |
| Update local `.env.template` but skip production secret seeding runbook update | Both files updated same PR — local template documents shape, runbook documents production fetch path |

---

## 5. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 5.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching §Applies-to trigger paths:

- [ ] PR touches local config-shape artifact (compose / application.yml / Dockerfile / @Value / .env.template)?
- [ ] Nếu CÓ — production-equivalent surface checked per §2 matrix?
- [ ] §3.1 parity table present in PR description? OR §3.2 follow-up gap filed? OR §3.3 override trailer?
- [ ] Specific check: new env var → terraform secret resource declared OR follow-up?
- [ ] Specific check: new @Value annotation → IAM grant exists (wildcard pattern OR explicit) + fetch-secrets.sh fetch line?
- [ ] Cross-reference với `production-env-config-registry.md` §3 registry?

### 5.2 Memory mirror (paired same-PR)

Memory entry `feedback_local_fix_production_parity.md` text shipped inline trong PR body per `post-merge-sync-completeness.md` §7.5 (user copy-paste to user-memory dir + update MEMORY.md index).

### 5.3 CI grep detector (deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions)

Per Wave 99C META-META GAP-675 audit tightened §3.1 legitimate-deferral conditions:
- **Detector complexity:** scanning PR diff for additions to `docker-compose*.yml environment:` block + cross-reference với `infrastructure/terraform-aws/secrets.tf` co-edit OR follow-up gap link requires multi-source diff parsing + cross-reference logic, NOT trivial <50 LOC bash
- **Recurrence count:** 1 pre-rule (Wave 104.5 GAP-717 surfaced 2026-05-22; Wave 81 GAP-509 = same pattern recurrence but pre-rule landed)
- **FP risk:** Moderate — heuristic regex may false-positive on legitimate cases (e.g., dev-only env var addition that genuinely has no prod equivalent)
- **Decision:** Reviewer-checklist §5.1 + worked self-test §6 (Wave 81 + Wave 104.5 retroactive both same incident class) + memory mirror §5.2 sufficient cho v1.0.0; revisit detector when recurrence-count ≥2 post-rule landing OR proven diff parser available

Future heuristic regex (when implemented, WARN-mode):

```bash
# Detect new env var additions in local compose without matching prod IaC change
git diff --name-only origin/main...HEAD | grep -qE "docker-compose\.kitehub\.yml$" \
  && ! git diff --name-only origin/main...HEAD | grep -qE "infrastructure/terraform-aws/secrets\.tf$" \
  && ! git log origin/main..HEAD --format=%B | grep -q "LOCAL_FIX_PROD_PARITY_DEFER:" \
  && { echo "WARN: local compose changed without terraform IaC parity — verify per local-fix-production-parity-check.md §2"; exit 0; }
```

WARN-only initially. Track follow-up gap khi stabilize.

### 5.4 Override mechanism

Per §3.3 override trailer:

```
git commit -m "...
LOCAL_FIX_PROD_PARITY_DEFER: <surface — reason — follow-up gap>"
```

Common valid override cases:
- AWS account suspended (GAP-612 class) → defer verification but ship IaC code change
- Vendor template English-only / regulator-required hardcoded value
- Phase 1 BETA acceptable scope explicitly documented in rule §5 of `production-env-config-registry.md`

---

## 6. Worked self-test — Wave 81 + Wave 104.5 + GAP-717 same incident class

### 6.1 Wave 81 GAP-509 (2026-05-15) — original incident, pre-rule

**Trigger:** Wave 79 Bucket C `ChallengeTokenService.@PostConstruct` fail-fast guard requires `JWT_CHALLENGE_SECRET` non-empty in production profile.

**Local fix (Wave 81 Bucket F PR #1388):**
- `scripts/fetch-secrets.sh` line 72 — added `JWT_CHALLENGE_SECRET=$(fetch_secret jwt-challenge-secret)`
- `scripts/fetch-secrets.sh` line 157 — added `JWT_CHALLENGE_SECRET=${JWT_CHALLENGE_SECRET}` to `/etc/kite/.env` template
- `documents/02-architecture/env-vars-registry.md` line 43 — documented secret + manual creation note
- Wave 81 jwt-secret-fix-runbook → manual `aws secretsmanager create-secret --name kitehub/production/jwt-challenge-secret` via runbook

**Production surfaces missing parity (pre-rule, undetected):**

| Surface | Status |
|---|---|
| `infrastructure/terraform-aws/secrets.tf` `aws_secretsmanager_secret.jwt_challenge` resource | ❌ MISSING — manual creation = IaC drift |
| `infrastructure/terraform-aws/iam.tf` grant | ✅ wildcard `${project}/${env}/*` pattern covers — no edit needed |
| Live verify post-creation | ✅ done via Wave 81 runbook |

**Cost of missing IaC declaration:** silent IaC drift latent for ~7 days until Wave 104.5 surfaced when user inspected. Next infrastructure teardown/restore (e.g., post-GAP-612 AWS account restore) will require manual import OR re-creation OR terraform will try to create duplicate (rejected by AWS).

### 6.2 Wave 104.5 PR #1715 (2026-05-22) — recurrence, pre-rule

**Trigger:** Wave 104.5 Bucket E added `JWT_CHALLENGE_SECRET` env to `kitehub/docker-compose.kitehub.yml` (local dev compose) commit `b45f9b28`.

**Apply rule retroactively (counterfactual):**

| Local surface | Prod surface required | Same-PR? | Verdict |
|---|---|---|---|
| docker-compose.kitehub.yml JWT_CHALLENGE_SECRET env (local dev) | docker-compose.production.yml `env_file: /etc/kite/.env` passthrough | ✅ pre-existing (file root level uses env_file passthrough) | OK |
| docker-compose.kitehub.yml JWT_CHALLENGE_SECRET env | scripts/fetch-secrets.sh fetch line | ✅ pre-existing (Wave 81 PR #1388) | OK |
| docker-compose.kitehub.yml JWT_CHALLENGE_SECRET env | **infrastructure/terraform-aws/secrets.tf jwt_challenge_secret resource** | ❌ **MISSING — same gap as Wave 81 6.1** | FILE FOLLOW-UP |

**Cost of recurrence:** ~1 turn user inspection + Wave 105 Bucket E0 scope expansion to ship IaC + META rule.

### 6.3 Wave 105 Bucket E0 (this PR) — rule landing + closure

**Apply rule prospectively:**
- Ship META rule (this file) — force-multiplier prevention
- Ship IaC declaration `random_password.jwt_challenge_secret` + `aws_secretsmanager_secret.jwt_challenge_secret` + `aws_secretsmanager_secret_version.jwt_challenge_secret` in `infrastructure/terraform-aws/secrets.tf` matching existing pattern (jwt/encryption/seed_admin with `lifecycle ignore_changes`)
- Document `terraform import aws_secretsmanager_secret.jwt_challenge_secret kitehub/production/jwt-challenge-secret` requirement post-AWS-restore (per GAP-612 unblock)
- Update env-vars-registry.md row 43 with Wave 105 IaC parity note
- GAP-717 close (PARTIAL — code+IaC shipped, live verify deferred GAP-612)
- GAP-718 close (DONE — rule shipped)

**Verdict:** Rule fires correctly on 2 retroactive incidents (Wave 81 + Wave 104.5 same class). Counterfactual: rule landed Wave 81 would have caught IaC drift at PR #1388 time → no Wave 104.5 recurrence → no Wave 105 Bucket E0 META scope expansion. Self-test PASS ✅

**Cost-save projection:** 1 production hotfix cycle eliminated per future recurrence (~30 min user round-trip + ~1h IaC retro work per occurrence). Annual estimate: ~3-5 occurrences/year × 1.5h = ~5-8 hours/year saved + 0 production incidents from IaC drift class.

---

## 7. Relationship to other rules

- **`audit-to-gap-pipeline.md`** §2.7 Decision-Doc Code-Sync — INVERSE direction (decision-doc → code-sweep); this rule covers code-fix → prod-env-surface-sweep direction. Sister rules at different boundaries.
- **`production-env-config-registry.md`** v1.1.1 §3 registry + §11 4 audit scripts — runtime env-var coverage; this rule extends to per-PR check at fix-time (registry catches at pre-release tag boundary; this rule catches at PR boundary)
- **`pre-mutation-state-check.md`** v1.2.0 — pre-mutation audit artifact mandate; complementary at deploy-trigger time
- **`agent-aws-access.md`** v1.0.3 §4.3 Tier 3 banned — manual secret creation per runbook (Tier 2 always-confirm); this rule mandates terraform IaC follow-up after Tier 2 confirm
- **`release-deploy-standard.md`** v1.2.0 §3.1 — pre-release secrets management checklist; this rule operationalizes per-PR check
- **`gap-done-discipline.md`** v1.0.1 §3 PARTIAL exit ramp — when local fix ships but prod parity deferred, gap stays PARTIAL until parity ships
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (this rule applies to mọi future config fix subsequent)
- **`post-merge-sync-completeness.md`** v1.0.1 §2 — 4-target sync; this rule complementary to target 1 (CSV) when gap status changes
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 + memory mirror paired same PR
- **`incident-to-rule-pipeline.md`** v1.1 5-stage applied:
  - Detect ✓ (user-flagged meta gap Wave 104.5 close-loop 2026-05-22: "ngoài ra check meta có audit cho việc fix local thì phải check fix env cho cả production chưa?")
  - Classify ✓ (no existing rule covers code-fix → prod-env-sweep direction; sister rules cover related-but-different patterns per GAP-718 §Problem table)
  - Rule+Enforce ✓ (this file + concrete GAP-717 fix + memory mirror inline PR body + worked self-test §6 retroactive Wave 81 + Wave 104.5 paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate)
  - Self-Test ✓ (§6 worked example on 2 retroactive incidents Wave 81 + Wave 104.5 — rule fires correctly + counterfactual eliminates recurrence + force-multiplier projection)
  - Retro Log ✓ (§8 below + GAP-718 closure DONE + GAP-717 closure PARTIAL per `gap-done-discipline.md` §3 exit ramp)
- **`feedback_local_fix_production_parity.md`** (memory entry text inline PR body per `post-merge-sync-completeness.md` §7.5)

---

## 8. Log

- **2026-06-19 (v1.1.0):** MINOR — added §2.5 "Value-resolution dimension — multi-host topology config". Triggered by user-flagged 2026-06-19 retro on config-bug campaign #2492-2496: "các PR vừa qua phải fix rất nhiều config khi đưa lên AWS, rõ ràng G3 đã không cover được". Root cause: G2★ walk chạy trên 1 Docker host local KHÔNG tái hiện topology 3-EC2 production (cross-host private IP / SG self-ref / nginx per-tenant proxy / S3 IAM-role / EC2 bash `set -u` quirk); §2 shape-parity kiểm artifact-tồn-tại tại PR-time nhưng KHÔNG kiểm value-resolution tại deploy-time → cross-host config-bug (`KITECLASS_CORE_URL`/`INTERNAL_API_URL` trỏ private IP host khác) rơi vào vùng KHÔNG gate nào → chỉ lòi khi deploy AWS, fix từng cái. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged retro) → Classify ✓ (rule v1.0.x §2 covers shape-parity PR-time only; G3-infra campaign định nghĩa hẹp TLS/DNS-only; no gate covers deploy-time multi-host config value-resolution) → Rule+Enforce ✓ (§2.5 + paired same-PR G3-config gate split trong `flow-verification-campaign.md` §1 + registry `documents/05-guides/deploy/prod-deploy-config-registry.md` + smoke gate `scripts/smoke-prod-config.sh` per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (config-bug campaign #2492-2496 9 bug class — KITECLASS_CORE_URL cross-EC2 / INTERNAL_API_URL / SG self-ref / S3 IAM / nginx /api / fetch-secrets set-u / OTel / payment — đều thuộc value-resolution class §2.5 mà §2 shape-parity miss; smoke gate sẽ catalog chúng tại deploy) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — registry + smoke gate trả lãi ngay mỗi lần redev `terraform apply` (IP/ID mới). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — adds previously-uncovered deploy-time value-resolution layer complementary to §2 PR-time shape-parity; no constraint loosening; existing PRs grandfathered; rule applies prospectively từ this PR forward 2026-06-19). Smoke gate detector = primary enforcement (paired same-PR via agent build); CI shape-parity grep deferred unchanged.
- **2026-05-31** (v1.0.1): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~20k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **2026-05-22 (v1.0.0):** Rule created in response to user-flagged meta gap Wave 104.5 close-loop 2026-05-22: "ngoài ra check meta có audit cho việc fix local thì phải check fix env cho cả production chưa?". Concrete sister bug GAP-717 (Wave 81 + Wave 104.5 same incident class — JWT_CHALLENGE_SECRET manual secret creation never followed by terraform IaC declaration → IaC drift) surfaced same session as instantiation. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged meta gap) → Classify ✓ (existing rules cover related-but-different patterns per GAP-718 §Problem table: `audit-to-gap-pipeline.md` §2.5/§2.6/§2.7/§2.8 cover filing/planning/decision-doc/fix-time directions but NOT code-fix→prod-env-sweep direction; `production-env-config-registry.md` §11 audits run pre-release not per-fix; `release-deploy-standard.md` §3.1 one-off pre-release checklist; `pre-mutation-state-check.md` covers PRE-mutation investigation not POST-fix sweep) → Rule+Enforce ✓ (this file + reviewer-checklist §5.1 + memory mirror §5.2 inline PR body + worked self-test §6 retroactive 2 incidents + paired same-PR with GAP-717 concrete fix (terraform secret declaration) + GAP-718 closure + `output-review-mandate.md` §3 matrix row + rules-index.csv row + audit-to-gap-pipeline.md §2.7 cross-link + production-env-config-registry.md §11 cross-link per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on Wave 81 + Wave 104.5 — rule fires correctly + counterfactual eliminates 1 recurrence + force-multiplier projection ~5-8h/year saved + 0 IaC drift production incidents) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix coverage 1 lần → mọi future code/config fix subsequent auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered class "local code/config fix → production-equivalent surface sweep"; no constraint loosening; existing PRs grandfathered until next refresh; rule applies prospectively từ Wave 105+ forward). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: local fix → prod env sweep direction) + ✅ unique (sister rule `audit-to-gap-pipeline.md` §2.7 covers inverse direction; no overlap với existing rules covering different boundaries) + ✅ widely applicable (every PR touching config-shape artifact) + ✅ body discipline §1 ≤2 "and" conjunctions. CI grep detector (§5.3) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (complexity moderate + recurrence count 1 + honest defer cited inline); reviewer-checklist + worked self-test + memory mirror sufficient cho v1.0.0.
