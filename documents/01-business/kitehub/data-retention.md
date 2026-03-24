# Data Retention

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| RET-01 | TRIAL tier retention | 7 days after suspension | `kitehub.data-retention.trial` |
| RET-02 | FREE tier retention | 7 days after suspension | `kitehub.data-retention.free` |
| RET-03 | BASIC tier retention | 30 days after suspension | `kitehub.data-retention.basic` |
| RET-04 | PREMIUM tier retention | 60 days after suspension | `kitehub.data-retention.premium` |
| RET-05 | ENTERPRISE tier retention | 90 days after suspension | `kitehub.data-retention.enterprise` |
| RET-06 | Warning count | 2 warnings before deletion | `kitehub.data-retention.warning-count` |
| RET-07 | First warning timing | At 50% of retention period | shouldSendWarning() |
| RET-08 | Second warning timing | At 80% of retention period | shouldSendWarning() |
| RET-09 | Final warning | 1 day before deletion | processExpiredRetention() |
| RET-10 | Deletion method | Soft delete (status=DELETED, deleted flag) | instance.softDelete() |
| RET-11 | Scheduler time | Daily at 3:00 AM | `0 0 3 * * *` |
| RET-12 | Retention start point | Instance updatedAt (when suspended) | ChronoUnit.DAYS.between(suspendedAt, now) |
| RET-13 | Default tier fallback | FREE tier (7 days) if tier unknown | getRetentionDays() default case |
| RET-14 | TRIAL/FREE treated same | Both map to trial config value | getRetentionDays() switch |

## Flow

### Daily Retention Check (3:00 AM)
```
DataRetentionScheduler.checkRetention()
  |
  +-> processRetentionWarnings()
  |     Find all SUSPENDED, non-deleted instances
  |     For each instance:
  |       1. Get retention days for tier
  |       2. Calculate daysSuspended = now - updatedAt
  |       3. Calculate daysLeft = retentionDays - daysSuspended
  |       4. If daysLeft <= 0, skip (handled by processExpiredRetention)
  |       5. If daysSuspended == retentionDays/2 (50%), send warning
  |       6. If daysSuspended == retentionDays*0.8 (80%), send warning
  |
  +-> processExpiredRetention()
        Find all SUSPENDED, non-deleted instances
        For each instance:
          1. Get retention days for tier
          2. Calculate retentionExpiry = updatedAt + retentionDays
          3. If 1 day until expiry -> send final warning email
          4. If now > retentionExpiry:
             a. Set status = DELETED
             b. Soft delete (set deleted flag)
             c. Send data-deleted notification email
```

### Warning Schedule Example (BASIC tier, 30 days)
```
Day 0:  Instance suspended
Day 15: First warning (50%) - "data-retention-warning"
Day 24: Second warning (80%) - "data-retention-warning"
Day 29: Final warning - "data-retention-final-warning"
Day 30: Data deleted - "data-deleted"
```

### Warning Schedule Example (FREE/TRIAL tier, 7 days)
```
Day 0:  Instance suspended
Day 3:  First warning (50% ~= day 3) - "data-retention-warning"
Day 5:  Second warning (80% ~= day 5) - "data-retention-warning"
Day 6:  Final warning - "data-retention-final-warning"
Day 7:  Data deleted - "data-deleted"
```

## Emails

| Trigger | Template | Method |
|---------|----------|--------|
| 50% of retention period | data-retention-warning | sendRetentionWarning() |
| 80% of retention period | data-retention-warning | sendRetentionWarning() |
| 1 day before deletion | data-retention-final-warning | sendDataRetentionFinalWarning() |
| Data deleted | data-deleted | sendDataDeletedNotification() |

## Config

```yaml
kitehub:
  data-retention:
    trial: 7         # Days for TRIAL tier
    free: 7          # Days for FREE tier
    basic: 30        # Days for BASIC tier
    premium: 60      # Days for PREMIUM tier
    enterprise: 90   # Days for ENTERPRISE tier
    warning-count: 2 # Number of warnings before deletion

# Scheduler cron
# DataRetentionScheduler: 0 0 3 * * * (daily 3:00 AM)
```
