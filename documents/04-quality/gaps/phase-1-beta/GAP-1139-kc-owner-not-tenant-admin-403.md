# GAP-1139: KiteClass OWNER không được công nhận tenant-admin → 403 trên reports/enrollments/payroll

**Status:** 🟡 PARTIAL (95% — code fix shipped #2296, CI regression tests added wave p0-b, human G2 re-walk pending kc-core rebuild)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-10 (Flow Verification Campaign — owner G2 browser walk KiteClass `:3000`)
**Affects:** kiteclass-core — `AuthorizationBean.isAdmin()`, `ReportController`, `PayrollController` (+ mọi endpoint @authz-gated: enrollment/attendance/grade/assignment/class)

## Problem

OWNER (chủ trường — role cao nhất trong tenant KiteClass) login qua tenant-auth (PR #2292) và bị **403 Forbidden** khi vào điểm danh + /reports:
- `GET /api/v1/enrollments/class/{id}?status=ACTIVE` → 403 (load roster điểm danh)
- `GET /api/v1/reports/attendance?months=12` → 403
- `GET /api/v1/reports/revenue?months=12` → 403

**Root cause:** `GatewayHeaderAuthenticationFilter` map seeded OWNER → authority `ROLE_OWNER`. Nhưng `AuthorizationBean.isAdmin()` (kiteclass-core common/security) chỉ true cho `ROLE_PLATFORM_ADMIN` / `ROLE_ADMIN` — **KHÔNG có `ROLE_OWNER`**. `hasAccessToClass()`/`hasAccessToStudent()`/... đều `if (isAdmin())` bypass rồi mới fallback "là teacher của lớp"; OWNER không phải teacher → deny 403. Song song, `ReportController` 2 endpoint + `PayrollController` 3 endpoint dùng `@PreAuthorize("hasRole('ADMIN')")` literal → OWNER có `ROLE_OWNER` không khớp → 403.

Authz layer viết trước khi RBAC seed role OWNER (RoleSeederService). Lỗi chỉ lộ sau PR #2292 cho owner đăng nhập tới được các endpoint này. Tenant scoping vẫn đảm bảo upstream (gateway `X-Tenant-Id` + Hibernate tenantFilter), nên OWNER = tenant-admin = full quyền đọc trong tenant của mình là đúng design intent (javadoc ReportController vốn ghi "Owner/admin-only").

## Proposed Fix

1. `AuthorizationBean.isAdmin()`: thêm `|| roles.contains("ROLE_OWNER")` → OWNER bypass class-teacher check trong tenant của mình. Fix lan toả mọi endpoint @authz-gated (enrollment/attendance/grade/assignment/class).
2. `ReportController` 2× + `PayrollController` 3× `@PreAuthorize("hasRole('ADMIN')")` → `hasAnyRole('ADMIN','OWNER')`.

## Acceptance Criteria

- [x] OWNER `isAdmin()` → true (ROLE_OWNER recognized)
- [x] ReportController revenue/attendance → `hasAnyRole('ADMIN','OWNER')`
- [x] PayrollController 3 endpoint → `hasAnyRole('ADMIN','OWNER')`
- [x] CI-bound regression tests lock the fix (wave p0-b): `AuthorizationBeanTest` +6 OWNER cases (isAdmin OWNER→true, OWNER bypass class/enrollment/student, non-owner deny preserved); new `ReportControllerAuthzTest` (`*Test`, surefire) — OWNER→200 revenue+attendance, non-owner STUDENT→403, OWNER without `X-Tenant-Id`→400 `TENANT_NOT_SET` (tenant isolation preserved). `ReportControllerIT` was `*IT` = NOT CI-bound + never exercised OWNER.
- [ ] Re-walk owner: điểm danh load roster + /reports render (200, không 403) — **human G2 re-walk pending sau rebuild kiteclass-core** (code + automated tests done; chỉ còn human browser walk)

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class:** endpoint `hasRole('ADMIN')` literal HOẶC `isAdmin()` chỉ ADMIN/PLATFORM_ADMIN → OWNER (top tenant role) bị 403.

```bash
grep -rn "hasRole('ADMIN')" kiteclass/kiteclass-core/src/main/java --include="*.java"
grep -rn "@PreAuthorize" kiteclass/kiteclass-core/src/main/java --include=*.java | grep -i ADMIN | grep -iv "OWNER\|@authz"
```

| Site | Verdict | Reason |
|---|---|---|
| `AuthorizationBean.isAdmin()` | FIX | root — gate cho mọi @authz endpoint |
| `ReportController` revenue+attendance | FIX | owner-dashboard financial, OWNER must access |
| `PayrollController` 3 endpoint (bảng lương) | FIX | same class — owner xem bảng lương |
| `marketing/{LandingPage,ContactMessage,Lead}Controller` (10 sites, `hasAnyRole('ADMIN','TEACHER')`) | **DEFER → GAP-1150** | different scope (CRM/marketing); cần design decision OWNER có trong allowlist marketing không |
| `report/package-info.java` + `payroll` javadoc `{@code hasRole('ADMIN')}` | DEFER (doc-drift) | cosmetic javadoc, no behavior; clean next touch |

- Sites FIXED this PR: 5 (+ isAdmin root)
- Sites DEFERRED: 10 marketing → GAP-1150
- Sites EXEMPT: 0

## Related

- Discovered in: Flow Verification Campaign owner G2 walk 2026-06-10
- Caused-exposed-by: PR #2292 (GAP-1122/1127 tenant-auth login)
- Follow-up DEFER: GAP-1150 (marketing OWNER access) — note: marketing sites already include OWNER as of #2376
- Design: RBAC fixed-curated 5 seeded roles (OWNER/STAFF/TEACHER/PARENT/STUDENT)

## Log

- **2026-06-14 — wave `wave-2026-06-14-p0-closeout-1` Bucket B (this PR):** Code fix already on main (commit `c1920b6b8` / #2296 — `AuthorizationBean.isAdmin()` += `ROLE_OWNER`, `ReportController` 2× + `PayrollController` 3× → `hasAnyRole('ADMIN','OWNER')`). This PR adds the **missing CI-bound regression tests**: the prior `ReportControllerIT` is a `*IT` (NOT bound to CI — no failsafe in kiteclass-core) and never exercised the OWNER role. Added:
  - `AuthorizationBeanTest` (`*Test`, CI-bound) +6 cases — `isAdmin()` recognizes `ROLE_OWNER`; non-owner/non-admin (`ROLE_STUDENT`) → false; unauthenticated → false; OWNER bypass on `hasAccessToClass` / `hasAccessToEnrollment` / `hasAccessToStudent` (no DB query) → covers the enrollment/attendance/grade/assignment fan-out; non-owner non-teacher still denied (per-resource check intact).
  - `ReportControllerAuthzTest` (`*Test`, `@WebMvcTest` slice mirroring the proven `ReportControllerIT` config) — OWNER → 200 on `/reports/revenue` + `/reports/attendance`; non-owner STUDENT → 403 (service NOT invoked); OWNER without `X-Tenant-Id` → 400 `TENANT_NOT_SET` (OWNER cannot run an unscoped cross-tenant aggregate — tenant isolation preserved).
  - **Cross-flow sweep re-run (2026-06-14):** 0 behavioral controller site missing OWNER (all tenant-owner-relevant `@PreAuthorize` already grant OWNER). Only residual = `report/package-info.java:17` javadoc `{@code hasRole('ADMIN')}` doc-drift → **DEFER** (cosmetic, no behavior).
  - **code + automated tests done; the only remaining AC is the human G2 browser re-walk** (cannot be performed by an agent) → gap stays **PARTIAL** (95%).
  - Tenant data isolation across tenants (tenant A's OWNER sees only tenant A rows) is enforced at the persistence layer by the Hibernate `tenantFilter`/RLS, exercised by tenant-filter integration tests; the controller slice here proves only that OWNER cannot bypass the `X-Tenant-Id` scoping gate.
