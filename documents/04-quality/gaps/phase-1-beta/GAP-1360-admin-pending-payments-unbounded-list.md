# GAP-1360: AdminPaymentsController.listPendingPayments() unbounded list

**Status:** 🟡 PARTIAL
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

- [~] `listPendingPayments()` paginated (`Page<PaymentResponse>`) — **PARTIAL**: bounded (hard cap
  500) nhưng giữ shape `List<PaymentResponse>`, KHÔNG đổi sang `Page<>` envelope (xem Resolution)
- [x] `paymentService.getPendingPayments()` có overload Pageable
- [ ] FE admin list tiêu thụ pagination — deferred (FE out of scope wave này)

## Resolution (2026-06-15, branch fix/audit-fixE-perf-2026-06-14) — PARTIAL

**Đã làm:**
- Thêm overload `PaymentService.getPendingPayments(Pageable)` trả `Page<PaymentResponse>` +
  repo query `PaymentRepository.findPendingPayments(Pageable)` (bounded, soft-delete filter).
- `AdminPaymentsController.listPendingPayments()` giờ gọi bản bounded
  (`PageRequest.of(0, PENDING_PAYMENTS_MAX=500)`) + trả `.getContent()` → loại bỏ unbounded materialization.

**Vì sao PARTIAL (không break caller blind):** FE `useAdminPendingPayments()`
(`kitehub-frontend/src/hooks/use-admin.ts:177`) tiêu thụ `AdminPayment[]` (JSON array thuần).
Đổi controller sang `Page<PaymentResponse>` envelope (`{content, totalElements,...}`) sẽ vỡ FE
parse. Giữ shape `List<>` + hard cap để bịt rủi ro perf TRƯỚC; `Page<>` envelope + FE consume
pagination defer sang 1 wave có phối hợp FE.

- Test: `AdminPaymentsControllerTest` (2 list tests cập nhật stub `getPendingPayments(Pageable)`).
  Full admin surefire 64 tests PASS (incl. context-load).

## Related

- Discovered in: 2026-06-14 performance audit (F-004)
- Pattern ref: kiteclass-core PaymentController.getPendingPayments(pageable)
