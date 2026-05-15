# Instance Provisioning — API Contract

## GET /api/platform/instances
**Use case:** UC-INS-04
**Auth:** Bearer token (Admin)
**Query params (offset pagination — default):**
- `page` (int, default `0`) — zero-based page index
- `size` (int, default `50`, max `200`) — page size; values >200 capped server-side
- `sort` (optional, default `createdAt,desc`)

**Query params (cursor pagination — Wave 85 Bucket D D-AC1, recommended cho dataset >1M rows để tránh OFFSET cliff):**
- `cursor` (string, opaque base64-encoded `id` of last row from prior page; mutually exclusive với `page`)
- `size` (int, default `50`, max `200`)
- Sort fixed `id ASC` khi cursor mode active (cho stable keyset traversal)

**Response 200 (offset mode):**
```json
{
  "content": [/* InstanceResponse[] */],
  "totalElements": 1234,
  "totalPages": 25,
  "page": 0,
  "size": 50,
  "first": true,
  "last": false
}
```

**Response 200 (cursor mode):**
```json
{
  "content": [/* InstanceResponse[] */],
  "size": 50,
  "nextCursor": "eyJpZCI6ImFiYy0xMjMifQ==",
  "hasNext": true
}
```

**Errors:** 400 `size > 200` (auto-capped instead — no error), 400 cả `page` lẫn `cursor` cùng truyền

**Performance note (GAP-432 Wave 41 + Wave 85 D-AC1):** prior unbounded `findAll()` đã eliminated; default size 50 + max 200 hard cap ngăn OOM scan. Cursor mode khuyến nghị cho list admin >1M rows để tránh OFFSET N skip-cost.

---

## POST /api/platform/instances
**Use case:** UC-INS-01
**Auth:** Bearer token (Admin)
**Request:**
```json
{
  "organizationName": "Trường ABC",
  "subdomain": "thptabc",
  "ownerEmail": "admin@abc.edu.vn",
  "ownerName": "Admin ABC"
}
```
**Response 201:** InstanceResponse
**Errors:** 409 subdomain/email conflict, 400 invalid subdomain

---

## POST /api/platform/instances/register
**Use case:** UC-INS-02, UC-INS-03
**Auth:** None (public)
**Request:**
```json
{
  "organizationName": "Trường ABC",
  "subdomain": "thptabc",
  "ownerEmail": "admin@abc.edu.vn",
  "ownerPassword": "SecurePass123!",
  "ownerName": "Nguyễn Văn A"
}
```
**Response 201:**
```json
{
  "instance": { "id": "uuid", "subdomain": "thptabc", "status": "TRIAL" },
  "user": { "id": "uuid", "email": "...", "name": "..." },
  "tokens": { "accessToken": "...", "refreshToken": "..." },
  "trialExpiresAt": "2026-04-07T00:00:00Z"
}
```
**Errors:** 409 subdomain/email exists, 400 reserved subdomain

---

## GET /api/platform/instances/{id}
**Auth:** Bearer token
**Response 200:** InstanceResponse
**Errors:** 404 not found

---

## GET /api/platform/instances/subdomain/{subdomain}
**Auth:** Bearer token
**Response 200:** InstanceResponse
**Errors:** 404 not found

---

## GET /api/platform/instances/owner/{ownerId}
**Auth:** Bearer token
**Response 200:** `[InstanceResponse]`

---

## PUT /api/platform/instances/{id}
## PATCH /api/platform/instances/{id}
**Use case:** UC-INS-05
**Auth:** Bearer token (Owner/Admin)
**Request:**
```json
{ "organizationName": "New Name" }
```
**Response 200:** Updated InstanceResponse
**Errors:** 404 not found

---

## POST /api/platform/instances/{id}/activate
**Use case:** UC-INS-03 (email verification activation)
**Auth:** Token from email verification link
**Response 200:**
```json
{
  "instance": { "id": "uuid", "subdomain": "string", "status": "TRIAL" },
  "trialExpiresAt": "datetime"
}
```
**Errors:** 400 invalid/expired token, 404 instance not found, 409 already activated

> **Flow (UC-INS-03):** `POST /register` → email verification sent → user clicks link → `POST /{id}/activate` → trial starts, DB provisioned, welcome email sent.

---

## DELETE /api/platform/instances/{id}
**Use case:** UC-INS-06
**Auth:** Bearer token (Admin)
**Response 204:** No content
**Errors:** 404 not found

---

# Drift fix (verified 2026-04-26 — GAP-229 Phase 3)

Verified `kitehub-subscription` `InstanceController` against this contract. 3 endpoints exist trong code nhưng thiếu trong doc; 1 endpoint trong doc đã move sang `AuthController`. Append corrections:

## GET /api/platform/instances/{id}/trial-status
**Auth:** Bearer token
**Response 200:** `TrialStatusResponse` — current trial state, days remaining, expiration
**Use case:** UC-INS-07 (FE poll trial expiry banner)
**Code:** `InstanceController.getTrialStatus()`

## POST /api/platform/instances/{id}/extend-trial?days={n}
**Auth:** Bearer token (Admin)
**Request params:** `days` — number of days to extend (integer)
**Response 204:** No content
**Use case:** UC-INS-08 (admin grants trial extension for tenant)
**Code:** `InstanceController.extendTrial()`

## DELETE /api/platform/instances/{id}/purge
**Auth:** Bearer token (Admin)
**Response 200:** `PurgeResult { instanceId, subdomain, status: SUCCESS|SKIPPED_NO_BACKUP|FAILED, databaseDropped, backupFilesDeleted, emailLogsDeleted, brandingCleanupPublished, purgedAt, errorMessage }`
**Use case:** UC-INS-09 (hard purge after retention — destructive, requires backup verification)
**Preconditions:**
- Instance status MUST be `DELETED` (else FAILED)
- At least 1 `COMPLETED` backup MUST exist (else SKIPPED_NO_BACKUP — safety per `InstancePurgeService` line 128)
**Code:** `InstanceController.purgeInstance()` → `InstancePurgeService.adminPurge()`
**Side effects (per GAP-222c Exception A):** Outbox row written tới `subscription_outbox` (event_type `instance.purge.requested`) + best-effort fast-path RabbitMQ `instance.purge.exchange` fanout. Consumers: `kitehub-branding` cleanup, `kiteclass-core` cleanup.

## POST /api/platform/auth/verify-email?token={t}  *(moved — was POST /{id}/activate)*
**Auth:** Token from email verification link (query param)
**Response 200:** `LoginResponse` (instance + user + tokens)
**Use case:** UC-INS-03 step 3-4 (email-verification activation)
**Code:** `AuthController.verifyEmail()` (was previously documented as `POST /api/platform/instances/{id}/activate`; controller relocated cho consolidated auth domain).

> **Note for UC-INS-03 update:** The flow remains the same (register → email verification sent → user clicks link → activate). Only the endpoint path changed from `/instances/{id}/activate` to `/auth/verify-email?token=...`. The activation logic still lives in `AuthService.verifyEmail()` which calls `InstanceLifecycleService` internally to start the trial.
