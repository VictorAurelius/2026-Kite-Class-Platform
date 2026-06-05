# GAP-991: single-mark POST /api/v1/attendance thiếu authz guard (OWASP A01)

**Status:** 🟢 DONE (Wave flow-kc5 G1 walk PASS, 2026-06-05)
**Priority:** 🟠 P1
**Domain:** Backend (security — KC-5)
**Found:** 2026-06-05 (Wave flow-kc5 pre-walk persona simulation, FM #2)
**Affects:** `AttendanceController.markAttendance` (`/api/v1/attendance` POST single-mark)

## Problem

Single-mark POST `/api/v1/attendance` KHÔNG có `@PreAuthorize` + service KHÔNG check caller là teacher của lớp → bất kỳ authenticated tenant user nào điểm danh được cho enrollment bất kỳ (vi phạm BR-ATT-006/007 permission matrix). OWASP A01 broken access control. Cross-flow sweep miss của GAP-729 (Wave 105 đã guard bulk/stats/period nhưng bỏ sót single-mark).

## Proposed Fix

Thêm `@PreAuthorize("@authz.hasAccessToEnrollment(#request.enrollmentId)")` lên `markAttendance` single path — đối xứng với bulk (`hasAccessToClass`). Helper `hasAccessToEnrollment` đã tồn tại (resolve enrollment→class→teacher-or-admin).

## Acceptance Criteria
- [x] Single-mark by non-teacher/non-admin của lớp → 403 (W6/W6b: no/wrong X-User-Id → 403)
- [x] Single-mark by class teacher (X-User-Id = classes.teacher_id) → 201 (W1)
- [x] Test cover authz nhánh (live walk — IT có method-security OFF nên không enforce)

## Related
- Cross-flow sweep: GAP-729 (Wave 105 bulk authz); `cross-flow-bug-class-sweep.md`
- Discovered in: Wave flow-kc5 pre-walk 2026-06-05 (FM #2)

## Log

- **2026-06-05 (Wave flow-kc5 — DONE):** `@PreAuthorize hasAccessToEnrollment` added; G1 walk PASS — teacher → 201, no/wrong user → 403. Cross-flow sweep GAP-729 closed.
