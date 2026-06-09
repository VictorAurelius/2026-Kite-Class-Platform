---
audience: mixed
---

# 2026-06-10 — Outside-In Audit (3-lens): FE LMS wave (KiteClass)

**Scope:** Module LMS của KiteClass (`kiteclass/kiteclass-core/.../module/lms` + future `kiteclass-frontend`). Audit này = outside-in prereq cho FE LMS wave tương lai (per `outside-in-coverage-trigger.md` — FE wave là user-facing scope, cần persona + benchmark + failure-mode trước khi lock scope). Deliverable cho GAP-1113 scope.
**Method:** 3-lens — (1) Persona simulation, (2) External benchmark VN edu, (3) Failure-mode matrix.
**Audit date:** 2026-06-10
**BE-fix wave kèm theo:** GAP-1115 (P0 paywall) + GAP-1116 (P1 enrollment) + GAP-1117 (P1 500) + GAP-1118 (P2 tenant-leak) — fix trong cùng PR (Phase 0 BE-fix bên dưới).

---

## Bối cảnh — LMS hiện trạng (đã verify code)

3-tier: **Course → CourseModule → Lesson** + `LearningResource` (chỉ URL) + `LessonProgress`.

Endpoints hiện có (`LmsController` + `LessonProgressController`):
- Guest: `getCourseStructurePublic` (chỉ trial), `getLessonPublic` (chỉ trial).
- Student: `getCourseStructureForStudent`, `getLessonForStudent`, progress (`completeLesson` / `getCourseProgress` / `getLessonProgress`).
- Teacher CRUD: module/lesson/resource create-update-delete (owner-only qua `verifyCourseOwnership`).

Lệch quan trọng: LMS gắn enrollment qua **Class** (Course → Class → Enrollment), KHÔNG trực tiếp Course→Enrollment. `verifyStudentEnrollment` resolve mọi Class của Course rồi check ACTIVE enrollment ở bất kỳ Class nào.

---

## Lens 1 — Persona simulation

Personas: **Học viên (Student)**, **Phụ huynh (Parent)**, **Giáo viên/Trợ giảng (Teacher)**, **Chủ trung tâm (Owner)**.

| Persona | Kỳ vọng | Gap surfaced |
|---|---|---|
| Student | Vào học bài đã mua, xem trial trước khi mua, theo dõi progress | **F1 paywall** (xem full content bài trả phí khi chưa mua); thiếu FE lesson-player (video + markdown + resource); `getCourseStructureForStudent` không 404 khi course không tồn tại (F9) |
| Teacher | Soạn khoá: tạo module/lesson, sắp xếp thứ tự, đánh dấu trial, đính kèm tài liệu | Thiếu **FE teacher-authoring UI** (builder); resource chỉ nhận URL (không upload file — F-resource); không có reorder endpoint atomic (F4) |
| Owner | Quản lý khoá toàn trung tâm, gán giáo viên, xem ai hoàn thành | Thiếu **owner-scope endpoint** (chỉ teacher-owner CRUD; owner/admin oversight thiếu — F14); thiếu completion-roster ("ai hoàn thành bài/khoá") |
| Parent | Xem tiến độ học của con | Thiếu **parent-scope endpoint** xem progress của con (F13) |

**Kết luận Lens 1:** LMS BE đủ cho luồng teacher-owner-CRUD + student-access cơ bản, nhưng (a) **paywall hổng** (F1, fix wave này), (b) thiếu FE 2 mặt (teacher-authoring + student-player), (c) thiếu owner/parent oversight scope. Lệch Course↔Class↔Enrollment khiến UX enroll khó trực giác (student enroll vào Class chứ không phải Course).

---

## Lens 2 — External benchmark (trung tâm dạy thêm VN)

Benchmark đối tượng: trung tâm dạy thêm / luyện thi VN (không phải Coursera/Udemy global content-LMS).

**Phát hiện chính:** trung tâm VN **operations-first** (điểm danh + học phí + thông báo Zalo), KHÔNG content-LMS-first. LMS nội dung là phụ trợ, không phải core. Vì vậy:

**MVP LEAN (Phase 1 BETA + 1.5):**
- Course builder (teacher tạo module/lesson + đánh trial).
- Lesson-player (student xem video URL + markdown content + tải resource).
- Trial/paywall UI (rõ ràng "bài này cần mua / đã mở khoá").
- Progress hiển thị (% hoàn thành khoá).
- Surface `assignment` — BE đã có module assignment riêng; LMS nên link tới.

**DEFER (không Phase 1):**
- Quiz auto-grade — **P0 nếu cohort luyện thi THPT** (trắc nghiệm là nghiệp vụ chính của luyện thi); ngược lại defer.
- Analytics học tập sâu (heatmap, dropout).
- Zalo-notify khi hoàn thành bài — **tách track riêng** (Zalo integration là wave riêng per `thesis-as-future-state-mandate.md`).

