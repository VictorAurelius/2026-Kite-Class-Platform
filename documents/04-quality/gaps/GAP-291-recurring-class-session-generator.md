# GAP-291: Recurring class session generator (RRULE-based)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core/clazz module) + Frontend (class form)
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher (multi-week courses), P2 Center (regular schedules), P3 Medium Center

## Problem

P1 AC-OPS-002: "Teacher có thể tạo recurring class (weekly Tuesday 19:00-20:30 trong 12 tuần) với 1 form duy nhất". State-check `kiteclass-core` for `recurring|RRULE|RecurrenceRule|recurrence` → 2 matches:

1. `db/migration/V44__create_class_schedule_slots.sql` — schema migration
2. `module/k12/entity/ClassScheduleSlot.java:89` — has free-text `recurrence_note` field; comment at line 87-88 explicitly says **"structured exception handling deferred to Phase 2"**

Free-text note is documentation-only; no parser, no session generator. Teacher must create each session manually for 12-24 sessions per course.

## Root Cause

Phase 1 of K-12 module shipped intentionally without recurrence parser per V44 migration. Solo persona use-case wasn't enumerated as gating feature.

## Proposed Fix

1. Add `recurrence_rule` column (RFC 5545 RRULE format) to `class_schedule_slots` (or class entity).
2. Implement `RecurrenceService` parsing RRULE → list of `LocalDateTime` occurrences within bounded window (e.g. max 1 year).
3. Use library `org.dmfs:lib-recur` or `net.fortuna.ical4j` for RRULE parsing (battle-tested).
4. On class create with recurrence: generate sessions in same transaction via outbox per `design-patterns.md` §3.5.
5. Frontend: `ClassForm` adds recurrence picker (Daily / Weekly / Monthly + days-of-week selector + end-date OR end-after-N-occurrences).
6. Cap: max 100 sessions per recurrence to prevent abuse.

## Acceptance Criteria

- [ ] `class_schedule_slots.recurrence_rule` column added (migration)
- [ ] RecurrenceService unit tests cover Daily/Weekly/Monthly + DST + leap year + bounded window
- [ ] Frontend recurrence picker integrated in ClassForm
- [ ] Sessions auto-generated on save; ≤100 occurrences per recurrence
- [ ] AC-OPS-002 PASS in re-test (12-week × 1 day = 12 sessions in 1 form submit)

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §2
- AC: AC-OPS-002
- Existing partial: V44 migration + ClassScheduleSlot.java line 89

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check confirmed Phase 2 deferral comment in code (line 87-88).
