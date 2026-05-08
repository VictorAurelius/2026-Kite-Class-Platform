# AWS Cost Scheduling Runbook — EventBridge Scheduler stop/start

**Owner:** Solo-dev / SRE
**Source:** [GAP-446](../../04-quality/gaps/GAP-446-aws-resource-scheduling-cost-saving.md), [Wave 43](../../03-planning/waves/wave-2026-05-08-43-cost-discipline.md)
**Terraform:** `infrastructure/terraform-aws/scheduler.tf`
**Last Updated:** 2026-05-08

---

## 1. Purpose

EventBridge Scheduler stops EC2 (`kh-backend`, `kc-app`) + RDS (`kitehub-postgres`) outside working hours to save ~$70/mo while Phase 1 BETA is invite-only solo-dev mode. AWS-managed.

---

## 2. Schedule (Asia/Ho_Chi_Minh ICT)

| Cron expression | Action | Targets |
|---|---|---|
| `cron(0 22 ? * MON-FRI *)` | Stop | EC2 (2 instances) + RDS |
| `cron(0 8 ? * MON-FRI *)` | Start | EC2 (2 instances) + RDS |
| `cron(0 22 ? * FRI *)` | Stop (defensive) | EC2 + RDS |
| `cron(0 8 ? * MON *)` | Start | EC2 + RDS |

**Off-hours coverage:** ~58% of week (10h × 5 weeknights + 60h weekend ≈ 98h/168h).

---

## 3. Verification

After terraform apply, verify the 8 schedules are firing:

```bash
# List all schedules in the cost-saving group
aws scheduler list-schedules \
  --group-name kitehub-cost-saving \
  --region ap-southeast-1 \
  --query 'Schedules[*].[Name,State]' --output table

# Inspect one schedule
aws scheduler get-schedule \
  --name stop-weekday-evening-ec2 \
  --group-name kitehub-cost-saving \
  --region ap-southeast-1
```

After firing time passes, confirm transitions in CloudTrail:

```bash
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=EventName,AttributeValue=StopInstances \
  --region ap-southeast-1 \
  --max-results 5
```

---

## 4. Manual override — emergency START

When an instance needs to be running outside scheduled hours (incident triage, demo, late-night dev):

```bash
# Start EC2 instances (replace IDs with `terraform output` values)
aws ec2 start-instances \
  --instance-ids i-xxxxxxxx i-yyyyyyyy \
  --region ap-southeast-1

# Start RDS
aws rds start-db-instance \
  --db-instance-identifier kitehub-postgres \
  --region ap-southeast-1

# Wait for healthy
aws ec2 wait instance-running --instance-ids i-xxxxxxxx i-yyyyyyyy
aws rds wait db-instance-available --db-instance-identifier kitehub-postgres
```

The next scheduled stop (22:00 ICT) WILL still fire. To pause schedules during a longer window, see §5.

---

## 5. Disable scheduling (for incident, demo day, GA cutover)

### Option A — Pause individual schedule (no terraform churn)

```bash
aws scheduler update-schedule \
  --name stop-weekday-evening-ec2 \
  --group-name kitehub-cost-saving \
  --state DISABLED \
  --region ap-southeast-1
# ... repeat for the other 7 schedules as needed
```

Re-enable with `--state ENABLED`.

### Option B — Disable all scheduling via terraform (full off)

Set the variable to `false` and re-apply:

```bash
cd infrastructure/terraform-aws
terraform plan -var=enable_cost_scheduling=false
# Human reviews plan, then:
terraform apply -var=enable_cost_scheduling=false
```

This destroys the schedule group + 8 schedules + IAM role. Re-applying with the variable back to `true` (default) recreates everything.

---

## 6. Monitoring — did the schedule actually fire?

### Quick check via CloudTrail

```bash
# Last 24h Stop/Start events
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=Username,AttributeValue=AWSScheduler_kitehub \
  --region ap-southeast-1 \
  --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%SZ) \
  --query 'Events[].[EventTime,EventName,Resources[0].ResourceName]' \
  --output table
```

### Alarm hook (future enhancement)

If a schedule fails (IAM denied / target ARN drift), EventBridge Scheduler increments `FailedInvocations` CloudWatch metric. Wire `aws_cloudwatch_metric_alarm` per `GAP-413` budget alarm pattern → SNS topic → email. Tracked under existing GAP-413 follow-up; no separate gap.

---

## 7. Caveats

- **RDS 7-day auto-restart:** AWS auto-starts a stopped RDS after 7 days. Friday-stop → Monday-start (~60h) stays well under 7d, OK.
- **ALB stays running** — cannot be stopped (only deleted). Cost ~$22/mo. Out of scope; right-sizing tracked under GAP-447.
- **Public IP changes** — when EC2 stops, public IPv4 address is released; new IP assigned on start. ALB target group health-check re-registers automatically. DNS via Cloudflare proxy → ALB DNS unchanged.
- **Cold-start latency on Monday morning** — first request after start hits a cold JVM (~30-60s). Acceptable for invite-only solo-dev mode.
- **Cost monitoring** — combine with GAP-413 (Budgets alarms) + GAP-414 (monthly review) for full FinOps cycle.

---

## 8. Rollback (if scheduling causes incident)

1. **Immediate:** Disable all 8 schedules via Option A in §5 (per-schedule, takes ~30s each).
2. **Verify:** instances start manually per §4 if currently stopped.
3. **Followup:** file P0 incident gap referencing this runbook + RCA.
4. **Permanent off:** apply Option B with `enable_cost_scheduling=false`.

---

## 9. Related

- `infrastructure/terraform-aws/scheduler.tf` — terraform definitions
- `.claude/rules/agent-aws-access.md` §2 — `aws scheduler list-schedules` is Tier 1 read-only (logged here is OK)
- `.claude/rules/release-deploy-standard.md` §3 — cost guards for Phase 1 BETA
- GAP-413 (Budgets alarms PARTIAL), GAP-414 (monthly review), GAP-447 (right-size review)
