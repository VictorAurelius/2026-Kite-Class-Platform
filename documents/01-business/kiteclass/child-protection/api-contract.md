# Child Protection — API Contract

**Domain:** KiteClass Core / Compliance / Safeguarding
**Version:** 0.2 (Phase 1A service-layer + Phase 1B foundation REST endpoints for vetting)
**Created:** 2026-05-04
**Last-Reviewed:** 2026-05-04

> Phase 1A ships the SERVICE LAYER (`IncidentService`) only. HTTP endpoints + DTOs ship in Phase 1B (GAP-322b) once RBAC gates + state machine are in place. This document specifies the planned REST shape for Phase 1B and the service-layer contract for Phase 1A.

---

## Phase 1A — Service-layer contract

`com.kiteclass.core.module.childprotection.service.IncidentService`:

| Method | Signature | Behavior |
|--------|-----------|----------|
| `create` | `Incident create(String title, String description, String evidencePaths, IncidentSeverity severity, IncidentCategory category, Long reporterUserId, Long subjectStudentId)` | UC-CHILD-PROT-001. Validates + persists. Encrypted via converter. Default `status=REPORTED`. |
| `findById` | `Incident findById(Long id)` | UC-CHILD-PROT-003. Excludes soft-deleted. Throws `EntityNotFoundException` if missing. |
| `findAll` | `Page<Incident> findAll(IncidentSeverity severity, IncidentCategory category, IncidentStatus status, Pageable pageable)` | UC-CHILD-PROT-002. Null filters = no filter. |
| `updateStatus` | `Incident updateStatus(Long id, IncidentStatus newStatus)` | UC-CHILD-PROT-004. Phase 1A: free transitions. Phase 1B: state-machine. |
| `assignOfficer` | `Incident assignOfficer(Long id, Long officerUserId)` | UC-CHILD-PROT-004. |
| `softDelete` | `void softDelete(Long id)` | UC-CHILD-PROT-009. Phase 1A: free. Phase 1C: blocks CLOSED+age<7y + CSAM. |

---

## Phase 1B — Planned REST endpoints (target shape, not yet shipped)

Base path: `/api/v1/incidents`

### `POST /api/v1/incidents` — Submit incident (UC-001)

**RBAC:** any user with `INCIDENT_REPORT` permission.

**Request:**
```json
{
  "title": "Bullying observed in class 7A",
  "description": "Detailed sensitive narrative ...",
  "evidencePaths": "minio/evidence-1.jpg\nminio/evidence-2.png",
  "severity": "HIGH",
  "category": "BULLYING",
  "subjectStudentId": 12345
}
```

**Response 201:**
```json
{
  "id": 42,
  "title": "Bullying observed in class 7A",
  "severity": "HIGH",
  "category": "BULLYING",
  "status": "REPORTED",
  "reporterUserId": 100,
  "subjectStudentId": 12345,
  "createdAt": "2026-05-04T10:30:00Z"
}
```
Note: `description` + `evidencePaths` are NOT echoed back to reporter — Phase 1B RBAC restricts to officer-tier readers.

**Errors (4xx):**
- `400 VALIDATION_TITLE_REQUIRED` / `_TITLE_TOO_LONG` / `_SEVERITY_REQUIRED` / `_CATEGORY_REQUIRED`
- `403 FORBIDDEN` — caller lacks `INCIDENT_REPORT`

### `GET /api/v1/incidents` — List incidents (UC-002)

**RBAC (Phase 1B):** `SAFEGUARDING_OFFICER` + `PRINCIPAL` + `COUNSELOR`.

**Query params:**
- `severity` (optional): `LOW|MEDIUM|HIGH|CRITICAL`
- `category` (optional): `BULLYING|ABUSE|GROOMING|CSAM|OTHER`
- `status` (optional): `REPORTED|INVESTIGATING|ESCALATED|RESOLVED|CLOSED`
- `page`, `size`, `sort` (Spring Pageable)

**Response 200:** paged list. Phase 1B restricts decryption — list rows omit `description` + `evidencePaths`; only single-detail GET returns decrypted.

### `GET /api/v1/incidents/{id}` — Read detail (UC-003)

**RBAC (Phase 1B):** `INCIDENT_READ_DECRYPTED` permission required.

**Response 200:**
```json
{
  "id": 42,
  "title": "...",
  "description": "<decrypted plaintext>",
  "evidencePaths": "<decrypted plaintext>",
  "severity": "HIGH",
  "category": "BULLYING",
  "status": "INVESTIGATING",
  "reporterUserId": 100,
  "subjectStudentId": 12345,
  "assignedOfficerUserId": 555,
  "createdAt": "2026-05-04T10:30:00Z",
  "updatedAt": "2026-05-04T11:15:00Z"
}
```

**Errors:**
- `404 INCIDENT_NOT_FOUND`
- `403 FORBIDDEN_DECRYPT` — caller lacks `INCIDENT_READ_DECRYPTED`
- `500 DECRYPT_FAILED` — tampered ciphertext or wrong key (BR-CHILD-PROT-003)

