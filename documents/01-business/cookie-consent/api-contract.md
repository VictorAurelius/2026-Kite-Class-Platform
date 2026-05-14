# Cookie Consent — API Contract

**Domain:** PDPL-compliant cookie consent persistence + withdrawal (Wave 79 Bucket B — GAP-558)
**Source-of-truth controller:** (planned) `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consent/ConsentController.java`
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)

This contract là source-of-truth cross-layer cho Wave 79 Bucket B, consumed by:
- Bucket B (GAP-558) — `kitehub-frontend/src/components/CookieConsent.tsx` (FE banner)
- Bucket B (GAP-558) — BE `ConsentController` + `CookieConsent` entity + Flyway migration `V[N]__create_cookie_consents.sql`
- MSW handler `kitehub-frontend/src/test/msw/handlers/cookie-consent.ts` (this PR — Bucket 0)

---

## Endpoints overview

3 endpoints. All under canonical path `/api/v1/consent/cookie/*`. No alias path (greenfield).

| Method | Path | Auth | Use case |
|--------|------|------|----------|
| POST | `/api/v1/consent/cookie` | Public (optional Bearer if authenticated user) | UC-COOKIE-CONSENT-RECORD |
| GET | `/api/v1/consent/cookie/{cookieId}` | Public (cookie ID as anonymous identifier) | UC-COOKIE-WITHDRAW (read current state) |
| PUT | `/api/v1/consent/cookie/{cookieId}` | Public + cookie ID match | UC-COOKIE-WITHDRAW (partial update) |
| DELETE | `/api/v1/consent/cookie/{cookieId}` | Public + cookie ID match | UC-COOKIE-WITHDRAW (full withdraw) |

---

## POST /api/v1/consent/cookie

**Use case:** UC-COOKIE-CONSENT-RECORD
**Auth:** Public; optional `Authorization: Bearer <accessToken>` để link với user account.

