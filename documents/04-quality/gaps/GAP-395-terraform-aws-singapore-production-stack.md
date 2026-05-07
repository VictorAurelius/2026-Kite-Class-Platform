# GAP-395: Terraform AWS Singapore Production Stack Provision

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Infrastructure / Terraform
**Found:** 2026-05-07 (Wave 36 audit aggregate — release-hardening 5-layer planning)
**Affects:** Phase 1 BETA deploy readiness — without infra Terraform plan, Docker images cannot deploy

## Problem

`infrastructure/terraform-aws/` đã có scaffold cho ADR-025 AWS Singapore Free Tier nhưng chưa verify đủ resource cần thiết cho Architecture B Phase 1 BETA (~$72/mo).

## Required AWS resources (Architecture B per `release-deploy-standard.md` §3.4)

- VPC + 2 public subnets (single AZ ap-southeast-1a Phase 1)
- 1 × t3.medium EC2 (KH backend cluster)
- 1 × t3.small EC2 (KC + frontends)
- RDS db.t3.micro PostgreSQL (free 12 tháng)
- ECR private repo (free 500MB) — 6 KH services + 2 KC services + 2 frontends
- S3 bucket (replace MinIO, free 5GB)
- ALB (Cloudflare proxy front — ALB optional)
- Route53 hosted zone (or Cloudflare DNS free)
- Secrets Manager (8-10 secrets)
- IAM roles (EC2 instance profile, ECR pull, RDS access)
- Security groups (web 443/80, DB 5432 internal only)

## Proposed Fix

1. Audit current `infrastructure/terraform-aws/*.tf` scope
2. Add missing resources per Architecture B
3. Variables file `terraform.tfvars.example` với AWS region pin + sizing
4. Apply per `release-deploy-standard.md` §9 — agent writes code, human executes `terraform apply` (Phase 2 BANNED for agent)

## Acceptance Criteria

- [ ] All 11 resource categories above present in `.tf` files
- [ ] `terraform plan` returns 0 resources to add nếu run twice (idempotent)
- [ ] Cost projection comment trong `main.tf` matches Architecture B $72/mo
- [ ] No secrets hardcoded; reference Secrets Manager ARN

## Related

- ADR-025 AWS Singapore Free Tier
- `release-deploy-standard.md` §3.4 MAJOR checklist
- GAP-103 deployment-strategy.md (DONE)
- Sister: GAP-396 (state backend), GAP-397 (plan CI)
