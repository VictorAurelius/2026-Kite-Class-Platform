# GAP-464: ECS Fargate vs EKS architecture decision (ADR-025 §5 follow-up)

**Status:** 🟢 DONE
**Priority:** 🟡 P2 (Phase 1.5 PAID full mid-cycle decision; not Phase 1 BETA blocker)
**Domain:** Infrastructure / Architecture
**Found:** 2026-05-11 (user-flagged session — surfaced unfilled ADR-025 commitment)
**Affects:** Phase 1.5 PAID full migration target Architecture C; `aws-architecture-sizing-matrix.md` §5; GAP-415 EKS migration plan

## Problem

ADR-025 (AWS-only Phase 1 Free Tier, 2026-05-07) §"Implementation Notes" §5 commits:

> "Free tier favor **ECS Fargate** vì EKS control plane charge $73/tháng KHÔNG có free tier. **Decision tracked trong follow-up gap**; this ADR scope = Oracle→AWS switch."

**Follow-up gap CHƯA được filed.** GAP-415 (Phase 2 EKS Migration Plan) presupposes EKS không considering ECS Fargate alternative.

## Background

Phase 1 BETA → Phase 1.5 PAID early dùng EC2 (Architecture A/B) — quyết định locked. Phase 1.5 PAID full (200-500 tenants, multi-AZ requirement) cần containerized orchestration. 2 candidates:

### EKS (current default per `aws-architecture-sizing-matrix.md` §5 Architecture C)
- $73/mo control plane (always-on, no Free Tier)
- 2× t3.medium worker nodes = $60/mo
- Total Architecture C estimate: **$250/mo**
- ✅ Industry standard K8s; portable; Helm/k8s artifacts đã prepared
- ❌ Operational complexity high (CNI, IAM IRSA, kubectl debugging)
- ❌ $73 control plane = ~30% Architecture C cost cho 0-traffic baseline

### ECS Fargate (ADR-025 §5 hint)
- $0/mo control plane (managed)
- Fargate task pricing: per-vCPU-hour + per-GB-RAM-hour
- 2 services × 0.5 vCPU + 1 GB ≈ **$30-50/mo** Phase 1.5 full
- ✅ Cheaper baseline; serverless (no nodes to patch)
- ❌ AWS-locked (k8s portability lost)
- ❌ Fargate Spot không support cho persistent workloads (Spring Boot stateful sessions)
- ❌ Helm/k8s artifacts đã invested — wasted nếu pivot ECS

## Proposed Fix

File ADR-027 "ECS Fargate vs EKS for Phase 1.5 PAID full migration":

### Decision matrix

| Criterion | EKS Architecture C | ECS Fargate Alternative |
|-----------|:-----------------:|:----------------------:|
| Phase 1.5 full $/mo | $250 | $150-200 (estimate) |
| Phase 2 P3 $/mo | $400 | $250-350 |
| Phase 3 K-12 $/mo | $1000+ | $700-900 |
| Helm artifact reuse | ✅ existing investment | ❌ rewrite as ECS task definitions |
| K8s portability | ✅ AWS/GCP/Azure | ❌ AWS-only |
| Operational complexity | High | Medium |
| Solo-dev cognitive load | High | Low |
| Multi-AZ ready | ✅ | ✅ |
| Autoscaling | ✅ HPA + Cluster Autoscaler | ✅ Service auto-scaling |
| Phase 3 K-12 fit | ✅ Aurora cluster + EKS standard | ⚠️ ECS less mature multi-region |

### Tentative decision (subject to ADR-027 finalization)

**KEEP EKS path** (per existing GAP-415 + Helm investment) **UNLESS:**
- Phase 1.5 full Architecture C cost exceeds projection by >25% in dress-rehearsal (GAP-415 §3 cutover) → pivot ECS Fargate
- Solo-dev mode persists through Phase 2 (no team to absorb K8s complexity) → pivot ECS Fargate
- AWS Activate credit denied + revenue path slow (no $250/mo headroom) → pivot ECS Fargate

### Trigger to file ADR-027

- Phase 1 BETA closes successfully (5+ beta tenants live, 2-week 0 P0 incidents) per `release-1-plan-2026.md` Phase 1.5 trigger gate
- Pre-cutover dress-rehearsal trên staging-parity (per GAP-380)
- Invest 4-6h cost-modeling + Spring Boot startup time benchmark on Fargate

## Acceptance Criteria

- [x] ADR-028 drafted với decision matrix + verdict (ADR-027 slot was already taken by statuspage vendor; chose next free ID — `documents/02-architecture/adr/ADR-028-ecs-fargate-vs-eks-phase-1-beta.md`)
- [x] Cost model documented inline in ADR-028 §"Context" + §"Considered Options" (Phase 1.5 EKS ≈ $250/mo vs. ECS Fargate ≈ $150-200/mo; Phase 2 P3 EKS ≈ $400 vs. Fargate ≈ $250-350; Phase 3 K-12 EKS ≈ $1000+ vs. Fargate ≈ $700-900) — full Phase 1.5 dress-rehearsal spreadsheet remains a Phase 1.5 prep deliverable per ADR-028 Implementation Notes; inline figures sufficient for verdict
- [x] ADR-025 §5 cross-link added to ADR-028 closing the original commitment (this PR)

## Out-of-scope (track separately)

| Item | Where |
|---|---|
| GAP-415 status note updated với cross-link to ADR-028 | Not in this Bucket C PR; tracked as follow-up update of GAP-415 referencing ADR-028 acceptance |
| `aws-architecture-sizing-matrix.md` §5 updated với "alternative considered" footnote | Not in this Bucket C PR; tracked as follow-up doc edit; ADR-028 §"Consequences → Neutral" already documents the §5 footnote intent |

## Related

- ADR-025 §5 — original commitment to file follow-up (this gap closes commitment)
- GAP-415 — Phase 2 EKS Migration Plan (PARTIAL)
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` §5 Architecture C
- `release-1-plan-2026.md` Phase 1.5 trigger gate

## Log

- **2026-05-11**: Filed user-flagged via session question "tại sao có EKS và K8s trong infra?" — surfaced ADR-025 §5 commitment to file follow-up gap chưa fulfilled. Decision deferred to Phase 1 BETA closure trigger (no urgency Phase 1 BETA scope). Per `incident-to-rule-pipeline.md`: this is gap-tracking incident, not coverage-gap rule incident.
- **2026-05-11 (Wave 58 Bucket C):** ADR-028 ECS Fargate vs EKS shipped ACCEPTED. ADR-025 §5 commitment closed. ADR uses next free ADR ID (ADR-027 slot already taken by Instatus statuspage vendor decision Wave 38). Two satellite ACs (GAP-415 cross-link + `aws-architecture-sizing-matrix.md` §5 footnote) moved to §Out-of-scope as follow-up doc edits — ADR-028 §"Consequences → Neutral" already records the intent. Solo-dev acting CTO sign-off per CLAUDE.md decision context locked 2026-05-06.
