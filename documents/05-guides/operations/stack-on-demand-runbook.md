---
title: Stack On-Demand Runbook — Start/Stop Phase 1 BETA AWS Stack When Needed
status: active
created: 2026-05-11
phase: phase-1-beta
related_gaps: [GAP-473]
related_rules:
  - .claude/rules/agent-aws-access.md
  - .claude/rules/release-deploy-standard.md
related_scripts:
  - scripts/aws/start-stack.sh
  - scripts/aws/stop-stack.sh
---

# Stack On-Demand Runbook

> **Pattern:** Phase 1 BETA (invite-only beta, thưa khách). Stack STOPPED by default; resume on-demand cho mỗi demo/tenant-session/smoke-test. ALB always-on (Free Tier). RDS + EC2 compute paused → storage-only cost ~$3-5/mo.

## 1. When to start the stack

| Trigger | Owner | Expected duration |
|---------|-------|-------------------|
| Demo session (live customer / investor) | Solo-dev | 1-2h |
| Beta tenant onboarding (provision new tenant) | Solo-dev | 30-60min |
| Smoke test (post-deploy verification) | Solo-dev | 15min |
| Production data seed (one-time per release) | Solo-dev | 25min |
| Scheduled M-F 9-17 ICT (if EventBridge wired — §6) | EventBridge cron | 8h continuous |

**Decision rule:** if no expected use within next 30 minutes → keep stopped. Storage minimal; compute is the cost.

## 2. When to stop the stack

| Trigger | Owner |
|---------|-------|
| Idle >30 minutes (no traffic on ALB) | Solo-dev |
| End of demo session | Solo-dev |
| End of business day (if no overnight tenants active) | Solo-dev |
| Scheduled M-F 17:00 ICT auto-stop (if EventBridge wired) | EventBridge cron |

## 3. Pre-checks (before starting)

```bash
# 1. Verify AWS credentials work
aws sts get-caller-identity

# 2. Check cost MTD (informational — should be <$10 for Phase 1 BETA)
aws ce get-cost-and-usage \
  --time-period Start=$(date -u -d "$(date -u +%Y-%m-01)" +%Y-%m-%d),End=$(date -u +%Y-%m-%d) \
  --granularity MONTHLY \
  --metrics UnblendedCost \
  --query 'ResultsByTime[0].Total.UnblendedCost.Amount' \
  --output text

# 3. Verify ALB still healthy (always-on, should never be down)
aws elbv2 describe-load-balancers \
  --names kitehub-alb \
  --query 'LoadBalancers[0].State.Code' \
  --output text
# Expected: active
```

## 4. Manual start/stop cycle

### 4.1 Start (target ≤10 min wall-clock)

```bash
# Reason field optional but useful for audit trail
bash scripts/aws/start-stack.sh --reason "demo Tenant Alpha 2026-05-12 14:00 ICT"
```

What happens:
1. `aws ec2 start-instances --instance-ids i-0b65c3947d36cae61 i-07f6de54544162124`
2. `aws rds start-db-instance --db-instance-identifier kitehub-postgres`
3. Wait EC2 `running` (~30s)
4. Wait RDS `available` (~5-8min)
5. Wait EC2 status checks 2/2 (~2min after running)
6. Append session entry to `.aws-stack-state.json`

Expected total: **5-10 minutes** (RDS is the long pole).

Post-start verification:
```bash
curl -sI https://api.kitehub.me/actuator/health
# Expected: HTTP/2 200 + HSTS header
```

### 4.2 Stop (target ≤3 min wall-clock)

```bash
bash scripts/aws/stop-stack.sh
# 60s grace warning; Ctrl-C to abort. Use --force to skip warning.
```

What happens:
1. 60s grace warning (skip with `--force`)
2. `aws ec2 stop-instances ...`
3. `aws rds stop-db-instance --db-instance-identifier kitehub-postgres`
4. Wait EC2 `stopped` (~1-2min)
5. Wait RDS `stopped` (~2-3min)
6. Close session entry in `.aws-stack-state.json` (records `stop_time` + `duration_minutes`)

## 5. Dry-run mode (no AWS calls)

Both scripts support `--dry-run` for testing the orchestration without touching AWS:

```bash
bash scripts/aws/start-stack.sh --dry-run --reason "validation"
bash scripts/aws/stop-stack.sh  --dry-run
```

