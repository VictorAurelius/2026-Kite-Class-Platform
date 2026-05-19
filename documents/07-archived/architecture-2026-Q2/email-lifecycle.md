# Email Lifecycle

**Last updated:** 2026-03-24
**Service:** kitehub-email (Thymeleaf templates) + kitehub-subscription (schedulers)

## Journey

```
Register → [email-verification] → Verify → Start Trial
  │
  ├── Immediately: [welcome]
  ├── +24h: [onboarding-tips]
  ├── Day 7: [trial-midpoint]
  ├── Day 11: [trial-expiration-warning] (3 days left)
  ├── Day 13: [trial-expiration-warning] (1 day left)
  │
  ├── Day 14: Trial expires
  │   └── [trial-expired]
  │
  ├── Upgrade path: [subscription-created]
  │   ├── Before expiry: [subscription-renewal-reminder] at -7d, -3d, -1d
  │   └── After expiry: [subscription-expired] → grace 3d → [subscription-suspended]
  │
  ├── Suspended (any tier):
  │   ├── Mid-retention: [data-retention-warning]
  │   ├── Near-end: [data-retention-final-warning]
  │   └── End: [data-deleted]
```

## Templates

| # | Template | Trigger | Scheduler / Service | Cron |
|---|----------|---------|---------------------|------|
| 1 | `email-verification.html` | Register / resend | AuthService | — (immediate) |
| 2 | `welcome.html` | Email verified + trial started | AuthService | — (immediate) |
| 3 | `onboarding-tips.html` | ~24h after trial start | OnboardingEmailScheduler | `0 0 * * * *` (hourly) |
| 4 | `trial-midpoint.html` | Day 7 of trial | TrialExpirationChecker | `0 0 8 * * *` (8 AM) |
| 5 | `trial-expiration-warning.html` | 3d / 1d before trial end | TrialExpirationChecker | `0 0 8 * * *` (8 AM) |
| 6 | `trial-expired.html` | Trial day 14 (expire) | TrialExpirationChecker | `0 0 8 * * *` (8 AM) |
| 7 | `subscription-created.html` | Upgrade / new subscription | SubscriptionService | — (immediate) |
| 8 | `subscription-renewal-reminder.html` | 7d / 3d / 1d before sub end | SubscriptionExpirationChecker | `0 0 9 * * *` (9 AM) |
| 9 | `subscription-expired.html` | Subscription expires | SubscriptionExpirationChecker | `0 0 10 * * *` (10 AM) |
| 10 | `subscription-suspended.html` | Grace period ends → suspend | SubscriptionExpirationChecker | `0 0 10 * * *` (10 AM) |
| 11 | `data-retention-warning.html` | Mid-retention period | DataRetentionScheduler | `0 0 3 * * *` (3 AM) |
| 12 | `data-retention-final-warning.html` | Near end of retention | DataRetentionScheduler | `0 0 3 * * *` (3 AM) |
| 13 | `data-deleted.html` | Data cleanup complete | DataRetentionScheduler | `0 0 3 * * *` (3 AM) |

## Idempotency

All scheduler-triggered emails use `EmailSentLogRepository.alreadySentToday()` guard
to prevent duplicate sends on retry or overlapping scheduler runs.

## Config Keys

```yaml
kitehub:
  trial:
    duration-days: 14
    warning-days: [3, 1]
    midpoint-day: 7
  subscription:
    grace-period-days: 3
    warning-days: [7, 3, 1]
  data-retention:
    trial: 7
    free: 7
    basic: 30
    premium: 60
    enterprise: 90
    warning-count: 2
```
