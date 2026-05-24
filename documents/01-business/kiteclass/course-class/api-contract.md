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

### POST /api/v1/classes/{classId}/reschedule
**Use Case:** UC-CRS-11 (Wave beta-readiness-4 Bucket D — GAP-291)  |  **Auth:** Bearer token + `@authz.hasAccessToClass(#classId)`  |  **Role:** ADMIN / TEACHER (owner of class)

Đổi lịch lớp học — giữ nguyên status `SCHEDULED`, ghi audit log (5 trường), publish Outbox event `class.rescheduled`. Notification classification = **OPERATIONAL** (bypass `marketing_consented` gate).

```json
// Request
{
  "newStartDate": "2026-05-21",                  // ISO YYYY-MM-DD, required
  "newEndDate":   "2026-07-07",                  // required, must be > newStartDate
  "reasonCategory": "GV_OM_BAN_DOT_XUAT",        // required enum: GV_OM_BAN_DOT_XUAT |
                                                 //   PHONG_HOC_KHONG_KHA_DUNG | MAT_DIEN_INTERNET |
                                                 //   LE_TET_NGHI_CHINH_THUC | HOC_SINH_XIN_NGHI_TAP_THE | LY_DO_KHAC
  "reasonNotes":  "Cô giáo phụ trách lớp xin nghỉ ốm 1 tuần."  // optional, ≤ 2000 chars
}

// Response 200 — Updated ClassResponse (status unchanged = SCHEDULED)
{
  "success": true,
  "data": {
    "id": 12345,
    "name": "Lớp Anh ngữ 5A1",
    "status": "SCHEDULED",
    "startDate": "2026-05-21",
    "endDate":   "2026-07-07",
    ...
  },
  "message": "Đã đổi lịch lớp học thành công"
}
```

**Error codes:**
| Status | Code | Khi nào |
|---|---|---|
| 400 | `CLASS_INVALID_DATES` | `newEndDate <= newStartDate` |
| 400 | Bean validation | `reasonCategory` thiếu HOẶC `reasonNotes` > 2000 chars |
| 403 | `ACCESS_DENIED` | Caller không phải owner/admin của class |
| 404 | `CLASS_NOT_FOUND` | classId không tồn tại hoặc đã bị xóa |
| 409 | `CLASS_CANNOT_RESCHEDULE` | Class không ở trạng thái SCHEDULED (đã IN_PROGRESS/COMPLETED/CANCELLED) |
| 500 | `RESCHEDULE_EVENT_SERIALIZATION_FAILED` | Outbox event serialization fails |

**Side effects:**
- `classes` row updated: 6 audit columns (`rescheduled_by_user_id`, `rescheduled_at`, `previous_start_date`, `previous_end_date`, `reschedule_reason_category`, `reschedule_reason_notes`)
- Outbox event `class.rescheduled` published cùng transaction (atomic guarantee)
- Default consumer: `ClassRescheduledNoOpConsumer` logs only
- Feature flag `kite.class.reschedule.notify.enabled=true` → `ClassRescheduledEmailConsumer` forwards to `class.rescheduled.email.queue` (kitehub-email render Thymeleaf + send)
- **Attendance + grade history PRESERVED** — không có new `ClassStatus.RESCHEDULED` enum (cross-bucket LOCKED decision §3.6 Wave beta-readiness-4)

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


---

## Pricing Model API (Wave beta-readiness-4 Bucket C, GAP-292)

### POST /api/v1/courses — extended với pricing_model + unit_price

Request body extended (additive, backward-compatible với clients gửi legacy `price`):

```json
{
  "name": "Lớp Anh ngữ 5A1 — Trung tâm Sky Education",
  "code": "ENG-5A1-2026",
  "pricingModel": "PER_HOUR",
  "unitPrice": 250000.00,
  "price": null,
  "...": "other existing fields"
}
```

Field semantics:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `pricingModel` | enum `PER_HOUR \| MONTHLY \| COURSE_PACKAGE \| FREE` | YES (defaults PER_HOUR if omitted, matches V67 DEFAULT) | BR-COURSE-PRICING-001 |
| `unitPrice` | decimal (NUMERIC 19,2 VND) | YES | BR-COURSE-PRICING-002; CHECK ≥ 0; FREE → must = 0 |
| `price` | decimal | NO (deprecated) | Legacy flat-fee. New code should NOT send; backward-compat preserved |

Response body extended:

```json
{
  "success": true,
  "data": {
    "id": 42,
    "name": "Lớp Anh ngữ 5A1 — Trung tâm Sky Education",
    "pricingModel": "PER_HOUR",
    "unitPrice": 250000.00,
    "price": null,
    "...": "other existing fields"
  }
}
```

| Status | Code | Message |
|--------|------|---------|
| 400 | VALIDATION_PRICING_MODEL_REQUIRED | "Hình thức tính học phí không được để trống" |
| 400 | VALIDATION_UNIT_PRICE_NEGATIVE | "Đơn giá phải >= 0" |
| 400 | VALIDATION_FREE_PRICING_NONZERO | "Khóa học FREE phải có đơn giá = 0" |
| 409 | PRICING_MODEL_IMMUTABLE | "Không thể đổi hình thức tính học phí khi đã có học viên đăng ký" (BR-COURSE-PRICING-003) |

### PUT /api/v1/courses/{id} — pricing fields restricted

- `pricingModel` đổi được CHỈ khi Course chưa có active enrollment (BR-COURSE-PRICING-003). Service layer check + return 409 nếu vi phạm.
- `unitPrice` đổi được mọi lúc (giá có thể adjust, model không).

### Cross-references

- **Business rules:** `documents/01-business/kiteclass/course-class/rules.md` §6
- **ADR:** `documents/02-architecture/adr/ADR-035-pricing-model-taxonomy.md`
- **Migration:** V67 (DDL), R67 (rollback)
- **Pre-migration audit:** `scripts/audit-pre-pricing-model.sql`
