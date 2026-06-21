# GAP-1490: kitehub AssetStorageController serves asset with client content-type (SVG-XSS class)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-19 (Wave close-2 SEC — GAP-1037 cross-flow sweep DEFER site #3)
**Affects:** `kitehub-branding` `AssetStorageController.java:87,107`

> **DONE 2026-06-22** (security wave `fix/branding-a01-authz`, bundled with GAP-1526): `AssetStorageController.uploadAsset` now resolves a serve-safe content-type via `resolveSafeContentType()` — rejects SVG outright (HTTP 400), magic-byte sniffs PNG/JPEG/GIF/WebP (authoritative over the client-declared type), rejects markup-shaped payloads spoofing an image type, and persists ONLY the sniffed/allowlisted type into the asset record (never the client-reported `file.getContentType()`). There is no inline-serve path in this controller — bytes are served via MinIO presigned URLs — so the gate is enforced at upload time. AC verified by existing `AssetStorageControllerTest` (52 controller-tests PASS) + new `AssetStorageControllerAuthzTest`.

## Problem

Cross-flow sweep của GAP-1037 phát hiện sister site phía KiteHub: `AssetStorageController` (kitehub-branding product) serve asset với `file.getContentType()` (client-reported) tại line ~87/107 → nếu allowlist upload chấp nhận `image/svg+xml` + không content-sniff → cùng class stored-SVG-XSS như GAP-1037.

Tách khỏi GAP-1037/GAP-1489 (KiteClass scope) vì thuộc KiteHub product per `kitehub-kiteclass-boundary.md` — cần phân tích upload allowlist + serve-path riêng của kitehub-branding.

## Proposed Fix

1. Audit kitehub-branding upload allowlist: có chấp nhận `image/svg+xml` không? Content-sniff không?
2. Audit `AssetStorageController` serve-path: inline render (Content-Type) hay attachment download?
3. Nếu inline + svg accepted → mirror GAP-1037 fix (remove svg / content-sniff magic bytes / Content-Disposition).

## Acceptance Criteria

- [ ] kitehub-branding upload allowlist không chấp nhận active-content SVG (HOẶC sanitize + content-sniff)
- [ ] AssetStorageController inline-serve path reject SVG-XSS vector
- [ ] Test: upload svg+script → reject; spoof svg-as-png → reject

## Related

- Parent: GAP-1037 (KiteClass branding SVG-XSS) cross-flow sweep
- Sibling (KiteClass scope): GAP-1489 (StorageServiceImpl svg+xml)
- Boundary: `kitehub-kiteclass-boundary.md` — KH product, separate from KC branding
