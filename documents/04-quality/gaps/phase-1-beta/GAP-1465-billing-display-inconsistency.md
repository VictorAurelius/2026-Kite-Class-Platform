# GAP-1465: KH-5 billing — display gói không đồng nhất /billing vs /billing/upgrade + giá năm sai + cancel-state nhầm

**Status:** 🟡 PARTIAL (format consolidation + double-₫ fixed PR #2456; cancel-state consistency defer)
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-16 (KH-5 human G2 walk)
**Affects:** kitehub-frontend PlanComparison + TierSelector + CurrentPlanCard

## Problem

KH-5 human walk: data gói hiển thị không đồng nhất giữa `/billing` và `/billing/upgrade?tier=FREE` dù cả 2 đọc CÙNG `PLAN_DETAILS` (lib/pricing.ts). Nguyên nhân:
1. **Format khác:** PlanComparison dùng `formatVND()` local + toggle Tháng/Năm; TierSelector dùng `formatPrice()` luôn tháng → string/giá lệch.
2. **Giá năm sai:** PlanComparison khi bật Năm show `monthlyPrice*12` (BASIC=6M) thay vì `yearlyPrice` thật (5.4M, -10%).
3. **Cancel-state nhầm:** sau hủy, /billing có thể show PREMIUM (còn hạn) trong khi /upgrade treat current=FREE → mâu thuẫn trạng thái.

## Acceptance Criteria

- [ ] Thống nhất 1 format function (formatPrice từ lib/pricing) cho cả 2 page
- [ ] PlanComparison annual hiển thị yearlyPrice thật (không monthlyPrice*12)
- [ ] Đồng bộ "current tier" sau cancel giữa /billing và /upgrade (PREMIUM-còn-hạn vs FREE)

## Related
- Discovered in: 2026-06-16 KH-5 G2 walk
