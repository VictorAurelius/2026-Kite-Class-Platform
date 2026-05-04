# Notification Preferences — API Contract

**Last verified:** 2026-05-04
**Wave:** 18a Bucket B (GAP-063 Phase 1)
**Base path:** `/api/v1/notification-preferences`
**Auth:** JWT (Bearer); user resolved from `sub` claim per BR-NOTIF-009.

---

## GET `/api/v1/notification-preferences`

List the authenticated user's preferences (synthesized for missing rows per BR-NOTIF-005/006).

**Request:** no body. Headers: `Authorization: Bearer <jwt>`.

**Response 200:**

```json
{
  "preferences": [
    {
      "notificationType": "BILLING_INVOICE",
      "enabledChannels": ["EMAIL"],
      "mandatory": true
    },
    {
      "notificationType": "ABSENCE",
      "enabledChannels": ["EMAIL"],
      "mandatory": false
    }
    // ... 7 rows total in Phase 1
  ]
}
```

**Response 401:** missing/invalid JWT.

---

## PATCH `/api/v1/notification-preferences/{notificationType}`

Update the channel set for a single `NotificationType`.

**Path variable:** `notificationType` ∈ `ABSENCE | FEE_REMINDER | EXAM_RESULT | TRIAL_ENDING | BILLING_INVOICE | SECURITY_ALERT | GENERAL_ANNOUNCEMENT`

**Request:**

```json
{
  "enabledChannels": ["EMAIL"]
}
```

`enabledChannels` is a set (de-duplicated). Phase 1: only `EMAIL` activates a real send. `SMS`, `ZALO`, `PUSH` accepted in payload (forward-compatible) but logged as `channel.disabled.in.phase1` and skipped at send time per BR-NOTIF-010.

**Response 200:**

```json
{
  "notificationType": "ABSENCE",
  "enabledChannels": ["EMAIL"],
  "mandatory": false
}
```

**Errors:**

| HTTP | Code | When |
|------|------|------|
| 400 | `MANDATORY_TYPE_CANNOT_BE_DISABLED` | Trying to remove `EMAIL` from a mandatory type (BILLING_INVOICE / SECURITY_ALERT / TRIAL_ENDING). |
| 400 | `INVALID_NOTIFICATION_TYPE` | `notificationType` path var not in enum. |
| 400 | `INVALID_CHANNEL_TYPE` | `enabledChannels` contains unknown value. |
| 401 | — | Missing/invalid JWT. |

---

## Error envelope

All 4xx errors share the standard envelope:

```json
{
  "errorCode": "MANDATORY_TYPE_CANNOT_BE_DISABLED",
  "message": "Loại thông báo bắt buộc không thể tắt.",
  "timestamp": "2026-05-04T10:32:17.481Z"
}
```

## Out-of-scope (GAP-063b)

- POST/DELETE endpoints (Phase 1 = upsert via PATCH only — defaults are synthesized)
- Bulk PATCH (Phase 1 = single-type only)
- Quiet hours sub-resource
- Cost summary endpoint

## Log

- **2026-05-04** — API contract drafted for Wave 18a Bucket B Phase 1.
