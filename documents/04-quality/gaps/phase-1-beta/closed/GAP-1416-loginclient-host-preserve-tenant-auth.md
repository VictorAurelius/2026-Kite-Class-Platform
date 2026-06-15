# GAP-1416: KC FE loginClient không preserve tenant Host → tenant-auth login từ subdomain fail (walk-blocker, FIXED)

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (walk-blocker)
**Domain:** Frontend
**Found:** 2026-06-15 (RBAC G2 browser-walk — blocker; per `g1-browser-walk-before-flip` curl-miss class)
**Affects:** `kiteclass-frontend/src/lib/api/auth.ts` loginClient

## Problem

`auth.ts` `loginClient` dùng static `baseURL = NEXT_PUBLIC_API_URL` (`localhost:9000`) — KHÔNG preserve tenant Host như `publicApiClient` (GAP-1207 fix chỉ apply cho public client, KHÔNG cho login). → khi page served từ tenant subdomain (nip.io `<slug>.127.0.0.1.nip.io:3000` / prod `*.kitehub.me`), login POST đi tới `localhost:9000` (Host=localhost) → gateway không resolve tenant từ subdomain (client X-Tenant-Id bị strip GAP-814) → TEACHER/STUDENT tenant-auth login fail ("service không load request"). Browser-walk bắt; curl miss vì set Host/X-Tenant-Id tay (chính xác lý do `g1-browser-walk-before-flip` tồn tại).

## Proposed Fix (DONE)

`loginClient` thêm request interceptor `loginBaseUrl()` mirror `public.ts` `browserBaseUrl()` (GAP-1207) + SSR guard: page có subdomain → gọi gateway trên CÙNG hostname (port từ env). Verified: smoke teacher login qua gateway Host=subdomain → HTTP 200 role=TEACHER; user G2 confirm 3-role login work.

## Acceptance Criteria

- [x] loginClient resolve baseURL per-request preserve tenant Host (subdomain → gateway same host)
- [x] SSR guard (window undefined → configured fallback)
- [x] G2 verify: teacher/student/owner login từ nip.io subdomain → tenant resolve → 200

## Related

- Umbrella audit: RBAC G2 walk · `public.ts` GAP-1207 (sister host-preserve, public client) · GAP-814 (gateway strips client X-Tenant-Id) · `g1-browser-walk-before-flip` (curl-miss class)
- Sibling walk-found: GAP-1417 (logout shells), GAP-1418 (console-401)
