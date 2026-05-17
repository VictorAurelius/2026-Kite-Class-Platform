# GAP-602: pm2-ecosystem.config.js cwd path mismatch monorepo standalone

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-05-17 (Wave 88 Bucket D FE deploy)
**Affects:** kitehub-frontend + kiteclass-frontend PM2 deployment on kc-app-fe EC2

## Problem

`infrastructure/fe-host/pm2-ecosystem.config.js` hardcodes:
```js
cwd: '/var/www/kitehub-frontend/.next/standalone',
script: 'server.js'
```

Nhưng Next.js standalone build trong monorepo workspace tạo `server.js` ở path nested:
```
/var/www/kitehub-frontend/kitehub/kitehub-frontend/server.js
```

Wave 88 Bucket D manual deploy bypass ecosystem config — start PM2 trực tiếp via:
```bash
cd /var/www/kitehub-frontend/kitehub/kitehub-frontend && pm2 start server.js --name kitehub-frontend ...
```

→ Works but không persist qua reboot (no ecosystem config = no `pm2 save` resurrect script).

## Root Cause

Wave 82 Bucket B drafted ecosystem config assuming non-monorepo Next.js layout. Wave 88 Bucket D shipped FE build qua `pnpm --filter kitehub-frontend build` → standalone preserves workspace structure.

## Proposed Fix

Update `infrastructure/fe-host/pm2-ecosystem.config.js`:

```js
apps: [
  {
    name: 'kitehub-frontend',
    cwd: '/var/www/kitehub-frontend/kitehub/kitehub-frontend',  // ← updated
    script: 'server.js',
    env: { PORT: 4701, HOSTNAME: '127.0.0.1', NODE_ENV: 'production', ... },
    // ... rest of config
  },
  {
    name: 'kiteclass-frontend',
    cwd: '/var/www/kiteclass-frontend/kiteclass/kiteclass-frontend',  // ← updated
    script: 'server.js',
    env: { PORT: 4700, ... },
  }
]
```

Update fe-self-host-runbook.md §4.2 to reflect monorepo standalone layout.

## Acceptance Criteria

- [ ] `pm2-ecosystem.config.js` cwd paths updated cho cả 2 apps
- [ ] SSM SendCommand re-deploy với `pm2 start /var/www/pm2-ecosystem.config.js` → 2 apps online
- [ ] Verify `pm2 list` matches manual deploy state
- [ ] PM2 systemd auto-start works after reboot (link với GAP-603)
- [ ] Runbook §4.2 documents monorepo cwd

## Related

- Wave 82 Bucket B: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md`
- Wave 88 closure: `documents/04-quality/audits/aws-verification/2026-05-17-wave-88-cutover-post-apply.md` §8
- `infrastructure/fe-host/pm2-ecosystem.config.js`
- `documents/05-guides/deploy/fe-self-host-runbook.md`
- GAP-603 PM2 systemd auto-start (companion)

## Log

- **2026-05-17:** Gap filed during Wave 88 closure. Manual `pm2 start server.js` workaround active on kc-app-fe. Reboot will lose PM2 state.
- **2026-05-17 (Wave 89 Bucket B PARTIAL — PR #1479):** Code ship — `infrastructure/fe-host/pm2-ecosystem.config.js` cwd field updated cho cả 2 apps (kitehub-frontend + kiteclass-frontend) sang monorepo nested path. `scripts/deploy-fe.sh` defensive script paired (explicit absolute config path). Runbook `documents/05-guides/deploy/pm2-systemd-auto-start.md` ship. **Live verify deferred** per `gap-done-discipline.md` §3 PARTIAL exit ramp — yêu cầu user trigger SSM re-deploy với new config (rsync + `pm2 reload`) + maintenance window. Companion GAP-603 systemd wire same PR. Status PARTIAL ~80% (code complete; live AC #2-#5 chờ user execute).
