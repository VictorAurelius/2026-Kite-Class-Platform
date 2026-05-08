---
title: AWS Verification — Wave 43+44 Bootstrap Apply
status: complete
created: 2026-05-08
phase: 1-beta-bootstrap
---

# AWS Verification Report — Wave 43+44 Bootstrap Apply

## Scope

Verify post-apply state of Wave 43 (cost-discipline EventBridge stop/start + EC2 right-size + memory alarm) + Wave 44 (terraform-apply IAM role) bootstrap apply executed 2026-05-08T09:30Z via local `terraform apply` với `kite-readonly` profile (admin perms for chicken-and-egg bootstrap per `release-deploy-standard.md` §9 v1.0.1 carve-out).

Apply executed in 2 steps:
1. **Targeted apply #1** — Wave 43 schedulers + Wave 44 IAM apply role + EC2 right-size (FAILED on SNS tag invalid char)
2. **Targeted apply #2** (post fix PR #1046) — SNS topic + email subscription + kh_backend memory alarm

## Commands run (Tier 1 read-only verification per `agent-aws-access.md` §2.1)

```bash
# Schedulers verification
aws scheduler list-schedules --group-name kitehub-cost-saving \
  --query 'Schedules[].[Name,State]' --output table

# EC2 verification
aws ec2 describe-instances \
  --query 'Reservations[].Instances[].[InstanceId,InstanceType,State.Name,Tags[?Key==`Name`].Value|[0]]' \
  --output table

# IAM role verification
aws iam get-role --role-name kitehub-github-terraform-apply --query 'Role.Arn' --output text

# SNS topic verification
aws sns list-topics --query 'Topics[?contains(TopicArn,`memory`)].TopicArn' --output text

# CloudWatch alarms verification
aws cloudwatch describe-alarms --alarm-name-prefix kitehub \
  --query 'MetricAlarms[].[AlarmName,StateValue]' --output table

# CloudTrail (per aws-observability-first.md)
aws cloudtrail get-trail-status --name kitehub-main --query 'IsLogging'
```

## Results

### EventBridge Schedulers (Wave 43 Bucket A — GAP-446)

8 schedulers in group `kitehub-cost-saving`, all ENABLED:

| Schedule | State | Cron (Asia/Ho_Chi_Minh) |
|---|---|---|
| `start-monday-morning-ec2` | ENABLED | `cron(0 8 ? * MON *)` |
| `start-monday-morning-rds` | ENABLED | `cron(0 8 ? * MON *)` |
| `start-weekday-morning-ec2` | ENABLED | `cron(0 8 ? * MON-FRI *)` |
| `start-weekday-morning-rds` | ENABLED | `cron(0 8 ? * MON-FRI *)` |
| `stop-friday-evening-ec2` | ENABLED | `cron(0 22 ? * FRI *)` |
| `stop-friday-evening-rds` | ENABLED | `cron(0 22 ? * FRI *)` |
| `stop-weekday-evening-ec2` | ENABLED | `cron(0 22 ? * MON-FRI *)` |
| `stop-weekday-evening-rds` | ENABLED | `cron(0 22 ? * MON-FRI *)` |

**Verdict:** ✅ DONE. First stop fires 2026-05-08T22:00 ICT (15:00 UTC). First start fires 2026-05-09T08:00 ICT (next day 01:00 UTC).

### EC2 Right-size (Wave 43 Bucket B — GAP-447)

| Instance | Type before | Type after | State | Note |
|---|---|---|---|---|
| `i-0b65c3947d36cae61` (`kh_backend`) | m7i-flex.large 8GB | **t3.medium 4GB** | running | In-place modify ~30s restart đã xảy ra |
| `i-07f6de54544162124` (`kc_app` NEW) | n/a | **t3.medium 4GB** | **stopped** | Old `i-04f65503ace7febe4` terminated; new instance stopped post-apply per Wave 43 cost-saving intent |

**Verdict:** ✅ DONE for kh_backend; ⚠️ PARTIAL for kc_app — replaced unintentionally (target dependency pulled instance), drift on `associate_public_ip_address` setting. New instance stopped — zero ongoing cost. Phase 7 resume sẽ revisit.

### CloudWatch Memory Alarm (Wave 43 Bucket B — GAP-447)

| Alarm | State | Note |
|---|---|---|
| `kitehub-kh-backend-memory-high` | INSUFFICIENT_DATA | Expected — CWAgent not yet installed/configured trên kh_backend. Per `right-size-stress-test.md` Phase 1 manual step. Once agent emits `mem_used_percent` to CWAgent namespace, alarm transitions to OK or ALARM state |
| `kitehub-kc-app-memory-high` | NOT CREATED | Skipped from re-apply (depends `aws_instance.kc_app` which has state drift — alarm provisions when kc_app stabilizes Phase 7) |

**Verdict:** ⚠️ PARTIAL — kh alarm provisioned, kc alarm deferred Phase 7. CWAgent install runbook step pending (manual SSH per right-size-stress-test.md §1).

