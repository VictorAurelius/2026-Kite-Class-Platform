# GAP-1436: DangerZone "Hủy đăng ký" no-op DELETE cho owner không có subscription nhưng vẫn redirect success giả

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-5)
**Affects:** KH-5 — `kitehub-frontend (customer)/settings/components/DangerZone.tsx:72-85` `handleCancelSubscription`

## Problem
Discovered Phase-2 browser walk KH-5. Guard `if (instance?.subscriptionId)` bỏ qua lệnh DELETE khi `subscriptionId` vắng (owner TRIAL/không có subscription) nhưng VẪN `router.push('/billing?success=cancelled')`. Owner TRIAL thấy thông báo "đã hủy" thành công giả mà không có cancel thật nào xảy ra.

## Proposed Fix
Disable/ẩn card "Hủy đăng ký" khi không có subscription active, hoặc hiển thị "bạn chưa có gói để hủy" thay vì redirect tới success giả.

## Acceptance Criteria
- [ ] Owner không có subscription active không thấy success "đã hủy" giả
- [ ] Card "Hủy đăng ký" disabled/ẩn hoặc báo "chưa có gói để hủy" khi `subscriptionId` vắng

## Related
- Discovered in: Phase-2 browser walk (flow KH-5), 2026-06-16
