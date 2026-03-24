# Notification & Email — API Contract

## Endpoints — ContactMessageController

### POST /api/v1/contact

**Use Case:** UC-NTF-01
**Auth:** Public (không cần token)

**Request:**
```json
{ "name": "string", "email": "string", "subject": "string", "message": "string", "phone": "string?" }
```

**Response 201:**
```json
{ "success": true, "data": { "id": 1, "name": "Nguyen Van A", "email": "a@mail.com", "phone": "0901234567", "subject": "Hỏi về khóa học", "message": "...", "isRead": false, "readAt": null, "readBy": null, "createdAt": "2026-03-24T10:00:00", "updatedAt": "2026-03-24T10:00:00" } }
```

**Errors:**
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Email is required" |

### GET /api/v1/contact-messages

**Use Case:** UC-NTF-02 | **Auth:** Bearer token | **Role:** ADMIN

**Query:** `?isRead=true&page=0&size=20&sort=createdAt,desc`

**Response 200:** `{ "success": true, "data": { "content": [ContactMessageResponse], "totalElements": 50, "totalPages": 3 } }`

### GET /api/v1/contact-messages/unread-count

**Auth:** Bearer token | **Role:** ADMIN

**Response 200:** `{ "success": true, "data": 5 }`

### PUT /api/v1/contact-messages/{id}/read

**Auth:** Bearer token | **Role:** ADMIN

**Response 200:** ContactMessageResponse with `isRead: true, readAt: "...", readBy: "admin"`

### DELETE /api/v1/contact-messages/{id}

**Auth:** Bearer token | **Role:** ADMIN

**Response 204:** No content

---

## Endpoints — LeadController

### POST /api/v1/leads

**Use Case:** UC-NTF-03 | **Auth:** Public

**Request:**
```json
{ "email": "string", "name": "string", "phone": "string?", "source": "WEBSITE|FACEBOOK|ZALO|REFERRAL", "courseInterestId": "long?", "message": "string?" }
```

**Response 201:**
```json
{ "success": true, "data": { "id": 1, "email": "b@mail.com", "name": "Tran B", "phone": null, "source": "WEBSITE", "status": "NEW", "courseInterestId": 5, "message": "Quan tâm lớp guitar", "createdAt": "...", "updatedAt": "..." } }
```

### GET /api/v1/leads

**Use Case:** UC-NTF-04 | **Auth:** Bearer token | **Role:** ADMIN

**Query:** `?status=NEW&page=0&size=20`

**Response 200:** `{ "success": true, "data": { "content": [LeadResponse], "totalElements": 30 } }`

### GET /api/v1/leads/{id}

**Auth:** Bearer token | **Role:** ADMIN

**Response 200:** `{ "success": true, "data": LeadResponse }`

### PUT /api/v1/leads/{id}/status

**Auth:** Bearer token | **Role:** ADMIN

**Request:** `{ "status": "CONTACTED|CONVERTED|LOST" }`

### PUT /api/v1/leads/{id}

**Auth:** Bearer token | **Role:** ADMIN

**Request:** `{ "name": "string?", "phone": "string?", "message": "string?" }`

### DELETE /api/v1/leads/{id}

**Auth:** Bearer token | **Role:** ADMIN | **Response 204:** No content

**Errors (shared):**
| Status | Code | Message |
|--------|------|---------|
| 404 | LEAD_NOT_FOUND | "Lead not found" |
| 400 | INVALID_STATUS | "Invalid lead status transition" |
| 409 | DUPLICATE_EMAIL | "Lead with this email already exists" |

## Internal — EmailService

`sendContactNotification`, `sendLeadConfirmation`, `sendTemplateEmail` — triggered nội bộ, không có REST endpoint.
