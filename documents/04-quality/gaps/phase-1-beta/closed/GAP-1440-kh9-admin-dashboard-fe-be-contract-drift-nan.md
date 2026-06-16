# GAP-1440: Dashboard admin FE↔BE contract drift — KPI/revenue/pending render NaN/undefined dù BE trả data hợp lệ

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-9)
**Affects:** KH-9 admin dashboard — `types/admin.ts:51-58` (DashboardStats flat) + `(admin)/admin/page.tsx:103-144` ↔ `kitehub-admin AnalyticsService.getDashboardStats()` (nested) + `use-admin.ts:16`

## Problem
Discovered Phase-2 browser walk KH-9. FE `DashboardStats` khai flat fields; `page.tsx:103-144` đọc `stats.activeInstances`/`trialInstances`/`suspendedInstances`/`totalRevenue`/`monthlyRevenue`/`pendingPayments`. BE `getDashboardStats()` trả nested `instancesByStatus.{ACTIVE,TRIAL,SUSPENDED}` + `mrr/arr`; hook `use-admin.ts:16` return data unmapped → render NaN/undefined/blank. Chỉ `totalInstances` khớp. Browser bắt được, curl 200 che.

## Proposed Fix
Align contract: đổi FE `DashboardStats` type + `page.tsx` đọc đúng `instancesByStatus['ACTIVE']/['TRIAL']/['SUSPENDED']` + `mrr/arr`; HOẶC thêm mapping layer trong `useAdminDashboard` queryFn (BE nested → FE flat). Thêm IT/contract test bắt drift.

## Acceptance Criteria
- [x] Dashboard admin render đúng số liệu (không NaN/undefined/blank) — mapping layer added (code-complete; pending browser re-walk)
- [x] Contract/IT test bắt được FE↔BE shape drift này

## Fix (2026-06-16, branch `fix/phase3-bucketB-kh9-admin`)
- Added `DashboardStatsResponse` (BE nested shape) + `mapDashboardStats()` mapper in `hooks/use-admin.ts`; `useAdminDashboard` now fetches the nested BE payload and maps to flat `DashboardStats` (`activeInstances` ← `instancesByStatus.ACTIVE`, `totalRevenue` ← `arr`, `monthlyRevenue` ← `mrr`, `newInstancesThisMonth` ← `newSignupsLast30Days`); defensive `?? 0` eliminates NaN/undefined.
- `pendingPayments` removed from flat `DashboardStats` (not exposed by `/admin/dashboard`); dashboard page now sources the count from `useAdminPendingPayments().length`.
- Contract test `hooks/__tests__/use-admin-dashboard.test.tsx` (8 cases) — fixture mirrors `com.kitehub.admin.dto.DashboardStats`; asserts mapped values + no-NaN + key-presence drift guard.
- Verify: `vitest run` 23/23 PASS (3 suites). Pending: human browser re-walk at `:3001/admin`.

## Related
- Discovered in: Phase-2 browser walk (flow KH-9), 2026-06-16
- GAP-1441 (revenue admin stub unwired)
