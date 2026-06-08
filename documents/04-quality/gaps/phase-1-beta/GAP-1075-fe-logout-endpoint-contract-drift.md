# GAP-1075: FE logout gọi BE endpoint không tồn tại (404) + thiếu server-side refresh-token revocation

**Status:** 🟡 PARTIAL
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

**DEFER (server-side, P1):**
- Implement `POST /api/auth/logout` trong AuthController + AuthService → blacklist refresh token (cần Redis blacklist store + tests). Stateless JWT hiện tại không revoke refresh token → token bị lộ vẫn valid tới hết hạn. Security hardening.

## Acceptance Criteria

- [x] FE logout clear session client-side bất kể BE endpoint (browser walk bước 5 PASS — logout A, tab B vẫn login)
- [x] Không còn 404 noise mỗi lần logout (bỏ BE call)
- [ ] BE `POST /api/auth/logout` + refresh-token revocation (Redis blacklist) — DEFER
- [ ] Reuse token đã logout bị reject server-side — DEFER (sau khi blacklist ship)

## Related

- Discovered in: KC-1 G2 walk 2026-06-08 (session isolation recipe `documents/05-guides/operations/2026-06-08-g2-recipe-kc1-session-isolation.md`)
- Bug class: GAP-1070 (FE↔BE contract drift detector) — detector nên cover auth endpoints kitehub-subscription, không chỉ kiteclass-core
- Sister drift DEFER: GAP-803 (`/reset-password` + `/forgot-password` route mismatch)
- Walk-blocking fix per `small-gap-inline-fix.md` SPLIT (FE inline + BE defer)
