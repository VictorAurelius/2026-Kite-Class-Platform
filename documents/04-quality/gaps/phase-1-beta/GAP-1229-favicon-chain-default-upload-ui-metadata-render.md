# GAP-1229: Favicon chain đứt 3 chỗ FE — không default, không upload UI, không render per-tenant (BE đã đủ)

**Status:** 🟡 PARTIAL 85% — code shipped Wave ui-kits-100 Bucket G (builds 3/3 PASS); residual = browser walk upload→tab-đổi (human G2, per feature-ship-runtime-walk-mandate §1)
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

- [x] Kit spec: landing-personal head spec (#2336 Bucket C) + wizard v3 favicon affordance (#2338 Bucket D)
- [x] 2 app có default favicon `src/app/icon.svg` (kite mark từ logo brand) — `next build` cả 2 app PASS
- [ ] Tenant upload favicon qua Settings → tab landing đổi theo (browser walk per `g1-browser-walk-before-flip`)
- [x] Chưa upload → fallback `/icon.svg` trong `generateMetadata icons:` (faviconUrl null-safe)
- [x] Favicon URL durable — BE đọc transient từ Branding + `assetUrlResolver.regenerate()` mỗi read (same GAP-1204 mechanism, không persist presigned)

## Related

- ADR-009 (branding package), GAP-1035/1036 (BE upload DONE), GAP-1204 (presigned URL class), GAP-1212 (wizard kit v3 — design host), GAP-366 (kit-as-source-of-truth standard)
- Discovered in: Wave ui-kits-100 session 2026-06-11 (user-flagged)

## Log

- **2026-06-11 (PARTIAL 85% — Wave ui-kits-100 Bucket G, coordinator inline):** Code shipped đủ chuỗi: (1) `icon.svg` default 2 app (kite mark); (2) BE `LandingPageResponse.faviconUrl` + enrich transient từ `Branding` per read + regenerate durable (GAP-1204 mechanism); (3) FE `generateMetadata icons:` fallback `/icon.svg`; (4) settings UI section Favicon + `useUploadFavicon` hook (nối `uploadFavicon()` 0-caller); (5) logotype 2 SVG asset Inter → Be Vietnam Pro (đóng DEFER E0). Verify: KC build + KH build + kiteclass-core compile 3/3 PASS + tsc clean. Residual AC "upload → tab đổi" = browser walk human G2 (stack local cần up) — blocker liệt kê trong wave reconciliation per gate §2.5 exception.
- **2026-06-11:** Merged PR #2339 (squash 8b76597c6) — Log ref bổ sung post-merge per audit-gate flag.
- **2026-06-12 (follow-up — user-flagged 2 điểm):** (1) ui_kits Pages demo CHƯA có favicon (0/244 trang có `rel="icon"`) → thêm `_shared/assets/favicon.svg` (kite mark) + insert depth-aware vào 237 trang kit; (2) production KC chưa tận dụng PNG sẵn có `public/icons/icon-192/512.png` ("KC" mark, PWA manifest) → fallback chain mới: tenant faviconUrl → [icon.svg + icons/icon-192.png] + apple-touch icon-192.png (reuse asset trên remote per user direction). Lưu ý brand: PNG "KC" chữ ≠ kite mark design-system — 2 mark đang song song (manifest vs favicon); nếu muốn thống nhất cần regenerate PNG từ kite SVG (quyết định brand riêng).
