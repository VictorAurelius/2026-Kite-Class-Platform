# Trial Lifecycle — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.trial`

## Rules

| ID | Rule | Value | Config Key | Code Location |
|----|------|-------|-----------|---------------|
| TR-01 | Trial duration | 14 days | `kitehub.trial.duration-days` | Instance.startTrial() |
| TR-02 | Max trial per owner | 1 | `kitehub.trial.max-per-owner` | InstanceService |
| TR-03 | Warning emails | Day 11, 13 | `kitehub.trial.warning-days: [3,1]` | TrialExpirationChecker |
| TR-04 | Auto-suspend on expire | Yes | — | TrialExpirationChecker |
| TR-05 | Data retention after suspend | 7 days | `kitehub.data-retention.trial: 7` | DataRetentionService |
| TR-06 | Retention warnings | Day 3, 6 after suspend | `kitehub.data-retention.warning-count: 2` | DataRetentionScheduler |
| TR-07 | Re-trial prevention | Block if ever had trial | — | InstanceService |

## Config

```yaml
kitehub:
  trial:
    duration-days: 14
    max-per-owner: 1
    warning-days: [3, 1]       # ngày trước khi hết
    midpoint-day: 7            # gửi email ngày 7
  data-retention:
    trial: 7                   # ngày giữ data sau suspend
    warning-count: 2           # số lần cảnh báo trước xóa
```