Used in CI gates (Wave 61 closure verification).

## 6. Optional EventBridge cron schedule (deferred Phase 1.5)

**Status:** NOT WIRED in Wave 61 (defer until beta volume justifies). When ready:

### 6.1 Architecture

```
EventBridge Rule (cron) → Lambda function → ec2:StartInstances + rds:StartDBInstance
```

Two rules:
- `kitehub-stack-start`: `cron(0 2 ? * MON-FRI *)` → 09:00 ICT M-F start
- `kitehub-stack-stop`:  `cron(0 10 ? * MON-FRI *)` → 17:00 ICT M-F stop

### 6.2 Lambda function template (skeleton)

```python
# lambda_function.py — runtime python3.11
import boto3, os
EC2_IDS = os.environ['EC2_IDS'].split(',')
RDS_ID = os.environ['RDS_ID']
ACTION = os.environ['ACTION']  # 'start' or 'stop'

def lambda_handler(event, context):
    ec2 = boto3.client('ec2')
    rds = boto3.client('rds')
    if ACTION == 'start':
        ec2.start_instances(InstanceIds=EC2_IDS)
        rds.start_db_instance(DBInstanceIdentifier=RDS_ID)
    elif ACTION == 'stop':
        ec2.stop_instances(InstanceIds=EC2_IDS)
        rds.stop_db_instance(DBInstanceIdentifier=RDS_ID)
    return {'statusCode': 200}
```

### 6.3 IAM role inline policy (minimum-scope)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["ec2:StartInstances", "ec2:StopInstances"],
      "Resource": [
        "arn:aws:ec2:ap-southeast-1:906286017800:instance/i-0b65c3947d36cae61",
        "arn:aws:ec2:ap-southeast-1:906286017800:instance/i-07f6de54544162124"
      ]
    },
    {
      "Effect": "Allow",
      "Action": ["rds:StartDBInstance", "rds:StopDBInstance"],
      "Resource": "arn:aws:rds:ap-southeast-1:906286017800:db:kitehub-postgres"
    },
    {
      "Effect": "Allow",
      "Action": ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": "arn:aws:logs:ap-southeast-1:906286017800:*"
    }
  ]
}
```

### 6.4 Wiring steps (when ready)

User wires via Console (per `agent-aws-access.md` §4: agent KHÔNG create Lambda autonomously):

1. AWS Console → Lambda → Create function (`kitehub-stack-control`, python3.11)
2. Paste skeleton above + set env vars
3. Attach IAM role with policy above
4. EventBridge → Create rule → cron expression → target = Lambda function (×2 rules with different `ACTION`)
5. Test via "Test event" → CloudWatch Logs verify
6. Disable rules during AWS Activate evaluation period (D+14) to avoid masking actual cost pattern

Track follow-up in **GAP-473** when justification crosses threshold (≥5 active beta tenants OR sustained M-F traffic pattern).

## 7. Session ledger analysis

`.aws-stack-state.json` accumulates entries per start/stop cycle:

```json
{
  "sessions": [
    {
      "start_time": "2026-05-12T07:00:00Z",
      "stop_time": "2026-05-12T09:30:00Z",
      "duration_minutes": 150.0,
      "expected_stop_time": "2026-05-12T09:00:00Z",
      "reason": "demo Tenant Alpha 2026-05-12 14:00 ICT",
      "ec2_instance_ids": ["i-0b65c3947d36cae61", "i-07f6de54544162124"],
      "rds_db_identifier": "kitehub-postgres",
      "status": "stopped"
    }
  ]
}
```

### 7.1 Cost forecast (manual analysis)

Run quick math monthly:

```bash
python3 - <<'PY'
import json
with open('.aws-stack-state.json') as f:
    data = json.load(f)
