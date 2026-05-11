# GAP-092: Re-trial Prevention (TR-07) Not Implemented in Code

**Status:** 🟢 DONE
**PR:** #311
**Priority:** 🔴 P0
**Domain:** KiteHub / Subscription / Business Logic
**Found:** 2026-04-16 (SaaS business logic audit)
**Affects:** Trial abuse prevention

## Problem

Business rule TR-07 states: "Re-trial prevention: Block if ever had trial." Config exists: `kitehub.trial.max-per-owner: 1`.

Nhưng **không tìm thấy code enforce rule này**. Không có `hadTrialBefore()` check trong `InstanceService.registerInstance()` hay `TrialService.startTrial()`.

User có thể tạo account mới hoặc request trial lần 2 → trial miễn phí vô hạn.

## Proposed Fix

```java
// In TrialService.startTrial() or InstanceService.registerInstance():
long previousTrials = instanceRepository.countByOwnerIdAndEverHadTrial(ownerId);
if (previousTrials >= trialConfig.getMaxPerOwner()) {
    throw new BusinessException("TRIAL_LIMIT_EXCEEDED", "Mỗi tài khoản chỉ được dùng thử 1 lần");
}
```

Cần thêm field `everHadTrial` hoặc query `count(status IN ('TRIAL','SUSPENDED','EXPIRED','DELETED') AND original_tier='TRIAL')`.

## Acceptance Criteria

- [ ] Owner không thể start trial lần 2
- [ ] API trả error code + message rõ ràng
- [ ] Unit test: owner đã có trial → block lần 2
- [ ] Edge case: owner có instance DELETED (từng trial) → vẫn block
