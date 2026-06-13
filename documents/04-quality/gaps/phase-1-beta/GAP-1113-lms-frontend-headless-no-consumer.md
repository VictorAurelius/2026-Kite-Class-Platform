# GAP-1113: LMS frontend headless — BE đủ 15 endpoint nhưng kiteclass-frontend 0 consumer

**Status:** 🟡 PARTIAL (60% — Increment A shipped FE: teacher authoring + guest catalog/trial + assignment give/grade; student-player gated KC-9; G2 browser-walk pending)
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-10 (audit cluster doc DB schema-reference LMS — phát hiện BE LMS đầy đủ nhưng FE chưa tiêu thụ)
**Affects:** `kiteclass/kiteclass-frontend` (0 consumer LMS) ↔ `kiteclass-core/module/lms` (15 endpoint: `LmsController` 12 + `LessonProgressController` 3)

## Problem

Backend LMS `kiteclass-core` đã ship đầy đủ tầng API:

- **Teacher authoring (CRUD):** `POST/PUT/DELETE /modules`, `POST/PUT/DELETE/GET /lessons/.../manage`, `POST/DELETE /resources` — chỉ course owner (`X-Teacher-Id`, BR-LMS-006/010/015).
- **Guest/Student preview + consumption:** `GET /courses/{courseId}/modules` (course structure), `GET /lessons/{lessonId}` (lesson detail — guest chỉ thấy trial, BR-LMS-001/002).
- **Student progress:** `POST /lessons/{lessonId}/complete` (idempotent), `GET /progress/courses/{courseId}`, `GET /progress/lessons/{lessonId}` (BR-LMS-016..020).

NHƯNG `kiteclass-frontend` **KHÔNG có consumer nào** cho 15 endpoint này — module LMS hoàn toàn headless ở FE. Học sinh/giáo viên không có UI để authoring nội dung, preview khóa học, hay theo dõi tiến độ.

## Proposed Fix

Build FE LMS theo 2 increment (tách theo dependency auth):

- **Increment A — Teacher authoring + Guest preview/trial:** UI teacher CRUD module/lesson/resource (dùng teacher-auth) + trang guest course-preview + trial lesson viewer (dùng guest + course-structure endpoints). KHÔNG bị chặn bởi student-auth.
- **Increment B — Student consumption:** UI student xem paid lesson (cần enrollment) + complete lesson + progress dashboard. **CHẶN bởi student-auth KC-9.**

### Prerequisites BẮT BUỘC (trước khi scope/build)

1. **GAP-1121 (RLS) merge trước khi đưa LMS lên production** — cụm LMS V79 chưa bật RLS DB-level; build FE consumer mà chưa fix RLS → tăng surface cross-tenant leak khi có traffic thật.
2. **Student-auth KC-9** cho Increment B — student consumption không build được cho tới khi student-auth flow hoàn chỉnh.
3. **Outside-in persona audit (student / teacher) TRƯỚC khi scope FE** — per `.claude/rules/outside-in-coverage-trigger.md`: chạy persona simulation (giáo viên authoring + học sinh học) để bắt gap UX/kỳ vọng trước khi lock scope FE, không build inside-out thuần.

## Acceptance Criteria

- [ ] **DEFER — không build session này.** Gap track FE LMS consumer cho wave sau.
- [ ] Increment A (teacher-authoring + guest preview/trial): UI tiêu thụ teacher CRUD + course-structure + trial lesson endpoints
- [ ] Increment B (student consumption): UI complete lesson + progress — sau khi KC-9 student-auth unblock
- [ ] Outside-in persona audit (student/teacher) chạy TRƯỚC khi lock scope FE
- [ ] GAP-1121 RLS merged trước khi LMS FE lên production

## Related

- Discovered in: PR `feature/gap-1111-lms-db-doc-cluster` (cluster DB docs audit 2026-06-10)
- BE endpoints: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/lms/controller/LmsController.java` (12) + `LessonProgressController.java` (3)
- Business rules: `documents/01-business/kiteclass/lms/rules.md` (BR-LMS-001..020)
- Schema cluster: `documents/02-architecture/database/kiteclass/09-lms.md` (GAP-1120)
- Prereq RLS: **GAP-1121** (enable RLS DB-level cụm LMS) — merge trước production
- Prereq student-auth: **KC-9** (student-auth flow) — chặn Increment B
- Outside-in rule: `.claude/rules/outside-in-coverage-trigger.md` (persona audit trước scope)
