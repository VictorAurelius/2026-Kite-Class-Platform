# GAP-1440: Dashboard admin FE↔BE contract drift — KPI/revenue/pending render NaN/undefined dù BE trả data hợp lệ

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-9)
**Affects:** KH-9 admin dashboard — `types/admin.ts:51-58` (DashboardStats flat) + `(admin)/admin/page.tsx:103-144` ↔ `kitehub-admin AnalyticsService.getDashboardStats()` (nested) + `use-admin.ts:16`

## Problem
Discovered Phase-2 browser walk KH-9. FE `DashboardStats` khai flat fields; `page.tsx:103-144` đọc `stats.activeInstances`/`trialInstances`/`suspendedInstances`/`totalRevenue`/`monthlyRevenue`/`pendingPayments`. BE `getDashboardStats()` trả nested `instancesByStatus.{ACTIVE,TRIAL,SUSPENDED}` + `mrr/arr`; hook `use-admin.ts:16` return data unmapped → render NaN/undefined/blank. Chỉ `totalInstances` khớp. Browser bắt được, curl 200 che.

## Proposed Fix
Align contract: đổi FE `DashboardStats` type + `page.tsx` đọc đúng `instancesByStatus['ACTIVE']/['TRIAL']/['SUSPENDED']` + `mrr/arr`; HOẶC thêm mapping layer trong `useAdminDashboard` queryFn (BE nested → FE flat). Thêm IT/contract test bắt drift.

## Acceptance Criteria
- [ ] Dashboard admin render đúng số liệu (không NaN/undefined/blank)
- [ ] Contract/IT test bắt được FE↔BE shape drift này

## Related
- Discovered in: Phase-2 browser walk (flow KH-9), 2026-06-16
- GAP-1441 (revenue admin stub unwired)
