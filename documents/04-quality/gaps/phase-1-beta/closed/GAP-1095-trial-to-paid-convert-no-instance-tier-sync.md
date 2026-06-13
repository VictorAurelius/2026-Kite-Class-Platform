# GAP-1095: `TrialService.convertTrialToSubscription` không sync `instance.tier` — trial→paid convert path cùng desync class GAP-1090

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-09 (tier-UI fix session — browser verify + BE sweep)
**Affects:** `kitehub-subscription` `TrialService.convertTrialToSubscription(UUID)` (`service/TrialService.java:190`) — gọi từ `TrialToPaidService.java:199` + `MigrationRetryRunner.java:103`

## Problem

Sister của GAP-1090 (đã fix `applyPendingUpgrade` 3 path). Path **trial→paid migration** chưa được cover trong fix GAP-1090 và tách riêng:

- `TrialService.convertTrialToSubscription(UUID instanceId)` (`TrialService.java:190`, đang `@Deprecated(since="1.0.0 (GAP-192 Phase 4b-i)")`) flip `instance.setStatus(InstanceStatus.ACTIVE)` (line 200) NHƯNG **không gọi `instance.setTier(...)`** — và signature chỉ nhận `UUID instanceId`, KHÔNG có param target tier để plumb.
- Hệ quả: tenant convert trial→paid qua orchestrator (`TrialToPaidService` line 199 / `MigrationRetryRunner` line 103) sẽ có `instance.tier` kẹt giá trị cũ dù subscription đã ACTIVE → cùng triệu chứng split-brain "PREMIUM shows trial UI" mà GAP-1090 mô tả, nhưng ở convert path.
- Khác GAP-1090: fix cần **signature change** (thêm tier param vào `convertTrialToSubscription`) để biết target tier, không thể chỉ thêm `setTier` như 3 path của `applyPendingUpgrade`.

## Root Cause

`convertTrialToSubscription` được thiết kế chỉ flip status (TRIAL → ACTIVE), không nhận thông tin tier đích → không thể sync `instance.tier`. Invariant "instance.tier mirror active subscription.tier" (rules.md SUB-21, ship cùng GAP-1090) chưa được enforce ở path convert này.

## Proposed Fix

1. Thêm param target tier vào `convertTrialToSubscription` (vd `convertTrialToSubscription(UUID instanceId, SubscriptionTier targetTier)`) — resolve targetTier từ subscription đích trong orchestrator (`TrialToPaidService`).
2. Set `instance.setTier(targetTier)` cùng `setStatus(ACTIVE)`.
3. Cập nhật 2 call-site (`TrialToPaidService.java:199` + `MigrationRetryRunner.java:103`) truyền targetTier — sweep callers + run tests per `api-contract-change-caller-sweep.md` (signature change).

## Acceptance Criteria

- [x] `convertTrialToSubscription` nhận target tier + set `instance.setTier(targetTier)`
- [x] 2 call-site (`TrialToPaidService` + `MigrationRetryRunner`) truyền target tier đúng
- [x] Sau convert trial→paid, `instance.tier` khớp active subscription.tier (no split-brain)
- [x] Test convert path verify instance.tier synced

## Related

- Discovered in: tier-UI fix session 2026-06-09 (BE sweep cùng class GAP-1090)
- Sister / root cause: GAP-1090 (`applyPendingUpgrade` 3 path setTier + V68 backfill + rules.md SUB-21 invariant) — DONE; gap này cover convert path tách riêng cần signature change
- Cùng class dead-method: GAP-1096 (`activateSubscription` cũng set status không sync tier)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE-1 SUB-21 sweep — convertTrialToSubscription sync instance.tier.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
