# GAP-1215: Preview-source ≠ deploy-source — WYSIWYG vỡ cố hữu

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-11 (branding-100 failure-mode audit #2)
**Affects:** `buildLandingPreviewHtml` (preview, RETIRED) vs landing render thật (TemplateRenderer)

## Problem

Preview wizard render bằng `buildLandingPreviewHtml` (HTML riêng) trong khi landing thật render bằng TemplateRenderer/sections — 2 code path → cái user duyệt ≠ cái sẽ lên trang. Drift cố hữu mỗi khi landing đổi (như toàn bộ wave landing-100 hôm nay: F-sections/hero khung/carousel — preview không biết).

## Proposed Fix

Preview dùng CHÍNH landing render path (iframe `src` trỏ kiteclass `/preview` route với draft-theme query params) thay vì HTML builder riêng. Bucket D wave branding-100.

## Current State (verified 2026-06-12 — branding-100 Bucket D)

Code-level fix SHIPPED:
- `buildLandingPreviewHtml.ts` (+ test) **RETIRED** (xóa) — không còn second render path.
- `Step6Preview.tsx` iframe đổi `srcDoc={previewHtml}` → `src={previewSrc}` (main + fullscreen). `previewSrc` = `useLandingPreviewUrl(...)` → `${NEXT_PUBLIC_KITECLASS_URL}/preview?primary=&secondary=&accent=&template=&orgName=&logo=&heroImage=&tenant=`.
- Kiteclass `(public)/preview/page.tsx` re-export `(public)/page.tsx` default → **CÙNG component** (TemplateRenderer + sections) ⇒ 1 render path, section/theme mới tự xuất hiện.
- `(public)/page.tsx` nhận thêm preview params `orgName`/`logo`/`heroImage` (additive, guarded) trên nền `tenant`/`template`/`primary`/`secondary`/`accent` đã có.
- CSP/iframe: kiteclass `/preview` được `frame-ancestors` cho KH origin (`:3001` dev + `kitehub.me`) + bỏ `X-Frame-Options: DENY` riêng route `/preview` (broad rule negative-lookahead exclude); `/` giữ DENY. KH thêm `frame-src` cho KC origin (`:3000` dev + `kiteclass.com`). KHÔNG wildcard.
- FE build local PASS cả 2 (kitehub-frontend + kiteclass-frontend `next build`); `/preview` route generated trong KC build.

## Acceptance Criteria

- [x] Preview = đúng render path landing thật (1 nguồn) — `/preview` re-export `(public)/page.tsx`, builder riêng đã xóa
- [x] Theme/section mới tự xuất hiện trong preview không cần sync tay — cùng `TemplateRenderer` + section components
- [ ] G2★ browser-walk: mở wizard `:3001` → Step preview → iframe render landing thật từ `:3000/preview` (cross-origin frame OK + banner/orgName/theme đúng) — **PENDING human walk** (cần 2 stack up; per `g1-browser-walk-before-flip.md`)

## Related

- Failure-mode #2; GAP-1213 (propagation), GAP-826/1210 (landing render vừa đổi); GAP-1231 (test un-skip cùng cụm); GAP-1245 (variant-pick → deploy palette wiring, follow-up Bucket C scope)

## Log

- **2026-06-12 (branding-100 Bucket D — Agent D):** Code-level fix shipped (xem Current State). Status → 🟡 PARTIAL (code AC done, G2★ browser-walk pending). Files: `kitehub-frontend/.../wizard/{Step6Preview.tsx, paletteVariants.ts, hooks/useLandingPreviewUrl.ts}` + retire `buildLandingPreviewHtml.ts`; `kiteclass-frontend/src/app/(public)/{page.tsx, preview/page.tsx}` + 2 `next.config.js` CSP. Multi-variant pick (GAP-1212 kit) shipped như preview affordance; deploy-of-non-base-variant defer GAP-1245.
