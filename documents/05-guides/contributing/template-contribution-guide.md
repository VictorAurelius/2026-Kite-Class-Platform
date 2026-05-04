# Template Contribution Guide

**Audience:** Designers contributing SVG templates to the AI Branding template gallery
**Last verified:** 2026-04-26 (GAP-229 Phase 2.3)
**Related:** [`ai-branding-guidelines.md §8`](../../.claude/rules/ai-branding-guidelines.md), GAP-011 (template library curation)

---

## What you're contributing

Templates are **brand-agnostic SVG/HTML compositions** that the runtime composes with tenant-specific branding parameters (colors, logo, typography, headline copy). They sit in the `TEMPLATE` resource category (per ADR-005, BR-RES-001) and serve ~80% of branding requests — the goal is for AI generation to be the rare exception, not the default.

A good template:

- Renders identically across 320 px (mobile) → 3840 px (4K)
- Accepts arbitrary tenant brand colors without breaking contrast
- Holds a 50-character Vietnamese headline without text overflow
- Looks coherent as part of a "set" alongside its sibling templates

---

## The 5 review criteria (MANDATORY before merge)

Per GAP-011, every new template MUST pass these 5 criteria. Reviewers reject any submission missing even one.

### 1. Brand-agnostic (placeholders, not hardcoded)

```xml
<!-- ✅ GOOD — uses CSS vars resolved at runtime -->
<rect fill="var(--brand-primary)" />
<text font-family="var(--brand-font-heading)">{{HEADLINE}}</text>

<!-- ❌ BAD — hardcoded values -->
<rect fill="#1a73e8" />
<text font-family="Roboto">Welcome to ABC School</text>
```

Required placeholder names (must use exactly these):

| Placeholder | Source | Example |
|-------------|--------|---------|
| `var(--brand-primary)` | `Branding.primaryColor` | `#1a73e8` |
| `var(--brand-secondary)` | `Branding.secondaryColor` | `#fbbc04` |
| `var(--brand-accent)` | `Branding.accentColor` | `#10B981` |
| `var(--brand-font-heading)` | Selected typography theme | `Roboto Slab` |
| `var(--brand-font-body)` | Same | `Inter` |
| `{{HEADLINE}}` | Tenant's display name + tagline (50 char max) | `"Trường ABC — Vươn tới tương lai"` |
| `{{LOGO_URL}}` | Uploaded or generated | `https://cdn/.../logo.svg` |
| `{{ORG_NAME}}` | `Branding.displayName` | `Trường ABC` |

### 2. WCAG AA contrast (≥ 4.5:1)

Every text-on-background pair MUST achieve 4.5:1 contrast for body text, 3:1 for large text (≥ 18pt or bold ≥ 14pt).

How to verify before submitting:

1. Pick the **worst-case** color combination your template might produce (e.g. `--brand-primary` vs `--brand-secondary` if both can be light or dark)
2. Test with: bright + bright, dark + dark, complementary edge cases
3. Use [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/) or `npx @axe-core/cli` against rendered SVG

If your design depends on contrast that only works for specific brand-color combos, add a **fallback rule** in the template:

```xml
<!-- Auto-pick text color based on background luminance -->
<text fill="var(--brand-primary-text-on-bg)">
```

