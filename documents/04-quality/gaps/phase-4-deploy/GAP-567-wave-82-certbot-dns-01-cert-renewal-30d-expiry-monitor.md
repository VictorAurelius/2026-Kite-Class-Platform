---
id: GAP-567
title: Certbot DNS-01 cert renewal via Cloudflare API + 30d expiry CloudWatch monitor (Wave 82 Bucket B prerequisite)
status: OPEN
priority: P0
domain: DevOps
phase: phase-1-beta
percent_complete: 0
created: 2026-05-15
updated: 2026-05-15
wave_target: 82
---

# GAP-567 — Wave 82 Certbot DNS-01 + cert expiry monitor

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — BLOCKING Bucket B FE deploy + post-90d outage risk
**Domain:** DevOps / Infrastructure
**Found:** 2026-05-15 (Wave 82 Bucket A outside-in failure-mode matrix audit, finding F10)
**Affects:** Wave 82 FE self-host EC2 kc-app — TLS cert for FE domain (`app.kitehub.me` hoặc tương đương)

---

## Problem

Wave 82 Bucket A failure-mode matrix audit (finding F10) flag rủi ro nghiêm trọng: Let's Encrypt cert mặc định 90-day expiry. Nếu certbot auto-renewal lỗi mà không có monitoring, ngày 90 cert expire → toàn bộ FE traffic 100% outage (browser hiển thị `NET::ERR_CERT_DATE_INVALID`, không user nào bypass được warning để login).

Failure mode cụ thể trên Wave 82 setup:
- **HTTP-01 challenge race condition**: certbot HTTP-01 cần bind port 80 để serve `/.well-known/acme-challenge/`. Nhưng nginx đã hold port 80 cho redirect-to-HTTPS. Certbot phải hoặc (a) stop nginx tạm, (b) dùng webroot mode trỏ vào nginx-served path. Cả 2 đều có race với PM2 / nginx config drift → renewal có thể silent fail.
- **No monitoring**: nếu renewal fail, không alert → discover khi user complain ngày cert hết hạn.

DNS-01 challenge giải quyết race: certbot tạo TXT record qua Cloudflare API, Let's Encrypt verify qua DNS, KHÔNG cần port 80 / không cần nginx restart. Cộng với CloudWatch metric `days_to_expire` + alarm threshold 30d, có 30 ngày buffer để fix nếu renewal vẫn fail.

---

## Root Cause

Default certbot tutorial cho EC2/nginx dùng HTTP-01 vì đơn giản (`certbot --nginx`). Nhưng với production hardening:
- nginx config phức tạp (gzip, rate limit, security headers) → certbot tự edit config có thể conflict
- PM2 reload chu kỳ có thể trigger nginx reload → race với certbot ACME challenge
- Free Tier không có upstream monitor service → cert expiry blind spot

Failure matrix F10 surface mitigation = (a) chuyển DNS-01 challenge dùng Cloudflare API token (KiteHub đã active CF DNS per Wave 81 closure), (b) systemd timer cho auto-renewal, (c) CloudWatch custom metric `cert_days_to_expire` + alarm at <30 ngày.

---

## Proposed Fix

### Bước 1: Cloudflare API token cho DNS-01

Tạo CF API token scoped tới `kitehub.me` zone với permission `Zone:DNS:Edit` (only DNS write, không read full zone). Lưu vào AWS Secrets Manager:

```bash
aws secretsmanager create-secret \
  --name kitehub/production/cloudflare-api-token-certbot \
  --description "Cloudflare API token for certbot DNS-01 challenge (kitehub.me zone DNS:Edit only)" \
  --secret-string "$CF_API_TOKEN"
```

### Bước 2: Install certbot + DNS-01 plugin trong EC2 user_data

```bash
# Amazon Linux 2023
sudo dnf install -y certbot python3-certbot-dns-cloudflare

# Fetch CF token từ Secrets Manager
sudo mkdir -p /etc/letsencrypt
CF_TOKEN=$(aws secretsmanager get-secret-value \
  --secret-id kitehub/production/cloudflare-api-token-certbot \
  --query SecretString --output text)
sudo tee /etc/letsencrypt/cloudflare.ini > /dev/null <<EOF
dns_cloudflare_api_token = $CF_TOKEN
EOF
sudo chmod 600 /etc/letsencrypt/cloudflare.ini

# Initial issuance (replace <fe-domain>)
sudo certbot certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials /etc/letsencrypt/cloudflare.ini \
  --dns-cloudflare-propagation-seconds 60 \
  -d app.kitehub.me \
  --non-interactive --agree-tos -m admin@kitehub.me
```

### Bước 3: systemd timer cho auto-renewal

certbot package mặc định ship `certbot-renew.timer` (Amazon Linux 2023 + Ubuntu). Verify:

```bash
sudo systemctl enable --now certbot-renew.timer
sudo systemctl list-timers certbot-renew.timer
# Test dry-run renewal
sudo certbot renew --dry-run
```

Expected output: `Congratulations, all simulated renewals succeeded`.

### Bước 4: CloudWatch custom metric cho cert days-to-expire

Script `/usr/local/bin/cert-days-to-expire.sh`:

