---
title: Pre-walk persona simulation — KC-3 course→class→schedule
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign — Wave flow-kc3 pre-walk per pre-walk-persona-simulation-mandate.md §1
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-2026-06-05-flow-kc3-academic-year-course-class-schedule.md
  - .claude/rules/pre-walk-persona-simulation-mandate.md
---

# Pre-walk persona simulation — KC-3 course→class→schedule

**Persona walked:** Owner/STAFF tenant `sky-education` setup cấu trúc học thuật lần đầu (course → class → schedule → sessions). Academic-year DROPPED (orphan GAP-982).

**Agent:** Opus 4.7 background, spawned 2026-06-05 per `pre-walk-persona-simulation-mandate.md` §1 + `agent-model-opus-default.md`.

## Failure modes (10)

1. **DTO drift — `CreateScheduleRequest` require `daysOfWeek` (NotEmpty), curl spec ban đầu thiếu → 400**
   - Where: `clazz/dto/CreateScheduleRequest.java:24-26` (`@NotEmpty List<DayOfWeek> daysOfWeek`) + `ClassServiceImpl.createSchedule:336,356`
   - Symptom: POST `/schedule` với `{startTime,endTime}` → 400 "Phải chọn ít nhất 1 ngày học". Body đúng: `{"daysOfWeek":["MONDAY","WEDNESDAY"],"startTime":"18:00","endTime":"20:00"}` — enum `java.time.DayOfWeek` full-name, KHÔNG iCal 2-letter.
   - Confidence: HIGH. **Fix walk spec trước.**

2. **`createClass` set `teacher_id = caller UUID` (Owner), không phải teacher gán → cross-actor 403**
   - Where: `ClassServiceImpl.createClass:116-119` (`setTeacherId(UserContext.getCurrentUser())`) + `AuthorizationBean.hasAccessToClass:88-94`. `CreateClassRequest` KHÔNG có teacherId.
   - Symptom: Owner tạo lớp → teacher_id=Owner. Nếu walk như teacher thật → `hasAccessToClass` deny.
   - Confidence: MEDIUM. Persona decision: walk caller = lớp owner.

3. **RLS GUC timing tại `@PreAuthorize` native query → default-deny → 403 lớp của chính owner (sister GAP-727)**
   - Where: `AuthorizationBean.hasAccessToClass:88-93` native query tại SpEL `@PreAuthorize` phase; `TenantAwareDataSourceInterceptor:84,95` chỉ set GUC khi `@Transactional` active.
   - Symptom: PATCH/start/complete/schedule/reschedule → 403 dù đúng owner (classes FORCE RLS V58:41), vì GUC chưa set lúc authz query chạy.
   - Confidence: HIGH blocker class. **Verify empirically tại walk.** Workaround: ADMIN role bypass (`isAdmin` line 79) HOẶC caller == lớp owner.

4. **Course require numeric `teachers.id` (Long) — truyền STAFF UUID → 404 TEACHER_NOT_FOUND**
   - Where: `CreateCourseRequest.java:72-73` (`@NotNull Long teacherId`) + `CourseServiceImpl.createCourse:103-107` (`teacherRepository.findByIdAndDeletedFalse`).
   - Symptom: teachers.id là BIGINT, không phải auth UUID. Phải lấy numeric id từ psql trước.
   - Confidence: HIGH. **Lấy numeric teachers.id từ psql trước walk.**

5. **Gateway 503 cold-start `/api/v1/courses/**` + `/classes/**` (GAP-918 recurrence)**
   - Where: gateway route, first request sau idle.
   - Symptom: POST đầu → 503.
   - Confidence: MEDIUM. Warm-up 2-3 lần + verify image date vs HEAD (GAP-978).

6. **`createSchedule` require class.startDate+endDate not null (CLASS_NO_DATES) — DTO optional → fail muộn**
   - Where: `ClassServiceImpl.createSchedule:340-342` + `CreateClassRequest.java:43-45` (dates optional).
   - Symptom: tạo lớp không dates → bước schedule 400 CLASS_NO_DATES.
   - Confidence: HIGH. **Include startDate+endDate khi tạo lớp** (endDate>startDate per validateDates:581).

7. **Recurrence validation strict + KHÔNG auto holiday-skip (academic-year orphan)**
   - Where: `RecurrenceServiceImpl.validate:91-105` + safetyCap:56-59 (>3700 days). Holiday-skip chỉ qua `excludeDates` thủ công, không từ Holiday entity.
   - Symptom: `by_day:[]`→RECURRENCE_NO_DAYS; until<start→RECURRENCE_INVALID_RANGE; sessions sinh trên ngày lễ.
   - Confidence: MEDIUM. Body hợp lệ: `{"freq":"WEEKLY","by_day":["MO","WE"],"start_time":"18:00","end_time":"20:00","until":"2026-08-01"}`. Document holiday gap.

8. **Schedule overlap/double-booking KHÔNG validate (missing feature)**
   - Where: `ClassServiceImpl.createSchedule:344-368` chỉ check endTime>startTime, không check overlap teacher/room.
   - Symptom: 2 lớp cùng teacher cùng slot → cả hai 201.
   - Confidence: LOW. File discovery gap nếu surface.

9. **`createSchedule` APPEND sessions (max+1) thay vì replace → gọi lần 2 sinh trùng; khác `generate-from-recurrence` (soft-delete regenerable)**
   - Where: `ClassServiceImpl.createSchedule:348-352` vs `generateSessionsFromRecurrence:441-447`.
   - Symptom: xếp lại lịch = double sessions, không idempotent.
   - Confidence: LOW. File discovery gap.

10. **maxStudents edge (0/null khác message) + class name uniqueness query tenant scope**
    - Where: `ClassServiceImpl.createClass:97-100` (`existsByNameAndCourseIdAndInstanceIdAndDeletedFalse` — có explicit tenantId per GAP-799) + `CreateClassRequest.java:47-50` (@NotNull @Min(1) @Max(500)) vs service default 30 (dead code).
    - Symptom: maxStudents=0→400; trùng tên→CLASS_NAME_EXISTS.
    - Confidence: LOW. Cosmetic spot-check.

## Recommended pre-walk batch fix

- **HIGH (verify/fix trước walk):** #1 (walk spec daysOfWeek), #3 (RLS GUC verify empirical — blocker), #4 (numeric teachers.id psql), #6 (class startDate+endDate).
- **MEDIUM (spot-check):** #2 (persona caller==owner), #5 (warm-up + image date), #7 (recurrence body + holiday gap doc).
- **LOW (defer/file gap):** #8 (overlap missing), #9 (append idempotency), #10 (maxStudents edge + i18n).

## Walk readiness note

Không có code-fix bắt buộc pre-walk — HIGH items là (a) walk-spec corrections (#1/#4/#6) + (b) empirical verification (#3). Walk proceed với corrected curl specs + catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4. #8/#9 → discovery gaps nếu confirmed.