(`--brand-primary-text-on-bg` is computed by the runtime from primary's HSL lightness — black if L > 50, white otherwise.)

> **Status:** real WCAG measurement automation tracked in GAP-226 (Wave 8+). Until then, manual verification is mandatory per this checklist.

### 3. Responsive (320 px → 3840 px)

Use `viewBox` + relative units, never fixed pixel dimensions:

```xml
<!-- ✅ GOOD — scales -->
<svg viewBox="0 0 1200 600" preserveAspectRatio="xMidYMid meet">
  <text x="50%" y="50%" font-size="6vw" text-anchor="middle">{{HEADLINE}}</text>
</svg>

<!-- ❌ BAD — fixed sizes break on small screens -->
<svg width="1200" height="600">
  <text x="600" y="300" font-size="48px">{{HEADLINE}}</text>
</svg>
```

Test viewports (must look acceptable in all):

| Width | Common device | What to check |
|-------|---------------|---------------|
| 320 px | small phone | text not clipped, logo visible |
| 768 px | tablet portrait | layout doesn't break |
| 1280 px | laptop | primary use case |
| 1920 px | desktop | no awkward whitespace |
| 3840 px | 4K display | not pixelated, text legible |

### 4. Text safety (50-char Vietnamese headline, no overflow)

Vietnamese text has diacritics that increase visual height. Test with the canonical worst-case:

```
"Trường THCS Nguyễn Bỉnh Khiêm — Vì tương lai"
```

(50 characters, multiple diacritics, mixed-case.)

Acceptable outcomes:
- Text fits on 1 line at default size, OR
- Text wraps to 2 lines without colliding with surrounding elements, OR
- Text auto-shrinks via SVG `textLength` + `lengthAdjust="spacingAndGlyphs"`

Unacceptable:
- Text overflows container
- Text overlaps logo / decorations
- Diacritics get clipped at top edge

### 5. Brand-family consistency

Templates ship in **families** (e.g. "Education Modern" has a hero, banner, dashboard header, email header that share a visual language). All siblings in a family MUST share:

- Same color palette ratios (e.g. all use 60% primary / 30% secondary / 10% accent)
- Same typography hierarchy
- Same iconography style (filled vs outlined, rounded vs sharp)
- Same spatial rhythm (grid system, spacing scale)

Reviewer will check by composing all family members side-by-side. A "lone wolf" template that doesn't match its siblings gets rejected even if technically correct.

---

## File structure

Each template is a directory under `kiteclass/kiteclass-core/src/main/resources/templates/branding/`:

```
templates/branding/
└── edu-modern-3/                    ← family slug
    ├── manifest.json                 ← metadata (see below)
    ├── hero.svg                      ← composition file
    ├── banner.svg
    ├── dashboard-header.svg
    ├── email-header.svg
    └── preview.png                   ← 1200×600 thumbnail for wizard Step 5
```

`manifest.json`:

```json
{
  "id": "edu-modern-3",
  "name": "Education Modern — Variant 3",
  "category": "education",
  "audience": ["K-12", "center"],
  "tone": ["friendly", "professional"],
  "active": true,
  "designer": "Your Name <email@kitehub.vn>",
  "previewUrl": "preview.png",
  "compositions": ["hero", "banner", "dashboard-header", "email-header"]
}
```

Fields:

- `audience` / `tone` — drives template selection in `PickTemplateStep`. List the combinations your template handles well; a wider list = more wizard hits but reviewer scrutiny is harder.
- `category` — `education`, `business`, `general` (per AIB-11 in `documents/01-business/kitehub/ai-branding/rules.md`)
- `active: false` — submitted but not yet reviewer-approved → invisible in wizard

---

## Submit workflow

1. **Branch:** `template/{family-slug}` (e.g. `template/edu-modern-4`)
2. **Add files** under `templates/branding/{family-slug}/`
3. **Local preview:** run `mvn -pl kiteclass-core spring-boot:run` and visit `http://localhost:8080/dev-tools/template-preview?id={family-slug}` (dev profile only)
4. **Self-review against the 5 criteria above** — fix before requesting review
5. **PR title:** `template: add {family-slug}` — body must include:
   - Preview screenshots at 320 / 768 / 1920 px
   - Color-swatch grid showing 3 worst-case brand combos with WCAG ratios
   - 50-char VN headline rendered
   - Family consistency proof (compose alongside siblings)
6. **Reviewers:** designer-lead + at least one engineer (for SVG correctness + perf budget — keep file under 50 KB raw / 10 KB gzipped per file)

---

## Commit checklist

Copy this into your PR description:

```
- [ ] manifest.json present with all required fields
- [ ] All composition files use approved CSS-var placeholders (no hardcoded colors/fonts)
- [ ] Tested at 320 / 768 / 1280 / 1920 / 3840 px viewports
- [ ] Worst-case brand-color combo passes WCAG AA (≥ 4.5:1 body, ≥ 3:1 large)
- [ ] 50-char Vietnamese headline ("Trường THCS Nguyễn Bỉnh Khiêm — Vì tương lai") fits / wraps cleanly
- [ ] All family siblings share palette ratio + typography + iconography style
- [ ] preview.png at 1200×600, < 200 KB
- [ ] Each composition file < 50 KB raw / < 10 KB gzipped
- [ ] No external network references in SVG (no <use href="https://..."/>)
- [ ] Validates as well-formed XML / parsed by `lxml`
- [ ] Designer + engineer have signed off in PR review
```

---

## Anti-patterns (will reject)

| ❌ Don't | ✅ Do |
|---------|------|
| Hardcode `#1a73e8` because "blue is fine for now" | Always use `var(--brand-primary)` |
| Embed font files inline (bloats SVG) | Reference Google Fonts / system stack via CSS var |
| Use `<image href="https://..."/>` to external assets | Inline only; assets ship in repo |
| Submit one composition (e.g. just `hero.svg`) | Templates ship as families — minimum 4 compositions |
| Set `active: true` on first PR | Reviewer flips to `true` after approval |
| Skip the WCAG check because "it looks fine to me" | Run a contrast checker — eyeballing fails for 8% of users |
| Hand-tweak diacritic positions | Use proper SVG text — don't outline-to-paths Vietnamese text (kills accessibility + SEO + i18n) |
| Reuse another family's `id` | Each family-slug must be unique across the gallery |
| Embed JavaScript in SVG (`<script>`) | Static markup only — security policy strips scripts at upload |

---

## Working with the runtime

When `PickTemplateStep` selects your template (matching `audience`+`tone`), it calls:

```java
TemplateResourceHandler.handle(request, context)
```

The handler:
1. Loads `manifest.json` + composition files from classpath
2. Substitutes CSS-var placeholders with tenant's `Branding` values
3. Substitutes `{{...}}` tokens (HEADLINE, LOGO_URL, ORG_NAME)
4. Returns rendered composition as the resource (cached server-side per BR-PKG-003)

Your template files are loaded **as-is** — no preprocessing, no transformation. What you write is what ships. Validate accordingly.

---

## Promotion path (testing → production)

| Stage | `active` flag | Visible in | Trigger |
|-------|:-------------:|------------|---------|
| Submitted | `false` | dev preview only | PR opened |
| Approved | `true` (post-merge) | wizard Step 5 for matching audience+tone | Merge to main |
| Featured | `featured: true` (separate flag) | top of preview grid | Designer-lead promotes after 2-week observation |
| Retired | `active: false` (do NOT delete files) | hidden from wizard but renders for tenants who still use it | Set when superseded; never break existing tenants |

---

## Related

- Real handler: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/branding/handler/TemplateResourceHandler.java`
- Classifier: `TemplateMatchClassifier` (decides when YOUR template wins)
- Rules: `documents/01-business/kitehub/ai-branding/rules.md` BR-RES-002, AIB-10..13
- Architecture: `documents/02-architecture/ai-branding-v2-redesign.md` §Resource Categories
- Quality gate: `InstanceQualityReviewer` runs `LogoPlacementQualityCheck` against rendered output — your template's logo placeholder positioning is what gets graded
- ai-branding-guidelines.md §8 (Template Creation Rules) — original 5-criteria source
