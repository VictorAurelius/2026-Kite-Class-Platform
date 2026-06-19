# GAP-1490: kitehub AssetStorageController serves asset with client content-type (SVG-XSS class)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-19 (Wave close-2 SEC — GAP-1037 cross-flow sweep DEFER site #3)
**Affects:** `kitehub-branding` `AssetStorageController.java:87,107`

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
