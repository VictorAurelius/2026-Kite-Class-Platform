# GAP-573 — cert-days-monitor systemd timer + publisher script chưa wired vào EC2 user_data (Wave 82 GAP-567 follow-up)

**Status:** PARTIAL
**Priority:** P2
**Domain:** DevOps
**Phase:** phase-1-beta
**Completion:** 50%
**Found-date:** 2026-05-15
**Last-verified:** 2026-05-15
**Source:** CloudWatch alarm `kitehub-kc-app-fe-cert-expiry` state ALARM event 2026-05-15

---

## 1. Problem

CloudWatch alarm `kitehub-kc-app-fe-cert-expiry` đang ở state **ALARM** trên account 906286017800 ap-southeast-1, NHƯNG đây KHÔNG phải production incident:

- Cert thật trên `kitehub.me` + `*.kitehub.me` healthy: `notAfter=2026-08-13 10:54:32 UTC` (89 ngày remaining)
- Cert được issued thành công qua Cloudflare DNS-01 challenge (verified qua SSM `certbot certificates`)

Alarm trigger reason: metric `KiteHub/FE/CertDaysToExpire` dimension `InstanceId=i-05cfda7c6c60b683f` KHÔNG có datapoint trong 7 ngày từ Wave 84 EC2 launch (2026-05-15 11:33Z) → terraform alarm config `treat_missing_data = "breaching"` (line 323 `ec2-kc-app.tf`) → ALARM.

**Root cause:** Wave 82 GAP-567 ship script `infrastructure/fe-host/certbot-dns-01-setup.sh` (Steps 1-6 setup cert + systemd timer + publisher) NHƯNG terraform `ec2-kc-app.tf` user_data CHỈ chạy `pip3 install certbot certbot-dns-cloudflare` — **KHÔNG invoke setup script**. Hệ quả:

- Steps 1-3 (install + fetch CF token + issue cert) đã chạy qua manual SSM hoặc bootstrap khác → cert đã có
- Steps 4-5 (renewal timer + cert-days-monitor systemd timer + publisher) **chưa chạy** → metric KHÔNG được publish → alarm missing-data

**Secondary finding:** Setup script publisher (Step 5) publish vào namespace `KiteHub/EC2/kc-app` dimension `Domain=kitehub.me`, NHƯNG terraform alarm watch namespace `KiteHub/FE` dimension `InstanceId=<id>`. Mismatch — kể cả nếu setup script đã chạy, alarm vẫn missing data.

---

## 2. Acceptance Criteria

- [x] **AC1**: Pre-mutation audit artifact tạo trước mọi terraform change (`documents/04-quality/audits/aws-verification/2026-05-15-cert-days-monitor-user-data-wiring.md`)
- [x] **AC2**: `ec2-kc-app.tf` `local.kc_app_fe_user_data` extended với inline systemd setup (service + timer + publisher script). Publisher align namespace `KiteHub/FE` + dimension `InstanceId=<self>` match terraform alarm.
- [x] **AC3**: Publisher tolerant pre-issuance window — exit 0 + WARN log nếu `/etc/letsencrypt/live/kitehub.me/cert.pem` chưa có (timer KHÔNG fail-loop)
- [x] **AC4**: Backfill script `infrastructure/fe-host/cert-days-monitor-backfill.sh` ship trong cùng PR — wire systemd lên LIVE EC2 i-05cfda7c6c60b683f (vì `user_data_replace_on_change=false`)
- [x] **AC5**: `terraform plan` clean — `0 to add, 1 to change, 0 to destroy` (chỉ aws_instance user_data hash drift)
- [ ] **AC6**: User trigger `terraform-apply.yml` workflow_dispatch (human-only per `release-deploy-standard.md` §9)
- [ ] **AC7**: User chạy backfill script qua SSM SendCommand lên i-05cfda7c6c60b683f
- [ ] **AC8**: Verify post-backfill: `systemctl is-active cert-days-monitor.timer` = `active` + CloudWatch metric `KiteHub/FE/CertDaysToExpire` có datapoint
- [ ] **AC9**: Verify alarm flip OK: `aws cloudwatch describe-alarms --alarm-names kitehub-kc-app-fe-cert-expiry --query 'MetricAlarms[0].StateValue'` = `OK` (sau 1 evaluation period 86400s)

→ Status flip `🟢 DONE` khi AC6-9 hoàn thành. Hiện tại PARTIAL (AC1-5 done, AC6-9 pending user action).

---

## 3. Proposed Fix

**Approach (a) inline systemd setup vào terraform user_data** — đơn giản, không cần SSM document hay null_resource, idempotent cho EC2 replacement tương lai.

### Files changed

