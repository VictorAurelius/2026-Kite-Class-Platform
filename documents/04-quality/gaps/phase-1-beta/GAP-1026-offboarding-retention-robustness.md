# GAP-1026: Off-boarding/retention robustness — purge non-deleted trả 200 FAILED + retention warning exact-day-match

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (KH-8 off-boarding G1 walk)
**Affects:** `InstanceController.purgeInstance` + `DataRetentionService` (kitehub-subscription)

## Problem

KH-8 G1 walk catalog 2 robustness issue ở off-boarding/retention:

1. **Purge non-DELETED instance → 200 + status=FAILED (FM-5):** `DELETE /{id}/purge` trên instance chưa soft-deleted trả **HTTP 200** với body `status=FAILED` thay vì `409 Conflict`. Walk evidence: purge instance ACTIVE → 200 FAILED. Client không phân biệt được success vs invalid-state qua HTTP code; nên 409 (precondition: instance phải DELETED trước khi purge).

2. **Retention warning exact-day-match (FM-6):** `DataRetentionService.processRetentionWarnings` dùng exact-day comparison (`daysSuspended == retentionDays/2`, `daysUntilExpiry == 1`). Nếu cron job downtime đúng ngày đó → warning bị skip âm thầm, instance bị purge mà user không nhận cảnh báo trước (PDPL: phải thông báo trước khi xoá data). Nên dùng range check (`>=` + flag đã-gửi) thay vì exact-day.

## Root Cause

(1) purge trả result object thay vì map invalid-state → HTTP 4xx. (2) Scheduled warning logic dựa exact-day equality, fragile với cron downtime.

## Proposed Fix

1. `purgeInstance`: instance không DELETED → throw → map 409 (hoặc 422). Idempotent cho already-purged.
2. `processRetentionWarnings`: range-based (`daysUntilExpiry <= warningThreshold AND NOT warning_sent`) + persist `warning_sent` flag để không gửi lặp + không skip khi downtime.

## Acceptance Criteria

- [ ] Purge non-DELETED instance → 409 (không phải 200 FAILED)
- [ ] Retention warning gửi đúng kể cả cron downtime 1 ngày (range + flag)
- [ ] IT cover purge precondition + warning range logic

## Related

- Discovered in: KH-8 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh8-offboarding-pdpl-consent.md` (FM-5 + FM-6)
- Related: PDPL data retention + pre-deletion notice requirement
