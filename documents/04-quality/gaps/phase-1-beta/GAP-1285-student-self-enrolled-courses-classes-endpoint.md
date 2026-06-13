# GAP-1285: Thiếu endpoint student-self liệt kê khóa học/lớp đã ghi danh

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core)
**Found:** 2026-06-14 (Wave rbac-lms-student-fe — khi build student-shell PART 1/2/3 FE)
**Affects:** `kiteclass-frontend` student-shell (`/student/learning`, `/student/assignments`) ↔ `kiteclass-core` enrollment/course/class controllers

## Problem

Khi build student-shell (LMS lesson-player + assignment-submit), FE cần một danh sách "khóa học / lớp tôi đã ghi danh" để làm entry point cho học sinh. Nhưng **không có endpoint nào STUDENT tự gọi được để liệt kê enrollment của chính mình**:

- `GET /api/v1/enrollments/student/{studentId}` — guarded `@PreAuthorize("hasAnyRole('STAFF') or @authz.hasAccessToStudent(#studentId)")`. `hasAccessToStudent` (`AuthorizationBean.java:206`) chỉ trả `true` khi actor là admin HOẶC actor **dạy** một lớp chứa student đó (native query `c.teacher_id = :userId`). STUDENT tự gọi cho chính mình → KHÔNG khớp → **403**.
- `GET /api/v1/classes` (`ClassController` SHARED READ) — student gọi được nhưng trả **toàn bộ lớp trong tenant**, KHÔNG scope theo enrollment của student.
- `GET /api/v1/courses` (`CourseController`) — student gọi được nhưng là **catalog PUBLISHED toàn tenant**, không phải khóa đã ghi danh.

Hệ quả FE (workaround hiện tại trong Wave rbac-lms-student-fe):
- `/student/learning` liệt kê **catalog PUBLISHED toàn tenant** (`coursesApi.getAll(PUBLISHED)`) thay vì khóa student đã ghi danh. Lesson-player vẫn đúng (BE strip content bài trả phí khi chưa enroll per GAP-1115 → paywall CTA).
- `/student/assignments` quét **published assignments qua các lớp trong tenant** (`classesApi.list()` SHARED READ) thay vì lớp student đã ghi danh.

Với tenant beta một-trường thì workaround chấp nhận được (universe = đúng tập của student), nhưng KHÔNG đúng khi tenant nhiều lớp/khoá mà student chỉ học một phần → student thấy lớp/khoá không thuộc mình.

## Proposed Fix

Thêm endpoint student-self-accessible ở kiteclass-core (BE — ngoài scope wave FE này):
- `GET /api/v1/enrollments/me` (X-User-Id / JWT-derived) trả enrollment của chính actor STUDENT, hoặc nới `@authz.hasAccessToStudent` cho self-access (actor.userId == studentId).
- Trả kèm `courseId` + `courseName` + `className` để FE dựng "Khóa học của tôi" + "Lớp của tôi" enrollment-scoped, thay workaround catalog/SHARED-READ.

FE sau đó đổi `/student/learning` + `/student/assignments` sang endpoint enrollment-scoped (xóa NOTE GAP-1285 trong 2 page).

## Acceptance Criteria

- [ ] Có endpoint STUDENT tự gọi liệt kê enrollment của chính mình (course + class), không 403.
- [ ] `/student/learning` liệt kê khóa đã ghi danh (không phải catalog toàn tenant).
- [ ] `/student/assignments` quét đúng lớp student ghi danh (không phải toàn tenant).

## Related

- Discovered in: Wave rbac-lms-student-fe (FE student-shell build) — design-first investigation: `hasAccessToStudent` authz + `ClassController` SHARED READ note.
- BE authz: `kiteclass-core/.../common/security/AuthorizationBean.java` `hasAccessToStudent`
- BE controllers: `EnrollmentController` (`/student/{id}` guarded), `ClassController` (`/api/v1/classes` SHARED READ), `CourseController` (`/api/v1/courses` catalog)
- Parent shells: GAP-1119 (role-shell), GAP-1113 (LMS FE consumer)
