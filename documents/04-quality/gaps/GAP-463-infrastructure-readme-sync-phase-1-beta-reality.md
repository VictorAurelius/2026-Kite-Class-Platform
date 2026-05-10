# GAP-463: infrastructure/README.md sync với Phase 1 BETA reality

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (docs-only; new-contributor confusion risk)
**Domain:** Documentation / Infrastructure
**Found:** 2026-05-11 (user-flagged session — infra deployment understanding question)
**Affects:** `infrastructure/README.md`

## Problem

`infrastructure/README.md` mô tả EKS deployment + terraform-oracle, KHÔNG khớp với Phase 1 BETA reality:

```markdown
infrastructure/
├── helm/                   # Helm charts for Kubernetes      ← dormant Phase 1 BETA
├── k8s/                    # Raw Kubernetes manifests        ← dormant Phase 1 BETA
├── terraform-aws/          # AWS infrastructure (VPC, EKS, RDS, S3, ECR)  ← misleading "EKS" — actually EC2 docker-compose
├── terraform-oracle/       # Oracle Cloud infrastructure (compute, network)  ← ARCHIVED per ADR-025
└── logs/                   # CI/CD operation logs (mostly gitignored)
```

**Verified facts (2026-05-11):**
- `infrastructure/terraform-aws/ec2.tf` — 2× `aws_instance` resources (`kh_backend` + `kc_app`); zero `aws_ecs_*` / `aws_eks_*`
- `infrastructure/terraform-oracle/` — KHÔNG còn tồn tại (archived per ADR-025 §"Same-PR landing" 2026-05-07)
- `infrastructure/helm/` + `k8s/` — pre-existing templates dormant; planned Phase 1.5 PAID full migration per GAP-415

## Root cause

README chưa được updated khi:
1. ADR-025 (2026-05-07) archived `terraform-oracle/`
2. Wave 50 Phase 2.3 production apply (2026-05-08) deployed EC2 docker-compose (KHÔNG EKS)

## Proposed Fix

Rewrite `infrastructure/README.md` per actual structure + add "Phase 1 BETA active vs Phase 1.5+ future" section:

```markdown
infrastructure/
├── helm/                   # Helm charts (Phase 1.5+ MIGRATION TARGET — dormant Phase 1 BETA)
│   ├── kitehub/            
│   └── kiteclass-instance/ 
├── k8s/                    # Raw K8s manifests (Phase 1.5+ MIGRATION TARGET — dormant)
├── terraform-aws/          # ✅ ACTIVE Phase 1 BETA — VPC + EC2 + RDS + ALB + S3 + Route 53 + CloudTrail
└── logs/                   # CI/CD operation logs (gitignored)
```

Add section explaining:
- **Phase 1 BETA active deploy:** EC2 docker-compose (Architecture B per ADR-025)
- **Phase 1.5+ future:** EKS via Helm charts (per GAP-415 migration plan)
- **terraform-oracle removed** 2026-05-07 (ADR-025 Oracle→AWS switch)

## Acceptance Criteria

- [ ] `infrastructure/README.md` Directory Map updated
- [ ] "Phase 1 BETA active vs Phase 1.5+ future" section added
- [ ] terraform-oracle reference removed
- [ ] EKS reference clarified as future-target not current
- [ ] Cross-link added: ADR-025 + GAP-415 + `aws-architecture-sizing-matrix.md`

## Related

- ADR-025 — AWS-only Phase 1 Free Tier (Oracle→AWS switch)
- GAP-415 — Phase 2 EKS Migration Plan (PARTIAL)
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` §3 — Architecture B Phase 1 BETA detail
- ROADMAP §🚀 Next Action — current deploy state

## Log

- **2026-05-11**: Filed user-flagged via session question "tại sao có EKS và K8s trong infra?" — README inconsistency surfaced. Per `feedback_post_merge_doc_sync.md` + `incident-to-rule-pipeline.md` Stage 3: this is README drift incident; fix inline trong follow-up wave/PR.
