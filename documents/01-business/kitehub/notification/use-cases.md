# Notification Preferences — Use Cases

**Last verified:** 2026-05-04
**Wave:** 18a Bucket B (GAP-063 Phase 1)

## UC-NOTIF-PREFERENCE-LIST

**Actor:** Authenticated User (any role).

**Trigger:** User opens `/settings/notifications`.

**Pre-conditions:** Valid JWT.

**Steps:**
1. FE calls `GET /api/v1/notification-preferences`.
2. Backend resolves user from JWT `sub`.
3. Service looks up rows in `notification_preferences` filtered by `user_id`.
4. For every `NotificationType` in the enum, if no row exists, service synthesizes a default row per BR-NOTIF-005/006.
5. Response = full list of `NotificationPreferenceDto` (one per type, 7 rows in Phase 1).

**Post-conditions:** UI renders 7 rows; mandatory types (BR-NOTIF-005) marked read-only.

**Errors:** none expected (idempotent read; defaults synthesized for missing rows).

**FE behavior:** Settings tab "Notifications" sub-page lists each `NotificationType` with channel toggles. SMS/ZALO/PUSH toggles are disabled with tooltip "Sắp ra mắt — GAP-063b".

## UC-NOTIF-PREFERENCE-EDIT

**Actor:** Authenticated User.

**Trigger:** User toggles EMAIL on/off for a non-mandatory `NotificationType`.

**Pre-conditions:** Valid JWT; type is not in mandatory list (BR-NOTIF-008).

**Steps:**
1. FE calls `PATCH /api/v1/notification-preferences/{notificationType}` with `{ enabledChannels: ["EMAIL"] }` or `{ enabledChannels: [] }`.
2. Backend resolves user, validates type is not mandatory.
3. Service upserts the row in `notification_preferences`.
4. Service logs `notification.preference.changed` (BR-NOTIF-011).
5. Response = updated `NotificationPreferenceDto`.

**Post-conditions:** Row exists in DB; future producers consult it.

**Errors:**
- HTTP 400 `MANDATORY_TYPE_CANNOT_BE_DISABLED` — if user attempts to disable EMAIL on a mandatory type (BR-NOTIF-005, BR-NOTIF-008).
- HTTP 400 `INVALID_NOTIFICATION_TYPE` — if `notificationType` path var is unknown.
- HTTP 400 `INVALID_CHANNEL_TYPE` — if request body contains a channel not in the enum.
- HTTP 401 — JWT missing/invalid.

**FE behavior:** Toggle is optimistic-update; on error rollback + toast in Vietnamese: "Không thể tắt loại thông báo bắt buộc."

## UC-NOTIF-CHANNEL-SEND (existing producer migration, Phase 1)

**Actor:** Internal — any service producing a notification (e.g., trial reminder scheduler).

**Trigger:** Domain event (trial expiring, payment failed, attendance recorded).

**Pre-conditions:** Recipient user exists.

**Steps (Phase 1 simplified):**
1. Producer calls `NotificationChannel#send(recipient, message, ctx)` via the EMAIL implementation (`SESEmailService`).
2. Adapter routes to existing SES/SMTP/mock provider per existing `email.provider` config.
3. (Phase 2 only — deferred): a dispatcher will look up `notification_preferences` row, choose enabled channels, fan out across adapters with fallback chain.

**Post-conditions:** Email sent (or mock-logged in dev).

**Errors:** Same as existing email flow — `EmailResponse.status = "FAILED"` with error message; producer logs and continues.

**FE behavior:** N/A (server-to-server only).

## Out-of-scope (Phase 2 → GAP-063b)

- Channel selection by preference (Phase 1 just sends EMAIL via legacy callers)
- SMS adapter
- Zalo ZNS adapter
- Quiet hours
- Cost tracking
- Fallback chain
- Push notifications

## Log

- **2026-05-04** — Use cases drafted for Wave 18a Bucket B Phase 1.
