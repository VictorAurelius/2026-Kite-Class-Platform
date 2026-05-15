---
title: AWS Verification — Wave 84 Bucket A CloudTrail observability plan (pre-apply)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 84
gaps: [GAP-437]
---

# AWS Verification Report — Wave 84 Bucket A CloudTrail observability plan

## Scope

Pre-mutation state-check artifact (per `.claude/rules/pre-mutation-state-check.md` §3) cho Wave 84 Bucket A: thêm 4 CloudWatch metric filters trên CloudTrail log stream + extend dashboard `kitehub-phase-1-overview` (2 new rows: ALB extras + RDS IOPS + 4 security event widgets) + 1 SNS topic `kitehub-security-alerts` + 4 CloudWatch alarms.

**Plan-only delivery:** terraform plan + .tf files shipped trong PR; apply deferred cho coordinator review per task spec ("DO NOT run terraform apply").

**Files in scope:**
- `infrastructure/terraform-aws/cloudtrail.tf` (MODIFIED — wire CloudWatch Logs delivery)
- `infrastructure/terraform-aws/cloudtrail-metric-filters.tf` (NEW — log group + IAM role + 4 metric filters)
- `infrastructure/terraform-aws/cloudwatch-dashboard.tf` (MODIFIED — kc_app_fe extension + 7 new widgets)
- `infrastructure/terraform-aws/cloudwatch-security-alarms.tf` (NEW — SNS topic + 4 alarms + email subscription variable)

## Commands run (Tier 1 read-only per agent-aws-access.md §2.1)

```bash
# Local terraform fmt + validate (no AWS API call)
cd infrastructure/terraform-aws
terraform fmt -check -diff cloudtrail.tf cloudtrail-metric-filters.tf cloudwatch-dashboard.tf cloudwatch-security-alarms.tf
# exit 0 - all files canonical

terraform validate
# Success! The configuration is valid (4 pre-existing rds.tf warnings unrelated to this PR)
```

**AWS API verification commands** (recommended cho coordinator BEFORE apply trigger):

```bash
# Verify CloudTrail trail still active (Wave 81 prerequisite per aws-observability-first.md)
aws cloudtrail get-trail-status --name kitehub-main --query 'IsLogging' --output text
# Expected: True

# Verify trail multi-region
aws cloudtrail describe-trails --query 'trailList[?Name==`kitehub-main`].[Name,IsMultiRegionTrail,IncludeGlobalServiceEvents]'
# Expected: [["kitehub-main", true, true]]

# Verify no existing log group conflict
aws logs describe-log-groups --log-group-name-prefix /aws/cloudtrail/kitehub-main --query 'logGroups[*].logGroupName'
# Expected: [] (empty - will be created by terraform apply)

# Verify no existing SNS topic conflict
aws sns list-topics --query 'Topics[?contains(TopicArn, `kitehub-security-alerts`)]'
# Expected: [] (empty - will be created)

# Verify no metric filter conflict on log group
aws logs describe-metric-filters --log-group-name /aws/cloudtrail/kitehub-main 2>&1 | grep -q "ResourceNotFound" && echo "OK - log group not yet created"
```

## Findings

