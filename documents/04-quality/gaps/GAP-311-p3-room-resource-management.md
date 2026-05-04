# GAP-311: Room/Resource Management with Capacity + Equipment Booking

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core room module new) + Frontend (resource manager UI)
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 1 AC tenant-level (P3 archetype)

---

## Problem

P3 thường có 5 rooms × capacity 20-30 + equipment (projector, máy lạnh, thiết bị Sciences). Quản lý học vụ cần:
1. Track room capacity → block class enrollment > capacity
2. Equipment booking riêng (e.g. projector cho lớp Tin chỉ 2 ngày/tuần)
3. Tag-based equipment (filter "rooms với projector")

Without this, class assignment dễ vượt capacity room → student stand outside.

## Root Cause

Room hiện chỉ là string field trên Class entity. Không có Room entity, capacity, equipment tags, booking.

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Room entity | — | ❌ missing (room is just string on Class) |
| Equipment entity / EquipmentBooking | — | ❌ missing |
| Capacity validation at enrollment | — | ❌ missing |
| Resource manager UI | — | ❌ missing |

## Proposed Fix

1. `Room` entity (name, capacity, equipment_tags JSONB)
2. `EquipmentBooking` entity (equipment_id × class_id × time-slot)
3. Validator on enrollment: block if room.capacity < enrolled_count
4. Frontend: resource manager CRUD + booking calendar
5. Filter rooms by equipment tag for class assignment

## Acceptance Criteria

- [ ] Room entity with capacity + equipment_tags
- [ ] Enrollment blocked when capacity exceeded with error message
- [ ] Equipment booking calendar visible to ops admin
- [ ] Filter rooms by equipment tag (e.g. "projector") for class assignment
- [ ] Migration backward-compatible (preserves existing string room field)

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-OPS-006 | Tenant Director | `P3-medium-center.md` |

## Related

- Depends on: GAP-307 (schedule slot uses Room entity)
- Persona review: §2 (Tenant AC-OPS-006)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
