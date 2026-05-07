# Kite Platform — Terraform AWS Singapore (Phase 1 BETA)

Architecture B per [ADR-025](../../documents/02-architecture/adr/ADR-025-aws-only-deploy-phase-1-free-tier.md) — AWS Free Tier, region `ap-southeast-1`.

Closes GAP-395 (production stack), GAP-396 (state backend), GAP-397 (plan CI).

## Resources Provisioned

| Resource | Type | Free Tier | Phase 1 cost |
|----------|------|-----------|--------------|
| VPC + 2 public + 2 private subnets | Network | Free | $0 |
| 2× EC2 instances (kh-backend + kc-app) | t3.micro | 750h/mo (1 inst 24/7) | ~$8.5/mo (over-cap) |
| RDS PostgreSQL | db.t3.micro | 12 months | $0 → ~$13/mo Yr2 |
| ALB | Application Load Balancer | — | ~$16/mo |
| ECR | 10 private repos | 500 MB | ~$0.5/mo |
| S3 (assets) | Standard storage | 5 GB | ~$0.5/mo |
| Secrets Manager | 8 secrets × $0.40 | — | ~$3.2/mo |
| Route53 (optional) | Hosted zone | — | ~$0.5/mo |
| **Total Yr1 estimate** | | | **~$25-40/mo (within free tier headroom)** |

NAT Gateway disabled by default (~$32/mo saving).

## Quick Start

```bash
# 1. Bootstrap state backend (ONE-TIME)
cd bootstrap
terraform init
terraform apply
# Note the state_bucket_name output

# 2. Update backend.tf with bucket name from step 1
cd ..
# Edit backend.tf: replace <ACCOUNT_ID> with the bucket name's account-id portion

# 3. Configure variables
cp terraform.tfvars.example terraform.tfvars
# Edit if needed (defaults are Phase 1 BETA Architecture B)

# 4. Initialize backend (migrates state to S3)
terraform init

# 5. Validate + plan (no apply!)
terraform fmt -check
terraform validate
terraform plan

# 6. Apply (HUMAN ONLY — agent is BANNED per GAP-381)
terraform apply
```

## Outputs (post-apply)

```bash
terraform output kh_backend_public_ip       # EC2 IP for KH backend
terraform output kc_app_public_ip           # EC2 IP for KC app
terraform output alb_dns_name               # ALB DNS for Cloudflare CNAME
terraform output rds_endpoint               # RDS connection endpoint
terraform output ecr_registry_url           # ECR registry for docker tag
terraform output github_terraform_plan_role_arn  # OIDC role ARN for CI
terraform output -raw next_steps            # Post-apply user actions
```

## File Layout

```
terraform-aws/
├── main.tf              # Provider + version constraints + cost projection
├── backend.tf           # S3 + DynamoDB lock backend (GAP-396)
├── variables.tf         # All variables with Phase 1 BETA defaults
├── terraform.tfvars.example
├── vpc.tf               # VPC + 2 public + 2 private + optional NAT
├── security-groups.tf   # ALB / EC2 / RDS SGs
├── ec2.tf               # 2 instances + ALB + target groups + listeners
├── rds.tf               # PostgreSQL db.t3.micro
├── ecr.tf               # 10 repos with `kite/<service>` naming
├── s3.tf                # Assets bucket
├── secrets.tf           # 3 generated + 5 placeholder secrets
├── iam.tf               # EC2 instance profile + GitHub OIDC for CI plan
├── route53.tf           # Optional hosted zone (Cloudflare primary)
├── outputs.tf           # Compute + database + storage + IAM + next-steps
├── bootstrap/           # State backend bootstrap (run ONCE)
│   ├── main.tf          # S3 state bucket + DynamoDB lock table
│   └── README.md
└── modules/
    └── dns/             # Cloudflare DNS module (skeleton — GAP-191 follow-up)
```

## CI: terraform-plan workflow (GAP-397)

`.github/workflows/terraform-plan.yml` runs `terraform fmt -check` + `terraform validate` + `terraform plan` on PRs touching `infrastructure/terraform-aws/**`. Uses GitHub OIDC (no static keys). Plan output is posted as PR comment.

## Region pin

Region pinned to `ap-southeast-1` via `var.aws_region` validation block. Migrating to a different region (Phase 3 VN-resident cloud cutover) requires:
1. Update validation in `variables.tf`
2. Update region in `backend.tf`
3. Re-run `bootstrap/` in new region
4. Migrate state with `terraform init -migrate-state`
5. New ADR superseding ADR-025

## Compliance

Phase 1 BETA infrastructure runs in Singapore (`ap-southeast-1`) — VN data localization compliance debt acknowledged per ADR-025 §Negative. Risk-managed via:
- Invite-only (~10-20 tenants) — sub-regulator-radar
- Explicit consent flow per Bucket B (PDPL Phase 2)
- Phase 3 GA gate = counsel review OR data-layer migration to VN cloud

## Destroy

```bash
terraform destroy   # WARNING: irreversible — destroys ALL Phase 1 resources
# Then bootstrap teardown:
cd bootstrap && terraform destroy
```
