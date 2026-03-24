# Instance Provisioning — API Contract

## GET /api/platform/instances
**Use case:** UC-INS-04
**Auth:** Bearer token (Admin)
**Response 200:** `[InstanceResponse]`

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
