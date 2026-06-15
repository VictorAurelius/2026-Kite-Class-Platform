---
title: AWS cost-reduction sweep — bill spike investigation + teardown
date: 2026-06-15
scope: aws-cost-reduction
authorized-by: user (2026-06-15 "bill AWS tăng đột biến, check và giảm toàn bộ, cloudwatch cũng nên tắt đi")
account: 906286017800
region: ap-southeast-1
---

# AWS Cost-Reduction Sweep — 2026-06-15

## 1. Investigation (Cost Explorer, read-only Tier 1)

| Service | June 1–15 | May (full) | Note |
|---|---|---|---|
| ECR | $43.10 | $36.16 | #1 driver — 206 GB / ~2,746 images (lifecycle not pruning) |
| CloudWatch | $6.30 | $0 | dashboard ($3) + 16 alarms + 3.1 GB log group `/aws/ec2/kite-prod` ret=never |
| RDS | $6.23 | $0 | stopped instance still charged for 20 GB storage |
| EC2 - Other | $3.52 | $0 | 3× gp3 EBS (80 GB) on stopped instances + 1 EIP IPv4 charge |
| Secrets Manager | $2.68 | $0 | 16 secrets × $0.40/mo |
| VPC | $1.66 | $0 | IPv4/EIP idle charge (no NAT gateway present) |
| **TOTAL** | **$67.50** | **$36.18** | June run-rate ~2× |

Daily cost: spike 01–10 June (CI builds + cosign incident 06-10 $8.78); 11–15 June already <$1/day.

## 2. Actions taken (Tier 3, user pre-authorized)

### Tier 1 — SAFE / reversible (executed)
- **ECR prune:** kept 3 newest images per repo, deleted **2,719 images** (206 GB → ~12 GB). Rebuildable from CI on next merge.
- **CloudWatch teardown:** deleted dashboard `kitehub-phase-1-overview`, 16 metric alarms, log groups `/aws/ec2/kite-prod` (3.1 GB) + `kite-prod-kc` + 2 lambda + ssm. CloudTrail trail `kitehub-main` + its CW log group RETAINED (security baseline).

### Tier 2 — DESTRUCTIVE (pending user decision)
- RDS `kitehub-postgres` (stopped, ~$12/mo storage) — snapshot + delete vs keep stopped.
- EC2 ×3 stopped + 80 GB EBS + EIP `52.221.161.175` — terminate vs keep restartable.
- Secrets Manager 16 secrets ($6.4/mo) — delete vs keep.

## 3. Follow-ups
- **ECR lifecycle policy** (`infrastructure/terraform-aws/ecr.tf`) not pruning — 2,746 images accumulated. Verify lifecycle `countNumber` + untagged/sig handling. Per `retention-policy-completeness.md`.
- Hook gap: `aws ecr batch-delete-image` not caught by `pre-tool-guard.py` AWS_TIER3_RE (verb `batch-delete-` ≠ `delete-`). File meta-gap.
- CloudWatch alarms/dashboard re-provision via terraform when AWS stack restarts (security alarms: root-account-use, failed-iam-auth, sg-changes, secrets-burst).

AGENT_AWS_TIER3_OK: user pre-authorized 2026-06-15 cost-reduction sweep (ECR prune + CloudWatch teardown); destructive RDS/EC2/secrets deferred to explicit confirmation.
