# GAP-1255: Trial-expiry scheduler bỏ qua migration_phase → suspend instance đang giữa MIGRATING

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` InstanceRepository.findExpiredTrials + trial-expiry scheduler

## Problem

Failure-mode audit (FM-4): `InstanceRepository.findExpiredTrials:35` chỉ lọc theo `status='TRIAL'`, bỏ qua cột `migration_phase`. Instance ở phase `PAYMENT_CAPTURED` / `MIGRATING` vẫn giữ `status=TRIAL` (chưa flip ACTIVE) → bị scheduler quét vào và suspend giữa chừng migration → mất tiền đã capture + instance kẹt trạng thái.

## Proposed Fix

Bổ sung điều kiện `AND migration_phase IN (NULL, 'NONE')` vào query `findExpiredTrials` để loại trừ instance đang trong tiến trình migration.

## Acceptance Criteria

- [ ] Query `findExpiredTrials` loại trừ instance có `migration_phase` ∈ {PAYMENT_CAPTURED, MIGRATING}
- [ ] Test: instance TRIAL + migration_phase=MIGRATING KHÔNG bị scheduler suspend

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-4)
- Sister: GAP-1253 (lock), GAP-1256 (tier desync)
