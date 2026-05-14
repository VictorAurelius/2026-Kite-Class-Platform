# Release Deploy Standard — Generic deploy artifact + process baseline

**Priority:** 🔴 CRITICAL — every production release must satisfy this standard
**Version:** 1.1.0
**Created:** 2026-05-06
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every git tag matching `v[0-9]+.[0-9]+.[0-9]+*` (per `versioning-policy.md`); every production deploy; every pre-release (alpha/beta/rc) shipping to invite tenants

---

## 1. The Rule

> **Every production release MUST satisfy the artifact + process checklist per bump type (per §3 below). Pre-release tags (alpha/beta/rc) satisfy a subset; PATCH satisfies subset; MAJOR satisfies full. Skipping items requires explicit override trailer with reason.**

This rule consolidates deploy requirements into a single standard, replacing ad-hoc gap filing per release. Filed as response to user feedback 2026-05-06: "tạo rất nhiều file gaps, nhưng cần có tiêu chuẩn chung deploy cho 1 version".

---

## 2. Standards we ground in

This rule is NOT made up freely. Grounded in:

| Standard | Source | Coverage |
|---|---|---|
| **AWS Well-Architected Framework** | aws.amazon.com/architecture/well-architected | 6 pillars: Operational Excellence, Security, Reliability, Performance Efficiency, Cost Optimization, Sustainability |
| **The Twelve-Factor App** | 12factor.net | Container/microservice config + deploy patterns |
| **DORA metrics** | Google Cloud DORA | Deployment frequency, Lead time for changes, MTTR, Change failure rate |
| **OWASP Top 10 (2021)** | owasp.org/Top10 | Web application security baseline |
| **NIST SP 800-53 Rev 5** | nvlpubs.nist.gov | Security + privacy controls |
| **CNCF Cloud Native Trail Map** | github.com/cncf/trailmap | Observability + GitOps patterns |
| **PDPL 2023 + Decree 13/2023/NĐ-CP** | thuvienphapluat.vn | VN data protection (already covered Wave 23) |
| **Luật An ninh mạng 2018 + Decree 53/2022/NĐ-CP** | thuvienphapluat.vn | VN cybersecurity + data localization |
| **Project source-of-truth:** [`documents/02-architecture/deployment-strategy.md`](../../documents/02-architecture/deployment-strategy.md) | GAP-103 (DONE 2026-04-18) | 5 nguyên tắc + env matrix |
| **Project ADR:** [`documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md`](../../documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md) | GAP-103 | AWS Agent Plugins evaluation = DEFER Q3 2026 |

---

## 3. Required artifacts per bump type

### 3.1 PRE-RELEASE (alpha / beta / rc) — subset

Pre-release tags (e.g., `v0.9.0-beta`, `v1.0.0-rc.1`) ship to invite tenants / staging only. Required:

#### Operational Excellence (Well-Architected pillar 1)
- [ ] **Deploy plan document** linked (vd: `release-1-deploy-plan.md`) — pre-deploy checklist + step-by-step deploy commands
- [ ] **Smoke test script** (`scripts/smoke-test.sh`) — automated post-deploy verification (per GAP-377)
- [ ] **Rollback procedure** documented (per GAP-378) — at minimum one-command rollback
- [ ] **Status page** or equivalent (per GAP-373) — for incident comms during pre-release

#### Security (Well-Architected pillar 2)
- [ ] **Secrets management** (per GAP-379) — no hardcoded secrets in config
- [ ] **HTTPS / TLS** active on all endpoints
- [ ] **Pre-release disclaimer** trên signup + dashboard banner (per beta-only requirement)
- [ ] **Auth flow** tested end-to-end

#### Reliability (Well-Architected pillar 3)
- [ ] **Database backup** taken pre-deploy
- [ ] **Health check endpoint** (`/actuator/health`) returns 200
- [ ] **Logs aggregated** (per GAP-115) — minimum 24h retention
- [ ] **Restore drill** documented (per GAP-117)

#### Twelve-Factor compliance
- [ ] **Config in env vars** — không hardcoded
- [ ] **Stateless processes** — sticky sessions OK if documented
- [ ] **Port binding** explicit
- [ ] **Disposability** — graceful shutdown <10s

#### DORA baseline measurement
- [ ] **Deploy duration** logged (target: <30 min for pre-release)
- [ ] **First incident triggered MTTR** measured

### 3.2 PATCH (vX.Y.Z+1) — subset (hotfix-friendly)

