# GAP-1259: Pending-payment TTL auto-expire + grace-period dunning reminders

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` Payment lifecycle + grace-period scheduler

## Problem

Benchmark audit (F2 + F3) + persona audit (F6): Payment ở trạng thái PENDING sống vô hạn (không TTL) → pendingPaymentId không bao giờ được giải phóng, instance kẹt. Đồng thời grace-period 3 ngày (rule SUB-04) không gửi reminder nào → owner không biết sắp bị suspend.

## Proposed Fix

Thêm TTL cho Payment PENDING (vd 7 ngày → auto-EXPIRED + giải phóng `pendingPaymentId`). Thêm dunning reminder trong grace period: 'còn X ngày trước khi tạm ngưng'.

## Acceptance Criteria

- [x] Payment PENDING quá TTL → auto chuyển EXPIRED + giải phóng pendingPaymentId
- [x] Grace period gửi ≥1 reminder trước khi suspend
- [x] Test scheduler: PENDING quá hạn được dọn đúng

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-benchmark.md` (F2, F3)
- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-persona.md` (F6)
- Sister: GAP-1257 (pending status UI), GAP-1260 (involuntary churn path), GAP-1080 (idempotent create)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE — pending-payment TTL auto-expire + grace-period dunning reminders.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
