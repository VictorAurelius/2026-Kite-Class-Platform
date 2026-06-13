# GAP-1274: STAFF role template has no `@PreAuthorize` coverage in kiteclass-core

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (Wave rbac-lms-be-foundation — PART 3 `@PreAuthorize` role-literal audit)
**Resolved:** 2026-06-14 (Wave rbac-lms-kc9-staff — STAFF added to staff permission bundle)
**Affects:** `kiteclass/kiteclass-core/**` controllers (enrollment / attendance / invoice / payment-record)

## Problem

GAP-1119 fixes the 5 RBAC templates as OWNER/STAFF/TEACHER/PARENT/STUDENT, and this wave ships `RoleSeederService` seeding all 5 + `RoleController` to assign them. BUT a full audit of the 32 `@PreAuthorize` annotations in kiteclass-core shows **`STAFF` appears in ZERO endpoint guards**. Distinct role literals enforced: `OWNER`, `ADMIN`, `TEACHER`, `PLATFORM_ADMIN`, `PRINCIPAL`.

Per GAP-1119: STAFF = "subset owner theo permission bundle (enrollment + attendance + invoice + staff)". Those endpoints currently guard with `hasAnyRole('ADMIN','OWNER')` or `hasAnyRole('ADMIN','TEACHER','OWNER')` — a STAFF-role user is denied. So an owner can assign STAFF, but STAFF can't actually reach the staff-scoped surface.

Note the two authz layers: the `@PreAuthorize` literals match the gateway `X-User-Roles` authorities (the real enforcement), NOT the `roles` table; the seeded template / `user_roles` layer is assignment metadata. The gap is that the enforcement layer never grants STAFF.

## Proposed Fix

Add `STAFF` to the `hasAnyRole(...)` literals on staff-scoped endpoints (enrollment / attendance / invoice / staff controllers) per the GAP-1119 STAFF permission bundle. Confirm the gateway emits `ROLE_STAFF` for STAFF-assigned users (kitehub-side auth). Re-verify with an authz test per endpoint.

## Resolution (2026-06-14, Wave rbac-lms-kc9-staff)

STAFF added to the `@PreAuthorize` of the staff permission bundle (enrollment + attendance + invoice + billing), per GAP-1119 STAFF = "subset owner (enrollment + attendance + invoice + staff)". Two annotation shapes:

- **Role-literal gates** (`hasAnyRole(...)`) → appended `, 'STAFF'`:
  - `InvoiceController` (all 13 endpoints — invoice = bundle)
  - `PaymentRecordController` (2 endpoints — fee/billing payment recording, invoice-adjacent)
- **Reference-based gates** (`@authz.hasAccessTo*`) → wrapped `hasAnyRole('STAFF') or @authz...` (STAFF is tenant back-office, not class-bound; the `or` is additive — TEACHER still passes via ownership, OWNER/ADMIN via `isAdmin()` bypass, RLS `@Filter` tenant-scope preserved on every query):
  - `EnrollmentController` (5 — get/student-list/class-roster/status/withdraw)
  - `AttendanceController` (5) + `AttendanceClassBatchController` (1) + `AttendancePeriodController` (2)

**Endpoints deliberately NOT granted STAFF (owner-only / platform-only / out-of-bundle):**
- `PayrollController`, `BrandingController`, `BrandingVersionController`, `ReportController` (owner analytics), `RoleController` (role-assign), `OnboardingController` (owner setup), `ParentConsentAdminController` (child-protection consent), `DocumentGenerationController` + marketing controllers (out of bundle)
- `GradeController` — grade entry is TEACHER's domain (`SystemRoleTemplate.TEACHER` "nhập điểm"), NOT in the STAFF bundle → left ownership-scoped
- `StudentController` / `ClassController` — student read currently has no role gate (STAFF already reaches as any authenticated tenant user); class id-scoped endpoints stay teacher-ownership-scoped (class management = owner/teacher)

**Gateway propagation:** `GatewayHeaderAuthenticationFilter` already maps any `X-User-Roles` value → `ROLE_<X>` (upper-case + prefix), so a STAFF token's role claim becomes `ROLE_STAFF` in kc-core authorities — generic, pre-existing mechanism (proven by `GatewayHeaderAuthenticationFilterTest`). KH-side STAFF token mint is existing generic infra (OWNER/STAFF login KiteHub per GAP-1119), unchanged this PR.

## Acceptance Criteria

- [x] STAFF-scoped controllers include `STAFF` in their `@PreAuthorize` (enrollment/attendance/invoice/payment)
- [x] Authz test: a STAFF-authority request reaches staff endpoints (invoice list + enrollment roster via role branch), is denied on owner-only endpoints (payroll + role-assign) — `StaffRolePreAuthorizeIT` (`@WebMvcTest` + `@EnableMethodSecurity`, 5 tests PASS)
- [x] Gateway propagates `ROLE_STAFF` — `GatewayHeaderAuthenticationFilter` generic bridge (verified `GatewayHeaderAuthenticationFilterTest`); KH-side STAFF token mint = existing generic infra

## Related

- Discovered in: branch `wave/rbac-lms-be-foundation` (this PR, PART 3 audit)
- GAP-1119 (RBAC shell — fixed-curated 5 templates)
- Sister finding: GAP-1275 (PRINCIPAL literal inconsistency)
