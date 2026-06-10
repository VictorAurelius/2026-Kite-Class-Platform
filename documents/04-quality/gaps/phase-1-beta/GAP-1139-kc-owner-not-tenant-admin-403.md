# GAP-1139: KiteClass OWNER không được công nhận tenant-admin → 403 trên reports/enrollments/payroll

**Status:** 🟡 PARTIAL (90% — code fix shipped, human G2 re-walk pending kc-core rebuild)
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
- [ ] Re-walk owner: điểm danh load roster + /reports render (200, không 403) — **human G2 re-walk pending sau rebuild kiteclass-core**

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
- Follow-up DEFER: GAP-1150 (marketing OWNER access)
- Design: RBAC fixed-curated 5 seeded roles (OWNER/STAFF/TEACHER/PARENT/STUDENT)
