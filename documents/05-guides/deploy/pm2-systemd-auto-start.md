# PM2 systemd Auto-Start Runbook — Wave 89 Bucket B (GAP-602 + GAP-603)

**Phase:** Phase 1 BETA FE self-host
**Scope:** Cấu hình PM2 trên kc-app-fe EC2 để auto-restart cả 2 Next.js standalone app sau reboot/maintenance
**Status:** Active (Wave 89 ship; live verify chờ user reboot test trong maintenance window)
**Related:** GAP-602 (cwd path mismatch), GAP-603 (systemd auto-start), Wave 82 FE self-host parent, Wave 88 cutover audit

---

## 1. Purpose

Sau khi PM2 daemon được wire vào systemd qua `pm2 startup systemd`, mỗi lần kc-app-fe EC2 reboot/maintenance, systemd sẽ tự động:
1. Start `pm2-ec2-user.service`
2. Resurrect process list từ `/home/ec2-user/.pm2/dump.pm2` (file generated bởi `pm2 save`)
3. Re-launch cả `kitehub-frontend` (port 4701) + `kiteclass-frontend` (port 4700) theo cấu hình ecosystem

KHÔNG cần SSH thủ công chạy `pm2 start` mỗi lần reboot.

GAP-602 fix paired song song: ecosystem `cwd` trỏ đúng monorepo nested path để PM2 start không lỗi "ENOENT server.js".

---

## 2. Directory map

```
infrastructure/
├── fe-host/
│   └── pm2-ecosystem.config.js          # GAP-602 fix — cwd monorepo path
└── terraform-aws/
    └── ec2-kc-app.tf                    # GAP-603 user_data wire pm2 startup systemd

scripts/
└── deploy-fe.sh                         # Defensive PM2 invocation + pre-flight systemd check

documents/05-guides/deploy/
├── pm2-systemd-auto-start.md            # ← this runbook
└── fe-self-host-runbook.md              # Wave 82 parent (full FE self-host architecture)
```

---

## 3. File placement (per `docs-folder-structure.md` §3)

| File | Location | Why |
|---|---|---|
| Runbook (this file) | `documents/05-guides/deploy/` | One-time per release cluster (Wave 89 ship), pre-deploy operation per `deployment-naming-convention.md` §2 |
| Bash deploy script | `scripts/deploy-fe.sh` | Repository-root scripts/ per existing pattern (`deploy-prod.sh`, `deploy-kc.sh`) |
| Terraform user_data block | inline trong `ec2-kc-app.tf` | Per `aws-observability-first.md` + Wave 82 pattern — user_data co-locate với resource |
| PM2 ecosystem config | `infrastructure/fe-host/pm2-ecosystem.config.js` | Wave 82 Bucket B canonical location |

---

## 4. Archive policy

Runbook này stable cho tới khi:
- Phase 2 K8s cutover (replace PM2 với Deployment + Service)
- Hoặc EC2 self-host bị deprecated (vd switch sang ECS Fargate)

Khi trigger → move file sang `documents/07-archived/deploy-pm2-systemd-YYYY/`.

---

## 5. Execution flow (lần đầu / sau EC2 replace)

### 5.1 Pre-condition

- EC2 kc-app-fe đã apply Wave 89 ec2-kc-app.tf (user_data có pm2 startup systemd block)
- `infrastructure/fe-host/pm2-ecosystem.config.js` đã copy sang `/var/www/pm2-ecosystem.config.js`
- Next.js standalone artifacts đã rsync sang `/var/www/{kitehub,kiteclass}-frontend/`

### 5.2 Verify systemd unit installed (post terraform apply)

```bash
# Via SSM SendCommand từ local:
aws ssm send-command \
  --instance-ids <kc-app-fe-id> \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["systemctl list-unit-files pm2-ec2-user.service"]' \
  --region ap-southeast-1
```

Expected: `pm2-ec2-user.service    enabled`.

Nếu `disabled` hoặc absent → re-run user_data segment thủ công (idempotent):

```bash
sudo -u ec2-user pm2 startup systemd -u ec2-user --hp /home/ec2-user \
  2>&1 | grep -E '^sudo ' | sh
```

### 5.3 Start PM2 lần đầu + save

```bash
sudo -u ec2-user bash /opt/kite-fe/scripts/deploy-fe.sh start
```

Script sẽ:
1. Verify `/var/www/pm2-ecosystem.config.js` tồn tại
2. Verify `pm2-ec2-user.service` đã installed (WARN nếu thiếu, không hard-fail)
3. `pm2 start /var/www/pm2-ecosystem.config.js --update-env` (explicit absolute path = GAP-602 defensive)
4. `pm2 save` — generate `/home/ec2-user/.pm2/dump.pm2` để systemd resurrect

