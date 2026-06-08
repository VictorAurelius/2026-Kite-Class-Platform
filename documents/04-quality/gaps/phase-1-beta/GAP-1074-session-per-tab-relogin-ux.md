# GAP-1074: Session per-tab — mở URL ở tab mới bắt login lại (Option B tenant-scoped localStorage)

**Status:** 🟡 PARTIAL — code + 24 unit tests + build PASS; browser re-walk pending (2026-06-08)
**Priority:** 🟡 P2
**Domain:** Frontend (auth/security UX)
**Found:** 2026-06-08 (KC-1 G2 — user login 1 tab, mở URL tab khác → bắt login lại)
**Affects:** `kiteclass-frontend/src/lib/auth/jwt-storage.ts` + `stores/auth-store.ts` (sessionStorage → tenant-scoped localStorage)

## Problem

Login ở 1 tab; mở URL KiteClass ở tab khác → **bắt login lại**. Root cause: token lưu `sessionStorage` (per-tab, KHÔNG share cross-tab) — cố ý theo **GAP-830** (2 tab khác tenant clobber token nhau trong localStorage → cross-tenant leak). Trade-off "đóng tab = re-login" ghi "Acceptable" lúc đó. NHƯNG UX thật khó chịu cho owner mở link tab mới.

**Đây là DESIGN/SECURITY decision, không phải bug đơn thuần** — cần chốt trade-off.

## Proposed Fix (cần user chốt)

- **Option A (giữ nguyên):** sessionStorage per-tab — secure, re-login mỗi tab. Status quo.
- **Option B (khuyến nghị):** localStorage với key **scoped theo tenant** (`accessToken:<tenantId>`) — vừa cross-tab persist (hết re-login) vừa per-tenant isolated (giải đúng lo ngại GAP-830 mà không hy sinh UX). Cần refactor jwt-storage + auth-store + verify isolation 2-tenant.
- **Option C:** localStorage thuần (cross-tab) — bỏ GAP-830 isolation (regression security, KHÔNG khuyến nghị).

Security-sensitive → KHÔNG fix inline; scope proper sau khi user chốt option.

## Acceptance Criteria

- [x] User chốt option → **Option B** (localStorage scoped theo tenant)
- [x] Tenant-scoped localStorage (`kc:<tenantId>:accessToken` / `refreshToken` / `auth-store`)
- [x] Verify 2-tenant isolation không leak (24 unit tests, gồm tab-A-không-đọc-token-B + logout-A-không-xóa-B + fresh-tab-consistent-pair) — PASS
- [x] `pnpm --filter kiteclass-frontend build` PASS + 237 consumer tests PASS
- [ ] Re-walk browser: login tab 1 → mở tab 2 → KHÔNG bắt login lại + vẫn đúng tenant A data (coordinator/user G2)

## Implementation (2026-06-08, Option B)

**Pattern chốt — key scheme + fresh-tab resolution:**
- Token store: `localStorage['kc:<tenantId>:accessToken']` / `['kc:<tenantId>:refreshToken']` (cross-tab persist).
- Per-tab binding: `sessionStorage['kc:currentTenant']` = tenantId tab này (per-tab, survive reload, NOT shared) — mọi getter/setter resolve qua đây trước → tab bound vào tenant A chỉ đọc namespace A.
- Fresh-tab pointer: `localStorage['kc:activeTenant']` = tenantId login gần nhất (non-scoped). Tab MỚI chưa bind → fallback pointer → load namespace tương ứng + tự bind. Single-owner (đa số) đúng ngay; multi-tenant-tab: theo last-login (chấp nhận; production subdomain override + BE validate JWT tenantId claim).
- Zustand persist: custom `tenantScopedStateStorage` adapter → `kc:<tenantId>:auth-store` (localStorage), namespace per tenant → 2 tab khác tenant không clobber blob.

**Security (vì sao không leak):** getter trả token + tenantId là CẶP NHẤT QUÁN từ 1 namespace — không bao giờ phục vụ token A dưới id B. `clearTokens()` chỉ xóa scoped keys của tenant hiện tại (logout A không động tenant B). Worst-case fresh-tab = hiện tenant KHÁC của CHÍNH chủ (không phải user khác); BE re-check JWT claim → cross-user leak bất khả.

**Files sửa + caller swept:**
- `src/lib/auth/jwt-storage.ts` — rewrite hoàn toàn (sessionStorage → tenant-scoped localStorage); thêm `getCurrentTenantId` + `tenantScopedStateStorage`; signature các function giữ nguyên (getAccessToken/getRefreshToken/getTenantId/setTokens/setAccessToken/setRefreshToken/setTenantId/clearTokens/restorePersistedTokens/clearLegacyLocalStorageTokens).
- `src/stores/auth-store.ts` — persist storage: `sessionStorage` → `tenantScopedStateStorage`.
- `src/hooks/useAuth.ts` — `setTokens` gọi TRƯỚC `setAuth` (bind tenant trước khi store persist → blob vào đúng namespace).
- `src/lib/api-client.ts` — comment GAP-830 → GAP-1074 (interceptor dùng facade, logic không đổi).
- `src/components/auth/student-register-form.tsx` — comment update (setTokens resolve tenant từ JWT claim / default).
- `src/app/(dashboard)/branding/wizard/page.tsx` — bỏ dead fallback `localStorage.getItem('tenantId')` (key không còn ghi + là UUID không phải slug).

## Re-walk bước verify cho coordinator/user (G2)

1. Login tenant A ở tab 1 → vào dashboard, thấy data tenant A.
2. Copy URL dashboard, mở tab 2 (cùng browser) → **KHÔNG bắt login lại**, hiển thị đúng data tenant A.
3. (Isolation) Mở tab 3, login tenant B → tab 1 (tenant A) refresh vẫn là tenant A (không bị clobber sang B).
4. Logout tenant A ở tab 1 → tenant B (tab 3) vẫn đăng nhập.
5. DevTools → Application → Local Storage: thấy `kc:<A>:accessToken` + `kc:<B>:accessToken` riêng biệt + `kc:activeTenant`.

## Related

- Discovered in: KC-1 G2 walk 2026-06-08
- GAP-830 (sessionStorage per-tab decision — superseded by Option B này)
- Implemented in: Wave KC-1 G2 fix (Option B per user direction 2026-06-08)
