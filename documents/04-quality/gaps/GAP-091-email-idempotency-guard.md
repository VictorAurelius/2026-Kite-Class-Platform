# GAP-091: Welcome + Subscription-Created Email Missing Idempotency Guard

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** KiteHub / Email / Business Logic
**Found:** 2026-04-16 (SaaS business logic audit)
**Affects:** kitehub-subscription EmailServiceClient

## Problem

Tất cả email types khác (trial-warning, trial-expired, renewal-reminder, etc.) đều có `alreadySentToday()` idempotency check. Nhưng 2 email types thiếu:

1. `sendWelcomeEmail()` (line 450-472) — không check duplicate
2. `sendSubscriptionCreatedEmail()` (line 483-505) — không check duplicate

Nếu API call retry (network timeout, client retry) → user nhận 2+ welcome emails.

## Proposed Fix

```java
// Before sending in sendWelcomeEmail():
if (alreadySentToday(instanceId, "welcome", to)) return;

// Before sending in sendSubscriptionCreatedEmail():
if (alreadySentToday(instanceId, "subscription-created", to)) return;
```

## Acceptance Criteria

- [ ] Welcome email has idempotency guard
- [ ] Subscription-created email has idempotency guard
- [ ] Unit test: gọi 2 lần liên tiếp → chỉ gửi 1 email