1. **`infrastructure/terraform-aws/ec2-kc-app.tf`** — extend `local.kc_app_fe_user_data` heredoc với:
   - Inline `/usr/local/bin/cert-days-to-expire.sh` (publisher, namespace KiteHub/FE + dimension InstanceId qua IMDSv2)
   - Inline `/etc/systemd/system/cert-days-monitor.service` (Type=oneshot)
   - Inline `/etc/systemd/system/cert-days-monitor.timer` (OnCalendar=daily, RandomizedDelaySec=300)
   - `systemctl daemon-reload && systemctl enable --now cert-days-monitor.timer`
   - First-run initial push để alarm có baseline data point ngay (avoid 24h wait)
   - Publisher tolerant `cert.pem` chưa có (exit 0 + WARN — quan trọng cho pre-issuance window)

2. **`infrastructure/fe-host/cert-days-monitor-backfill.sh`** (new file) — content giống user_data inline, dùng cho one-time apply lên live EC2 i-05cfda7c6c60b683f qua SSM SendCommand (vì `user_data_replace_on_change=false`).

### Apply path

```bash
# Step 1: User trigger workflow per dev-authorized-terraform-trigger.md
gh workflow run terraform-apply.yml --ref fix/cert-days-monitor-user-data-wiring \
  -f confirm=APPLY -f dry_run=true -f version=fix/cert-days-monitor-user-data-wiring
# Verify plan matches audit artifact, then dry_run=false

# Step 2 (sau apply): Backfill live EC2 qua SSM
SCRIPT_B64=$(base64 -w0 infrastructure/fe-host/cert-days-monitor-backfill.sh)
aws ssm send-command \
  --instance-ids i-05cfda7c6c60b683f \
  --document-name AWS-RunShellScript \
  --parameters "commands=[\"echo $SCRIPT_B64 | base64 -d | sudo bash\"]" \
  --profile dev-admin --region ap-southeast-1

# Step 3: Verify (Tier 1 read-only)
aws cloudwatch get-metric-statistics --namespace KiteHub/FE \
  --metric-name CertDaysToExpire \
  --dimensions Name=InstanceId,Value=i-05cfda7c6c60b683f \
  --start-time $(date -u -d '1 hour ago' +%FT%TZ) \
  --end-time $(date -u +%FT%TZ) \
  --period 3600 --statistics Maximum \
  --profile dev-admin --region ap-southeast-1
# Expected: 1+ datapoint với Maximum ~89 (cert days remaining)

aws cloudwatch describe-alarms --alarm-names kitehub-kc-app-fe-cert-expiry \
  --query 'MetricAlarms[0].StateValue' \
  --profile dev-admin --region ap-southeast-1
# Expected: OK (sau 1 evaluation period — alarm period 86400s, có thể đợi 1 ngày)
```

### Trade-offs considered

| Approach | Pros | Cons | Choice |
|---|---|---|---|
| (a) Inline user_data | Simple, idempotent future EC2 replace, no SSM doc | Doesn't auto-apply live EC2 (cần backfill script) | ✅ chosen |
| (b) null_resource + SSM SendCommand | Auto-applies live EC2 | Adds complexity, null_resource state tricky, depends on aws_ssm provider | Rejected — backfill script đơn giản hơn |
| (c) `user_data_replace_on_change=true` ONE-TIME | Force re-run user_data | Replace EC2 → lose disk state (cert re-issue risk hit LE rate limit, nginx config drift) | Rejected — high risk |

---

## 4. Cross-references

- **Wave 82 GAP-567**: `documents/04-quality/gaps/GAP-567-wave-82-certbot-dns-01-cert-renewal-30d-expiry-monitor.md` — source script created NHƯNG terraform wiring incomplete
- **Audit artifact**: `documents/04-quality/audits/aws-verification/2026-05-15-cert-days-monitor-user-data-wiring.md` — full state + plan output
- **Setup source**: `infrastructure/fe-host/certbot-dns-01-setup.sh`
- **Terraform**: `infrastructure/terraform-aws/ec2-kc-app.tf` (line 197 user_data, line 305 alarm)
- **Backfill script**: `infrastructure/fe-host/cert-days-monitor-backfill.sh`
- **Rules applied**: `pre-mutation-state-check.md` §3, `agent-aws-access.md` §2.1, `release-deploy-standard.md` §9, `dev-authorized-terraform-trigger.md`
- **CloudWatch alarm**: `kitehub-kc-app-fe-cert-expiry` (resource `aws_cloudwatch_metric_alarm.kc_app_fe_cert_expiry`)

---

## 5. Log

- **2026-05-15** (Status: PARTIAL 50%): Gap filed. Pre-mutation audit + terraform user_data edit + backfill script all shipped same PR `fix/cert-days-monitor-user-data-wiring`. Terraform plan clean (0 add / 1 change / 0 destroy — user_data hash only, `user_data_replace_on_change=false` prevents EC2 replacement). AC1-5 DONE. AC6-9 pending user trigger workflow + post-apply SSM backfill + verify alarm OK. Reviewer: @nguyenvankiet (solo-dev). Cross-references Wave 82 GAP-567 unfinished wiring + alarm event 2026-05-15.
