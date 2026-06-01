# Beta Access — API Contract

**Domain:** Beta tenant invite mechanism (Wave 33 — GAP-372 + Wave 35 PDPL consent — GAP-385)
**Source-of-truth controller:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/controller/BetaAccessController.java`
**Last verified:** 2026-05-16 (Wave 86 — GAP-584 AC#2 origin Cache-Control wiring)

---

## Cache-Control policy (Wave 86 GAP-584 AC#2)

All `/api/v1/auth/beta-signup/**` endpoints (and any future `/api/v1/auth/{magic,invite}/**` siblings) MUST include these response headers per `MagicLinkCacheControlInterceptor`:

```
Cache-Control: no-store, no-cache, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
```

Origin defense-in-depth pairing the edge layer Cloudflare Page Rule (`cache_level=bypass`). Prevents any intermediate cache (browser, CDN, proxy) from storing a single-use invite token response.

This contract is the cross-layer source-of-truth consumed by:
- BE Bucket B (GAP-385) — `BetaRequestDto` + `BetaAccessRequest` entity + V32 migration
- FE Bucket B (GAP-385) — `BetaRequestForm.tsx` consent checkbox
- BE Bucket A (GAP-384) — `@PreAuthorize` admin guards on `/admin/beta-requests/*`
- BE Bucket D (GAP-387) — Micrometer counters wrapping these endpoints

---

## Endpoints

### POST /api/v1/auth/request-beta-access

**Use case:** UC-BETA-001 — Submit beta access request (PDPL consent required)
**Auth:** Public (unauthenticated). Honeypot field MUST be empty. Rate-limit per IP enforced at gateway.

**Request body (`BetaRequestDto`):**
```json
{
  "email": "owner@example.edu.vn",
  "name": "Nguyễn Văn A",
  "orgName": "Trung tâm Anh ngữ ABC",
  "persona": "P2_CENTER_OWNER",
  "referralSource": "google",
  "honeypot": "",
  "consentGiven": true
}
```

**Field constraints:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `email` | string | yes | RFC-5321, ≤320 chars, `@Email` |
| `name` | string | yes | ≤200 chars, non-blank |
| `orgName` | string | yes | ≤200 chars, non-blank |
| `persona` | enum | yes | `P1_SOLO_TEACHER` \| `P2_CENTER_OWNER` (Wave 35 Phase 1 BETA scope) |
| `referralSource` | string | optional | ≤500 chars |
| `honeypot` | string | yes (empty) | MUST equal `""` (anti-bot trap) |
| `consentGiven` | boolean | yes | MUST be `true` (PDPL Art 11 — explicit consent) |

**Response 201 Created (`BetaRequestResponse`):**
```json
{
  "id": 12345,
  "email": "owner@example.edu.vn",
  "name": "Nguyễn Văn A",
  "orgName": "Trung tâm Anh ngữ ABC",
  "persona": "P2_CENTER_OWNER",
  "referralSource": "google",
  "status": "PENDING",
  "createdAt": "2026-05-08T10:23:00Z",
  "approvedAt": null,
  "rejectedAt": null,
  "rejectionReason": null
}
```

**Errors:**

| HTTP | Error code | Trigger |
|------|------------|---------|
| 400 | `BETA_CONSENT_REQUIRED` | `consentGiven` is `false`, `null`, or missing — PDPL 2023 violation |
| 400 | `BETA_INVALID_EMAIL` | `email` fails `@Email` validation |
| 400 | `BETA_INVALID_PERSONA` | `persona` not in allowed enum |
| 400 | `BETA_HONEYPOT_FILLED` | `honeypot` non-empty (silently rejected by service; surface only in tests) |
| 400 | `BETA_INVALID_INPUT` | `name` / `orgName` / `referralSource` contains HTML structural characters (`<`, `>`, `&`) — XSS hardening per BR-BETA-005 (Wave 105 Bucket A failure-mode A4) |
| 409 | `BETA_DUPLICATE_EMAIL` | Active PENDING/APPROVED request exists for the email |
| 429 | `RATE_LIMITED` | Per-IP rate limit at gateway exceeded |

**Audit log:** A successful 201 emits `beta.consent.given` event via outbox (Bucket B wires this; consumed by audit/analytics).

---

### GET /api/v1/auth/beta-signup/validate

**Use case:** UC-BETA-002 — Validate invite token (signup pre-fill)
**Auth:** Public unauthenticated.

**Query params:** `?token=<UUID>`

**Response 200 OK (token valid):**
```json
{
  "valid": true,
  "email": "owner@example.edu.vn",
  "name": "Nguyễn Văn A",
  "persona": "P2_CENTER_OWNER",
  "expiresAt": "2026-05-09T10:23:00Z"
}
```

**Response 404 Not Found (invalid/expired):**
```json
{ "valid": false, "errorCode": "TOKEN_NOT_FOUND" }
```

`errorCode` phân biệt rõ từng tình huống lỗi (GAP-610 — sửa lifecycle-collapse):

| errorCode | Ý nghĩa |
|---|---|
| `TOKEN_NOT_FOUND` | Không có row nào khớp token (chưa cấp token, hoặc token đã bị xóa sau khi đăng ký xong) |
| `TOKEN_NOT_APPROVED` | Row tồn tại nhưng status là `PENDING`/`REJECTED`/`ABORTED` (yêu cầu beta chưa được duyệt) |
| `TOKEN_EXPIRED` | Row `APPROVED` nhưng token đã hết hạn (TTL 24h) |
| `ALREADY_USED` | Row đã `SIGNED_UP` (token đã được dùng để đăng ký) |

Trước GAP-610, cả `PENDING`/`REJECTED`/`ABORTED` đều trả `TOKEN_NOT_FOUND` — operator/UI không phân biệt được "không có row" với "row sai trạng thái".

---

### POST /api/v1/auth/beta-signup

**Use case:** UC-BETA-003 — Complete beta signup with invite token
**Auth:** Public unauthenticated; token-redemption.

**Request body (`BetaSignupCommand`):**
```json
{
  "token": "uuid-v4",
  "password": "...",
  "acceptTos": true
}
```

**Response 200 OK:** `BetaRequestResponse` (status flipped to `SIGNED_UP`).

**Errors:** `404` invalid token; `409` already signed up.

---

### GET /api/v1/admin/beta-requests

**Use case:** UC-BETA-004 — Coordinator listing
**Auth:** **Bearer + role `PLATFORM_ADMIN`** (Bucket A — GAP-384 ships `@PreAuthorize`)

**Query params:** `?status=PENDING&page=0&size=20` — giá trị hợp lệ cho `status`: `PENDING` | `APPROVED` | `REJECTED` | `SIGNED_UP` | `ABORTED`

**Response 200 OK (`BetaRequestPage`):**
```json
{
  "content": [ /* BetaRequestResponse[] */ ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

