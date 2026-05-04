# Child Protection — API Contract

**Domain:** KiteClass Core / Compliance / Safeguarding
**Version:** 0.1 (Phase 1A — service-layer only; HTTP endpoints in Phase 1B)
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

## Log

- **2026-05-04** (v0.1): Phase 1A service-layer contract + Phase 1B planned REST shape documented. Wave 18b1 Bucket E.
