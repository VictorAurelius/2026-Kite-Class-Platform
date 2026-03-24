# Subscription & Billing — API Contract

## POST /api/platform/subscriptions
**Use case:** UC-SUB-01
**Auth:** Bearer token (Owner)
**Request:**
```json
{
  "instanceId": "uuid",
  "tier": "BASIC",
  "billingCycle": "MONTHLY",
  "autoRenew": true
}
```
**Response 201:**
```json
{
  "id": "uuid",
  "instanceId": "uuid",
  "tier": "BASIC",
  "billingCycle": "MONTHLY",
  "status": "ACTIVE",
  "autoRenew": true,
  "startsAt": "2026-03-24T00:00:00Z",
  "expiresAt": "2026-04-24T00:00:00Z"
}
```
**Errors:** 400 FREE tier, 409 duplicate active subscription

---

## GET /api/platform/subscriptions/{id}
**Auth:** Bearer token
**Response 200:** SubscriptionResponse object
**Errors:** 404 not found

---

## GET /api/platform/subscriptions/instance/{instanceId}/active
**Use case:** UC-SUB-01 (check current state)
**Auth:** Bearer token
**Response 200:** Active SubscriptionResponse
**Errors:** 404 no active subscription

---

## GET /api/platform/subscriptions/instance/{instanceId}
**Auth:** Bearer token
**Response 200:** `[SubscriptionResponse]` (all subscriptions, including history)

---

## PATCH /api/platform/subscriptions/{id}/upgrade
**Use case:** UC-SUB-02
**Auth:** Bearer token (Owner)
**Request:**
```json
{ "newTier": "PREMIUM" }
```
**Response 200:** Updated SubscriptionResponse
**Errors:** 400 invalid tier direction, 404 not found

---

## PATCH /api/platform/subscriptions/{id}/downgrade
**Use case:** UC-SUB-03
**Auth:** Bearer token (Owner)
**Request:**
```json
{ "newTier": "BASIC" }
```
**Response 200:** Updated SubscriptionResponse with pendingTier set

---

## DELETE /api/platform/subscriptions/{id}
**Use case:** UC-SUB-04
**Auth:** Bearer token (Owner)
**Request params:** `?immediate=false` (default: end-of-cycle)
**Response 204:** No content

---

## POST /api/platform/subscriptions/{id}/renew
**Use case:** UC-SUB-05
**Auth:** Bearer token (Owner)
**Response 204:** No content
**Errors:** 404 not found

---

## GET /api/platform/subscriptions/expiring
**Auth:** Bearer token (Admin)
**Response 200:** `[SubscriptionResponse]` (expiring in next 30 days)

---

## Note: UC-SUB-06 — Automated Expiration Scheduler (no HTTP endpoint)

`SubscriptionExpirationChecker` runs daily (scheduler-triggered, no endpoint):

| Time | Action |
|------|--------|
| 9 AM | Scan ACTIVE subscriptions expiring in 7/3/1 days → send `renewal-reminder` emails |
| 10 AM | Mark expired ACTIVE subscriptions → `EXPIRED` |
| 10 AM | Suspend instances if grace period (3 days) elapsed (SUB-04) |

Monitor via `GET /api/platform/subscriptions/expiring` and instance status.
