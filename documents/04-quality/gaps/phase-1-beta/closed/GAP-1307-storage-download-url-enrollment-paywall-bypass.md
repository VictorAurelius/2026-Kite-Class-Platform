# GAP-1307: StorageController download-url bỏ qua LMS enrollment paywall (chỉ check visibility, không check enrollment)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (wave-p0-closeout-1 Bucket A — cross-flow sweep DEFER, PR #2403)
**Resolved:** 2026-06-15 (dedicated fix — deterministic FK + context-resilient guard)
**Affects:** `kiteclass-core` storage + LMS (cross-module)

## Problem

`StorageController.generateDownloadUrl` (`GET /api/v1/storage/{fileId}/download-url`, `StorageController.java:140`) enforce access control bằng **visibility model** (`PUBLIC` / `PRIVATE` / `TENANT`) — KHÔNG check LMS enrollment.

Hệ quả paywall bypass: file tài liệu (material) của **bài học trả phí** nếu được lưu ở scope `TENANT` → **bất kỳ user cùng tenant nào** (kể cả student CHƯA enroll khóa trả phí) đều có thể lấy presigned download URL + tải nội dung. Đây cùng class với GAP-1115/1116 (LMS paywall) nhưng qua **đường storage download**, mà `LessonAccessGuard` (shipped #2403, service-layer LMS) KHÔNG cover — guard chỉ gate read-path (`getCourseStructureForStudent` / `getLessonForStudent`) + write-path (`completeLesson`), không gate file-download path.

Phát hiện qua cross-flow sweep của Bucket A (`cross-flow-bug-class-sweep.md` §3): classify FIX (3 LMS service site) + EXEMPT (guest/teacher) + **DEFER (this) = storage download-url** — cross-module concern ngoài scope service-layer của Bucket A.

## Proposed Fix

Khi file thuộc tài liệu bài học trả phí, storage download-url PHẢI thêm enrollment check (delegate `LessonAccessGuard` hoặc tương đương) — không chỉ dựa visibility. Cần liên kết file ↔ lesson ↔ pricing (cross-module storage↔LMS). Free/preview lesson + non-lesson files không đổi hành vi.

## Acceptance Criteria

- [x] Student cùng tenant CHƯA enroll khóa trả phí → `download-url` cho file của bài trả phí bị từ chối (403). — `StorageServicePaywallTest.guardPresent_rejects_deniesBeforePresign` + `LessonMaterialAccessGuardImplTest.paidLesson_nonEnrolled_denied`.
- [x] Student đã enroll → tải bình thường (200). — `StorageServicePaywallTest.guardPresent_allows_returnsUrl` + `LessonMaterialAccessGuardImplTest.paidLesson_enrolled_allowed`.
- [x] File của bài free/preview + file không thuộc lesson → không bị siết (hành vi cũ giữ nguyên). — `LessonMaterialAccessGuardImplTest.{nullFk_allowed, noLinkedResource_allowed, trialLesson_allowed}`.
- [x] Regression test (CI-bound `*Test`): non-enrolled → reject, enrolled → allow. — 11 test mới (8 guard + 3 service); `StorageServiceAuthzTest` (IDOR cũ) vẫn 4/4 PASS; **`OpenApiSpecExportTest` 1/1 PASS** (full-context load không gãy với wiring ObjectProvider); `Wave02MigrationsTest` 11/11 PASS (validate V100 qua full Flyway chain trên Postgres thật).

## Resolution (2026-06-15)

Khác với attempt #2416 (heuristic `url CONTAINS storage_path` + hard-require cross-module bean → gãy context-load), fix dedicated dùng:

1. **FK deterministic** — `V100__learning_resources_uploaded_file_fk.sql`: thêm cột nullable `learning_resources.uploaded_file_id BIGINT` + FK → `uploaded_files(id)` `ON DELETE SET NULL` + index `idx_learning_resources_uploaded_file_id`; backfill best-effort idempotent (DO $$ guard) link các row cũ qua `url LIKE '%' || storage_path || '%'` scoped theo tenant. Entity `LearningResource.uploadedFileId` (scalar Long) + `@Index`.
2. **Guard context-resilient** — interface `storage.service.LessonMaterialAccessGuard` + impl `lms.service.LessonMaterialAccessGuardImpl`. `StorageServiceImpl` inject qua `ObjectProvider<LessonMaterialAccessGuard>` + `getIfAvailable()` → context thiếu LMS bean vẫn load (skip paywall = allow); production full-context có bean → enforce. Guard resolve lesson qua FK (`findByUploadedFileIdAndDeletedFalse`), KHÔNG dùng url heuristic.
3. **download-url** — `generatePresignedDownloadUrl(fileId, requesterId, tenantId, elevatedRole)`; staff (`TEACHER/OWNER/ADMIN/PLATFORM_ADMIN`) + uploader + file free/trial/non-lesson → allow; paid-lesson material + non-enrolled non-staff student → 403 (`STUDENT_NOT_ENROLLED_IN_COURSE`) TRƯỚC bước S3 presign (không leak URL).

## Related

- Discovered in: PR #2403 (wave-p0-closeout-1 Bucket A cross-flow sweep DEFER) — coordinator allocated gap-ID per `multi-session-concurrency-coordination.md` (agent cố ý không tự mint để tránh collision).
- GAP-1115 / GAP-1116 (LMS paywall read+write path — service-layer, đã hardened qua `LessonAccessGuard` #2403).
- `cross-flow-bug-class-sweep.md` §3 — sweep evidence nguồn.
- #2416 (split-out, reverted heuristic) — bài học: deterministic FK + ObjectProvider thay cho substring heuristic + hard-require bean.
- GAP-1405 (follow-up P2) — create-path chưa tự set `uploaded_file_id`: material trả phí MỚI tạo sau V100 cần create-path set FK để được paywall (V100 chỉ backfill row cũ). Residual đã track, không silent.
