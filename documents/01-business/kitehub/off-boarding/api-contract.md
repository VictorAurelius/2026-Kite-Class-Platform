# Tenant Off-boarding — API Contract

All endpoints assume Bearer JWT (Owner or Admin role) unless noted. Error envelope follows project standard (`{error: {code, message, details}}`).

## POST /api/platform/instances/{id}/off-boarding/cancel
**Use case:** UC-OFF-01
**Auth:** Bearer token (Owner of instance + 2FA)
**Request:**
```json
{
  "reason": "SWITCHED_PROVIDER",
  "reasonDetail": "Moving to in-house LMS",
  "exportRequested": true,
  "idempotencyKey": "uuid-v4"
}
```
**Response 202 (Accepted):**
```json
{
  "instanceId": "uuid",
  "offBoardingPhase": "CANCEL_REQUESTED",
  "requestedAt": "2026-04-20T12:00:00Z",
  "graceEndsAt": "2026-05-20T12:00:00Z",
  "archiveScheduledAt": "2026-07-19T12:00:00Z",
  "undoUrl": "/api/platform/instances/{id}/off-boarding/undo",
  "exportBundleEta": "2026-04-21T12:00:00Z"
}
```
**Errors:**
- 403: 2FA not enabled — `{error.code: "MFA_REQUIRED"}`
- 409: already in off-boarding — `{error.code: "OFFBOARDING_IN_FLIGHT", details: {currentPhase}}`
- 422: billing dispute open — `{error.code: "BILLING_DISPUTE_OPEN", details: {disputeRef}}`

**Idempotency:** `idempotencyKey` persisted 10 min; duplicate returns original 202.

---

## POST /api/platform/instances/{id}/off-boarding/undo
**Use case:** UC-OFF-02
**Auth:** Bearer token (Owner) OR signed undo token from email
**Request:** `{}` (empty body) or `{undoToken}` if via email link
**Response 200:**
```json
{
  "instanceId": "uuid",
  "offBoardingPhase": "NONE",
  "status": "PAID_ACTIVE",
  "undoneAt": "2026-04-25T09:00:00Z"
}
```
**Errors:**
- 410: undo window expired — `{error.code: "UNDO_WINDOW_EXPIRED"}`
- 409: phase already beyond undo boundary — `{error.code: "UNDO_NOT_ALLOWED", details: {currentPhase}}`

---

## POST /api/platform/instances/{id}/off-boarding/rtbf
**Use case:** UC-OFF-03 (right-to-be-forgotten request initiation)
**Auth:** Bearer token (Owner) OR verified subject email flow
**Request:**
```json
{
  "legalBasis": "GDPR_ART_17",
  "subjectEmail": "owner@example.com",
  "reason": "Withdrawn consent"
}
```
**Response 202:**
```json
{
  "requestId": "uuid",
  "confirmationChannel": "email",
  "tokenExpiresAt": "2026-04-20T12:15:00Z"
}
```
**Errors:**
- 409: legal hold in place — `{error.code: "LEGAL_HOLD_ACTIVE", details: {holdRef, estimatedLiftDate}}`
- 403: subject lacks standing — `{error.code: "SUBJECT_NOT_AUTHORIZED"}`

---

## POST /api/platform/instances/{id}/off-boarding/rtbf/confirm
**Use case:** UC-OFF-03 (token confirmation)
**Auth:** Request ID + token (no JWT required — acts on token)
**Request:**
```json
{
  "requestId": "uuid",
  "token": "123456"
}
```
**Response 202:**
```json
{
  "instanceId": "uuid",
  "offBoardingPhase": "RTBF_FAST_TRACK",
  "purgeScheduledAt": "2026-04-21T12:00:00Z",
  "exportBundleEta": "2026-04-21T10:00:00Z"
}
```
**Errors:**
- 423: token expired — `{error.code: "CONFIRMATION_EXPIRED"}`
- 401: token mismatch — generic 401 (no enumeration)

---

## POST /api/platform/instances/{id}/export
**Use case:** UC-OFF-05
**Auth:** Bearer token (Owner)
**Request:**
```json
{
  "scope": "FULL"
}
```
`scope` ∈ `FULL | BRANDING_ONLY | FINANCIAL_ONLY | ACADEMIC_ONLY`.

**Response 202:**
```json
{
  "exportJobId": "uuid",
  "scope": "FULL",
  "estimatedReadyAt": "2026-04-21T12:00:00Z",
  "statusUrl": "/api/platform/exports/{jobId}"
}
```
**Errors:**
- 429: rate limit — max 1 FULL per 24h

---

