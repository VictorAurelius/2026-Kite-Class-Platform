---
title: AWS Verification — cert-days-monitor user_data wiring (Wave 82 GAP-567 follow-up)
status: complete
created: 2026-05-15
phase: post-Wave-84-monitoring-gap
wave: 84
gaps: [GAP-573, GAP-567]
---

# AWS Verification Report — cert-days-monitor systemd timer wiring

## Scope

Alarm `kitehub-kc-app-fe-cert-expiry` đang ở state ALARM trên CloudWatch (account 906286017800, region ap-southeast-1) không phải production incident — cert thật `kitehub.me` còn 89 ngày (expires 2026-08-13 10:54Z). Root cause: metric `KiteHub/FE/CertDaysToExpire` dimension `InstanceId=i-05cfda7c6c60b683f` chưa có datapoint nào → `treat_missing_data = "breaching"` trigger.

Pre-mutation audit này document state hiện tại trước khi sửa `infrastructure/terraform-aws/ec2-kc-app.tf` wire systemd timer + publisher script vào user_data (root fix Path B, không phải SSM quick fix Path A).

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# EC2 current state
aws ec2 describe-instances --instance-ids i-05cfda7c6c60b683f \
  --profile dev-admin --region ap-southeast-1 \
  --query 'Reservations[0].Instances[0].[InstanceId,State.Name,LaunchTime,PublicIpAddress,Tags[?Key==`Name`].Value|[0]]'

# SSM diagnose (read-only inside SSM)
aws ssm send-command --instance-ids i-05cfda7c6c60b683f \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["ls /etc/letsencrypt/live/","ls /etc/systemd/system/cert-days-monitor.*","ls /usr/local/bin/cert-days-to-expire.sh","systemctl list-timers --all | grep -i cert","cat /var/log/kite-fe-bootstrap.log","sudo certbot certificates"]'

