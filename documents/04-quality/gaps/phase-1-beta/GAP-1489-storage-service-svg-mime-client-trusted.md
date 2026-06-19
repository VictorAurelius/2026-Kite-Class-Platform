# GAP-1489: StorageServiceImpl allowlist accepts image/svg+xml + client-trusted MIME

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-19 (Wave close-2 SEC — GAP-1037 cross-flow sweep DEFER site #2)
**Affects:** `kiteclass-core` `storage/.../StorageServiceImpl.java:88` `ALLOWED_MIME_TYPES`

## Problem

Cross-flow sweep của GAP-1037 (branding SVG-XSS fix) phát hiện sister site: generic attachment storage `StorageServiceImpl.ALLOWED_MIME_TYPES` (line ~88) vẫn chứa `image/svg+xml` + validate bằng client-reported MIME header (spoofable, không content-sniff magic bytes).

Khác GAP-1037 (branding inline render): storage này phục vụ file qua presigned download (PDF/video/doc), KHÔNG render inline như tenant-branding. Risk thấp hơn (Content-Disposition download thường vô hiệu hóa inline script) nhưng vẫn cần phân tích: nếu bất kỳ consumer nào serve các asset này inline → cùng class stored-SVG-XSS.

## Proposed Fix

1. Audit consumer của StorageServiceImpl: có path nào serve asset inline (Content-Type render) thay vì attachment download không?
2. Nếu có inline path → áp dụng cùng pattern GAP-1037 (remove svg / content-sniff / Content-Disposition: attachment + CSP).
3. Nếu chỉ download → enforce `Content-Disposition: attachment` + document tại sao svg acceptable.

## Acceptance Criteria

- [ ] Consumer audit: liệt kê mọi serve-path của StorageServiceImpl asset (inline vs attachment)
- [ ] Inline path (nếu có) reject svg+xml HOẶC content-sniff + sanitize
- [ ] Download-only path enforce Content-Disposition: attachment

## Related

- Parent: GAP-1037 (branding SVG-XSS — DONE-PARTIAL Wave close-2 SEC) cross-flow sweep
- Sibling sweep site (KH boundary): GAP-1490 (kitehub AssetStorageController content-type)
- MIME-validation class: `pre-handoff-self-test-completeness.md` §2.5 file-upload checklist
