# GAP-1436: DangerZone "Hủy đăng ký" no-op DELETE cho owner không có subscription nhưng vẫn redirect success giả

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-5)
**Affects:** KH-5 — `kitehub-frontend (customer)/settings/components/DangerZone.tsx:72-85` `handleCancelSubscription`

## Problem
Discovered Phase-2 browser walk KH-5. Guard `if (instance?.subscriptionId)` bỏ qua lệnh DELETE khi `subscriptionId` vắng (owner TRIAL/không có subscription) nhưng VẪN `router.push('/billing?success=cancelled')`. Owner TRIAL thấy thông báo "đã hủy" thành công giả mà không có cancel thật nào xảy ra.

## Proposed Fix
Disable/ẩn card "Hủy đăng ký" khi không có subscription active, hoặc hiển thị "bạn chưa có gói để hủy" thay vì redirect tới success giả.

## Acceptance Criteria
- [x] Owner không có subscription active không thấy success "đã hủy" giả
- [x] Card "Hủy đăng ký" disabled/ẩn hoặc báo "chưa có gói để hủy" khi `subscriptionId` vắng

## Fix implemented (2026-06-16, pending re-walk)
- `kitehub/kitehub-frontend/src/app/(customer)/settings/components/DangerZone.tsx`: thêm `hasActiveSubscription = Boolean(instance?.subscriptionId)`. Khi KHÔNG có subscription → render disabled "Hủy đăng ký" button + message "Bạn chưa có gói đăng ký để hủy" (không mở dialog → không redirect success giả). `handleCancelSubscription` thêm defensive guard: nếu `!instance?.subscriptionId` → close wizard + return, KHÔNG `router.push('/billing?success=cancelled')`.
- Test: `src/app/(customer)/settings/components/__tests__/DangerZone.test.tsx` (2 cases PASS) — no-subscription shows disabled + message; with-subscription cancel card interactive.
- `pnpm --filter kitehub-frontend build` PASS.

## Related
- Discovered in: Phase-2 browser walk (flow KH-5), 2026-06-16