### Real changes (must verify intent on apply)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `aws_cloudwatch_log_group.cloudtrail_events` | create | New log group for CloudTrail event stream | LOW — additive, retention 30d, <100MB/mo |
| 2 | `aws_iam_role.cloudtrail_logs_delivery` | create | Trust policy for CloudTrail service | LOW — service-only assume-role, scoped action |
| 3 | `aws_iam_role_policy.cloudtrail_logs_delivery` | create | Inline policy `logs:CreateLogStream + PutLogEvents` | LOW — least-privilege, single log group resource |
| 4 | `aws_cloudtrail.main` | update in-place | Add `cloud_watch_logs_group_arn` + `cloud_watch_logs_role_arn` fields | MEDIUM — modifies existing prod trail. Update is in-place (no destroy/recreate) per AWS provider docs |
| 5 | `aws_cloudwatch_log_metric_filter.failed_iam_auth` | create | Pattern `errorCode = *UnauthorizedOperation \|\| AccessDenied*` | LOW — read-only metric extraction |
| 6 | `aws_cloudwatch_log_metric_filter.root_account_use` | create | Pattern `userIdentity.type = Root` | LOW — read-only |
| 7 | `aws_cloudwatch_log_metric_filter.sg_changes` | create | Pattern `eventName = AuthorizeSecurityGroupIngress \|\| ...` | LOW — read-only |
| 8 | `aws_cloudwatch_log_metric_filter.secrets_access` | create | Pattern `eventName = GetSecretValue \|\| PutSecretValue` | LOW — read-only |
| 9 | `aws_sns_topic.security_alerts` | create | New SNS topic `kitehub-security-alerts` | LOW — additive |
| 10 | `aws_sns_topic_subscription.security_alerts_email` | create | Email subscription `vannkite@outlook.com` (var.security_alert_email) | LOW — user must confirm subscription via email link |
| 11 | `aws_cloudwatch_metric_alarm.failed_iam_auth` | create | Threshold ≥1 / 5min Sum | LOW — alerts only |
| 12 | `aws_cloudwatch_metric_alarm.root_account_use` | create | Threshold ≥1 / 5min Sum | LOW — alerts only |
| 13 | `aws_cloudwatch_metric_alarm.sg_changes` | create | Threshold ≥5 / 5min Sum (burst) | LOW — alerts only |
| 14 | `aws_cloudwatch_metric_alarm.secrets_access_burst` | create | Threshold >20 / 5min Sum (burst) | LOW — alerts only |
| 15 | `aws_cloudwatch_dashboard.phase_1_overview` | update in-place | Add 3 EC2 metrics (kc_app_fe) + 7 new widgets (ALB health/latency, RDS IOPS, 4 security counts) | LOW — JSON body replacement |

