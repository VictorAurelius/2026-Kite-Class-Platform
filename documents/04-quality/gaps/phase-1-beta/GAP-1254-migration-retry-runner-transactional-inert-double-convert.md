# GAP-1254: MigrationRetryRunner @Transactional vô hiệu (new-instantiated + self-invoke) + retry có thể double-convert

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` TrialToPaidService.MigrationRetryRunner

## Problem

Failure-mode audit (FM-2 + FM-5): `MigrationRetryRunner` được `new` trực tiếp trong constructor `TrialToPaidService` ctor:79 — KHÔNG phải Spring bean → `@Transactional` trên nó vô hiệu (no proxy). Ngoài ra runner còn bị self-invoke tại `:67` (cùng instance, Spring AOP không intercept). Hậu quả thứ hai: `resetToPaymentCapturedForRetry:128` có thể re-convert một instance đã ACTIVE (không kiểm tra status trước khi reset) → double-convert.

## Proposed Fix

Biến `MigrationRetryRunner` thành `@Component` (Spring-managed) HOẶC bọc logic transactional bằng `TransactionTemplate`. Thêm guard kiểm tra status trước `resetToPaymentCapturedForRetry` để chặn re-convert instance đã ACTIVE.

## Acceptance Criteria

- [ ] Retry transactional boundary thực sự active (bean-managed hoặc TransactionTemplate)
- [ ] `resetToPaymentCapturedForRetry` reject instance status đã ACTIVE
- [ ] Retry test xác nhận rollback đúng + không double-convert

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-2, FM-5)
- Sister: GAP-1253 (pessimistic lock), GAP-1271 (idempotency)
