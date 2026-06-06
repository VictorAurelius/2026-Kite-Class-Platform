# Teacher — API Contract

## TeacherController — `/api/v1/teachers`

### POST /api/v1/teachers
**Use Case:** UC-TCH-01  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "name": "string", "email": "string", "phoneNumber": "string", "specialization": "string", "bio": "string", "qualification": "string", "experienceYears": "int" }
// Response 200
{ "success": true, "data": { "id": "long", "name": "string", "email": "string", "phoneNumber": "string", "specialization": "string", "bio": "string", "qualification": "string", "experienceYears": "int", "avatarUrl": "string", "status": "string" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Name is required" |
| 409 | DUPLICATE_EMAIL | "Email already exists" |

### POST /api/v1/teachers/{id}/credentials
**Use Case:** UC-AUTH-02 (xem `tenant-auth/`)  |  **Auth:** Bearer token  |  **Role:** OWNER, ADMIN, PRINCIPAL
Set/reset teacher KC-native login password (Wave auth-1, Hướng B). Provision/UPSERT `auth_credentials` (entity_type=TEACHER, entity_id=teacher.id, email=teacher.email). Email + role lấy từ teacher entity; request chỉ mang password.
```json
// Request — SetPasswordRequest
{ "password": "string (8-100 chars, regex: letter + digit + special)" }
// Response 200
{ "success": true, "data": null, "message": "Đặt mật khẩu giáo viên thành công" }
```
| Status | Code | Message |
|--------|------|---------|
| 403 | — | Forbidden (caller không phải OWNER/ADMIN/PRINCIPAL) |
| 400 | VALIDATION_ERROR | "Mật khẩu phải từ 8-100 ký tự" / "Mật khẩu phải có chữ, số và ký tự đặc biệt" |
| 404 | TEACHER_NOT_FOUND | "Teacher not found" |

Chi tiết đầy đủ: `tenant-auth/api-contract.md` §2.

### GET /api/v1/teachers/{id}
**Use Case:** UC-TCH-02  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Response 200:** `ApiResponse<TeacherResponse>` (same fields as above)
- **404:** `TEACHER_NOT_FOUND` — "Teacher not found"

### GET /api/v1/teachers
**Use Case:** UC-TCH-03  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **Query:** `?search=&status=&page=0&size=20&sort=name,asc`
- **Response 200:** `ApiResponse<PageResponse<TeacherResponse>>`

### PUT /api/v1/teachers/{id}
**Use Case:** UC-TCH-04  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "name": "string", "email": "string", "phoneNumber": "string", "specialization": "string", "bio": "string", "qualification": "string", "experienceYears": "int", "status": "string" }
```
| Status | Code | Message |
|--------|------|---------|
| 404 | TEACHER_NOT_FOUND | "Teacher not found" |
| 409 | DUPLICATE_EMAIL | "Email already exists" |

### DELETE /api/v1/teachers/{id}
**Use Case:** UC-TCH-05  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **Response 200:** `ApiResponse<Void>`
- **404:** `TEACHER_NOT_FOUND` — "Teacher not found"

### GET /api/v1/teachers/search
**Use Case:** UC-TCH-06  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **Query:** `?specialization=&page=0&size=20&sortBy=name&direction=asc`
- **Response 200:** `ApiResponse<PageResponse<TeacherResponse>>`

---

## TeacherClassController — `/api/v1/classes/{classId}/teachers`

### POST /api/v1/classes/{classId}/teachers
**Use Case:** UC-TCH-07  |  **Auth:** Bearer token  |  **Role:** ADMIN, OWNER, CREATOR
```json
// Request
{ "teacherId": "long", "role": "MAIN_TEACHER | ASSISTANT" }
// Response 200
{ "success": true, "data": { "classId": "long", "teacherId": "long", "role": "string" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | Teacher not ACTIVE | "Only active teachers can be assigned to new classes" |
| 409 | Already assigned | "Teacher already assigned to this class" |

### DELETE /api/v1/classes/{classId}/teachers/{teacherId}
**Use Case:** UC-TCH-07  |  **Auth:** Bearer token  |  **Role:** ADMIN, OWNER
- **Response 200:** `ApiResponse<Void>`
- **404:** `TEACHER_NOT_FOUND` — "Teacher not found in class"

> **Note — UC-TCH-08 (Independent Teacher/Owner Flow):** No dedicated endpoint. OWNER role bypasses all `teacher_classes` permission checks automatically (BR-TCH-007). Owner auto-assigned as CREATOR on course creation and MAIN_TEACHER on class creation.

---

## Internal Teacher API — `/internal/teachers`

### GET /internal/teachers/{id}
**Use Case:** UC-TCH-09  |  **Auth:** Internal service token
- **Response 200:** `TeacherResponse`
- **404:** Teacher not found

### POST /internal/teachers
**Use Case:** UC-TCH-09  |  **Auth:** Internal service token
```json
// Request
{ "name": "string", "email": "string", "instanceId": "uuid" }
// Response 201
{ "id": "long", "name": "string", "email": "string", "status": "ACTIVE" }
```

### DELETE /internal/teachers/{id}
**Use Case:** UC-TCH-09  |  **Auth:** Internal service token
- **Response 204:** No content

---

## Common Response Wrapper

All endpoints return `ApiResponse<T>`:
```json
{
  "success": true,
  "data": { },
  "message": "string",
  "timestamp": "datetime"
}
```

Error response:
```json
{
  "success": false,
  "error": { "code": "ERROR_CODE", "message": "Human-readable message" },
  "timestamp": "datetime"
}
```

## Pagination Response

`PageResponse<T>` fields:
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```
