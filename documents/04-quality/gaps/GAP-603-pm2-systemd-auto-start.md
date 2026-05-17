# GAP-603: PM2 systemd auto-start on EC2 reboot chưa wired

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-05-17 (Wave 88 Bucket D FE deploy)
**Affects:** kc-app-fe EC2 — PM2 processes restart sau reboot/maintenance

## Problem

PM2 đang chạy `kitehub-frontend` + `kiteclass-frontend` qua manual `pm2 start` (Wave 88 Bucket D). `pm2 save` đã chạy nhưng `pm2 startup systemd` CHƯA execute → reboot kc-app-fe → PM2 daemon không tự khởi động → cả 2 FE app chết → nginx 502.

EC2 user_data có install pm2 nhưng KHÔNG wire `pm2 startup systemd -u ec2-user` để generate systemd service unit + persist process list trigger on boot.

## Root Cause

Wave 82 Bucket B user_data script ghi:
```bash
npm install -g pm2 pnpm
```
Nhưng skip step `pm2 startup systemd -u ec2-user` (generate /etc/systemd/system/pm2-ec2-user.service) + `pm2 save`. Wave 82 Bucket C runbook §4.2 mention nhưng không có instance step automation.

Verify hiện tại:
```bash
$ systemctl list-unit-files | grep pm2
# Expected: pm2-ec2-user.service enabled
# Actual: empty (no pm2 systemd unit)
```

## Proposed Fix

Extend `ec2-kc-app.tf` user_data block:

```bash
# (Add after npm install -g pm2 pnpm)
sudo -u ec2-user pm2 startup systemd -u ec2-user --hp /home/ec2-user 2>&1 | grep "sudo " | sh
# pm2 startup generates command requiring sudo → pipe to sh executes it
# Subsequent `pm2 save` (after deploy) persists process list
```

Wave 88 manual hotfix on existing EC2:
```bash
aws ssm send-command --instance-ids i-05cfda7c6c60b683f --document-name AWS-RunShellScript --parameters 'commands=[
  "sudo -u ec2-user pm2 startup systemd -u ec2-user --hp /home/ec2-user | grep \"sudo \" | sh",
  "sudo -u ec2-user pm2 save"
]'
```

## Acceptance Criteria

- [ ] `pm2 startup systemd` executed trên kc-app-fe — systemd unit `pm2-ec2-user.service` exists + enabled
- [ ] `pm2 save` persist current process list `/home/ec2-user/.pm2/dump.pm2`
- [ ] Test reboot EC2 → `pm2 list` shows both apps online ≤30s post-boot
- [ ] `kitehub.me/` + `app.kitehub.me/` smoke ≤60s post-boot
- [ ] Update `ec2-kc-app.tf` user_data — future EC2 replacement auto-wires PM2 systemd
- [ ] Document in `documents/05-guides/deploy/fe-self-host-runbook.md` §4.2

## Related

- Wave 88 closure: `documents/04-quality/audits/aws-verification/2026-05-17-wave-88-cutover-post-apply.md` §8
- GAP-602 PM2 ecosystem path (companion — fix both before reboot test)
- `infrastructure/terraform-aws/ec2-kc-app.tf` user_data block
- `infrastructure/fe-host/pm2-ecosystem.config.js`

## Log

- **2026-05-17:** Gap filed during Wave 88 closure. Manual PM2 process list saved via `pm2 save` but no systemd resurrect → reboot will lose state.
