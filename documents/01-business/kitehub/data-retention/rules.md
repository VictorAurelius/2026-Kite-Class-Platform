# Data Retention — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.data-retention`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| RET-01 | TRIAL tier retention | 7 ngày sau suspend | `kitehub.data-retention.trial` |
| RET-02 | FREE tier retention | 7 ngày sau suspend | `kitehub.data-retention.free` |
| RET-03 | BASIC tier retention | 30 ngày sau suspend | `kitehub.data-retention.basic` |
| RET-04 | PREMIUM tier retention | 60 ngày sau suspend | `kitehub.data-retention.premium` |
| RET-05 | ENTERPRISE tier retention | 90 ngày sau suspend | `kitehub.data-retention.enterprise` |
| RET-06 | Warning count | 2 lần trước khi xóa | `kitehub.data-retention.warning-count` |
| RET-07 | Warning 1 | 50% retention period | shouldSendWarning() |
| RET-08 | Warning 2 | 80% retention period | shouldSendWarning() |
| RET-09 | Final warning | 1 ngày trước khi xóa | processExpiredRetention() |
| RET-10 | Deletion method | Soft delete (status=DELETED, deleted=true) | instance.softDelete() |
| RET-11 | Scheduler time | Daily 3:00 AM | `0 0 3 * * *` |
| RET-12 | Retention start | Instance updatedAt (when suspended) | ChronoUnit.DAYS.between() |
| RET-13 | Default fallback tier | FREE (7 ngày) | getRetentionDays() default |
| RET-14 | TRIAL/FREE xử lý như nhau | Cùng map tới trial config | getRetentionDays() switch |

## Warning Schedule Examples

**FREE/TRIAL (7 ngày):**
- Ngày 0: Suspended
- Ngày 3 (50%): Warning 1
- Ngày 5 (80%): Warning 2
- Ngày 6: Final warning
- Ngày 7: Data deleted

**BASIC (30 ngày):**
- Ngày 0: Suspended
- Ngày 15 (50%): Warning 1
- Ngày 24 (80%): Warning 2
- Ngày 29: Final warning
- Ngày 30: Data deleted

## Config

```yaml
kitehub:
  data-retention:
    trial: 7
    free: 7
    basic: 30
    premium: 60
    enterprise: 90
    warning-count: 2
```
