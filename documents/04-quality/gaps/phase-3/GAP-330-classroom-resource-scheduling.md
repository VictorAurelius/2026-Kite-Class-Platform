# GAP-330: Classroom Resource Scheduling (Lab / Máy tính / Sân) with Conflict Detection

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D — NEW from P5 §"NEW-1")
**Related:** P5-k12-school.md AC-OPS-007

## Current State (verified 2026-05-04)

```bash
grep -rl "ResourceBooking\|room.scheduling" kiteclass/ --include="*.java"
```
Result: zero. GVs hiện tranh nhau qua group chat.

## Problem

Phòng máy tính, lab, năng khiếu, sân TD limited. GV cần đặt lịch theo tiết, conflict detection, audit.

## Proposed Fix

1. **Resource entity:** name, type, capacity, owner_role
2. **Booking entity:** resource_id, period_no, date, booked_by, class_id
3. **Conflict detection** at booking time
4. **Daily report** of resource utilization

## Acceptance Criteria

- [ ] Resource + Booking entities
- [ ] Conflict detection real-time
- [ ] Daily utilization report
- [ ] Test: book conflicting → reject with alternative slot suggestion
- [ ] business-logic-review.md 5-attribute

## Related

- **Cross-cuts:** GAP-053 (calendar)
- **Wave plan:** Bucket D Stage 5

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
