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
