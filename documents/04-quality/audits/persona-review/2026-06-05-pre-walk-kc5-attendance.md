---
title: "Pre-Walk Persona Simulation — KC-5 Attendance"
audience: dev
created: 2026-06-05
scope: "Flow Verification Campaign KC-5 — Mark attendance (POST /api/v1/attendance + bulk per-session) + period attendance/daily rollup (K12) + attendance stats trong kiteclass-core. Pre-walk static persona simulation per pre-walk-persona-simulation-mandate.md §3 — surface failure modes TRƯỚC khi coordinator walk local Docker stack."
---

# Pre-Walk Persona Simulation — KC-5 Attendance

**Mục tiêu:** Mô phỏng tâm lý 3 persona (Teacher điểm danh đơn + bulk grid, Admin xem stats, GVCN K12 period+rollup) → liệt kê failure mode LIKELY trước khi walk, để batch-fix các finding HIGH trước.

**Phạm vi đã state-check (endpoints EXIST):**
- `AttendanceController` @ `/api/v1/attendance` — POST mark single + POST `/classes/{classId}/sessions/{sessionId}/attendance` bulk + GET `/{id}` + GET `/enrollment/{enrollmentId}` + GET `/classes/{classId}/sessions/{sessionId}/attendance` + GET `/stats/student/{studentId}` + GET `/stats/class/{classId}` + PATCH `/{id}` + DELETE `/{id}`.
- `AttendanceClassBatchController` @ `/api/v1/attendance/class` — POST `/{classId}/batch`.
- `AttendancePeriodController` @ `/api/v1/attendance/periods` — GET/POST/PATCH + `/daily-rollup` (K12).

**Context quan trọng — GAP-983 (Wave security-1, vừa fix):** cross-tenant by-id read leak đã vá bằng Hibernate `tenantFilter` (declared trên `BaseEntity` `@Filter`, `Attendance extends BaseEntity` nên honor) enabled trên transaction-bound session qua `TenantAwareDataSourceInterceptor.enableTenantFilter`, cộng Postgres RLS qua GUC `app.current_tenant_id`. Hai lớp CHỈ active khi `TenantContext.isSet()` + transaction active. Đây là trục cần verify kỹ nhất cho attendance — mọi attendance read by-id/session/enrollment dùng `findByIdAndDeletedFalse`/`findBySessionId...` KHÔNG có `instanceId` trong query (giống GAP-746 fragile path) → phụ thuộc 100% vào tenantFilter/RLS.

---

## Phát hiện chính trước khi đọc chi tiết

- **CONTRACT SURPRISE LỚN NHẤT: API điểm danh dùng `enrollmentId` + `sessionId`, KHÔNG phải `studentId` + `classId`.** Persona nghĩ "điểm danh học sinh trong lớp" nhưng body POST `/api/v1/attendance` cần `{enrollmentId, sessionId, status}`. Walker phải resolve enrollment_id (qua GET `/api/v1/enrollments` hoặc psql) TRƯỚC. Single-mark KHÔNG nhận classId.
- **CONTRACT SURPRISE #2: enum status là `EXCUSED` / `MAKEUP`, KHÔNG phải `EXCUSED_ABSENCE`.** rules.md + use-cases.md + BR-ATT-005 ghi `EXCUSED_ABSENCE` nhưng code enum (`AttendanceStatus`) chỉ có `PRESENT/ABSENT/LATE/EXCUSED/MAKEUP`. Gửi `"EXCUSED_ABSENCE"` → HMNR → **HTTP 400 MALFORMED_REQUEST_BODY**. Docs ↔ code drift.
- **CONTRACT SURPRISE #3: single-mark POST `/api/v1/attendance` KHÔNG có permission check.** Chỉ bulk + period dùng `@authz.hasAccessToClass`. Single-mark KHÔNG có `@PreAuthorize` + KHÔNG check teacher role → bất kỳ authenticated tenant user nào điểm danh được cho enrollment bất kỳ.
- **Auth qua `X-Teacher-Id` HEADER (bulk/period/batch), KHÔNG phải JWT.** Header thiếu → 400 (`@RequestHeader` required). Single-mark KHÔNG dùng header này → markedBy = null.
- **BR-ATT-005 (EXCUSED requires notes) + BR-ATTEND-002 (session not COMPLETED/CANCELLED) đều KHÔNG implement.**