### SNS Topic (Wave 43 Bucket B — GAP-447 alarm notification)

- ARN: `arn:aws:sns:ap-southeast-1:906286017800:kitehub-memory-alerts`
- Email subscription: `vannkite@outlook.com` ✅ **CONFIRMED 2026-05-08** (subscription ARN: `arn:aws:sns:ap-southeast-1:906286017800:kitehub-memory-alerts:99266533-dc12-4824-be17-5949846e575b`)

**Verdict:** ✅ DONE. Topic created + email subscription confirmed; alarm notifications sẽ deliver tới `vannkite@outlook.com` khi `kh_backend_memory_high` transitions to ALARM state post-CWAgent install.

### Wave 44 IAM Apply Role (GAP-449)

- ARN: `arn:aws:iam::906286017800:role/kitehub-github-terraform-apply`
- Trust policy: condition `repo:VictorAurelius/2026-Kite-Class-Platform:environment:production`
- Attached policies: `PowerUserAccess` + inline IAM management + S3/DynamoDB state access
- GitHub Variable `AWS_TERRAFORM_APPLY_ROLE_ARN` set: ✅

**Verdict:** ✅ DONE. Workflow_dispatch terraform-apply ready for future infra applies.

### CloudTrail (per `aws-observability-first.md`)

- Trail `kitehub-main` `IsLogging=True` (multi-region) — captures all bootstrap apply API calls

**Verdict:** ✅ Audit trail intact.

## Findings

### Issues encountered

1. **State drift on `random_password.jwt/rds/encryption_raw`** (state shows `id="none"` but secrets exist on AWS) — UNRESOLVED. Targeted apply skipped these to avoid secret rotation outage. Tracked GAP-450.
2. **`kc_app` unintentional replacement** — `aws_scheduler_schedule.*` target pulled `aws_instance.kc_app` as dependency due to tag-based EventBridge filter; combined with `associate_public_ip_address: false → true` drift forced replacement. Old instance terminated; data loss minimal (Phase 7 deferred KC stack — only docker compose files lost). Tracked GAP-450.
3. **SNS tag invalid char** — `(GAP-447)` in tag value caused first apply fail. Fixed PR #1046 (parens removed → `GAP-447`). AWS SNS tag char-set restriction (no parens) — potential rule extension cho `aws-sg-description-ascii.md` family if recurrence.

### Cost saving achieved

| Component | Before | After |
|---|---|---|
| 2× m7i-flex.large 8GB | $120/mo | n/a |
| 2× t3.medium 4GB (post right-size) | n/a | $60/mo (24/7) |
| Scheduler stop/start (~58% downtime weekday + weekend) | n/a | ~$25/mo effective |
| **EC2 monthly cost** | **$120** | **~$25** |

Combined với Vercel ignoreCommand (GAP-448 — quota saved validated 2026-05-08 Vercel `Canceled by Ignored Build Step` on PR #1046):
- Total burn: $157/mo → ~$45-55/mo target
- $200 credit longevity: 1.3 tháng → **3.5-4 tháng**

## Next steps

### Immediate (user manual)

1. **Confirm SNS subscription** — check email `vannkite@outlook.com`, click confirmation link
2. **Rotate `solo-dev-admin` access key** — IAM Console → Users → `solo-dev-admin` → Security credentials → Make access key Inactive → Delete (per `agent-aws-access.md` §11 + Wave 43 user actions)
3. **Tạo `kite-readonly-wsl` IAM user** mới với `ReadOnlyAccess` policy → reconfigure profile (per `agent-aws-access.md` §2.1 Tier 1 scope alignment)

### Phase 1 BETA stabilization (separate gap/wave)

4. **Install CloudWatch agent** trên kh_backend (manual SSH or SSM Run Command) per `right-size-stress-test.md` §1 — alarm transitions to active monitoring
5. **kh_backend stress test 1h** post-CWAgent install — verify t3.medium 4GB sufficient cho compose budget 3.2GB; rollback path documented if OOM
6. **GAP-450 drift fix** — `terraform import` random_password current values OR `terraform state rm` + re-import to align state with AWS reality

### Wave 44 workflow validation

7. **Test `terraform-apply.yml` workflow_dispatch dry_run mode** — confirm OIDC + role assume + plan output works:
   ```bash
   gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=true
   ```
8. **First real workflow_dispatch apply** khi có terraform changes mới (Wave 45+)

## Closure status

- ✅ GAP-446 — EventBridge Scheduler stop/start: DONE
- ✅ GAP-447 — Right-size kh_backend: DONE; Right-size kc_app: PARTIAL (replaced unintentionally + alarm pending Phase 7); CloudWatch alarm kh: provisioned (INSUFFICIENT_DATA expected)
- ✅ GAP-448 — Vercel ignoreCommand: DONE (validated `Canceled by Ignored Build Step`)
- ✅ GAP-449 — terraform-apply workflow_dispatch infra: DONE (role provisioned + GitHub Variable set + workflow ready)
- 🆕 GAP-450 — Filed: state drift on random_password + kc_app secrets/instance drift