### `PUT /api/v1/incidents/{id}/status` — Transition status (UC-004)

**RBAC:** `SAFEGUARDING_OFFICER`.

**Request:** `{ "newStatus": "INVESTIGATING" }`

**Response 200:** updated incident summary.

**Errors:**
- `400 INVALID_STATUS_TRANSITION` (Phase 1B state machine)
- `404 INCIDENT_NOT_FOUND`

### `PUT /api/v1/incidents/{id}/officer` — Assign officer (UC-004)

**Request:** `{ "officerUserId": 555 }`

### `DELETE /api/v1/incidents/{id}` — Soft-delete (UC-009)

**RBAC:** `SAFEGUARDING_OFFICER` + non-CSAM-CRITICAL constraint (Phase 1C).

**Response 204:** no body.

**Errors:**
- `403 FORBIDDEN_DELETE_CSAM` (Phase 1C — BR-CHILD-PROT-016)
- `403 FORBIDDEN_DELETE_RETENTION` (Phase 1C — BR-CHILD-PROT-012, age<7y on CLOSED)

---

## Error code mapping (planned)

| HTTP | Code | Source |
|------|------|--------|
| 400 | `VALIDATION_TITLE_REQUIRED` | `ValidationException("Title is required")` |
| 400 | `VALIDATION_TITLE_TOO_LONG` | `ValidationException("Title too long ...")` |
| 400 | `VALIDATION_SEVERITY_REQUIRED` | `ValidationException("Severity is required")` |
| 400 | `VALIDATION_CATEGORY_REQUIRED` | `ValidationException("Category is required")` |
| 400 | `VALIDATION_REPORTER_REQUIRED` | `ValidationException("Reporter user id is required")` |
| 400 | `VALIDATION_STATUS_REQUIRED` | `ValidationException("Status is required")` |
| 400 | `VALIDATION_OFFICER_REQUIRED` | `ValidationException("Officer user id is required")` |
| 400 | `INVALID_STATUS_TRANSITION` (Phase 1B) | state machine |
| 403 | `FORBIDDEN_DECRYPT` (Phase 1B) | RBAC `INCIDENT_READ_DECRYPTED` |
| 403 | `FORBIDDEN_DELETE_CSAM` (Phase 1C) | BR-CHILD-PROT-016 |
| 403 | `FORBIDDEN_DELETE_RETENTION` (Phase 1C) | BR-CHILD-PROT-012 |
| 404 | `INCIDENT_NOT_FOUND` | `EntityNotFoundException` |
| 500 | `DECRYPT_FAILED` | `RuntimeException` from converter |

---

## Phase 1B foundation — Vetting REST endpoints (GAP-322b, Wave 18b2 Bucket B — SHIPPED)

Base path: `/api/v1/vettings`. RBAC: SAFEGUARDING_OFFICER role only on `X-User-Roles` header (BR-VETTING-003).

### Schemas

**`VettingResponse`**
```json
{
  "id": 7,
  "teacherId": 100,
  "status": "PENDING",
  "lltpNumber": "LLTP-12345",
  "policeCheckDetails": "Police check passed without remarks",
  "submittedAt": "2026-05-05T08:00:00Z",
  "interviewedAt": null,
  "decidedAt": null,
  "expiresAt": "2027-05-04T00:00:00Z",
  "decidedByUserId": null,
  "createdAt": "2026-05-04T07:30:00Z",
  "updatedAt": "2026-05-04T07:30:00Z"
}
```
Encrypted fields (`lltpNumber`, `policeCheckDetails`) are decrypted by `AesGcmAttributeConverter` on read; only returned to SAFEGUARDING_OFFICER callers.

**`VettingCreateRequest`**
```json
{
  "teacherId": 100,
  "lltpNumber": "LLTP-12345",
  "policeCheckDetails": "Pre-interview note (optional)",
  "expiresAt": "2027-05-04T00:00:00Z"
}
```
Only `teacherId` is required.

**`VettingTransitionRequest`**
```json
{ "targetStatus": "SUBMITTED" }
```

**`ApiResponse<T>`** — standard wrapper (`success`, `data`, `message`, `timestamp`).

### Endpoints

#### `GET /api/v1/vettings` — List vetting records (UC-VETTING-001+)

**Query params:** `status` (optional `VettingStatus` enum), Spring `Pageable` (`page`, `size`, `sort`).

**Response 200:** `ApiResponse<Page<VettingResponse>>`.

**Errors:** `403 VETTING_RBAC_DENIED`.

#### `GET /api/v1/vettings/{id}` — Read detail

**Response 200:** `ApiResponse<VettingResponse>`.

**Errors:** `404 VETTING_NOT_FOUND`, `403 VETTING_RBAC_DENIED`.

#### `POST /api/v1/vettings` — Create vetting (UC-VETTING-001)

**Request:** `VettingCreateRequest`.

**Response 201:** `ApiResponse<VettingResponse>` with `status=PENDING`.

