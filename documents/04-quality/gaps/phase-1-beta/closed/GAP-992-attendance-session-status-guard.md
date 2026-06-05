# GAP-992: markAttendance thiếu session-status guard (BR-ATTEND-002) + session-existence

**Status:** 🟢 DONE (Wave flow-kc5 G1 walk PASS, 2026-06-05)
**Priority:** 🟠 P1
**Domain:** Backend (business rule — KC-5)
**Found:** 2026-06-05 (Wave flow-kc5 pre-walk persona simulation, FM #4)
**Affects:** `AttendanceServiceImpl.markAttendance` (single) + `markBulkAttendance`

## Problem

Single-mark KHÔNG load ClassSession → điểm danh được vào session đã COMPLETED/CANCELLED (201 thay vì 400), và mark với sessionId không tồn tại vẫn 201 (FK-only, không 404 graceful). Bulk load session nhưng chỉ check `classId`, không check status. Vi phạm BR-ATTEND-002 (`Attendance.java:37` javadoc: "Session must exist and not be COMPLETED/CANCELLED"). Hệ quả: ghi điểm danh cho buổi đã kết thúc/hủy.

## Proposed Fix

Single-mark: load session via `classSessionRepository.findByIdAndDeletedFalse` → 404 `SESSION_NOT_FOUND` nếu thiếu; 400 `SESSION_NOT_MARKABLE` nếu status ∈ {COMPLETED, CANCELLED}. Bulk: thêm status check sau session load (fail-fast cả batch).

## Acceptance Criteria
- [x] Mark vào session COMPLETED → 400 SESSION_NOT_MARKABLE (W4 single + W9b bulk)
- [x] Mark với sessionId không tồn tại (99999) → 404 SESSION_NOT_FOUND (W4b)
- [x] Mark vào session SCHEDULED → 201 (W1)
- [x] Unit + IT cover (AttendanceServiceTest +1 test + IT session fixtures)

## Related
- Mirror class GAP-989 (KC-4 enroll class-status guard)
- Discovered in: Wave flow-kc5 pre-walk 2026-06-05 (FM #4)

## Log

- **2026-06-05 (Wave flow-kc5 — DONE):** session existence + status guard (single + bulk); G1 walk PASS — COMPLETED→400, 99999→404, SCHEDULED→201.
