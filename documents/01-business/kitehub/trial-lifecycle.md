# Trial Lifecycle

**Last verified:** 2026-03-23
**Config prefix:** `kitehub.trial`

## 1. Rules

| ID | Rule | Value | Config Key | Code Location |
|----|------|-------|-----------|---------------|
| TR-01 | Trial duration | 14 days | `kitehub.trial.duration-days` | Instance.startTrial() |
| TR-02 | Max trial per owner | 1 | `kitehub.trial.max-per-owner` | InstanceService |
| TR-03 | Warning emails | Day 11, 13 | `kitehub.trial.warning-days: [3,1]` | TrialExpirationChecker |
| TR-04 | Auto-suspend on expire | Yes | — | TrialExpirationChecker |
| TR-05 | Data retention after suspend | 7 days | `kitehub.data-retention.trial: 7` | DataRetentionService |
| TR-06 | Retention warnings | Day 3, 6 after suspend | `kitehub.data-retention.warning-count: 2` | DataRetentionScheduler |
| TR-07 | Re-trial prevention | Block if ever had trial | — | InstanceService |

## 2. Flow

```
Register → Verify Email → Start Trial (14 days)
  │
  ├── Day 7: [midpoint email] (engagement)
  ├── Day 11: [warning email] "3 ngày còn lại"
  ├── Day 13: [warning email] "1 ngày còn lại"
  │
  ├── Day 14: Trial expire
  │   ├── Instance status → SUSPENDED
  │   ├── [expired email] "Hết trial. Data lưu 7 ngày."
  │   └── User KHÔNG thể truy cập instance
  │
  ├── Day 17 (suspend+3): [retention warning] "Còn 4 ngày"
  ├── Day 20 (suspend+6): [retention warning] "Còn 1 ngày"
  │
  └── Day 21 (suspend+7): Data cleanup
      ├── Backup database → S3
      ├── Drop instance database
      ├── Instance status → DELETED
      └── [deleted email] "Data đã xóa"

NGOẠI LỆ:
  ├── User nâng cấp TRƯỚC khi hết trial → Trial → ACTIVE (zero downtime)
  └── Admin extend trial → trialExpiresAt += N days (max 90)
```

## 3. Emails

| Trigger | Template | Timing | Variables |
|---------|----------|--------|-----------|
| Trial start | `welcome.html` | Ngay lập tức | {orgName, trialDays, expiryDate} |
| Day 7 | `trial-midpoint.html` | 8 AM scheduler | {orgName, daysLeft, upgradeUrl} |
| Day 11 | `trial-expiration-warning.html` | 8 AM scheduler | {orgName, daysRemaining, upgradeUrl} |
| Day 13 | `trial-expiration-warning.html` | 8 AM scheduler | {orgName, daysRemaining, upgradeUrl} |
| Day 14 | `trial-expired.html` | 8 AM scheduler | {orgName, retentionDays, upgradeUrl} |
| Suspend+3 | `data-retention-warning.html` | 3 AM scheduler | {orgName, daysLeft} |
| Suspend+6 | `data-retention-warning.html` | 3 AM scheduler | {orgName, daysLeft} |
| Suspend+7 | `data-deleted.html` | 3 AM scheduler | {orgName} |

## 4. Config

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