---

## Các failure mode (12) — theo confidence

### 1. [HIGH] Status `EXCUSED_ABSENCE` (theo docs/UI) → 400, code chỉ chấp nhận `EXCUSED`
- **(a) Where:** `AttendanceStatus.java:22-26` enum chỉ có `PRESENT/ABSENT/LATE/EXCUSED/MAKEUP`; `CreateAttendanceRequest.status` line 45 deserialize bằng Jackson. rules.md:23 + use-cases.md:18,30 ghi `EXCUSED_ABSENCE`.
- **(b) Symptom:** Persona/UI gửi `{"status":"EXCUSED_ABSENCE"}` (theo docs) → `HttpMessageNotReadableException` (invalid enum value) → handler line 247-249 → **HTTP 400 `MALFORMED_REQUEST_BODY`** "malformed JSON or invalid enum value". Persona bối rối "tôi chọn Có phép mà lỗi?". Nếu FE hardcode đúng `EXCUSED` thì OK — nhưng docs misled.
- **(c) Pre-walk check:** `grep -n "EXCUSED" AttendanceStatus.java` → confirm chỉ `EXCUSED` (không `_ABSENCE`). Walk: POST với `"status":"EXCUSED"` (đúng enum) → 201; thử `"EXCUSED_ABSENCE"` → kỳ vọng 400. **Walker PHẢI dùng `EXCUSED` không phải `EXCUSED_ABSENCE`.**

### 2. [HIGH] Single-mark POST `/api/v1/attendance` KHÔNG có permission guard — bất kỳ tenant user điểm danh enrollment bất kỳ
- **(a) Where:** `AttendanceController.markAttendance` line 63-73 — KHÔNG có `@PreAuthorize`; `AttendanceServiceImpl.markAttendance` line 58-108 — chỉ check enrollment active + duplicate, KHÔNG check caller là teacher của lớp / role. Tương phản: bulk path line 85 + service line 121-130 check `MAIN_TEACHER`.
- **(b) Symptom:** Authenticated user (vai trò bất kỳ trong tenant, kể cả không phải teacher của lớp đó) POST `/api/v1/attendance` với enrollmentId hợp lệ → 201, điểm danh thành công + award/deduct points. Vi phạm BR-ATT-006/007 permission matrix (rules.md:18-19,29). OWASP A01 broken access control trên single-mark write path. (Cross-flow sweep: GAP-729 Wave 105 đã guard bulk/stats/period nhưng MISS single-mark.)
- **(c) Pre-walk check:** `grep -n "PreAuthorize\|MAIN_TEACHER\|hasAccessToClass" AttendanceController.java AttendanceServiceImpl.java` → confirm single-mark markAttendance (line 60) có 0 guard. Walk sad-path: login user KHÔNG phải teacher lớp → POST single mark → kỳ vọng 403 (đúng) nhưng thực tế 201 (bug).