Production hotfixes = subset to ship fast:
- [ ] All §3.1 PRE-RELEASE items
- [ ] **Regression test** for the bug fixed
- [ ] **Changelog entry** (`CHANGELOG-vX.Y.Z+1.md`) — security/bug section
- [ ] **Backport plan** if maintaining multiple version branches

### 3.3 MINOR (vX.Y+1.0) — most items

Production minor releases = features within current persona scope:
- [ ] All §3.1 + §3.2 items
- [ ] **API contract documented** if new endpoints (`api-contract.md`)
- [ ] **Business rule docs** updated if new BRs
- [ ] **Feature flag** option for gradual rollout (recommend)
- [ ] **Backwards-compatible** API verified (no breaking)

### 3.4 MAJOR (vX+1.0.0) + **first PRODUCTION** (v1.0.0) — full

Persona expansion / compliance class change / first production launch = full scope:

- [ ] All §3.1 + §3.2 + §3.3 items
- [ ] **DNS production setup** (per GAP-369)
- [ ] **CDN setup** (per GAP-371) — Cloudflare proxy + DDoS protection
- [ ] **Email transactional** (per GAP-370) — SES/SendGrid production-ready
- [ ] **Pen-test light** (OWASP top 10 + security headers + CSRF) — minimum baseline
- [ ] **Production data seed** (per GAP-376)
- [ ] **Deploy runbook detailed** (per parent deploy plan)
- [ ] **Monitoring dashboards** active + alerts wired (Grafana per GAP-115 scope)
- [ ] **SLO targets** documented (per GAP-135) — uptime, latency, error rate
- [ ] **Tag-based release CI** automation (per GAP-374)
- [ ] **GitHub Release** với changelog (per GAP-375)
- [ ] **Staging environment parity** validated (per GAP-380)
- [ ] **Beta tenant invite mechanism** (per GAP-372) — if pre-release subset previously
- [ ] **Migration guide** if breaking change từ previous MAJOR
- [ ] **Counsel-reviewed legal docs** (Phase 3 K-12 only — DPO + DPIA + MPS A05)

---

## 4. Process — Pre-deploy / Deploy / Post-deploy

### 4.1 Pre-deploy (T-7 days → T-1)

- T-7: Final feature freeze on `main`
- T-7: Quality audit /100 ≥ threshold (PRE-RELEASE: ≥80; PROD MAJOR: ≥85)
- T-3: Final smoke test on staging
- T-1: Code freeze; backup snapshot; on-call standby

### 4.2 Deploy (T-0)

Per parent deploy plan:
1. Tag release (`git tag -s vX.Y.Z`)
2. CI builds + pushes images (per GAP-374 automation)
3. Apply terraform changes
4. Blue-green deploy nếu MAJOR
5. Run Flyway migrations
6. Smoke tests automated (per GAP-377)
7. DNS cutover nếu MAJOR
8. Public announcement (per status page + email)

### 4.3 Post-deploy (T+1h → T+30 days)

- T+1h: Continuous monitoring
- T+24h: Daily check error rate + P95 latency + signup conversion
- T+7 days: Weekly review trends
- T+30 days: Full quality audit + persona-based business review
- Hotfix queue: PATCH releases ready
- **Monthly:** `bash scripts/smoke-rollback-cycle.sh --dry-run` (validates SHA resolution + smoke framework — no real rollback)
- **Quarterly (maintenance window):** `bash scripts/smoke-rollback-cycle.sh --execute` để measure real TTR baseline + verify restore-forward path

### 4.4 Rollback execution

Production rollback theo §9 "Deploy execution — human-triggered workflow_dispatch + confirm input + narrow OIDC role" pattern. Sister-mechanism của terraform-apply.yml — cùng confirm-input gate + ephemeral OIDC, scope khác (ECS service rollback thay vì infra apply):