```bash
#!/bin/bash
set -e

DOMAIN="${1:-app.kitehub.me}"
CERT_FILE="/etc/letsencrypt/live/$DOMAIN/cert.pem"

if [ ! -f "$CERT_FILE" ]; then
  echo "Cert not found: $CERT_FILE" >&2
  exit 1
fi

EXPIRY_EPOCH=$(date -d "$(openssl x509 -enddate -noout -in "$CERT_FILE" | cut -d= -f2)" +%s)
NOW_EPOCH=$(date +%s)
DAYS=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))

aws cloudwatch put-metric-data \
  --namespace "KiteHub/EC2/kc-app" \
  --metric-name "CertDaysToExpire" \
  --dimensions Domain="$DOMAIN" \
  --value "$DAYS" \
  --unit Count
```

systemd timer chạy hằng ngày:

```ini
# /etc/systemd/system/cert-days-monitor.service
[Unit]
Description=Push cert days-to-expire to CloudWatch

[Service]
Type=oneshot
ExecStart=/usr/local/bin/cert-days-to-expire.sh app.kitehub.me

# /etc/systemd/system/cert-days-monitor.timer
[Unit]
Description=Daily cert days-to-expire push

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
sudo systemctl enable --now cert-days-monitor.timer
```

### Bước 5: CloudWatch alarm at <30 days

Trong `infrastructure/terraform-aws/cloudwatch-alarms.tf`:

```hcl
resource "aws_cloudwatch_metric_alarm" "kc_app_cert_expiry" {
  alarm_name          = "kitehub-kc-app-cert-expiry"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = 1
  metric_name         = "CertDaysToExpire"
  namespace           = "KiteHub/EC2/kc-app"
  period              = 86400
  statistic           = "Minimum"
  threshold           = 30
  alarm_description   = "kc-app TLS cert <30 days to expire — verify certbot-renew.timer"
  alarm_actions       = [aws_sns_topic.kite_alerts.arn]
  treat_missing_data  = "breaching"

  dimensions = {
    Domain = "app.kitehub.me"
  }
}
```

### Bước 6: Test alarm fires (mock cert-days=29)

```bash
# One-shot push fake value để verify alarm wiring
aws cloudwatch put-metric-data \
  --namespace KiteHub/EC2/kc-app \
  --metric-name CertDaysToExpire \
  --dimensions Domain=app.kitehub.me \
  --value 29

# Wait ~5min, verify alarm state ALARM
aws cloudwatch describe-alarms --alarm-names kitehub-kc-app-cert-expiry \
  --query 'MetricAlarms[].StateValue'
# Expected: "ALARM"

# Reset
aws cloudwatch put-metric-data \
  --namespace KiteHub/EC2/kc-app \
  --metric-name CertDaysToExpire \
  --dimensions Domain=app.kitehub.me \
  --value 90
```

---

## Acceptance Criteria

- [ ] CF API token `kitehub/production/cloudflare-api-token-certbot` exists trong AWS Secrets Manager với scope DNS:Edit only (verify via `aws secretsmanager describe-secret`)
- [ ] EC2 user_data install `certbot` + `python3-certbot-dns-cloudflare` packages successful
- [ ] Initial cert issuance qua `certbot certonly --dns-cloudflare` returns success cho `app.kitehub.me`
- [ ] `/etc/letsencrypt/live/app.kitehub.me/cert.pem` exists + readable
- [ ] `sudo certbot renew --dry-run` outputs `Congratulations, all simulated renewals succeeded`
- [ ] systemd timer `certbot-renew.timer` enabled + active: `systemctl is-active certbot-renew.timer` returns `active`
- [ ] systemd timer `cert-days-monitor.timer` enabled + active
- [ ] CloudWatch metric `KiteHub/EC2/kc-app/CertDaysToExpire` có data point trong 24h: `aws cloudwatch get-metric-statistics --namespace KiteHub/EC2/kc-app --metric-name CertDaysToExpire --start-time $(date -u -d '24 hours ago' +%FT%TZ) --end-time $(date -u +%FT%TZ) --period 86400 --statistics Maximum`
- [ ] CloudWatch alarm `kitehub-kc-app-cert-expiry` exists trong state `OK` post-deploy (cert >30d sau initial issuance)
- [ ] Alarm fires test: push value 29 → alarm transition to ALARM trong ~5min → reset OK
- [ ] Cross-link `pre-launch-secrets-hardening-checklist.md` §2.5 rotation runbook discipline

## Related

- Wave plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §1 Brainstorm Q3 + §3 Bucket B
- Failure-matrix finding: **F10** Wave 82 Bucket A outside-in audit 2026-05-15
- Sister gaps: GAP-565 (F6 SG) · GAP-566 (F7 RAM) · GAP-568 (F11 CORS)
- Rules: `.claude/rules/pre-launch-secrets-hardening-checklist.md` §2.5 (rotation runbook pattern)
- Cloudflare DNS reference: Wave 81 closure CF DNS active for `kitehub.me` zone (per ROADMAP snapshot)
- AWS Secrets Manager pattern: similar to JWT/TOTP fail-fast guards Wave 81 Bucket F

## Log

- **2026-05-15:** Gap filed via Wave 82 Bucket A outside-in failure-mode matrix audit (finding F10). P0 BLOCKING — phải address trong Bucket B EC2 provisioning. DNS-01 challenge eliminate race với nginx port 80 hold; CloudWatch metric + alarm provide 30d buffer trước cert expire. Cốt lõi: 90d Let's Encrypt expire → silent fail = 100% FE outage nếu không có monitoring layer.
