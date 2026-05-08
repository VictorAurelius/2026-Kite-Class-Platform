# GAP-411: AWS Architecture B Sizing + Phase Progression Matrix

**Status:** 🟢 DONE 2026-05-07 (Wave 37 Bucket E PR — sizing matrix shipped)
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Infrastructure / Cost
**Found:** 2026-05-07 (Wave 37 — Layer 5 AWS cost)
**Affects:** Phase 1 BETA cost forecast + Phase 1.5/2 sizing decisions

## Problem

Architecture B chốt 2026-05-07 (split EC2 t3.medium + t3.small + RDS db.t3.micro free) nhưng KHÔNG có document chi tiết sizing matrix qua các phase.

## Proposed Fix

Tạo `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` với:

| Phase | Tenants | Architecture | $/mo Yr1 | $/mo Yr2+ | Trigger gate |
|---|---|---|---|---|---|
| Phase 1 BETA invite | 5-10 | B (split EC2 t3.medium + t3.small + RDS free) | **$72** | $89 | Quality ≥80 + 5 tenants live |
| Phase 1.5 PAID early | 50-100 | A (single t3.large + RDS db.t3.small) | $115 | $135 | 30 paying tenants |
| Phase 1.5 PAID full | 200-500 | C (EKS minimal: $73 control + 2 t3.medium) | $250 | $280 | 100 paying tenants |
| Phase 2 P3 | 500-1000 | EKS + autoscaling + RDS read replica | $400-600 | | 500 tenants |
| Phase 3 K-12 | 1000+ | Multi-AZ EKS + Aurora | $1000+ | | counsel + MoET approval |

Cost driver breakdown 89% EC2 — optimization knobs:
- JVM heap shrink (per GAP-408 dev pattern → also production)
- EC2 Reserved Instance (1-year commit ~30% off Phase 1.5+)
- EC2 Spot for non-critical (ECS Fargate Spot 50-70% off)

## Acceptance Criteria

- [x] `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` exists
- [x] Sizing matrix per phase documented
- [x] Cost projection 3-year (Phase 1 → Phase 2)
- [x] Optimization roadmap (RI, Spot, autoscaling)
- [x] Hidden cost section (egress, NAT, CloudWatch ingest)

## Related

- ADR-025 AWS Singapore
- ADR-026 Ollama defer Phase 2
- GAP-395 (Terraform stack matches Architecture B)
- GAP-412 (AWS Activate credit application — neutralize Yr1 cost)
- `release-deploy-standard.md` §3

## Post-Vercel Pivot Update (2026-05-08)

Following 2026-05-07 Vercel pivot for KC frontend, sizing matrix Phase 1 BETA row is amended:

### kh-backend revision

- **Before (PR #1031):** m7i-flex.large 8GB ($60/mo) — emergency over-correction after Phase 7 t3.micro 1GB OOM cascade.
- **After (Wave 43 Bucket B / GAP-447):** t3.medium 4GB ($30/mo) — matches GAP-411 original sizing.
- **Evidence:** `docker-compose.production.yml` §13-19 RAM partitioning chốt 5 KH services + redis + rabbitmq + gateway = ~3.2GB peak → 800MB headroom on t3.medium 4GB.
- OOM #1031 root cause = sizing matrix stale enforce (deployed t3.micro 1GB instead of charted t3.medium 4GB), not insufficient memory at t3.medium. Right-size restores GAP-411's intent.

### kc-app revision (post-Vercel pivot)

- **Before (GAP-411 original):** t3.small 2GB — assumed KC frontend (Next.js) ON kc-app.
- **After (Vercel pivot 2026-05-07 + GAP-447):** t3.medium 4GB ($30/mo) — KC frontend lives on Vercel; kc-app = backend-only stack.
- **Evidence:** `docker-compose.kc.yml` §13-18 RAM partitioning chốt kiteclass-core + gateway + redis + rabbitmq = ~2.5GB peak. t3.small 2GB insufficient (under peak); t3.medium 4GB gives 1.5GB headroom.
- Rationale: Vercel removes Next.js footprint from EC2, but Java backend + broker still need ≥2.5GB. Sizing matrix kept t3.small from pre-pivot plan and is now stale.

### OOM safety net (BẮT BUỘC trước khi downsize)

- CloudWatch alarm `MemoryUtilization > 85%` per EC2 → SNS topic `kitehub-memory-alerts` → email `vannkite@outlook.com` (`infrastructure/terraform-aws/cloudwatch.tf`).
- Pre-requisite: CloudWatch agent install + start on EC2 (cloud-init installs the package; runtime config via SSM run-command per `documents/05-guides/deploy/right-size-stress-test.md`).
- Rollback escalation (per GAP-447): JVM heap tune → t3.large 8GB (same family) → m7i-flex.large 8GB revert.

### Combined cost saving

- Right-size only: $120 → $60/mo EC2 (-$60/mo).
- Combined với GAP-446 EventBridge stop/start (~58% downtime): EC2 ~$25/mo (vs $120/mo current).

### Sizing matrix Phase 1 BETA row update

| Phase | Tenants | Architecture | $/mo Yr1 | Trigger gate |
|---|---|---|---|---|
| Phase 1 BETA invite (revised) | 5-10 | B-revised: 2× t3.medium 4GB EC2 + RDS db.t3.micro free + Vercel KC FE | **$60** EC2 + $0 RDS = **$60** | Quality ≥80 + 5 tenants live |

Cross-reference: `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` should be updated in same PR (sizing matrix row + Vercel pivot footnote) — tracked in GAP-447 follow-up if not landed inline.

## Log

- **2026-05-08** — Post-Vercel pivot update appended (GAP-447 / Wave 43 Bucket B). kh-backend revert m7i-flex.large → t3.medium per original matrix; kc-app revise t3.small → t3.medium per post-Vercel backend-only stack reality. OOM safety net shipped via `infrastructure/terraform-aws/cloudwatch.tf`. Status remains 🟢 DONE — sizing matrix doc itself is intact; this update reflects post-pivot reality + emergency over-correction reversal.
- **2026-05-07** — DONE. Sizing matrix shipped at `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` (13 sections, 5-phase progression, hidden cost line items, optimization roadmap). Wave 37 Bucket E.
