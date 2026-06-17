# GAP-1475: Teacher gradebook (và mọi roster dùng /enrollments/class) hiển thị tên học sinh trống — EnrollmentResponse thiếu studentName

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-17 (cross-flow sweep khi fix GAP-1474 — per `cross-flow-bug-class-sweep.md`)
**Affects:** `kiteclass/kiteclass-core/.../module/enrollment/dto/EnrollmentResponse.java`, `kiteclass/kiteclass-frontend/src/app/(teacher)/teacher/grades/[classId]/page.tsx`

## Problem

BE `EnrollmentResponse` (trả về bởi `GET /api/v1/enrollments/class/{classId}` qua `EnrollmentMapper.toResponse`) **chỉ có `studentId`, KHÔNG có `studentName`** — dù FE type `Enrollment` khai báo `studentName: string`. Mọi surface render `enrollment.studentName` từ roster endpoint này sẽ hiện tên TRỐNG (`undefined`).

Phát hiện trong cross-flow sweep khi fix GAP-1474:

- **Teacher gradebook** `(teacher)/teacher/grades/[classId]/page.tsx:119` — `roster` map `fullName: e.studentName` (từ `useActiveEnrollmentsByClass`) → cột tên học sinh trong gradebook trống. (Empty-state khi 0 ACTIVE đã có thông báo "chưa có học sinh nào đang học" — KHÔNG im lặng — nên chỉ blank-name là vấn đề ở surface này.)
- **GAP-1474 attendance + class-roster** đã workaround bằng cách resolve name client-side từ `useStudents` map (fallback `enrollment.studentName || map.get(studentId) || 'Học sinh #id'`) — band-aid, KHÔNG phải root fix.

## Proposed Fix

Fix hệ thống tại 1 chỗ thay vì N FE map: **thêm `studentName` vào `EnrollmentResponse`** + enrich trong `EnrollmentServiceImpl` (`getEnrollmentsByClass` / `ByClassAndStatus`) — join/lookup `students.name` theo `studentId` (giống cách `getMyEnrollments` enrich `className`/`courseName`). Khi đó attendance + class-roster + gradebook đều có tên đúng; các FE fallback `enrollment.studentName || ...` tự short-circuit về giá trị BE (forward-compatible, không cần gỡ).

## Acceptance Criteria

- [ ] `EnrollmentResponse` có `studentName`; `GET /api/v1/enrollments/class/{classId}` trả tên học sinh.
- [ ] Teacher gradebook (KC-6) hiển thị tên học sinh thật (không trống).
- [ ] Không regress attendance/class-roster (FE fallback vẫn đúng).

## Related

- Discovered in: cross-flow sweep của GAP-1474 (wave-flow-kc3, branch `docs/gap-1474-kc3-roster-attendance`)
- Parent: GAP-1474 (KC-3 owner roster + attendance PENDING_PAYMENT)
- Rule: `cross-flow-bug-class-sweep.md` (DEFER verdict — sister surface, KC-6 scope, root fix là BE enrichment)