## GET /api/platform/exports/{jobId}
**Use case:** UC-OFF-05 + UC-OFF-01 (poll export status)
**Auth:** Bearer token (Owner who requested)
**Response 200:**
```json
{
  "exportJobId": "uuid",
  "status": "READY",
  "bundleUrl": "https://minio.kitehub.vn/exports/signed...",
  "bundleExpiresAt": "2026-04-28T12:00:00Z",
  "bundleSizeBytes": 15728640,
  "bundleChecksum": "sha256:..."
}
```
`status` ∈ `PENDING | BUILDING | READY | EXPIRED | FAILED`.

---

## GET /api/platform/instances/{id}/off-boarding/status
**Use case:** UC-OFF-07
**Auth:** Bearer token (Owner or Admin)
**Response 200:**
```json
{
  "instanceId": "uuid",
  "offBoardingPhase": "CANCEL_GRACE_ACTIVE",
  "status": "PAID_ACTIVE",
  "requestedAt": "2026-04-20T12:00:00Z",
  "graceEndsAt": "2026-05-20T12:00:00Z",
  "archiveScheduledAt": "2026-07-19T12:00:00Z",
  "purgeScheduledAt": "2026-07-19T12:00:00Z",
  "undoAvailable": true,
  "bundleUrl": "https://...",
  "bundleExpiresAt": "2026-04-28T12:00:00Z"
}
```

**Phase enum:** `NONE | CANCEL_REQUESTED | EXPORT_READY | CANCEL_GRACE_ACTIVE | CANCEL_GRACE_READONLY | ARCHIVED | PURGED | RTBF_FAST_TRACK`

---

## POST /api/platform/admin/instances/{id}/off-boarding/cancel
**Use case:** UC-OFF-04 (staff-managed Enterprise cancel)
**Auth:** Bearer token (Admin role)
**Request:**
```json
{
  "reason": "CONTRACT_END",
  "reasonDetail": "Enterprise contract end of term",
  "ticketRef": "TICKET-12345",
  "scheduledPurgeDate": "2026-08-01T00:00:00Z"
}
```
**Response 202:** Same envelope as POST /cancel
**Errors:**
- 403: not admin
- 409: in billing dispute

---

## Export Bundle Specification

Content: ZIP at `MinIO://exports/{instanceId}/{jobId}.zip`

```
bundle.zip
├── manifest.json              # inventory of all files + checksums + scope
├── README.md                  # GDPR Art. 20 format explanation
├── academic/                  # scope: FULL | ACADEMIC_ONLY
│   ├── students.xlsx
│   ├── classes.xlsx
│   ├── grades.xlsx
│   └── attendance.xlsx
├── financial/                 # scope: FULL | FINANCIAL_ONLY
│   ├── invoices.pdf           # per-period PDFs
│   └── payment-log.xlsx
├── branding/                  # scope: FULL | BRANDING_ONLY (GAP-034)
│   ├── logo.svg
│   ├── palette.json
│   ├── templates/
│   └── ai-history.json
├── users/                     # scope: FULL
│   ├── staff.xlsx
│   └── parents.xlsx
└── audit/                     # scope: FULL
    └── activity-log.jsonl
```

**Formats (per manifest):**
- Tabular: XLSX + CSV mirror
- Documents: PDF (stylesheet applied)
- Structured: JSON (manifest + ai-history)
- Audit: JSONL (line-delimited)

**Integrity:** SHA256 checksum per file in `manifest.json`; bundle-level checksum in response header `X-Bundle-Checksum`.

**Delivery:** Presigned MinIO URL; TTL 7 days (OFF-06). Re-request after expiry rebuilds fresh bundle.

---

## Outbox Events (consumed, not exposed as HTTP)

Published to `kitehub.offboarding` topic. See `rules.md` §5 for full schema.

---

## Error Codes Reference

| Code | HTTP | Meaning |
|------|------|---------|
| `MFA_REQUIRED` | 403 | 2FA must be enabled before cancellation |
| `OFFBOARDING_IN_FLIGHT` | 409 | Another off-boarding phase already active |
| `BILLING_DISPUTE_OPEN` | 422 | Cannot cancel while dispute open |
| `UNDO_WINDOW_EXPIRED` | 410 | Beyond 30d (OFF-07) |
| `UNDO_NOT_ALLOWED` | 409 | Phase past undo boundary (READONLY+) |
| `LEGAL_HOLD_ACTIVE` | 409 | Purge blocked by legal hold (DMCA/litigation) |
| `SUBJECT_NOT_AUTHORIZED` | 403 | RTBF subject lacks standing |
| `CONFIRMATION_EXPIRED` | 423 | RTBF token expired (OFF-10 15m) |