- **Workflow:** [`.github/workflows/rollback.yml`](../../.github/workflows/rollback.yml) (Wave 63 GAP-477, narrow OIDC role `your-product-a-rollback-role` least-privilege)
- **Invocation:** `gh workflow run rollback.yml -f target_sha=<sha> -f confirm=APPLY -f dry_run=false`
- **Confirm gate:** input `confirm` MUST equal `APPLY` verbatim (case-sensitive cognitive checkpoint)
- **Approver gate:** GitHub Environment `production` requires reviewer approval (manual gate trước khi apply job chạy)
- **Smoke wrap:** [`scripts/smoke-rollback-cycle.sh`](../../scripts/smoke-rollback-cycle.sh) `--execute` chạy pre-rollback smoke + rollback + post-rollback smoke + restore-forward cycle để baseline TTR
- **Cadence:** monthly `--dry-run` (default), quarterly `--execute` trong maintenance window (per §4.3)
- **Audit trail:** workflow output writes to GitHub Step Summary + CloudWatch metric `<your-product-a>/Rollback/TimeToRecovery`
- **TTR target:** <5 min từ trigger đến health-back; pattern >5 min trong 2 incidents liên tiếp = file follow-up gap

Reference runbook: [`documents/05-guides/operations/incident-response-runbook.md`](../../documents/05-guides/operations/incident-response-runbook.md) §8 (Rollback Workflow & Cycle Validation) — invocation details, troubleshooting matrix, when NOT to use.

---

## 5. Override mechanism

Genuine exception (e.g., hotfix CVE without time for full checklist):

```
git commit -m "...
RELEASE_DEPLOY_OVERRIDE: <reason — explain why standard partial>
RELEASE_DEPLOY_FOLLOWUP: <link to follow-up gap closing skipped items within N days>"
```

Trailer logged in quarterly retro. Pattern frequency >5% of releases triggers meta-review of standard.

---

## 6. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity)

Same-PR shipped với this rule:

- **Skill** `.claude/skills/quality/release-deploy/SKILL.md` — guides through checklist execution
- **PR template checkbox** (extending `output-review-mandate.md` §6.2):
  > - [ ] **Production deployment** — if PR is a release tag candidate (per `versioning-policy.md`), satisfies `release-deploy-standard.md` per-bump-type checklist (§3); deploy plan linked; smoke test passes
- **Reviewer checklist** for release PRs:
  > Did this PR involve a release tag? If yes:
  > - For each new tag: §3 per-bump-type checklist verified
  > - Deploy plan parent doc linked
  > - Smoke test + rollback procedure exist
- **Quarterly audit:** `quality-audit` skill samples 5 random releases. Verify §3 checklist ticked + override trailers explained.

### Override audit

- Trailer presence with valid follow-up gap → WARN
- Trailer presence without follow-up gap → BLOCK
- Pattern >5% release frequency triggers meta-review

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| File 12 gaps per release for deploy artifacts ad-hoc | Codify standard once via this rule; reference in deploy plan |
| Make up artifact requirements per gut feeling | Ground in AWS Well-Architected + Twelve-Factor + DORA |
| Skip security headers because "small audience" | OWASP Top 10 baseline applies to all production tenants regardless |
| Hardcode secrets "temporarily" then forget | Secrets manager from day 1 (per GAP-379) |
| Ship MAJOR without smoke test "because manual review enough" | Automated smoke test post-deploy mandatory |
| Override checklist without follow-up gap | Trailer requires gap link; audit catches missing |
| Ignore VN data localization in deploy decisions | Production must comply Luật An ninh mạng 2018 + Decree 53/2022 |

---

## 8. Sub-component standards (cross-references)

