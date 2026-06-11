# GAP-1229: Favicon chain đứt 3 chỗ FE — không default, không upload UI, không render per-tenant (BE đã đủ)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend (kiteclass-frontend + kitehub-frontend) / Design System
**Found:** 2026-06-11 (user-flagged trong wave ui-kits-100: "favicon cũng cần fix nhỉ, tenant cũng phải upload được favicon nhỉ? hoặc mặc định của kiteclass")
**Affects:** Browser tab mọi tenant landing + dashboard (globe trắng); branding completeness per-tenant

## Problem

Design intent (ADR-009 branding package + DB `branding.favicon_url` + `ResourceType.FAVICON` V32) = tenant có favicon riêng. BE ĐÃ ĐỦ: `POST /api/v1/settings/branding/favicon` (authz GAP-1035 ✅, bucket GAP-1036 ✅), API client `uploadFavicon()` + type `faviconUrl` có sẵn. Nhưng chuỗi FE đứt 3 chỗ:

1. **Không có favicon mặc định** — `kiteclass-frontend/src/app/` KHÔNG có `favicon.ico`/`icon.png` (Next.js file convention); chỉ có PWA icons (`public/icons/icon-192/512`) trong manifest. **kitehub-frontend cũng thiếu**. Tab browser = globe trắng cả 2 app.
2. **Settings UI không có upload favicon** — `components/settings/branding-settings.tsx` chỉ có logo; `lib/api/branding.ts#uploadFavicon` **0 caller**.
3. **Landing không render favicon tenant** — `(public)/page.tsx` `generateMetadata` không có field `icons:` → tenant upload xong cũng không hiển thị; không có fallback default KiteClass.

Same-class cảnh báo: GAP-1204 (logo persist presigned MinIO URL hết hạn 7d) — favicon URL phải dùng cùng cơ chế durable đã fix cho logo, không lưu presigned.

## Design-first (per `frontend-standards.md` §3.1 Kit as Source of Truth — GAP-366, shipped Wave ui-kits-100 Bucket F)

Kit design TRƯỚC implementation — gắn vào wave ui-kits-100:
- **Bucket C** (landing-personal kit): spec favicon trong `<head>` landing per-tenant (faviconUrl → fallback default KiteClass) — annotation/comment spec trong kit
- **Bucket D** (wizard kit v3 per GAP-1212): affordance upload favicon trong bước Logo (cạnh logo upload, preview 16/32px, accept .ico/.png/.svg ≤200KB)

## Proposed Fix (implementation — PR code riêng sau kit)

1. Default favicon: thêm `src/app/icon.png` (+ `favicon.ico`) cho CẢ `kiteclass-frontend` (logo KiteClass) + `kitehub-frontend` (logo KiteHub) — Next.js tự serve
2. `(public)/page.tsx` `generateMetadata` thêm `icons: { icon: faviconUrl ?? '/icon.png' }` từ landing payload (BE đã trả faviconUrl trong branding package per ADR-009 — verify field có trong `LandingPageResponse`)
3. `branding-settings.tsx` thêm section upload favicon (gọi `uploadFavicon()` sẵn có) + preview + validate type/size
4. Favicon URL durable (không presigned) — theo cơ chế GAP-1204 đã fix cho logo

## Acceptance Criteria

- [ ] Kit spec: landing-personal head spec + wizard v3 favicon affordance (Bucket C/D wave ui-kits-100)
- [ ] 2 app có default favicon (tab hiển thị logo, không globe trắng) — cả localhost lẫn production build
- [ ] Tenant upload favicon qua Settings → tab landing đổi theo (browser walk per `g1-browser-walk-before-flip`)
- [ ] Chưa upload → fallback default KiteClass trên landing tenant
- [ ] Favicon URL không hết hạn sau 7d (durable, same cơ chế GAP-1204)

## Related

- ADR-009 (branding package), GAP-1035/1036 (BE upload DONE), GAP-1204 (presigned URL class), GAP-1212 (wizard kit v3 — design host), GAP-366 (kit-as-source-of-truth standard)
- Discovered in: Wave ui-kits-100 session 2026-06-11 (user-flagged)
