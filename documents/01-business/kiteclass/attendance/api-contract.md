# Attendance — API Contract

**Domain:** KiteClass Core
**Audience:** mixed (Claude + dev — backend, frontend, integration)
**Version:** 2.0
**Last-Updated:** 2026-05-26 (Wave beta-readiness-6 Bucket B — GAP-232 sync)

> **Source of truth:** 4 controllers thực tế trong `kiteclass-core` (verified 2026-05-26 empirical state-check):
> - `AttendanceController` (`/api/v1/attendance`) — single + session-bulk + stats CRUD (9 endpoints, legacy + Wave 105 authz)
> - `AttendanceClassBatchController` (`/api/v1/attendance/class`) — GAP-268a class-overview save (1 endpoint, Wave 51 Bucket B)
> - `AttendancePeriodController` (`/api/v1/attendance/periods`) — K-12 per-period (7 endpoints, GAP-323 Phase 1A + GAP-323b Phase 1B)
> - `ParentAttendanceFacetController` (`/api/v1/parent`) — parent-facet read (1 endpoint, GAP-321b Phase 1B)
>
> **Total:** 18 endpoints (gap report claim "9 endpoints" outdated — drift bao gồm Wave 51 + Wave 18b1/18b2 + Wave 18b2 Bucket C parent-facet ship sau audit 2026-04-26).

### Endpoint inventory (per `check-cross-layer-contract-drift.sh` format)

Plain inventory (line-per-endpoint) để CI drift script grep được:

```
POST /api/v1/attendance                                                            (§2.1 single)
POST /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance          (§2.2 bulk session)
GET /api/v1/attendance/{id}                                                        (§2.3 single read)
GET /api/v1/attendance/enrollment/{enrollmentId}                                   (§2.4 enrollment history)
GET /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance           (§2.5 session roster)
GET /api/v1/attendance/stats/student/{studentId}                                   (§2.6 student stats)
GET /api/v1/attendance/stats/class/{classId}                                       (§2.7 class stats)
PATCH /api/v1/attendance/{id}                                                      (§2.8 update)
DELETE /api/v1/attendance/{id}                                                     (§2.9 soft delete)
POST /api/v1/attendance/class/{classId}/batch                                      (§3.1 class-overview save)
GET /api/v1/attendance/periods/{id}                                                (§4.1 period single)
GET /api/v1/attendance/periods/students/{studentId}                                (§4.2 student period range)
GET /api/v1/attendance/periods/classes/{classId}                                   (§4.3 daily class roster)
POST /api/v1/attendance/periods                                                    (§4.4 period batch upsert)
PATCH /api/v1/attendance/periods/{id}                                              (§4.5 period update)
GET /api/v1/attendance/periods/daily-rollup                                        (§4.6 daily rollup)
GET /api/v1/attendance/periods/subject-sections/{subjectSectionId}                 (§4.7 subject section history)
GET /api/v1/parent/children/{childId}/attendance                                   (§5.1 parent-facet read)
```

---

## 1. Shared conventions

### 1.1 Auth headers (mọi endpoint trừ public)

| Header | Mandatory? | Mô tả |
|---|---|---|
| `Authorization: Bearer <JWT>` | Yes (mọi endpoint) | JWT chứa `userId`, `role`, `tenantSlug` claims (per Gateway) |
| `X-Tenant-Id: <tenantSlug>` | Yes | Multi-tenant isolation per BR-ATT-009 (V60 RLS migration enforces) |
| `X-Teacher-Id: <userId>` | Conditional | Required cho mọi write/upsert path (bulk mark, batch, period upsert, period PATCH) |
| `X-User-Reference-Id: <userId>` | Conditional | Required cho parent-facet endpoints (Gateway-injected từ `users.reference_id` khi `userType=PARENT`) |

### 1.2 Common error envelope

