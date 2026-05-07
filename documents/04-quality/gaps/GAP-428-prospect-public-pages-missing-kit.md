# GAP-428: Prospects / Public Pages Have No UI Kit Coverage

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend — KiteClass + KiteHub public marketing surface
**Found:** 2026-05-08 (UI Review /128 Wave 40 Bucket A milestone audit — production parity check)
**Affects:** All first-time visitors, prospects, and unauthenticated flows; conversion funnel top-of-funnel
**Related:** GAP-274 (Prospects persona kit — original gap for this coverage area)

---

## Problem

The Wave 40 Bucket A production parity check revealed that public/marketing pages in both `kiteclass-frontend` and `kitehub-frontend` have **no HTML kit prototype**, meaning they have no `/128 UI score baseline** and no anchor design specification:

| Production path | Status |
|-----------------|--------|
| `kiteclass-frontend/src/app/(public)/page.tsx` — KiteClass landing | ❌ No kit |
| `kiteclass-frontend/src/app/(public)/catalog/page.tsx` — Course catalog | ❌ No kit |
| `kitehub-frontend/src/app/(public)/pricing/page.tsx` — Pricing page | ❌ No kit |
| `kitehub-frontend/src/app/(public)/page.tsx` — KiteHub landing (if exists) | ❌ No kit |

Without kit coverage:
- These pages have never been through a formal `/128` review
- Conversion-critical copy (CTAs, pricing, social proof) is not anchored to a design spec
- Accessibility (WCAG AA) is unverified against a systematic rubric
- Brand consistency with authenticated kit surfaces is unguaranteed

---

## Context

GAP-274 (filed 2026-04-29, Wave UI Kits Round 3 follow-up) identified the Prospects persona as missing from kit coverage. This gap is the implementation task for GAP-274's findings, scoped specifically to:
1. KiteClass public landing + catalog
2. KiteHub pricing page

The consent banner (`kitehub-story-v2/consent-banner.html`) does cover one public-surface component (PDPL compliant) but does not cover the full page flows.

---

## Proposed Fix

Create a new kit folder or add screens to `kitehub-pro-v2` / a new `kiteclass-public` kit:

### Option A — New `kiteclass-public` kit (recommended)
```
ui_kits/kiteclass-public/
├── index.html          — kit index
├── README.md
├── styles.css (symlink to _shared)
└── screens/
    ├── landing.html            — Hero + value prop + CTA
    ├── catalog.html            — Course browse + filter
    ├── catalog-detail.html     — Course detail / enrollment CTA
    └── signup-flow.html        — Trial signup (+ beta invite if applicable)
```

### Option B — Add screens to `kitehub-pro-v2`
Add `pricing.html` screen (pricing table + tier comparison + CTA) to existing `kitehub-pro-v2` kit.

Both options should target ≥110/128 for public/conversion screens (higher bar than internal dashboard screens because conversion impact is direct).

---

## Acceptance Criteria

- [ ] At minimum 3 public-surface screens covered by HTML kit:
  - [ ] KiteClass landing / hero page
  - [ ] KiteHub pricing page (tier comparison table)
  - [ ] At least 1 catalog/browse screen
- [ ] Each screen has `Score self-estimate: ≥110/128` annotation
- [ ] WCAG AA self-measurement documented in HTML comment
- [ ] Kit README links to `documents/00-brd/personas-catalog.md` Prospects persona row
- [ ] Production parity check in next UI audit shows ✅ for these paths

---

## Related

- GAP-274 (Prospects persona kit — original gap; this is implementation scope)
- `documents/04-quality/audits/ui/2026-05-08-wave-40-milestone.md` — production parity table
- `kiteclass-frontend/src/app/(public)/` — production public routes
- `kitehub-frontend/src/app/(public)/pricing/` — production pricing route
- `documents/00-brd/personas-catalog.md` — Prospects persona definition

## Log

- **2026-05-08:** Filed from Wave 40 Bucket A production parity check. Public marketing pages have no kit coverage. This is the concrete implementation task for GAP-274 (Prospects persona). P1 priority because conversion funnel is the entry point for all beta tenants and v1.0 commercial launch.
