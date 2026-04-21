# School MIS Integration — API Contract

**Domain:** KiteClass Core / Integration / MIS Roster Import
**Version:** 1.0 (Phase 1 — GAP-200)
**Updated:** 2026-04-21
**Module:** `kiteclass-core` package `com.kiteclass.core.integration.mis.controller` (Phase 2)

Phase 1 documents the **planned contract**. Only the interface + adapter
skeleton is implemented this PR. Controllers land in Phase 2.

---

## Authentication & Headers

| Header | Source | Required for |
|--------|--------|--------------|
| `X-Tenant-Id` | Gateway (sub-domain → instanceId) | All endpoints |
| `X-User-Id` | Gateway (admin JWT) | All endpoints |
| HMAC signature | `InternalRequestFilter` | `GET /internal/mis/import/{jobId}` (service-to-service) |

All MIS endpoints require role `TENANT_ADMIN` (enforced in Gateway).

---

## MisCredentialsController — `/api/v1/mis/credentials`

### POST /api/v1/mis/credentials/test
**Use Case:** UC-MIS-01 | **Auth:** Bearer + X-User-Id + X-Tenant-Id

```json
// Request — TestMisCredentialsRequest
{
  "provider": "VNEDU",
  "apiKey": "************",
  "providerTenantId": "HCM-00123",
  "baseUrl": "https://api.vnedu.vn/v1"
}

// Response 200 — ApiResponse<MisConnectionStatus>
{
  "success": true,
  "message": "Kết nối thành công",
  "data": {
    "provider": "VNEDU",
    "connected": true,
    "providerVersion": "v1.4",
    "schoolName": "THPT Lê Hồng Phong",
    "testedAt": "2026-04-21T08:42:00Z"
  }
}
```

**Error responses:**
- `400 MIS_INVALID_CREDENTIALS` — auth failure at provider
- `400 MIS_UNSUPPORTED_PROVIDER` — enum not in `MisProvider`
- `503 MIS_DISABLED` — feature flag off
- `403 MIS_DPA_REQUIRED` — tenant missing DPA

### POST /api/v1/mis/credentials
Upsert credentials (stored encrypted). Body identical to test endpoint.
Returns `201` with credential id (no secrets echoed).

### DELETE /api/v1/mis/credentials
Soft-delete credentials. Use case UC-MIS-05. Returns `204`.
Error: `409 MIS_IMPORT_IN_PROGRESS`.

---

## MisImportController — `/api/v1/mis/import`

### POST /api/v1/mis/import
**Use Case:** UC-MIS-02 (dryRun=true) / UC-MIS-03 (commit)

Query params:
- `dryRun` (boolean, default `false`) — preview mode
- `academicYear` (string, default current) — e.g. `"2025-2026"`

```json
// Response 200 (dry-run) — ApiResponse<MisImportPreview>
{
  "success": true,
  "data": {
    "provider": "VNEDU",
    "academicYear": "2025-2026",
    "counts": {
      "students": 1242,
      "parents": 1180,
      "teachers": 87,
      "classes": 42,
      "enrollments": 1242
    },
    "samples": {
      "students": [ /* first 10 StudentRecord */ ],
      "teachers": [ /* first 10 TeacherRecord */ ]
    },
    "warnings": [
      { "code": "DUPLICATE_EMAIL", "count": 3, "records": [ /* ids */ ] }
    ]
  }
}

// Response 202 (commit) — ApiResponse<MisImportJobRef>
{
  "success": true,
  "message": "Import đã được xếp hàng",
  "data": {
    "jobId": "job_01HKV9...",
    "statusUrl": "/api/v1/mis/import/job_01HKV9...",
    "enqueuedAt": "2026-04-21T08:45:00Z"
  }
}
```

