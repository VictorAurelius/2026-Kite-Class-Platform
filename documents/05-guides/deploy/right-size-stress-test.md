# Right-Size EC2 Stress-Test Runbook — Hướng Dẫn Test Tải Trước Khi Hạ Cấp Instance

**Trạng thái:** active
**Tạo:** 2026-05-08
**Wave:** 43 Bucket B
**Closes:** GAP-447 (PARTIAL — terraform shipped, apply + stress test post-merge)
**Liên quan:** GAP-411 (sizing matrix), GAP-446 (EventBridge stop/start), `release-deploy-standard.md` §3.4

---

## Mục đích

Step-by-step runbook for right-sizing both Phase 1 BETA EC2 instances from `m7i-flex.large` 8GB → `t3.medium` 4GB safely:

1. Pre-downsize backup + CloudWatch agent setup (OOM safety net)
2. Apply ordering: kh-backend trước (live data, dễ stress-test), kc-app sau
3. 1-hour stress test on kh-backend before applying kc-app
4. Memory monitoring commands
5. Rollback escalation: JVM heap tune → t3.large → m7i-flex.large

---

## Phase 0 — Điều kiện tiên quyết

- [ ] Wave 43 Bucket B PR merged (terraform diff: `kh_backend_instance_type` + `kc_app_instance_type` defaults → `t3.medium`; `cloudwatch.tf` SNS + 2 memory alarms)
- [ ] Email `vannkite@outlook.com` confirmed SNS subscription (AWS sends confirmation link after `terraform apply`)
- [ ] Current AMI snapshots taken for both instances (rollback path)
- [ ] User has SSM Session Manager or SSH access to both EC2
- [ ] Stress-test window scheduled (low-traffic period; ~2h budget for kh-backend)

---

## Phase 1 — Cài đặt CloudWatch agent (CẢ 2 instances, TRƯỚC khi downsize)

CloudWatch agent emits `mem_used_percent` to `CWAgent` namespace. Without this, the alarms in `cloudwatch.tf` stay `INSUFFICIENT_DATA` (quiet, not noisy — but no OOM detection).

### 1.1 Agent config payload

Save this as `/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json` on each EC2 (use SSM `AWS-RunShellScript` to push):

```json
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root"
  },
  "metrics": {
    "namespace": "CWAgent",
    "append_dimensions": {
      "InstanceId": "${aws:InstanceId}"
    },
    "metrics_collected": {
      "mem": {
        "measurement": ["mem_used_percent"],
        "metrics_collection_interval": 60
      },
      "disk": {
        "resources": ["/"],
        "measurement": ["used_percent"],
        "metrics_collection_interval": 300
      }
    }
  }
}
```

### 1.2 Start agent (per instance)

```bash
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json \
  -s
```

### 1.3 Verify metrics flowing (Tier 1 read-only)

```bash
# Wait ~3 min after agent start
aws cloudwatch get-metric-data \
  --metric-data-queries '[{"Id":"m1","MetricStat":{"Metric":{"Namespace":"CWAgent","MetricName":"mem_used_percent","Dimensions":[{"Name":"InstanceId","Value":"<INSTANCE_ID>"}]},"Period":60,"Stat":"Average"}}]' \
  --start-time $(date -u -d '10 minutes ago' +%FT%TZ) \
  --end-time   $(date -u +%FT%TZ) \
  --region ap-southeast-1
```

Expected: `Values` array non-empty. If empty after 5 min → agent config error; check `/var/log/amazon-cloudwatch-agent.log`.

### 1.4 Confirm alarm transition out of INSUFFICIENT_DATA

```bash
aws cloudwatch describe-alarms \
  --alarm-names kitehub-kh-backend-memory-high kitehub-kc-app-memory-high \
  --query 'MetricAlarms[*].[AlarmName,StateValue]' \
  --output table \
  --region ap-southeast-1
```

Expected: `OK` (assuming current m7i-flex.large 8GB is comfortably under 85%).

---

## Phase 2 — Downsize kh-backend TRƯỚC (sequential, KHÔNG parallel)

Why kh-backend first: live tenant data, broader service mix → richer stress signal. If t3.medium 4GB OOMs on KH, no point trying KC.

### 2.1 Apply terraform (user-only — Tier 3 banned for agent)

User runs:

```bash
cd infrastructure/terraform-aws
terraform plan -target=aws_instance.kh_backend
# Verify diff = instance_type m7i-flex.large → t3.medium ONLY
terraform apply -target=aws_instance.kh_backend
```

EC2 instance_type change → instance stop/start (NOT replace). Public IP MAY change if not Elastic IP — verify before relying on cached endpoints.

### 2.2 Post-apply verification (agent — Tier 1)

```bash
aws ec2 describe-instances \
  --instance-ids <KH_BACKEND_ID> \
  --query 'Reservations[0].Instances[0].[InstanceType,State.Name,PublicIpAddress]' \
  --output table \
  --region ap-southeast-1
```

