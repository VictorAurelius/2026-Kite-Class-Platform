# GAP-1267: Self-serve billing portal completeness (history / next-renewal / invoice / receipt)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` billing portal

## Problem

Benchmark audit (F8): billing portal thiếu các thành phần self-serve cơ bản — lịch sử thanh toán, ngày gia hạn kế tiếp, danh sách invoice, tải biên nhận. Owner phải hỏi support cho mọi việc → tăng tải vận hành. Phụ thuộc GAP-1079 (billing crash) phải fix trước.

## Proposed Fix

Bổ sung billing portal page đầy đủ: payment history, next-renewal date, invoice list, receipt download (sau khi GAP-1079 fix crash).

## Acceptance Criteria

- [ ] Billing portal hiển thị payment history + next-renewal date
- [ ] Invoice list + receipt download khả dụng
- [ ] Không crash khi pendingPaymentId (sau GAP-1079)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F8)
- Blocker: GAP-1079 (billing 400 crash), GAP-1093
- Sister: GAP-1266 (receipt), GAP-1262 (prorated breakdown)
