---
title: AWS Verification — Wave 86 Bucket H pre-apply state (AlertManager wiring + RDS storage alarm)
status: complete
created: 2026-05-16
phase: Wave 86 Bucket H
wave: 86
gaps: [GAP-144, GAP-583, GAP-591, GAP-592]
---

# AWS Verification Report — Wave 86 Bucket H pre-apply state

## Scope

Pre-apply state-check trước khi propose terraform changes cho:
- **H-AC4 (GAP-144 P0 BLOCKER)** — wire SNS topic + email subscription cho AlertManager production receivers + audit gap closure
- **H-AC2 (GAP-583 P1)** — CloudWatch alarm `RDSFreeStorageSpace < 5GB` → SNS topic
- 3 runbook docs (H-AC5/H-AC8/H-AC14) — không động AWS, doc-only

Per `pre-mutation-state-check.md` §3 — audit artifact MANDATORY trước khi đề xuất terraform diff.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Existing SNS topics
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws sns list-topics --query 'Topics[].TopicArn' --output text

# Existing CloudWatch alarms
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws cloudwatch describe-alarms --alarm-name-prefix kitehub \
    --query 'MetricAlarms[].AlarmName' --output text

# RDS state
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws rds describe-db-instances \
    --query 'DBInstances[].[DBInstanceIdentifier,AllocatedStorage,DBInstanceClass,StorageType]' \
    --output text

# Memory alerts subscription state
AWS_PROFILE=dev-admin AWS_DEFAULT_REGION=ap-southeast-1 \
  aws sns list-subscriptions-by-topic \
    --topic-arn arn:aws:sns:ap-southeast-1:906286017800:kitehub-memory-alerts
