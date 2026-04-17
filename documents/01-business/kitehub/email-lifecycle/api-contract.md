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

---

## Admin Email Management API

> Các endpoint dưới đây dành cho Platform Admin, yêu cầu quyền admin.
> Base path: `/api/platform/admin/emails`

### GET /api/platform/admin/emails/history
**Use case:** Admin xem lịch sử gửi email
**Auth:** Platform Admin
**Query params:**
| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `instanceId` | UUID | No | — | Filter theo instance |
| `emailType` | String | No | — | Filter theo loại email |
| `from` | ISO DateTime | No | 30 ngày trước | Thời gian bắt đầu |
| `to` | ISO DateTime | No | Hiện tại | Thời gian kết thúc |
| `page` | int | No | 0 | Trang (0-based) |
| `size` | int | No | 20 | Số item mỗi trang |

**Response 200:**
```json
{
  "content": [
    {
      "id": "uuid",
      "instanceId": "uuid",
      "emailType": "trial-expiration-warning",
      "recipient": "user@example.com",
      "sentAt": "2026-04-15T10:30:00",
      "status": "SUCCESS"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0,
  "size": 20
}
```
> `status` = `SUCCESS` hoặc `FAILED` (FAILED khi emailType chứa suffix `:FAILED` từ DLQ consumer).

---

### GET /api/platform/admin/emails/stats
**Use case:** Admin xem thống kê email tổng hợp
**Auth:** Platform Admin
**Response 200:**
```json
{
  "totalSentToday": 42,
  "totalSentThisWeek": 285,
  "failedToday": 3,
  "countByType": {
    "trial-expiration-warning": 15,
    "renewal-reminder": 8,
    "suspension-notification": 5
  }
}
```

---

### GET /api/platform/admin/emails/config
**Use case:** Admin xem trạng thái toggle email hiện tại
**Auth:** Platform Admin
**Response 200:**
```json
{
  "queueEnabled": true,
  "emailTypeToggles": {
    "trial-expiration-warning": true,
    "suspension-notification": true,
    "renewal-reminder": false
  }
}
```

---

### PUT /api/platform/admin/emails/config
**Use case:** Admin bật/tắt loại email cụ thể (runtime, in-memory)
**Auth:** Platform Admin
**Request:**
```json
{
  "trial-expiration-warning": true,
  "suspension-notification": false
}
```
> Body là `Map<String, Boolean>` — key = email type, value = enabled/disabled.

**Response 200:**
```json
{
  "queueEnabled": true,
  "emailTypeToggles": {
    "trial-expiration-warning": true,
    "suspension-notification": false,
    "renewal-reminder": true
  }
}
```
> **Note:** Thay đổi chỉ lưu in-memory. Để persist, cập nhật `application.yml`.

---

### POST /api/platform/admin/emails/trigger
**Use case:** Admin gửi email thủ công cho một instance
**Auth:** Platform Admin
**Request:**
```json
{
  "instanceId": "uuid",
  "emailType": "trial-expiration-warning"
}
```
> Validation: `instanceId` — @NotNull, `emailType` — @NotBlank

**Response 200:** (empty body)
**Errors:**
- 400: instanceId null hoặc emailType blank
- 404: instance not found
- 409: email đã được gửi hôm nay (idempotency check)
