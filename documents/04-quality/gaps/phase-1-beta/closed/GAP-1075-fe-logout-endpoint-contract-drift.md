# GAP-1075: FE logout gọi BE endpoint không tồn tại (404) + thiếu server-side refresh-token revocation

**Status:** 🟢 DONE 2026-06-08 — BE endpoint + Redis blacklist shipped + live-walk PASS on real Redis
**Priority:** 🟠 P1
**Domain:** Mixed (Frontend + Backend)
**Found:** 2026-06-08 (KC-1 G2 browser walk — bước 5 logout-isolation)
**Affects:** `kiteclass-frontend` auth flow + `kitehub-subscription` AuthController

## Problem

KC-1 G2 browser walk bước 5 (logout): browser console báo `POST :9000/api/auth/logout → 404` và **không logout được**.

Root cause = FE↔BE contract drift (class GAP-1070):
- FE `kiteclass-frontend/src/lib/api/auth.ts:26` gọi `POST /api/auth/logout` với `{ refreshToken }`.
- BE `kitehub-subscription/.../AuthController.java` (`@RequestMapping("/api/auth")`) KHÔNG có endpoint logout — chỉ có register/login/refresh/verify-email/resend-verification/profile/change-password.
- Logout side-effects (`clearAuth` + `clearTokens` + redirect) bị gate sau `onSuccess` của mutation (`useAuth.ts:75`) → 404 throw → onSuccess không chạy → token không xóa, user kẹt đăng nhập.

Cross-flow sweep (per `cross-flow-bug-class-sweep.md`) phát hiện 2 endpoint cùng class drift trong auth.ts: `/api/auth/forgot-password` + `/api/auth/reset-password` (BE cũng không có) → DEFER sang GAP-803 (password-reset flow, đã tracked).

## Proposed Fix

**FIX INLINE (shipped 2026-06-08):**
- `auth.ts` logout → client-side only (no BE call), comment ref gap này (BE endpoint chưa có).
- `useAuth.ts` logout mutation `onSuccess` → `onSettled` → local clear luôn chạy bất kể success/error.

**BE SERVER-SIDE (shipped 2026-06-08, commit `fa1f5faa`):**
- `POST /api/auth/logout` (kitehub-subscription `AuthController`) → `AuthService.logout()` blacklist refresh token.
- `RefreshTokenBlacklistService` — Redis-backed (design-canonical per `service-catalog-and-auth-flow.md`); key = `refresh-blacklist:<sha256(token)>`, TTL = remaining token life (self-expires). **Fail-open**: Redis outage không break logout/refresh (availability > absolute revocation, khớp stateless-JWT model).
- `AuthService.refresh()` reject token đã blacklist.
- `spring-boot-starter-data-redis` + `spring.data.redis` config (env fallback chain đọc `SPRING_REDIS_HOST=kite-redis` compose đã set). Lettuce connect lazy → Redis down lúc boot không chặn startup.
- FE `auth.ts` logout giờ gọi endpoint thật (best-effort, swallow error).
- 14 unit test PASS (8 blacklist + 6 logout/refresh): `RefreshTokenBlacklistServiceTest` + `AuthServiceLogoutTest`.

## Acceptance Criteria

- [x] FE logout clear session client-side bất kể BE endpoint (browser walk bước 5 PASS — logout A, tab B vẫn login)
- [x] Không còn 404 noise mỗi lần logout (bỏ BE call → gọi endpoint thật)
- [x] BE `POST /api/auth/logout` + refresh-token revocation (Redis blacklist) — SHIPPED code+test (commit `fa1f5faa`)
- [x] Reuse token đã logout bị reject server-side — **LIVE WALK PASS** (real Redis): login → refresh 200 (control) → logout 200 → blacklist key `refresh-blacklist:43a280f5...` ghi Redis → reuse refresh token → HTTP 400 "Invalid or expired refresh token" (reject = 400 khớp behavior invalid-refresh sẵn có)

## Related

- Discovered in: KC-1 G2 walk 2026-06-08 (session isolation recipe `documents/05-guides/operations/2026-06-08-g2-recipe-kc1-session-isolation.md`)
- Bug class: GAP-1070 (FE↔BE contract drift detector) — detector nên cover auth endpoints kitehub-subscription, không chỉ kiteclass-core
- Sister drift DEFER: GAP-803 (`/reset-password` + `/forgot-password` route mismatch)
- Walk-blocking fix per `small-gap-inline-fix.md` SPLIT (FE inline + BE defer)

## Log

- **2026-06-08:** BE server-side logout shipped (commit `fa1f5faa`) — `POST /api/auth/logout` + Redis-backed `RefreshTokenBlacklistService` (fail-open) + `refresh()` blacklist check + 14 unit tests green. Design-first investigation confirmed logout belongs in kitehub-subscription (KC-1 owner login routes there via gateway), không phải kiteclass-core. kitehub-subscription chưa wire Redis (dùng Caffeine) → thêm `spring-boot-starter-data-redis` + config. User chose Full Redis blacklist (design-canonical) per AskUserQuestion. Status PARTIAL→90%; còn live curl walk pending stack rebuild.
- **2026-06-08 (DONE):** Stack rebuilt (build-all) + `up --force-recreate` → kitehub-subscription healthy (redis wiring confirmed: context load `RefreshTokenBlacklistService` + StringRedisTemplate OK). **Live walk PASS** qua gateway :9000: login owner@skyedu.vn → refresh 200 (control) → logout 200 "Đăng xuất thành công" → Redis key `refresh-blacklist:43a280f5...` ghi thật → reuse refresh token → 400 reject. Tất cả 4 AC verified. Flip DONE per `gap-done-discipline.md` §2 + `pre-handoff-self-test-completeness.md` §2.1.