Standards trong sub-components đã có (don't recreate):
- **Database migration:** Flyway `V[N]__description.sql` (per `backend/backend-standards.md`)
- **API versioning:** URL-based `/api/v1/...` (per `versioning-policy.md` §7.1)
- **Frontend bundle:** `package.json` version sync (per `versioning-policy.md` §7.3)
- **Docker images:** `your-core:vX.Y.Z` (per `versioning-policy.md` §7.5)
- **Helm charts:** `infrastructure/helm/` versioned with chart `version` field
- **Logs:** `.claude/rules/logs-format-standard.md` (existing)
- **Output review:** `output-review-mandate.md` §3 (existing — extend with this rule's row)

---

## 9. Claude agent role in deploy (per GAP-381)

Per ADR-015 + GAP-381 evaluation, Claude Code subagents trong this project:

| Phase | Agent role | Reason |
|---|---|---|
| **Deploy preparation** | ✅ ADOPT — generate runbooks, file gaps, write plans, generate Helm values, generate smoke test scripts | Already proven via wave-pack pattern; high productivity |
| **Deploy execution — auto-apply on git push** | ❌ AUTONOMOUS BANNED | Removes "look at plan + think" cognitive checkpoint; production blast radius too high to gate on CI alone |
| **Deploy execution — agent-spawned `terraform apply`/`kubectl apply` autonomously** | ❌ AGENT-INITIATED BANNED | Agent autonomy violates accountability mandate per `output-review-mandate.md`; ADR-015 defers AWS Agent Plugins Q3 2026 |
| **Deploy execution — human-triggered `workflow_dispatch` + confirm input + narrow OIDC role** | ✅ ALLOWED | Human-clicks-button + types "APPLY" verbatim = explicit cognitive checkpoint preserved + audit trail GitHub Actions + ephemeral OIDC creds (better security than static admin key on laptop); industry standard pattern (Atlantis, Terraform Cloud) |
| **Deploy execution — local `terraform apply` with admin key** | ⚠️ ALLOWED for one-time bootstrap (chicken-and-egg provisioning of OIDC apply role) — rotate admin key immediately after | Necessary first-apply path; subsequent applies must use workflow_dispatch per row above |
| **Rollback execution — human-triggered `workflow_dispatch` (rollback.yml) + confirm input + narrow OIDC role** | ✅ ALLOWED | Same cognitive-checkpoint pattern as terraform-apply.yml; sister-mechanism for ECS service rollback (Wave 63 GAP-477); narrow OIDC role `your-product-a-rollback-role`. See §4.4. |
| **Post-deploy verification** | ✅ ADOPT — agent runs smoke test scripts, parses logs, suggests fixes, updates status page | Read-only observation safe; speeds debugging |
| **Rollback decision** | ⚠️ HUMAN-IN-THE-LOOP — agent can flag issues but human decides rollback trigger | Rollback decision requires judgment; agent can WARN only |

This delineation matches `output-review-mandate.md` Section 6 (Production deployment review = human-required) + ADR-015 (defer AWS Agent Plugins for production AWS operations).

---

## 10. Relationship to other rules

- **`output-review-mandate.md`** §3 row "Production Deployment" — this rule = the standard for that row
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — same-PR enforcement (skill + GAP-381) per Stage 3
- **`versioning-policy.md`** — provides version conventions; this rule provides per-bump-type artifact checklist
- **`incident-to-rule-pipeline.md`** — this rule was created via Stage 3 response to user-flagged miss "tạo nhiều gaps thay vì rule"
- **`audit-to-gap-pipeline.md`** Step 2.5 state-check — this rule creation itself violated state-check (didn't read GAP-103 first); meta-lesson logged in §11
- **`gap-done-discipline.md`** — release deploy artifact gaps (GAP-369..380) close per §2 only when corresponding standard checklist item satisfied

---

## 11. Self-test (worked example — Release 1)

Apply rule §3.4 (MAJOR + first PRODUCTION) to Release Lần 1 v1.0.0:

| Required artifact | Status | Reference |
|---|---|---|
| Deploy plan document linked | ✅ | `release-1-deploy-plan.md` |
| Smoke test script | ⏳ | GAP-377 (P1) |
| Rollback procedure | ⏳ | GAP-378 (P1) |
| Status page | ⏳ | GAP-373 (P1) |
| Secrets management | ⏳ | GAP-379 (P1) |
| HTTPS/TLS | ⏳ | Per Oracle Cloud setup |
| DNS production setup | ⏳ | GAP-369 (P0 BLOCKING) |
| CDN setup | ⏳ | GAP-371 (P1) |
| Email transactional | ⏳ | GAP-370 (P0 BLOCKING) |
| Pen-test light | ⏳ | New gap (mentioned in `release-1-deploy-plan.md` Phase 1.5) |
| Production data seed | ⏳ | GAP-376 (P0 BLOCKING) |
| Monitoring dashboards | ⏳ | GAP-115 (PARTIAL) |
| SLO targets | ⏳ | GAP-135 (PARTIAL) |
| Tag-based release CI | ⏳ | GAP-374 (P1) |
| GitHub Release với changelog | ⏳ | GAP-375 (P2) |
| Staging environment parity | ⏳ | GAP-380 (P1) |
| Beta tenant invite mechanism | ⏳ | GAP-372 (P0 BLOCKING — for v0.9.0-beta) |
| Counsel-reviewed legal docs | ⏳ | GAP-182 + GAP-184 Phase 2 (Phase 3 K-12 trigger) |

→ Rule §3.4 successfully maps Release 1 v1.0.0 readiness state. 12 gaps GAP-369..380 + existing GAP-115/135/182/184 = comprehensive coverage. **Rule fires correctly on existing scope.** ✅

### Meta-lesson

Rule creation itself surfaced 2 misses by current author:
1. State-check vi phạm — không đọc `deployment-strategy.md` (GAP-103 DONE) trước khi file 12 deploy gaps
2. Standard groundwork miss — generated artifacts free-form thay vì cite Well-Architected/Twelve-Factor/DORA/OWASP

Both addressed via this rule §2 (standards reference) + §10 (cross-link GAP-103) + 12 gaps' updates (cross-ref `deployment-strategy.md`).

---

## 12. Scope clarifications

### 12.1 What this rule does NOT cover

- **Detailed go-live runbook** — that's per-release plan responsibility (vd: `release-1-deploy-plan.md` §4)
- **Vendor-specific deploy commands** — Helm/kubectl/terraform commands belong in deploy plan
- **K-12 LEGAL counsel process** — separate scope (`business-logic-review.md` + GAP-182/184)
- **AI Branding production cutover** — separate scope (GAP-225 cluster)

### 12.2 When to invoke

| Context | Apply | Frequency |
|---|---|---|
| Tag a release (any bump) | ✅ §3 per-bump-type checklist | Always |
| Pre-release deploy to staging | ✅ §3.1 PRE-RELEASE subset | Always |
| Production hotfix | ✅ §3.2 PATCH subset | Always |
| Rollback execution | ✅ §4.3 post-deploy + rollback runbook | Per incident |
| Documentation-only PR | ❌ skip | — |
| Internal refactor without release | ❌ skip | — |

If unsure: default to apply per-bump-type checklist; skipping requires override trailer.

---

## 13. Log

- **2026-05-11 (v1.1.0):** MINOR — added §4.4 Rollback execution section + §9 matrix row "Rollback execution — human-triggered workflow_dispatch" per Wave 63 GAP-477 rollback.yml landing. Also added monthly `--dry-run` + quarterly `--execute` cadence bullets to §4.3 post-deploy. Sister-mechanism của terraform-apply.yml pattern; reuses confirm-input gate + ephemeral OIDC pattern; scope = ECS service rollback (narrow role `your-product-a-rollback-role`). Cross-link added: `documents/05-guides/operations/incident-response-runbook.md` §8 for invocation details + troubleshooting. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — extends existing §9 carve-out to rollback scope; no constraint loosening; consistent with v1.0.1 expansion pattern).

- **2026-05-08 (v1.0.1):** PATCH — §9 matrix "Deploy execution" row replaced single ❌ SKIP cell với 4-case distinction (auto-apply BANNED / agent-apply BANNED / human-triggered workflow_dispatch ALLOWED / one-time local bootstrap ALLOWED for chicken-and-egg). Triggered by Wave 43 closure user-flagged miss "tại sao cần rule terraform apply human-only" — surfaced rule conflated 3 cases, banning workflow_dispatch + confirm-input pattern that's industry standard (Atlantis/TF Cloud). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user retro post Wave 43 closure) → Classify ✓ (existing §9 didn't distinguish autonomy vs human-trigger) → Rule+Enforce ✓ (this entry + paired same-PR Bucket B `terraform-apply.yml` + IAM apply role + Bucket C bootstrap runbook per `rule-change-process.md` §6.5) → Self-Test ✓ (worked example: Wave 43 GAP-446/447 PARTIAL state would be unblocked by workflow_dispatch carve-out without violating rule spirit) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — clarification only, no constraint loosening for actual-banned cases; auto-apply BAN preserved, agent-apply BAN preserved, only adds explicit carve-out for human-triggered workflow_dispatch). Closes Wave 44 Bucket A via GAP-449 Phase 1.
- **2026-05-06 (v1.0.0):** Rule created in response to user feedback "tạo nhiều gaps thay vì rule chung". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged ad-hoc gap pattern instead of codified standard) → Classify ✓ (no existing rule covers production deploy standard) → Rule+Enforce ✓ (this file + paired same-PR `quality/release-deploy/SKILL.md` + GAP-381 + cross-link updates from 12 gaps + `output-review-mandate.md` §3 row) → Self-Test ✓ (§11 worked example on Release 1 v1.0.0) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new rule with built-in enforcement, no constraint loosening). Standards explicitly grounded: AWS Well-Architected + Twelve-Factor + DORA + OWASP + NIST + CNCF + VN PDPL + VN Cybersecurity Law. Meta-lesson §11 acknowledges this author's prior state-check miss + free-form generation issues; corrective references shipped.
