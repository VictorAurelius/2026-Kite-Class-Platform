# GAP-1122: KiteClass FE role-shell foundation — login role-redirect + RoleGuard + role-name parity

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend `:3000`)
**Found:** 2026-06-10 (Wave RBAC-Shell 1 Bucket A — foundation cho mọi KC user-facing shell)
**Affects:** `kiteclass-frontend` auth + mọi route group `(dashboard)`/`(teacher)`/`(public)`/`(auth)` + downstream LMS/course/grade/attendance/parent-student shells (GAP-1119 umbrella)

## Problem

FE KiteClass thiếu **tầng nền role-shell** (state-check verify code 2026-06-10):

1. **Không có login→role-based-redirect.** `useAuth.ts` `onSuccess` push cứng `/dashboard` cho mọi role — teacher/parent/student/owner đều đáp cùng 1 chỗ bất kể role.
2. **Không có RoleGuard component.** grep `RoleGuard`/`useRole`/`hasRole` → 0 hit. Logic guard bị **duplicate inline** ở 3 layout: `(teacher)`, `(dashboard)/parent`, `(dashboard)/student` — mỗi cái tự `if (userType !== X) router.replace('/dashboard')`.
3. **IDOR-by-navigation (security, P1).** Route group `(dashboard)/admin` (payroll / staff vetting / bulk-import / attendance override) **KHÔNG có** role-guard — chỉ kế thừa auth-check của `(dashboard)/layout.tsx`. Bất kỳ user đã login (teacher/parent/student) gõ thẳng `/admin/payroll` đều vào được.
4. **Role-name parity drift (Risk #3, tiền lệ Wave 78 GAP-518 `PLATFORM_ADMIN` vs `ADMIN`).** BE phát role bằng nhiều "từ vựng" khác nhau; FE `UserType` enum thiếu `OWNER` + không normalize được tên hierarchical.

## Role-name parity finding (Risk #3)

| Layer | Role vocabulary | Nguồn |
|---|---|---|
| FE `UserType` enum (pre-fix) | ADMIN, STAFF, TEACHER, PARENT, STUDENT — **thiếu OWNER** | `types/auth.ts` |
| Plan §2 canonical | OWNER, STAFF, TEACHER, PARENT, STUDENT | `wave-rbac-shell-1.md` §2 |
| JWT `role` claim KC-native (Option B) | PARENT, TEACHER, STUDENT | BR-AUTH-002 + `AuthTokenService.claim("role", entityType)` |
| Token/header KH (owner/staff SSO) | OWNER (+ ADMIN) | `GatewayHeaderAuthenticationFilter` "OWNER" / "OWNER,ADMIN" |
| BE hierarchical Role entity (V30) | TENANT_OWNER, PRINCIPAL, VICE_PRINCIPAL, DEPT_HEAD, HOMEROOM_TEACHER, SUBJECT_TEACHER, ACCOUNTANT, RECEPTIONIST, STUDENT, PARENT, PLATFORM_ADMIN | ADR-003 / `Role.java` |
| BE `@PreAuthorize` literals | OWNER, ADMIN, PRINCIPAL, TEACHER, PARENT, STUDENT | controllers (OnboardingController, TeacherController, ParentConsentAdminController...) |

**Reconcile action (shipped):** FE adapter `normalizeRole()` (`src/lib/auth/roles.ts`) collapses MỌI từ vựng BE → 1 canonical 6-role set {OWNER, ADMIN, STAFF, TEACHER, PARENT, STUDENT}. Aliases: TENANT_OWNER→OWNER, PLATFORM_ADMIN/PRINCIPAL/VICE_PRINCIPAL→ADMIN, HOMEROOM_TEACHER/SUBJECT_TEACHER→TEACHER, ACCOUNTANT/RECEPTIONIST/DEPT_HEAD→STAFF. Thêm `OWNER` vào `UserType` enum. Đây là **FE-only adapter** — KHÔNG đổi BE rule.

**BE-side follow-up (out of Bucket A scope):** `@PreAuthorize` literals dùng lẫn `OWNER`+`ADMIN`+`PRINCIPAL` không thống nhất; tenant-auth chỉ provision PARENT/TEACHER/STUDENT. Đề xuất gap riêng align BE role literal vocabulary (xem §Related).

## Proposed Fix (shipped trong PR này)

1. **`src/lib/auth/roles.ts`** — `KcRole` type + `normalizeRole()` (parity adapter) + `roleHome()` (role→home route) + `canAccess()`.
2. **`src/components/auth/role-guard.tsx`** — `<RoleGuard allow={[...]}>` reusable: hydration + auth-check + role allow-list → bounce non-allowed về role-home của chính họ.
3. **`useAuth.ts`** — login `onSuccess` push `roleHome(normalizeRole(role))` thay `/dashboard` cứng.
4. **Layouts** — refactor `(teacher)`/`(dashboard)/parent`/`(dashboard)/student` dùng RoleGuard; **tạo mới `(dashboard)/admin/layout.tsx`** allow OWNER+ADMIN (đóng IDOR gap #3).
5. **`UserType` enum** thêm `OWNER`.

## Acceptance Criteria

- [x] Login mint token → redirect đúng role-home (TEACHER→/teacher, PARENT→/parent, owner/admin/staff→/dashboard); unknown role → /dashboard neutral
- [x] RoleGuard chặn route ngoài quyền → bounce về role-home (teacher gõ /admin → /teacher)
- [x] RoleGuard redirect /login khi chưa auth
- [x] Role-name parity: `normalizeRole` map BE hierarchical + @PreAuthorize literals → FE canonical (unit-tested 17 BE vocab tokens)
- [x] `(dashboard)/admin` có role-guard (đóng IDOR-by-navigation security gap)
- [x] FE tests Red→Green: roles (13) + RoleGuard (4) + login-redirect (3) — `pnpm test` PASS
- [x] G1 browser-walk + feature-ship runtime walk trên local Docker stack (coordinator — Playwright walk 2026-06-10, xem §Walk evidence)

## Out-of-scope (track riêng)

| Item | Where |
|---|---|
| Owner/staff per-role nav restriction trên `(dashboard)` shared routes (`/classes`, `/students`...) | Bucket B (per-role nav) — chưa scope GAP-1122 Bucket A |

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 + g1-browser-walk-before-flip.md)

**Stack:** kiteclass-frontend `:3000` + kite-gateway `:9000` + kiteclass-core + kite-postgres (all healthy). **Tool:** Playwright (`@playwright/test`) headless Chromium qua FE `:3000` thật.

**Login fix (GAP-1127, cùng PR):** FE login form trước đó gọi `/api/auth/login` (KH subscription) → tenant roles 400. Sửa `authApi.login` probe `/api/v1/tenant-auth/login` (KC) trước, fallback KH cho owner. Test creds: `teacher_a@test.com`, `parent-walk@test.com`, `owner.test@test.vn` (mật khẩu walk `Walk@1234` / `Test@1234`).

| Bước (browser-real) | Kết quả | Verdict |
|---|---|---|
| TEACHER login qua form `:3000/login` | redirect `/teacher` (shell teacher) | ✅ roleHome(TEACHER) |
| PARENT login qua form | redirect `/parent` | ✅ roleHome(PARENT) |
| OWNER login (KH fallback) | redirect `/dashboard` | ✅ roleHome(OWNER) |
| TEACHER → `/admin/payroll` | bounce `/teacher/dashboard` (heading "Chào buổi sáng, Cô Hà") | ✅ IDOR-by-nav closed |
| PARENT → `/admin/payroll` | bounce `/parent` | ✅ IDOR closed |
| OWNER → `/admin/payroll` | renders (heading "Bảng lương giáo viên") | ✅ allow[OWNER,ADMIN] |
| OWNER → `/teacher`, `/parent` | bounce `/dashboard` | ✅ RoleGuard allow-list |
| UNAUTH → `/teacher` / `/admin/payroll` | redirect `/login` | ✅ RoleGuard unauth |

Console: 0 crash; chỉ 1 `400 /api/auth/login` khi cố tình test bad-creds (đúng kỳ vọng) + 404 asset lẻ (favicon). Discovery: `/admin` index thiếu `page.tsx` → 404 (GAP-1128, P3, OPEN).

## Related

- Discovered in: Wave RBAC-Shell 1 Bucket A (branch `wave/rbac-shell-1-a-roleguard`)
- Umbrella: GAP-1119 (KC role-shell wave)
- Wave 78 GAP-518 (role-name parity precedent `PLATFORM_ADMIN` vs `ADMIN`)
- Dep: Bucket C cross-product SSO KH→KC (owner/staff login functional); KC-9 student auth (student login functional)
- Follow-up đề xuất: BE `@PreAuthorize` role-literal vocabulary alignment (OWNER/ADMIN/PRINCIPAL inconsistency) — coordinator lấy ID tiếp từ block 1121-1132
- Enabler: GAP-1127 (FE login form → KC tenant-auth wiring) — unblock TEACHER/PARENT login qua FE để walk full
- Discovery: GAP-1128 (`/admin` index route thiếu page.tsx → 404)

## Log

- **2026-06-10 (DONE):** G1 browser-walk PASS qua Playwright trên FE `:3000` thật (xem §Walk evidence) — roleHome redirect TEACHER→/teacher + PARENT→/parent + OWNER→/dashboard, RoleGuard bounce + IDOR-by-nav closed trên `/admin/payroll` (teacher/parent bounce, owner allowed), unauth→/login. Headline AC verified end-to-end. TEACHER/PARENT login qua FE form được unblock bởi GAP-1127 (FE login wiring fix, cùng PR — `authApi.login` probe `/api/v1/tenant-auth/login` trước, fallback KH owner). Owner/staff per-role nav (line cũ) chuyển §Out-of-scope = Bucket B. Flip PARTIAL 70% → DONE 100%; git mv → phase-1-beta/closed/.