**CẮT (không làm):**
- SCORM/xAPI, certificate generation, DRM-video, forum/discussion — over-scope cho trung tâm dạy thêm.

---

## Lens 3 — Failure-mode matrix

| ID | Severity | Mô tả | Trạng thái |
|---|---|---|---|
| **F1** | 🔴 P0 | `getCourseStructureForStudent` trả full `content`+`videoUrl` bài trả phí cho student chưa enroll (paywall bypass) | **FIX wave này → GAP-1115** |
| **F2** | 🟠 P1 | `completeLesson` không enforce enrollment cho bài trả phí (BR-LMS-019 no-op) | **FIX wave này → GAP-1116** |
| **F3** | 🟠 P1 | Thiếu header X-User-Id/X-Teacher-Id → `MissingRequestHeaderException` rơi catch-all → HTTP 500 | **FIX wave này → GAP-1117** (global, cross-flow) |
| **F10** | 🟡 P2 | `findLessonWithTenantContext` set TenantContext không clear/restore → rò cross-tenant pooled thread | **FIX wave này → GAP-1118** |
| F4 | P1 | Không có reorder endpoint atomic (sắp xếp lại lesson order) | DEFER-doc — FE wave |
| F5 | P1 | Create-order race → duplicate order_number → 500 (uniqueConstraint vi phạm thay vì 409) | DEFER-doc |
| F6 | P2 | Enrollment COMPLETED mất quyền ôn lại (chỉ check ACTIVE) → học viên xong khoá không xem lại được | DEFER-doc |
| F7 | P2 | `progressPercent` > 100% khả thi khi lesson bị soft-delete sau khi đã complete (completed > total) | DEFER-doc |
| F8 | P3 | Module rỗng (0 lesson) — FE cần xử lý empty state | DEFER (FE) |
| F9 | P2 | Student structure endpoint không 404 khi course không tồn tại (trả list rỗng) | DEFER-doc |
| F11 | P3 | 2 mã lỗi 403 (`TRIAL_LESSON_REQUIRED` vs `STUDENT_NOT_ENROLLED`) — FE cần phân biệt message | DEFER (FE) |
| F13 | P1 | Thiếu parent-scope endpoint xem progress của con | DEFER-doc (FE wave Phase 2) |
| F14 | P1 | Thiếu admin/owner oversight endpoint (chỉ teacher-owner) | DEFER-doc |
| F15 | P3 | delete-module cascade — FE cần confirm "module có N lesson, xoá hết?" | DEFER (FE) |
| F17 | P2 | PATCH update lesson dùng `NullValuePropertyMappingStrategy.IGNORE` → không clear được field về null (vd gỡ videoUrl) | DEFER-doc |
| F18 | P2 | Trial-flip concurrent (2 teacher cùng đổi isTrial) — FE/optimistic-lock | DEFER (FE) |

**Đã fix wave này:** F1 + F2 + F3 + F10. **Còn lại defer-doc** (F4/F5/F6/F7/F9/F13/F14/F17) + defer-FE (F8/F11/F15/F18) → đưa vào scope FE LMS wave.

---

## 3-Phase plan (kết luận audit)

**Phase 0 — BE-fix (wave NÀY):** F1/F2/F3/F10 → GAP-1115/1116/1117/1118 (fix + unit test, runtime-walk pending DONE flip).

**Phase 1 — BE-gap-fill (wave kế):**
- Course-list endpoint (liệt kê khoá của trung tâm/teacher).
- Publish/unpublish course state machine.
- Reorder endpoint atomic (F4) + create-order race → 409 (F5).
- Resource upload thật (MinIO/S3) thay vì chỉ URL.
- Owner-scope (F14) + parent-scope (F13) progress oversight.

**Phase 2 — FE lean MVP:**
- **Increment A (sau Phase 0+1):** Teacher-authoring builder + guest preview (trial). Không phụ thuộc student-auth.
- **Increment B (chờ student-auth KC-9):** Student lesson-player + progress UI + paywall surface. Phụ thuộc parent/student portal (KC-9).

**Quiz auto-grade:** quyết theo cohort — nếu beta tenants có luyện thi THPT → kéo lên P0 Phase 1; ngược lại defer Phase 2+.

---

## Cross-link

- BE-fix gaps: `documents/04-quality/gaps/phase-1-beta/GAP-1115..1118-*.md`
- Trigger rule: `.claude/rules/outside-in-coverage-trigger.md` (audit này = outside-in prereq cho FE LMS wave)
- Thesis-as-future-state: Zalo-notify completion tách track riêng per `.claude/rules/thesis-as-future-state-mandate.md`
- KC-9 dependency: student/parent portal auth (memory `project_parent_student_portal_phase2_gated`)
