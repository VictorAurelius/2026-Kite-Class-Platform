---
title: "GAP-574: pm2-ecosystem.config.js có 3 bugs surface khi deploy"
status: OPEN
priority: P1
domain: DevOps
phase: phase-1-beta
wave: 82-bucket-c-followup
created: 2026-05-15
---

# GAP-574: pm2-ecosystem.config.js có 3 bugs cần fix

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (future deploys sẽ fail; current hot-fix on EC2 manually applied)
**Domain:** DevOps / FE deploy
**Found:** 2026-05-15 Wave 82 Bucket C FE deploy via SSM SendCommand
**Affects:** Mọi future Wave deploy dùng `infrastructure/fe-host/pm2-ecosystem.config.js` từ repo

## Problem

Wave 82 Bucket C deploy đã hot-fix manual trên EC2 i-05cfda7c6c60b683f (PM2 running kitehub-frontend at 122.9MB), nhưng repo source vẫn có 3 bugs sẽ cause future deploy fail nếu dev không nhớ apply hot-fix:

### Bug 1: `max_memory_restart: '1.2G'` invalid PM2 format

PM2 error message:
```
[PM2][WARN] Verify "max_memory_restart" with regex failed, it should be a NUMBER -
byte, "[NUMBER]G"(Gigabyte), "[NUMBER]M"(Megabyte) or "[NUMBER]K"(Kilobyte)
```

PM2 expects integer with unit suffix: `1G`, `1200M`, `1024K`. Decimal `1.2G` không valid.

### Bug 2: `cwd: '/var/www/kitehub-frontend/.next/standalone'` wrong cho monorepo

Next.js standalone output cho pnpm workspace package nests theo workspace path:
- Actual: `/var/www/kitehub-frontend/kitehub/kitehub-frontend/server.js`
- Config assumes: `/var/www/kitehub-frontend/.next/standalone/server.js`

PM2 error:
```
[PM2][ERROR] Error: Script not found: /var/www/kitehub-frontend/.next/standalone/server.js
```

### Bug 3: `/var/log/pm2/` directory không exist + ec2-user không có permission tạo

PM2 config có `error_file` / `out_file` pointing to `/var/log/pm2/...`. Khi ec2-user (non-root) `pm2 start` lần đầu, PM2 cố tạo `/var/log/pm2/` → permission denied.

PM2 error:
```
[PM2][WARN] Folder does not exist: /var/log/pm2
[PM2] Creating folder: /var/log/pm2
[PM2][ERROR] Could not create folder: /var/log/pm2
```

## Root Cause

Agent 2 (Wave 82 Bucket B drafts agent) viết `pm2-ecosystem.config.js` based on PM2 docs assumption + Next standalone cho single-app (không monorepo). Cả 3 bugs surface chỉ khi actually deploy on real EC2 (CI mvn verify không catch — config file là JavaScript, parsed by node but PM2-specific schema chỉ verify at runtime).

## Proposed Fix

Edit `infrastructure/fe-host/pm2-ecosystem.config.js`:

```javascript
// Fix Bug 1: '1.2G' → '1200M' (integer + unit, no decimal)
max_memory_restart: '1200M',

// Fix Bug 2: cwd path for monorepo standalone output
// Old: cwd: '/var/www/kitehub-frontend/.next/standalone'
// New (matches actual rsync target structure):
cwd: '/var/www/kitehub-frontend/kitehub/kitehub-frontend',
```

Fix Bug 3 trong setup runbook OR via terraform user_data — pre-create `/var/log/pm2/` with `ec2-user` owner:

```bash
# Add to ec2-kc-app.tf user_data heredoc:
mkdir -p /var/log/pm2
chown ec2-user:ec2-user /var/log/pm2
```

OR change log paths trong pm2-ecosystem.config.js to `/home/ec2-user/.pm2/logs/` (default PM2 location, no permission issue).

## Acceptance Criteria

- [ ] `infrastructure/fe-host/pm2-ecosystem.config.js` Bug 1 fixed (`1.2G` → `1200M`)
- [ ] `infrastructure/fe-host/pm2-ecosystem.config.js` Bug 2 fixed (cwd matches actual standalone output for pnpm workspace package)
- [ ] EITHER terraform user_data tạo `/var/log/pm2/` với ec2-user ownership OR PM2 config dùng default `~/.pm2/logs/`
- [ ] Fresh deploy via SSM SendCommand starts PM2 thành công on first try (no manual fix)
- [ ] kiteclass-frontend block trong PM2 ecosystem cũng fixed nếu enable Phase 7 (cwd: `/var/www/kiteclass-frontend/kiteclass/kiteclass-frontend`)
- [ ] Update `documents/05-guides/deploy/fe-self-host-runbook.md` Bucket C nếu paths thay đổi

## Workaround (current state)

Hot-fix applied trên EC2 `i-05cfda7c6c60b683f` 2026-05-15 12:04 UTC:
- `/opt/kite-fe/pm2.config.js` sed `1.2G` → `1200M`
- `/opt/kite-fe/pm2.config.js` sed cwd path
- `sudo mkdir -p /var/log/pm2 && sudo chown ec2-user:ec2-user /var/log/pm2`
- `pm2 start /opt/kite-fe/pm2.config.js --only kitehub-frontend` → online

PM2 process saved (`pm2 save`), startup script set (`pm2 startup systemd`). Future EC2 reboot sẽ resume kitehub-frontend tự động.

## Related

- Wave 82 Bucket C post-apply: hot-fix sequence via SSM commands `dd2f7daf-1c1b-4547-b427-f41c8c949e67` + `0e707b04-8f34-4ed1-8bc1-a5cc2405cf86` + `59724c2e-160a-437f-81d6-00bd7ca935e7` + `90dd908d-2bf0-4ac2-a519-4db3148c669b`
- Agent 2 original output: `infrastructure/fe-host/pm2-ecosystem.config.js` shipped via PR #1398
- Sister gap: GAP-572 (cert systemd timer — different file but same Wave 82 Bucket B/C "agent design assumption không match AL2023/monorepo reality" pattern)
- Rule: `pre-handoff-self-test-completeness.md` §2 — endpoint-level verify post-deploy required (current hot-fix verified at endpoint level via curl `https://kitehub.me/` 200)

## Log

- **2026-05-15:** Gap filed post Wave 82 Bucket C deploy. 3 bugs trong PM2 config surface during real deploy on EC2 (PM2 schema strict — không catch ở CI). Hot-fix manual applied + PM2 online; repo source still has bugs. Fix Wave 83 hoặc next FE deploy cycle to prevent recurrence.
