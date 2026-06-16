# GAP-1443: SupportMenu + FeedbackWidget mồ côi — feedback & support không có entry point FE trong app

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P1
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-10)
**Affects:** KH-10 notification/email/feedback/support — `kitehub-frontend/src/components/onboarding/OnboardingCoordinator.tsx` (0 JSX mount) + `(customer)/layout.tsx` + `components/layout/DashboardLayout.tsx`

## Problem
Discovered Phase-2 browser walk KH-10. `OnboardingCoordinator` (mounter duy nhất của `<SupportMenu>` tại `SupportMenu.tsx:61`) không được mount ở đâu trong cây app; `(customer)/layout.tsx` + `DashboardLayout.tsx` không có mount support/feedback/help. Owner đăng nhập không có affordance gửi feedback/truy cập support. BE `POST /api/v1/feedback` đã hoạt động (201); chỉ thiếu FE affordance. Built Wave 98 B0/B5/B6 nhưng top-level mount chưa wire.

## Proposed Fix
Mount `<OnboardingCoordinator>` (render `SupportMenu` floating button → FeedbackForm modal + quick-help + mailto support@kitehub.me + Zalo OA) trong `DashboardLayout` authenticated của customer (hoặc root authenticated layout).

## Acceptance Criteria
- [x] Owner đăng nhập thấy nút support/feedback floating — `DashboardLayout` mount `<OnboardingCoordinator>` (render banner + `SupportMenu` floating); `DashboardLayout.test.tsx` assert `support-menu-trigger` present. Runtime confirm pending walk.
- [ ] Submit feedback qua FE → POST /api/v1/feedback 201 — SupportMenu wires FeedbackForm modal (BE 201 sẵn); runtime submit confirm tại consolidated walk.
- [ ] Quick-help + mailto + Zalo OA reachable từ menu — SupportMenu render đủ items; runtime click confirm tại walk.

## Fix (Phase-3 coordinator inline, 2026-06-16)
- `kitehub-frontend/.../layout/DashboardLayout.tsx` — swap standalone `<BetaDisclaimerBanner>` → `<OnboardingCoordinator>` (renders banner without dup + mounts `SupportMenu`).
- Test: `kitehub-frontend/.../layout/__tests__/DashboardLayout.test.tsx` (2 PASS) — regression guard cho mount.
- Build: `pnpm build` kitehub-frontend exit 0.
- Status PARTIAL: mount proven via test + build; runtime feedback-submit + click affordances confirm tại consolidated walk.

## Related
- Discovered in: Phase-2 browser walk (flow KH-10), 2026-06-16
- Fixed in: Wave flow-fix-1 Phase-3 (coordinator inline)