total_min = sum(s.get('duration_minutes') or 0 for s in data.get('sessions', []))
total_hours = total_min / 60
# t3.medium = $0.0416/hr × 2 = $0.0832/hr; db.t3.micro RDS ~ $0.018/hr
hourly_cost = 0.0832 + 0.018
print(f"Total runtime: {total_hours:.1f}h")
print(f"Compute cost MTD: ~${total_hours * hourly_cost:.2f}")
print(f"Storage (EBS + RDS, always-on): ~$3-5/mo")
PY
```

### 7.2 Compare to always-on baseline

- Always-on Architecture B: **~$30/mo** (per `release-1-deploy-plan.md` Phase 1 BETA estimate)
- Stop-when-idle (this pattern, ~4h/day × 5 days/week): **~$8-10/mo**
- Savings: **65-75%** while beta sparse

## 8. Troubleshooting

### 8.1 EC2 stuck in `pending` >5 min

```bash
aws ec2 describe-instance-status \
  --instance-ids i-0b65c3947d36cae61 i-07f6de54544162124 \
  --include-all-instances \
  --query 'InstanceStatuses[].[InstanceId,InstanceState.Name,InstanceStatus.Status,SystemStatus.Status]'
```

If `pending` persists: check AWS Health Dashboard for AZ ap-southeast-1a issues. Force-stop then start fresh:
```bash
aws ec2 stop-instances --instance-ids ... --force
# Wait for stopped, then start again
```

### 8.2 RDS stuck in `modifying` or `backing-up`

RDS won't accept `stop-db-instance` while in transitional states. Wait:
```bash
aws rds describe-db-instances --db-instance-identifier kitehub-postgres \
  --query 'DBInstances[0].DBInstanceStatus'
```

If `backing-up`: automatic backup window active. Wait 10-15 min. To avoid: schedule starts/stops outside the backup window (currently 18:00-19:00 UTC = 01:00-02:00 ICT).

### 8.3 ALB targets unhealthy after start

```bash
aws elbv2 describe-target-health \
  --target-group-arn $(aws elbv2 describe-target-groups \
    --names kitehub-kh-backend-tg --query 'TargetGroups[0].TargetGroupArn' --output text)
```

If unhealthy after 5 min: SSH to instance, check `systemctl status kitehub-*` — application may not be set to auto-start after EC2 boot. Fix in EC2 user-data script (separate gap).

### 8.4 RDS `stop-db-instance` returns error "is not currently available"

RDS auto-restarts every 7 days regardless of stop state (AWS hard limit). If RDS just auto-restarted, status is `starting` and `stop-db-instance` will fail. Wait for `available` then stop.

### 8.5 Session ledger orphan entries

If start runs but stop is skipped (forgot, crashed), latest entry has `stop_time: null`. Next stop closes it. Manually fix if needed:

```bash
python3 -c "
import json
with open('.aws-stack-state.json') as f: d = json.load(f)
for s in d['sessions']:
    if s.get('stop_time') is None:
        s['stop_time'] = 'YYYY-MM-DDTHH:MM:SSZ'  # manual entry
        s['status'] = 'stopped-manual'
with open('.aws-stack-state.json', 'w') as f: json.dump(d, f, indent=2)
"
```

## 9. Boundaries (per `agent-aws-access.md` §4)

| Action | Owner |
|--------|-------|
| Author scripts + runbook | Agent |
| Tier 1 read-only verification (describe / list) | Agent |
| `ec2:StartInstances` / `StopInstances` execution | **User only** |
| `rds:StartDBInstance` / `StopDBInstance` execution | **User only** |
| Wire EventBridge + Lambda (§6) | **User only** (Console + IAM) |

Agent NEVER invokes `scripts/aws/start-stack.sh` or `stop-stack.sh` directly. Always user-triggered.

## 10. Related

- Plan: `documents/03-planning/waves/wave-2026-05-12-61-stop-when-idle-cutover.md` §3 Bucket D
- Gap: `documents/04-quality/gaps/GAP-473-aws-stack-on-demand-automation.md`
- Sister scripts: `scripts/smoke-test.sh` (post-start verification), `scripts/aws/start-stack.sh`, `scripts/aws/stop-stack.sh`
- Cost context: `release-1-deploy-plan.md` Phase 1 BETA architecture cost
- Rule: `.claude/rules/agent-aws-access.md` §4 (BANNED autonomous lifecycle ops)
- Future: GAP-473 §Proposed Fix — EventBridge cron + Lambda scheduler (defer Phase 1.5)

## 11. Log

- **2026-05-11:** Runbook authored as part of Wave 61 Bucket D. Manual start/stop scripts shipped + ledger schema defined; EventBridge cron deferred to GAP-473 future scope per stop-when-idle pattern (invite-only beta thưa).