### 3. [HIGH] Cross-tenant attendance read/mark phụ thuộc HOÀN TOÀN tenantFilter + RLS (GAP-983 re-walk)
- **(a) Where:** Mọi attendance lookup KHÔNG dùng `instanceId` trong query: `getAttendanceById` line 209 `findByIdAndDeletedFalse(id)`; `getAttendanceByEnrollment` line 228 `findByEnrollmentIdAndDeletedFalse`; `getAttendanceBySession` line 240 `findBySessionIdAndDeletedFalse`; single-mark line 65 `enrollmentRepository.findByIdAndDeletedFalse` (GAP-746 fragile path mà repo javadoc cảnh báo). Chỉ dựa `@Filter tenantFilter` (BaseEntity) + RLS GUC.
- **(b) Symptom:** Nếu `TenantContext` chưa set (gateway không truyền tenant cho attendance route, hoặc filter chạy ngoài transaction), tenantFilter KHÔNG enable → attendance/enrollment của tenant KHÁC bị load → cross-tenant mark/read (201/200 thay vì 404). Đây CHÍNH là class lỗi GAP-983 vừa fix — cần re-walk verify fix giữ vững cho attendance path (per `pre-handoff-self-test-completeness.md` §3 post-fix re-walk).
- **(c) Pre-walk check:** `grep -n "findByIdAndInstanceId\|findByIdAndDeletedFalse\|findBySessionIdAndDeletedFalse" AttendanceServiceImpl.java AttendanceRepository.java` → confirm 0 instanceId param trên read path. Walk sad-path: login tenant A, GET `/api/v1/attendance/{id}` với id thuộc tenant B → kỳ vọng 404 (fix giữ) NOT 200; tương tự POST mark với enrollmentId tenant B → kỳ vọng 404.

### 4. [HIGH] BR-ATTEND-002 không enforce — điểm danh được vào session đã COMPLETED/CANCELLED
- **(a) Where:** `markAttendance` single line 58-108 KHÔNG load ClassSession để check status (chỉ check enrollment). Bulk line 133 load session NHƯNG chỉ check `getClassId().equals(classId)` (line 136), KHÔNG đọc `session.getStatus()`. Entity javadoc `Attendance.java:37` ghi BR-ATTEND-002 "Session must exist and not be COMPLETED/CANCELLED" + `ClassSession.status` (SessionStatus) tồn tại (line 105-107).
- **(b) Symptom:** Teacher điểm danh vào session id đã `COMPLETED`/`CANCELLED` → 201 (bug) thay vì 400. Business-invalid: ghi điểm danh cho buổi đã kết thúc/hủy. Single-mark còn KHÔNG validate session tồn tại → mark với sessionId không tồn tại vẫn 201 (FK-only, không 404 graceful).
- **(c) Pre-walk check:** `grep -n "getStatus\|SessionStatus\|COMPLETED\|CANCELLED\|findByIdAndDeletedFalse.*[Ss]ession" AttendanceServiceImpl.java` → 0 status check. Walk: psql UPDATE class_sessions SET status='CANCELLED' → POST mark → kỳ vọng 201 (bug). Single-mark với sessionId=99999 (không tồn tại) → kỳ vọng 201 (không 404 — bug).

### 5. [HIGH] BR-ATT-005 không enforce — EXCUSED không bắt buộc notes
- **(a) Where:** `markAttendance` + `markBulkAttendance` + `updateAttendanceStatus` KHÔNG check `status == EXCUSED && notes == null`. `CreateAttendanceRequest.notes` line 51 chỉ `@Size(max=500)`, không conditional-required. rules.md:17 BR-ATT-005 + use-cases.md:30 "EXCUSED_ABSENCE without notes → 400".
- **(b) Symptom:** POST mark với `{"status":"EXCUSED"}` không notes → 201 (bug) thay vì 400 "Excused absence requires a note". Business rule documented nhưng không implement.
- **(c) Pre-walk check:** `grep -n "EXCUSED.*notes\|notes.*EXCUSED\|requireNote\|ValidationException.*EXCUSED" AttendanceServiceImpl.java` → 0 hit. Walk: POST `{"status":"EXCUSED"}` không notes → kỳ vọng 400 (per docs) nhưng thực tế 201.

