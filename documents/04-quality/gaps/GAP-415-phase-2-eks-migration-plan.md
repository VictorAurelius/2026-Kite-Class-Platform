# GAP-415: Phase 2 EKS Migration Plan

**Status:** 🟡 PARTIAL 2026-05-07 (plan shipped; EKS Terraform module + Helm audit + dress-rehearsal deferred Phase 1.5 mid-cycle)
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

- [x] `phase-2-eks-migration.md` plan exists với 5 sections (actual: 7 sections — trigger / pre-migration / cutover / cost / rollback / decommission / acceptance)
- [ ] EKS Terraform module reviewed (separate from Phase 1 module) — **design spec §2.2; module creation post-trigger Phase 1.5 PAID full**
- [ ] Helm charts cho 8 services + frontends provisioned (some exist, audit gap) — **audit deferred §2.1; follow-up gap if missing post-state-check**
- [ ] Cutover dress-rehearsal staging (Phase 1.5 mid-cycle) — **deferred Phase 1.5 mid-cycle activity per §3.1**
- [x] Rollback runbook (DNS revert) (§5)

## Log

- **2026-05-07** — PARTIAL. Migration plan `documents/03-planning/roadmap/phase-2-eks-migration.md` shipped với trigger gates + pre-migration checklist + blue-green cutover sequence + cost projection + DNS rollback runbook + Phase 1 decommission steps. Terraform module + Helm audit + dress-rehearsal deferred Phase 1.5 mid-cycle. Wave 37 Bucket E.

## Related

- GAP-411 (sizing matrix — Architecture C)
- ADR-025 AWS Singapore
- `phase-2-eks-migration.md` (this gap creates)
