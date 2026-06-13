# GAP-1255: Trial-expiry scheduler bỏ qua migration_phase → suspend instance đang giữa MIGRATING

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` InstanceRepository.findExpiredTrials + trial-expiry scheduler

## Problem

Failure-mode audit (FM-4): `InstanceRepository.findExpiredTrials:35` chỉ lọc theo `status='TRIAL'`, bỏ qua cột `migration_phase`. Instance ở phase `PAYMENT_CAPTURED` / `MIGRATING` vẫn giữ `status=TRIAL` (chưa flip ACTIVE) → bị scheduler quét vào và suspend giữa chừng migration → mất tiền đã capture + instance kẹt trạng thái.

## Proposed Fix

Bổ sung điều kiện `AND migration_phase IN (NULL, 'NONE')` vào query `findExpiredTrials` để loại trừ instance đang trong tiến trình migration.

## Acceptance Criteria

- [x] Query `findExpiredTrials` loại trừ instance có `migration_phase` ∈ {PAYMENT_CAPTURED, MIGRATING}
- [x] Test: instance TRIAL + migration_phase=MIGRATING KHÔNG bị scheduler suspend

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-4)
- Sister: GAP-1253 (lock), GAP-1256 (tier desync)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. Bucket 0 — findExpiredTrials migration-phase guard (scheduler bỏ qua instance MIGRATING).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