**Error responses:**
- `429 MIS_REIMPORT_COOLDOWN` — cooldown active (body includes `retryAfterSeconds`)
- `503 MIS_ADAPTER_UNREACHABLE` — circuit breaker open
- `504 MIS_IMPORT_TIMEOUT` — fetch timeout (dry-run only; commits always async)
- `400 MIS_BATCH_TOO_LARGE` — record count exceeds `max-records-per-import`

### GET /api/v1/mis/import/{jobId}
**Use Case:** UC-MIS-03 status polling

```json
// Response 200 — ApiResponse<MisImportJobStatus>
{
  "success": true,
  "data": {
    "jobId": "job_01HKV9...",
    "status": "RUNNING",
    "progressPercent": 42,
    "provider": "VNEDU",
    "startedAt": "2026-04-21T08:45:03Z",
    "endedAt": null,
    "counts": {
      "imported": 520,
      "updated": 1,
      "conflicts": 2,
      "rejected": 0
    },
    "errorMessage": null
  }
}
```

Terminal statuses: `COMPLETED`, `FAILED`, `PARTIAL`.

**Error:** `404 MIS_IMPORT_JOB_NOT_FOUND`.

---

## MisConflictController — `/api/v1/mis/conflicts` (Phase 2)

Documented here to lock the contract. Implementation deferred.

### GET /api/v1/mis/conflicts?status=PENDING
Returns paginated list of `MisConflict` rows with `mis` and `kiteclass` value
pairs per conflicting field.

### POST /api/v1/mis/conflicts/{id}/resolve

```json
// Request
{ "decision": "USE_MIS" }   // or USE_KITECLASS | SKIP

// Response 200 — ApiResponse<MisConflictResolution>
{ "success": true, "data": { "conflictId": "...", "resolvedAt": "..." } }
```

**Errors:** `404 MIS_CONFLICT_NOT_FOUND`, `403 MIS_CONFLICT_READONLY`.

---

## Error Code Catalog

| Code | HTTP | Meaning |
|------|:----:|---------|
| `MIS_DISABLED` | 503 | Feature flag off |
| `MIS_DPA_REQUIRED` | 403 | PDPL DPA not on file |
| `MIS_INVALID_CREDENTIALS` | 400 | Provider auth failed |
| `MIS_UNSUPPORTED_PROVIDER` | 400 | Unknown provider value |
| `MIS_IMPORT_IN_PROGRESS` | 409 | Active RUNNING job blocks action |
| `MIS_REIMPORT_COOLDOWN` | 429 | Cooldown not elapsed |
| `MIS_BATCH_TOO_LARGE` | 400 | Record count > max |
| `MIS_ADAPTER_UNREACHABLE` | 503 | Circuit breaker open |
| `MIS_IMPORT_TIMEOUT` | 504 | Upstream timeout (dry-run only) |
| `MIS_IMPORT_JOB_NOT_FOUND` | 404 | jobId unknown |
| `MIS_CONFLICT_NOT_FOUND` | 404 | Conflict id unknown |
| `MIS_CONFLICT_READONLY` | 403 | Conflict already resolved |

All errors follow the project's `ApiResponse<Void>` envelope with `success=false`
+ `errorCode` + i18n-ready `message`.

---

## Phase 1 Implementation — What Actually Ships

This PR ships:
- `MisRosterSource` interface (`com.kiteclass.core.integration.mis.MisRosterSource`)
- `RosterImport` DTO + nested record types
- `VneduAdapter` class implementing `MisRosterSource` with `TODO:` markers
- `VneduAdapterTest` unit test validating interface shape (mocked)

Controllers, service layer, persistence, queue integration → **Phase 2**.

---

## References

- Rules: `rules.md`
- Use cases: `use-cases.md`
- ADR: `documents/02-architecture/adr/ADR-017-mis-sync-strategy.md`
- Gap: `documents/04-quality/gaps/GAP-200-school-mis-integration.md`
- Catalog: `documents/02-architecture/integrations/school-mis-catalog.md`
