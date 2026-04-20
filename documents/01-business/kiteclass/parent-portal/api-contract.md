# Parent Portal — API Contract

**Domain:** KiteClass Core / Parent Portal
**Version:** 1.0 (Wave 2 MVP)
**Updated:** 2026-04-19
**Module:** `kiteclass-core` package `com.kiteclass.core.module.parent.controller`

---

## Authentication & Headers

| Header | Source | Required for |
|--------|--------|--------------|
| `X-User-Id` | Gateway (admin/teacher JWT) | `POST /api/v1/parent-invitations` (UC-PARENT-01) |
| `X-Tenant-Id` | Gateway (sub-domain → instanceId) | `POST /api/v1/parent-invitations/redeem/{token}` (UC-PARENT-02) |
| `X-User-Reference-Id` | Gateway (`users.reference_id` cho `userType=PARENT`) | `GET /api/v1/parent/me`, `GET /api/v1/parent/me/children` |
| HMAC signature | `InternalRequestFilter` | `GET /internal/parents/{id}` |

---

## ParentInvitationController — `/api/v1/parent-invitations`

### POST /api/v1/parent-invitations
**Use Case:** UC-PARENT-01 | **Auth:** Bearer + `X-User-Id` + `X-Tenant-Id` | **Role:** ADMIN, TEACHER (enforce ở Gateway)

```json
// Request — InviteParentRequest
{
  "studentId": 123,
  "parentEmail": "phuhuynh@example.com"
}

// Response 201 — ApiResponse<ParentInvitationResponse>
{
  "success": true,
  "message": "Lời mời đã được gửi",
  "data": {
    "id": 456,
    "email": "phuhuynh@example.com",
    "studentId": 123,
    "studentName": "Nguyễn Văn A",
    "status": "PENDING",
    "expiresAt": "2026-04-20T10:00:00Z",
    "token": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

| Status | Code | Condition | Message |
|--------|------|-----------|---------|
| 401 | AUTH_REQUIRED | `X-User-Id` thiếu | "AUTH_REQUIRED" |
| 503 | PARENT_PORTAL_DISABLED | Feature flag tắt | "PARENT_PORTAL_DISABLED" |
| 404 | STUDENT_NOT_FOUND | Student không tồn tại / soft-deleted | "STUDENT_NOT_FOUND" |
| 409 | PARENT_EMAIL_EXISTS | Parent ACTIVE đã tồn tại cho email trong tenant | "PARENT_EMAIL_EXISTS" |
| 400 | VALIDATION_ERROR | studentId null hoặc email blank/sai format | "studentId là bắt buộc" / "Email không hợp lệ" |

**Note:** `token` chỉ trả ở response create (BR-PARENT-INV-008) — không bao giờ trả lại ở list endpoints.

---

### POST /api/v1/parent-invitations/redeem/{token}
**Use Case:** UC-PARENT-02 | **Auth:** Public + `X-Tenant-Id` (Gateway populate) | **Role:** Public

```json
// Request — RedeemInvitationRequest
{
  "password": "Phuhuynh@2026",
  "fullName": "Trần Thị B",
  "phoneNumber": "0912345678",
  "relationship": "MOTHER"
}