Mọi error response dùng RFC 7807 Problem Details:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Notes must not exceed 500 characters",
  "code": "VALIDATION_ERROR",
  "errors": [
    {"field": "notes", "message": "Notes must not exceed 500 characters"}
  ]
}
```

### 1.3 Common error codes

| HTTP | Code | Khi nào |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Body/query validation fail (size, range, required) |
| `400` | `BAD_REQUEST` | Logical bad input (vd childId/from/to missing or inverted) |
| `401` | `AUTH_REQUIRED` | Missing JWT hoặc header bắt buộc |
| `401` | `UNAUTHENTICATED` | JWT expired/invalid |
| `403` | `FORBIDDEN_ROLE` | Role không khớp permission matrix per `rules.md` |
| `403` | `PARENT_FACET_FORBIDDEN` | Parent không link với child (BR-PARENT-FACET-ATT-001) |
| `404` | `NOT_FOUND` | Resource missing (attendance / enrollment / session / student / class) |
| `409` | `ALREADY_MARKED` | Duplicate (student, session) per BR-ATT-001 |
| `409` | `OPTIMISTIC_LOCK_CONFLICT` | Concurrent `@Version` bump (V50 unique index backstop) |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | Body không phải application/json |
| `422` | `OUT_OF_WINDOW` | Roll-call ngoài grace/late threshold (BR-ATT-003/004) — reserved (planned) |
| `500` | `INTERNAL` | Bug — capture in CloudWatch + Sentry |

### 1.4 Pagination defaults

| Param | Default | Max | Sort default |
|---|---|---|---|
| `page` | `0` | — | — |
| `size` | `20` (legacy attendance) hoặc `50` (period + parent-facet) | `100` | varies — xem từng endpoint |

### 1.5 Enum `AttendanceStatus`

Source: `com.kiteclass.core.common.constant.AttendanceStatus`.

| Value | Display VI | Short | Color |
|---|---|---|---|
| `PRESENT` | Có mặt | P | green |
| `ABSENT` | Vắng | V | red |
| `LATE` | Đi trễ | T | yellow |
| `EXCUSED` | Có phép | CP | blue |
| `MAKEUP` | Học bù | HB | purple |

> **Note (drift cần fix riêng):** `use-cases.md` và `rules.md` BR-ATT-005 đề cập `EXCUSED_ABSENCE` — enum thực tế là `EXCUSED`. Out-of-scope GAP-232 (chỉ touch api-contract); follow-up gap nên rename trong use-cases.md + rules.md để khớp source-of-truth.

---

## 2. Endpoints — `AttendanceController` (`/api/v1/attendance`)

### 2.1 POST `/api/v1/attendance` — Mark single attendance

**Use case:** UC-ATT-01 (Mark Single Attendance)
**Auth:** Teacher (MAIN_TEACHER/ASSISTANT) hoặc Admin (per BR-ATT-006, BR-ATT-007) — KHÔNG có `@PreAuthorize` annotation hiện tại (defense-in-depth gap, follow-up).

**Request body (`CreateAttendanceRequest`):**

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `enrollmentId` | Long | ✅ | `@Positive` | Enrollment phải active |
| `sessionId` | Long | ✅ | `@Positive` | Class session phải exist |
| `status` | enum `AttendanceStatus` | ✅ | non-null | PRESENT/ABSENT/LATE/EXCUSED/MAKEUP |
| `notes` | String | optional | max 500 chars | Bắt buộc khi `status=EXCUSED` per BR-ATT-005 (service enforce) |
| `markedBy` | Long | optional | — | Teacher ID; service can override từ header |

**Response 201 Created:** `AttendanceResponse` (§7.1)

**Errors:**
- `400 VALIDATION_ERROR` — body invalid (enrollmentId/sessionId/status missing, notes > 500 chars)
- `400 EXCUSED_REQUIRES_NOTE` — `status=EXCUSED` + `notes` empty (BR-ATT-005)
- `404 NOT_FOUND` — enrollment/session không tồn tại
- `409 ALREADY_MARKED` — đã có record cho `(enrollmentId, sessionId)` per BR-ATT-001

---

### 2.2 POST `/api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance` — Bulk mark session

**Use case:** UC-ATT-02 (Mark Bulk Attendance)
**Auth:** `@PreAuthorize("@authz.hasAccessToClass(#classId)")` — Wave 105 Bucket E0 per-resource authz (OWASP A01). Teacher không assigned to classId → 403.

**Headers:** `X-Teacher-Id: <Long>` (required) — recording teacher.

**Path params:** `classId` (Long), `sessionId` (Long).

**Request body (`BulkAttendanceRequest`):**

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `sessionId` | Long | ✅ | `@Positive` | Phải match path `{sessionId}` (service verify) |
| `records[]` | List<AttendanceRecord> | ✅ | `@NotEmpty @Valid` | ≥1 record |
| `records[].enrollmentId` | Long | ✅ | `@Positive` | |
| `records[].status` | enum | ✅ | non-null | |
| `records[].notes` | String | optional | max 500 chars | |

**Response 201 Created:** `List<AttendanceResponse>` (entry order preserved)

**Idempotency:** application-layer dedupe; existing `(enrollmentId, sessionId)` → 409 trên record đó. Partial-fail behavior: service throws on first conflict (transaction rolls back toàn bộ batch). Future Phase 1.5: per-record idempotency.

**Errors:**
- `400 VALIDATION_ERROR` — records empty / size mismatch
- `403 FORBIDDEN_ROLE` — teacher không có access to classId
- `404 NOT_FOUND` — class/session không tồn tại
- `409 ALREADY_MARKED` — bất kỳ record nào đã có entry per BR-ATT-001 (toàn batch rollback)

---

### 2.3 GET `/api/v1/attendance/{id}` — Get single record

**Auth:** Teacher / Admin / Student (own record) per permission matrix.

**Response 200:** `AttendanceResponse` (§7.1)

**Errors:**
- `404 NOT_FOUND` — record không tồn tại

---

### 2.4 GET `/api/v1/attendance/enrollment/{enrollmentId}` — Attendance history per enrollment

**Use case:** UC-ATT-05 variant (student-side history)
**Auth:** Student (own enrollment) / Teacher / Admin.

**Pagination:** `Pageable` với default `sort=markedDate,DESC`, default size 20, max 100.

**Response 200:** `Page<AttendanceResponse>`

**Errors:**
- `404 NOT_FOUND` — enrollment không tồn tại

---

### 2.5 GET `/api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance` — Session roster

**Auth:** `@PreAuthorize("@authz.hasAccessToClass(#classId)")` — Wave 105 Bucket E0.

**Pagination:** default `sort=enrollmentId,ASC`, default size 20.

**Response 200:** `Page<AttendanceResponse>`

**Errors:**
- `403 FORBIDDEN_ROLE` — teacher không có access
- `404 NOT_FOUND` — class/session không tồn tại

---

### 2.6 GET `/api/v1/attendance/stats/student/{studentId}` — Student stats

**Use case:** UC-ATT-05 (View Student Attendance Stats)
**Auth:** Student (own) / Teacher / Admin.

**Response 200:** `AttendanceStatsResponse` (§7.2)

**Tenant isolation:** Filter by `instance_id` per BR-ATT-009.

**Errors:**
- `404 NOT_FOUND` — student không tồn tại

---

### 2.7 GET `/api/v1/attendance/stats/class/{classId}` — Class stats

**Use case:** UC-ATT-06 (View Class Attendance Stats)
**Auth:** Teacher (own classes) / Admin.

**Response 200:** `AttendanceStatsResponse` (§7.2, `targetType=CLASS`)

**Errors:**
- `404 NOT_FOUND` — class không tồn tại

---

### 2.8 PATCH `/api/v1/attendance/{id}` — Update status

**Use case:** UC-ATT-04 (Update Attendance Status — correct mistake / override)
**Auth:** MAIN_TEACHER hoặc Admin per BR-ATT-006 (ASSISTANT → 403).
**Headers:** `X-Teacher-Id: <Long>` (required).

**Request body (`UpdateAttendanceStatusRequest`):**

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `status` | enum | ✅ | non-null | New status |
| `notes` | String | optional | max 500 chars | Override reason (recommended when overriding QR auto-status) |

**Response 200:** `AttendanceResponse` với `pointsAwarded` recalculated.

**Audit-log:** service recalculates `attendance_rate` per BR-ATT-008. Gamification points re-applied via outbox event. (Note: dedicated `attendance_audit_log` table — Phase 1.5+ scope.)

**Errors:**
- `400 VALIDATION_ERROR` — status null hoặc notes > 500
- `403 FORBIDDEN_ROLE` — ASSISTANT cố override
- `404 NOT_FOUND` — record không tồn tại

---

### 2.9 DELETE `/api/v1/attendance/{id}` — Soft delete

**Use case:** UC-ATT-07 (Delete Attendance Record)
**Auth:** Admin only per BR-ATT-007 (service-layer guard, không có `@PreAuthorize` hiện tại — defense-in-depth gap).

**Response 204 No Content.**

**Semantics:** Soft delete — set `deletedAt`. Stats recalculated per BR-ATT-008.

**Errors:**
- `403 FORBIDDEN_ROLE` — non-admin
- `404 NOT_FOUND` — record không tồn tại

---

## 3. Endpoints — `AttendanceClassBatchController` (`/api/v1/attendance/class`)

### 3.1 POST `/api/v1/attendance/class/{classId}/batch?date=YYYY-MM-DD` — Class-overview batch save (GAP-268a)

**Use case:** UC-ATT-09 (Class-overview batch save — Wave 51 Bucket B)
**Auth:** `@PreAuthorize("@authz.hasAccessToClass(#classId)")` — Wave 105 Bucket C per-class authz (OWASP A01). Teacher không assigned → 403.
**Headers:** `X-Teacher-Id: <Long>` (required) — recording GVCN.

**Path:** `classId` (Long).
**Query:** `date` (LocalDate ISO-8601) — lesson date.

**Request body (`ClassBatchAttendanceRequest`):**

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `entries[]` | List<ClassBatchAttendanceEntry> | ✅ | `@NotEmpty @Size(max=200) @Valid` | Per BR-ATT-CLASS-BATCH-001 cap 200 cells = 10 tiết × 20 students |
| `entries[].studentId` | Long | ✅ | non-null | |
| `entries[].subjectSectionId` | Long | ✅ | non-null | SubjectSection từ GAP-054 |
| `entries[].periodNo` | Integer | ✅ | `@Min(1) @Max(10)` | Tiết 1..10 per TT 22/2021/TT-BGDĐT |
| `entries[].status` | enum | ✅ | non-null | |
| `entries[].notes` | String | optional | max 500 chars | |

**Response 201 Created:** `List<AttendancePeriodResponse>` (entry order preserved) (§7.3)

**Idempotency (per BR-ATT-CLASS-BATCH-002):** DB unique index `(student_id, subject_section_id, date, period_no, instance_id)` (V50 migration) backstops resubmits. Same body → cùng final state, no duplicate rows. Partial-fail: row-level upsert; service wraps trong `@Transactional` — single optimistic-lock conflict rolls back batch.

**Errors:**
- `400 VALIDATION_ERROR` — entries empty / batch > 200 / periodNo out of 1..10 / missing X-Teacher-Id header
- `403 FORBIDDEN_ROLE` — teacher không có access to classId (OWASP A01 guard)
- `409 OPTIMISTIC_LOCK_CONFLICT` — concurrent `@Version` bump beats request

**Outbox:** Phase 1 v1 — no outbox event emitted (consistent với existing `AttendancePeriodService.upsertBatch` path). Tracked cross-cutting refactor.

---

## 4. Endpoints — `AttendancePeriodController` (`/api/v1/attendance/periods`)

K-12 per-period (per-tiết) attendance. GAP-323 Phase 1A (Wave 18b1 read-only) + GAP-323b Phase 1B (Wave 18b2 — batch upsert + PATCH + roll-up).

### 4.1 GET `/api/v1/attendance/periods/{id}` — Get single period record

**Auth:** Teacher / Admin.

**Response 200:** `AttendancePeriodResponse` (§7.3)

**Errors:**
- `404 NOT_FOUND` — row không tồn tại

---

### 4.2 GET `/api/v1/attendance/periods/students/{studentId}?from=YYYY-MM-DD&to=YYYY-MM-DD` — Student period range

**Use case:** Parent portal feeds, student history.
**Auth:** Student (own) / Parent (linked via separate `/api/v1/parent/...` endpoint) / Teacher / Admin.

**Pagination:** default `size=50`, `sort=[date,periodNo],DESC`.

**Query:** `from` + `to` (LocalDate ISO-8601, inclusive).

**Response 200:** `Page<AttendancePeriodResponse>`

**Errors:**
- `400 BAD_REQUEST` — from/to missing or inverted
- `404 NOT_FOUND` — student không tồn tại

---

### 4.3 GET `/api/v1/attendance/periods/classes/{classId}?date=YYYY-MM-DD` — Daily class roster

**Auth:** `@PreAuthorize("@authz.hasAccessToClass(#classId)")` — Wave 105 Bucket C.

**Query:** `date` (LocalDate ISO-8601).

**Response 200:** `List<AttendancePeriodResponse>` (all periods + students cho `classId` trên ngày `date`).

**Errors:**
- `403 FORBIDDEN_ROLE` — teacher không có access
- `404 NOT_FOUND` — class không tồn tại

---

### 4.4 POST `/api/v1/attendance/periods` — Idempotent batch upsert (per-tiết)

**Use case:** GVCN/bộ môn mark từng tiết riêng lẻ.
**Auth:** Teacher (any class teacher của subject_section).
**Headers:** `X-Teacher-Id: <Long>` (required) — recording teacher.

**Request body (`AttendancePeriodBatchCreateRequest`):**

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `entries[]` | List<AttendancePeriodCreateRequest> | ✅ | `@NotEmpty @Size(max=60) @Valid` | Cap 60 = 1 full class (~42 students) + headroom |
| `entries[].studentId` | Long | ✅ | non-null | |
| `entries[].classId` | Long | ✅ | non-null | |
| `entries[].subjectSectionId` | Long | ✅ | non-null | |
| `entries[].periodNo` | Integer | ✅ | `@Min(1) @Max(10)` | Tiết 1..10 |
| `entries[].date` | LocalDate | ✅ | non-null | ISO-8601 |
| `entries[].status` | enum | ✅ | non-null | |
| `entries[].notes` | String | optional | max 500 chars | |

**Response 201 Created:** `List<AttendancePeriodResponse>`

**Idempotency:** DB unique index `(student_id, subject_section_id, date, period_no, instance_id)` (V50 migration) — resubmit cùng body → cùng state, no duplicate.

**Errors:**
- `400 VALIDATION_ERROR` — entries empty / size > 60 / periodNo out of range
- `409 OPTIMISTIC_LOCK_CONFLICT` — concurrent edit beats request

---

### 4.5 PATCH `/api/v1/attendance/periods/{id}` — Update single period row

**Use case:** GVCN correct mistake (per-tiết).
**Auth:** Teacher of class (service-layer guard).
**Headers:** `X-Teacher-Id: <Long>` (required).

**Request body (`AttendancePeriodUpdateRequest`):**

| Field | Type | Required | Validation | Description |
|---|---|---|---|---|
| `status` | enum | ✅ | non-null | New status |
| `notes` | String | optional | max 500 chars | Override note |
| `version` | Long | ✅ | non-null | JPA `@Version` value client đã đọc; stale → 409 OPTIMISTIC_LOCK_CONFLICT |

**Response 200:** `AttendancePeriodResponse`

**Errors:**
- `400 VALIDATION_ERROR` — status/version null
- `404 NOT_FOUND` — row không tồn tại
- `409 OPTIMISTIC_LOCK_CONFLICT` — stale `version` (first save wins, second 409s with current state)

---

### 4.6 GET `/api/v1/attendance/periods/daily-rollup?classId=X&from=YYYY-MM-DD&to=YYYY-MM-DD` — Daily roll-up

**Auth:** `@PreAuthorize("@authz.hasAccessToClass(#classId)")` — Wave 105 Bucket C.

**Query (all required):**
- `classId` (Long)
- `from` (LocalDate ISO-8601, inclusive)
- `to` (LocalDate ISO-8601, inclusive)

**Response 200:** `List<DailyAttendanceRollupResponse>` (§7.4)

**Semantics:** Per-(student, date) counts + boolean `allDayAbsent` (= `absentCount + lateCount ≥ 7` per TT 22/2021/TT-BGDĐT "vắng cả ngày" threshold). LATE intentionally lumped với ABSENT — regulation treats both as missed instructional time.

**Phase 1B v1:** On-demand aggregation; materialized-view path deferred (GAP-323b §1B.4).

**Errors:**
- `400 BAD_REQUEST` — params missing or from > to
- `403 FORBIDDEN_ROLE` — teacher không có access
- `404 NOT_FOUND` — class không tồn tại

---

### 4.7 GET `/api/v1/attendance/periods/subject-sections/{subjectSectionId}?from=YYYY-MM-DD&to=YYYY-MM-DD` — Subject-section history

**Use case:** Bộ môn (subject teacher) review surface — references SubjectSection từ GAP-054 Phase 1.
**Auth:** Teacher của subject_section / Admin.

**Pagination:** default `size=50`, `sort=[date,periodNo],DESC`.

**Response 200:** `Page<AttendancePeriodResponse>`

**Errors:**
- `400 BAD_REQUEST` — from/to missing or inverted
- `404 NOT_FOUND` — subject_section không tồn tại

---

## 5. Endpoints — `ParentAttendanceFacetController` (`/api/v1/parent`)

### 5.1 GET `/api/v1/parent/children/{childId}/attendance?from=YYYY-MM-DD&to=YYYY-MM-DD` — Parent reads child period attendance

**Use case:** GAP-321b Phase 1B foundation — Wave 18b2 Bucket C. Parent portal reads.
**Auth:** `@PreAuthorize("@authz.hasAccessToChild(#childId)")` — Wave 105 Bucket E0 OWASP A01 defense-in-depth.
**Headers:** `X-User-Reference-Id: <Long>` (required) — Gateway-injected từ `users.reference_id` khi `userType=PARENT`.

**Path:** `childId` (Long).
**Query:** `from` + `to` (LocalDate ISO-8601, inclusive).
**Pagination:** default `size=50`, `sort=[date,periodNo],DESC`.

**Response 200 (`ApiResponse<Page<AttendancePeriodResponse>>`):**

```json
{
  "success": true,
  "data": {
    "content": [/* AttendancePeriodResponse[] */],
    "page": 0,
    "size": 50,
    "totalElements": 123,
    "totalPages": 3
  }
}
```

**Scope guard:** `ParentAttendanceFacetService` enforces `ParentStudentLink` edge — parent phải có active link với child (BR-PARENT-FACET-ATT-001).

**Errors:**
- `400 BAD_REQUEST` — childId/from/to missing or inverted
- `401 AUTH_REQUIRED` — `X-User-Reference-Id` header missing
- `403 PARENT_FACET_FORBIDDEN` — parent không link với child (BR-PARENT-FACET-ATT-001)

**Different shape vs main controller:** wrapper `ApiResponse<>` envelope (parent module convention) — main `AttendanceController` returns raw `Page<>` body.

---

## 6. Endpoint × use-case × business-rule traceability matrix

| Endpoint | Use case | Business rules |
|---|---|---|
| §2.1 POST /attendance | UC-ATT-01 | BR-ATT-001, BR-ATT-005, BR-ATT-006, BR-ATT-007, BR-ATT-008 |
| §2.2 POST /classes/{}/sessions/{}/attendance | UC-ATT-02 | BR-ATT-001, BR-ATT-005, BR-ATT-006, BR-ATT-008, BR-ATT-009 |
| §2.3 GET /attendance/{id} | (read) | BR-ATT-009 (tenant filter) |
| §2.4 GET /attendance/enrollment/{} | UC-ATT-05 variant | BR-ATT-009 |
| §2.5 GET /classes/{}/sessions/{}/attendance | (roster read) | BR-ATT-009 |
| §2.6 GET /stats/student/{} | UC-ATT-05 | BR-ATT-008, BR-ATT-009 |
| §2.7 GET /stats/class/{} | UC-ATT-06 | BR-ATT-008, BR-ATT-009 |
| §2.8 PATCH /attendance/{id} | UC-ATT-04 | BR-ATT-006, BR-ATT-008 |
| §2.9 DELETE /attendance/{id} | UC-ATT-07 | BR-ATT-007, BR-ATT-008, BR-ATT-009 |
| §3.1 POST /class/{}/batch | UC-ATT-09 | BR-ATT-001, BR-ATT-005, BR-ATT-CLASS-BATCH-001, BR-ATT-CLASS-BATCH-002, BR-ATT-009 |
| §4.1-4.3, §4.6-4.7 (period reads) | (planned UC-ATT-PERIOD-*) | BR-ATT-009 |
| §4.4 POST /periods | (planned UC-ATT-PERIOD-UPSERT) | BR-ATT-CLASS-BATCH-002 (same idempotency principle), BR-ATT-009 |
| §4.5 PATCH /periods/{id} | (planned UC-ATT-PERIOD-UPDATE) | BR-ATT-006, BR-ATT-009 |
| §4.6 GET /periods/daily-rollup | (planned UC-ATT-PERIOD-ROLLUP) | BR-ATT-009, TT 22/2021/TT-BGDĐT (vắng cả ngày ≥7 tiết) |
| §5.1 GET /parent/children/{}/attendance | (planned UC-ATT-PARENT-FACET) | BR-PARENT-FACET-ATT-001, BR-ATT-009 |

> **Drift (out-of-scope GAP-232):** use-cases.md hiện chỉ có UC-ATT-01..UC-ATT-09. Endpoints §4 (period) và §5 (parent-facet) thiếu UC entries — follow-up gap nên thêm UC-ATT-PERIOD-{UPSERT,UPDATE,ROLLUP} + UC-ATT-PARENT-FACET.

---

## 7. DTOs

### 7.1 `AttendanceResponse`

| Field | Type | Description |
|---|---|---|
| `id` | Long | Attendance record ID |
| `enrollmentId` | Long | Enrollment reference |
| `studentName` | String | Denormalized từ enrollment |
| `sessionId` | Long | Class session reference |
| `sessionNumber` | Integer | Session sequence number |
| `status` | `AttendanceStatus` | PRESENT/ABSENT/LATE/EXCUSED/MAKEUP (§1.5) |
| `markedDate` | LocalDateTime | When attendance was marked |
| `markedBy` | Long | User ID who marked |
| `markedByName` | String | Denormalized teacher name |
| `notes` | String | Optional notes (max 500 chars) |
| `pointsAwarded` | Integer | Gamification points (PRESENT +N, ABSENT -N — config) |
| `createdAt` | Instant | Record creation time |
| `updatedAt` | Instant | Last update time |

### 7.2 `AttendanceStatsResponse`

| Field | Type | Description |
|---|---|---|
| `targetId` | Long | Student ID hoặc Class ID |
| `targetType` | String | `STUDENT` hoặc `CLASS` |
| `totalSessions` | Long | Total sessions counted |
| `presentCount` | Long | Number of PRESENT records |
| `absentCount` | Long | Number of ABSENT records |
| `lateCount` | Long | Number of LATE records |
| `excusedCount` | Long | Number of EXCUSED records |
| `makeupCount` | Long | Number of MAKEUP records |
| `attendanceRate` | Double | `(presentCount + lateCount) / totalSessions * 100` per BR-ATT-008; null nếu totalSessions=0 |

### 7.3 `AttendancePeriodResponse`

| Field | Type | Description |
|---|---|---|
| `id` | Long | Row ID |
| `studentId` | Long | |
| `classId` | Long | |
| `subjectSectionId` | Long | SubjectSection từ GAP-054 |
| `periodNo` | Integer | Tiết 1..10 per TT 22/2021/TT-BGDĐT |
| `date` | LocalDate | Lesson date |
| `status` | `AttendanceStatus` | (§1.5) |
| `recordedBy` | Long | Teacher / GVCN user ID |
| `recordedAt` | LocalDateTime | When attendance was recorded |
| `notes` | String | Optional (max 500 chars) |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

> Note: `version` field (JPA `@Version`) used internally cho optimistic-lock không exposed in response — client truyền lại trong PATCH body. Future: consider adding `version` to response for UI to track optimistically.

### 7.4 `DailyAttendanceRollupResponse`

| Field | Type | Description |
|---|---|---|
| `studentId` | Long | |
| `classId` | Long | |
| `date` | LocalDate | |
| `periodCount` | long | Total periods that day |
| `presentCount` | long | |
| `absentCount` | long | |
| `lateCount` | long | |
| `excusedCount` | long | |
| `makeupCount` | long | |
| `allDayAbsent` | boolean | True khi `absentCount + lateCount ≥ 7` (TT 22/2021/TT-BGDĐT "vắng cả ngày") |

---

## 8. Cross-references

- **Business rules:** `documents/01-business/kiteclass/attendance/rules.md` (BR-ATT-001..009 + BR-ATT-CLASS-BATCH-001/002)
- **Use cases:** `documents/01-business/kiteclass/attendance/use-cases.md` (UC-ATT-01..09)
- **Source controllers (verified 2026-05-26):**
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/controller/AttendanceController.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/controller/AttendanceClassBatchController.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/controller/AttendancePeriodController.java`
  - `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/controller/ParentAttendanceFacetController.java`
