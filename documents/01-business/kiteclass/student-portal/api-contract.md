# Student Portal — API Contract

**Domain:** kiteclass-core read APIs serving kc-student FE production routes
**Status:** Phase 1 v1 — endpoint contracts published; full data joins follow when FE consumer PR lands
**Created:** 2026-05-10 (Wave 51 Bucket B — GAP-269b)

All endpoints below are **read-only** (per BR-STUDENT-PORTAL-005). All require Gateway-injected `X-User-Reference-Id` header (Long). **Option B (Wave auth-1):** Gateway re-inject từ `referenceId` claim của KC-native token (= `auth_credentials.entity_id`, entity_type=STUDENT) sau khi strip client value (anti-spoof); xem `tenant-auth/api-contract.md` §3. **Option A (superseded):** mapped from `users.reference_id` when `userType = STUDENT`. Missing header → `401 AUTH_REQUIRED`.

Response envelope: `ApiResponse<T>` with `{success, data, message, code, path, timestamp}` (project standard).

---

## Endpoints

### GET `/api/v1/students/me/today`

Today's schedule + assignments due today for the calling student.

- **Headers:** `X-User-Reference-Id: <studentReferenceId>` (Long, required)
- **Response 200:**
  ```json
  {
    "success": true,
    "data": {
      "date": "2026-05-10",
      "schedulePeriods": [
        {
          "periodNo": 1,
          "subjectName": "Toán",
          "teacherName": "Nguyễn Văn A",
          "startTime": "07:00",
          "endTime": "07:45",
          "room": "P301"
        }
      ],
      "assignmentsDueToday": [
        {
          "assignmentId": 42,
          "title": "Bài tập chương 3",
          "subjectName": "Toán",
          "dueAt": "2026-05-10T23:59:00Z",
          "status": "PENDING"
        }
      ]
    }
  }
  ```
- **Errors:**
  - `401 AUTH_REQUIRED` — header missing
- **Phase 1 v1 stub:** returns `{date: today, schedulePeriods: [], assignmentsDueToday: []}`

---

### GET `/api/v1/students/me/grades`

Per-subject grades summary for the calling student.

- **Headers:** `X-User-Reference-Id` (Long, required)
- **Response 200:**
  ```json
  {
    "success": true,
    "data": [
      {
        "subjectId": 7,
        "subjectName": "Toán",
        "average": 8.5,
        "highest": 9.5,
        "lowest": 7.0,
        "entryCount": 5
      }
    ]
  }
  ```
- **Errors:** `401 AUTH_REQUIRED`
- **Phase 1 v1 stub:** returns `[]`

---

### GET `/api/v1/students/me/grades/{subjectId}`

Detailed grade entries for one subject the student is enrolled in.

- **Path:** `subjectId` (Long)
- **Headers:** `X-User-Reference-Id` (Long, required)
- **Response 200:**
  ```json
  {
    "success": true,
    "data": {
      "subjectId": 7,
      "subjectName": "Toán",
      "average": 8.5,
      "entries": [
        {
          "entryId": 101,
          "type": "TX",
          "label": "Kiểm tra 15p",
          "score": 8.0,
          "maxScore": 10.0,
          "gradedAt": "2026-05-08",
          "comment": "Cần luyện tập thêm"
        }
      ]
    }
  }
  ```
- **Errors:**
  - `401 AUTH_REQUIRED`
  - `404 STUDENT_PORTAL_SUBJECT_NOT_FOUND` — student not enrolled in `subjectId` (Phase 2+ once enrollment join lands)
- **Phase 1 v1 stub:** returns `{subjectId: <input>, subjectName: null, average: null, entries: []}`

---

### GET `/api/v1/students/me/payments`

Invoice list for the calling student (own invoices only per BR-STUDENT-PORTAL-001 / PDPL data-minimization).

- **Headers:** `X-User-Reference-Id` (Long, required)
- **Response 200:**
  ```json
  {
    "success": true,
    "data": [
      {
        "invoiceId": 555,
        "invoiceNumber": "INV-2026-0042",
        "description": "Học phí tháng 5/2026",
        "amountDue": 2500000,
        "amountPaid": 2500000,
        "dueDate": "2026-05-05",
        "status": "PAID",
        "currency": "VND"
      }
    ]
  }
  ```
