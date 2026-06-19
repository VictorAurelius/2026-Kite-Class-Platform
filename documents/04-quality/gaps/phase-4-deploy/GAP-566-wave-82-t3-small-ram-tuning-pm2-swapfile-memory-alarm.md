---
id: GAP-566
title: t3.small RAM tuning — PM2 max_memory_restart + 2GB swapfile + CloudWatch memory alarm (Wave 82 Bucket B prerequisite)
status: OPEN
priority: P0
domain: DevOps
phase: phase-1-beta
percent_complete: 0
created: 2026-05-15
updated: 2026-05-15
wave_target: 82
---

# GAP-566 — Wave 82 t3.small RAM tuning + memory observability

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — BLOCKING Bucket B FE deploy
**Domain:** DevOps / Infrastructure
**Found:** 2026-05-15 (Wave 82 Bucket A outside-in failure-mode matrix audit, finding F7)
**Affects:** Wave 82 FE self-host EC2 kc-app (t3.small 2GB RAM) — kitehub-frontend + kiteclass-frontend Next standalone + nginx + PM2

---

## Problem

Wave 82 Bucket A failure-mode matrix audit (finding F7) flag rủi ro: t3.small instance chỉ có 2GB RAM mà Bucket B sẽ chạy đồng thời 2 Next.js standalone (kitehub-frontend + kiteclass-frontend) + nginx + PM2 process manager + system overhead. Dưới load thông thường có thể OK, nhưng khi route `/beta-status` (đã dùng Next ISR `revalidate = 300` per Wave 78 PR) trigger regen đồng thời với traffic burst, V8 heap có thể vượt limit → Linux OOM-killer terminate Node process.

Khi OOM xảy ra:
- (a) PM2 sẽ tự restart, nhưng restart loop nếu RAM không giải phóng → service unstable
- (b) User thấy 502 Bad Gateway từ nginx upstream
- (c) Không có observability nếu không setup CloudWatch memory metric — chỉ phát hiện qua user complaint

t3.small tight nhưng vẫn workable nếu setup đúng: swapfile cung cấp safety net, PM2 `max_memory_restart` graceful restart trước khi OOM, CloudWatch alarm cảnh báo sớm trước khi user thấy ảnh hưởng.

---

## Root Cause

Default EC2 Amazon Linux 2023 + Ubuntu AMI KHÔNG có swap enabled. PM2 ecosystem.config.js mặc định không có `max_memory_restart` → process chạy đến khi OS OOM-kill. CloudWatch agent KHÔNG được install default → memory metric không lên Console, không có alarm.

Failure matrix F7 surface 3 mitigation cần ship đồng thời để t3.small reliable:
1. Swapfile 2GB (= RAM size, standard Linux recommendation cho small instances)
2. PM2 `max_memory_restart = '1.2G'` per Next instance (1.2GB × 2 = 2.4GB nominal, swapfile gánh overhead)
3. CloudWatch agent + alarm at >85% memory 5min

---

## Proposed Fix

### Bước 1: Provisioning script tạo 2GB swapfile

Trong user_data của `aws_instance.kc_app` (terraform):

```bash
#!/bin/bash
set -e

# Swapfile 2GB
if [ ! -f /swapfile ]; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

# Verify
swapon --show
free -h
```

### Bước 2: PM2 ecosystem.config.js với max_memory_restart

File: `/home/ec2-user/kc-app/ecosystem.config.js` (deploy artifact)

```javascript
module.exports = {
  apps: [
    {
      name: 'kitehub-frontend',
      script: 'node',
      args: 'server.js',
      cwd: '/home/ec2-user/kitehub-frontend/.next/standalone',
      env: {
        PORT: 4701,
        NODE_ENV: 'production',
        HOSTNAME: '127.0.0.1'  // bind loopback only, nginx proxies
      },
      max_memory_restart: '1.2G',
      autorestart: true,
      max_restarts: 10,
      min_uptime: '60s'
    },
    {
      name: 'kiteclass-frontend',
      script: 'node',
      args: 'server.js',
      cwd: '/home/ec2-user/kiteclass-frontend/.next/standalone',
      env: {
        PORT: 4702,
        NODE_ENV: 'production',
        HOSTNAME: '127.0.0.1'
      },
      max_memory_restart: '1.2G',
      autorestart: true,
      max_restarts: 10,
      min_uptime: '60s'
    }
  ]
};
```

