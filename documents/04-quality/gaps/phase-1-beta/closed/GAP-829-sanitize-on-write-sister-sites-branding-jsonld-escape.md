---
id: GAP-829
title: Sanitize-on-write sister sites — Branding write-path + kiteclass JsonLd </script> escape
status: DONE
priority: P1
phase: phase-1-beta
domain: Mixed
created: 2026-06-01
closed: 2026-06-02
---

# GAP-829 — Sanitize-on-write sister sites (GAP-827 cross-flow sweep DEFER)

> Surfaced bởi GAP-827 cross-flow sweep (per `cross-flow-bug-class-sweep.md` §3). GAP-827 đóng landing write-path sanitize; sweep tìm 2 sister site cùng bug-class signature ("tenant free-text persisted → reused on non-auto-escape render surface") nhưng khác scope → DEFER theo `cross-flow-bug-class-sweep.md` §5.

## Problem

GAP-827 fix (`LandingPageContentSanitizer` + wire vào `LandingPageServiceImpl.updateLandingPage`) đóng landing write-path. Cross-flow sweep surfaced 2 sister site:

1. **`kiteclass-frontend/src/components/seo/JsonLd.tsx` thiếu `</script>` escape (FE-side, P1):**
   `kitehub-frontend` JsonLd có `escapeScriptContent()` (escape `</script>` + `<!--`) chống JSON-injection breakout. `kiteclass-frontend` JsonLd KHÔNG có defense này — chỉ `JSON.stringify`. Với GAP-827 backend sanitize-on-write strip `<`/markup ở source, vector này coi như đóng tại source, NHƯNG defense-in-depth FE-side vẫn nên match kitehub pattern (2 site cùng component, drift).

2. **`Branding` write-path (`BrandingMapper` → `Branding` entity, BE-side, P2):**
   `Branding.displayName`/`tagline`/`contactEmail` = tenant free-text persisted qua `BrandingMapper` (partial-update MapStruct, không sanitize). Hiện feed CSS vars + scalar JSX (auto-escaped) nên risk thấp; NHƯNG `getOrCreateDefault` copy `Branding.displayName` → `LandingPage.heroTitle` + `tagline` → landing — sanitize chỉ chạy khi tenant update landing, KHÔNG khi seed-from-branding. Branding write-path nên có cùng sanitize layer cho consistency.

## Acceptance Criteria

- [x] `kiteclass-frontend` JsonLd.tsx thêm `escapeScriptContent()` match kitehub pattern (escape `</script>` + `<!--`) — base `JsonLd` component (chỉ nơi `dangerouslySetInnerHTML`); `Organization`/`Course` delegate nên cover hết
- [x] `Branding` write-path sanitize tenant free-text — wire `LandingPageContentSanitizer.sanitizeText` vào `BrandingServiceImpl.updateBranding` cho `displayName`/`tagline`/`address` (email/phone/social URL đã `@Email`/`@Pattern`/`@Size`-constrained → no free-text surface, khớp note của chính sanitizer)
- [x] Verify Branding sanitize + VN diacritic roundtrip — `BrandingServiceTest.shouldSanitizeFreeTextAndPreserveVnDiacritics` chạy REAL `LandingPageContentSanitizerImpl` (no DB) qua service write-path: markup stripped + `Trần Thị Hồng`/`Học tiếng Anh`/`Lê Lợi` preserved (NFC). Sanitizer's own DB-grade roundtrip đã cover ở `LandingPageContentSanitizerTest`; reuse bean unchanged nên Branding-specific Testcontainers IT không cần thiết
- [x] Cross-flow re-sweep — `grep -rln dangerouslySetInnerHTML kiteclass-frontend/src` → CHỈ `JsonLd.tsx` (no other unsanitized tenant-text surface)

## Current State (verified 2026-06-02) — FIX SHIPPED

Branch `fix/GAP-829-sanitize-sister-sites-jsonld-branding`:
- FE: `kiteclass-frontend/src/components/seo/JsonLd.tsx` — thêm `escapeScriptContent()` (escape `</script` + `<!--`) áp vào base `JsonLd`. JSON roundtrip giữ nguyên (`\/` là valid JSON escape). Test `JsonLd.test.tsx` +1 case escape (6/6 pass).
- BE: `BrandingServiceImpl` — inject `LandingPageContentSanitizer` (optional, null-guarded), sanitize `displayName`/`tagline`/`address` sau `updateFromRequest`. Constructor +1 param → swept lone caller `BrandingServiceTest:60` (per `api-contract-change-caller-sweep.md`).
- Test: `BrandingServiceTest` +1 test (real sanitizer, markup strip + VN preserve); existing tests unaffected (identity for clean text). `./mvnw test -Dtest=BrandingServiceTest` exit 0.
- Verify: `pnpm --filter kiteclass-frontend build` exit 0 (60/60 static pages) + JsonLd vitest 6/6 + BE test exit 0.

## Related

- GAP-827 (parent) — landing input safety; this gap = sweep DEFER sites
- `cross-flow-bug-class-sweep.md` §5 — DEFER verdict requires follow-up gap (this file)
- `vn-localization-audit-checklist.md` §5 — sanitize must preserve VN diacritics (reuse GAP-827 sanitizer)
- GAP-815 (landing editor UI) — Branding + landing self-service input both should be sanitize-gated
- `api-contract-change-caller-sweep.md` — BrandingServiceImpl constructor +1 param → swept lone test caller

## Log

- **2026-06-02:** Closed both sweep DEFER sites. FE — `JsonLd.tsx` base component gets `escapeScriptContent()` (match kitehub pattern; JSON roundtrip preserved via `\/` escape) + escape test case. BE — `BrandingServiceImpl.updateBranding` sanitizes `displayName`/`tagline`/`address` via reused `LandingPageContentSanitizer` (optional, null-guarded); constructor +1 param swept lone caller `BrandingServiceTest:60` per `api-contract-change-caller-sweep.md`. AC #3 verified via `shouldSanitizeFreeTextAndPreserveVnDiacritics` running the REAL sanitizer through the service write-path (no DB) — markup stripped + VN diacritics preserved; the sanitizer bean's DB-grade roundtrip already covered by `LandingPageContentSanitizerTest`, so a duplicate Branding Testcontainers IT is unnecessary. Re-sweep confirmed `JsonLd.tsx` is the only `dangerouslySetInnerHTML` surface in kiteclass-frontend. Verify: BE `mvnw test -Dtest=BrandingServiceTest` exit 0 + FE `pnpm build` exit 0 (60/60 static) + JsonLd vitest 6/6.
- **2026-06-01:** Filed từ GAP-827 cross-flow sweep DEFER (per `cross-flow-bug-class-sweep.md` §5).
