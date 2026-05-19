---
title: Data Retention Policy
audience: dev
created: 2026-03-24
last-reviewed: 2026-05-19
status: living
---

# Data Retention Policy

**Last updated:** 2026-03-24
**Config prefix:** `kitehub.data-retention`
**Service:** DataRetentionService + DataRetentionScheduler

## Retention Per Tier

| Tier | Retention (days) | Config Key | Warnings |
|------|-----------------|------------|----------|
| TRIAL | 7 | `kitehub.data-retention.trial` | 2 |
| FREE | 7 | `kitehub.data-retention.free` | 2 |
| BASIC | 30 | `kitehub.data-retention.basic` | 2 |
| PREMIUM | 60 | `kitehub.data-retention.premium` | 2 |
| ENTERPRISE | 90 | `kitehub.data-retention.enterprise` | 2 |

Warning count config: `kitehub.data-retention.warning-count: 2`

## Lifecycle

```
Instance SUSPENDED (trial expired / subscription lapsed)
  │
  ├── Day 0: Suspension starts, retention countdown begins
  │
  ├── Warning 1: ~midpoint of retention
  │   └── [data-retention-warning] email
  │
  ├── Warning 2: ~1 day before deletion
  │   └── [data-retention-final-warning] email
  │
  └── Retention end: Data cleanup
      ├── Backup database → S3 (DatabaseBackupService)
      ├── Drop tenant database
      ├── Instance status → DELETED
      └── [data-deleted] email
```

## Scheduler

- **DataRetentionScheduler** runs daily at `0 0 3 * * *` (3 AM)
- Calls `DataRetentionService.processRetention()`
- Steps: send warnings → delete expired data
- **DatabaseBackupScheduler** runs daily at `0 0 2 * * *` (2 AM) for backups

## Recovery

- If user upgrades BEFORE retention ends → instance reactivated, countdown stops
- After DELETED → data unrecoverable (backup available on S3 for manual restore)

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
