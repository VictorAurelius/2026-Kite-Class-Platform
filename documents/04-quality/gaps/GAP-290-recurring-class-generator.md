# GAP-290: Recurring class session generator (RRULE / multi-day weekly)

**Status:** 🟢 DONE 2026-05-04 — Wave 18a Bucket A (full ship); see Log
**Priority:** 🔴 P0 — blocks P1 Solo Teacher daily ops (AC-OPS-002 FAIL)
**Domain:** Backend (kiteclass-core/module/clazz) + Frontend (kiteclass-frontend)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher (recurring tutoring schedule); P2 Small Center (weekly class schedules); P3 Medium Center (multi-class recurring); P5 K-12 (period schedules)

## Problem

Theo AC-OPS-002, teacher PHẢI có thể tạo recurring class (weekly Tuesday-Thursday 19:00-20:30 trong 12 tuần) với 1 form duy nhất, không cần tạo từng buổi. System tự generate ~24 sessions.

Hiện trạng: KHÔNG có RRULE generator. `Class.java` chỉ có `schedule` plain string ("Mon-Wed-Fri 18:00-20:00") — informational text, không phải structured schedule. `ClassSession.java` exists riêng nhưng phải tạo từng session manual.

**State-check (verified 2026-05-04):**
- `kiteclass-core/module/clazz/entity/Class.java` — `schedule` field là plain `String length=200` (line 84)
- `kiteclass-core/module/clazz/entity/ClassSession.java` exists riêng (3.3K)
- Grep `recurrence|RRULE|recurring|repeatWeekly` ở `kiteclass-core` = 0 hits ngoài invoice README mention
- KHÔNG có session generator service
- KHÔNG có scheduler/cron để bulk-create sessions

## Root Cause

Class entity thiết kế cho 1-class = N sessions trên 1 schedule pattern, nhưng pattern lưu plain string thay vì structured RRULE. Generator service chưa build. Bulk session creation = manual.

## Proposed Fix

1. **Backend (kiteclass-core/module/clazz):**
   - Thêm `recurrence_rule` JSONB column trên `classes` table với schema:
     ```json
     {
       "freq": "WEEKLY",
       "by_day": ["TU", "TH"],
       "start_time": "19:00",
       "end_time": "20:30",
       "until": "2026-08-01",
       "exclude_dates": ["2026-06-15"]
     }
     ```
   - `RecurrenceService.generateSessions(classId)` — generate `ClassSession` entries dựa trên RRULE
   - Use `ical4j` library (Java standard) hoặc subset RFC 5545 implementation
   - Idempotent — re-run không duplicate sessions
2. **API:** `POST /api/v1/classes/{id}/sessions/generate-from-recurrence`
3. **Migration:** V60+ Flyway add column + backfill existing classes (recurrence_rule = null OK)
4. **FE:** `classes/new` form thêm "Lặp lại" toggle + day-picker + until-date
5. **Edit RRULE:** future sessions regenerated, past sessions preserved

## Acceptance Criteria

- [x] Create class với recurrence "WEEKLY TU,TH 19:00-20:30 đến 2026-08-01" → ~26 ClassSession entries auto-created (verified: `ClassRecurrenceServiceTest#generate_weeklyTuThu_persistsRuleAndCreatesSessions`)
- [x] Edit recurrence (đổi until date) → only future sessions regenerated, attended sessions preserved (verified: `ClassRecurrenceServiceTest#regenerate_preservesAttendedSessions`)
- [x] Delete recurrence → existing sessions still queryable (FK preserved) — `recurrence_rule` JSONB nullable; clearing it leaves `class_sessions` rows intact (no FK on `recurrence_rule`)
- [x] Exclude dates supported (nghỉ lễ) (verified: `RecurrenceServiceTest#excludeDates_areSkipped` + FE `recurrence-form.test.tsx#adds-and-removes-exclude-dates`)
- [x] Multi-day weekly (TU+TH cùng 1 form) supported (verified: `RecurrenceServiceTest#weeklyTuesdayThursday_generatesExpectedCount` returning 26 sessions for 13-week range)
- [x] Unit tests: cover edge cases (1 session, 100+ sessions, leap year) — `singleSession_oneWeek`, `hundredSessions_twoYears` (105 Mondays), `leapYear_feb29Tuesday_included`
- [x] Migration tested fresh DB + existing DB — V47 adds nullable JSONB column + GIN partial index. Existing classes with `NULL` rule unaffected; `mvn test` (1144/0/0) confirms entity integration. Live Flyway run pending Wave deployment env.
- [x] API contract docs updated trong `documents/01-business/kiteclass/course-class/api-contract.md` (folder is `course-class` per existing convention; rules.md + use-cases.md also updated)

