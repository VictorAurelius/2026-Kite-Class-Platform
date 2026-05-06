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
- BR-PDPL-DSAR-001..005 in [`rules.md`](./rules.md) (Wave 26 Bucket A)
- DR-03 retention rule (`documents/01-business/kitehub/data-retention/rules.md`)
- GAP-353b (parent gap)
- GAP-353c (DSAR self-service — see §8 below, shipped Wave 26 Bucket A)

---

## 8. DSAR self-service endpoints (GAP-353c, Wave 26 Bucket A)

Self-service intake for the six PDPL Art 14 data-subject rights. Both endpoints
are public + unauthenticated by design — DSAR submitters are not logged-in users
by definition. Identity verification happens out-of-band via the
`nationalIdLast4` + DPO callback (see `BR-PDPL-DSAR-003`).

### 8.1 `POST /api/v1/dsar/request`

Submit a fresh DSAR ticket. Idempotency is intentionally NOT applied — every call
creates a new ticket so duplicate submissions surface as separate audit records;
DPO triage merges duplicates manually.

#### 8.1.1 Request body

```json
{
  "rightType":        "ACCESS | RECTIFICATION | ERASURE | PORTABILITY | RESTRICT | OBJECT",
  "requesterEmail":   "string (max 320, valid email — required)",
  "requesterName":    "string (max 200, required)",
  "nationalIdLast4":  "string (4 digits, required)",
  "scope":            "string (max 4000, optional)",
  "reason":           "string (max 4000, optional)",
  "contactPreference":"string (max 50, optional — e.g. 'email', 'phone')",
  "companyWebsite":   "string (max 500, optional honeypot — must be empty)"
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `rightType` | ✅ | Enum mirrors PDPL Art 14 — see `BR-PDPL-DSAR-001` |
| `requesterEmail` | ✅ | RFC-5321 email; full address — needed for DPO callback |
| `requesterName` | ✅ | Free text, no PII shape constraint beyond length |
| `nationalIdLast4` | ✅ | Exactly 4 digits — full ID never collected (BR-PDPL-DSAR-003) |
| `scope` | ❌ | Optional free text; helps DPO scope the response |
| `reason` | ❌ | Optional free text |
| `contactPreference` | ❌ | Free text; FE limits to `email` / `phone` |
| `companyWebsite` | ❌ | Honeypot. Server rejects HTTP 400 when non-empty (BR-PDPL-DSAR-005) |

#### 8.1.2 Response — 201 Created

```json
{
  "ticketId":     "uuid (v4 server-generated)",
  "rightType":    "ACCESS",
  "status":       "PENDING",
  "slaDeadline":  "2026-05-26T07:30:00Z (created_at + 20 days)",
  "createdAt":    "2026-05-06T07:30:00Z",
  "resolvedAt":   null
}
```

The response is intentionally redacted — no `requesterEmail`, no
`nationalIdLast4`, no `resolution`. Public-safe view only; full ticket data
lives behind DPO callback.

#### 8.1.3 Error responses

| Status | When |
|--------|------|
| 400 | Validation failure (missing required field, invalid email, `nationalIdLast4` not 4 digits, honeypot populated) |
| 429 | Rate-limit at the gateway (out of scope for this contract) |
| 500 | Persistence failure |

### 8.2 `GET /api/v1/dsar/{ticketId}`

Public status lookup. Requesters keep `ticketId` from §8.1.2 to poll status.

#### 8.2.1 Path parameter

| Name | Notes |
|------|-------|
| `ticketId` | UUID v4 returned by §8.1.2 |

#### 8.2.2 Response — 200 OK

Same shape as §8.1.2 with current `status` and possibly populated `resolvedAt`.

#### 8.2.3 Error responses

| Status | When |
|--------|------|
| 404 | No ticket exists for that UUID |
| 400 | `ticketId` is not a valid UUID |

### 8.3 SLA + retention

| Concern | Window | Owner |
|---------|--------|-------|
| Response SLA (PDPL Art 14 / Decree 13/2023 Art 19) | 20 days from `createdAt` | DPO; `SlaTimerCron` flags overdue daily 04:00 |
| Ticket retention (BR-PDPL-DSAR-004 / DR-03) | 36 months from `resolvedAt` | Future retention cron (follow-up gap) |
