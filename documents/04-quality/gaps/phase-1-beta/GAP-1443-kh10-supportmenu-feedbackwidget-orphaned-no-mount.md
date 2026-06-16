# GAP-1443: SupportMenu + FeedbackWidget mồ côi — feedback & support không có entry point FE trong app

**Status:** 🔵 OPEN
**Priority:** 🔴 P1
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-10)
**Affects:** KH-10 notification/email/feedback/support — `kitehub-frontend/src/components/onboarding/OnboardingCoordinator.tsx` (0 JSX mount) + `(customer)/layout.tsx` + `components/layout/DashboardLayout.tsx`

## Problem
Discovered Phase-2 browser walk KH-10. `OnboardingCoordinator` (mounter duy nhất của `<SupportMenu>` tại `SupportMenu.tsx:61`) không được mount ở đâu trong cây app; `(customer)/layout.tsx` + `DashboardLayout.tsx` không có mount support/feedback/help. Owner đăng nhập không có affordance gửi feedback/truy cập support. BE `POST /api/v1/feedback` đã hoạt động (201); chỉ thiếu FE affordance. Built Wave 98 B0/B5/B6 nhưng top-level mount chưa wire.

## Proposed Fix
Mount `<OnboardingCoordinator>` (render `SupportMenu` floating button → FeedbackForm modal + quick-help + mailto support@kitehub.me + Zalo OA) trong `DashboardLayout` authenticated của customer (hoặc root authenticated layout).

## Acceptance Criteria
- [ ] Owner đăng nhập thấy nút support/feedback floating
- [ ] Submit feedback qua FE → POST /api/v1/feedback 201
- [ ] Quick-help + mailto + Zalo OA reachable từ menu

## Related
- Discovered in: Phase-2 browser walk (flow KH-10), 2026-06-16
