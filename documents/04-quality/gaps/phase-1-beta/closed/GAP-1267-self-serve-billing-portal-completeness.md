# GAP-1267: Self-serve billing portal completeness (history / next-renewal / invoice / receipt)

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-frontend` billing portal

## Problem

Benchmark audit (F8): billing portal thiếu các thành phần self-serve cơ bản — lịch sử thanh toán, ngày gia hạn kế tiếp, danh sách invoice, tải biên nhận. Owner phải hỏi support cho mọi việc → tăng tải vận hành. Phụ thuộc GAP-1079 (billing crash) phải fix trước.

## Proposed Fix

Bổ sung billing portal page đầy đủ: payment history, next-renewal date, invoice list, receipt download (sau khi GAP-1079 fix crash).

## Acceptance Criteria

- [x] Billing portal hiển thị payment history + next-renewal date
- [x] Invoice list + receipt download khả dụng
- [x] Không crash khi pendingPaymentId (sau GAP-1079)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F8)
- Blocker: GAP-1079 (billing 400 crash), GAP-1093
- Sister: GAP-1266 (receipt), GAP-1262 (prorated breakdown)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. FE-1 — self-serve billing portal (history / next-renewal / invoice / receipt).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