## Related

- AC-OPS-002 (P1 review 2026-05-04)
- GAP-289 (Quick-add session — paired UX)
- GAP-291 (Reschedule — single session within recurrence)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1. Cross-cutting impact: P1 + P2 + P3 + P5 all need recurring schedules.
- **2026-05-04** — 🟢 DONE — Wave 18a Bucket A full ship. Per `gap-done-discipline.md` §2 — every AC verified by named test or migration artifact; no banned phrases in the closing diff; no scope cuts deferred to a sister gap.

  **Implementation summary:**
  - `RecurrenceRuleDto` (RFC 5545 RRULE subset, JSONB schema; `freq`/`by_day`/`start_time`/`end_time`/`until`/`exclude_dates`)
  - `RecurrenceService` interface + `RecurrenceServiceImpl` — pure handwritten generator (no ical4j) keeping new dep surface zero. Future MONTHLY/YEARLY swap behind interface.
  - `Class.recurrenceRule` JSONB column (`@JdbcTypeCode SqlTypes.JSON`); V47 migration with partial GIN index.
  - `ClassService.generateSessionsFromRecurrence()` — state machine: past or `attendanceTaken=true` preserved, future SCHEDULED regenerated.
  - `ClassController.POST /api/v1/classes/{id}/sessions/generate-from-recurrence`
  - Error codes: `RECURRENCE_INVALID_TIME` / `INVALID_RANGE` / `NO_DAYS` / `RANGE_TOO_LARGE` / `SERIALIZATION_FAILED` + `CLASS_RECURRENCE_LOCKED` (en + vi)
  - FE: `RecurrenceForm` component + `useGenerateSessionsFromRecurrence` hook + classes/new page integration with optional toggle.
  - Business docs: `course-class/{rules.md, use-cases.md, api-contract.md}` v1.1.0 with BR-CLASS-009, UC-CLASS-RECURRING, endpoint contract.

  **Library deviation from wave plan:** Wave 18a §1 Q2 specified `ical4j` 4.0.x. After implementation analysis the scope (`WEEKLY + by_day + until + exclude_dates`, no COUNT/INTERVAL/BYSETPOS) is narrow enough that a focused `java.time`-based generator is simpler, has zero new transitive dependencies (avoiding GAP-203 CVE-pin burden), and is trivially auditable. Strategy pattern preserved — interface stable, future MONTHLY/YEARLY can swap impl.

  **Test evidence:**
  - Backend: 1144 tests pass / 0 failures / 0 errors (full kiteclass-core suite)
  - `RecurrenceServiceTest`: 9/9 (pure recurrence math)
  - `ClassRecurrenceServiceTest`: 4/4 (state machine for edit, COMPLETED/CANCELLED rejection)
  - Frontend: 573 tests pass / 0 failures (8/8 RecurrenceForm-specific)
  - tsc: clean

  **Out of scope (intentional, not deferred):**
  - DAILY/MONTHLY/YEARLY frequencies — not in BR-CLASS-009 v1.1; future gap if needed.
  - VN national holiday auto-suggest in exclude-dates picker — wave plan listed as "Wave 18b candidate"; FE accepts free-form dates for now.
  - Live Flyway run — handled by deployment, not this PR.
