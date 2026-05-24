# API Contract — Consent v2 (Immutable + hash chain)

**Wave:** beta-readiness-4 Bucket B — GAP-353b
**Service:** `kitehub-subscription`
**Path prefix:** `/api/v1/consent/v2`
**Status:** ⚠️ PARTIAL — v1 dev-implementation, counsel formal review queued Phase 2

Server-side immutable consent record với SHA-256 hash chain audit trail.
Mirrors runtime endpoints implemented in
`kitehub-subscription/src/main/java/com/kitehub/subscription/consent/immutable/ImmutableConsentController.java`.

## Distinction từ v1 path

| Aspect | v1 (`/api/v1/consent/*` Wave 25) | v2 (`/api/v1/consent/v2/*` Wave br-4) |
|--------|----------------------------------|---------------------------------------|
| Key | Pseudonymous `visitorId` UUID | Authenticated `userId` BIGINT |
| Idempotency | Upsert in-place same row | INSERT new row each call |
| Audit trail | Last-write-wins (revokedAt flag) | Append-only hash chain |
| RLS | None | UPDATE + DELETE blocked at DB level |
| Schema | 3 boolean columns | JSONB `granted` (flex) |
| Use case | Pre-login banner stage | Post-login authenticated capture |

Co-exist không conflict: pre-login banner còn dùng v1; v2 path activate when user logs in
or admin explicit consent capture flow.

---

## 1. Endpoints

| # | Method | Path | Auth | Description |
|---|--------|------|------|-------------|
| 1 | POST | `/api/v1/consent/v2/record` | gateway-level (rate-limit) | INSERT new consent row + hash chain link |
| 2 | GET | `/api/v1/consent/v2/{userId}` | tenant-scope (admin OR owner self) | Return consent history + validate chain |
| 3 | POST | `/api/v1/consent/v2/withdraw` | gateway-level | INSERT new row với analytics+marketing=false (PDPL Art 14) |

---

## 2. POST `/api/v1/consent/v2/record`

INSERT new row into immutable `consent_record_immutable` table. Computes
`current_hash = SHA-256(prev_hash || canonical(user|tenant|granted|ip|ua|signedAt))`.
`prev_hash` = latest row's `current_hash` cho cùng userId (NULL = chain head).

### 2.1 Request

```http
POST /api/v1/consent/v2/record HTTP/1.1
Content-Type: application/json
User-Agent: Mozilla/5.0 ...
X-Forwarded-For: 203.0.113.7

{
  "userId": 42,
  "tenantId": 7,
  "granted": {
    "essential": true,
    "analytics": true,
    "marketing": false
  },
  "ipAddress": null,
  "userAgent": null
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `userId` | integer (long) | yes | Soft reference; FK omitted per multi-tenant arch |
| `tenantId` | integer (long) | no | Nullable cho pre-tenant visitor |
| `granted` | object<string, boolean> | yes | Min keys `essential`+`analytics`+`marketing`; server coerces `essential=true`; extra keys (e.g., `personalization`) preserved verbatim |
| `ipAddress` | string ≤45 | no | Auto-filled from `X-Forwarded-For` then `RemoteAddr` |
| `userAgent` | string ≤4096 | no | Auto-filled from `User-Agent` header |

### 2.2 Response — 201 Created

```json
{
  "id": 42,
  "userId": 42,
  "tenantId": 7,
  "granted": {
    "essential": true,
    "analytics": true,
    "marketing": false
  },
  "prevHash": null,
  "currentHash": "5c8a9e1f3b2d4c7e8a9b0d1e2f3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c",
  "ipAddress": "203.0.113.7",
  "signedAt": "2026-05-24T05:00:00Z"
}
```

### 2.3 Errors

| HTTP | Error code | Meaning |
|------|-----------|---------|
| 400 | `VALIDATION_ERROR` | Missing `userId` / `granted` empty / `ipAddress` blank after auto-fill |
| 500 | `INTERNAL_ERROR` | DB unavailable, hash compute failure (extremely rare) |

---

## 3. GET `/api/v1/consent/v2/{userId}`

Return full consent history cho user oldest→newest. Server validates hash chain
integrity at read time; tampering detected → 500 with diagnostic message.

### 3.1 Request

```http
GET /api/v1/consent/v2/42 HTTP/1.1
```

### 3.2 Response — 200 OK

```json
{
  "userId": 42,
  "chainValid": true,
  "records": [
    {
      "id": 42,
      "userId": 42,
      "tenantId": 7,
      "granted": { "essential": true, "analytics": true, "marketing": false },
      "prevHash": null,
      "currentHash": "5c8a9e1f...",
      "ipAddress": "203.0.113.7",
      "signedAt": "2026-05-24T05:00:00Z"
    },
    {
      "id": 88,
      "userId": 42,
      "tenantId": 7,
      "granted": { "essential": true, "analytics": false, "marketing": false },
      "prevHash": "5c8a9e1f...",
      "currentHash": "8d7b6c5a...",
      "ipAddress": "203.0.113.7",
      "signedAt": "2026-05-24T05:15:00Z"
    }
  ]
}
```

### 3.3 Errors

| HTTP | Error code | Meaning |
|------|-----------|---------|
| 404 | `NOT_FOUND` | No consent records exist cho userId |
| 500 | `CHAIN_INTEGRITY_VIOLATION` | Hash chain validator detected tampering — manual audit required |

---

## 4. POST `/api/v1/consent/v2/withdraw`

INSERT new row với `granted={essential:true,analytics:false,marketing:false}`.
Per PDPL Decree 13/2023 Article 14: "rút lại sự đồng ý dễ dàng như cho đồng ý"
(withdraw must be as easy as grant — single-call API mirrors grant flow).

### 4.1 Request

```http
POST /api/v1/consent/v2/withdraw HTTP/1.1
Content-Type: application/json

{
  "userId": 42,
  "tenantId": 7,
  "ipAddress": null,
  "userAgent": null
}
```

### 4.2 Response — 201 Created

Same shape as POST `/record` response. Latest row reflects withdrawn state.

### 4.3 Errors

| HTTP | Error code | Meaning |
|------|-----------|---------|
| 400 | `VALIDATION_ERROR` | Missing `userId` |

---

## 5. Concurrency model

- SERIALIZABLE isolation + REQUIRES_NEW propagation per insert
- Service-level retry loop (3 attempts, exponential backoff 50ms→100ms→200ms)
- Concurrent inserts cùng userId: Postgres detects serialization conflict (SQLSTATE 40001) → retry catches → hash chain preserved (no fork)
- IT proof: `ConcurrentConsentWritesIT` — 2 threads × 4 inserts = 8 rows + linear chain

## 6. Sample data (VN-friendly per `vn-localization-audit-checklist.md`)

```json
{
  "userId": 42,
  "tenantId": 7,
  "granted": { "essential": true, "analytics": true, "marketing": false },
  "ipAddress": "203.0.113.7",
  "userAgent": "Mozilla/5.0 (Linux; Android 13; Trần Thị Hồng phone) Chrome/118",
  "tenantName": "Trung tâm Anh ngữ Sky Education",
  "exampleAmount": "1.500.000đ"
}
```

## 7. Related

- `rules.md` — BR-PDPL-CONSENT-001..004 reference
- `../../../02-architecture/adr/ADR-034-cookie-consent-vendor.md`
- `../../../04-quality/compliance/pdpl-pre-launch-checklist.md`
- `../marketing/api-contract.md` — v1 visitor_id path (Wave 25 Bucket A)
