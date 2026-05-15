---
title: AWS Verification - Wave 85 Bucket E Tier 2 Config Pre-Apply
status: complete
created: 2026-05-15
phase: wave-85-bucket-e
wave: 85
gaps: [GAP-503, GAP-502]
---

# AWS Verification Report - Wave 85 Bucket E Tier 2 Config Pre-Apply

## Scope

Wave 85 Bucket E ships Tier 2 JVM/Tomcat/HikariCP right-sizing plus 3 new CloudWatch alarms guarding the new thresholds. Production mutation = 3 new `aws_cloudwatch_metric_alarm` resources in `infrastructure/terraform-aws/cloudwatch-alarms-jvm-pool.tf`. Pre-mutation state-check artifact per `pre-mutation-state-check.md` §3.

Rules applied:
- `pre-mutation-state-check.md` v1.1.1 (audit artifact mandate)
- `concurrent-production-mutation-ops.md` v1.0.0 §4 (pre-flight check)
- `aws-observability-first.md` v1.0.0 (CloudTrail baseline — verified PASS post-Wave-84-Bucket-A)
- `pre-launch-infra-hardening-checklist.md` v1.0.1 (sister rule, Cat 5 §2.8 CloudTrail re-confirm)
- `aws-sg-description-ascii.md` v1.0.1 (alarm_description ASCII-only — verified)

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Pre-flight concurrent op check (per concurrent-production-mutation-ops.md §4)
gh run list --status in_progress --json workflowName --jq '.[].workflowName'
# Expected: empty before trigger

# Verify CloudTrail baseline still IsLogging (per aws-observability-first.md §6)
aws cloudtrail get-trail-status --name kitehub-main --query 'IsLogging'
# Expected: true (already verified Wave 84 Bucket A apply)

# Inspect current alarm landscape to confirm no name collision
aws cloudwatch describe-alarms \
  --query 'MetricAlarms[?contains(AlarmName, `kitehub-jvm`) || contains(AlarmName, `kitehub-tomcat`) || contains(AlarmName, `kitehub-hikari`)].AlarmName'
# Expected: empty list (new alarms, no collision)

# Confirm SNS topic memory_alerts exists (referenced by new alarms)
aws sns list-topics --query 'Topics[?contains(TopicArn, `kitehub-memory-alerts`)].TopicArn'
# Expected: 1 entry from cloudwatch.tf GAP-447 (Wave Y)

# Plan output verification (post-terraform-plan)
gh run download <plan-run-id> -p plan-output -D /tmp/plan-wave-85-e
grep -E "will be created|will be destroyed|will be updated|must be replaced" /tmp/plan-wave-85-e/plan-output/*.txt
# Expected: only 3 new aws_cloudwatch_metric_alarm resources + helm/application.yml drift = 0 (chart unchanged)
```

## Findings

### Real changes (must verify intent)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | aws_cloudwatch_metric_alarm.jvm_heap_usage_high | create | E-AC1 JVM heap >90% alarm (GAP-503 Tier 2 monitoring) | LOW - read-only metric watch; no compute impact; alarm action = SNS publish to existing topic |
| 2 | aws_cloudwatch_metric_alarm.tomcat_threads_busy_high | create | E-AC2 Tomcat thread pool busy >80% alarm | LOW - same shape as #1 |
| 3 | aws_cloudwatch_metric_alarm.hikari_connection_wait_high | create | E-AC3 HikariCP pending connection > 5 alarm | LOW - same shape as #1 |

### Phantom updates (no real change)

None expected. Plan-only PR; user triggers terraform-apply.yml in follow-up.

### Verdict

3 new alarm resources, additive only. Zero destroy/replace/modify. Blast radius = SNS topic publish path (already validated by GAP-447 memory alarms using same topic). Cost: CloudWatch alarms are billed at $0.10/alarm/month standard resolution -> 3 alarms = $0.30/mo additional, well within Phase 1 BETA Free Tier reserve.

INSUFFICIENT_DATA state expected until CWAgent prometheus exporter wired to scrape `/actuator/prometheus` and forward to `KiteHub/JVM` namespace. This is acceptable Phase 1 BETA observability signal (per cloudwatch-alarms-jvm-pool.tf §"Follow-up notes" comment).

Application config changes (helm values.yaml + 7 application-production.yml + helm deployment.yaml startupProbe initialDelay 30s -> 120s) are NOT terraform-managed; they ship via docker-compose deploy (separate workflow). No terraform drift expected from these edits.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| GAP-447 memory_alerts SNS topic + 2 alarms create | Earlier Wave | cloudwatch.tf lines 21-49 (aws_sns_topic.memory_alerts + aws_cloudwatch_metric_alarm.kh_backend_memory_high) |
| GAP-431 startupProbe wired Helm | Wave 84 Bucket H closure | infrastructure/helm/kitehub/templates/deployment.yaml lines 135-142 (this PR bumps initialDelaySeconds 30 -> 120 per Wave 81 Bucket F finding) |
| GAP-437 CloudTrail multi-region IsLogging=true | Wave 84 Bucket A apply | aws-verification audit 2026-05-15-wave-84-bucket-a-cloudtrail-apply.md |
| GAP-414 EC2 cost monitoring 3 low-CPU alarms | Earlier Wave | cost-monitoring.tf (verified present pre-spawn) |
| Existing kitehub/kiteclass production profile files | Wave 82 Bucket F6 | 5 of 7 services already have application-production.yml; this PR extends 5 + creates 2 missing (kiteclass-core, kiteclass-gateway) |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| terraform-plan.yml dry_run=true workflow | User trigger | Plan-only verification; no apply |
| terraform-apply.yml workflow_dispatch | User trigger (per `release-deploy-standard.md` §9 + `dev-authorized-terraform-trigger.md` if explicit) | NOT scope of this PR per task spec "KHONG terraform apply" |
| CloudWatch agent prometheus exporter config | Follow-up gap | Until wired, 3 new alarms = INSUFFICIENT_DATA |
| **Concurrent op check** | Agent verification | Confirmed zero active workflows touching infrastructure/terraform-aws/** at branch creation 2026-05-15 |

## Recommendations

1. **Plan only** for this PR per task spec — do NOT trigger terraform-apply.yml. Plan output will be attached to PR description.
2. **Post-merge cadence:** User triggers `terraform-apply.yml -f confirm=APPLY -f dry_run=true` first to re-verify plan summary stable, then `-f dry_run=false` for actual apply.
3. **Post-apply verification (next session):** `aws cloudwatch describe-alarms --query 'MetricAlarms[?Tags[?Key==\`Gap\` && Value==\`GAP-503\`]].AlarmName'` should return 3 entries.
4. **Watch-for items:** if alarm stays INSUFFICIENT_DATA after 24h, file follow-up gap to wire CWAgent prometheus collector (depends on /actuator/prometheus reachable from agent + JMX/JVM metrics exporter config).

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md` §3 Bucket E
- Parent gap: GAP-503 (jvm-tomcat-hikari-tier-2-optimization)
- Sister gap: GAP-502 (rabbitmq-auth-fail-plus-oom-thrash-kh-backend) - E-AC1 60% override prevents recurrence
- Rules: pre-mutation-state-check.md, concurrent-production-mutation-ops.md, aws-observability-first.md, aws-sg-description-ascii.md
- Sister-file: infrastructure/terraform-aws/cloudwatch.tf (GAP-447 sister memory alarms - same SNS topic reused)