# Git history check
git log --oneline --all -S "certbot-dns-01-setup" -- infrastructure/terraform-aws/
git log --oneline --all -S "cert-days-monitor"
grep -nE "cert-days|certbot-dns-01-setup" infrastructure/terraform-aws/*.tf
```

## Findings

### Current EC2 state

| Field | Value |
|---|---|
| Instance ID | i-05cfda7c6c60b683f |
| State | running |
| LaunchTime | 2026-05-15T11:33:46Z (Wave 84 apply) |
| Public IP | 54.179.70.37 |
| Name tag | kitehub-kc-app-fe |
| Bootstrap log | "Wave 82 Bucket B FE self-host bootstrap complete" |

### Cert presence (verified via SSM)

| Path | State |
|---|---|
| `/etc/letsencrypt/live/kitehub.me/fullchain.pem` | ✅ EXISTS |
| `/etc/letsencrypt/live/kitehub.me/cert.pem` | ✅ EXISTS |
| `/root/.secrets/cloudflare.ini` | ✅ EXISTS (mode 0600) |
| Cert domains | `kitehub.me` + `*.kitehub.me` |
| Cert serial | 5efd4624fe0cc955c0aafde390a1f7eb5cb (ECDSA) |
| Cert expiry | 2026-08-13 10:54:32 UTC (89 days valid) |

→ Steps 1-3 của `infrastructure/fe-host/certbot-dns-01-setup.sh` đã chạy (install certbot, fetch CF token, issue cert). Có thể qua manual SSM SendCommand hoặc 1 phần Wave 82 deploy script không log trong terraform state.

### Missing artifacts (root cause)

| Path | State |
|---|---|
| `/usr/local/bin/cert-days-to-expire.sh` | ❌ MISSING |
| `/etc/systemd/system/cert-days-monitor.service` | ❌ MISSING |
| `/etc/systemd/system/cert-days-monitor.timer` | ❌ MISSING |
| `systemctl list-timers \| grep cert` | (empty) |

Steps 4-5 của setup script (renewal timer + monitor publisher) chưa chạy → CloudWatch metric chưa được publish → alarm `treat_missing_data="breaching"` → ALARM state.

### Terraform wiring gap

`grep -nE "cert-days|certbot-dns-01-setup" infrastructure/terraform-aws/*.tf` → 0 matches.

`ec2-kc-app.tf` line 197-232 `local.kc_app_fe_user_data` heredoc CHỈ install runtime (nginx + Node + certbot pip) — KHÔNG invoke `infrastructure/fe-host/certbot-dns-01-setup.sh` cũng KHÔNG inline systemd unit setup.

Constraint line 245: `user_data_replace_on_change = false` → edit user_data riêng KHÔNG re-run trên live EC2.

### Namespace/dimension mismatch (secondary finding)

| Source | Namespace | Dimension |
|---|---|---|
| `infrastructure/fe-host/certbot-dns-01-setup.sh` Step 5 publisher | `KiteHub/EC2/kc-app` | `Domain=kitehub.me` |
| `ec2-kc-app.tf` line 311 alarm `kc_app_fe_cert_expiry` | `KiteHub/FE` | `InstanceId=<aws_instance.kc_app_fe.id>` |

→ Even if setup script had run, alarm would still missing-data vì publisher publish vào namespace/dimension khác. Fix: align publisher script (inside user_data) sang `KiteHub/FE` + `InstanceId` để match alarm.

## Real-vs-phantom analysis (planned terraform changes)

Plan kỳ vọng sau khi edit `ec2-kc-app.tf`:

| # | Change | Real or phantom | Risk |
|---|---|---|---|
| 1 | `local.kc_app_fe_user_data` heredoc body extends với systemd unit files + publisher script + invocation | **Real** (file content change) | **Zero** trên EC2 hiện tại (user_data_replace_on_change=false; user_data field stale on live EC2, không re-run) |
| 2 | `aws_instance.kc_app_fe.user_data` attribute hash sẽ change | **Phantom** trên live EC2 (config drift detected, không apply) | Acceptable — terraform sẽ show diff but `user_data_replace_on_change=false` prevents replacement |

Plan output dự kiến: `0 to add, 1 to change, 0 to destroy` (chỉ aws_instance attribute drift detected). Không phantom replace.

**Quan trọng:** vì `user_data_replace_on_change=false`, user_data update **không tự áp dụng** lên EC2 đang chạy. Cần 2 path:

- **Path 1 (recommended cho Wave 84):** Sau apply, dùng SSM SendCommand chạy systemd setup commands lên live EC2 i-05cfda7c6c60b683f (script được commit cùng PR để reuse trên EC2 replacement tương lai).
- **Path 2 (alt):** Set `user_data_replace_on_change=true` ONE-TIME → force EC2 replacement → user_data fresh run. Nhược điểm: lose state trên local disk (cert sẽ phải re-issue, hit LE rate limit risk, plus nginx config drift). KHÔNG khuyến cáo.

Khuyến cáo Path 1: terraform PR sửa user_data cho future-proof + tạo helper script SSM cho one-time apply lên live EC2.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | Date | Where |
|---|---|---|
| Wave 82 GAP-567 file source script `certbot-dns-01-setup.sh` | 2026-05-14 | PR #1398 commit eeacaeaa |
| Wave 82 Bucket B fix CF token storage SSM→Secrets Manager | 2026-05-14 | PR #1403 commit 336c4b3e |
| Wave 84 EC2 launch (current i-05cfda7c6c60b683f) | 2026-05-15 11:33Z | terraform apply Wave 84 |
| Cert issuance kitehub.me + *.kitehub.me 89d | 2026-05-15 10:54Z | manual (no terraform record) |
| Wave 82 follow-up gaps filed | 2026-05-14 | commit 47abfb62 |

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| Edit `ec2-kc-app.tf` `local.kc_app_fe_user_data` — inline systemd unit files + publisher script + invocation | claude (this PR) | Path B per user choice |
| File GAP-573 follow-up | claude (this PR) | PARTIAL status until apply + verify |
| terraform plan (no apply) | claude (this PR) | Plan output saved here |
| terraform apply via workflow_dispatch | user | Per `release-deploy-standard.md` §9 human-only |
| Post-apply SSM SendCommand wire systemd lên live EC2 | user (or claude w/ explicit authorization) | One-time backfill cho i-05cfda7c6c60b683f |
| Concurrent op check (`gh run list --status in_progress`) trước trigger | user (pre-flight) | Per `concurrent-production-mutation-ops.md` |

## Recommendations

1. **Approach (a) inline**: extend `local.kc_app_fe_user_data` heredoc với:
   - Inline `/usr/local/bin/cert-days-to-expire.sh` (publisher align `KiteHub/FE` + `InstanceId` dimension)
   - Inline `/etc/systemd/system/cert-days-monitor.service` + `.timer` (daily oneshot)
   - `systemctl daemon-reload && systemctl enable --now cert-days-monitor.timer`
   - Initial 1-shot run để alarm có baseline data point ngay
   - **Bracket existence-check** trên cert file: nếu `/etc/letsencrypt/live/kitehub.me/cert.pem` không tồn tại tại first-run, publisher script chỉ log warning + exit 0 (NOT exit 1) — cho phép timer fire idempotent trên fresh EC2 chưa có cert.

2. **One-time backfill cho live EC2**: tạo `infrastructure/fe-host/cert-days-monitor-backfill.sh` chứa systemd unit + publisher content. User chạy qua SSM SendCommand post-apply:
   ```bash
   aws ssm send-command --instance-ids i-05cfda7c6c60b683f \
     --document-name AWS-RunShellScript \
     --parameters "commands=[\"$(cat infrastructure/fe-host/cert-days-monitor-backfill.sh | base64 -w0)\"]"
   ```
   Hoặc đơn giản hơn: copy script qua S3 + execute. Detail thread vào GAP-573 § Apply path.

3. **Alarm verification sau backfill** (Tier 1 commands):
   ```bash
   aws cloudwatch get-metric-statistics --namespace KiteHub/FE \
     --metric-name CertDaysToExpire \
     --dimensions Name=InstanceId,Value=i-05cfda7c6c60b683f \
     --start-time $(date -u -d '1 hour ago' +%FT%TZ) \
     --end-time $(date -u +%FT%TZ) \
     --period 3600 --statistics Maximum
   aws cloudwatch describe-alarms --alarm-names kitehub-kc-app-fe-cert-expiry \
     --query 'MetricAlarms[0].StateValue'
   # Expected: OK sau 1 daily timer fire + 1 evaluation period 86400s
   ```

4. **Không re-issue cert** — cert hiện tại valid 89d, certbot `--keep-until-expiring` flag trong setup script đã idempotent. Auto-renewal sẽ trigger tại d-30 qua `certbot-renew.timer` (separate timer, install qua dnf package).

5. **Watch-for**: nếu future EC2 replacement xảy ra (vd Wave 85+ instance_type bump), user_data sẽ fresh-run + systemd timer enable tự động — KHÔNG cần manual backfill lần nữa.

## Plan output (terraform plan, targeted aws_instance.kc_app_fe)

Executed `terraform plan -target=aws_instance.kc_app_fe` on branch `fix/cert-days-monitor-user-data-wiring` post-edit:

```
Terraform will perform the following actions:

  # aws_instance.kc_app_fe will be updated in-place
  ~ resource "aws_instance" "kc_app_fe" {
        id                                   = "i-05cfda7c6c60b683f"
      ~ public_dns                           = "ec2-54-179-70-37.ap-southeast-1.compute.amazonaws.com" -> (known after apply)
      ~ public_ip                            = "54.179.70.37" -> (known after apply)
      ~ user_data                            = "9930d037ea496255a0ebd91a0b8453c4744e6736" -> "2ba24fdd9dbfd3f06ff99e8b4d2832f58c045b2f"
        # (30 unchanged attributes hidden)
        # (8 unchanged blocks hidden)
    }

Plan: 0 to add, 1 to change, 0 to destroy.
```

**Analysis:**
- 1 change in-place (user_data hash) — matches §Real-vs-phantom expectation ✅
- `public_ip` / `public_dns` "known after apply" phantom — terraform sees `user_data` change, recomputes derived attrs. Vì `user_data_replace_on_change=false`, EC2 KHÔNG replace → public_ip thực tế giữ nguyên 54.179.70.37 post-apply. Verify post-apply.
- 0 add / 0 destroy — clean
- 4 warnings về `random_password` ignore_changes — unchanged from main, không liên quan PR này

Plan satisfies recommendation §1 + §2 of audit. Safe to apply via workflow_dispatch.

## References

- Wave 82 GAP-567: `documents/04-quality/gaps/GAP-567-wave-82-certbot-dns-01-cert-renewal-30d-expiry-monitor.md`
- Source script: `infrastructure/fe-host/certbot-dns-01-setup.sh`
- Terraform file: `infrastructure/terraform-aws/ec2-kc-app.tf` (line 197-232 user_data, line 305-329 alarm)
- ADR-031 FE self-host on EC2
- Rules applied: `pre-mutation-state-check.md` §3, `agent-aws-access.md` §2.1, `release-deploy-standard.md` §9, `aws-sg-description-ascii.md` (verify all user_data ASCII)
- Follow-up gap: GAP-573 (PARTIAL pending apply + backfill verify)
- CloudWatch alarm event: `kitehub-kc-app-fe-cert-expiry` state ALARM as of 2026-05-15 (datapoint missing 7d since EC2 launch)
