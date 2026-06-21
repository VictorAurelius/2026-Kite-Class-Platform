# GAP-1489: StorageServiceImpl allowlist accepts image/svg+xml + client-trusted MIME

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-19 (Wave close-2 SEC — GAP-1037 cross-flow sweep DEFER site #2)
**Closed:** 2026-06-22 (GAP-1527 A01 sweep — removed `image/svg+xml` from `StorageServiceImpl.ALLOWED_MIME_TYPES`)
**Affects:** `kiteclass-core` `storage/.../StorageServiceImpl.java` `ALLOWED_MIME_TYPES`

## Problem

Cross-flow sweep của GAP-1037 (branding SVG-XSS fix) phát hiện sister site: generic attachment storage `StorageServiceImpl.ALLOWED_MIME_TYPES` (line ~88) vẫn chứa `image/svg+xml` + validate bằng client-reported MIME header (spoofable, không content-sniff magic bytes).

Khác GAP-1037 (branding inline render): storage này phục vụ file qua presigned download (PDF/video/doc), KHÔNG render inline như tenant-branding. Risk thấp hơn (Content-Disposition download thường vô hiệu hóa inline script) nhưng vẫn cần phân tích: nếu bất kỳ consumer nào serve các asset này inline → cùng class stored-SVG-XSS.

## Proposed Fix

1. Audit consumer của StorageServiceImpl: có path nào serve asset inline (Content-Type render) thay vì attachment download không?
2. Nếu có inline path → áp dụng cùng pattern GAP-1037 (remove svg / content-sniff / Content-Disposition: attachment + CSP).
3. Nếu chỉ download → enforce `Content-Disposition: attachment` + document tại sao svg acceptable.

## Acceptance Criteria

- [x] `image/svg+xml` removed from `StorageServiceImpl.ALLOWED_MIME_TYPES` (raster-only: jpeg/png/gif/webp + docs/video/audio)
- [x] Class-level javadoc documents the SVG-drop rationale (presigned-URL flow → server never sees bytes → cannot sniff/sanitize; SVG = active content)

Note on the original "audit inline-serve consumers" scope: this is a **presigned-URL** upload/download flow — the client declares the MIME in `PresignedUploadRequest` and `validateFileType` checks only the client-declared MIME; the server never holds the bytes, so magic-byte sniffing is not possible at this layer. Removing SVG from the allowlist (mirroring the GAP-1037 branding raster-only hardening) is the actionable closure. Magic-byte sniffing applies where the server DOES hold bytes (e.g., VettingController, fixed in the same GAP-1527 PR).

## Related

- Closed by: GAP-1527 (kiteclass-core OWASP A01 residual sweep, 2026-06-22)
- Parent: GAP-1037 (branding SVG-XSS — DONE-PARTIAL Wave close-2 SEC) cross-flow sweep
- Sibling sweep site (KH boundary): GAP-1490 (kitehub AssetStorageController content-type)
- MIME-validation class: `pre-handoff-self-test-completeness.md` §2.5 file-upload checklist

## Log

- **2026-06-22** — Closed via GAP-1527. Removed `image/svg+xml` from `ALLOWED_MIME_TYPES`. Presigned-URL flow means server never sees bytes (cannot magic-byte sniff) → SVG-drop is the correct closure; mirrors GAP-1037 branding raster-only hardening. `git mv` to `phase-1-beta/closed/`.
