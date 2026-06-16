# GAP-1437: generate-theme trả 500 (không 400) khi body rỗng/null — LogoAnalysis thiếu validation

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-6)
**Affects:** KH-6 AI branding wizard — `kitehub-branding AIBrandingController.java:200` `generateTheme(@Valid LogoAnalysis)`

## Problem
Discovered Phase-2 browser walk KH-6. `LogoAnalysis.java` có 0 annotation `@NotBlank`/`@NotNull` → null fields đi qua `@Valid` → NPE trong `themeGenerationService.generateThemeConfig` → HTTP 500. Verified: payload null-fields → 500; payload đúng shape → 200. Low-reach (FE luôn gửi đúng) nhưng 500 leak server error + contract gap.

## Proposed Fix
Thêm `@NotBlank` cho `primaryColor`/`theme`/`targetAudience` (+ các field bắt buộc) trên `LogoAnalysis` để malformed body trả 400 thay vì 500.

## Acceptance Criteria
- [ ] POST generate-theme với body null/rỗng → HTTP 400 (không 500)
- [ ] POST với body hợp lệ → HTTP 200 (không regress)

## Related
- Discovered in: Phase-2 browser walk (flow KH-6), 2026-06-16
