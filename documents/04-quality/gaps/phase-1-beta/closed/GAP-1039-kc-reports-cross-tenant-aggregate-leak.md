# GAP-1039: Reports revenue/attendance cross-tenant aggregate leak khi thiếu X-Tenant-Id

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core) — security (multi-tenant isolation)
**Found:** 2026-06-06 (KC-11 G1 walk, FM-1)
**Closed:** 2026-06-07 (P3 G3 batch 2 fix — explicit instance_id predicate + fail-closed `/reports/**`; walk-verified revenue scoped 2M not 3.5M + no-tenant 400 via :9000)
**Affects:** `RevenueReportRepository:41-46` + `AttendanceReportRepository:36-43` + `TenantFilterInterceptor:77-100` (kiteclass-core)

## Problem

KC-11 G1 walk: `GET /api/v1/reports/revenue` (và `/attendance`) **aggregate revenue/attendance của TẤT CẢ tenant** khi request không có `X-Tenant-Id` header — thay vì scope theo tenant của caller.

**Walk evidence (direct kiteclass-core :8080):**
```
GET /api/v1/reports/revenue  X-User-Roles:ROLE_ADMIN, KHÔNG X-Tenant-Id
→ 200 totalRevenue = 3,500,000  (SUM all tenants trong shared DB)

GET /api/v1/reports/revenue  X-User-Roles:ROLE_ADMIN, X-Tenant-Id:aaaabbbb-…-0001
→ 200 totalRevenue = 2,000,000  (scoped đúng tenant)
```

3.5M (all-tenant) vs 2M (scoped) → confirmed cross-tenant data leak khi tenant filter không apply. `@PreAuthorize("hasRole('ADMIN')")` (`ReportController:55,74`) chặn non-admin nhưng KHÔNG ngăn cross-tenant aggregate.

## Root Cause

`RevenueReportRepository` + `AttendanceReportRepository` query **KHÔNG có `instance_id` predicate** trong WHERE — hoàn toàn dựa vào Hibernate `tenantFilter`. `TenantFilterInterceptor:77-100` chỉ enable filter khi `X-Tenant-Id` present; absent → `else` branch chỉ log, **KHÔNG reject** → query chạy không filter → SUM/COUNT toàn bộ tenant.

**Exploitability:** qua gateway :9000 thì X-Tenant-Id luôn được inject từ JWT tenantId claim (scoped). NHƯNG: (a) token PLATFORM_ADMIN có thể không có tenantId claim → no header → leak; (b) internal/service call bypass gateway; (c) bug filter không apply. Defense-in-depth thiếu = single point of failure.

## Proposed Fix

1. **Explicit instance_id predicate** trong RevenueReportRepository + AttendanceReportRepository (`WHERE instance_id = :tenantId`) — không chỉ dựa filter (defense-in-depth).
2. `TenantFilterInterceptor` absent-header → **reject 400** cho tenant-scoped endpoint (fail-closed) thay vì log-and-continue.
3. Test: report query không có tenant context → 400/empty, không aggregate-all.

## Acceptance Criteria

- [x] `GET /reports/revenue` thiếu X-Tenant-Id → 400 (fail-closed) — walk via :9000 (no tenantId claim) → 400; TenantContext → TenantNotSetException → GlobalExceptionHandler 400
- [x] Repos có explicit instance_id WHERE clause — `RevenueReportRepository` `WHERE p.instanceId = :tenantId` + `AttendanceReportRepository` `WHERE a.instanceId = :tenantId`
- [x] Tenant A ADMIN report → chỉ tenant A data — walk: ADMIN tenantId=aaaabbbb → totalRevenue 2,000,000 (NOT 3,500,000 all-tenant)
- [x] Integration test cross-tenant isolation cho reports — `ReportTenantIsolationIT` (Testcontainers Postgres, filter-off slice reproduces bug, 4/4 PASS)

## Related

- Discovered in: KC-11 G1 walk (Wave flow-kc11), pre-walk FM-1
- Sister tenant-isolation class: GAP-983 (KC-3 P0 leak, Wave security-1), GAP-1015/1019 IDOR. Batch Wave security-1.
- Contrast: document gen tenant-bound đúng via TenantContext (no leak); reports là điểm yếu duy nhất.