**Errors:** `400 VETTING_TEACHER_ID_REQUIRED`, `403 VETTING_RBAC_DENIED`.

#### `PATCH /api/v1/vettings/{id}/transition` — Advance state (UC-VETTING-002..005)

Requires `X-User-Reference-Id` header (officer user id, recorded as `decidedByUserId` on APPROVED/REJECTED).

**Request:** `VettingTransitionRequest`.

**Response 200:** `ApiResponse<VettingResponse>`.

**Errors:**
- `400 VETTING_TARGET_STATUS_REQUIRED` — null `targetStatus`.
- `400 VETTING_INVALID_TRANSITION` — illegal transition per BR-VETTING-001.
- `404 VETTING_NOT_FOUND`.
- `403 VETTING_RBAC_DENIED`.

#### `DELETE /api/v1/vettings/{id}` — Soft delete (BR-VETTING-005)

**Response 204:** no body. Phase 1C will tighten anti-delete on REJECTED + 7-year retention.

**Errors:** `404 VETTING_NOT_FOUND`, `403 VETTING_RBAC_DENIED`.

#### `POST /api/v1/vettings/{vettingId}/documents` — Upload evidence (BR-VETTING-006, Wave 18b3)

Single-file multipart upload (`file` field). Persists the file bytes to the dedicated MinIO bucket (`childprotection.minio.bucket`, default `kiteclass-vetting`) under deterministic key `vetting/{vettingId}/{sanitized-filename}`.

**Request:** `multipart/form-data` with single field `file` (PDF or image; ≤10MB).

**Response 201:** `ApiResponse<VettingDocumentResponse>` where `VettingDocumentResponse = { vettingId, storageKey, sizeBytes, contentType }`.

**Errors:**
- `400 VETTING_DOC_EMPTY` — empty multipart payload.
- `400 VETTING_DOC_TOO_LARGE` — file size exceeds 10MB cap.
- `400 VETTING_DOC_FILENAME_REQUIRED` — missing/blank `originalFilename`.
- `404 VETTING_NOT_FOUND` — vetting record id does not exist.
- `403 VETTING_RBAC_DENIED` — caller lacks `SAFEGUARDING_OFFICER` role.
- `500 VETTING_DOC_UPLOAD_FAILED` — IOException reading multipart body (rare).

### Vetting error code table

| HTTP | Code | Source |
|------|------|--------|
| 400 | `VETTING_TEACHER_ID_REQUIRED` | `ValidationException` |
| 400 | `VETTING_TARGET_STATUS_REQUIRED` | `ValidationException` |
| 400 | `VETTING_INVALID_TRANSITION` | `VettingServiceImpl` state-machine guard |
| 400 | `VETTING_DOC_EMPTY` / `VETTING_DOC_TOO_LARGE` | `VettingController.uploadDocument` (10MB cap) |
| 400 | `VETTING_DOC_FILENAME_REQUIRED` / `VETTING_DOC_CONTENT_REQUIRED` / `VETTING_DOC_TTL_INVALID` / `VETTING_DOC_ID_REQUIRED` / `VETTING_ID_REQUIRED` | `MinIOVettingDocumentStorageImpl` validation guards |
| 403 | `VETTING_RBAC_DENIED` | `VettingController.requireSafeguardingOfficer` |
| 404 | `VETTING_NOT_FOUND` | `EntityNotFoundException` |
| 500 | `VETTING_DOC_UPLOAD_FAILED` | IOException reading multipart body |
| 404 | `VETTING_NOT_FOUND_FOR_TEACHER` | `EntityNotFoundException` |

### Storage contract (Phase 1B foundation)

`com.kiteclass.core.module.childprotection.storage.VettingDocumentStorage`:

| Method | Purpose | Phase 1B foundation behaviour |
|--------|---------|-------------------------------|
| `String storeDocument(Long vettingId, String filename, byte[] content)` | Persist evidence document | Stub returns `minio://vetting/{vettingId}/{filename}` after sanitizing path-traversal. Concrete MinIO SDK wiring deferred. |
| `String getDownloadUrl(Long vettingId, String docId, Duration ttl)` | Issue short-lived download URL | Stub returns `<docId>?ttl=<seconds>`. Concrete signed-URL impl deferred. |
| `void deleteDocument(Long vettingId, String docId)` | Delete document | Stub no-op (logs at INFO). Phase 1C ties to retention enforcement. |

The `MinIOVettingDocumentStorageImpl` Spring bean satisfies the contract today so callers (controller, future upload UI) can compile + smoke-test against the abstraction.

## Log

- **2026-05-04** (v0.2): Phase 1B foundation — Vetting REST endpoints, schemas, error codes, and storage contract documented. Wave 18b2 Bucket B (GAP-322b). LLTP file-upload endpoint + verify-queue UI deferred to Phase 1B follow-up.
- **2026-05-04** (v0.1): Phase 1A service-layer contract + Phase 1B planned REST shape documented. Wave 18b1 Bucket E.