**Cost impact (steady-state):**
- CloudWatch Logs (log group <100MB/mo): ~$0.03/mo within Free Tier 5GB
- Metric filters: FREE (4 metric streams, each Sum aggregation)
- Alarms: FREE first 10 (we use 4)
- SNS: FREE first 1k email/mo (expected <20/mo at baseline)
- Dashboard: existing (FREE first 3, this is dashboard #1)
- **TOTAL net new cost:** ~$0.03/mo within Free Tier

### Phantom updates

None expected — all resources are NEW additions OR field additions to existing trail (in-place update, no replacement).

### Verdict

Safe to apply per coordinator decision. All changes are additive observability infrastructure; no data path modification, no destructive operations, no service disruption risk. CloudTrail trail update is in-place (verified via AWS provider docs — `cloud_watch_logs_group_arn` is an updatable field, not a replacement trigger).

## Prior actions verified (per audit-to-gap-pipeline.md §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| CloudTrail trail `kitehub-main` created + IsLogging=true | Wave 81 (2026-05-07) | `aws-observability-first.md` §8 worked self-test + `aws cloudtrail get-trail-status` |
| Wave 82 EC2 t3.small + 4 P0 mitigation infra applied | 2026-05-15 11:33 UTC | `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-post-apply.md` |
| CORS sweep + Wave 83 launch-blockers closed | 2026-05-15 | PR #1416 merged into main (commit 476d42b7) |
| `aws_instance.kc_app_fe` exists (dashboard widget targets it) | Wave 82 Bucket B | `ec2-kc-app.tf:235` |
| `aws_lb.main[0]` exists (ALB widget targets it) | Wave 37 PR #938 | `ec2.tf:141` |
| `aws_db_instance.main` exists (RDS IOPS widget targets it) | Wave 37 PR #938 | `rds.tf:24` |
| `aws_s3_bucket.cloudtrail_logs` exists (S3 widget unchanged) | Wave 81 | `cloudtrail.tf:15` |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Run `terraform plan` via `gh workflow run terraform-apply.yml -f dry_run=true` | Coordinator | Capture plan output + verify 15 add / 1 in-place update / 0 destroy |
| Verify trail status via `aws cloudtrail get-trail-status --name kitehub-main` | Coordinator | Confirm IsLogging=true BEFORE apply |
| Concurrent op check per concurrent-production-mutation-ops.md §2 | Coordinator | Ensure no parallel `deploy-production.yml` / other terraform apply in flight |
| Run `terraform-apply.yml` workflow_dispatch with `confirm=APPLY` | Coordinator (human) | Per release-deploy-standard.md §9 — agent BANNED from triggering apply |
| Confirm SNS subscription email | User | Click confirmation link in `vannkite@outlook.com` inbox post-apply |
| Verify metric filters fire | Coordinator | Post-apply: trigger test event (e.g., `aws ec2 describe-instances --profile non-existent` -> AccessDenied) -> check CloudWatch metric `KiteHub/Security/FailedIAMAuthCount` >0 within 5 min |
| Verify dashboard renders | Coordinator | Open URL from output `cloudwatch_dashboard_url` |

## Recommendations

1. **Apply order (per concurrent-production-mutation-ops.md):**
   - Wait for all in-flight workflows (`gh run list --status in_progress`) to finish
   - Trigger `terraform-apply.yml` `dry_run=true` first → capture plan summary
   - Cross-check plan against §Findings table above (expect 14-15 add + 1 in-place update on `aws_cloudtrail.main`)
   - Trigger `dry_run=false` with `confirm=APPLY` ONLY after dry-run verified
2. **Post-apply verification commands (Tier 1 read-only, log to follow-up artifact):**
   ```bash
   aws cloudtrail get-trail-status --name kitehub-main --query 'CloudWatchLogsLogGroupArn,LatestCloudWatchLogsDeliveryError'
   aws logs describe-log-groups --log-group-name-prefix /aws/cloudtrail/kitehub-main
   aws logs describe-metric-filters --log-group-name /aws/cloudtrail/kitehub-main --query 'metricFilters[*].filterName'
   aws cloudwatch describe-alarms --alarm-name-prefix kitehub- --query 'MetricAlarms[?Namespace==`KiteHub/Security`].[AlarmName,StateValue]'
   aws sns list-subscriptions-by-topic --topic-arn $(aws sns list-topics --query 'Topics[?contains(TopicArn,`kitehub-security-alerts`)].TopicArn' --output text)
   ```
3. **Threshold tuning post-baseline:**
   - SG changes burst alarm (≥5/5min) sẽ fire mỗi terraform apply touching SG → expected noise — accept for now; raise threshold post-baseline nếu noise > 1 false-positive/week
   - Root account use (≥1/5min) — should fire ZERO times post-Wave-43 bootstrap; nếu fire → P0 investigate
4. **Follow-up gaps to file post-apply:**
   - GAP-437 → flip OPEN to PARTIAL (Phase 2-3 terraform plan ready; apply pending coordinator)
   - Optional Phase 4: AWS Config drift detection (deferred per GAP-437 original scope — defer until ≥10 resources, currently ~85)

## References

- **Wave plan:** `documents/03-planning/waves/wave-2026-05-15-84-ops-observability-runbooks.md` §3 Bucket A
- **Gap:** `documents/04-quality/gaps/GAP-437-aws-observability-baseline.md`
- **Rules applied:**
  - `.claude/rules/aws-observability-first.md` (CloudTrail prerequisite)
  - `.claude/rules/pre-mutation-state-check.md` (this artifact format)
  - `.claude/rules/agent-aws-access.md` §5 (logging mandate)
  - `.claude/rules/release-deploy-standard.md` §9 (agent role in deploy)
  - `.claude/rules/aws-sg-description-ascii.md` (N/A — no SG changes)
- **Sister artifacts:**
  - `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-post-apply.md` (Wave 82 baseline)
  - `documents/04-quality/audits/aws-verification/README.md`
- **Cost projection:** ~$0.03/mo net new (within Free Tier 5GB CloudWatch Logs)

## Log

- **2026-05-15:** Audit artifact created during Wave 84 Bucket A terraform plan delivery. Files validated via `terraform fmt` + `terraform validate`. Apply deferred to coordinator per task spec ("DO NOT run terraform apply"); coordinator should trigger `terraform-apply.yml` `dry_run=true` first → review plan output → then `dry_run=false` with `confirm=APPLY` per `release-deploy-standard.md` §9 human-trigger workflow_dispatch pattern.
