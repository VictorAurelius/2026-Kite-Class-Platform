# GAP-1274: STAFF role template has no `@PreAuthorize` coverage in kiteclass-core

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (Wave rbac-lms-be-foundation — PART 3 `@PreAuthorize` role-literal audit)
**Affects:** `kiteclass/kiteclass-core/**` controllers (32 files using `@PreAuthorize`)

## Problem

GAP-1119 fixes the 5 RBAC templates as OWNER/STAFF/TEACHER/PARENT/STUDENT, and this wave ships `RoleSeederService` seeding all 5 + `RoleController` to assign them. BUT a full audit of the 32 `@PreAuthorize` annotations in kiteclass-core shows **`STAFF` appears in ZERO endpoint guards**. Distinct role literals enforced: `OWNER`, `ADMIN`, `TEACHER`, `PLATFORM_ADMIN`, `PRINCIPAL`.

Per GAP-1119: STAFF = "subset owner theo permission bundle (enrollment + attendance + invoice + staff)". Those endpoints currently guard with `hasAnyRole('ADMIN','OWNER')` or `hasAnyRole('ADMIN','TEACHER','OWNER')` — a STAFF-role user is denied. So an owner can assign STAFF, but STAFF can't actually reach the staff-scoped surface.

Note the two authz layers: the `@PreAuthorize` literals match the gateway `X-User-Roles` authorities (the real enforcement), NOT the `roles` table; the seeded template / `user_roles` layer is assignment metadata. The gap is that the enforcement layer never grants STAFF.

## Proposed Fix

Add `STAFF` to the `hasAnyRole(...)` literals on staff-scoped endpoints (enrollment / attendance / invoice / staff controllers) per the GAP-1119 STAFF permission bundle. Confirm the gateway emits `ROLE_STAFF` for STAFF-assigned users (kitehub-side auth). Re-verify with an authz test per endpoint.

## Acceptance Criteria

- [ ] STAFF-scoped controllers include `STAFF` in their `@PreAuthorize` (enrollment/attendance/invoice/staff)
- [ ] Authz test: a STAFF-authority request reaches staff endpoints, is denied on owner-only endpoints
- [ ] Gateway propagates `ROLE_STAFF`

## Related

- Discovered in: branch `wave/rbac-lms-be-foundation` (this PR, PART 3 audit)
- GAP-1119 (RBAC shell — fixed-curated 5 templates)
- Sister finding: GAP-1275 (PRINCIPAL literal inconsistency)
