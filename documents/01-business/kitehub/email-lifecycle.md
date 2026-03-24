# Email Lifecycle

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| EML-01 | Idempotency guard | Max 1 email per type per instance per day | alreadySentToday() check |
| EML-02 | Idempotency storage | EmailSentLog entity (instanceId, emailType, recipient, sentAt) | email_sent_log table |
| EML-03 | Email service endpoint | POST /api/platform/emails/send | `email.service.url` |
| EML-04 | Email service URL | http://kitehub-email:8080 (internal) | `email.service.url` |
| EML-05 | Template count | 13 HTML templates | kitehub-email templates/ |
| EML-06 | Failure handling | Log error and continue (no retry at client level) | (hardcoded try/catch) |
| EML-07 | Welcome email has no idempotency guard | Sent once on activation | sendWelcomeEmail() |
| EML-08 | Subscription-created email has no idempotency guard | Sent once on creation | sendSubscriptionCreatedEmail() |

## Flow

### Email Send Flow
1. Check `alreadySentToday(instanceId, emailType, recipient)`
2. If already sent today, skip (log info and return)
3. Build EmailRequest (to, subject, templateName, variables)
4. POST to email service `/api/platform/emails/send`
5. On success: record in EmailSentLog via `recordEmailSent()`
6. On failure: log error, throw exception (caller catches)

### Scheduler Triggers
```
Hourly:   OnboardingEmailScheduler      -> onboarding-tips (23-25h after activation)
Daily:    TrialExpirationChecker (8AM)   -> trial-expiration-warning (3d, 1d before)
                                         -> trial-midpoint (day 7 of trial)
                                         -> trial-expired (on expiry)
Daily:    SubscriptionExpirationChecker
          (9AM)                          -> subscription-renewal-reminder (7d, 3d, 1d)
          (10AM)                         -> subscription-suspended (after grace period)
Daily:    DataRetentionScheduler (3AM)   -> data-retention-warning (50%, 80% of retention)
                                         -> data-retention-final-warning (1 day before deletion)
                                         -> data-deleted (on deletion)
Event:    Instance activation            -> welcome
Event:    Subscription created           -> subscription-created
Event:    Subscription expired           -> subscription-expired
```

## Emails

| # | Template File | Email Type Key | Trigger | Scheduler/Event | Idempotent |
|---|--------------|----------------|---------|-----------------|------------|
| 1 | welcome.html | welcome | Instance activated (PENDING->TRIAL) | activatePendingInstance() | No |
| 2 | email-verification.html | (standalone) | Registration flow | (manual) | N/A |
| 3 | onboarding-tips.html | onboarding-tips | 23-25h after trial start | OnboardingEmailScheduler (hourly) | Yes |
| 4 | trial-midpoint.html | trial-midpoint | Day 7 of trial | TrialExpirationChecker (8AM) | Yes |
| 5 | trial-expiration-warning.html | trial-warning | 3, 1 days before trial end | TrialExpirationChecker (8AM) | Yes |
| 6 | trial-expired.html | trial-expired | Trial expires | TrialExpirationChecker (8AM) | Yes |
| 7 | subscription-created.html | subscription-created | Subscription created | SubscriptionService.create() | No |
| 8 | subscription-renewal-reminder.html | renewal-reminder | 7, 3, 1 days before sub expiry | SubscriptionExpirationChecker (9AM) | Yes |
| 9 | subscription-expired.html | subscription-expired | Subscription expired | EmailServiceClient event | Yes |
| 10 | subscription-suspended.html | suspension-notification | Instance suspended | EmailServiceClient event | Yes |
| 11 | data-retention-warning.html | retention-warning | 50%, 80% of retention period | DataRetentionScheduler (3AM) | Yes |
| 12 | data-retention-final-warning.html | retention-final-warning | 1 day before data deletion | DataRetentionScheduler (3AM) | Yes |
| 13 | data-deleted.html | data-deleted | Data deleted | DataRetentionScheduler (3AM) | Yes |

## Config

```yaml
email:
  service:
    url: ${EMAIL_SERVICE_URL:http://localhost:8083}

kitehub:
  email-service:
    url: ${EMAIL_SERVICE_URL:http://kitehub-email:8080}
    enabled: ${EMAIL_SERVICE_ENABLED:true}

# Scheduler cron expressions:
# OnboardingEmailScheduler:        0 0 * * * *    (every hour)
# TrialExpirationChecker:          0 0 8 * * *    (daily 8:00 AM)
# SubscriptionExpirationChecker:   0 0 9 * * *    (daily 9:00 AM)
# processExpiredSubscriptions:     0 0 10 * * *   (daily 10:00 AM)
# DataRetentionScheduler:          0 0 3 * * *    (daily 3:00 AM)
# DatabaseBackupScheduler:         0 0 2 * * *    (daily 2:00 AM)
# DatabaseBackupScheduler cleanup: 0 0 3 * * SUN  (weekly Sunday 3:00 AM)
```
