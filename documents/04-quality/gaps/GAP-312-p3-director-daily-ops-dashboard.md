# GAP-312: Director Daily Operations Dashboard with Drill-Down Widgets

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (kiteclass-frontend dashboard) + Backend (aggregation queries)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 2 ACs — tenant director + admin (giám đốc)

---

## Problem

Giám đốc P3 cần at-a-glance daily dashboard:
- Today's classes count (vd 30)
- Attendance % (vd 92%)
- No-show alerts count (vd 3) với drill-down student list
- Complaints in SLA (vd 5/7)
- Revenue YTD vs target (vd 245M / 250M)

Click widget → drill-down detail. Refresh ≤30s.

## Root Cause

`(dashboard)/page.tsx` exists nhưng widgets không persona-specific cho giám đốc; không có aggregation queries optimized; không có drill-down navigation.

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Dashboard route | `(dashboard)/page.tsx` | ✅ scaffold (likely generic landing) |
| Aggregation service for KPIs | — | ❌ missing |
| Widget components (today-classes, attendance%, no-show, complaints, revenue) | — | ❌ missing |
| Drill-down routes | — | ❌ missing |
| Data freshness ≤30s caching | — | ❌ missing |

## Proposed Fix

1. `DashboardKpiService.computeKpis(tenantId, date)` returns KPI bundle
2. Frontend widget components with skeleton + auto-refresh
3. Drill-down: click widget → filtered list view (e.g. no-show → student list with parent contact)
4. Cache layer (Redis) ≤30s TTL for KPI snapshots
5. RBAC: only giám đốc + admin roles see this dashboard

## Acceptance Criteria

- [ ] 5 widgets render: today-classes / attendance% / no-show / complaints-in-SLA / revenue-YTD
- [ ] Dashboard load time ≤2s
- [ ] Widget auto-refresh ≤30s with stale indicator if cache expired
- [ ] Click widget → drill-down list view with relevant filter
- [ ] RBAC: lễ tân/kế toán/teacher cannot access this dashboard (403)

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-OPS-009 | Tenant Director | `P3-medium-center.md` |
| AC-OPS-007 | Admin (giám đốc) | `secondary/admin-in-P3.md` |

## Related

- Depends on: GAP-058 / GAP-308 (RBAC scope), GAP-316 (complaints), GAP-306 (revenue/payroll)
- Persona review: §2 (Tenant AC-OPS-009), §4 (Admin AC-OPS-007)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
