# GAP-1127: KiteClass FE login form → KC-native tenant-auth wiring

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend `:3000`)
**Found:** 2026-06-10 (discovered trong GAP-1122 G1 browser-walk)
**Affects:** `kiteclass-frontend/src/lib/api/auth.ts` (login path) — TEACHER/PARENT/STUDENT đăng nhập qua FE form `:3000/login`

## Problem

FE login form KiteClass (`login-form.tsx` → `useAuth().login` → `authApi.login`) gọi **`POST /api/auth/login`** — endpoint KH subscription (OWNER/STAFF). Tenant-scoped roles (TEACHER/PARENT/STUDENT) sống trong `auth_credentials` của kiteclass-core, login qua **`POST /api/v1/tenant-auth/login`** (Wave auth-1, GAP-725/GAP-798b). Không có FE call nào tới tenant-auth.

Empirical (2026-06-10, gateway `:9000`):
- `teacher_a@test.com` → `/api/auth/login` (FE gọi) = **HTTP 400 "Invalid email or password"**
- `teacher_a@test.com` → `/api/v1/tenant-auth/login` (đúng) = **HTTP 200** + role/referenceId/tenantId

→ TEACHER/PARENT **không đăng nhập được qua FE form** → GAP-1122 roleHome redirect cho tenant roles không walk được. FE login wiring vốn scope "Bucket E+" deferred (wave-auth-1 plan line 46); Bucket A/C chỉ ship BE + gateway.

## Root Cause

`authApi.login` viết cho KH flat shape (`data.user.role`) sau GAP-724; chưa probe tenant-auth. `apiClient` interceptor force-redirect `/login` trên mọi 401 → không thể dùng làm probe (owner email không phải tenant credential → 401 → redirect nhầm).

## Proposed Fix (shipped trong PR này)

`authApi.login` dùng **bare axios client** (no interceptor) probe tenant-auth trước, fallback KH:
1. `POST /api/v1/tenant-auth/login` → 200 + `accessToken`+`role` → adapt `{accessToken, refreshToken:'', tokenType, expiresIn, user:{id:referenceId, email, name, roles:[role]}}` (unified `AuthResponse`; `useAuth` đọc `roles[0]` → normalize → roleHome redirect).
2. Probe lỗi (owner 401 / KC core down) → fallback `POST /api/auth/login` (KH flat shape giữ nguyên cho OWNER/STAFF).
3. Cả hai fail → throw VN error `"Email hoặc mật khẩu không đúng"` → `useAuth.onError` toast.

Bare client tránh interceptor side-effect (force `/login` redirect + spurious toast trên probe-401). `useAuth` KHÔNG đổi.

## Acceptance Criteria

- [x] `authApi.login` probe tenant-auth trước, fallback KH owner; adapt cả 2 shape về `AuthResponse`
- [x] TEACHER/PARENT login qua FE form `:3000/login` → roleHome redirect (`/teacher`, `/parent`) — browser-walk PASS
- [x] OWNER login qua FE form vẫn hoạt động (KH fallback → `/dashboard`)
- [x] Bad creds → 1 VN toast, không force-redirect/spurious toast
- [x] Unit test: tenant-auth path + KH fallback + uniform-error (`auth.test.ts`) — `vitest` PASS; `tsc --noEmit` clean; production `next build` (Docker) PASS

## Walk evidence

Playwright headless qua FE `:3000` 2026-06-10 — xem GAP-1122 §Walk evidence (cùng walk). TEACHER→/teacher, PARENT→/parent, OWNER→/dashboard, IDOR `/admin/payroll` closed, unauth→/login. Container kiteclass-frontend rebuild từ source sửa.

## Related

- Enables: GAP-1122 (FE role-shell foundation — headline AC walk)
- Advances: GAP-725 (KC Parent/Teacher auth path — "FE login form các role" Bucket E+) ; GAP-798b
- BE source: kiteclass-core `AuthController` `POST /api/v1/tenant-auth/login` + `AuthService` (BCrypt, HS512)
- Discovered in: GAP-1122 G1 browser-walk session 2026-06-10

## Log

- **2026-06-10 (DONE):** Fix shipped — `auth.ts` bare loginClient probe-then-fallback + response adapter; `auth.test.ts` rewrite (3 login cases). `vitest` 30 PASS (incl use-auth + role-redirect, no regression) + `tsc` clean + Docker `next build` PASS. Browser-walk verified live (GAP-1122 §Walk evidence). Per `design-first-investigation-order.md`: design (wave-auth-1 plan "FE login form các role = Bucket E+") + code (`authApi.login` → KH endpoint) confirmed gap vs intended. STUDENT login vẫn gated KC-9 (out-of-scope, GAP-269b).
