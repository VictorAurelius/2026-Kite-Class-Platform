# GAP-1160: Banner thiếu ảnh chân dung — Playwright in-container không fetch được portrait presigned URL

**Status:** 🟡 PARTIAL — fix shipped PR #2289, pending G2 visual confirm
**Priority:** 🟡 P2
**Domain:** Backend (banner render / S3 presigned host)
**Found:** 2026-06-10 (Wizard Step 7 G2 browser-walk — PR #2289, follow-up commit 9b2070ac "portraits in banner")
**Affects:** `kitehub-branding` `BannerHtmlComposer` · `PlaywrightBannerRenderer` · `S3StorageService` · preview-banner flow

## Problem

G2 walk: banner WebP render thành công (`rendered=true`) nhưng **không có ảnh chân dung giáo viên**, dù commit trước (9b2070ac) đã truyền `portraitUrls` thật vào preview.

Root cause (state-check qua log + env):
- Banner do **Playwright render TRONG container** `kitehub-branding`. `BannerHtmlComposer` nhúng portrait/logo `<img src="<presigned URL>">` y nguyên.
- Presigned URL ký theo `S3_PUBLIC_ENDPOINT=http://localhost:9100` (cho **browser** trên host máy).
- Từ **trong container**, `localhost:9100` = chính container (không phải MinIO host port-mapping) → Playwright fetch portrait **fail** → banner render thiếu ảnh.
- Internal endpoint `S3_ENDPOINT=http://kite-minio:9000` reachable từ container nhưng presigned URL **không** ký theo host này (signature bind Host header → string-replace host làm hỏng chữ ký).

Cùng lớp GAP-1149 (presigned host reachability) nhưng cho **Playwright in-container fetch** thay vì browser.

## Proposed Fix (đã ship)

`S3StorageService.inlineImageDataUri(url)`: fetch object bytes qua **internal s3Client** (`kite-minio:9000`, reachable trong container + prod) → base64 → trả `data:<contentType>;base64,...`. `BrandingJobV1Controller.previewBanner` map portrait + logo qua method này TRƯỚC khi compose → Playwright render inline, **không cần fetch network** → host-agnostic (đúng mọi env). Best-effort: lỗi/external URL → giữ URL gốc (degrade về behavior cũ).

## Acceptance Criteria

- [ ] Banner ở Bước 7 hiển thị ảnh chân dung giáo viên (browser verify) — cho tenant có PORTRAIT asset.
- [ ] Logo (nếu có) cũng render trong banner.
- [ ] Tenant không có portrait → fallback icon (📚) như cũ, không lỗi.

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10), follow-up commit 9b2070ac
- GAP-1134 (portrait upload step) · GAP-1135 (banner render wiring, phase-1.5) · GAP-1149 (assets presigned host, same class)