### 6. [MEDIUM] Bulk `findAllById(enrollmentIds)` — JpaRepository default, không filter `deleted`, dựa tenantFilter cho instance
- **(a) Where:** `markBulkAttendance` line 146 `enrollmentRepository.findAllById(enrollmentIds)` — Spring Data default method, query KHÔNG có `deleted=false` (khác các method khác). Line 156 sau đó check `!e.isDeleted()` in-memory; instance scope dựa tenantFilter.
- **(b) Symptom:** Nếu enrollment đã soft-delete, `findAllById` vẫn load (query không exclude deleted) → size match → nhưng line 155-156 `allInClass` check `!e.isDeleted()` → ValidationException `ENROLLMENT_NOT_IN_CLASS` (400). Confusing error code: học sinh đã rút khỏi lớp → báo "không thuộc lớp" thay vì "enrollment not found"/404. Edge: nếu tenantFilter không enable, cross-tenant enrollment count match → load enrollment tenant khác.
- **(c) Pre-walk check:** Confirmed `findAllById` (no deletedFalse). Walk: bulk với 1 enrollmentId đã soft-delete → kỳ vọng error nhưng quan sát code/message (400 ENROLLMENT_NOT_IN_CLASS — misleading).

### 7. [MEDIUM] Single-mark + N+1 markAttendance reuse trong bulk → points double-award + per-row exception abort
- **(a) Where:** `markBulkAttendance` line 167-184 loop gọi `markAttendance(singleRequest)` mỗi record (N+1: mỗi record = save + pointService.award + re-fetch line 181). Bất kỳ record nào throw (duplicate/inactive enrollment) → toàn bộ bulk rollback (1 `@Transactional` line 111) → UC-ATT-02 "Any duplicate in batch → 409" (đúng all-or-nothing) NHƯNG error message từ single-mark (`ATTENDANCE_ALREADY_MARKED` ValidationException → 400, KHÔNG 409 như use-cases ghi).
- **(b) Symptom:** Bulk có 1 record trùng (enrollment+session đã mark) → `markAttendance` throw `ValidationException("ATTENDANCE_ALREADY_MARKED")` → handler line 93-107 → **HTTP 400** (không phải 409 như UC-ATT-02 errors table). Cả batch rollback (đúng). Persona kỳ vọng 409 theo docs nhưng nhận 400. Points: mỗi record gọi `pointService.awardAttendancePoints` — nếu re-run sau rollback có thể inconsistent với gamification (cross-module).
- **(c) Pre-walk check:** Confirmed duplicate → ValidationException (400) not DuplicateResourceException (409). Walk: bulk 3 records, 1 trùng → kỳ vọng 409 (docs) nhưng thực tế 400 + cả batch fail.

### 8. [MEDIUM] markedBy = null trên single-mark path (mapper ignore + service không set)
- **(a) Where:** `AttendanceMapper` line 29 `@Mapping(target="markedBy", ignore=true)` "Set by service from security context"; `markAttendance` single line 58-108 KHÔNG `setMarkedBy`. `CreateAttendanceRequest.markedBy` line 58 "Optional, will be set from request header in controller" NHƯNG controller markAttendance (line 66) KHÔNG đọc header nào. Chỉ bulk (line 173) + update (line 289) set markedBy.
- **(b) Symptom:** Single-mark → attendance.markedBy = null trong DB. enrichResponse line 454 `if (markedBy != null)` → markedByName null. Audit gap: không biết ai điểm danh (single path). Không crash nhưng data-quality + accountability hole.
- **(c) Pre-walk check:** Confirmed single-mark không setMarkedBy. Walk: POST single mark → `psql SELECT marked_by FROM attendance WHERE id=...` → kỳ vọng NULL (bug).

### 9. [MEDIUM] Stats N+1 + per-enrollment count loop — slow + EXCUSED stats dùng wrong enum count vs rate formula
- **(a) Where:** `getStudentAttendanceStats` line 334-348 + `getClassAttendanceStats` line 384-398 — loop mỗi enrollment, gọi 6 count queries (`countByEnrollmentIdAndStatusAndDeletedFalse` × PRESENT/ABSENT/LATE/EXCUSED/MAKEUP + total). N enrollments × 6 = 6N queries (N+1). attendanceRate line 350 = `presentCount*100/totalSessions` — CHỈ PRESENT, nhưng BR-ATT-008 (rules.md:20) = `(PRESENT + LATE)/total`. LATE không tính vào rate.
- **(b) Symptom:** (1) Stats rate sai theo BR-ATT-008: học sinh LATE 100% → rate 0% (chỉ đếm PRESENT) thay vì 100% (PRESENT+LATE). (2) Class với nhiều enrollment → 6N queries → slow. totalSessions = 0 → rate = null (OK, không div-by-zero vì ternary line 350).
- **(c) Pre-walk check:** Confirmed rate = presentCount only. Walk: mark 1 student PRESENT + 1 LATE same enrollment, GET `/stats/student/{id}` → kỳ vọng rate=100% (BR-ATT-008) nhưng thực tế 50% (chỉ PRESENT). div-by-zero: student không record → rate null (OK).

