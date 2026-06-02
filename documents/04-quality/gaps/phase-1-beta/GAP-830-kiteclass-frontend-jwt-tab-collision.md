# GAP-830: JWT storage key collision 2-tab — kiteclass-frontend (cùng bug class GAP-599)

**Status:** 🟡 PARTIAL (85% — sessionStorage facade + 5 sites migrated + unit tests; live 2-tab Playwright deferred, stack down)
**Priority:** 🟠 P1
**Domain:** Frontend
**Detected:** 2026-06-02
**Related PRs:** [GAP-599 closure PR — kitehub-frontend]
**Related Docs:** `documents/04-quality/gaps/phase-1-beta/GAP-599-jwt-tab-collide-storage-isolation.md`, `.claude/rules/cross-flow-bug-class-sweep.md`

## Current State (verified 2026-06-02)

Cross-flow sweep (per `cross-flow-bug-class-sweep.md` §3) sau khi đóng GAP-599 cho kitehub-frontend phát hiện kiteclass-frontend có **đúng bug class** — JWT lưu trong `localStorage` single-key, shared across tabs cùng origin.

| Piece | File / Path | Status |
|-------|-------------|--------|
| Login token write | `kiteclass/kiteclass-frontend/src/hooks/useAuth.ts:49-50` (`localStorage.setItem('accessToken'/'refreshToken')`) | ❌ localStorage (shared tab) |
| API client read | `kiteclass/kiteclass-frontend/src/lib/api-client.ts:29` (`localStorage.getItem('accessToken')`) | ❌ localStorage |
| API client refresh rotate | `kiteclass/kiteclass-frontend/src/lib/api-client.ts:56,67` (refresh-token flow ghi `localStorage`) | ❌ localStorage |
| Student register token write | `kiteclass/kiteclass-frontend/src/components/auth/student-register-form.tsx:128-129` (`localStorage.setItem('access_token'/'refresh_token')`) | ❌ localStorage + **key name khác** (`access_token` snake_case vs `accessToken` camelCase ở useAuth) |
| Logout cleanup | `kiteclass/kiteclass-frontend/src/hooks/useAuth.ts:76-77` (`localStorage.removeItem`) | ❌ localStorage |
| Auth store | `kiteclass/kiteclass-frontend/src/stores/auth-store.ts` | zustand store (cần verify persist middleware tương tác) |
| sessionStorage facade | (chưa có — kitehub-frontend có `src/lib/auth/jwt-storage.ts`) | ❌ missing |

**Grep commands run:**
```bash
grep -rnE "localStorage|sessionStorage" kiteclass/kiteclass-frontend/src \
  | grep -iE "token|jwt|accessToken|refreshToken" | grep -v "__tests__" | grep -v ".test."
find kiteclass/kiteclass-frontend/src -iname "*jwt*" -o -iname "*storage*" -o -iname "*auth*"
```

## Problem

Giống hệt GAP-599 nhưng ở **kiteclass-frontend** (multi-tenant app — mỗi tenant là 1 trường). Mở 2 tab cùng domain tenant:
1. Tab A: đăng nhập GVCN → JWT vào `localStorage['accessToken']`
2. Tab B: đăng nhập học sinh/phụ huynh khác → ghi đè `localStorage['accessToken']` của Tab A

Hệ quả: Tab A submit form sau đó mang JWT của Tab B → 403 hoặc sai role context. KiteClass là multi-tenant nên rủi ro cross-tenant/cross-role cao hơn (nhiều actor đồng thời).

Thêm một vấn đề phụ surfaced trong sweep: **key name không nhất quán** — `useAuth.ts` dùng `accessToken`/`refreshToken` (camelCase) còn `student-register-form.tsx` dùng `access_token`/`refresh_token` (snake_case). `api-client.ts` đọc `accessToken` camelCase → student register có thể ghi key mà api-client không đọc được. Cần reconcile khi fix.

## Context

Phát hiện qua cross-flow sweep bắt buộc (per `cross-flow-bug-class-sweep.md`) khi đóng GAP-599. GAP-599 scope chỉ kitehub-frontend; kiteclass-frontend là app riêng, kiến trúc auth khác (zustand store + refresh interceptor) → tách thành gap riêng (DEFER verdict trong sweep) thay vì mở rộng GAP-599.

## Evidence

Sweep table phía trên (7 sites). Verdict cross-flow sweep: **DEFER** (same bug class, scope riêng app + cần reconcile key-name inconsistency + tương tác zustand persist).

## Proposed Fix

