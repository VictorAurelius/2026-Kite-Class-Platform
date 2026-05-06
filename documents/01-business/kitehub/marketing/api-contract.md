# KiteHub Marketing — API Contract

**Owner:** kitehub-subscription module
**Wave:** Wave 25 Bucket A — GAP-353b (PDPL Phase 2 server consent API)
**Last updated:** 2026-05-06

This document mirrors the runtime endpoints implemented in
`kitehub-subscription/src/main/java/com/kitehub/subscription/consent/controller/ConsentController.java`.
Behavioural rules live in [`rules.md`](./rules.md).

---

## 1. Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | POST | `/api/v1/consent/record` | none (public — pseudonymous) | Record or update consent for a visitor |
| 2 | GET | `/api/v1/consent/{visitorId}` | none | Read latest consent record for visitor |
| 3 | POST | `/api/v1/consent/{visitorId}/revoke` | none | Revoke consent (PDPL Art 13(1)) |

**Auth model:** `visitorId` is a pseudonymous UUID v4 generated client-side and persisted in
LocalStorage (`kite_visitor_id`). It carries no user identity by itself; abuse mitigation lives at
the gateway (rate-limit / WAF), not at the application layer.

---

## 2. POST `/api/v1/consent/record`

Idempotent upsert keyed on `visitorId`. If an active (non-revoked, non-expired) record exists
for the visitor, it is updated in place; otherwise a new row is inserted. Re-posting after a
revoke creates a NEW row to keep the audit trail honest.

### 2.1 Request

```http
POST /api/v1/consent/record HTTP/1.1
Content-Type: application/json
User-Agent: <browser ua>
X-Forwarded-For: <client ip>          # optional — gateway-injected

{
  "visitorId": "11111111-2222-4333-8444-555555555555",
  "userId": null,
  "tenantId": null,
  "essentialConsented": true,
  "analyticsConsented": true,
  "marketingConsented": false,
  "consentVersion": 1,
  "ipAddress": null,
  "userAgent": null
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `visitorId` | UUID | yes | Pseudonymous client-generated id |
| `userId` | integer | no | Populated after login |
| `tenantId` | UUID | no | Marketing-surface visitors pre-tenant = `null` |
| `essentialConsented` | boolean | no | Server coerces to `true` per BR-PDPL-CONSENT-001 |
| `analyticsConsented` | boolean | yes | — |
| `marketingConsented` | boolean | yes | — |
| `consentVersion` | integer | no | Defaults to `1`. Bump on schema change |
| `ipAddress` | string ≤45 | no | Auto-filled from `X-Forwarded-For` when omitted |
| `userAgent` | string ≤4096 | no | Auto-filled from `User-Agent` header when omitted |

### 2.2 Response — 201 Created

```json
{
  "id": 42,
  "visitorId": "11111111-2222-4333-8444-555555555555",
  "userId": null,
  "tenantId": null,
  "essentialConsented": true,
  "analyticsConsented": true,
  "marketingConsented": false,
  "consentVersion": 1,
  "createdAt": "2026-05-06T05:00:00Z",
  "updatedAt": "2026-05-06T05:00:00Z",
  "expiresAt": "2027-05-06T05:00:00Z",
  "revokedAt": null,
  "revoked": false
}
```

`expiresAt = createdAt + 12 months` per BR-PDPL-CONSENT-002 (re-prompt cadence). Distinct from
the DR-03 36-month retention enforced by the daily cron.

### 2.3 Error responses

| Status | Code | When |
|--------|------|------|
| 400 | `VALIDATION_ERROR` | `visitorId`, `analyticsConsented`, or `marketingConsented` missing/null/invalid UUID |

---

## 3. GET `/api/v1/consent/{visitorId}`

Read latest consent record for the given visitor (most-recent-first by `createdAt`).

### 3.1 Request

```http
GET /api/v1/consent/11111111-2222-4333-8444-555555555555 HTTP/1.1
```

### 3.2 Response — 200 OK

Same body shape as §2.2.

### 3.3 Error responses

| Status | When |
|--------|------|
| 404 | No record exists for `visitorId` |
| 400 | `visitorId` is not a valid UUID |

---

## 4. POST `/api/v1/consent/{visitorId}/revoke`

Revoke consent — PDPL Art 13(1) right to withdraw. Sets `revokedAt = now`, flips
`analyticsConsented + marketingConsented` to `false`. Idempotent: re-revoking returns the
existing revoked record unchanged.

### 4.1 Request

```http
POST /api/v1/consent/11111111-2222-4333-8444-555555555555/revoke HTTP/1.1
```

### 4.2 Response — 200 OK

Same body shape as §2.2 with `revoked: true` and `revokedAt` populated.

### 4.3 Error responses

| Status | When |
|--------|------|
| 404 | No prior record exists for `visitorId` (cannot revoke what was never given) |
| 400 | `visitorId` is not a valid UUID |

---

## 5. Frontend integration

The shared `useConsent` hook (`packages/shared-ui/src/components/ConsentBanner/useConsent.ts`)
consumes these endpoints best-effort: LocalStorage stays the primary truth so offline / API
outages never block the user, but every state change (`give` / `reject` / `revoke`) fires a
fire-and-forget API call so cross-device records stay synced.

The wrapper (`api.ts`) NEVER throws — failures resolve to `false`/`null` so callers can ignore
them.

---

## 6. Retention

| Concern | Window | Owner |
|---------|--------|-------|
| Re-prompt expiry | 12 months from `createdAt` | Banner (FE) checks `expiresAt < now` |
| DR-03 hard delete | 36 months from `createdAt` | `ConsentRetentionCron` (daily 03:00) |

---

## 7. Related

- BR-PDPL-CONSENT-001..004 in [`rules.md`](./rules.md)
- DR-03 retention rule (`documents/01-business/kitehub/data-retention/rules.md`)
- GAP-353b (parent gap)
- GAP-353c (DSAR self-service — separate endpoint family, future)