### 10. [MEDIUM] `updateAttendanceStatus` PATCH yêu cầu MAIN_TEACHER nhưng KHÔNG cho ADMIN/ASSISTANT
- **(a) Where:** `updateAttendanceStatus` line 273-281 — chỉ accept `TeacherClassRole.MAIN_TEACHER`, throw `PermissionDeniedException` cho mọi role khác. use-cases.md UC-ATT-04 line 85 "MAIN_TEACHER / Admin"; BR-ATT-007 ADMIN full access. Permission qua `teacherClassRepository.findByTeacherIdAndClassId(teacherId, ...)` — ADMIN không có TeacherClass row → throw `TEACHER_NOT_IN_CLASS`.
- **(b) Symptom:** ADMIN cố override status qua PATCH `/{id}` với X-Teacher-Id = admin user → `findByTeacherIdAndClassId` empty → `PermissionDeniedException("TEACHER_NOT_IN_CLASS")` → 403. ADMIN bị chặn dù BR-ATT-007 cho full access. Persona Admin "tôi là admin sao không sửa được?".
- **(c) Pre-walk check:** Confirmed PATCH chỉ MAIN_TEACHER via TeacherClass lookup. Walk (Admin persona): PATCH với admin teacherId không có TeacherClass row → kỳ vọng 200 (BR-ATT-007) nhưng thực tế 403.

### 11. [LOW] Tên/notes tiếng Việt có dấu — UTF-8 round-trip notes field
- **(a) Where:** `Attendance.notes` line 113 `@Column(length=500)` ; `CreateAttendanceRequest.notes`. enrichResponse line 445 studentName = placeholder `"Student-"+studentId` (KHÔNG fetch real name — không phải encoding issue nhưng UI hiển thị placeholder thay tên thật).
- **(b) Symptom:** notes "Học sinh nghỉ có phép, ốm" → lưu/đọc đúng nếu DB charset UTF-8 (likely OK). NHƯNG studentName luôn là `"Student-{id}"` placeholder (line 445) → UI session roster hiển thị "Student-42" thay vì "Trần Thị Hồng". markedByName = `"Teacher-{id}"` placeholder line 455. sessionNumber = null line 450.
- **(c) Pre-walk check:** Confirmed enrichResponse trả placeholder names. Walk: GET attendance → response `studentName` = "Student-N" (placeholder bug, không phải real name); notes có dấu round-trip OK.

### 12. [LOW] Invalid status string / missing field → graceful 400 (path đúng — verify)
- **(a) Where:** `CreateAttendanceRequest` `@NotNull` enrollmentId/sessionId/status (line 28-45); invalid enum → HMNR handler line 247-249 (400 MALFORMED_REQUEST_BODY); missing field → MethodArgumentNotValid handler line 109-131 (400 VALIDATION_ERROR). Enrollment không tồn tại → `EntityNotFoundException("ENROLLMENT_NOT_FOUND")` handler line 61-75 → 404.
- **(b) Symptom:** Đây là path ĐÚNG — flag LOW để confirm: invalid status "FOO" → 400 graceful (không 500); missing enrollmentId → 400 field error; enrollmentId không tồn tại → 404 graceful. Enrollment INACTIVE → `ValidationException("ENROLLMENT_NOT_ACTIVE")` 400.
- **(c) Pre-walk check:** Đã xác nhận handlers tồn tại. Walk happy verify: POST status="FOO" → 400; missing sessionId → 400; enrollmentId=99999 → 404 ENROLLMENT_NOT_FOUND.

---

