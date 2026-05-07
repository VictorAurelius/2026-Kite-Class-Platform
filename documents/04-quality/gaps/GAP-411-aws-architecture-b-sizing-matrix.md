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

## Log

- **2026-05-07** — DONE. Sizing matrix shipped at `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` (13 sections, 5-phase progression, hidden cost line items, optimization roadmap). Wave 37 Bucket E.
