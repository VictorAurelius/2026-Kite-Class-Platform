---
title: AWS Verification — Current state inventory (post Wave 43-46 + key rotation)
status: complete
created: 2026-05-08
phase: post-Wave-46
---

# AWS Verification Report — Current State Inventory

## Scope

Snapshot kiểm tra trạng thái mọi production resource sau:
- Wave 43 cost-discipline (EventBridge stop/start + EC2 right-size)
- Wave 44 terraform-apply workflow_dispatch
- Wave 46 Java deps + Alpine base bumps
- PR #1064 IAM key rotation

Profile: `kite-readonly` (newly rotated AKID `AKIA…SVMD`).

## Commands run

| # | Command | Tier | Notes |
|---|---|---|---|
| 1 | `aws sts get-caller-identity` | 1 | Identity ✓ |
| 2 | `aws ec2 describe-instances` | 1 | Compute inventory |
| 3 | `aws rds describe-db-instances` | 1 | DB inventory |
| 4 | `aws elbv2 describe-load-balancers` + `describe-target-groups` + `describe-target-health` | 1 | LB state |
| 5 | `aws scheduler list-schedules` | 1 | Wave 43 scheduler verify |
| 6 | `aws cloudwatch describe-alarms` | 1 | Alarm state |
| 7 | `aws cloudtrail describe-trails` + `get-trail-status` | 1 | Audit baseline |
| 8 | `aws secretsmanager list-secrets` | 1 | Metadata only |
| 9 | `aws iam list-roles` | 1 | Role inventory |
| 10 | `aws ecr describe-repositories` | 1 | Container registry |
| 11 | `aws s3api list-buckets` | 1 | Storage inventory |
| 12 | `aws ec2 describe-vpcs` + `describe-subnets` | 1 | Network |

## Results

### Compute — all stopped (cost-saving working ✓)

| Resource | ID | State | Type | Notes |
|---|---|---|---|---|
| kh-backend EC2 | `i-0b65c3947d36cae61` | **stopped** | t3.medium | Right-sized Wave 43 (was m7i-flex.large) |
| kc-app EC2 | `i-07f6de54544162124` | **stopped** | t3.medium | Same |
| RDS postgres | `kitehub-postgres` | **stopped** | db.t3.micro | 20GB, no MultiAZ, private |
| ALB `kitehub-alb` | `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` | **active** | internet-facing | Kept running per Wave 43 design (DNS reuse, ~$22/mo) |
| Target health | i-0b65c…:8080 | `unused` (Target.InvalidState) | — | Expected — instance stopped |

### EventBridge Scheduler (Wave 43) — 8/8 ENABLED ✓

Group: `kitehub-cost-saving`. All schedules ACTIVE:
- `start-weekday-morning-{ec2,rds}` × 2
- `stop-weekday-evening-{ec2,rds}` × 2
- `start-monday-morning-{ec2,rds}` × 2
- `stop-friday-evening-{ec2,rds}` × 2

### CloudWatch alarm

| Alarm | State | Threshold | Reason |
|---|---|---|---|
| `kitehub-kh-backend-memory-high` | **OK** | 85% mem_used_percent | No datapoints (treat as NonBreaching) |

