# GAP-1037: Logo upload chấp nhận `image/svg+xml` + MIME client-trusted → SVG-XSS latent

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core) — security
**Found:** 2026-06-06 (KC-10 G1 walk, FM-3)
**Affects:** `BrandingServiceImpl:46-49` MIME allowlist + uploadLogo/uploadFavicon

## Problem

KC-10 G1 walk: `BrandingServiceImpl` MIME allowlist (GAP-804) cho phép `image/svg+xml`:
```java
// BrandingServiceImpl:48-49
"image/png", "image/jpeg", "image/webp",
"image/svg+xml", "image/x-icon", "image/vnd.microsoft.icon"
```

SVG có thể chứa `<script>` embedded → nếu logo served inline (Content-Type `image/svg+xml`, không `Content-Disposition: attachment`, không sanitize) → **stored XSS** khi browser render logo trên trang tenant/login. MIME validate từ **client-supplied header** (spoofable) — không content-sniff.

**Trạng thái verify:** LATENT — runtime verify bị chặn bởi GAP-1036 (upload 500 vì bucket thiếu). Không xác nhận được SVG có bị sanitize/serve-as-attachment hay không. Cần re-test sau khi GAP-1036 fix.

## Root Cause

- `image/svg+xml` trong allowlist nhưng SVG là active content (XSS vector), khác PNG/JPEG passive.
- MIME check dựa client header, không magic-byte content-sniff → attacker gửi `.html` payload với header `image/png`.

## Proposed Fix

1. **Loại `image/svg+xml`** khỏi logo/favicon allowlist (raster only: png/jpeg/webp/ico) — đơn giản nhất, đa số branding logo dùng raster.
2. NẾU phải hỗ trợ SVG → sanitize (strip `<script>`/`<foreignObject>`/event handlers via DOMPurify-server hoặc allowlist tags) + serve `Content-Disposition: attachment` HOẶC `Content-Security-Policy` sandbox.
3. Content-sniff magic bytes thay vì trust client MIME header.

## Acceptance Criteria

- [ ] SVG với `<script>` → reject 400 HOẶC stored sanitized (no script) + served non-inline
- [ ] MIME spoof (svg payload as `image/png` header) → reject (content-sniff)
- [ ] Logo served với CSP/Content-Disposition ngăn inline script exec
- [ ] Re-test sau GAP-1036 fix (upload phải work mới verify được)

## Related

- Discovered in: KC-10 G1 walk (Wave flow-kc10), pre-walk FM-3
- Blocked-by: GAP-1036 (upload 500 chặn runtime verify)
- File-upload security checklist: `pre-handoff-self-test-completeness.md` §2.5. Batch Wave security-1.
