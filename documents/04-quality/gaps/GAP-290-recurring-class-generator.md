# GAP-290: Recurring class session generator (RRULE / multi-day weekly)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — blocks P1 Solo Teacher daily ops (AC-OPS-002 FAIL)
**Domain:** Backend (kiteclass-core/module/clazz)
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

- [ ] Create class với recurrence "WEEKLY TU,TH 19:00-20:30 đến 2026-08-01" → ~24 ClassSession entries auto-created
- [ ] Edit recurrence (đổi until date) → only future sessions regenerated, attended sessions preserved
- [ ] Delete recurrence → existing sessions still queryable (FK preserved)
- [ ] Exclude dates supported (nghỉ lễ)
- [ ] Multi-day weekly (TU+TH cùng 1 form) supported
- [ ] Unit tests: cover edge cases (1 session, 100 sessions, leap year)
- [ ] Migration tested fresh DB + existing DB
- [ ] API contract docs updated trong `documents/01-business/kiteclass/clazz/api-contract.md`

## Related

- AC-OPS-002 (P1 review 2026-05-04)
- GAP-289 (Quick-add session — paired UX)
- GAP-291 (Reschedule — single session within recurrence)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1. Cross-cutting impact: P1 + P2 + P3 + P5 all need recurring schedules.