Expected: `t3.medium`, `running`, IP. Then verify Docker stack:

```bash
# SSM session
sudo docker ps --format 'table {{.Names}}\t{{.Status}}'
# All 5 KH services + redis + rabbitmq + gateway = healthy
```

### 2.3 1-hour stress test

Drive synthetic traffic OR real user traffic (low-impact, monitored):

```bash
# Watch memory in real-time
watch -n 30 'free -m && docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}"'
```

Pass criteria after 1h:
- [ ] `mem_used_percent` peak <85% (alarm threshold)
- [ ] Zero OOMKilled events: `dmesg -T | grep -i 'killed process'`
- [ ] All KH services responsive: `curl -s http://localhost:8080/actuator/health` returns 200
- [ ] No SNS email triggered

If pass → proceed Phase 3. If fail → **STOP, escalate to Phase 5 rollback**.

---

## Phase 3 — Downsize kc-app (sau khi kh-backend stress test pass)

Same procedure as Phase 2, target `aws_instance.kc_app`.

```bash
terraform plan -target=aws_instance.kc_app
terraform apply -target=aws_instance.kc_app
```

Post-apply verification + 1h stress test (lighter traffic — Vercel handles FE; backend-only KC stack).

---

## Phase 4 — Theo dõi memory (định kỳ, post-deploy)

CloudWatch dashboard `kitehub-phase-1-overview` (per `cloudwatch-dashboard.tf`) does NOT yet include memory widget — add memory row in follow-up PR if needed. For now:

```bash
# Spot check
aws cloudwatch get-metric-statistics \
  --namespace CWAgent \
  --metric-name mem_used_percent \
  --dimensions Name=InstanceId,Value=<ID> \
  --start-time $(date -u -d '1 hour ago' +%FT%TZ) \
  --end-time   $(date -u +%FT%TZ) \
  --period 300 \
  --statistics Average,Maximum \
  --region ap-southeast-1
```

Alarms in `cloudwatch.tf` will fire automatically; no polling needed.

---

## Phase 5 — Rollback escalation (khi instance vượt ngưỡng)

If memory consistently >85% OR OOMKilled events:

### Step 1 — JVM heap tune (cheapest, try first)

Edit `docker-compose.production.yml` (KH) or `docker-compose.kc.yml` (KC). Reduce non-critical service heaps:

| Service | Current `-Xmx` | Tuned `-Xmx` |
|---|---|---|
| `kitehub-email` (low traffic) | 384m | 256m |
| `kitehub-platform` (low traffic) | 384m | 256m |
| `kiteclass-gateway` | 256m | 192m |

Re-deploy: `bash scripts/deploy-prod.sh` (or `deploy-kc.sh`). Re-run 1h stress test.

### Step 2 — Upsize t3.large 8GB (same family, ~$60/mo)

```bash
# variables.tf override OR -var on command line
terraform apply -var="kh_backend_instance_type=t3.large" -target=aws_instance.kh_backend
```

t3.large 8GB still cheaper than m7i-flex.large 8GB ($60 vs $60 — comparable; pick t3 for free-tier compatibility).

### Step 3 — Revert m7i-flex.large 8GB (if Step 1+2 insufficient)

```bash
terraform apply -var="kh_backend_instance_type=m7i-flex.large" -target=aws_instance.kh_backend
```

File follow-up gap on root cause (memory leak? unaccounted service growth?). Right-size revisit deferred until root cause fixed.

---

## Kết hợp với GAP-446 stop/start scheduling

After right-size lands, EventBridge stop/start (per GAP-446) compounds savings:

- Right-size only: $120 → $60/mo
- Stop/start ~58% downtime: $60 × 42% = ~$25/mo

Stop/start should land AFTER right-size proves stable (≥7 days post-stress-test).

---

## Verification artifact (tài liệu xác minh)

Per `release-deploy-standard.md` §9 (post-deploy verification = agent role) + `agent-aws-access.md` §5 (mandatory logging):

After stress test completes, save report to:
`documents/04-quality/audits/aws-verification/2026-05-NN-wave-43-right-size.md`

Include:
- CloudWatch agent install timestamp
- terraform apply outputs (per instance)
- Stress test peak memory (graph URL or CSV export)
- Pass/fail per pass criteria
- Any rollback steps taken

---

## Tiêu chí nghiệm thu (closes GAP-447)

- [ ] Phase 1 CloudWatch agent installed + emitting `mem_used_percent` on both EC2
- [ ] Phase 2 kh-backend downsized + 1h stress test passed
- [ ] Phase 3 kc-app downsized + 1h stress test passed
- [ ] Phase 4 alarms confirmed `OK` state for ≥24h
- [ ] Verification artifact saved per `agent-aws-access.md` §5

PARTIAL until all checked. Phase 5 rollback steps used → file follow-up gap on root cause.
