# GAP-395: Terraform AWS Singapore Production Stack Provision

**Status:** 🟢 DONE 2026-05-09 — Wave 37 Bucket A
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

- [x] All 11 resource categories above present in `.tf` files
- [x] `terraform validate` passes — agent verification gate per GAP-381 Phase 2 ban (live `terraform plan` runs human-side once AWS account ready, tracked in GAP-394)
- [x] Cost projection comment trong `main.tf` matches Architecture B ~$25-72/mo Yr1
- [x] No secrets hardcoded; reference Secrets Manager ARN

## Resolution

Wave 37 Bucket A (PR #TBD) shipped Architecture B Terraform stack:

- **VPC** (`vpc.tf`) — 2 public + 2 private subnets (2 AZ for RDS subnet-group requirement); NAT Gateway optional flag (default false for cost saving ~$32/mo).
- **EC2** (`ec2.tf`) — 2 instances (kh-backend t3.micro + kc-app t3.micro) + ALB + 2 target groups + path-based listener routing. Cloud-init installs Docker + Compose v2 + ECR login helper.
- **RDS** (`rds.tf`) — PostgreSQL 15 db.t3.micro, single-AZ Phase 1, 20GB gp3 storage, 7-day backup retention, encrypted.
- **ECR** (`ecr.tf`) — 10 repos using `kite/<service>` naming convention (consumed by Bucket B), lifecycle policy keeps last 10 + expires untagged after 7d.
- **S3** (`s3.tf`) — assets bucket with versioning + AES256 encryption + 90-day non-current expiry.
- **Secrets Manager** (`secrets.tf`) — 3 generated (db-password / jwt / encryption) + 5 placeholders (ses-smtp / cloudflare / openai / anthropic / rabbitmq) for user post-apply population.
- **IAM** (`iam.tf`) — EC2 instance profile (ECR + Secrets + S3 + SSM + CloudWatch); GitHub OIDC provider + read-only role for terraform-plan workflow.
- **Security Groups** (`security-groups.tf`) — ALB 80/443 from internet; EC2 app from ALB only; RDS 5432 from EC2 SG only.
- **Route53** (`route53.tf`) — optional, default disabled (Cloudflare DNS primary per ADR-018).

Region pinned to `ap-southeast-1` via variable validation block. `terraform fmt -check` ✅, `terraform validate` ✅ on both root + bootstrap modules. Live `terraform apply` is human-only per GAP-381 Phase 2 ban; runbook in `README.md` Quick Start §6.

## Related

- ADR-025 AWS Singapore Free Tier
- `release-deploy-standard.md` §3.4 MAJOR checklist
- GAP-103 deployment-strategy.md (DONE)
- Sister: GAP-396 (state backend), GAP-397 (plan CI)
- Wave plan: `documents/03-planning/waves/wave-2026-05-09-37-release-hardening-5-layer.md` Bucket A

## Log

- **2026-05-09:** Status flipped 🔵 OPEN → 🟢 DONE. Wave 37 Bucket A ship: 13 .tf files (main/backend/variables/vpc/security-groups/iam/ec2/rds/ecr/s3/secrets/route53/outputs) + tfvars example + README. Verification: `terraform fmt -check -recursive` exit 0; `terraform init -backend=false && terraform validate` Success. Human-side `terraform apply` runbook in README per GAP-381 Phase 2 separation; tracked in GAP-394 (AWS account creation).
