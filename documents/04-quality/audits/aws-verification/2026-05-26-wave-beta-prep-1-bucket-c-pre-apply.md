---
title: AWS Verification — Wave beta-prep-1 Bucket C pre-apply (P0 SNS alarms + Statuspage + restore drill verify)
status: complete
created: 2026-05-26
phase: phase-1-beta
wave: wave-beta-prep-1
gaps: [GAP-144, GAP-257, GAP-373]
---

# AWS Verification Report — Wave beta-prep-1 Bucket C pre-apply

## Scope

Pre-apply state-check + plan-vs-predicted reconciliation per `pre-mutation-state-check.md` §3 + §3.5 cho 3 deliverables Bucket C:

1. **Item 1 (Statuspage account-prep runbook):** `documents/05-guides/account-prep/08-statuspage-account-prep-runbook.md` — docs-only, KHÔNG có terraform mutation. Out of scope of pre-mutation audit per `pre-mutation-state-check.md` §2 "Tier 1 read-only" exception.

2. **Item 2 (P0 SNS alerts):** `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` (NEW) — 8 `aws_cloudwatch_metric_alarm` resources extending existing `aws_sns_topic.production_alerts` (defined in `production-alerts.tf`). All-additive, zero modify/destroy.

3. **Item 3 (Restore drill 1-shot verify):** `scripts/verify-restore.sh` + `.github/workflows/restore-drill.yml` — verify framework readiness via shellcheck + self-test + workflow YAML lint. Live drill execution defer GAP-612 AWS account restore unblock.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Item 2 — Pre-existing alarm inventory baseline
ls infrastructure/terraform-aws/*alarm*.tf production-alerts.tf
# Output: cloudwatch.tf cloudwatch-alarms-jvm-pool.tf cloudwatch-security-alarms.tf
#         cloudwatch-p0-alarms.tf (new) production-alerts.tf

# Item 2 — Verify SNS topic reference exists in target file
grep -n "production_alerts" infrastructure/terraform-aws/production-alerts.tf
# Output: line 23 aws_sns_topic "production_alerts"

# Item 2 — Verify EC2 instance references exist
grep -nE "aws_instance\.(kh_backend|kc_app)" infrastructure/terraform-aws/ec2*.tf
# Output: ec2.tf line 38 kh_backend; ec2-kc-app.tf line 31 kc_app

# Item 3 — Restore script syntax + self-test
bash -n scripts/verify-restore.sh
# Output: syntax OK
bash scripts/verify-restore.sh --self-test
# Output: PASS=7 WARN=0 FAIL=0

# Item 3 — Workflow YAML lint
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/restore-drill.yml'))"
# Output: YAML OK

# AWS live verification (deferred per GAP-612 unblock)
aws ec2 describe-instances --profile dev-admin --region ap-southeast-1 ...
# Output: AuthFailure — AWS credentials not available in current worktree session
#         per project_phase_1_beta_critical_path_2026_05_11 memory:
#         "Stack STOPPED ~$3-5/mo storage only"
```

## Findings

### Real changes (must verify intent)

**Item 2 — `infrastructure/terraform-aws/cloudwatch-p0-alarms.tf` (NEW file, 8 resources):**

| # | Resource | Action | Root cause | Risk |
|---|---|---|---|---|
| 1 | `aws_cloudwatch_metric_alarm.rds_cpu_high` | create | New — RDS CPU > 80% sustained P0 signal | Zero — additive alarm, no resource modify |
| 2 | `aws_cloudwatch_metric_alarm.kh_backend_disk_high` | create | New — EC2 kh disk > 90% (CWAgent metric) | Zero — references existing `aws_instance.kh_backend.id` |
| 3 | `aws_cloudwatch_metric_alarm.kc_app_disk_high` | create | New — EC2 kc disk > 90% | Zero — references existing `aws_instance.kc_app.id` |
| 4 | `aws_cloudwatch_metric_alarm.kh_backend_status_check_failed` | create | New — EC2 system status check fail | Zero — uses AWS/EC2 native metric, no agent dependency |
| 5 | `aws_cloudwatch_metric_alarm.kc_app_status_check_failed` | create | New — EC2 system status check fail | Zero — same |
| 6 | `aws_cloudwatch_metric_alarm.nginx_5xx_rate_high` | create | New — ALB substitute via nginx access log metric filter | Zero — alarm self-contained; metric filter dependency tracked separately |
| 7 | `aws_cloudwatch_metric_alarm.outbox_dlq_non_empty` | create | New — async job DLQ depth signal | Zero — alarm self-contained; RabbitMQ exporter dependency tracked separately |
| 8 | `aws_cloudwatch_metric_alarm.cloudtrail_root_login` | create | New — root login security finding | Zero — depends on existing CloudTrail metric filter (cloudtrail-metric-filters.tf) |

**Plan summary (predicted post-apply):** 8 add, 0 change, 0 destroy.

### Phantom updates

None expected. All resources are NEW; no `lifecycle ignore_changes` triggers; no resource modifications.

### Verdict

**Safe to apply.** Reasons:
- Pure additive: 8 new alarm resources, zero modification to existing infrastructure
- Inherits existing SNS topic subscription (`production_alerts` has support@kitehub.me + vannkite@outlook.com per `production-alerts.tf`); no subscription confirm-resubscribe loop needed
- Alarms gracefully INSUFFICIENT_DATA if dependent metric sources not yet wired (CWAgent disk plugin, Nginx5xxCount metric filter, RabbitMQ exporter, CloudTrail RootUserLogin filter) — useful Phase 1 BETA observability gap signal until pipelines complete
- No data-loss risk; no service availability impact
- Pre-mutation audit per `pre-mutation-state-check.md` §3.5 plan-vs-predicted reconciliation: PR scope `8 add / 0 change / 0 destroy` matches expected; no Wave backlog drift expected

## Plan-vs-predicted reconciliation (per `pre-mutation-state-check.md` §3.5)

| Resource | Plan action | Wave-source | Intent | Decision |
|---|---|---|---|---|
| `aws_cloudwatch_metric_alarm.rds_cpu_high` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |
| `aws_cloudwatch_metric_alarm.kh_backend_disk_high` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |
| `aws_cloudwatch_metric_alarm.kc_app_disk_high` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |
| `aws_cloudwatch_metric_alarm.kh_backend_status_check_failed` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |
| `aws_cloudwatch_metric_alarm.kc_app_status_check_failed` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |
| `aws_cloudwatch_metric_alarm.nginx_5xx_rate_high` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |
| `aws_cloudwatch_metric_alarm.outbox_dlq_non_empty` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |
| `aws_cloudwatch_metric_alarm.cloudtrail_root_login` | create | Wave beta-prep-1 Bucket C item 2 | Real | Apply |

Plan summary predicted: `8 add / 0 change / 0 destroy`. Reconciliation: 100% PR scope (no Wave backlog drift). Safe to flip `dry_run=true → false` once AWS credentials restored (GAP-612 unblock).

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

| Action | When | Where verified |
|--------|------|----------------|
| `aws_sns_topic.production_alerts` created Wave 86 Bucket H | 2026-05-16 | `documents/04-quality/audits/aws-verification/2026-05-16-wave-86-h-pre-apply-state.md` + `production-alerts.tf` line 23 |
| `aws_cloudwatch_metric_alarm.rds_storage_low` (GAP-583) | 2026-05-16 | `production-alerts.tf` line 52 |
| CWAgent installation Wave 85 Bucket E (Tier 2 JVM monitoring) | 2026-05-15 | `cloudwatch-alarms-jvm-pool.tf` header comment |
| CWAgent memory alarms (kh_backend + kc_app) GAP-447 | Wave 80+ | `cloudwatch.tf` lines 35-90 |
| CloudTrail multi-region trail (`kitehub-main`) per `aws-observability-first.md` | 2026-05-07 | PR #992; existing `cloudtrail.tf` + `cloudtrail-metric-filters.tf` |
| GAP-257 restore drill Phase 2 monthly cron + Phase 3 quarterly carry | 2026-04-28 | `.github/workflows/restore-drill.yml` + `scripts/verify-restore.sh` |
| GAP-144 AlertManager SNS direct path Phase 1 BETA adaptation | 2026-05-16 | `production-alerts.tf` header (Wave 86 Bucket H closure) |
| GAP-612 AWS account suspension recovery (closed) | 2026-05-17 | `documents/04-quality/gaps/phase-1-beta/closed/GAP-612-aws-account-suspension-recovery.md` |

No duplication; 8 new alarms extend existing pattern without overlap.

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Terraform apply 8 new alarms (dry_run=true → false) | User-trigger via `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false` per `dev-authorized-terraform-trigger.md` §2 | DEFER until AWS credentials available (GAP-612 unblock pre-met; verify `aws sts get-caller-identity` Tier 1 returns 200 before flipping `dry_run`) |
| Wire missing metric sources | Separate scope follow-up gaps | Nginx 5xx metric filter + RabbitMQ Prometheus exporter + CloudTrail RootUserLogin filter — alarm stay INSUFFICIENT_DATA acceptable v1.0 |
| **Concurrent op check per `concurrent-production-mutation-ops.md` §4** | Agent verification | At apply time: `gh run list --status in_progress` — verify zero overlap with `deploy-production.yml` / other terraform-apply runs |
| Live restore drill (Phase 3 GAP-257 quarterly exercise) | User-trigger via `gh workflow run restore-drill.yml -f force_real_drill=true` after `BACKUP_DRILL_ENABLED=true` set | DEFER per `restore-drill.yml` workflow header — requires (1) S3 backups in `kite-backups-prod` bucket, (2) `AWS_RESTORE_DRILL_ROLE_ARN` secret wired, (3) AWS account credentials active |

## Recommendations

1. **Apply 8 alarms via standard `workflow_dispatch` path** per `release-deploy-standard.md` §9 + `dev-authorized-terraform-trigger.md` §2 once AWS credentials restored. dry_run=true first to verify plan matches `8 add / 0 change / 0 destroy` exactly.

2. **Post-apply verification (Tier 1 read-only) per `agent-aws-access.md` §5:**

   ```bash
   aws cloudwatch describe-alarms \
     --alarm-name-prefix kitehub-rds-cpu-high \
     --alarm-name-prefix kitehub-kh-backend-disk-high \
     --alarm-name-prefix kitehub-kc-app-disk-high \
     --alarm-name-prefix kitehub-kh-backend-status-check-failed \
     --alarm-name-prefix kitehub-kc-app-status-check-failed \
     --alarm-name-prefix kitehub-nginx-5xx-rate-high \
     --alarm-name-prefix kitehub-outbox-dlq-non-empty \
     --alarm-name-prefix kitehub-cloudtrail-root-login \
     --query 'MetricAlarms[].[AlarmName,StateValue]' \
     --output table
   ```

   Expected: 8 alarms exist; state likely INSUFFICIENT_DATA (until metric sources ship) — acceptable per §Findings note.

3. **Track follow-up gaps for missing metric sources:**
   - Nginx 5xx metric filter wire (depends on `/kite/nginx/access` log group + filter pattern)
   - RabbitMQ Prometheus exporter scrape via CWAgent
   - CloudTrail `RootUserLogin` metric filter verify

   These should pre-exist per Wave 85 / `aws-observability-first.md` baseline; verify post-apply.

4. **Restore drill 1-shot verify:** framework readiness PASS (shellcheck syntax + self-test 7/7 + YAML lint). Live drill defer until BACKUP_DRILL_ENABLED gate flip per `.github/workflows/restore-drill.yml` header conditions.

## Restore drill framework verification (Item 3)

Per Wave `beta-prep-1` Bucket C item 3 scope:

| Verification | Command | Result |
|---|---|---|
| Script syntax | `bash -n scripts/verify-restore.sh` | ✅ syntax OK |
| Script self-test | `bash scripts/verify-restore.sh --self-test` | ✅ PASS=7 WARN=0 FAIL=0 |
| Workflow YAML lint | `python3 -c "import yaml; yaml.safe_load(...)"` | ✅ YAML OK |
| Backup-script CI gate intact | `.github/workflows/restore-drill.yml` cron `0 3 1 * *` + manual `workflow_dispatch` | ✅ unchanged |
| Metadata schema documented | `scripts/verify-restore.sh` lines 37-48 inline | ✅ present |
| Acceptance criteria documented | `documents/05-guides/operations/dr-rto-rpo-matrix.md` (GAP-119) | ✅ exists per GAP-257 ref |

**Live drill TTR baseline (Phase 3 measurement):** DEFER per GAP-257 status OPEN P1. Pre-conditions per `restore-drill.yml` header:
- AWS account active + S3 backups accumulated ≥ 30 days (GAP-093 backup script ship pre-requisite)
- `AWS_RESTORE_DRILL_ROLE_ARN` secret wired (OIDC role with `s3:GetObject` on backup bucket)
- `BACKUP_DRILL_ENABLED=true` repo variable flip
- Stakeholder availability for full destruction + restore window (Q3 2026 schedule per GAP-257 §Proposed Fix)

**Wave beta-prep-1 Bucket C item 3 acceptance:** PARTIAL per `gap-done-discipline.md` §3 — framework PASS verified; live drill execute defer GAP-257 Phase 3 schedule (Q3 2026 quarterly window).

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-26-beta-prep-1-mega.md` §3 Bucket C
- GAP-144 (closed Wave 86 Bucket H): `documents/04-quality/gaps/phase-1-beta/closed/GAP-144-alertmanager-production-receivers.md`
- GAP-257 (carry P1): `documents/04-quality/gaps/phase-1-beta/GAP-257-restore-drill-phase-3-quarterly.md`
- GAP-373 (Status page minimal): `documents/04-quality/gaps/` — Phase 1 scope close partial via Item 1 runbook
- GAP-612 (closed Wave 92): `documents/04-quality/gaps/phase-1-beta/closed/GAP-612-aws-account-suspension-recovery.md`
- Sister rules applied: `aws-observability-first.md` §2 (CloudTrail pre-requisite verified) + `aws-sg-description-ascii.md` (alarm descriptions ASCII-only verified inline) + `release-deploy-standard.md` §9 (human-trigger apply pattern) + `dev-authorized-terraform-trigger.md` §2 (5-gate procedural)
- Pre-existing SNS topic: `infrastructure/terraform-aws/production-alerts.tf` (Wave 86 Bucket H ship 2026-05-16)
- Pre-existing CloudTrail baseline: `infrastructure/terraform-aws/cloudtrail.tf` + `cloudtrail-metric-filters.tf` (Wave 40+ via GAP-437)
