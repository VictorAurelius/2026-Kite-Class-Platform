# Email Lifecycle — API Contract

> Các email được gửi tự động qua internal service calls, không expose API ra ngoài.
> Auth API liên quan đến email verification:

## POST /api/auth/verify-email
**Use case:** UC-EML-03
**Auth:** None (token trong URL)
**Request params:** `?token=<verification-token>`
**Response 200:**
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "user": { "id": "uuid", "email": "...", "verified": true }
}
```
**Errors:**
- 400: token invalid or expired

---

## POST /api/auth/resend-verification
**Use case:** UC-EML-04
**Auth:** None
**Request:**
```json
{ "email": "user@example.com" }
```
**Response 200:**
```json
{ "message": "Email xác nhận đã được gửi lại" }
```
**Errors:**
- 404: email not found
- 400: already verified

---

## Internal: POST /api/platform/emails/send (kitehub-email service)
**Use case:** UC-EML-01, UC-EML-02
**Auth:** Internal service (no auth header)
**Request:**
```json
{
  "to": "user@example.com",
  "subject": "Chào mừng đến KiteClass",
  "templateName": "welcome",
  "variables": {
    "orgName": "Trường ABC",
    "trialDays": "14",
    "expiryDate": "2026-04-07"
  }
}
```
**Response 200:** `{ "sent": true }`
**Errors:**
- 500: SMTP failure (caught by client, logged, continued)
