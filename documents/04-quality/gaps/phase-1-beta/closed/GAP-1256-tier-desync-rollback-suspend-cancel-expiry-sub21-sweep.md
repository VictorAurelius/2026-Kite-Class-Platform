# GAP-1256: Tier desync trên rollback + suspend/cancel/expiry (instances.tier không reset) — mở rộng sweep SUB-21

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-13 (Wave kitehub-biz-100 outside-in audit)
**Affects:** `kitehub-subscription` rollback/suspend/cancel/expiry paths + DataRetentionService

## Problem

Failure-mode audit (FM-3 + FM-6): rule SUB-21 yêu cầu sync `instances.tier` theo subscription. Path `rollback:257` set `status=TRIAL` nhưng KHÔNG `setTier` → tier vẫn giữ giá trị paid cũ. Các path suspend / cancel / expiry cũng không reset tier. Hệ quả: `DataRetentionService.getRetentionDays:40` + pool-size logic đọc tier sai → retention/quota tính nhầm theo tier đã hết hiệu lực.

Sister gaps GAP-1090 / GAP-1095 / GAP-1096 đã cover các path convert/activate; gap này mở rộng SUB-21 sync list sang rollback + suspend + cancel + expiry.

## Proposed Fix

Thêm rollback / suspend / cancel / expiry vào danh sách path phải `setTier` (reset về tier tương ứng status mới) theo rule SUB-21.

## Acceptance Criteria

- [x] Rollback / suspend / cancel / expiry path đều reset `instances.tier` đúng
- [x] `DataRetentionService.getRetentionDays` đọc đúng tier sau rollback
- [x] Test xác nhận tier-status consistency qua các path trên

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-3, FM-6)
- Sister: GAP-1090, GAP-1095, GAP-1096 (convert/activate paths đã cover), GAP-1264 (retention clock)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. Bucket 0 — InstanceTierSyncService SUB-21 helper; tier reset trên rollback/suspend/cancel/expiry sweep.

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
