# GAP-1360: AdminPaymentsController.listPendingPayments() unbounded list

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 1.1/2.2)
**Affects:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/controller/AdminPaymentsController.java:50-54`

## Problem

`AdminPaymentsController.listPendingPayments()` (GET `/pending`) gọi `paymentService.getPendingPayments()` trả `List<PaymentResponse>` toàn cục — KHÔNG `Pageable`, KHÔNG scope. Pending-payment queue có thể tích lũy (admin chưa confirm kịp) → unbounded.

Pattern lệch: `kiteclass-core PaymentController:142` ĐÃ có bản `getPendingPayments(Pageable)` trả `Page<>`. Admin-side bỏ qua pagination.

## Proposed Fix

Đổi `listPendingPayments()` nhận `Pageable` + trả `Page<PaymentResponse>` (mirror kiteclass-core PaymentController paginated version). Update FE admin dashboard tiêu thụ Page envelope.

## Acceptance Criteria

- [ ] `listPendingPayments()` paginated (`Page<PaymentResponse>`)
- [ ] `paymentService.getPendingPayments()` có overload Pageable
- [ ] FE admin list tiêu thụ pagination

## Related

- Discovered in: 2026-06-14 performance audit (F-004)
- Pattern ref: kiteclass-core PaymentController.getPendingPayments(pageable)
