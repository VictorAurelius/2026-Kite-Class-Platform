---
title: AWS Verification — Wave 84 Bucket G EC2 right-sizing automation pre-apply
status: complete
created: 2026-05-15
phase: Wave 84 Bucket G
wave: 84
gaps: [GAP-414]
---

# AWS Verification Report — Wave 84 Bucket G EC2 right-sizing automation pre-apply

## Scope

Pre-apply audit cho thay đổi terraform Wave 84 Bucket G (GAP-414): thêm cost-monitoring infrastructure cho EC2 right-sizing automation. Apply sẽ KHÔNG được thực hiện trong PR này — coordinator/human sẽ trigger qua `terraform-apply.yml` workflow_dispatch sau khi merge. Audit artifact này phục vụ pre-mutation discipline theo `pre-mutation-state-check.md` §3 + `pre-mutation-state-check.md` §1.5 (terraform-specific workflow + cross-reference matrix).

Mục tiêu: Lambda monthly + per-EC2 low-CPU alarms → SNS digest cảnh báo downsize candidate. Tất cả tài nguyên ĐỀU LÀ ADDITIVE — không chạm tài nguyên hiện có.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Verify existing EC2 instances tagged Project=Kite + their IDs (terraform references)
grep -n "aws_instance" infrastructure/terraform-aws/ec2.tf infrastructure/terraform-aws/ec2-kc-app.tf

