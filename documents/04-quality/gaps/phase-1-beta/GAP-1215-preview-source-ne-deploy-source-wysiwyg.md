# GAP-1215: Preview-source ≠ deploy-source — WYSIWYG vỡ cố hữu

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-11 (branding-100 failure-mode audit #2)
**Affects:** `buildLandingPreviewHtml` (preview) vs landing render thật (TemplateRenderer)

## Problem

Preview wizard render bằng `buildLandingPreviewHtml` (HTML riêng) trong khi landing thật render bằng TemplateRenderer/sections — 2 code path → cái user duyệt ≠ cái sẽ lên trang. Drift cố hữu mỗi khi landing đổi (như toàn bộ wave landing-100 hôm nay: F-sections/hero khung/carousel — preview không biết).

## Proposed Fix

Preview dùng CHÍNH landing render path (iframe trỏ landing route với param preview/draft theme — `?tenant=` preview mode có sẵn) thay vì HTML builder riêng. Bucket D wave branding-100.

## Acceptance Criteria

- [ ] Preview = đúng render path landing thật (1 nguồn)
- [ ] Theme/section mới tự xuất hiện trong preview không cần sync tay

## Related

- Failure-mode #2; GAP-1213 (propagation), GAP-826/1210 (landing render vừa đổi)
