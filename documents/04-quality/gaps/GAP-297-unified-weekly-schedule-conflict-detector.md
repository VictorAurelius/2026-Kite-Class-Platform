# GAP-297: Unified weekly schedule view + teacher conflict detector + bulk-cancel notify

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (Feature-P0 — operational safety; bumped above its nominal P1 because absence of conflict catcher actively causes errors per `meta-gap-priority.md` §3 silent-degradation tie-breaker)
**Domain:** Frontend / Backend
**Found:** 2026-05-04 (P2 Small Center persona review round 1)
**Persona blocked:** P2 Small Tutoring Center (5 classes × 2-3 sessions/wk = 10-15 weekly slots, same teacher in 2-3 classes guarantees occasional conflicts); also affects P3 Medium Center
**Wave:** TBD

## Problem

Three operational ACs share a missing schedule-aggregation surface:

1. **AC-OPS-001 (P2 owner):** No unified weekly schedule view. `(dashboard)/` has `classes/`, `students/`, `teachers/`, `attendance/` routes but NO `schedule/` route. Owner must check 5 classes one-by-one to mentally reconcile teacher load.
2. **AC-OPS-001 (cont'd):** No teacher-conflict detector. grep `conflict.*schedul\|schedul.*conflict` in `kiteclass-core/src/main/java` returns 0 hits. Owner can accidentally book Teacher A at Toán 9A 19h AND Anh 12B 19h same day, system says nothing.
3. **AC-OPS-005 (P2 owner):** No single-session reschedule endpoint visible in `clazz/controller/`. ClassSession data layer supports per-session edits (sessionDate/startTime/endTime are mutable columns) but no API surface.
4. **AC-EDGE-001 (P2 owner):** No bulk-cancel-and-notify pipeline. When teacher reports sick at 17h, owner needs ONE action to cancel all of today's affected sessions + notify all 15+ parents.

## Root Cause

Schedule features grew per-class-first (each class has its own Sessions tab) without a cross-class index. Conflict detection requires a query across all classes for a tenant in a date window — not implemented because the use case (multi-class same-teacher) wasn't enumerated until persona review.

## Proposed Fix

| Sub-task | Surface | Estimate |
|---|---|---|
| `GET /api/v1/schedule/weekly?week=YYYY-WW` returns all sessions for a tenant | Backend | 0.5d |
| Conflict detector: query overlapping sessions where `teacher_id` matches | Backend | 0.5d |
| `(dashboard)/schedule/page.tsx` — week-grid 7×N with conflict highlight (red border) | Frontend | 1d |
| `PATCH /api/v1/sessions/{id}` for single-session reschedule (date/time/location) | Backend | 0.5d |
| Drag-drop reschedule on schedule view → calls PATCH endpoint | Frontend | 0.5d |
| Bulk-cancel modal: select range → `POST /api/v1/sessions/bulk-cancel` → triggers GAP-063 notify | Backend + Frontend | 1d |

Bulk-cancel-and-notify pipeline depends on **GAP-063** for the notification dispatch leg.

## Acceptance Criteria

- [ ] Weekly schedule API returns all 10-15 sessions for a P2-shaped tenant in one call
- [ ] Creating a session that overlaps an existing one with same teacher_id returns 409 Conflict (or warning flag in response)
- [ ] Schedule UI highlights conflicts in red with hover detail
- [ ] Single-session reschedule does not modify recurrence rule of the parent class
- [ ] Bulk-cancel endpoint records audit log + emits notify-event per cancelled session
- [ ] AC-OPS-001, AC-OPS-005, AC-EDGE-001 (P2 owner) flip PASS in next P2 review

## Related

- Audit: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` §2 + §5
- Dependencies: GAP-063 (notification dispatch for bulk-cancel)
- Reference AC docs: `documents/00-brd/persona-criteria/P2-small-center.md` §2 + §5
