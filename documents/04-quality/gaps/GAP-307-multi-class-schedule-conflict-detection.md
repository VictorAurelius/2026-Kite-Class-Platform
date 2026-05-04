# GAP-307: Multi-Class Schedule Conflict Detection (3-axis: Teacher × Room × Student)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (business-logic tier — P3 distinguishing differentiator)
**Domain:** Backend / Scheduling
**Found:** 2026-05-04 (Wave 17 Bucket C P3 persona review — round 1)
**Affects:** P3 Medium Center (30 classes × 12 teachers × 5 rooms), P5 K-12 School (40 classes × 45 teachers + period-based)

## Problem

P3 quy mô 30 classes × 12 teachers × 5 rooms × 7 time slots không thể schedule manual conflict-free. Quản lý học vụ cần Schedule Builder UI với drag-drop, system flag conflicts real-time on 3 axes:
1. Teacher double-booking
2. Room over-booking
3. Student schedule overlap (across multi-class enrollments)

State-check 2026-05-04:
- V44 migration `class_schedule_slots` table exists (GAP-099 Phase 1) — structured slots foundation ✅
- `Class.locationDetail` is free-text (`"Room 101 or zoom URL"`) — NO `Room` entity ❌
- `grep "schedule.*conflict\|TimeSlotConflict\|RoomConflict" kiteclass kitehub --include="*.java"` → 0 results ❌
- No conflict-detection service, no Schedule Builder UI

Affects ACs: AC-OPS-001 (tenant), AC-OPS-005/006 (tenant), AC-OPS-005 (admin), AC-OPS-001 (student multi-class unified calendar prerequisite).

## Root Cause

GAP-099 Phase 1 shipped slot foundation but conflict-detection service deferred. Substitute teacher matcher (GAP-312) depends on this gap because suggest-substitute must check teacher availability.

## Proposed Fix

3-phase delivery:

**Phase 1 — Room entity + capacity** (Wave 18-19)
- `Room` entity (instance_id + name + capacity + equipment_tags array + active flag)
- Migration V47+: rooms table + class_room_assignments
- API: `GET/POST /api/v1/rooms`

**Phase 2 — Conflict detection service** (Wave 19)
- `ScheduleConflictDetector` service with 3-axis check
- Algorithm: for each new slot, scan overlapping (teacher, room, student) within effective_from..effective_until
- API: `POST /api/v1/schedule/check-conflict` returns conflicts list
- Suggest 3 alternative slots when conflict detected

**Phase 3 — Schedule Builder UI** (Wave 19-20)
- FE drag-drop calendar `(dashboard)/admin/schedule/page.tsx`
- Real-time conflict highlighting + suggest-alternatives panel
- Notification fire to affected teachers + parents on schedule change

## Acceptance Criteria

- [ ] Phase 1: `Room` entity + migration V47+ + REST CRUD endpoints
- [ ] Phase 1: Room capacity validation enforced on class assignment
- [ ] Phase 2: `ScheduleConflictDetector` service detects 3-axis conflicts in <100ms for 30-class scope
- [ ] Phase 2: Suggest-alternatives algorithm returns 3 conflict-free slots (teacher qualified + room available + no student overlap)
- [ ] Phase 3: Schedule Builder UI drag-drop functional with real-time conflict warnings
- [ ] Phase 3: Notification fires to affected teachers + parents on schedule change (depends on GAP-309)
- [ ] Each phase: `documents/01-business/kiteclass/scheduling/{rules,use-cases,api-contract}.md` ships with code

## Related

- Audit report: `documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md` §Critical Findings #2
- Foundation: GAP-099 Phase 1 (V44 class_schedule_slots) — DONE
- Blocks: GAP-312 (substitute matcher needs conflict-check)
- Persona AC: P3-medium-center.md AC-OPS-001/005/006, admin-in-P3.md AC-OPS-005, student-in-P3.md AC-OPS-001
