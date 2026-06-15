# GAP-1405: Create-path không tự set learning_resources.uploaded_file_id (paywall download chỉ cover material đã backfill)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-15 (GAP-1307 dedicated fix follow-up — discovery-to-gap-inline-filing)
**Affects:** `kiteclass-core` lms (`LmsServiceImpl.createResource` + `CreateLearningResourceRequest` + `LmsMapper`) + storage paywall

## Problem

GAP-1307 (download-url paywall) dùng FK deterministic `learning_resources.uploaded_file_id` để liên kết file ↔ lesson. V100 **backfill** các row CŨ (qua `url LIKE '%' || storage_path || '%'`), nhưng **create-path KHÔNG tự set FK**:

- `POST /lessons/{lessonId}/resources` → `LmsServiceImpl.createResource` → `lmsMapper.toResourceEntity(CreateLearningResourceRequest)`.
- `CreateLearningResourceRequest` (record) chỉ có `type / url / title / fileSize` — **không có `uploadedFileId`**.

Hệ quả: material MỚI tạo SAU V100 (cho bài trả phí, dùng file upload TENANT-scoped) sẽ có `uploaded_file_id = NULL` → `LessonMaterialAccessGuardImpl` coi là "không phải lesson material" → **không bị paywall** qua đường download-url. Đây là residual của GAP-1307: cơ chế remediation + backfill exposure hiện tại đã DONE, nhưng material tương lai cần create-path set FK để được bảo vệ.

## Proposed Fix

Thêm `uploadedFileId` vào `CreateLearningResourceRequest` (optional) + map trong `LmsMapper.toResourceEntity` + cập nhật API contract (`documents/01-business/.../api-contract.md`) + FE gửi `uploadedFileId` khi material tạo từ file upload. (Hoặc: enrich tại create-time bằng lookup `uploaded_files` theo storage_path embedded trong url — nhưng tránh tái dùng heuristic; ưu tiên truyền FK tường minh.)

## Acceptance Criteria

- [ ] `CreateLearningResourceRequest` mang `uploadedFileId` (nullable) + `LmsMapper` map sang entity.
- [ ] Material mới tạo từ file upload → `learning_resources.uploaded_file_id` được set.
- [ ] Regression: material trả phí mới tạo + student chưa enroll → download-url 403 (end-to-end qua create → download).
- [ ] API contract + FE đồng bộ.

## Related

- GAP-1307 (download-url paywall — DONE: FK + resilient guard + backfill + tests). Đây là follow-up enrichment cho create-path.
- `discovery-to-gap-inline-filing.md` §3 — filed inline khi fix GAP-1307.
