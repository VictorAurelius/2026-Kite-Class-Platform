# GAP-307: Multi-Class Scheduling with 3-Axis Conflict Detection

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — blocks GA cho P3 (Medium Center) + P5 (K-12 School)
**Domain:** Backend (kiteclass-core) + Frontend (Schedule Builder UI for ops admin)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 4 ACs across 4 personas (tenant + student + admin + teacher)

---

## Problem

P3 vận hành 30 active classes × 12 teachers × 5 rooms × 7 time slots/ngày × 6 ngày/tuần. Quản lý học vụ phải:

1. Build weekly schedule không double-book teacher (1 teacher chỉ ở 1 lớp 1 thời điểm)
2. Không double-book room (1 room 1 lớp 1 thời điểm)
3. Không cho student trùng giờ với lớp khác đã enrolled (multi-class scenario — student × 3-5 môn)
4. Drag-drop UI để move class slot nhanh
5. Real-time conflict warning + suggest alternative slots
6. Notification gửi affected stakeholders khi confirm

Student bên cạnh cần **unified weekly view across 3-5 môn** trên mobile (không per-class navigation). Teacher cần **own-class weekly view** (chỉ 8 lớp của mình, không nhiễu 30 lớp).

**Without conflict detection, ops vận hành P3 break ngay đầu kỳ — multi-class enrollment impossible.**

## Root Cause

KHÔNG có module schedule trong codebase:
- `find kiteclass/kiteclass-core/src/main/java -type d -name "schedule*"` → chỉ `storage/scheduler` + `invoice/scheduler` (cron jobs, NOT class scheduling)
- `find kiteclass-frontend/src/app -type d -name "schedule*"` → 0 results
- Class entity (`module/clazz`) có `room` field nhưng không có `ScheduleSlot` entity với time + day-of-week
- Không có service `ConflictDetectionService` với 3-axis check

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Class entity | `kiteclass-core/.../module/clazz` | ✅ exists (no schedule slots) |
| ScheduleSlot entity | — | ❌ missing |
| Room entity | — | ❌ missing (room is just string field on Class) |
| ConflictDetectionService | — | ❌ missing |
| Schedule Builder UI | — | ❌ missing |
| Student weekly view (multi-class unified) | — | ❌ missing |
| Teacher own-class weekly view | `(dashboard)/teacher/dashboard/page.tsx` | 🟡 scaffold (no week-grid) |

## Proposed Fix

**Phase 1 — Domain model (Wave 18-D):**
1. Entities: `Room` (capacity + equipment tags), `ScheduleSlot` (class × room × day-of-week × start-time × end-time × teacher).
2. Service: `ConflictDetectionService.checkConflicts(slot)` returns `List<Conflict>` with type (TEACHER_DOUBLE_BOOK / ROOM_DOUBLE_BOOK / STUDENT_OVERLAP) + suggested alternatives.
3. Migration: `V71__schedule_slots_rooms.sql`.

**Phase 2 — Schedule Builder UI (Wave 18-E):**
1. Ops admin / Quản lý học vụ: drag-drop weekly grid view 7 days × time slots; live conflict highlights; suggest alternatives modal.
2. Student: unified weekly view across enrolled classes, color-coded per subject, conflict warnings.
3. Teacher: own-class weekly view with "Today's classes" widget + "Take attendance" CTA.

**Phase 3 — Notifications (Wave 18-F):**
1. Schedule change → notification gửi affected teacher + students/parents (depends on GAP-063 Zalo OA).

## Acceptance Criteria

- [ ] Room entity supports capacity (int) + equipment tags (e.g. "projector", "máy lạnh")
- [ ] ScheduleSlot entity validates non-overlapping (database constraint or service-level check)
- [ ] `ConflictDetectionService.checkConflicts()` returns 3 conflict types + ≥3 alternative slots
- [ ] Schedule Builder UI drag-drop responds <300ms on 30-class scale
- [ ] Student unified weekly view loads ≤2s on mobile (3G simulation) for 5-class enrollment
- [ ] Teacher own-class view scoped — RBAC blocks viewing other teachers' classes (403)
- [ ] Conflict on capacity (class enrollment > room capacity) blocked at slot creation
- [ ] Schedule change triggers Zalo notification to affected stakeholders (depends GAP-063)
- [ ] Performance test: build full week schedule for 30 classes in ≤5 min wall-clock interaction time
- [ ] All business rules in `documents/01-business/kiteclass/scheduling/rules.md` per `business-logic-review.md` §2 5-attribute standard

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-OPS-001 | Tenant Director | `P3-medium-center.md` |
| AC-OPS-001 | Student | `secondary/student-in-P3.md` |
| AC-OPS-005 | Admin (ops admin) | `secondary/admin-in-P3.md` |
| AC-OPS-001 | Teacher Employee | `secondary/teacher-employee-in-P3.md` |

## Related

- Persona review: [`documents/00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P3-medium-center-round-1-2026-05-04.md) §Finding 2
- Depends on: GAP-063 (Zalo notification — for schedule change alerts)
- Business doc target: `documents/01-business/kiteclass/scheduling/{rules.md,use-cases.md,api-contract.md}`

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C. State-check confirmed no schedule/conflict module. 4 ACs blocked across 4 personas.