**Request body (`CookieConsentRequest`):**
```json
{
  "cookieId": "550e8400-e29b-41d4-a716-446655440000",
  "categories": {
    "essential": true,
    "functional": true,
    "analytics": false
  },
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ...",
  "language": "vi-VN"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `cookieId` | string (UUID v4) | yes | RFC 4122 v4 UUID format. FE generated. |
| `categories` | object | yes | Keys MUST be subset of `{essential, functional, analytics}` whitelist. |
| `categories.essential` | boolean | yes | MUST be `true` (BR-COOKIE-002). |
| `categories.functional` | boolean | yes | User choice. |
| `categories.analytics` | boolean | yes | User choice. |
| `userAgent` | string | optional | Truncate ≤200 chars server-side. |
| `language` | string | optional | BCP-47 tag (vi-VN, en-US). Server-side validate against whitelist. |

**Response 201 Created (`CookieConsentResponse`):**
```json
{
  "cookieId": "550e8400-e29b-41d4-a716-446655440000",
  "categoriesAccepted": {
    "essential": true,
    "functional": true,
    "analytics": false
  },
  "expiresAt": "2027-05-14T09:00:00Z",
  "status": "ACTIVE"
}
```

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `INVALID_CATEGORY` | `categories` keys không thuộc whitelist |
| 400 | `INVALID_ESSENTIAL_CONSENT` | `essential != true` |
| 400 | `INVALID_COOKIE_ID` | UUID format invalid |
| 409 | `CONSENT_ALREADY_RECORDED` | `cookieId` đã có active consent |
| 429 | `RATE_LIMITED` | Gateway rate limit (30 req/min/IP) |

---

## GET /api/v1/consent/cookie/{cookieId}

**Use case:** UC-COOKIE-WITHDRAW (read current consent state cho modal)
**Auth:** Public; `cookieId` path param identifies record.

**Path params:**

| Param | Type | Validation |
|-------|------|------------|
| `cookieId` | string (UUID v4) | RFC 4122 v4 |

**Response 200 OK (`CookieConsentResponse`):**
```json
{
  "cookieId": "550e8400-e29b-41d4-a716-446655440000",
  "categoriesAccepted": {
    "essential": true,
    "functional": true,
    "analytics": false
  },
  "createdAt": "2026-05-14T09:00:00Z",
  "expiresAt": "2027-05-14T09:00:00Z",
  "status": "ACTIVE"
}
```

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 404 | `CONSENT_NOT_FOUND` | `cookieId` không tồn tại |
| 410 | `CONSENT_EXPIRED` | `expires_at < now` |
| 429 | `RATE_LIMITED` | Gateway rate limit |

---

## PUT /api/v1/consent/cookie/{cookieId}

**Use case:** UC-COOKIE-WITHDRAW (partial update — update categories without revoking entire consent record)
**Auth:** Public; `cookieId` path param.

**Path params:**

| Param | Type | Validation |
|-------|------|------------|
| `cookieId` | string (UUID v4) | RFC 4122 v4 |

**Request body (`CookieConsentUpdateRequest`):**
```json
{
  "categories": {
    "essential": true,
    "functional": false,
    "analytics": false
  }
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `categories` | object | yes | Same constraints as POST. `essential` MUST stay `true`. |

**Response 200 OK (`CookieConsentResponse`):**
Same shape as POST 201.

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `INVALID_CATEGORY` / `INVALID_ESSENTIAL_CONSENT` | Same as POST |
| 404 | `CONSENT_NOT_FOUND` | Cookie ID không tồn tại |
| 410 | `CONSENT_ALREADY_WITHDRAWN` | Status đã `WITHDRAWN` |
| 410 | `CONSENT_EXPIRED` | `expires_at < now` |
| 429 | `RATE_LIMITED` | Gateway rate limit |

---

## DELETE /api/v1/consent/cookie/{cookieId}

**Use case:** UC-COOKIE-WITHDRAW (full withdraw)
**Auth:** Public; `cookieId` path param.

**Path params:** Same as PUT.

**Request body:** (empty)

**Response 204 No Content** — withdraw success.

BE side effects:
- Soft-delete: `cookie_consents.status = 'WITHDRAWN'`, `withdrawn_at = now`.
- Preserve row cho audit trail (per `pre-launch-owasp-rest-hardening-checklist.md` §2.8 A09).

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 404 | `CONSENT_NOT_FOUND` | Cookie ID không tồn tại |
| 410 | `CONSENT_ALREADY_WITHDRAWN` | Đã withdrawn rồi (idempotent — return 410 thay vì 204 để FE biết replay) |
| 429 | `RATE_LIMITED` | Gateway rate limit |

---

## Rate limits

Per BR-COOKIE-005 + `pre-launch-auth-hardening-checklist.md` §2.1:

| Endpoint | Limit | Key resolver |
|----------|-------|--------------|
| POST `/api/v1/consent/cookie` | 30 req/min/IP | ipKeyResolver |
| GET `/api/v1/consent/cookie/{id}` | 60 req/min/IP | ipKeyResolver |
| PUT `/api/v1/consent/cookie/{id}` | 30 req/min/IP | ipKeyResolver |
| DELETE `/api/v1/consent/cookie/{id}` | 30 req/min/IP | ipKeyResolver |

Exceed → `429 RATE_LIMITED` với header `Retry-After: 60`.

---

## CORS

Banner load on public `kitehub.me` + tenant subdomains. CORS allow:
- `https://kitehub.me`
- `https://*.kitehub.me` (tenant subdomains)
- `http://localhost:3000` (dev)

Methods: `POST, GET, PUT, DELETE, OPTIONS`.
Headers: `Content-Type, Authorization`.

---

## Audit logging

Per `pre-launch-owasp-rest-hardening-checklist.md` §2.8 A09, every cookie consent action logged structured:
```json
{
  "timestamp": "2026-05-14T09:00:00Z",
  "action": "CONSENT_GRANTED" | "CONSENT_UPDATED" | "CONSENT_WITHDRAWN",
  "cookieId": "uuid",
  "userId": "uuid or null",
  "tenantId": "uuid or null",
  "categoriesBefore": { ... } | null,
  "categoriesAfter": { ... },
  "requestIp": "1.2.3.0/24 (truncated)",
  "userAgent": "(truncated)"
}
```

PII handling:
- `request_ip` truncated to /24 (IPv4) hoặc /48 (IPv6) per PDPL data minimization.
- `user_agent` truncated 200 chars.
- `email`, name, etc. NOT collected by consent flow (cookie identifier only).

---

## Data retention

`cookie_consents` table retention:
- ACTIVE rows: until `expires_at` (12 months after create).
- WITHDRAWN rows: 12 months sau `withdrawn_at` cho audit trail, then hard-delete via cron job.

Retention rule cross-reference: BR-TENANT-DATA-RETENTION (general retention policy) — cookie consents inherit similar 12-month default per BR-COOKIE-003.

---

## Side effects

- POST consent với `analytics=true` → emit `consent.granted.analytics` event qua outbox (subscribers: backend analytics gateway enable for `cookieId` for 12 months; mainly informational).
- PUT toggle `analytics=true → false` → emit `consent.revoked.analytics` event (subscribers: analytics gateway flag user-data deletion request if retention rule mandates).
- DELETE → emit `consent.withdrawn` event với `cookieId` (subscribers: data deletion pipeline — PDPL Art 17 right to deletion).

---

## Related

- BR-COOKIE-001..005: `documents/01-business/cookie-consent/rules.md`
- UC-COOKIE-{BANNER-DISPLAY,CONSENT-RECORD,WITHDRAW,GRANULAR-CATEGORY-TOGGLE}: `documents/01-business/cookie-consent/use-cases.md`
- Wave 79 plan: `documents/03-planning/waves/wave-2026-05-14-79-beta-invite-close-out.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
- PDPL compliance: `pre-launch-owasp-rest-hardening-checklist.md` §2.8 A09
- Source migration (planned): Wave 79 Bucket B `V[N]__create_cookie_consents.sql`
- MSW handler: `kitehub/kitehub-frontend/src/test/msw/handlers/cookie-consent.ts` (this PR — Bucket 0)
- Gap: GAP-558 (this contract closes P0 PDPL deadline 2026-07-01 prerequisite)
