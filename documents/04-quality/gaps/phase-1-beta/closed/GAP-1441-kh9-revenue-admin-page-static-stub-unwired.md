# GAP-1441: Revenue admin page là static stub hardcode "0đ" — endpoint /admin/revenue trả data thật bị orphan

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-9)
**Affects:** KH-9 — `kitehub-frontend/src/app/(admin)/admin/revenue/page.tsx` ↔ BE `/api/platform/admin/revenue` (`AdminRevenueController`)

## Problem
Discovered Phase-2 browser walk KH-9. `revenue/page.tsx` (67 dòng) hardcode "0đ" (line 17/31), "Tháng" (line 45), placeholder chart (line 61), KHÔNG có `useQuery`. BE `/api/platform/admin/revenue` trả `{totalRevenue:999000, revenueByTier[], dailyRevenue[16]}` nhưng FE không gọi.

## Proposed Fix
Wire revenue page tới hook gọi `/admin/revenue` (`RevenueReport` type đã có ở `types/admin.ts:73`), render `totalRevenue` + `dailyRevenue` chart + `revenueByTier`. Lưu ý BE trả `revenueByTier`/`dailyRevenue` nhưng FE `RevenueReport` khai `items` → reconcile field name.

## Acceptance Criteria
- [x] Revenue page hiển thị `totalRevenue` + chart `dailyRevenue` + breakdown `revenueByTier` từ API thật (code-complete; pending browser re-walk)
- [x] Field name FE `RevenueReport` khớp BE response shape

## Fix (2026-06-16, branch `fix/phase3-bucketB-kh9-admin`)
- Rewrote `(admin)/admin/revenue/page.tsx`: removed hardcoded "0đ" stub; now wires `useAdminRevenue('MONTHLY', startDate, endDate)` (current-month range), renders `totalRevenue` + `mrr` (MRR) + period label + `dailyRevenue` CSS bar chart + `revenueByTier` breakdown, with loading/error/empty states.
- Reconciled FE `RevenueReport` type to BE shape (`com.kitehub.admin.dto.RevenueReport`): replaced `items: RevenueReportItem[]` with `revenueByTier: RevenueTierBreakdown[]` + `dailyRevenue: DailyRevenue[]` + `mrr`/`projectedArr`/`churnImpact`.
- Test `(admin)/admin/revenue/RevenuePage.test.tsx` (6 cases — render, real data, tier breakdown, loading, error, empty). Updated `test/mocks/admin-data.ts` `mockRevenueReport` to new shape.
- Verify: `vitest run` 23/23 PASS. Pending: human browser re-walk at `:3001/admin/revenue`.

## Related
- Discovered in: Phase-2 browser walk (flow KH-9), 2026-06-16
