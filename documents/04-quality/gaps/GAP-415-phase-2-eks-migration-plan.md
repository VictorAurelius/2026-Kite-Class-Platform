# GAP-415: Phase 2 EKS Migration Plan

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Infrastructure / Future planning
**Found:** 2026-05-07 (Wave 37 — Layer 5)
**Affects:** Phase 1.5 PAID → Phase 2 P3 medium-center (~30+ tenants trigger gate)

## Problem

Architecture B (split EC2 docker compose) cap khoảng 100-200 tenants. Phase 2 P3 medium-center scope = 200-500 tenants → cần EKS hoặc ECS Fargate (multi-AZ, autoscaling, rolling deploy).

Hiện chưa có migration plan EC2 → EKS — risk ad-hoc cutover dưới pressure.

## Proposed Fix

Migration plan document `documents/03-planning/roadmap/phase-2-eks-migration.md`:

1. **Trigger gate:** 30+ paying tenants HOẶC daily signup spike >10/day HOẶC multi-AZ requirement
2. **Pre-migration:** Helm charts production-ready (already partial), HPA configs, EKS Terraform module
3. **Cutover strategy:** Blue-green (new EKS cluster parallel với EC2) → DNS swap → 24h soak → decommission EC2
4. **Rollback:** DNS revert (Cloudflare TTL 60s) + EC2 still hot 24h
5. **Cost projection:** $115/mo → $250/mo step-up (verify revenue covers)

NOTE: Architecture C trong GAP-411 sizing matrix là target post-migration.

## Acceptance Criteria

- [ ] `phase-2-eks-migration.md` plan exists với 5 sections
- [ ] EKS Terraform module reviewed (separate from Phase 1 module)
- [ ] Helm charts cho 8 services + frontends provisioned (some exist, audit gap)
- [ ] Cutover dress-rehearsal staging (Phase 1.5 mid-cycle)
- [ ] Rollback runbook (DNS revert)

## Related

- GAP-411 (sizing matrix — Architecture C)
- ADR-025 AWS Singapore
- `phase-2-eks-migration.md` (this gap creates)
