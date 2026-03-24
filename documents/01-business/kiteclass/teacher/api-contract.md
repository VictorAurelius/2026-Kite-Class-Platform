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
