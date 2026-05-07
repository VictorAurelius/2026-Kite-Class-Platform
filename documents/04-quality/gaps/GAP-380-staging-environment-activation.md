# GAP-380: Staging Environment Activation + Parity Validation

**Status:** 🟡 PARTIAL — Architecture B revision artifacts shipped Wave 38 Bucket D (Terraform + workflow rewrite + fixtures script + activation runbook). Live activation steps (`terraform apply`, first deploy, DNS configure, smoke test) là USER-ACTION post-merge per `gap-done-discipline.md` §3 PARTIAL exit ramp + GAP-381 Phase 2 BANNED.
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA — pre-prod testing)
**Domain:** Infrastructure / DevOps
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Pre-prod confidence, regression detection before prod

## Problem

Staging environment exists trong terraform-aws + has `.github/workflows/deploy-staging.yml`, nhưng:
- Activation status TBD (chưa verified live)
- No parity validation với production architecture
- No automated staging→prod promotion gate
- No staging-specific data fixtures

Without working staging:
- Production deploys = first-test-in-prod (high risk)
- Cannot run E2E pre-prod
- Cannot test rollback procedure safely
- Cannot validate Helm changes pre-prod

## Proposed Fix

### Staging environment scope (revised 2026-05-07 — Architecture B per ADR-025)

Phase 1 BETA mirror production architecture nhỏ hơn (NO EKS — EC2 + docker-compose):
- 1× t3.micro EC2 chạy combined KH + KC stack qua docker-compose
- 1× RDS db.t3.micro (staging Postgres)
- 1× S3 bucket (assets staging)
- Redis + RabbitMQ self-host trên EC2 (containerized)
- Staging domain: `staging.kitehub.vn` + `staging.kiteclass.vn`
- Cloudflare proxy enabled (DNS primary per ADR-018)
- Same Spring profiles + configs as prod, except secrets + DB

Phase 2 EKS migration trigger gate documented in
`documents/05-guides/deploy/staging-activation-runbook.md` §7 (per Wave 37 GAP-415).

### Parity validation

- [ ] Staging deploy runs same Helm chart as prod (parametrized)
- [ ] Staging Flyway migrations identical to prod
- [ ] Staging API endpoints respond same as prod
- [ ] Staging E2E suite passes (Playwright)
- [ ] Staging smoke test (per GAP-377) passes

### Automated staging→prod promotion

- `vX.Y.Z-rc.N` tag → auto-deploy to staging
- Staging E2E + smoke test pass → manual approval gate
- Approval → promote to prod (manual `workflow_dispatch` + DEPLOY confirm)

### Staging data fixtures

- Synthetic test tenants (5-10)
- Sample students/classes/lessons cho realistic dashboard
- Mock payment processor (sandbox keys)
- Mock email transactional (catchall inbox)

## Acceptance Criteria

### Wave 38 Bucket D — code artifacts (PARTIAL DONE 2026-05-07)

- [x] Terraform staging module shipped (`infrastructure/terraform-aws/staging.tf` + variables)
- [x] `enable_staging` flag-gated provisioning (allows tear-down for cost savings)
- [x] `.github/workflows/deploy-staging.yml` rewritten Architecture B (drop EKS_CLUSTER + helm; SSM run-command + docker-compose pull/up)
- [x] `scripts/seed-staging-fixtures.sh` (synthetic 7 tenants + sandbox payment + MailHog catchall)
- [x] `documents/05-guides/deploy/staging-activation-runbook.md` (activation steps + Helm-skip rationale + Phase 2 EKS migration trigger gate)

### USER-ACTION (post-merge — per GAP-381 Phase 2 BANNED for agent)

- [ ] Run `terraform apply -var="enable_staging=true"` → provision staging EC2 + RDS + S3
- [ ] DNS staging.kitehub.vn + staging.kiteclass.vn configured trên Cloudflare (proxy enabled)
- [ ] docker-compose stack deployed via SSM (first run)
- [ ] Flyway migrations applied
- [ ] Smoke test passes on staging URL (`./scripts/smoke-test.sh`)
- [ ] E2E Playwright suite runs successfully (follow-up — out of Bucket D scope)
- [ ] Staging→prod promotion documented (cross-link cicd-release-procedure.md — DONE Wave 38 Bucket A GAP-374)
- [ ] Synthetic test data fixtures seeded (run `seed-staging-fixtures.sh`)
- [ ] Cost monitoring confirmed (staging cost <$50/mo via CloudWatch Billing alarm)
- [x] Cleanup mechanism documented (`terraform apply -var=enable_staging=false`)

## Open decisions

- Staging always-on vs on-demand (cost trade-off)
- Synthetic data vs cloned-from-prod (privacy concerns)
- Staging accessibility (VPN-restricted vs public-but-no-data)

## Effort estimate

~2-3 ngày infra activation + ~1 ngày fixtures + ~1 ngày E2E suite extension.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Existing: `.github/workflows/deploy-staging.yml`
- Existing: `infrastructure/terraform-aws/` (EKS staging config)
- Sister: GAP-377 (smoke test runs on staging too)

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-07** (Wave 38 Bucket D, PARTIAL flip): Architecture B revision artifacts shipped. Scope re-written từ EKS-based staging → EC2 + docker-compose Phase 1 (per ADR-025). Files: `infrastructure/terraform-aws/staging.tf` (NEW, ~270 LOC) + variables.tf extension (3 vars: `enable_staging`/`staging_instance_type`/`staging_rds_instance_class`) + `.github/workflows/deploy-staging.yml` (REWRITE — drop EKS_CLUSTER + helm; SSM run-command + ECR pull + docker-compose up + smoke test) + `scripts/seed-staging-fixtures.sh` (NEW — synthetic 7 tenants + sandbox payment + MailHog catchall, idempotent via `staging_fixture_marker` table) + `documents/05-guides/deploy/staging-activation-runbook.md` (NEW — 12-section activation runbook + Helm-skip rationale §8 + Phase 2 EKS migration trigger gate §7 cross-ref Wave 37 GAP-415). Verification gates passed: `terraform fmt -check` + `terraform validate` + `python3 yaml.safe_load(deploy-staging.yml)` + `shellcheck seed-staging-fixtures.sh`. Status flip → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp — `terraform apply` + first deploy + DNS configure + smoke-on-live = USER-ACTION (per GAP-381 Phase 2 BANNED for agent). Cost target <$50/mo confirmed (~$25-30/mo steady-state, <$10/mo Free Tier 12mo).
- **2026-05-06:** Filed by Release 1 deploy plan PR. STRONGLY recommend Phase 1 BETA — first-test-in-prod = high risk; staging parity reduces.
