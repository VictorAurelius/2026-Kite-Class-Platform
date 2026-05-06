# GAP-380: Staging Environment Activation + Parity Validation

**Status:** 🔵 OPEN
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

### Staging environment scope

Mirror production architecture nhỏ hơn:
- AWS EKS staging cluster (smaller node pool)
- RDS staging instance (db.t3.micro)
- ElastiCache staging
- S3 staging bucket
- Staging domain: `staging.kitehub.vn` + `staging.kiteclass.vn`
- Cloudflare proxy enabled
- Same Spring profiles + configs as prod, except secrets + DB

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

- [ ] Staging EKS cluster verified live
- [ ] DNS staging.kitehub.vn + staging.kiteclass.vn resolve
- [ ] Helm release deployed staging
- [ ] Flyway migrations applied
- [ ] Smoke test passes on staging
- [ ] E2E Playwright suite runs successfully
- [ ] Staging→prod promotion documented
- [ ] Synthetic test data fixtures
- [ ] Cost monitoring (staging cost <$50/mo)
- [ ] Cleanup script (tear down staging when not needed)

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

- **2026-05-06:** Filed by Release 1 deploy plan PR. STRONGLY recommend Phase 1 BETA — first-test-in-prod = high risk; staging parity reduces.
