# GAP-1271: Idempotency persist TOCTOU → return cached 202 (không phải 500)

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` MigrationIdempotencyKeyService

## Problem

Failure-mode audit (FM-8): `MigrationIdempotencyKeyService.persist:76` dùng pattern check-then-insert (TOCTOU). Khi hai request đồng thời cùng idempotency key, một request thắng INSERT, request thua gặp UNIQUE constraint violation → poison transaction → trả 500 thay vì trả lại kết quả cached (202). Client retry hợp lệ lại nhận lỗi server.

## Proposed Fix

Bắt `DataIntegrityViolationException` trên INSERT trùng key → đọc lại row đã tồn tại → return cached response (202) thay vì để exception nổ thành 500.

## Acceptance Criteria

- [x] Concurrent same-key request: loser nhận cached 202, không phải 500
- [x] Transaction không bị poison khi UNIQUE violation
- [x] Test idempotency race xác nhận hành vi

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-8)
- Sister: GAP-1253 (pessimistic lock), GAP-1254 (retry txn), GAP-1080 (create idempotency)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE — idempotency persist TOCTOU trả cached 202 (không 500).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