// Response 200 — ApiResponse<RedeemInvitationResult>
{
  "success": true,
  "message": "Kích hoạt tài khoản thành công",
  "data": {
    "parentId": 789,
    "email": "phuhuynh@example.com",
    "fullName": "Trần Thị B",
    "phoneNumber": "0912345678",
    "relationship": "MOTHER",
    "linkedStudentIds": [123]
  }
}
```

| Status | Code | Condition | Message |
|--------|------|-----------|---------|
| 503 | PARENT_PORTAL_DISABLED | Feature flag tắt | "PARENT_PORTAL_DISABLED" |
| 404 | PARENT_INVITATION_NOT_FOUND | Token sai / cross-tenant (BR-PARENT-INV-010) | "PARENT_INVITATION_NOT_FOUND" |
| 400 | PARENT_INVITATION_ALREADY_USED | Status REDEEMED/EXPIRED/REVOKED | "PARENT_INVITATION_ALREADY_USED" |
| 400 | PARENT_INVITATION_EXPIRED | `expiresAt < now` | "PARENT_INVITATION_EXPIRED" |
| 400 | VALIDATION_ERROR | Password yếu / fullName invalid / phone sai | (xem messages dưới) |

**Validation messages (vi):**
- Password: "Mật khẩu là bắt buộc" / "Mật khẩu phải từ 8-100 ký tự" / "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt"
- FullName: "Tên là bắt buộc" / "Tên phải từ 2-100 ký tự"
- Phone: "Số điện thoại không hợp lệ (phải là 10 số bắt đầu bằng 0)" (cho phép empty)
- Relationship: "Quan hệ phải là FATHER, MOTHER hoặc GUARDIAN" (cho phép empty → default GUARDIAN)

**Idempotency:** Gọi 2 lần với token đã REDEEMED → `400 PARENT_INVITATION_ALREADY_USED` (không re-create). Cùng email + token khác (child #2) → re-use Parent ACTIVE, tạo link mới (BR-PARENT-INV-004).

**Public route alias:** Gateway `POST /api/v1/auth/register-parent/{token}` forward tới endpoint này.

---

## ParentController — `/api/v1/parent` (Self-Service)

### GET /api/v1/parent/me
**Use Case:** UC-PARENT-03 | **Auth:** `X-User-Reference-Id` | **Role:** PARENT

```json
// Response 200 — ApiResponse<ParentResponse>
{
  "success": true,
  "data": {
    "id": 789,
    "fullName": "Trần Thị B",
    "email": "phuhuynh@example.com",
    "phoneNumber": "0912345678",
    "relationship": "MOTHER",
    "status": "ACTIVE"
  }
}
```

| Status | Code | Condition | Message |
|--------|------|-----------|---------|
| 401 | AUTH_REQUIRED | Header thiếu | "AUTH_REQUIRED" |
| 404 | PARENT_NOT_FOUND | Parent không tồn tại / cross-tenant / soft-deleted | "PARENT_NOT_FOUND" |

---

### GET /api/v1/parent/me/children
**Use Case:** UC-PARENT-04 | **Auth:** `X-User-Reference-Id` | **Role:** PARENT

```json
// Response 200 — ApiResponse<List<ChildSummaryResponse>>
{
  "success": true,
  "data": [
    {
      "studentId": 123,
      "studentName": "Nguyễn Văn A",
      "className": null,
      "grade": null,
      "linkType": "PRIMARY"
    }
  ]
}
```

| Status | Code | Condition | Message |
|--------|------|-----------|---------|
| 401 | AUTH_REQUIRED | Header thiếu | "AUTH_REQUIRED" |
| 404 | PARENT_NOT_FOUND | Parent assert tenant fail | "PARENT_NOT_FOUND" |

**Note:** `className` + `grade` luôn `null` ở Wave 2 MVP (Wave 5 sẽ JOIN với `homeroom_classes` / `subject_grades` để populate).

---

## InternalParentController — `/internal/parents` (Service-to-Service)

### GET /internal/parents/{id}
**Use Case:** UC-PARENT-06 | **Auth:** HMAC signature (`InternalRequestFilter`) | **Visibility:** `@Hidden` khỏi public Swagger

```json
// Response 200 — ApiResponse<ParentInternalResponse>
{
  "success": true,
  "data": {
    "id": 789,
    "email": "phuhuynh@example.com",
    "fullName": "Trần Thị B",
    "phoneNumber": "0912345678",
    "relationship": "MOTHER",
    "status": "ACTIVE",
    "linkedStudentIds": [123, 456]
  }
}
```

| Status | Code | Condition | Message |
|--------|------|-----------|---------|
| 401 | INVALID_SIGNATURE | HMAC sai (filter-level) | "INVALID_SIGNATURE" |
| 404 | PARENT_NOT_FOUND | Parent không tồn tại | "PARENT_NOT_FOUND" |

---

## Endpoint Summary

| Method | Path | Use Case | Auth | Visibility |
|--------|------|----------|------|------------|
| POST | `/api/v1/parent-invitations` | UC-PARENT-01 | Admin Bearer + `X-User-Id` | Public Swagger (Tag: Parent Invitation) |
| POST | `/api/v1/parent-invitations/redeem/{token}` | UC-PARENT-02 | Public + `X-Tenant-Id` | Public Swagger (Tag: Parent Invitation) |
| GET | `/api/v1/parent/me` | UC-PARENT-03 | `X-User-Reference-Id` | Public Swagger (Tag: Parent Self-Service) |
| GET | `/api/v1/parent/me/children` | UC-PARENT-04 | `X-User-Reference-Id` | Public Swagger (Tag: Parent Self-Service) |
| GET | `/internal/parents/{id}` | UC-PARENT-06 | HMAC | `@Hidden` |

**Total:** 5 endpoints (4 public + 1 internal)

---

## Standard Response Envelope

Tất cả endpoints trả `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Optional message (vi)",
  "data": { /* T */ },
  "errorCode": null
}
```

Error envelope:
```json
{
  "success": false,
  "message": "Tin nhắn lỗi (vi)",
  "data": null,
  "errorCode": "PARENT_INVITATION_EXPIRED"
}
```

---

## Error Code Reference

| Code | HTTP | Domain | Source |
|------|------|--------|--------|
| `AUTH_REQUIRED` | 401 | Common | `BusinessException` |
| `PARENT_PORTAL_DISABLED` | 503 | Parent | Feature flag (BR-PARENT-004) |
| `STUDENT_NOT_FOUND` | 404 | Student | `EntityNotFoundException` |
| `PARENT_EMAIL_EXISTS` | 409 | Parent | `DuplicateResourceException` (BR-PARENT-INV-002) |
| `PARENT_INVITATION_NOT_FOUND` | 404 | Parent | Cross-tenant guard (BR-PARENT-INV-010) |
| `PARENT_INVITATION_ALREADY_USED` | 400 | Parent | Status state machine (BR-PARENT-INV-003) |
| `PARENT_INVITATION_EXPIRED` | 400 | Parent | TTL check (BR-PARENT-003) |
| `PARENT_NOT_FOUND` | 404 | Parent | `EntityNotFoundException` |
| `VALIDATION_ERROR` | 400 | Common | Bean Validation (Vietnamese messages) |
| `INVALID_SIGNATURE` | 401 | Internal | `InternalRequestFilter` |

---

## OpenAPI Tags

- `Parent Invitation` — `description = "Parent onboarding via token-based invitations (GAP-052a)"`
- `Parent Self-Service` — `description = "Parent portal endpoints (GAP-052a)"`
- `Internal Parent API` — `description = "Service-to-service parent profile lookup"` (hidden)
