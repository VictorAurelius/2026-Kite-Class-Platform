---
id: GAP-827
title: Landing input safety — sanitize-on-write + heroImageUrl allowlist + JSONB sanitize + length cap
status: DONE
priority: P0
phase: phase-1-beta
domain: Backend
created: 2026-06-01
---

# GAP-827 — Landing input safety (sanitize + validate write-path)

> Surfaced bởi outside-in failure-mode audit (2026-06-01, 1/3 agent landing-input state-check). Write-path nhận tenant input (text + banner) hiện **0 sanitization** + heroImageUrl **0 scheme/host allowlist** → P0 stored-XSS + injection risk. Phải đóng TRƯỚC khi mở editor self-service (GAP-815) cho tenant nhập.

## Problem

`UpdateLandingPageRequest` → `LandingPageMapper.updateEntity` (MapStruct copy thẳng, không sanitize) → entity. Findings empirical (failure-mode agent):

- **P0 sanitize-on-write absent:** text fields lưu raw. React JSX auto-escape che XSS ở render `.tsx`, NHƯNG text được reuse ở surface KHÔNG auto-escape: `JsonLd.tsx` (`dangerouslySetInnerHTML`!), email template, PDF cert, `<meta>` tag → stored-XSS rò. Có `SvgSanitizer` trong codebase nhưng KHÔNG wire vào landing write-path.
- **P0 heroImageUrl scheme/host:** chấp nhận URL bất kỳ → `javascript:` / off-origin `.svg` chứa script / `.html` đổi đuôi `.png`.
- **P0 JSONB raw:** `teachers/testimonials/faqs/programs/pricingTiers/stats` = `JsonNode` thô, 0 validation nội dung — lỗ hổng lớn nhất; phải audit mọi FE render JSONB có `dangerouslySetInnerHTML`.
- **P1 unbounded length:** `teacherBio` / `aboutText` / `address` KHÔNG có `@Size` → DB/cache bloat (endpoint public-cached!).
- **P1 CSS resilience:** token đơn dài (200 ký tự không space) vỡ hero dù có `text-wrap:balance`; banner sai tỉ lệ méo.

## Acceptance Criteria

- [x] **Sanitize-on-write** mọi text field: Jsoup `Safelist.none()` trong service (`LandingPageContentSanitizer` wired vào `LandingPageServiceImpl.updateLandingPage` SAU MapStruct, TRƯỚC persist)
- [x] **heroImageUrl** host allowlist (`LandingPageSafetyProperties.allowedImageHosts`, config `landing.safety.allowed-image-hosts`) — chặn off-origin + non-https + `javascript:`/`data:`; throws `ValidationException` → HTTP 400. (Dùng config-driven `validateImageUrl` thay `@Pattern` vì host khác theo env — MinIO dev / CDN prod)
- [x] **JSONB string values** sanitize recursive (`sanitizeJson` walk object values + array elements, giữ keys + number/boolean). FE `dangerouslySetInnerHTML` audit: kiteclass `JsonLd.tsx` chỉ render scalar props (auto JSON.stringify) — backend strip `<` đóng vector tại source; FE `</script>` escape parity → GAP-829 DEFER.
- [x] **`@Size` cap** `teacherBio` 2000 / `aboutText` 5000 / `address` 500 trên entity + DTO (heroTitle/subtitle đã có `@Size` 200/500 sẵn)
- [ ] **CSS:** `overflow-wrap:anywhere` h1 + `aspect-ratio` + scrim — FE rendering concern, OUT OF SCOPE backend gap → tracked GAP-826 (multi-banner) + GAP-828 (conversion UI)
- [x] **NFC normalize** dấu tiếng Việt on-write (`Normalizer.Form.NFC`; Jsoup UTF-8 charset giữ diacritic raw, KHÔNG escape thành entity — Wave 106 GAP-764 class)
- [x] Cross-flow sweep per `cross-flow-bug-class-sweep.md` — 2 sister site (Branding write-path + kiteclass JsonLd escape) DEFER → GAP-829 (sweep evidence dưới)
- [x] IT verify: XSS payload mỗi field → stripped + VN diacritic JSONB roundtrip preserved trên real Postgres (`LandingPageSanitizePostgresIT` 3 IT PASS); 25 unit test `LandingPageContentSanitizerTest`

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** tenant free-text persisted via setter/MapStruct without sanitize → reused on non-auto-escape render surface (`dangerouslySetInnerHTML` / email / PDF / `<meta>`).

