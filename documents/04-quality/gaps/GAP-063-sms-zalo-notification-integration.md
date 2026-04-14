# GAP-063: SMS + Zalo Notification Integration

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Integration
**Persona blocked:** All (especially P5 K-12)
**Detected:** 2026-04-14

## Problem

Email không hiệu quả ở VN cho:
- Học sinh (ít check email)
- Phụ huynh (check Zalo > email)
- Urgent notifications (absence, emergency)

Platform cần:
- **SMS** (Twilio VN, VNStack, FPT SMS)
- **Zalo** (ZNS — Zalo Notification Service)

## Proposed Fix

### Notification abstraction

```java
public interface NotificationChannel {
  void send(String recipient, String message, NotificationContext ctx);
}

Implementations:
- EmailNotificationChannel (existing)
- SmsNotificationChannel (Twilio adapter)
- ZaloNotificationChannel (ZNS adapter)
- PushNotificationChannel (FCM)
```

### User preferences

```java
@Entity
public class NotificationPreference {
  User user;
  NotificationType type;  // ABSENCE, FEE_REMINDER, EXAM_RESULT, ...
  Set<NotificationChannel> enabledChannels;
  LocalTime quietHoursStart, quietHoursEnd;
}
```

### Zalo Official Account setup

- Register Zalo Business account
- Get ZNS API credentials
- Template message approval (Zalo requirement)
- Cost per message tracking

### SMS cost management

- Rate per SMS: ~200-300 VND
- Budget per tenant tier
- Cost attribution per tenant (billing integration)

## Acceptance Criteria

- [ ] Notification abstraction interface
- [ ] SMS adapter (1+ provider)
- [ ] Zalo ZNS adapter
- [ ] User preference UI
- [ ] Quiet hours respect
- [ ] Cost tracking per tenant
- [ ] Fallback chain (Zalo fails → SMS → Email)

## Dependencies

- GAP-021 (branding propagation in messages)
- GAP-017 (billing cho notification costs)

## Log
- 2026-04-14 — Persona review — critical VN market fit
