# GAP-291: Reschedule lesson session endpoint + lifecycle

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — blocks P1 Solo Teacher daily ops (AC-OPS-006 FAIL)
**Domain:** Backend (kiteclass-core/module/clazz)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher (booking churn daily); P2/P3 Center (reschedule scenarios)

## Problem

Theo AC-OPS-006, teacher PHẢI có thể reschedule 1 session (đổi ngày/giờ) trong ≤3 clicks và auto-notify students qua Zalo/SMS.

Hiện trạng: KHÔNG có reschedule endpoint. Class lifecycle chỉ có `canCancel()` + `cancelled_at`. Teacher phải delete + create lại = lost attendance/grade history + manual notification.

**State-check (verified 2026-05-04):**
- `kiteclass-core/module/clazz/entity/Class.java` line 175-228: chỉ có cancel/start/complete transitions
- Grep `reschedule|RESCHEDULED|Reschedule` ở `kiteclass-core` = 0 hits
- KHÔNG có `original_date` / `rescheduled_from_id` field tracking
- Notification gửi tự động: separate gap GAP-063 (Zalo/SMS)

## Root Cause

Class lifecycle thiết kế static: SCHEDULED → IN_PROGRESS → COMPLETED. Reschedule = real-world ops case chưa modeled.

## Proposed Fix

1. **Backend (kiteclass-core/module/clazz):**
   - Add `RESCHEDULED` to `ClassStatus` enum (or reschedule = SCHEDULED → SCHEDULED with new dates + audit log)
   - `ClassService.reschedule(classId, newStartDate, newEndDate, reason)`:
     - Validate `canEditSchedule()` (status = SCHEDULED)
     - Update start_date + end_date
     - Create AuditLog entry "class.rescheduled" với original/new dates
     - Publish domain event `ClassRescheduledEvent` (Outbox pattern per `design-patterns.md` §3.5)
     - Notification consumer (subscribed to event) gửi Zalo/SMS to enrolled students
2. **API:** `POST /api/v1/classes/{id}/reschedule` body: `{newStartDate, newEndDate, reason}`
3. **FE:** `classes/[id]/page.tsx` add "Đổi lịch" button → modal với date/time picker + reason field (optional)
4. **Notification:** depend on GAP-063 (Zalo/SMS) for actual delivery

## Acceptance Criteria

- [ ] Reschedule endpoint preserves attendance + grade history
- [ ] Audit log captures original + new dates + reason + actor
- [ ] Domain event fires via Outbox
- [ ] Status transition validated (chỉ SCHEDULED reschedulable, không IN_PROGRESS)
- [ ] FE 3-click flow: Open class → "Đổi lịch" → save
- [ ] Notification sent (when GAP-063 lands)
- [ ] Unit + IT tests cover reschedule + history preservation

## Related

- AC-OPS-006 (P1 review 2026-05-04)
- GAP-063 (Zalo/SMS notification — required for AC-OPS-006 full PASS)
- GAP-290 (Recurring class — paired session-management)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
