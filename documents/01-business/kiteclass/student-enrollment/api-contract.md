# Student & Enrollment — API Contract

## StudentController — `/api/v1/students`

### POST /api/v1/students
**Use Case:** UC-STU-01  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "name": "string", "email": "string", "phone": "string", "dateOfBirth": "date", "gender": "string", "address": "string", "note": "string" }
// Response 200
{ "success": true, "data": { "id": "long", "name": "string", "email": "string", "phone": "string", "dateOfBirth": "date", "gender": "string", "address": "string", "avatarUrl": "string", "status": "string", "note": "string" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Name is required" |
| 409 | DUPLICATE_EMAIL | "Email already exists" |

### GET /api/v1/students/{id}
**Use Case:** UC-STU-02  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Response 200:** `ApiResponse<StudentResponse>` (same as above)
- **404:** `STUDENT_NOT_FOUND` — "Student not found"

### GET /api/v1/students
**Use Case:** UC-STU-03  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Query:** `?search=&status=&page=0&size=20&sort=name,asc`
- **Response 200:** `ApiResponse<PageResponse<StudentResponse>>`

### PUT /api/v1/students/{id}
**Use Case:** UC-STU-04  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "name": "string", "email": "string", "phone": "string", "dateOfBirth": "date", "gender": "string", "address": "string", "status": "string", "note": "string" }
```
| Status | Code | Message |
|--------|------|---------|
| 404 | STUDENT_NOT_FOUND | "Student not found" |
| 409 | DUPLICATE_EMAIL | "Email already exists" |

### DELETE /api/v1/students/{id}
**Use Case:** UC-STU-05  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **Response 200:** `ApiResponse<Void>`
- **404:** `STUDENT_NOT_FOUND`

---

## EnrollmentController — `/api/v1/enrollments`

### POST /api/v1/enrollments
**Use Case:** UC-STU-06  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "studentId": "long", "classId": "long", "tuitionAmount": "decimal", "discountPercent": "decimal", "notes": "string" }
// Response 200
{ "success": true, "data": { "id": "long", "studentId": "long", "classId": "long", "enrollmentDate": "date", "status": "string", "tuitionAmount": "decimal", "discountPercent": "decimal", "finalAmount": "decimal", "notes": "string", "createdAt": "datetime", "updatedAt": "datetime" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Student ID is required" |
| 404 | STUDENT_NOT_FOUND | "Student not found" |
| 404 | CLASS_NOT_FOUND | "Class not found" |
| 409 | ALREADY_ENROLLED | "Student already enrolled in this class" |

### GET /api/v1/enrollments/{id}
**Use Case:** UC-STU-06  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Response 200:** `ApiResponse<EnrollmentResponse>`

### GET /api/v1/enrollments/student/{studentId}
**Use Case:** UC-STU-07  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER, STUDENT
- **Query:** `?page=0&size=20`
- **Response 200:** `ApiResponse<Page<EnrollmentResponse>>`

### GET /api/v1/enrollments/class/{classId}
**Use Case:** UC-STU-07  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Query:** `?page=0&size=20`
- **Response 200:** `ApiResponse<Page<EnrollmentResponse>>`

### PUT /api/v1/enrollments/{id}/status
**Use Case:** UC-STU-08  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "status": "string", "notes": "string" }
```
| Status | Code | Message |
|--------|------|---------|
| 404 | ENROLLMENT_NOT_FOUND | "Enrollment not found" |
| 400 | INVALID_STATUS | "Invalid status transition" |

### PUT /api/v1/enrollments/{id}/withdraw
**Use Case:** UC-STU-08  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **Response 200:** `ApiResponse<EnrollmentResponse>` with status=WITHDRAWN
- **404:** `ENROLLMENT_NOT_FOUND`
