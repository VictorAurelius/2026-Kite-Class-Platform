# Trial Lifecycle — API Contract

## POST /api/platform/instances/register
**Use case:** UC-TR-01
**Auth:** None (public)
**Request:**
```json
{
  "organizationName": "Trường THPT ABC",
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
  "user": { "id": "uuid", "email": "admin@abc.edu.vn", "name": "Nguyễn Văn A" },
  "tokens": { "accessToken": "...", "refreshToken": "..." },
  "trialExpiresAt": "2026-04-07T00:00:00Z"
}
```
**Errors:**
- 409: subdomain/email already exists

---

## GET /api/platform/instances/{id}/trial-status
**Use case:** UC-TR-05
**Auth:** Bearer token (Owner)
**Response 200:**
```json
{
  "instanceId": "uuid",
  "status": "TRIAL",
  "trialStartedAt": "2026-03-24T00:00:00Z",
  "trialExpiresAt": "2026-04-07T00:00:00Z",
  "daysLeft": 14
}
```
**Errors:**
- 404: instance not found

---

## POST /api/platform/instances/{id}/extend-trial
**Use case:** UC-TR-04
**Auth:** Bearer token (Admin only)
**Request params:** `?days=7`
**Response 204:** No content
**Errors:**
- 400: invalid days value
- 404: instance not found

---

## Note on Automated Use Cases (no HTTP endpoint)

| Use Case | Trigger | Description |
|----------|---------|-------------|
| UC-TR-02 | `TrialExpirationCheckerScheduler` (daily) | Check trial instances past expiry date → suspend instance |
| UC-TR-03 | `TrialExpirationCheckerScheduler` (daily) | Send trial expiration warning emails (3 days before, 1 day before) |

These lifecycle transitions are scheduler-triggered — no HTTP endpoint. Trial status is visible via `GET /api/platform/instances/{id}/trial-status`.
