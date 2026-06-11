# GAP-1166: Điểm danh hàng loạt — FE gọi `POST /api/v1/attendance/bulk` mà BE chưa expose (FE↔BE drift)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-11 (sweep từ GAP-1165 cross-flow)
**Affects:** `kiteclass-frontend` `lib/api/attendance.ts:38` `markBulkAttendance` → `use-attendance.ts:133` (`useMarkBulkAttendance`) ; `kiteclass-core` `AttendanceController`

## Problem

FE `attendanceApi.markBulkAttendance(data)` gọi `POST /api/v1/attendance/bulk`. `AttendanceController` KHÔNG có `/bulk` — bulk-mark thật nằm ở `POST /api/v1/attendance/classes/{classId}/sessions/{sessionId}/attendance` (cần `classId` + `sessionId` path params + header `X-Teacher-Id`). FE call hiện sẽ 404 khi giáo viên điểm danh hàng loạt 1 buổi.

Phát hiện trong cross-flow sweep của GAP-1165 (cùng bug class: FE call site lệch BE mapping). DEFER vì khác flow (điểm danh hàng loạt ≠ report-export demo) + payload shape khác (FE `BulkAttendanceRequest` không gửi classId/sessionId trong path).

## Proposed Fix

Quyết định 1 trong 2 (cần đối chiếu use-case điểm danh thật): (a) thêm BE alias `POST /api/v1/attendance/bulk` nhận `{classId, sessionId, records}` trong body + derive teacherId từ JWT; HOẶC (b) sửa FE `markBulkAttendance` + `BulkAttendanceRequest` để gọi `/classes/{classId}/sessions/{sessionId}/attendance` với header `X-Teacher-Id`. Ưu tiên (a) nếu muốn FE contract gọn; xác minh `@PreAuthorize` + teacher-id source.

## Acceptance Criteria

- [ ] FE bulk-mark → BE 2xx (không 404)
- [ ] Authz teacher-of-class enforced; teacherId không tin client tùy tiện
- [ ] Walk: giáo viên điểm danh 1 buổi nhiều HS → DB rows tạo đúng

## Related

- Sweep parent: GAP-1165
- Sister drift: GAP-1167