**Grep commands run:**
- `grep -rln "dangerouslySetInnerHTML" kiteclass/kiteclass-frontend/src kitehub/kitehub-frontend/src` (10 sites)
- `grep -rln "NullValuePropertyMappingStrategy|@MappingTarget" .../mapper/` (11 partial-update mappers)

**Sites found + verdict:**

| # | Site | Verdict | Reason |
|---|---|---|---|
| 1 | `LandingPageServiceImpl.updateLandingPage` (landing write-path) | **FIX** (this PR) | Root site — sanitize wired |
| 2 | `kiteclass JsonLd.tsx` (no `</script>` escape; kitehub has) | **DEFER** GAP-829 | Renders scalar props only; backend sanitize closes source; FE parity = defense-in-depth |
| 3 | `Branding` write-path (`BrandingMapper` displayName/tagline) | **DEFER** GAP-829 | Feeds CSS vars + scalar JSX (auto-escaped); landing copy-from-branding now sanitized at landing-write |
| 4 | blog `[slug]/page.tsx` + beta-status `contentHtml` | **EXEMPT** | Static MDX/markdown content, NOT tenant free-text write-path |
| 5 | branding wizard `TemplateGrid/Fullscreen` dangerouslySet | **EXEMPT** | Renders system template SVG (already `SvgSanitizer`-gated), not tenant landing text |
| 6 | help `[slug]/page.tsx` (4 personas) | **EXEMPT** | Static help-doc MDX, not tenant write-path |

**Decision:** FIXED 1 (landing root); DEFERRED 2 → GAP-829 (filed same PR per §5); EXEMPT 3 (documented).

## Related

- Outside-in failure-mode audit 2026-06-01 (3-agent landing-input state-check)
- `cross-flow-bug-class-sweep.md` — sanitize = sister bug class; sweep all tenant-text entry points
- GAP-815 (editor UI) — safety-gate phải đóng TRƯỚC khi mở self-service input
- GAP-826 (multi-banner) + GAP-828 (conversion scope) — sister landing-input gaps
- GAP-829 (sweep DEFER) — Branding write-path sanitize + kiteclass JsonLd `</script>` escape
- ADR-037 (AI branding stack) — AI-gen text cũng phải qua sanitize + prompt constraint (no fabricated stats)

## Log

- **2026-06-02** DONE. State-check confirmed symptom: `LandingPageServiceImpl.updateLandingPage` → `LandingPageMapper.updateEntity` MapStruct copy-through, 0 sanitize; `heroImageUrl` chỉ `@Size`, 0 scheme/host check; JSONB `teachers/testimonials/...` raw `JsonNode`; `teacherBio/aboutText/address` `columnDefinition=TEXT` unbounded. (Note: gap mô tả `kitehub-branding` domain nhưng code thực ở `kiteclass-core/module/marketing` — đã state-check confirm.) Fix: new `LandingPageContentSanitizer` (Jsoup `Safelist.none()` + `Normalizer.Form.NFC` preserve VN diacritic + recursive JSONB sanitize + `validateImageUrl` host-allowlist) wired post-MapStruct pre-persist; `LandingPageSafetyProperties` (config `landing.safety.allowed-image-hosts`) + `MarketingConfiguration` `@EnableConfigurationProperties`; `@Size` caps entity+DTO; i18n keys (vi+en). Verify: 25 unit + 3 Testcontainers IT (VN diacritic JSONB roundtrip on real Postgres) PASS; `LandingPageServiceTest` caller sweep (added sanitizer mock + verify); strict-warnings compile clean. Cross-flow sweep 6 sites (1 FIX / 2 DEFER GAP-829 / 3 EXEMPT). Files: `LandingPageContentSanitizer[Impl]` + `LandingPageSafetyProperties` + `MarketingConfiguration` + `LandingPageServiceImpl` + entity/DTO `@Size` + `messages[_vi].properties` + `application.yml` + 2 tests.
