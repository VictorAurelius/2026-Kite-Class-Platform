# GAP-1253: T2P-08 thiếu pessimistic lock trên instance khi migration → double-migration / double-payment

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` TrialToPaidService + InstanceRepository

## Problem

Failure-mode audit (FM-1): rule `T2P-08` yêu cầu pessimistic lock trên instance khi thực hiện migration trial → paid, nhưng `TrialToPaidService.loadInstance:362` dùng `findById` thường (không lock). `InstanceRepository` có 0 method `@Lock(PESSIMISTIC_WRITE)`. Hai request migration đồng thời cùng instance → cả hai cùng load state cũ → double-migration / double-payment capture.

## Proposed Fix

Thêm `findByIdForUpdate` với `@Lock(LockModeType.PESSIMISTIC_WRITE)` vào `InstanceRepository`, dùng cho mọi mutating path của migration flow (thay `findById` thường tại `loadInstance:362`).

## Acceptance Criteria

- [ ] `InstanceRepository` có method `findByIdForUpdate` annotate `@Lock(PESSIMISTIC_WRITE)`
- [ ] Migration mutating paths (loadInstance) dùng lock variant
- [ ] Concurrent-migration test xác nhận chỉ 1 request thắng, request kia chờ/abort

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-1)
- Sister: GAP-1254 (migration retry txn), GAP-1271 (idempotency TOCTOU)
