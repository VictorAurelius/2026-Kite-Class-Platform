# GAP-507: CORS production origins missing — Plan 1 self-test surfaced

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (Phase 1 BETA launch blocker — kitehub.me FE cannot POST to api.kitehub.me)
**Domain:** DevOps / Backend
**Found:** 2026-05-13 (Wave 70 Plan 1 self-test execution)
**Affects:** All FE→API POST/PUT/DELETE flows from `https://kitehub.me`

## Problem

User executed Plan 1 self-test Bước 2 (POST `/api/v1/auth/request-beta-access`) → browser CORS preflight 403:

```
Access to XMLHttpRequest at 'https://api.kitehub.me/api/v1/auth/request-beta-access'
from origin 'https://kitehub.me' has been blocked by CORS policy:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

## Root Cause

`kitehub-gateway/application.yml:11` — `CORS_ALLOWED_ORIGINS` default = `localhost:3000/3001 + internal docker hostnames`. Production override never set in `docker-compose.production.yml` or `/etc/kite/.env`. Production env var verification (`docker exec kitehub-gateway env | grep CORS`) returned empty.

## Proposed Fix

Add to `docker-compose.production.yml` kitehub-gateway service environment block:

```yaml
CORS_ALLOWED_ORIGINS: "https://kitehub.me,https://www.kitehub.me,https://kitehub-victoraurelius-projects.vercel.app"
```

## Acceptance Criteria

- [x] CORS_ALLOWED_ORIGINS env var set in docker-compose.production.yml
- [x] PR #1266 merged 2026-05-13 17:15Z
- [x] Pre-deploy live verification: `docker exec kitehub-gateway env | grep CORS` returns production origins
- [x] Post-deploy live verification: curl OPTIONS preflight returns 200 + ACAO header
- [x] Plan 1 Bước 2 POST request-beta-access works from browser

## Related

- Parent: GAP-502 (Wave 70 RC1+RC2 fix); GAP-508 (meta env-config registry — this gap is one symptom of broader systemic gap)
- Rule: `production-env-config-registry.md` v1.0.0 (mandates registry + scan)
- Registry: `documents/02-architecture/env-vars-registry.md` row 1
- PR: #1266 (CORS override)

## Log

- **2026-05-13:** Filed during Plan 1 self-test 2026-05-13 17:00Z. Single env-var fix in docker-compose. Closes via PR #1266. Triggered broader audit → GAP-508 filed for systemic registry/scan gap.
