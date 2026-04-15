# GAP-074: AI-Generated Alt-Text for Accessibility (a11y)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** AI Branding / Accessibility / UX
**Detected:** 2026-04-14 (simulation-gap-finder on Wave 3 scope)
**Matrix cell:** End User × Daily Usage × C2 UX Accessibility

## Problem

Branding assets (logo, banner, hero image) được AI generate nhưng **không có alt-text**. Student / teacher dùng screen reader (NVDA, JAWS, VoiceOver) nghe:
- "Graphic" — không biết cái gì
- File name → random hash
- Skip → miss key brand context

WCAG 2.1 AA yêu cầu: all non-decorative images có meaningful alt-text.

Legal: Vietnam Cybersecurity Law + Law on Persons with Disabilities (2010) + ADA (nếu US học viên) → accessibility compliance.

## Evidence

- `BrandingResource` entity (Wave 2 GAP-007) không có `altText` field
- `kiteclass-frontend` rendering branding images: `<img src={url} alt={name} />` — alt là filename, không meaningful
- AI pipeline (GAP-008) không generate alt-text cho output
- No axe-core CI check on branding pages

## Proposed Fix

### 1. Add altText to BrandingResource

```java
@Entity
class BrandingResource {
  // ... existing
  @Column(columnDefinition = "TEXT")
  private String altText;   // Vietnamese + optional English

  @Column(length = 10)
  private String altTextLang;  // "vi" | "en"
}
```

### 2. AI pipeline generates alt-text

Sub-PR 3.5 (AI Agent workflow) adds `GenerateAltTextStep`:

```java
// After image generation, invoke multimodal AI to describe
String prompt = "Describe this branding image in Vietnamese, 10-20 words, for accessibility. " +
    "Context: school banner for {audience}, tone {tone}.";
String altText = aiClient.describeImage(imageUrl, prompt);
resource.setAltText(altText);
```

Fallback: if AI fails → use template-based alt:
- LOGO → "Logo của trường {tenantName}"
- BANNER → "Banner chào mừng học sinh trường {tenantName}"
- HERO → "Hình ảnh chủ đạo của trường {tenantName}"

### 3. FE enforce alt attribute

```tsx
<BrandedImage
  resource={logoResource}
  fallbackAlt={t('branding.logo-alt', { name: tenantName })}
/>
```

Linter rule (ESLint plugin jsx-a11y) enforces `alt` on all `<img>`.

### 4. Decorative marker

Some images truly decorative (pattern fills, dividers). Allow `alt=""` only when `resource.decorative=true`.

### 5. CI accessibility check

axe-core test on branding-rendering pages (GAP-044 synthetic monitoring ties in) — blocks PR if violations.

## Acceptance Criteria

- [ ] `alt_text` + `alt_text_lang` columns on `branding_resources`
- [ ] AI pipeline generates alt-text per asset (with fallback)
- [ ] FE components enforce alt (TypeScript types require it)
- [ ] axe-core CI check catches missing alt
- [ ] 3-layer docs: update `resource-classification/rules.md` with alt-text rule
- [ ] i18n: alt-text available in vi + en
- [ ] Unit test: missing alt-text fallback works
- [ ] Manual test with NVDA / VoiceOver

## Dependencies

- Wave 2 GAP-007 — extend entity
- Wave 3 GAP-008 — pipeline step
- GAP-044 (synthetic monitoring + feature flags) — axe-core CI integration

## Target Wave

**Wave 5 or Wave 7** — non-blocking for core pipeline but required before GA.

## Log

- 2026-04-14 — Detected via simulation-gap-finder (End User daily usage, a11y gap)