⚠️ Alarm OK = false-positive — CWAgent chưa install (ROADMAP user action #3 pending). Khi CWAgent live, alarm sẽ transition INSUFFICIENT_DATA → real OK/ALARM.

### CloudTrail audit baseline ✓

| Field | Value |
|---|---|
| Trail | `kitehub-main` |
| Multi-region | True |
| Log file validation | True |
| S3 bucket | `kitehub-cloudtrail-logs-906286017800` |
| IsLogging | **True** |
| Latest delivery | 2026-05-08T23:48:50 ICT (current) |

### Secrets Manager — 9 production secrets

All under `kitehub/production/`:
- `jwt-secret`, `db-password`, `encryption-key`, `internal-api-secret`
- `ses-smtp-credentials`, `cloudflare-api-token`
- `ai-anthropic-api-key`, `ai-openai-api-key`
- `rabbitmq-default-creds`

Rotation: None enabled — manual.

### IAM roles — 7 OIDC/service roles

| Role | Purpose |
|---|---|
| `kitehub-github-deploy` | OIDC SSM deploy (Phase 4) |
| `kitehub-github-ecr-push` | OIDC ECR push (CI) |
| `kitehub-github-restore-drill` | OIDC restore validation |
| `kitehub-github-terraform-plan` | OIDC plan-only (read-mostly) |
| `kitehub-github-terraform-apply` | OIDC workflow_dispatch apply (Wave 44) |
| `kitehub-production-ec2-app` | EC2 instance profile |
| `kitehub-production-scheduler-executor` | EventBridge Scheduler service role (Wave 43) |

### ECR repositories — 10 container registries

`kite/{kitehub-gateway, kitehub-frontend, kitehub-platform, kitehub-email, kiteclass-frontend, kiteclass-core, kitehub-branding, kitehub-subscription, kitehub-admin, kiteclass-gateway}`. All `MUTABLE` tag policy (Phase 1 BETA — sẽ tighten cho production v1.0.0 release).

### S3 buckets — 3

| Bucket | Purpose |
|---|---|
| `kitehub-terraform-state-906286017800` | TF backend state |
| `kitehub-cloudtrail-logs-906286017800` | Audit baseline |
| `kitehub-assets-production-906286017800` | App assets |

### VPC + subnets

| Resource | Detail |
|---|---|
| VPC | `vpc-0fdff7788ffc434fe` 10.0.0.0/16 |
| Public subnets | `kitehub-public-0` (1a, 10.0.0.0/24) + `kitehub-public-1` (1b, 10.0.1.0/24) |
| Private subnets | `kitehub-private-0` (1a, 10.0.10.0/24) + `kitehub-private-1` (1b, 10.0.11.0/24) |

Default VPC (172.31.0.0/16, 3 auto-subnets) cũng tồn tại nhưng không dùng — có thể delete sau (separate gap).

## Findings

### ✅ Healthy

1. **Cost-saving active** — All 4 stoppable resources (2 EC2 + 1 RDS) STOPPED at 23:54 ICT (post-evening-stop schedule).
2. **CloudTrail logging** — multi-region, log-file-validation, current. Audit baseline solid per `aws-observability-first.md`.
3. **EventBridge Scheduler** — 8/8 schedules ENABLED, no drift.
4. **OIDC infra** — 5 GitHub OIDC roles in place; workflow_dispatch apply path (Wave 44) ready.

### ⚠️ Pending / known gaps

1. **CWAgent not installed** (ROADMAP #3) — memory alarm currently false-OK. Block: Phase 7 stress test pending.
2. **Secret rotation not automated** — 9 secrets manual-only; defer cho post-v1.0.0.
3. **GAP-450 drift** (ROADMAP #4) — `random_password` state drift unresolved.
4. **`solo-dev-admin` rotation** (ROADMAP #1) — script `scripts/rotate-iam-access-key.sh` reusable.
5. **Default VPC unused** — orphan resource, low priority cleanup.

### 💰 Cost posture estimate (current state)

| Component | Monthly cost |
|---|---|
| ALB (24/7) | ~$22 |
| EBS volumes (stopped) | ~$3 |
| RDS storage (stopped) | ~$2 |
| Secrets Manager (9 × $0.40) | ~$3.60 |
| S3 storage | ~$1 |
| EC2 stop/start runtime | ~$25 (when within schedule) |
| **Effective** | **~$45-55/mo** |

→ Aligned with Wave 43 target. $200 credit longevity ~3.5-4 tháng.

## Next steps

Không có action mới required từ check này — state khớp expected. Pending items đã track trong ROADMAP user actions #1, #3, #4.
