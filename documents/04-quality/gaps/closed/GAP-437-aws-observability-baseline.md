# GAP-437: AWS observability baseline — CloudTrail + dashboard

**Status:** 🟡 PARTIAL (Phase 1 DONE Wave 81; Phase 2-3 terraform plan ready Wave 84 Bucket A, apply pending coordinator)
**Priority:** 🟠 P1 (compliance + security baseline gap)
**Domain:** DevOps / Security / Compliance
**Found:** 2026-05-08 (user-flagged during Phase 2.2 review — "tera đã được chạy nhưng không có logs hay dashboard kiểm soát")
**Affects:** Entire AWS account 906286017800 — no audit trail of API actions, no resource visibility dashboard.

## Problem

Phase 2.1 + 2.2 đã apply 10 AWS resources (S3 + DynamoDB + IAM OIDC role) nhưng:
- **Không có CloudTrail** → API call audit log disabled (default mới account)
- **Không có CloudWatch dashboard** → không có visual view các resource live
- **Không có AWS Config** → drift detection disabled

Hệ quả:
- Nếu credentials leak → không biết attacker đã làm gì
- Compliance gap: PDPL 2023 + ISO27001 + SOC2 đều require audit log
- Debug khó khăn khi state lệch

## Root Cause

AWS account default config:
- CloudTrail disabled cho new accounts (cần explicit enable)
- AWS Config disabled (cost tier — $0.003/recorded item)
- Default Cost Explorer cần activate manual qua console
- Không có CloudWatch dashboard tự động

`infrastructure/terraform-aws/` Wave 37 PR #938 ship 13 .tf files (VPC, EC2, RDS, ECR, ALB, S3 assets, Route53, Secrets Manager) **không bao gồm** observability resources (`cloudtrail.tf`, `cloudwatch-dashboard.tf`, `aws-config.tf`).

## Proposed Fix — Phased

### Phase 1 — CloudTrail (this PR, ~30min)

**Goal:** Audit baseline NGAY. Capture all AWS API calls cho debugging + compliance.

Add `infrastructure/terraform-aws/cloudtrail.tf`:
- `aws_s3_bucket "cloudtrail_logs"` — separate audit log bucket (encryption + lifecycle 90d expire)
- `aws_s3_bucket_policy` — allow CloudTrail service principal write
- `aws_cloudtrail "main"` — multi-region trail, log validation enabled, management events default
- Output: `cloudtrail_log_bucket` + `cloudtrail_arn`

**Cost projection:**
- Management events: FREE (first copy)
- S3 storage Phase 1 BETA volume ~30MB/mo = $0 (Free Tier 5GB)
- Post-free-tier: ~$0.0007/mo (negligible)

### Phase 2 — CloudWatch Dashboard (follow-up, ~30min)

Add `infrastructure/terraform-aws/cloudwatch-dashboard.tf`:
- 1 dashboard `kitehub-phase-1-overview` (Free Tier 3 dashboards)
- Widgets:
  - S3 bucket size (terraform-state + cloudtrail-logs + assets buckets)
  - DynamoDB consumed capacity
  - IAM CloudTrail metric filter — failed sign-ins + role assumptions

Cost: $0 (1 dashboard free).

### Phase 3 — AWS Config (Phase 2.3+, ~$5-15/mo)

Defer until full Architecture B applied (EC2/RDS/ALB give Config something to track). Cost-benefit at <10 resources is poor.

### Phase 4 — Application metrics → CloudWatch Logs (overlap with GAP-115)

Out of scope here — GAP-115 covers app-level Loki/Promtail.

## Acceptance Criteria

### Phase 1 (this PR)
- [ ] `cloudtrail.tf` defines trail + audit bucket + bucket policy
- [ ] Targeted `terraform apply` clean (4-5 resources)
- [ ] `aws cloudtrail describe-trails` returns the trail
- [ ] First management event captured to S3 bucket within 15min of apply
- [ ] Output `cloudtrail_log_bucket` available

### Phase 2 (follow-up PR)
- [ ] `cloudwatch-dashboard.tf` defines dashboard + 3-4 widgets
- [ ] Dashboard URL accessible in console
- [ ] Failed-IAM-action metric filter wired

## Related

- Phase 2.1 bootstrap (this session): created S3 + DynamoDB without audit
- Phase 2.2 OIDC plan role (this session): created OIDC + IAM role without audit
- Phase 2.3 production apply: SHOULD wait for Phase 1 of this gap (audit baseline before infra explosion)
- GAP-115 log aggregation (different scope — app-level)
- GAP-135 SLO documentation (different scope)
- GAP-379 secrets management (relevant — secrets access audit needs CloudTrail)
- ADR-025 AWS Singapore — compliance basis demands audit trail

## Log

- **2026-05-15 (Wave 84 Bucket A):** Status flip OPEN → 🟡 PARTIAL. Phase 1 CloudTrail trail `kitehub-main` shipped Wave 81 (`IsLogging=true` verified per `aws-observability-first.md` §8 self-test). Phase 2-3 terraform plan ready in this PR — 4 metric filters (failed IAM auth, root account use, SG changes, secrets access) + CloudWatch log group + IAM delivery role + dashboard extension (3 new EC2 metrics on kc_app_fe + ALB health/latency widgets + RDS IOPS + 4 security event widgets) + SNS topic `kitehub-security-alerts` + 4 alarms. Pre-mutation state-check artifact: `documents/04-quality/audits/aws-verification/2026-05-15-wave-84-bucket-a-cloudtrail-observability-plan.md`. Apply deferred to coordinator per task spec ("DO NOT run terraform apply") — coordinator triggers `terraform-apply.yml` `dry_run=true` first then `confirm=APPLY`. Files: `cloudtrail.tf` (edit — wire CWL delivery), `cloudtrail-metric-filters.tf` (new), `cloudwatch-dashboard.tf` (edit — extend widgets), `cloudwatch-security-alarms.tf` (new). Terraform `validate` PASS; `fmt -check` clean. Cost projection: ~$0.03/mo within Free Tier. AC remaining: live verify post-apply (trail CWL delivery, metric filter firing, dashboard renders, SNS subscription confirmed). Phase 4 (AWS Config drift) deferred per original scope.
- **2026-05-08:** GAP filed during user review of Phase 2.2 — observability gap noted ("không có logs hay dashboard kiểm soát"). Phase 1 scope = CloudTrail only, target ~30min.
