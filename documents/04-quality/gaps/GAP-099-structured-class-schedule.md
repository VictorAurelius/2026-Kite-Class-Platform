# GAP-099: Structured Class Schedule (replaces free-form text)

**Status:** 🟡 PARTIAL (Phase 1 foundation shipped PR #355; Phase 2-3 still open)
**Priority:** 🟡 P2
**Domain:** KiteClass Core / Academic Year
**Found:** 2026-04-18 (TODO audit post Wave 4)
**Affects:** Class scheduling, timetable generation, attendance session mapping

## Problem

`kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/k12/entity/SubjectSection.java:24`:
> BR-SSEC-004: Schedule free-form text initially (TODO structured schedule)

Class schedule currently stored as unstructured String (e.g., "Monday 8:00-9:30, Wednesday 14:00-15:30"). No query-ability, no conflict detection, no iCal export, no automatic attendance session generation.

## Evidence

Current entity:
```java
@Column(name = "schedule_text", length = 500)
private String scheduleText; // Free-form: "Mon 8-9:30, Wed 14-15:30"
```

## Impact

- Can't detect teacher double-booking (same teacher, 2 classes same slot)
- Can't generate iCal for parents (GAP-052 parent portal)
- Attendance sessions created manually per session
- No weekly schedule visualization
- Mobile app timetable feature blocked

## Proposed Fix

**Phase 1: Data model** — DONE PR #355
1. ✅ New entity `ClassScheduleSlot` (subjectSectionId, dayOfWeek, startTime, endTime, effectiveFrom, effectiveUntil, recurrenceNote)
2. ✅ Flyway migration V44: `class_schedule_slots` table with 3 indexes + CHECK constraints
3. ⏸ Migration script converting free-form text — **deferred to Phase 2** (risky best-effort parsing, needs manual review workflow)
4. ✅ Repository với 3 common lookup methods
5. ✅ 6 unit tests (isActiveOn + getDurationMinutes)

**Phase 2: API + logic**
1. CRUD endpoints for slots
2. Conflict detection service (teacher + room double-booking)
3. Attendance session auto-generator (cron: weekly lookahead creates sessions)
4. iCal feed endpoint for parent portal

**Phase 3: UI**
1. Weekly grid UI for slot entry
2. Timetable view per student/teacher/room

## Acceptance Criteria

- [ ] Free-form text migrated (with audit trail)
- [ ] Conflict detection on create/update
- [ ] iCal feed per parent/teacher
- [ ] Weekly grid UI responsive
- [ ] 80% unit test coverage

## Dependencies

- Wave 5 (parent dashboard) benefits from iCal feed
- Estimated: M-L (2-3 weeks as dedicated wave)

## Related

- GAP-052 Parent Portal (iCal consumer)
- GAP-054 Multi-subject grades (same K-12 module)
