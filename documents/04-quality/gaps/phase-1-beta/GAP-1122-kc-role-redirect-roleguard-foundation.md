# GAP-1122: KiteClass FE role-shell foundation — login role-redirect + RoleGuard + role-name parity

**Status:** 🟡 PARTIAL
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
- [ ] G1 browser-walk + feature-ship runtime walk trên local Docker stack (coordinator — pre DONE flip per `feature-ship-runtime-walk-mandate.md` §1)
- [ ] Owner/staff per-role nav restriction trên `(dashboard)` shared routes (`/classes`, `/students`...) — **Bucket B scope**, không Bucket A

## Related

- Discovered in: Wave RBAC-Shell 1 Bucket A (branch `wave/rbac-shell-1-a-roleguard`)
- Umbrella: GAP-1119 (KC role-shell wave)
- Wave 78 GAP-518 (role-name parity precedent `PLATFORM_ADMIN` vs `ADMIN`)
- Dep: Bucket C cross-product SSO KH→KC (owner/staff login functional); KC-9 student auth (student login functional)
- Follow-up đề xuất: BE `@PreAuthorize` role-literal vocabulary alignment (OWNER/ADMIN/PRINCIPAL inconsistency) — coordinator lấy ID tiếp từ block 1121-1132