- **Source DTOs:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/attendance/dto/`
- **Integration tests:**
  - `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/attendance/AttendanceIntegrationTest.java` (legacy attendance endpoints §2)
  - `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/attendance/AttendancePeriodIntegrationTest.java` (period endpoints §4)
  - `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/attendance/controller/AttendanceClassBatchControllerIT.java` (§3 batch)
  - `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/module/parent/controller/ParentAttendanceFacetControllerIT.java` (§5 parent-facet)
- **Database migrations:** V50 (unique index `(student_id, subject_section_id, date, period_no, instance_id)`), V51 (periodNo CHECK 1..10), V60 (RLS tenant isolation)
- **TT 22/2021/TT-BGDĐT:** MoET regulation defining 1-10 tiết per day + "vắng cả ngày" threshold

---

## 9. Log

- **2026-05-26 (v2.0):** Full rewrite per GAP-232 (Wave beta-readiness-6 Bucket B) — sync với 4 controllers thực tế. Phát hiện drift breadth gấp 2× gap claim: 18 endpoints actual vs 9 documented (gap report) / 10 documented (file pre-rewrite). Added missing endpoints: §3 batch class-overview (Wave 51 GAP-268a), §4 period 7 endpoints (GAP-323 + GAP-323b), §5 parent-facet (GAP-321b Phase 1B). Added §1 shared conventions (auth headers, RFC 7807 error envelope, common error codes, pagination defaults, enum reference). Added §6 traceability matrix endpoint × UC × BR. Updated §7 DTO tables với complete field schemas. Surfaced 2 drifts out-of-scope: (a) enum `EXCUSED` vs use-cases.md `EXCUSED_ABSENCE` mismatch; (b) use-cases.md thiếu UC entries cho period + parent-facet endpoints — both flagged for follow-up gap. Reviewer: agent Bucket B (verified empirically 2026-05-26 4 controllers + 12 DTOs).
- **2026-05-10** (v1.0 - GAP-268a Wave 51 Bucket B): Added §3.1 `POST /api/v1/attendance/class/{classId}/batch` endpoint (legacy file structure).
- **2026-03-24:** Initial extracted from `AttendanceController`, `AttendanceResponse`, `AttendanceStatsResponse` (§2 9 endpoints).
