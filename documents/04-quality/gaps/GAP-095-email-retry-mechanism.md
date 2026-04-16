# GAP-095: Email Failure Retry Mechanism Missing

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** KiteHub / Email / Reliability
**Found:** 2026-04-16 (SaaS business logic audit)
**Affects:** All email notifications (13 types)

## Problem

`EmailServiceClient` catch exceptions nhưng chỉ log + swallow:
```java
try {
    // send email...
} catch (Exception e) {
    log.error("Failed to send {} email to {}: {}", type, to, e.getMessage());
    // No retry, no queue, email silently lost
}
```

Nếu email service down tạm thời (restart, network blip) → user không nhận email quan trọng (trial expiring, data deletion warning).

## Proposed Fix

### Option A: RabbitMQ Dead Letter Queue
1. Failed emails → publish vào `email.retry` queue
2. Consumer retry 3 lần với exponential backoff (1min, 5min, 30min)
3. Sau 3 fails → publish vào `email.dlq` + alert admin

### Option B: Database retry table (simpler)
1. Failed emails → insert vào `email_retry` table
2. Scheduler mỗi 5 phút scan + retry
3. Max 3 retries, sau đó mark FAILED + alert

### Recommendation: Option B (simpler, đủ cho scale hiện tại)

## Acceptance Criteria

- [ ] Failed emails auto-retry (max 3 lần)
- [ ] Retry có exponential backoff
- [ ] Sau max retries → alert admin
- [ ] Critical emails (trial-expiring, data-deletion) có higher retry priority
