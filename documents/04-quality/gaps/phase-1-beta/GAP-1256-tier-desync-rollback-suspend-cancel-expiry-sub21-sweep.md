# GAP-1256: Tier desync trên rollback + suspend/cancel/expiry (instances.tier không reset) — mở rộng sweep SUB-21

**Status:** 🔵 OPEN
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

- [ ] Rollback / suspend / cancel / expiry path đều reset `instances.tier` đúng
- [ ] `DataRetentionService.getRetentionDays` đọc đúng tier sau rollback
- [ ] Test xác nhận tier-status consistency qua các path trên

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-06-13-pre-wave-kitehub-biz-100-failure-mode.md` (FM-3, FM-6)
- Sister: GAP-1090, GAP-1095, GAP-1096 (convert/activate paths đã cover), GAP-1264 (retention clock)
