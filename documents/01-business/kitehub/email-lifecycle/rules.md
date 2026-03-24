# Email Lifecycle — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.email-service`, `email.service`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| EML-01 | Idempotency guard | Max 1 email per type per instance per ngày | alreadySentToday() |
| EML-02 | Idempotency storage | EmailSentLog entity (instanceId, emailType, recipient, sentAt) | email_sent_log table |
| EML-03 | Email service endpoint | POST /api/platform/emails/send | `email.service.url` |
| EML-04 | Email service URL internal | http://kitehub-email:8080 | `kitehub.email-service.url` |
| EML-05 | Template count | 13 HTML templates | kitehub-email/templates/ |
| EML-06 | Failure handling | Log error + continue (no retry) | try/catch |
| EML-07 | Welcome email | Không có idempotency guard | sendWelcomeEmail() |
| EML-08 | Subscription-created email | Không có idempotency guard | sendSubscriptionCreatedEmail() |

## Templates (13 files)

| # | Template | Email Type Key | Trigger | Idempotent |
|---|---------|----------------|---------|------------|
| 1 | welcome.html | welcome | Instance activated | No |
| 2 | email-verification.html | (standalone) | Registration | N/A |
| 3 | onboarding-tips.html | onboarding-tips | 23-25h sau trial start | Yes |
| 4 | trial-midpoint.html | trial-midpoint | Ngày 7 của trial | Yes |
| 5 | trial-expiration-warning.html | trial-warning | 3, 1 ngày trước hết trial | Yes |
| 6 | trial-expired.html | trial-expired | Trial expire | Yes |
| 7 | subscription-created.html | subscription-created | Sub created | No |
| 8 | subscription-renewal-reminder.html | renewal-reminder | 7/3/1 ngày trước hết sub | Yes |
| 9 | subscription-expired.html | subscription-expired | Sub expired | Yes |
| 10 | subscription-suspended.html | suspension-notification | Instance suspended | Yes |
| 11 | data-retention-warning.html | retention-warning | 50%, 80% retention period | Yes |
| 12 | data-retention-final-warning.html | retention-final-warning | 1 ngày trước xóa | Yes |
| 13 | data-deleted.html | data-deleted | Data deleted | Yes |

## Scheduler Triggers

```
Hourly:  OnboardingEmailScheduler   → onboarding-tips (23-25h)
8 AM:    TrialExpirationChecker     → trial-warning, trial-midpoint, trial-expired
9 AM:    SubscriptionExpirationChecker → renewal-reminder
10 AM:   SubscriptionExpirationChecker → suspension-notification, subscription-expired
3 AM:    DataRetentionScheduler     → retention-warning, retention-final-warning, data-deleted
Event:   Instance activated         → welcome
Event:   Subscription created       → subscription-created
```

## Config

```yaml
email:
  service:
    url: ${EMAIL_SERVICE_URL:http://localhost:8083}
kitehub:
  email-service:
    url: ${EMAIL_SERVICE_URL:http://kitehub-email:8080}
    enabled: ${EMAIL_SERVICE_ENABLED:true}
```
