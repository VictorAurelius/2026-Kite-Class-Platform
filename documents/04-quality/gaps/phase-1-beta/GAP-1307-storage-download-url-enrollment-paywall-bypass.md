# GAP-1307: StorageController download-url bỏ qua LMS enrollment paywall (chỉ check visibility, không check enrollment)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (wave-p0-closeout-1 Bucket A — cross-flow sweep DEFER, PR #2403)
**Affects:** `kiteclass-core` storage + LMS (cross-module)

## Problem

`StorageController.generateDownloadUrl` (`GET /api/v1/storage/{fileId}/download-url`, `StorageController.java:140`) enforce access control bằng **visibility model** (`PUBLIC` / `PRIVATE` / `TENANT`) — KHÔNG check LMS enrollment.

Hệ quả paywall bypass: file tài liệu (material) của **bài học trả phí** nếu được lưu ở scope `TENANT` → **bất kỳ user cùng tenant nào** (kể cả student CHƯA enroll khóa trả phí) đều có thể lấy presigned download URL + tải nội dung. Đây cùng class với GAP-1115/1116 (LMS paywall) nhưng qua **đường storage download**, mà `LessonAccessGuard` (shipped #2403, service-layer LMS) KHÔNG cover — guard chỉ gate read-path (`getCourseStructureForStudent` / `getLessonForStudent`) + write-path (`completeLesson`), không gate file-download path.

Phát hiện qua cross-flow sweep của Bucket A (`cross-flow-bug-class-sweep.md` §3): classify FIX (3 LMS service site) + EXEMPT (guest/teacher) + **DEFER (this) = storage download-url** — cross-module concern ngoài scope service-layer của Bucket A.

## Proposed Fix

Khi file thuộc tài liệu bài học trả phí, storage download-url PHẢI thêm enrollment check (delegate `LessonAccessGuard` hoặc tương đương) — không chỉ dựa visibility. Cần liên kết file ↔ lesson ↔ pricing (cross-module storage↔LMS). Free/preview lesson + non-lesson files không đổi hành vi.

## Acceptance Criteria

- [x] Student cùng tenant CHƯA enroll khóa trả phí → `download-url` cho file của bài trả phí bị từ chối (403) — qua `LessonMaterialAccessGuard` (chỉ khi link file↔lesson resolve được).
- [x] Student đã enroll → tải bình thường (200).
- [x] File của bài free/preview + file không thuộc lesson → không bị siết (hành vi cũ giữ nguyên).
- [x] Regression test (CI-bound `*Test`): non-enrolled → reject, enrolled → allow.

## Resolution (2026-06-15, audit-fixB PR) — PARTIAL

**Feasible guard đã ship** (đủ cho common case + AC test):

1. **Interface** `LessonMaterialAccessGuard` (module storage — dependency inversion, storage
   KHÔNG import LMS, tránh bean cycle vì `LmsServiceImpl` đã phụ thuộc `StorageService`).
2. **Impl** `LessonMaterialAccessGuardImpl` (module LMS): resolve file↔lesson qua
   `learning_resources.url CONTAINS storage_path` → lesson → module → course; nếu lesson
   **non-trial** + requester **không enroll** → ném `PermissionDeniedException("STUDENT_NOT_ENROLLED_IN_COURSE")`
   (403). Delegate enrollment check về `LessonAccessGuard.isStudentEnrolledInCourse` (cùng
   single-source-of-truth với read/write path GAP-1115/1116). No-op cho: staff (elevatedRole),
   uploader, trial/preview lesson, file không phải material (không match).
3. **download-url** (`StorageController` + `StorageServiceImpl.generatePresignedDownloadUrl`):
   thêm param `elevatedRole` (TEACHER/OWNER/ADMIN/PLATFORM_ADMIN từ X-User-Roles → exempt khỏi
   paywall student) + gọi guard SAU `checkAccessPermission` (visibility).

**Test:** `LessonMaterialAccessGuardImplTest` (Mockito, CI-bound `*Test`) 6/6 PASS — non-enrolled
deny (403), enrolled allow, trial allow, staff bypass, uploader allow, non-lesson allow.

### Vì sao PARTIAL — cross-module wiring còn lại

`learning_resources.url` là **free-text** (S3 key / presigned URL / external link / YouTube) —
KHÔNG có FK sạch tới `uploaded_files`. Guard match storage_path như substring của `url`:
- ✅ cover case URL chứa storage key (UUID ngẫu nhiên → gần như không collision);
- ⚠️ **residual hole**: nếu teacher lưu URL không chứa key (CDN ngoài) → file không nhận diện
  là material → KHÔNG bị paywall (leak vẫn còn ở case đó);
- ⚠️ phân biệt staff↔student dựa header `X-User-Roles` forward bởi gateway, không phải per-resource
  role lookup.

**Clean fix (out of scope batch này):** thêm cột FK `learning_resources.uploaded_file_id`
(cross-module schema change) để link chính xác + 1 endpoint integration walk. Giữ PARTIAL tới khi
có FK + walk.

## Related

- Discovered in: PR #2403 (wave-p0-closeout-1 Bucket A cross-flow sweep DEFER) — coordinator allocated gap-ID per `multi-session-concurrency-coordination.md` (agent cố ý không tự mint để tránh collision).
- GAP-1115 / GAP-1116 (LMS paywall read+write path — service-layer, đã hardened qua `LessonAccessGuard` #2403).
- `cross-flow-bug-class-sweep.md` §3 — sweep evidence nguồn.
