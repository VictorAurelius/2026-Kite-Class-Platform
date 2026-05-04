# Notification Preferences — Business Rules

**Last verified:** 2026-05-04
**Wave:** 18a Bucket B (GAP-063 Phase 1)
**Config prefix:** `kitehub.notification`

## Existing state (state-check 2026-05-04)

V18__add_notification_preferences.sql (GAP-098, 2026-04-XX) added 2 boolean columns
on the `instances` table — instance-level coarse preferences:

| V18 column | Purpose |
|-----------|---------|
| `instances.email_notifications` | Master toggle: receive any email about instance |
| `instances.trial_reminders` | Toggle for trial expiration reminders specifically |

Phase 1 of GAP-063 introduces a richer **per-User × per-NotificationType × Set<Channel>**
preference model in a NEW `notification_preferences` table (V23). The V18 columns stay
as a legacy instance-level fallback for back-compat — when a user has no row in
`notification_preferences` for a given `(notification_type, channel)`, the channel default
applies (see BR-NOTIF-006 below).

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| BR-NOTIF-001 | Notification abstraction interface | `NotificationChannel.send(recipient, message, ctx)` is the single entry-point used by every notification producer. Direct provider SDK calls (SES, Twilio, Zalo) are BANNED outside the adapter that wraps them. | `com.kitehub.email.api.NotificationChannel` |
| BR-NOTIF-002 | Phase 1 channels | EMAIL only (Phase 1). SMS + ZALO + PUSH stay in `NotificationChannelType` enum but their adapters defer to GAP-063b. UI shows them as "Coming soon". | `kitehub.notification.channels.enabled=EMAIL` |
| BR-NOTIF-003 | Notification types (Phase 1) | `ABSENCE`, `FEE_REMINDER`, `EXAM_RESULT`, `TRIAL_ENDING`, `BILLING_INVOICE`, `SECURITY_ALERT`, `GENERAL_ANNOUNCEMENT` (7 types). Adding a type requires DB seed of safe defaults (BR-NOTIF-006). | `NotificationType` enum |
| BR-NOTIF-004 | Preference key | `(user_id, notification_type)` is the unique key. One row per pair. `enabled_channels` is a comma-separated set of `NotificationChannelType` values. | DB `UNIQUE (user_id, notification_type)` |
| BR-NOTIF-005 | Default-on for transactional types | When no row exists for a `(user, type)` pair, EMAIL is enabled by default for: `BILLING_INVOICE`, `SECURITY_ALERT`, `TRIAL_ENDING`. These cannot be disabled via UI in Phase 1 (BR-NOTIF-008). | Service default fallback |
| BR-NOTIF-006 | Default-on for engagement types | When no row exists, EMAIL is enabled by default for: `ABSENCE`, `FEE_REMINDER`, `EXAM_RESULT`, `GENERAL_ANNOUNCEMENT`. User CAN disable via UI. | Service default fallback |
| BR-NOTIF-007 | Cascade on User delete | `notification_preferences` rows are CASCADE-deleted when the parent user row is removed. | DB `ON DELETE CASCADE` |
| BR-NOTIF-008 | Mandatory transactional types | Types in BR-NOTIF-005 are flagged `mandatory=true`. UI MUST display them as read-only "always on". Service rejects PATCH that would disable them with HTTP 400 `MANDATORY_TYPE_CANNOT_BE_DISABLED`. | `NotificationType.mandatory` boolean |
| BR-NOTIF-009 | Tenant isolation | Preference queries filter by `user_id`; the user is resolved from the JWT's `sub` claim. Cross-user reads return 403. | Spring Security `@PreAuthorize` |
| BR-NOTIF-010 | Future-channel placeholder behavior | `enabled_channels` MAY contain SMS/ZALO/PUSH values stored in DB. Send-time, the dispatcher logs `channel.disabled.in.phase1` and skips. No exception, no user-visible failure. | `NotificationDispatcher` (Phase 2) |
| BR-NOTIF-011 | Audit | Every create/update/delete of `notification_preferences` writes a structured log line at INFO level: `notification.preference.changed` with `userId`, `notificationType`, `before`, `after`. PII-safe: no message body. | logback JSON appender |
| BR-NOTIF-012 | Quiet hours (deferred GAP-063b) | Phase 1 stores no quiet-hours columns. `quiet_hours_start` + `quiet_hours_end` to be added in V24+ when GAP-063b ships. | — |

## Five-attribute review per `business-logic-review.md`

- **Source:** GAP-063 (2026-04-14) persona reviews — Vietnamese market data shows email open-rate <30% for parents/students; Zalo/SMS required for engagement parity. Phase 1 abstraction unblocks Phase 2 channel addition without re-architecting.
- **Rationale:** Build the abstraction once now (cheap, isolated). Channel adapters (Zalo/SMS) are independent work units that can land later without touching producers. User-level preferences are richer than instance-level (V18 GAP-098) because notifications target individual users (a parent vs an admin within the same instance want different things).
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-04). PDPL/CCPA review N/A — no new PII collected; preference rows reference `user_id` only.
- **Compliance check:** **Compliant** — PDPL 2023 Art 23 retention rules apply via existing user-cascade (BR-NOTIF-007); no new retention surface. Consumer Protection Law N/A — preference UI is opt-in, no commercial commitment changes.
- **Review cadence:** Quarterly. **Next review:** 2026-08-04. Event triggers: addition of 2nd channel adapter (Zalo or SMS landing GAP-063b); addition of 8th NotificationType (forces re-look at default fallback ordering).

## Code references

- Interface: `kitehub-email/src/main/java/com/kitehub/email/api/NotificationChannel.java`
- Email adapter: `kitehub-email/src/main/java/com/kitehub/email/service/SESEmailService.java`
- Entity: `kitehub-subscription/src/main/java/com/kitehub/subscription/notification/entity/NotificationPreference.java`
- Migration: `kitehub-subscription/src/main/resources/db/migration/V23__add_notification_preference.sql`

## Sister gap

Phase 2 work tracked as **GAP-063b** (filed by closure PR after this Phase 1 ships):
- Zalo ZNS adapter
- SMS adapter (Twilio / VNStack / FPT SMS)
- Quiet hours respect
- Cost tracking per tenant
- Fallback chain (Zalo → SMS → Email)

## Log

- **2026-05-04** — Doc created in Wave 18a Bucket B Phase 1 ship. Cited V18 (GAP-098) instance-level legacy in Existing state.
