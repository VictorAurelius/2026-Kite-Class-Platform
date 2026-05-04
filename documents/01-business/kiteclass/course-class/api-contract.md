# Course & Class — API Contract

**Version:** 1.1.0
**Updated:** 2026-05-04 (GAP-290 Wave 18a — POST /sessions/generate-from-recurrence)

## CourseController — `/api/v1/courses`

### POST /api/v1/courses
**Use Case:** UC-CRS-01  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "name": "string", "code": "string", "description": "string", "syllabus": "string", "objectives": "string", "prerequisites": "string", "targetAudience": "string", "teacherId": "long", "durationWeeks": "int", "totalSessions": "int", "price": "decimal", "level": "string", "category": "string" }
// Response 200
{ "success": true, "data": { "id": "long", "name": "string", "code": "string", "description": "string", "syllabus": "string", "objectives": "string", "prerequisites": "string", "prerequisiteCourses": ["long"], "targetAudience": "string", "teacherId": "long", "durationWeeks": "int", "totalSessions": "int", "price": "decimal", "status": "string", "coverImageUrl": "string", "level": "string", "category": "string", "createdAt": "datetime", "updatedAt": "datetime" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Name is required" |
| 409 | DUPLICATE_CODE | "Course code already exists" |

### GET /api/v1/courses/{id}
**Use Case:** UC-CRS-02  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER, STUDENT
- **Response 200:** `ApiResponse<CourseResponse>` — **404:** `COURSE_NOT_FOUND`

### GET /api/v1/courses
**Use Case:** UC-CRS-03  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
- **Query:** `?page=0&size=20` — **Response 200:** `ApiResponse<PageResponse<CourseResponse>>`

### PUT /api/v1/courses/{id}
**Use Case:** UC-CRS-04  |  **Auth:** Bearer token  |  **Role:** ADMIN
- Request: same fields as create — **404:** `COURSE_NOT_FOUND`

### DELETE /api/v1/courses/{id}
**Use Case:** UC-CRS-05  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **Response 200:** `ApiResponse<Void>` — **404:** `COURSE_NOT_FOUND`

### GET /api/v1/courses/search
**Use Case:** UC-CRS-03  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER, STUDENT
- **Query:** `?level=&category=&page=0&size=20&sortBy=name&direction=asc`

### POST /api/v1/courses/{id}/publish
**Use Case:** UC-CRS-06  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **400:** `INVALID_STATUS` — "Only DRAFT courses can be published"

### POST /api/v1/courses/{id}/archive
**Use Case:** UC-CRS-07  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **400:** `INVALID_STATUS` — "Only PUBLISHED courses can be archived"

### POST /api/v1/courses/{id}/prerequisites/{prerequisiteId}
**Use Case:** UC-CRS-08  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **Response 200:** `ApiResponse<Void>` — **409:** `CIRCULAR_PREREQUISITE`

### DELETE /api/v1/courses/{id}/prerequisites/{prerequisiteId}
**Use Case:** UC-CRS-08  |  **Auth:** Bearer token  |  **Role:** ADMIN

---

## ClassController

### POST /api/v1/courses/{courseId}/classes
**Use Case:** UC-CRS-09  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "name": "string", "description": "string", "schedule": "string", "locationType": "string", "locationDetail": "string", "startDate": "date", "endDate": "date", "maxStudents": "int" }
// Response 200 — ClassResponse
{ "success": true, "data": { "id": "long", "courseId": "long", "name": "string", "description": "string", "schedule": "string", "locationType": "string", "locationDetail": "string", "startDate": "date", "endDate": "date", "maxStudents": "int", "currentEnrolled": "int", "classCode": "string", "codeExpiresAt": "datetime", "status": "string", "startedAt": "datetime", "completedAt": "datetime", "cancelledAt": "datetime", "createdAt": "datetime", "updatedAt": "datetime" } }
```
| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_ERROR | "Name is required" |
| 404 | COURSE_NOT_FOUND | "Course not found" |

### GET /api/v1/courses/{courseId}/classes
**Use Case:** UC-CRS-09  |  **Query:** `?page=0&size=20`

### GET /api/v1/classes/{classId}
**Use Case:** UC-CRS-09  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER

### PATCH /api/v1/classes/{classId}
**Use Case:** UC-CRS-09  |  **Auth:** Bearer token  |  **Role:** ADMIN

### DELETE /api/v1/classes/{classId}
**Use Case:** UC-CRS-09  |  **Auth:** Bearer token  |  **Role:** ADMIN

### POST /api/v1/classes/{classId}/start
**Use Case:** UC-CRS-10  |  **Auth:** Bearer token  |  **Role:** ADMIN
- **400:** `INVALID_STATUS` — "Only SCHEDULED classes can be started"

### POST /api/v1/classes/{classId}/complete
**Use Case:** UC-CRS-10  |  **Auth:** Bearer token  |  **Role:** ADMIN

### POST /api/v1/classes/{classId}/cancel
**Use Case:** UC-CRS-10  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "reason": "string" }
```

### POST /api/v1/classes/{classId}/generate-code
**Use Case:** UC-CRS-09  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
```json
// Request
{ "customCode": "string?", "expiresAt": "datetime?" }
// Response 200
{ "success": true, "data": { "classCode": "string", "expiresAt": "datetime" } }
```

### POST /api/v1/classes/{classId}/schedule
**Use Case:** UC-CRS-09  |  **Auth:** Bearer token  |  **Role:** ADMIN
```json
// Request
{ "daysOfWeek": ["string"], "startTime": "time", "endTime": "time" }
// Response 200 — List<ClassSessionResponse>
{ "success": true, "data": [{ "id": "long", "classId": "long", "sessionNumber": "int", "sessionDate": "date", "startTime": "time", "endTime": "time", "location": "string", "topic": "string", "status": "string", "attendanceTaken": "boolean" }] }
```

### GET /api/v1/classes/{classId}/sessions
**Use Case:** UC-CRS-09  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER

### POST /api/v1/classes/{classId}/sessions/generate-from-recurrence
**Use Case:** UC-CLASS-RECURRING  |  **Auth:** Bearer token  |  **Role:** ADMIN, TEACHER
**Since:** GAP-290 / Wave 18a (2026-05-04)

```json
// Request — RecurrenceRuleDto (RFC 5545 RRULE subset; Phase 1: WEEKLY only)
{
  "freq": "WEEKLY",
  "by_day": ["TU", "TH"],
  "start_time": "19:00",
  "end_time": "20:30",
  "until": "2026-08-01",
  "exclude_dates": ["2026-06-15"]
}

// Response 200 — Merged session list (preserved + new) ordered by sessionNumber
{
  "success": true,
  "message": "Đã tạo 24 buổi học (lịch lặp lại)",
  "data": [
    {
      "id": 101,
      "classId": 42,
      "sessionNumber": 1,
      "sessionDate": "2026-05-05",
      "startTime": "19:00",
      "endTime": "20:30",
      "location": null,
      "topic": null,
      "status": "SCHEDULED",
      "attendanceTaken": false
    }
  ]
}
```

**Field reference:**

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| `freq` | enum | yes | Phase 1: `WEEKLY` only |
| `by_day` | array&lt;enum&gt; | yes | Subset of `MO/TU/WE/TH/FR/SA/SU` (RFC 5545) |
| `start_time` | LocalTime | yes | `HH:mm`, FE renders in `Asia/Ho_Chi_Minh` |
| `end_time` | LocalTime | yes | Must be strictly after `start_time` |
| `until` | LocalDate | yes | Last calendar date (inclusive) |
| `exclude_dates` | array&lt;LocalDate&gt; | no | Skipped dates (holidays, breaks) |

**Errors (4xx):**

| HTTP | code | When |
|------|------|------|
| 400 | `RECURRENCE_INVALID_TIME` | `end_time <= start_time` |
| 400 | `RECURRENCE_INVALID_RANGE` | `until < startDate` |
| 400 | `RECURRENCE_NO_DAYS` | `by_day` empty |
| 400 | `RECURRENCE_RANGE_TOO_LARGE` | `until - start > 3700 days` |
| 400 | `CLASS_RECURRENCE_LOCKED` | Class is `COMPLETED` or `CANCELLED` |
| 404 | `CLASS_NOT_FOUND` | Unknown `classId` |

**State machine on edit** (BR-CLASS-009):
- Past sessions (`sessionDate < today`) — preserved untouched.
- Sessions with `attendanceTaken=true` — preserved untouched (regardless of date).
- Future `SCHEDULED` sessions with `attendanceTaken=false` — soft-deleted, regenerated from new rule.
- Idempotent — re-running with same rule yields the same merged result.
