# GAP-1441: Revenue admin page là static stub hardcode "0đ" — endpoint /admin/revenue trả data thật bị orphan

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-9)
**Affects:** KH-9 — `kitehub-frontend/src/app/(admin)/admin/revenue/page.tsx` ↔ BE `/api/platform/admin/revenue` (`AdminRevenueController`)

## Problem
Discovered Phase-2 browser walk KH-9. `revenue/page.tsx` (67 dòng) hardcode "0đ" (line 17/31), "Tháng" (line 45), placeholder chart (line 61), KHÔNG có `useQuery`. BE `/api/platform/admin/revenue` trả `{totalRevenue:999000, revenueByTier[], dailyRevenue[16]}` nhưng FE không gọi.

## Proposed Fix
Wire revenue page tới hook gọi `/admin/revenue` (`RevenueReport` type đã có ở `types/admin.ts:73`), render `totalRevenue` + `dailyRevenue` chart + `revenueByTier`. Lưu ý BE trả `revenueByTier`/`dailyRevenue` nhưng FE `RevenueReport` khai `items` → reconcile field name.

## Acceptance Criteria
- [ ] Revenue page hiển thị `totalRevenue` + chart `dailyRevenue` + breakdown `revenueByTier` từ API thật
- [ ] Field name FE `RevenueReport` khớp BE response shape

## Related
- Discovered in: Phase-2 browser walk (flow KH-9), 2026-06-16