**Errors:** `401` unauthenticated; `403` non-admin authenticated user.

---

### POST /api/v1/admin/beta-requests/{id}/approve

**Use case:** UC-BETA-005 — Coordinator approves a pending request
**Auth:** **Bearer + role `PLATFORM_ADMIN`** (Bucket A)

**Request body (`BetaApproveCommand`):**
```json
{ "noteToRequester": "Welcome to the Phase 1 BETA cohort" }
```

**Response 200 OK:** `BetaRequestResponse` (status `APPROVED`, invite token issued, `+24h` expiry).
**Side effects:** Invite-sent event published via outbox (existing Wave 33 emitter).

**Errors:** `401`/`403` auth; `404` not found; `409` not in PENDING state.

---

### POST /api/v1/admin/beta-requests/{id}/reject

**Use case:** UC-BETA-006 — Coordinator rejects a pending request
**Auth:** **Bearer + role `PLATFORM_ADMIN`** (Bucket A)

**Request body (`BetaRejectCommand`):**
```json
{ "rejectionReason": "Out of Phase 1 scope (K-12)" }
```

**Response 200 OK:** `BetaRequestResponse` (status `REJECTED`).

**Errors:** `401`/`403`; `404` not found; `409` not in PENDING state.

---

## Status enum (`BetaAccessRequestStatus`)

| Value | Meaning |
|-------|---------|
| `PENDING` | Submitted, awaiting coordinator review |
| `APPROVED` | Coordinator approved, invite token issued, awaiting signup |
| `REJECTED` | Coordinator rejected (terminal) |
| `SIGNED_UP` | Token redeemed, tenant provisioning kicked off (terminal) |
| `ABORTED` | Tự động hủy bởi scheduler sau `kitehub.beta-access.abort-threshold-hours` giờ không có coordinator action (terminal) — BR-BETA-004, UC-BETA-007 |

---

## PDPL 2023 consent specifics (Bucket B scope)

- `consentGiven=true` is mandatory at submit time. Server-side validation MUST reject `false`/`null`/missing with `BETA_CONSENT_REQUIRED`.
- Persisted column: `consent_given BOOLEAN NOT NULL` + `consent_at TIMESTAMP WITH TIME ZONE NOT NULL` (set to `now()` at insert).
- The FE form MUST display a labeled checkbox linked to `/legal/privacy` and `/legal/terms` (or current placeholders) and MUST disable the submit button until checked.
- Audit log entry `beta.consent.given` is emitted via the existing per-module outbox emitter (Wave 33 pattern) on the same transaction as the insert.

---

## Related

- BR-BETA-001..004: `documents/01-business/kitehub/beta-access/rules.md`
- UC-BETA-001..007: `documents/01-business/kitehub/beta-access/use-cases.md`
- Source DTOs: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/`
- Wave 35 plan: `documents/03-planning/waves/wave-2026-05-08-35-audit-p0-blockers-sprint.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
