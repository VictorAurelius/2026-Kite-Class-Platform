# GAP-1309: StorageController confirmUpload + deleteFile thiếu per-resource ownership authz → intra-tenant IDOR

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (security full audit post wave-p0-closeout-1 — AUDIT-2026-06-14-security-full, F-002)
**Affects:** `kiteclass-core` storage module

## Problem

`StorageController` (`kiteclass-core/.../storage/controller/StorageController.java`) KHÔNG có `@PreAuthorize` ở class hoặc method nào, và 2 endpoint mutate nhận **chỉ `@PathVariable Long fileId`**:

- `POST /api/v1/storage/{fileId}/confirm` — `confirmUpload(Long fileId)` (L108)
- `DELETE /api/v1/storage/{fileId}` — `deleteFile(Long fileId)` (L173)

Service `StorageServiceImpl` (L151 / L225) làm `uploadedFileRepository.findByIdAndDeletedFalse(fileId)` rồi `confirmUpload()`/`softDelete()` **không nhận uploaderId/tenantId, không check uploader hay role**. So sánh: `generatePresignedDownloadUrl(fileId, requesterId, tenantId)` (L189) CÓ `checkAccessPermission` (PRIVATE=uploader-only, TENANT=same-tenant, L369-383) — bất đối xứng.

**Hệ quả:**
- **Cross-tenant:** được Hibernate `tenantFilter` chặn (UploadedFile extends BaseEntity `@Filter instance_id=:tenantId`) → findByIdAndDeletedFalse cross-tenant trả empty (404).
- **Intra-tenant: KHÔNG được chặn.** Bất kỳ user nào (mọi role — không có role gate) trong cùng tenant có thể **xóa mềm** hoặc confirm file của user khác bằng cách enumerate `fileId` tuần tự (vd STUDENT xóa material upload bởi TEACHER; quota của instance bị trừ qua `updateQuotaUsage(file.getInstanceId(), ...)`).

## Proposed Fix

1. Truyền `X-User-Id` (requesterId) + `X-Tenant-Id` vào `confirmUpload` + `deleteFile` (controller + service signature).
2. Service check owner-or-admin: chỉ uploader (hoặc role admin/owner tenant) được confirm/delete; ngược lại 403.
3. Thêm `@PreAuthorize` role gate hợp lý cho StorageController (ít nhất authenticated role hợp lệ của tenant).
4. (Liên quan, reference) ALLOWED_MIME_TYPES có `image/svg+xml` → stored-SVG-XSS, đề nghị mở rộng scope GAP-1037 sang storage chung.

## Acceptance Criteria

- [ ] User KHÔNG phải uploader (và không phải admin/owner) cùng tenant → confirm/delete file người khác bị từ chối (403).
- [ ] Uploader (hoặc admin/owner) → confirm/delete bình thường (2xx).
- [ ] Cross-tenant vẫn 404 (Hibernate filter giữ nguyên).
- [ ] Regression test (CI-bound `*Test`): non-owner deny, owner allow, admin allow.

## Related

- Discovered in: AUDIT-2026-06-14-security-full F-002 (EVIDENCE-2026-06-14-OWASP-A01-002). Reserved gap-ID per `multi-session-concurrency-coordination.md`.
- GAP-1307 (storage download-url enrollment paywall — OPEN P1) — cùng controller, concern khác (download vs confirm/delete).
- GAP-729 (11/19 controller no per-resource authz — DONE) — cùng class A01 IDOR, StorageController còn sót.
- GAP-1037 (logo upload svg+xml MIME client-trust → SVG-XSS — OPEN P2) — đề nghị mở rộng scope.