## Recommended pre-walk batch fix (theo confidence × impact)

### 🔴 HIGH — fix/clarify TRƯỚC khi walk

1. **Finding #1 (enum EXCUSED vs EXCUSED_ABSENCE drift).** Quyết định: hoặc (a) align docs (rules.md/use-cases.md) → `EXCUSED` (code là source of truth, ít rủi ro), hoặc (b) thêm `EXCUSED_ABSENCE` vào enum + migration. **Walker PHẢI dùng `EXCUSED` khi walk** — đây là contract surprise quan trọng nhất sau enrollmentId.
2. **Finding #2 (single-mark no authz).** Thêm `@PreAuthorize` / service-level role+class check cho `markAttendance` single path — đối xứng với bulk. OWASP A01 hole, cross-flow sweep miss của GAP-729.
3. **Finding #3 (cross-tenant re-walk GAP-983).** Bắt buộc walk sad-path login tenant A + GET/POST attendance của tenant B → verify 404. Post-fix re-walk per `pre-handoff §3`. Cân nhắc thêm `findByIdAndInstanceIdAndDeletedFalse` defensive (per GAP-746 hardening).
4. **Finding #4 (BR-ATTEND-002 session status guard) + #5 (BR-ATT-005 EXCUSED requires notes).** Cả 2 documented-not-implemented. Thêm guard `session.getStatus() ∉ {COMPLETED,CANCELLED}` + `status==EXCUSED → notes required`.

### 🟡 MEDIUM — spot-check trong walk (fix nếu confirm)

- **#6** bulk findAllById misleading error cho soft-deleted enrollment.
- **#7** bulk duplicate → 400 thay vì 409 (UC-ATT-02 docs mismatch).
- **#8** markedBy null single-mark (audit/accountability gap).
- **#9** attendanceRate sai BR-ATT-008 (chỉ PRESENT, thiếu LATE) + N+1 stats.
- **#10** ADMIN bị chặn PATCH override (BR-ATT-007 mismatch).

### 🟢 LOW — defer to walk observation

- **#11** studentName/markedByName placeholder (UI hiển thị "Student-N" thay tên thật).
- **#12** invalid-input handling (path đúng — verify graceful 400/404).

---

## Endpoint contract surprises (cho coordinator)

1. **Single-mark body = `{enrollmentId, sessionId, status, notes?}` — KHÔNG `studentId`/`classId`.** Walker phải resolve `enrollment_id` (GET `/api/v1/enrollments` hoặc `psql SELECT id FROM enrollments WHERE student_id=X AND class_id=Y`) + `session_id` (`SELECT id FROM class_sessions WHERE class_id=Y`) TRƯỚC.
2. **Status enum = `PRESENT/ABSENT/LATE/EXCUSED/MAKEUP`. KHÔNG `EXCUSED_ABSENCE`.** Docs sai. Dùng `EXCUSED`.
3. **Bulk + period + class-batch yêu cầu header `X-Teacher-Id: <teacherId>` (required).** Thiếu → 400. Single-mark KHÔNG dùng header này (và không có authz).
4. **Single-mark URL = POST `/api/v1/attendance` (no path vars). Bulk = POST `/api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance`.** Bulk body cũng có `sessionId` (redundant với path).
5. **Bulk + stats(class) + period + class-batch có `@authz.hasAccessToClass(#classId)` (403 nếu teacher không thuộc lớp). Single-mark + GET by-id/enrollment + stats(student) KHÔNG có guard.**
6. **K12 period attendance (`/periods`) là flow riêng (subjectSection × periodNo × date), unique index V50. Daily-rollup = TT 22/2021/TT-BGDĐT (vắng cả ngày ≥7 tiết).** Flag K12-only — secondary scope; GET single `/periods/{id}` + `/students/{id}` + `/subject-sections/{id}` KHÔNG có per-resource authz.
7. **`@Transactional` bulk reuse single `markAttendance` per record (N+1 + all-or-nothing). Duplicate → 400 ATTENDANCE_ALREADY_MARKED (không 409).**