**Option A — sessionStorage facade** (giống GAP-599 Wave 92 Bucket B):
- Tạo `kiteclass/kiteclass-frontend/src/lib/auth/jwt-storage.ts` facade (port từ kitehub-frontend pattern), backed bằng `sessionStorage` → per-tab native isolation
- Migrate 7 sites trên qua facade (`getAccessToken`/`setTokens`/`clearTokens`/refresh-rotate)
- Reconcile key name: chuẩn hóa về `accessToken`/`refreshToken` (sửa `student-register-form.tsx` dùng facade thay vì `access_token` snake_case)
- Verify tương tác zustand `auth-store.ts` persist middleware (nếu persist tokens vào localStorage → cần đổi storage adapter sang sessionStorage)
- Optional "remember me" persist path như GAP-599 (`persist=true` mirror localStorage)

**Tests:** port jsdom unit + simulation tests + thêm Playwright 2-tab spec (như `kitehub-frontend/e2e/jwt-2tab-isolation.spec.ts`).

## Acceptance Criteria

- [x] 2 tab kiteclass-frontend đăng nhập 2 actor khác nhau → JWT KHÔNG collide (mechanism: sessionStorage per-tab native isolation — facade + auth-store đều dùng sessionStorage; live browser verify deferred)
- [x] Key name nhất quán (`accessToken`/`refreshToken`) across mọi site (snake_case `access_token`/`refresh_token` ở student-register reconciled qua facade)
- [x] zustand auth-store persist không re-introduce shared-tab token (`createJSONStorage(() => sessionStorage)`)
- [x] Logout tab A không ảnh hưởng tab B (sessionStorage per-tab; `clearTokens()` chỉ clear current tab session + localStorage legacy sweep)
- [ ] Unit + 2-tab simulation + Playwright live test pass → Unit DONE (`jwt-storage.test.ts`); live 2-tab Playwright deferred (stack down)

## Current State (verified 2026-06-02) — FIX SHIPPED

Branch `feature/GAP-830-kc-jwt-sessionstorage-isolation`:
- NEW `kiteclass-frontend/src/lib/auth/jwt-storage.ts` — sessionStorage facade (port từ kitehub-frontend GAP-599 pattern + tenantId support + legacy snake_case sweep).
- Migrated 5 sites: `useAuth.ts` (setTokens/clearTokens), `api-client.ts` (getAccessToken/getRefreshToken/getTenantId/setAccessToken/clearTokens ×5), `student-register-form.tsx` (setTokens + key reconcile snake→camel), `auth-store.ts` (persist → `createJSONStorage(() => sessionStorage)` — closes 2nd collision vector).
- NEW unit test `src/lib/auth/__tests__/jwt-storage.test.ts` (set/get/clear + remember-me restore + key-name reconcile + JWT claim decode).
- Sweep post-fix: 0 remaining direct `localStorage.*Item('access'|'refresh')` sites trong src (excl facade legacy-sweep + tests).
- Verify: `pnpm --filter kiteclass-frontend lint` + `build` + jwt-storage unit test.

Live 2-tab Playwright walk (AC #5) deferred — `FEATURE_SHIP_WALK_DEFER: GAP-830 — stack down, 2-tab browser walk on stack restore`.

## Related

- Sister gap: GAP-599 (kitehub-frontend — đã đóng cùng pattern)
- Rule: `.claude/rules/cross-flow-bug-class-sweep.md` (sweep DEFER → file follow-up gap)
- Rule: `.claude/rules/pre-handoff-self-test-completeness.md` §2.7 multi-tenant tenant-switch checklist

## Log

- **2026-06-02 (e2e-helper sweep regression — fixed):** Cross-flow-bug-class-sweep MISS phát hiện khi reproduce GAP-872 local. Migration src/** sang sessionStorage (5 sites) đã bỏ sót **e2e/** test helpers vẫn đọc/ghi auth tokens qua `localStorage` (`auth-storage` + accessToken/refreshToken/tenantId) → login helper vỡ cho TOÀN BỘ KiteClass E2E suite (class-lifecycle gate + gap-759 + wave-49-followups). Vi phạm `cross-flow-bug-class-sweep.md`: sweep chỉ quét `src/` mà bỏ `e2e/`. Fixed 5 sites / 3 files cùng PR: `e2e/helpers/auth.ts` (login waitForFunction + isAuthenticated + injectAuthTokens), `e2e/wave-49-followups/_helpers.ts` (loginAs persona seed), `e2e/gap-759-flat-auth-shape-contract.spec.ts` (test name + assertion). Verify: class-lifecycle gate `6 passed` local sau fix. AC #5 (live 2-tab) vẫn DEFER (stack down) — GAP-830 giữ PARTIAL 85%.
- **2026-06-02:** Filed từ cross-flow sweep của GAP-599 closure (kitehub-frontend). Verdict DEFER: same bug class (localStorage single-key shared-tab) nhưng kiteclass-frontend là app riêng — zustand auth-store + refresh interceptor + key-name inconsistency (`access_token` vs `accessToken`) cần reconcile → tách scope. P1 (multi-tenant → cross-tenant/role risk cao, nhưng chưa có beta tenant KiteClass live nên chưa P0).
