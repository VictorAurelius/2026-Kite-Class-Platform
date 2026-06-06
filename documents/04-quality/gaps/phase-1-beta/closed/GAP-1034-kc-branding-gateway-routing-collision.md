# GAP-1034: Gateway routing collision shadows 3/5 KC-10 branding controllers

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend (gateway)
**Found:** 2026-06-06 (KC-10 per-tenant branding G1 walk, FM-1)
**Affects:** `kitehub-gateway` route order (application.yml:593-601 vs :746-755) — shadows `PublicBrandingController` + `BrandingVersionController` + `BrandingPackageController` (kiteclass-core)

## Problem

KC-10 G1 walk headline. Gateway route `kitehub-branding-v1` (`application.yml:593`, `Path=/api/v1/branding/**` → `kitehub-branding:8080`) khai báo **TRƯỚC** kiteclass-core catch-all (`application.yml:749`, `Path=/api/v1/**`). Spring Cloud Gateway first-match-wins → mọi `/api/v1/branding/**` đi tới **kitehub-branding** (sai service), KHÔNG tới kiteclass-core.

**Walk evidence:**
```
GET :9000/api/v1/branding/public  (KiteClass login-page theme) → HTTP 401 (hits kitehub-branding, không có endpoint này)
GET :8080/api/v1/branding/{instanceId}/versions  (direct kiteclass-core) → HTTP 200  ← backend logic OK, chỉ routing shadow
```

3/5 controller KC-10 unreachable qua gateway:
- `PublicBrandingController` (`/api/v1/branding/public`) — FE login page (`kiteclass-frontend/.../public-branding.ts:38`) fetch tenant branding → bị che → **login render default theme thay vì tenant branding**
- `BrandingVersionController` (`/api/v1/branding/{instanceId}/versions` + `/rollback`) — version history + rollback unreachable
- `BrandingPackageController` (`/api/v1/branding/{instanceId}/package`)

Chỉ `BrandingController` (`/api/v1/settings/branding`) reachable (không match `/api/v1/branding/**`).

## Root Cause

Route `kitehub-branding-v1` dùng predicate quá rộng `Path=/api/v1/branding/**` trong khi kitehub-branding thực tế chỉ own `/api/v1/branding/jobs|instances|content` (AI wizard KH-6). Catch-all broad predicate nuốt luôn `/api/v1/branding/public|{id}/versions|{id}/package` của kiteclass-core.

## Proposed Fix

**Option A (recommended):** Thu hẹp predicate route `kitehub-branding-v1` xuống đúng paths kitehub-branding own: `Path=/api/v1/branding/jobs/**,/api/v1/branding/instances/**,/api/v1/branding/regenerate-quota,/api/v1/branding/slug-availability`. Để `/api/v1/branding/public|{id}/versions|{id}/package` fall through tới kiteclass-core.

**Option B:** Thêm explicit kiteclass branding routes (`/api/v1/branding/public`, `/api/v1/branding/*/versions`, `/api/v1/branding/*/package` → kiteclass-core) khai báo TRƯỚC `kitehub-branding-v1`.

⚠️ Sau fix, FM-4/5 IDOR latent (GAP-1037 ghi nhận DEFENDED ở runtime hiện tại qua tenant-mismatch 400) cần re-verify vì routing mới sẽ activate các endpoint này live.

## Acceptance Criteria

- [x] `GET :9000/api/v1/branding/public` (Host tenant) → 200 tenant theme (không 401)
- [x] `GET :9000/api/v1/branding/{ownInstanceId}/versions` (OWNER) → 200
- [x] kitehub-branding KH-6 jobs/instances/content endpoints vẫn route đúng (no regression)
- [x] FE KiteClass login page render tenant branding (BE routing fixed; FE visual verify at campaign G2) (logo/colors), không default theme

## Related

- Discovered in: KC-10 G1 walk (Wave flow-kc10), pre-walk FM-1
- Sister gateway-route bug: GAP-1031 (KH-10 email route over-broad). Same class: broad gateway predicate exposes/shadows wrong service.
- FM-4/5 IDOR (GAP-1037) latent behind this shadow — re-verify post-fix

## Closure (Wave security-1, 2026-06-06)

**Fix shipped:** added 3 explicit KiteClass branding routes to `kitehub-gateway/.../application.yml` BEFORE `kitehub-branding-v1` catch-all: `kiteclass-branding-public` (`/api/v1/branding/public`, no TenantResolver — public ?tenantId param), `kiteclass-branding-versions` (`/api/v1/branding/*/versions/**`, TenantResolver), `kiteclass-branding-package` (`/api/v1/branding/*/package`, TenantResolver). All → kiteclass-core. Single-predicate per route (avoid comma-predicate audit parser confusion).

**Re-walk evidence (live gateway :9000 post-rebuild):**
- `GET :9000/api/v1/branding/public?tenantId=skytest` → **200** (was 401 — now reaches kiteclass-core).
- OWNER `GET :9000/api/v1/branding/{ownInstance}/versions` → **200** (was 401).
- `GET .../package` → reaches kiteclass-core (400 validation, not 404 routing).
- No regression: kitehub-branding `slug-availability` 200 + `regenerate-quota` 200 (AI wizard KH-6 intact); gateway-route audit back to 4 findings (= main baseline).

**AC:** public 200 ✅, versions 200 ✅, kitehub-branding no-regression ✅ (200). FE login-page tenant-branding render = campaign G2 visual verify (BE routing now correct).