- **Status enum:** `PENDING / PARTIALLY_PAID / PAID / OVERDUE / CANCELLED`
- **Errors:** `401 AUTH_REQUIRED`
- **Phase 1 v1 stub:** returns `[]`
- **Note:** Read-only per BR-STUDENT-PORTAL-005 — settlement happens via owner-side payment flow

---

### GET `/api/v1/students/me/notifications`

Cursor-paginated notification feed for the calling student.

- **Headers:** `X-User-Reference-Id` (Long, required)
- **Query:**
  - `cursor` (String, optional) — opaque cursor; omit for first page
  - `limit` (Integer, optional) — page size; default 20; clamped to `[1..100]` (out-of-range silently clamped per BR-STUDENT-PORTAL-003)
- **Response 200:**
  ```json
  {
    "success": true,
    "data": {
      "items": [
        {
          "id": 999,
          "category": "GRADE_PUBLISHED",
          "title": "Điểm Toán đã được công bố",
          "body": "Giáo viên vừa công bố điểm kiểm tra giữa kỳ.",
          "createdAt": "2026-05-10T14:32:00Z",
          "read": false
        }
      ],
      "nextCursor": "eyJjcmVhdGVkX2F0IjogIjIwMjYtMDUtMTBUMTQ6MzI6MDBaIn0="
    }
  }
  ```
- **Category enum (extensible):** `GRADE_PUBLISHED / ATTENDANCE_FLAG / PAYMENT_DUE / GENERIC` (more added cùng FE consumer)
- **Errors:** `401 AUTH_REQUIRED`
- **Phase 1 v1 stub:** returns `{items: [], nextCursor: null}`
- **Pagination contract:** FE stops calling once `nextCursor` is null. Cursor value is opaque — FE MUST NOT parse.

---

## Cross-references

- **Use Cases:** `UC-STUDENT-PORTAL-01 .. 05` (`use-cases.md`)
- **Business Rules:** `BR-STUDENT-PORTAL-001 .. 005` (`rules.md`)
- **Implementation:**
  - Controller: `kiteclass-core/src/main/java/com/kiteclass/core/module/student/portal/controller/StudentPortalController.java`
  - Service: `.../portal/service/StudentPortalService.java` + `StudentPortalServiceImpl.java`
  - DTOs: `.../portal/dto/Student{Today,GradeOverview,GradeDetailResponse,PaymentSummary,NotificationItem,NotificationFeedResponse}.java`
  - Tests: `.../student/portal/controller/StudentPortalControllerIT.java`
- **Wave history:** Wave 51 Bucket B (this PR) — endpoint contracts + auth scope guard + empty-payload v1 stubs
- **Future:** FE swap-to-real-data PR landing alongside service-layer joins against `ClassSchedule` / `Assignment` / `SubjectGrade` / `Invoice` / notification feed entities

---

## Phase 1 v1 vs Phase 2 split

| Aspect | Phase 1 v1 (this PR) | Phase 2 (follow-up) |
|--------|----------------------|---------------------|
| Endpoint contract | ✅ Published + stable shapes | (no change) |
| Auth header validation | ✅ Active | (no change) |
| Response shape | ✅ Empty / null fields populated | Real joined data |
| Service-layer joins | ❌ Returns empty | ✅ ClassSchedule/Assignment/Grade/Invoice/Notification joins |
| 404 enrollment scope on subject detail | ❌ Returns empty body | ✅ Throws `STUDENT_PORTAL_SUBJECT_NOT_FOUND` |
| Integration tests | ✅ WebMvcTest with mocked service | + repository-layer tests for join logic |

This split is intentional per `gap-done-discipline.md` §3 PARTIAL exit-ramp — endpoint contracts ship NOW so the FE can wire stable shapes; full join logic ships alongside the FE consumer PR to avoid building both ends in isolation.