```

## Findings

### Existing SNS topics (3)

| Topic ARN | Purpose | Subscriptions |
|---|---|---|
| `arn:aws:sns:ap-southeast-1:906286017800:kitehub-cost-alerts` | EC2 cost monitoring (Wave 84) | vannkite@outlook.com |
| `arn:aws:sns:ap-southeast-1:906286017800:kitehub-memory-alerts` | EC2 OOM safety net (GAP-447) | vannkite@outlook.com |
| `arn:aws:sns:ap-southeast-1:906286017800:kitehub-security-alerts` | CloudTrail security alarms (GAP-437) | vannkite@outlook.com (assumed) |

**Phát hiện:** không có topic `kitehub-production-alerts` chung cho production receivers. Existing topics chuyên biệt theo domain.

### Existing CloudWatch alarms (11)

| Alarm name | Domain | SNS target |
|---|---|---|
| kitehub-kh-backend-memory-high | EC2 OOM | kitehub-memory-alerts |
| kitehub-kc-app-memory-high | EC2 OOM | kitehub-memory-alerts |
| kitehub-kc-app-fe-memory-high | EC2 OOM | kitehub-memory-alerts |
| kitehub-{kh-backend,kc-app,kc-app-fe}-low-cpu-7d | Cost | kitehub-cost-alerts |
| kitehub-kc-app-fe-cert-expiry | TLS | (verify) |
| kitehub-root-account-use | Security | kitehub-security-alerts |
| kitehub-failed-iam-auth | Security | kitehub-security-alerts |
| kitehub-secrets-access-burst | Security | kitehub-security-alerts |
| kitehub-sg-changes-burst | Security | kitehub-security-alerts |

**Phát hiện:** chưa có alarm nào cho RDS storage. H-AC2 cần thêm `kitehub-rds-storage-low`.

### RDS state

| Identifier | Storage (GB) | Class | Storage type |
|---|---|---|---|
| kitehub-postgres | 20 | db.t3.micro | gp3 |

**Phát hiện:** allocated 20GB; threshold 5GB free = alarm fires khi used > 15GB (75%). Auto-scaling chưa enabled (per GAP-583 notes — t3.micro 20GB autoscale disabled cost saving).

### GAP-144 current state

Per gap file đọc trực tiếp:
- Chart-level wiring DONE 2026-04-28 (ADR-022 + ExternalSecret + values.yaml gated)
- Mock-fire runbook DONE 2026-05-11 (Wave 55 Bucket C)
- Live-cluster delivery test ⚠️ DEFERRED — yêu cầu Helm platform deploy (EKS + ESO + provisioned secrets)
- **Block point:** Phase 1 BETA dùng EC2 docker-compose, KHÔNG có EKS — AlertManager Helm chart chưa được deploy live

**Implication cho H-AC4:** GAP-144 scope (Helm AlertManager) không match Phase 1 BETA infra reality (EC2 docker-compose Prometheus stack). Cần adapt: dùng SNS-direct path từ CloudWatch alarms thay cho AlertManager.

## Verdict

### Real changes (terraform-aws/)

| # | Resource | Action | Root cause | Risk |
|---|---|---|---|---|
| 1 | `aws_sns_topic.production_alerts` | CREATE | Dedicated topic cho production-grade alerts (RDS storage + future prod alarms) | LOW — free tier SNS |
| 2 | `aws_sns_topic_subscription.production_alerts_email` | CREATE | Subscribe support@kitehub.me cho operational visibility | LOW — email subscription pending confirmation by recipient |
| 3 | `aws_sns_topic_subscription.production_alerts_personal` | CREATE | Subscribe vannkite@outlook.com (backup) | LOW |
| 4 | `aws_cloudwatch_metric_alarm.rds_storage_low` | CREATE | RDS free storage < 5GB threshold | LOW — alarm trigger doesn't mutate RDS |

### Phantom updates

Không phantom. Diff additive only.

### Verdict

**SAFE để apply** (when human-triggered via `terraform-apply.yml`). All changes:
- Additive (CREATE only, 0 destroy, 0 replace)
- Cost-neutral (SNS free tier 1M publishes/month; CloudWatch alarm $0.10/month per alarm)
- Reversible (terraform destroy safe nếu rollback cần)
- Aligned với existing pattern (`cloudwatch.tf` already has `aws_sns_topic.memory_alerts` + alarms cùng module)

## Prior actions verified

| Action | When | Where verified |
|--------|------|----------------|
| Memory alerts SNS topic + email sub | Wave ~30 (GAP-447) | `cloudwatch.tf` lines 21-34 |
| Cost alerts SNS + EC2 low-CPU alarms | Wave 84 (GAP-414) | `cost-monitoring.tf` |
| Security alerts SNS + CloudTrail alarms | Wave 84 (GAP-437) | `cloudwatch-security-alarms.tf` |
| GAP-144 Helm chart-level wiring | 2026-04-28 | `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` |
| GAP-144 mock-fire runbook | 2026-05-11 (Wave 55 Bucket C) | `documents/05-guides/operations/runbooks/alertmanager-mock-fire-runbook.md` |

## Pending (this PR)

| Action | Owner | Notes |
|--------|-------|-------|
| Add `aws_sns_topic.production_alerts` + 2 subscriptions | Human-triggered terraform-apply.yml | Per `release-deploy-standard.md` §9 |
| Add `aws_cloudwatch_metric_alarm.rds_storage_low` | Same | Targets `kitehub-postgres` instance |
| 4 runbook docs (H-AC2/AC5/AC8/AC14) | Agent (doc-only, no apply) | Auto-merge eligible per `docs-only-pr-auto-merge.md` after CI green |
| GAP-144 status flip → 🟢 DONE | Same PR | Self-test: SNS test publish + email receipt (user verify) |
| **Concurrent op check** | Agent verification | `gh run list --status in_progress` confirmed empty pre-trigger time |

## Recommendations

1. **Apply path:** human-triggered `gh workflow run terraform-apply.yml --ref <branch> -f confirm=APPLY -f dry_run=true` first; review plan summary matching §Verdict above; then `-f dry_run=false`.
2. **Email confirmation:** AWS SNS email subscriptions yêu cầu recipient click confirm link 1 lần. After apply, check support@kitehub.me + vannkite@outlook.com inbox cho "AWS Notification - Subscription Confirmation" email — click "Confirm subscription" link.
3. **Post-apply verify:** run `aws sns publish --topic-arn <arn> --message "Wave 86 H-AC4 self-test"` → verify email received in both inboxes within 1-2 min.
4. **GAP-144 closure:** flip Status → 🟢 DONE chỉ sau khi self-test email PASS empirically. Otherwise remain 🟡 PARTIAL per `gap-done-discipline.md` §3.
5. **Scope adaptation note:** GAP-144 original scope = Helm AlertManager. Wave 86 H-AC4 closes the Phase 1 BETA "alerts go somewhere" outcome via SNS-direct path (CloudWatch → SNS → email). Helm AlertManager live-cluster delivery test remains deferred until EKS platform deploy (Phase 1.5+). This adaptation documented in GAP-144 Log entry below.

## References

- Wave 86 plan: `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md` §3 Bucket H
- Gap files: `GAP-144-alertmanager-production-receivers.md`, `GAP-583-rds-storage-alarm-wiring-resize-runbook.md`, `GAP-591-cohort-retention-d7-d14-d30-framework.md`, `GAP-592-first-response-sla-published-phase-1-beta.md`
- Rules applied: `pre-mutation-state-check.md`, `release-deploy-standard.md` §9, `agent-aws-access.md` §2.1, `gap-done-discipline.md` §3
- Sister audit artifacts: `2026-05-15-wave-84-bucket-a-cloudtrail-metric-filters-plan.md`, `2026-05-15-wave-84-bucket-b-startup-probe-plan.md`