### Bước 3: CloudWatch agent + memory alarm

Install CloudWatch agent (Amazon Linux 2023):

```bash
sudo dnf install -y amazon-cloudwatch-agent
sudo cat > /opt/aws/amazon-cloudwatch-agent/etc/config.json <<'EOF'
{
  "metrics": {
    "namespace": "KiteHub/EC2/kc-app",
    "metrics_collected": {
      "mem": {"measurement": ["mem_used_percent"], "metrics_collection_interval": 60},
      "swap": {"measurement": ["swap_used_percent"], "metrics_collection_interval": 60}
    }
  }
}
EOF
sudo systemctl enable --now amazon-cloudwatch-agent
```

Terraform CloudWatch alarm (in `infrastructure/terraform-aws/cloudwatch-alarms.tf`):

```hcl
resource "aws_cloudwatch_metric_alarm" "kc_app_memory_high" {
  alarm_name          = "kitehub-kc-app-memory-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 5
  metric_name         = "mem_used_percent"
  namespace           = "KiteHub/EC2/kc-app"
  period              = 60
  statistic           = "Average"
  threshold           = 85
  alarm_description   = "kc-app memory >85% for 5min — PM2 reload risk"
  alarm_actions       = [aws_sns_topic.kite_alerts.arn]
  treat_missing_data  = "notBreaching"
}
```

### Bước 4: Load test PM2 graceful restart

Sau khi deploy, simulate memory pressure qua ISR regen + concurrent requests:

```bash
# Trigger ISR regen + traffic burst
for i in {1..50}; do curl -s "https://<fe-domain>/beta-status" > /dev/null & done
wait

# Verify PM2 status
pm2 list
pm2 logs --lines 100 | grep -i "memory\|restart"
```

Expected: PM2 reload graceful (Online status maintained), không có OOM-kill log trong `dmesg`.

---

## Acceptance Criteria

- [ ] EC2 user_data tạo `/swapfile` 2GB, `swapon --show` shows `/swapfile partition 2G` post-provision
- [ ] `/etc/fstab` chứa entry `/swapfile none swap sw 0 0` (persistent qua reboot)
- [ ] `ecosystem.config.js` 2 apps đều có `max_memory_restart: '1.2G'`
- [ ] CloudWatch agent service active: `systemctl is-active amazon-cloudwatch-agent` returns `active`
- [ ] CloudWatch namespace `KiteHub/EC2/kc-app` có metric `mem_used_percent` cập nhật mỗi 60s (verify trong Console hoặc `aws cloudwatch list-metrics --namespace KiteHub/EC2/kc-app`)
- [ ] CloudWatch alarm `kitehub-kc-app-memory-high` trong state `OK` post-deploy: `aws cloudwatch describe-alarms --alarm-names kitehub-kc-app-memory-high --query 'MetricAlarms[].StateValue'`
- [ ] Load test 50 concurrent requests vào `/beta-status` (ISR route per Wave 78): PM2 không OOM-kill, `dmesg | grep -i oom` trống, `pm2 list` 2 apps online
- [ ] Cross-link `release-deploy-standard.md` §3.1 Reliability pillar artifact list

## Related

- Wave plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §1 Brainstorm Q3 + §3 Bucket B
- Failure-matrix finding: **F7** Wave 82 Bucket A outside-in audit 2026-05-15
- Sister gaps: GAP-565 (F6 SG) · GAP-567 (F10 cert) · GAP-568 (F11 CORS)
- Existing route relying on ISR: `kitehub-frontend/src/app/beta-status/page.tsx` (`revalidate = 300`, Wave 78 PR)
- Rules: `.claude/rules/release-deploy-standard.md` §3.1 Reliability pillar
- AWS docs reference: [t3.small instance specs](https://aws.amazon.com/ec2/instance-types/t3/) — 2 vCPU / 2GB RAM / burstable

## Log

- **2026-05-15:** Gap filed via Wave 82 Bucket A outside-in failure-mode matrix audit (finding F7). P0 BLOCKING — phải address trong cùng Bucket B EC2 provisioning. 3 mitigations đồng thời (swapfile + PM2 max_memory_restart + CloudWatch memory alarm) cần ship cùng terraform apply. Nếu defer riêng → t3.small unstable dưới ISR regen load, user thấy 502.
