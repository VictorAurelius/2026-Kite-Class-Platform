# GAP-063: SMS + Zalo Notification Integration

**Status:** 🟡 PARTIAL — Phase 1 shipped Wave 18a Bucket B (notification abstraction + email adapter migrate + user preference entity + settings UI). Phase 2 deferred to **GAP-063b** (Zalo + SMS + quiet hours + cost tracking + fallback chain) — to be filed by closure PR.
**Priority:** 🟠 P1
**Domain:** Backend / Integration
**Persona blocked:** All (especially P5 K-12) — partial unblock for EMAIL channel
**Detected:** 2026-04-14
**Phase 1 PR:** (Wave 18a Bucket B)

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

- [x] Notification abstraction interface — `kitehub-email/api/NotificationChannel.java` + `NotificationContext` + `NotificationSendResult` (Phase 1, Wave 18a Bucket B). Strategy Pattern marker per design-patterns.md §1.1; BR-NOTIF-001.
- [ ] SMS adapter (1+ provider) — **DEFERRED to GAP-063b** (Twilio / VNStack / FPT)
- [ ] Zalo ZNS adapter — **DEFERRED to GAP-063b**
- [x] User preference UI — `kitehub-frontend/src/app/(customer)/settings/notifications/page.tsx` Phase 1 EMAIL only; SMS/Zalo/Push toggles disabled with "Sắp ra mắt — GAP-063b" tooltip per BR-NOTIF-002
- [ ] Quiet hours respect — **DEFERRED to GAP-063b** (no `quiet_hours_*` columns in V23; will land in V24+)
- [ ] Cost tracking per tenant — **DEFERRED to GAP-063b**
- [ ] Fallback chain (Zalo fails → SMS → Email) — **DEFERRED to GAP-063b** (Phase 1 has only EMAIL adapter wired)

### Phase 1 additional ACs delivered (Wave 18a Bucket B)

- [x] `NotificationPreference` entity per User × NotificationType × Set<Channel> (richer than V18 GAP-098 instance-level boolean)
- [x] V23 migration `add_notification_preference.sql` with FK + cascade delete + unique constraint
- [x] CRUD endpoints `GET /api/v1/notification-preferences` + `PATCH /api/v1/notification-preferences/{type}`
- [x] Mandatory-type guard (BR-NOTIF-008): EMAIL on `BILLING_INVOICE` / `SECURITY_ALERT` / `TRIAL_ENDING` cannot be disabled — enforced server-side + UI lock badge
- [x] Default-on synthesis for missing rows (BR-NOTIF-005/006) so UI always sees 7 rows without seeding DB at signup
- [x] Forward-compat: `enabledChannels` may persist SMS/ZALO/PUSH values; dispatcher (Phase 2) will skip them with `channel.disabled.in.phase1` log per BR-NOTIF-010
- [x] Backward compat: existing email producers (`EmailController`, `EmailServiceClient`, `OnboardingEmailScheduler`) still call `SESEmailService.sendEmail/sendTemplatedEmail` unchanged — 366 subscription tests + 25 email tests green post-refactor
- [x] Business docs 3-layer: `documents/01-business/kitehub/notification/{rules.md, use-cases.md, api-contract.md}` with BR-NOTIF-001..012 + 5-attribute review

## Dependencies

- GAP-021 (branding propagation in messages) — N/A for Phase 1; relevant when Zalo template approval lands in GAP-063b
- GAP-017 (billing cho notification costs) — N/A for Phase 1; relevant for GAP-063b cost tracking

## Log
- **2026-05-04** — Phase 1 shipped Wave 18a Bucket B. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 (Phase 2 deferred via sister gap GAP-063b filed by closure PR). Phase 1 deliverables: `NotificationChannel` interface + `SESEmailService` implementing it + `NotificationPreference` entity + V23 migration + CRUD service/controller + settings UI + business docs 3-layer. Existing email callers unchanged (backward compat verified via full subscription test suite + email module test suite). 11 new unit tests for service + controller; 5 contract tests for interface; 4 FE page tests. State-check 2026-05-04: V18 (GAP-098) instance-level columns retained as legacy fallback per BR-NOTIF-006.
- **2026-04-14** — Persona review — critical VN market fit
