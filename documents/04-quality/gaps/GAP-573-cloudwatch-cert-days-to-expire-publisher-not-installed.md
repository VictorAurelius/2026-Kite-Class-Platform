---
title: "GAP-573: CloudWatch CertDaysToExpire metric publisher not installed"
status: OPEN
priority: P2
domain: DevOps
phase: phase-1-beta
wave: 82-bucket-b-followup
created: 2026-05-15
---

# GAP-573: CloudWatch cert-days-to-expire metric publisher chưa install

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (cert valid 90d; alarm stuck nhưng không hide real issue)
**Domain:** DevOps / Monitoring
**Found:** 2026-05-15 Wave 82 Bucket B post-apply state-check
**Affects:** `aws_cloudwatch_metric_alarm.kc_app_fe_cert_expiry` — currently stuck `INSUFFICIENT_DATA` thay vì `OK`

## Problem

`infrastructure/fe-host/certbot-dns-01-setup.sh` Step 5 (install `/usr/local/bin/cert-days-to-expire.sh` + daily systemd timer publishing CloudWatch metric `KiteHub/EC2/kc-app/CertDaysToExpire`) chưa chạy do script aborted ở Step 4 (xem GAP-572).

Hậu quả: `aws cloudwatch describe-alarms --alarm-name-prefix kitehub-kc-app-fe` shows:

```
kitehub-kc-app-fe-cert-expiry  INSUFFICIENT_DATA  CertDaysToExpire  30.0
```

Alarm sẽ vĩnh viễn stuck `INSUFFICIENT_DATA` (treat_missing_data="breaching" theo terraform, nhưng `INSUFFICIENT_DATA` không tự `ALARM`). Khi cert thực sự còn <30d, alarm KHÔNG fire.

## Root Cause

Bash `set -euo pipefail` trong `certbot-dns-01-setup.sh` → khi Step 4 systemctl fail, script exit code 1 + Step 5/6 skipped.

GAP-572 fix sẽ unblock Step 5 chạy được. HOẶC Step 5 có thể tách ra script độc lập (recommended cho ops resilience).

## Proposed Fix

**Option A — Fix script + re-run:**
1. Apply GAP-572 fix
2. SSM SendCommand re-run `bash /tmp/certbot-dns-01-setup.sh` trên EC2 i-05cfda7c6c60b683f
3. Verify `/usr/local/bin/cert-days-to-expire.sh` exists + systemd timer active
4. Manual trigger: `sudo /usr/local/bin/cert-days-to-expire.sh kitehub.me` → publish metric ngay
5. Verify CloudWatch alarm transitions `INSUFFICIENT_DATA` → `OK`

**Option B — Extract Step 5 thành script độc lập (recommended for resilience):**
- `infrastructure/fe-host/cert-monitor-setup.sh` — chỉ install metric publisher (không depend on certbot timer)
- SSM SendCommand riêng cho cert monitor setup
- Pros: Step 4 (timer) and Step 5 (monitor) decouple; failure in one doesn't cascade
- Cons: 2 separate setup steps; runbook needs update

## Acceptance Criteria

- [ ] `/usr/local/bin/cert-days-to-expire.sh` exists trên EC2 (executable)
- [ ] systemd timer `cert-days-monitor.timer` enabled + active
- [ ] `aws cloudwatch get-metric-statistics --namespace KiteHub/EC2/kc-app --metric-name CertDaysToExpire` returns data points
- [ ] Alarm `kitehub-kc-app-fe-cert-expiry` transitions từ `INSUFFICIENT_DATA` → `OK` (cert >30d remaining = OK)
- [ ] (Optional) Manual test alarm-fire flow: temporarily set CertDaysToExpire metric = 25 → alarm transitions to ALARM → revert

## Workaround (immediate)

Manual cert expiry check qua cron OR weekly reminder. Cert exp 2026-08-13 → set reminder 2026-07-13 (30d before).

## Related

- Sister gap: GAP-572 (certbot timer setup fail — root cause for this gap's Step 5 skip)
- Wave 82 Bucket B post-apply audit: `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-post-apply.md`
- Terraform alarm: `aws_cloudwatch_metric_alarm.kc_app_fe_cert_expiry` in `infrastructure/terraform-aws/ec2-kc-app.tf`
- Rule: `output-review-mandate.md` §3 Ops Readiness row (monitoring/alerting baseline)

## Log

- **2026-05-15:** Gap filed post Wave 82 Bucket B. Cert acquired successfully nhưng Step 5 metric publisher script not installed do upstream Step 4 fail (GAP-572). Alarm armed at terraform-apply time nhưng stuck `INSUFFICIENT_DATA`. Manual `certbot renew` + manual cert expiry check sufficient cho 90-day Phase 1 BETA window; auto-monitor là follow-up.
