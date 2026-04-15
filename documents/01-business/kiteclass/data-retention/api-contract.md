# Data Retention — API Contract

**Domain:** data-retention
**Source:** GAP-073, Wave 4 Sub-PR 4.4

> **Status:** REST endpoints are **deferred** to a follow-up Sub-PR that wires Settings →
> DangerZone on the KiteHub frontend. This document specifies the **service-layer API**
> delivered in Sub-PR 4.4 plus the target REST shape for the follow-up.

---

## Service-layer API (Sub-PR 4.4)

### `DeletionService` (`com.kiteclass.core.module.retention.DeletionService`)

| Method | Signature | Effect |
|--------|-----------|--------|
| `requestDeletion` | `DeletionRequest requestDeletion(Long userId, UUID tenantId)` | Creates PENDING row, stamps `graceEndsAt = now + 7d`; audits `deletion.requested` |
| `cancelDeletion` | `DeletionRequest cancelDeletion(Long deletionId, String reason)` | PENDING/GRACE_PERIOD → CANCELLED; audits `deletion.cancelled` |
| `startProcessing` | `DeletionRequest startProcessing(Long deletionId)` | PENDING → PROCESSING; audits `deletion.processing_started` |
| `markCompleted` | `DeletionRequest markCompleted(Long deletionId)` | PROCESSING → COMPLETED; audits `deletion.completed` |
| `expirePastGrace` | `int expirePastGrace()` | Scheduler entrypoint — promotes all PENDING rows with `graceEndsAt <= now` to PROCESSING; returns count |

All methods are `@Transactional`. State-machine violations throw `IllegalStateException`.
Duplicate requests throw `IllegalStateException` with message referencing existing id.

### `DataExportService`

| Method | Signature | Effect |
|--------|-----------|--------|
| `exportForUser` | `byte[] exportForUser(Long userId, UUID tenantId)` | Builds GDPR Art. 20 ZIP bytes; audits `deletion.export_generated` |

### `RetentionClassifier`

| Method | Signature | Effect |
|--------|-----------|--------|
| `classify` | `Classification classify(Class<?> entityClass)` | Reflection-based lookup of `@Retention`; default = `PURGE_ON_REQUEST` |

`Classification` is an immutable value (Lombok `@Value`) with fields:
`RetentionBucket bucket`, `String[] pseudonymizeFields`, `boolean explicit`.

---

## Target REST shape (follow-up Sub-PR)

Base path: `/api/v1/retention`

### POST `/api/v1/retention/deletion-requests`

**Description:** Request deletion of the authenticated user's account.

**Request:** empty body; user inferred from `Authorization` header.

**Response 201:**
```json
{
  "id": 123,
  "userId": 42,
  "tenantId": "...",
  "status": "PENDING",
  "graceEndsAt": "2026-04-22T10:00:00Z"
}
```

**Errors:**
| Code | Meaning |
|------|---------|
| 400 | Already has non-terminal deletion request |
| 401 | Not authenticated |

### POST `/api/v1/retention/deletion-requests/{id}/cancel`

**Request:**
```json
{ "reason": "changed my mind" }
```

**Response 200:** updated `DeletionRequest` JSON.

**Errors:**
| Code | Meaning |
|------|---------|
| 404 | Request not found |
| 409 | Terminal state (grace expired) |

### GET `/api/v1/retention/export`

**Description:** GDPR Art. 20 data export as ZIP.

**Response 200:** binary, `Content-Type: application/zip`,
`Content-Disposition: attachment; filename="kiteclass-export-{userId}.zip"`.

### Error envelope

Standard KiteClass error envelope (`{ code, message, traceId }`); HTTP codes per table above.

## Log
- 2026-04-15 — Initial contract, REST wiring deferred (Wave 4 Sub-PR 4.4)