### 5.4 Verify both apps online

```bash
sudo -u ec2-user bash /opt/kite-fe/scripts/deploy-fe.sh status
```

Expected output:
```
┌────┬───────────────────────┬─────────┬─────────┐
│ id │ name                  │ status  │ ↺       │
├────┼───────────────────────┼─────────┼─────────┤
│ 0  │ kitehub-frontend      │ online  │ 0       │
│ 1  │ kiteclass-frontend    │ online  │ 0       │
└────┴───────────────────────┴─────────┴─────────┘

● pm2-ec2-user.service - PM2 process manager
   Loaded: loaded (/etc/systemd/system/pm2-ec2-user.service; enabled; vendor preset: disabled)
   Active: active (running)
```

---

## 6. Live verify (post-reboot test) — DEFERRED tới user maintenance window

Per `gap-done-discipline.md` §3 PARTIAL exit ramp — Wave 89 ship rule + code, live reboot test deferred vì:
- Yêu cầu user trigger `terraform-apply.yml` (terraform user_data change = EC2 replace cycle)
- Yêu cầu maintenance window (downtime ~2-3 phút khi reboot + PM2 resurrect)

### 6.1 Live test procedure (when user ready)

```bash
# 1. Snapshot pre-reboot state
aws ssm send-command --instance-ids <kc-app-fe-id> \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["pm2 list", "curl -sI http://127.0.0.1:4701", "curl -sI http://127.0.0.1:4700"]' \
  --region ap-southeast-1

# 2. Reboot
aws ec2 reboot-instances --instance-ids <kc-app-fe-id> --region ap-southeast-1

# 3. Wait ~90s cho boot + PM2 resurrect
sleep 90

# 4. Verify both apps online without manual intervention
aws ssm send-command --instance-ids <kc-app-fe-id> \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["systemctl status pm2-ec2-user.service --no-pager", "pm2 list"]' \
  --region ap-southeast-1

# 5. Smoke test public endpoint
curl -sI https://kitehub.me/    # expect 200
curl -sI https://app.kitehub.me/ # expect 200 (kiteclass)
```

### 6.2 Acceptance criteria (live verify)

- [ ] `pm2-ec2-user.service` `Active: active (running)` sau reboot
- [ ] `pm2 list` shows cả 2 apps `online` ≤30s post-boot
- [ ] `kitehub.me/` + `app.kitehub.me/` 200 ≤60s post-boot
- [ ] KHÔNG cần SSM manual `pm2 start`

---

## 7. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `pm2 list` empty sau reboot | `pm2 save` chưa chạy trước reboot | Re-start apps + `pm2 save` |
| `pm2-ec2-user.service` `inactive (dead)` | systemd unit chưa enable | `sudo systemctl enable --now pm2-ec2-user.service` |
| `pm2 start` fail `ENOENT server.js` | cwd path mismatch (GAP-602 regression) | Verify `pm2-ecosystem.config.js` cwd = `/var/www/<app>/<workspace>/<app>` |
| nginx 502 sau reboot | PM2 daemon up nhưng app crash loop | `pm2 logs --lines 50` để debug; check `NODE_OPTIONS --max-old-space-size` + RAM |
| `systemctl list-unit-files pm2-ec2-user.service` returns nothing | user_data segment chưa execute (EC2 mới hoặc user_data skip) | Re-run thủ công per §5.2 |

---

## 8. Cross-references

- **`infrastructure/fe-host/pm2-ecosystem.config.js`** — canonical PM2 config (GAP-602 cwd fix)
- **`infrastructure/terraform-aws/ec2-kc-app.tf`** — user_data block wire systemd (GAP-603)
- **`scripts/deploy-fe.sh`** — defensive deploy script với explicit ecosystem absolute path
- **`documents/05-guides/deploy/fe-self-host-runbook.md`** — Wave 82 parent (full FE self-host architecture)
- **`documents/04-quality/audits/aws-verification/2026-05-17-wave-88-cutover-post-apply.md`** — production state context Wave 88
- **`.claude/rules/release-deploy-standard.md`** §9 — agent role matrix; reboot test = human-only per Tier 3
- **`.claude/rules/concurrent-production-mutation-ops.md`** — terraform user_data change = EC2 replace, serialize với deploy
- **`.claude/rules/gap-done-discipline.md`** §3 — live verify deferred = PARTIAL exit ramp acceptable