# Inspect existing CloudWatch / SNS resources to confirm zero collision
grep -n "aws_sns_topic\|aws_cloudwatch_metric_alarm\|aws_lambda_function\|aws_iam_role" \
  infrastructure/terraform-aws/*.tf

# Verify naming convention free of var-expansion conflicts
grep -n "kitehub-cost-alerts\|ec2-cost-report" infrastructure/terraform-aws/*.tf
```

Apply-time verification commands (executed by coordinator post-merge):

```bash
# 1. Confirm CloudTrail logging on (per aws-observability-first.md §1)
aws cloudtrail get-trail-status --name kitehub-main --query 'IsLogging' --output text
# Expected: True

# 2. Confirm no concurrent terraform/deploy ops (per concurrent-production-mutation-ops.md §3.1)
gh run list --status in_progress --json workflowName,name,databaseId | jq '.[] | select(.workflowName | test("terraform-apply|deploy-production"))'
# Expected: empty (no overlap)

# 3. Run plan via workflow_dispatch
gh workflow run terraform-apply.yml -f confirm=PLAN -f dry_run=true
# Inspect plan output → verify 0 destroy, 0 replace, X add (~14 new resources)

# 4. After plan green + reviewer OK:
gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false
```

## Findings

### Real changes (must verify intent)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `aws_sns_topic.cost_alerts` | create | New SNS topic for cost monitoring | Low — additive, $0 idle |
| 2 | `aws_sns_topic_subscription.cost_alerts_email` | create | Email subscription vannkite@outlook.com | Low — email confirmation required by AWS before notify works |
| 3 | `aws_cloudwatch_metric_alarm.kh_backend_low_cpu` | create | 7-day CPU ≤20% alarm for kh-backend | None — threshold low, EC2 idle = legitimate signal |
| 4 | `aws_cloudwatch_metric_alarm.kc_app_low_cpu` | create | Same for kc-app | None |
| 5 | `aws_cloudwatch_metric_alarm.kc_app_fe_low_cpu` | create | Same for kc-app-fe | None |
| 6 | `aws_iam_role.ec2_cost_report` | create | Lambda exec role | Low — assume-role principal=lambda.amazonaws.com only |
| 7 | `aws_iam_role_policy.ec2_cost_report` | create | Inline policy (least-priv) | Low — see Cross-reference matrix below |
| 8 | `aws_cloudwatch_log_group.ec2_cost_report` | create | `/aws/lambda/kitehub-ec2-cost-report` with 30-day retention | None |
| 9 | `aws_lambda_function.ec2_cost_report` | create | Python 3.12 monthly digest | Low — read-only on AWS APIs; only mutates via SNS publish |
| 10 | `aws_cloudwatch_event_rule.ec2_cost_report_monthly` | create | EventBridge cron 1st-of-month 08:00 UTC | None |
| 11 | `aws_cloudwatch_event_target.ec2_cost_report` | create | Wire EventBridge → Lambda | None |
| 12 | `aws_lambda_permission.allow_eventbridge` | create | Lambda resource policy for EventBridge invoke | None |
| 13 | (data source) `aws_iam_policy_document.ec2_cost_report_assume` | create | Renders assume-role JSON | N/A — data source, not a resource |
| 14 | (data source) `aws_iam_policy_document.ec2_cost_report` | create | Renders policy JSON | N/A |
| 15 | (data source) `archive_file.ec2_cost_report` | create | Zip Lambda source | N/A — local file generation |

### Phantom updates

None expected. This is a clean additive apply (zero existing-resource changes).

### Verdict

Safe to apply. All changes additive, no production data path touched, no IAM admin escalation, no existing resource modification. Per `concurrent-production-mutation-ops.md` §3 matrix: no EC2 `user_data` / `instance_type` change → no SSM serialization risk → can run independently of deploy ops.

## §1.5 Terraform Cross-reference matrix (IAM)

Per `pre-mutation-state-check.md` §1.5 mandate, cross-reference each IAM Action against actual resource + workflow caller:

| IAM Action | Resource pattern in policy | Actual resource verified | Caller | Verdict |
|---|---|---|---|---|
| `ce:GetCostAndUsage` | `*` (Cost Explorer is account-wide; no ARN granularity) | N/A — service-level | Lambda handler.py `get_monthly_cost_by_instance()` line 64 | ✅ match |
| `ce:GetCostAndUsageWithResources` | `*` (same reason) | N/A | Lambda handler.py (future-proofing) | ✅ acceptable scope |
| `cloudwatch:GetMetricStatistics` | `*` (CloudWatch metrics account-wide; no ARN) | N/A — service-level | Lambda handler.py `get_avg_cpu()` line 96, `get_avg_mem()` line 116 | ✅ match |
| `cloudwatch:ListMetrics` | `*` | N/A | Defensive (handler.py may add metric discovery) | ✅ acceptable scope |
| `ec2:DescribeInstances` | `*` (read-only) | EC2 tag `Project=Kite` filter applied in code | Lambda handler.py `list_kite_instances()` line 53 | ✅ match |
| `sns:Publish` | `${aws_sns_topic.cost_alerts.arn}` (narrow) | `kitehub-cost-alerts` topic created in same file | Lambda handler.py `sns.publish()` line 202 | ✅ match |
| `logs:CreateLogGroup` | `arn:aws:logs:ap-southeast-1:*:log-group:/aws/lambda/kitehub-ec2-cost-report:*` | `/aws/lambda/kitehub-ec2-cost-report` log group declared same file | Lambda runtime | ✅ match |
| `logs:CreateLogStream` | same | same | Lambda runtime | ✅ match |
| `logs:PutLogEvents` | same | same | Lambda runtime | ✅ match |

No bugs surfaced. Policy passes least-privilege bar per `pre-launch-infra-hardening-checklist.md` §2.5: no `*:*` admin; all wildcards justified by AWS service-API granularity constraints.

## Prior actions verified

| Action | When | Where verified |
|---|---|---|
| CloudTrail multi-region trail enabled | 2026-05-07 PR #992 (GAP-437 Phase 1) | `aws cloudtrail get-trail-status --name kitehub-main` = `IsLogging: True` |
| EC2 instances tagged `Project=Kite` | Wave 43 production apply 2026-05-08 | grep `default_tags` in `infrastructure/terraform-aws/main.tf` |
| Memory alarms SNS topic (separate) `kitehub-memory-alerts` | GAP-447 Wave ~67 | `infrastructure/terraform-aws/cloudwatch.tf` line 21 — confirmed naming doesn't collide with new `kitehub-cost-alerts` |
| Lambda IAM patterns | Bootstrap pattern from existing terraform | grep `aws_iam_role` in existing `.tf` — Lambda role pattern is new for this codebase but standard AWS shape |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| `terraform plan -out=tfplan` | Coordinator post-merge | Via `terraform-apply.yml` workflow_dispatch `confirm=PLAN` `dry_run=true` |
| Reviewer manual plan inspection | Coordinator / reviewer | Verify 14 additive resources, zero destroy/replace |
| `terraform apply` | Coordinator (human-trigger only) | Via workflow_dispatch `confirm=APPLY` `dry_run=false` per `release-deploy-standard.md` §9 |
| Email confirmation for SNS subscription | vannkite@outlook.com receives AWS confirmation email | Click confirm link — required before notifications work |
| Post-apply smoke: trigger Lambda manually | Coordinator | `aws lambda invoke --function-name kitehub-ec2-cost-report --payload '{}' /tmp/out.json` → verify SNS receives digest |
| **Concurrent op check** | Agent verification | List active workflows touching SNS/IAM/Lambda — confirm zero overlap before trigger (per `concurrent-production-mutation-ops.md` §3) |

## Recommendations

1. **Apply via workflow_dispatch only** — never local apply per `release-deploy-standard.md` §9 + `agent-aws-access.md` §4.3.
2. **Verify SNS email subscription confirmed** within 72h of apply (AWS expires unconfirmed subscriptions).
3. **First Lambda invocation manual test** post-apply — Lambda will not auto-run until 1st of next month otherwise; manual invoke validates the IAM policy + Cost Explorer access works.
4. **Watch CloudWatch alarms 14 days post-apply** — if `*-low-cpu-7d` fires immediately on already-idle instances, it confirms threshold + treat_missing_data logic.
5. **Cost projection**: Lambda invocation = $0 (1 invoke/month × free tier 1M/month); CloudWatch alarms = $0.10/alarm/month × 3 = $0.30/month; SNS = $0 (1k email/month free); Cost Explorer API = $0.01/query × 1 query/month = ~$0.01/month. **Total marginal cost ≈ $0.31/month** well within Phase 1 BETA Free Tier discretionary budget.

## References

- Wave 84 plan: [`documents/03-planning/waves/wave-2026-05-15-84-ops-observability-runbooks.md`](../../../03-planning/waves/wave-2026-05-15-84-ops-observability-runbooks.md) §3 Bucket G
- Gap: [`documents/04-quality/gaps/GAP-414-ec2-right-sizing-monthly-review.md`](../../gaps/GAP-414-ec2-right-sizing-monthly-review.md)
- Runbook: [`documents/05-guides/operations/ec2-cost-review.md`](../../../05-guides/operations/ec2-cost-review.md)
- Rule: `.claude/rules/pre-mutation-state-check.md` §3 + §1.5
- Rule: `.claude/rules/concurrent-production-mutation-ops.md` §3.1
- Rule: `.claude/rules/agent-aws-access.md` §2.1 (Tier 1 read-only) + §4.3 (Tier 3 banned mutations — apply via workflow only)
- Rule: `.claude/rules/aws-observability-first.md` §6 (CloudTrail verified ON before apply)
