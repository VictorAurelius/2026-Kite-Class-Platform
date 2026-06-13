# GAP-1254: MigrationRetryRunner @Transactional vô hiệu (new-instantiated + self-invoke) + retry có thể double-convert

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` TrialToPaidService.MigrationRetryRunner

## Problem

Failure-mode audit (FM-2 + FM-5): `MigrationRetryRunner` được `new` trực tiếp trong constructor `TrialToPaidService` ctor:79 — KHÔNG phải Spring bean → `@Transactional` trên nó vô hiệu (no proxy). Ngoài ra runner còn bị self-invoke tại `:67` (cùng instance, Spring AOP không intercept). Hậu quả thứ hai: `resetToPaymentCapturedForRetry:128` có thể re-convert một instance đã ACTIVE (không kiểm tra status trước khi reset) → double-convert.

## Proposed Fix

Biến `MigrationRetryRunner` thành `@Component` (Spring-managed) HOẶC bọc logic transactional bằng `TransactionTemplate`. Thêm guard kiểm tra status trước `resetToPaymentCapturedForRetry` để chặn re-convert instance đã ACTIVE.

## Acceptance Criteria

- [x] Retry transactional boundary thực sự active (bean-managed hoặc TransactionTemplate)
- [x] `resetToPaymentCapturedForRetry` reject instance status đã ACTIVE
- [x] Retry test xác nhận rollback đúng + không double-convert

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-2, FM-5)
- Sister: GAP-1253 (pessimistic lock), GAP-1271 (idempotency)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE — MigrationRetryRunner @Transactional bean-mediated + retry double-convert guard.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
