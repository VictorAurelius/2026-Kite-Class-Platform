# GAP-138: KiteClass Landing Hero — Duplicated "Chuyên nghiệp & Hiệu quả"

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / Copy / UX
**Found:** 2026-04-19 (UI audit catch-up — ui-review-2026-04-19.md §Top Findings #3, §New Issues U-4)
**Affects:** `kiteclass-frontend` `(public)/page.tsx` default landing render (no tenant override)

## Problem

`HeroSection.tsx` hardcodes `<span className="text-theme-primary">Chuyên nghiệp & Hiệu quả</span>` line 29 after rendering `{heroTitle}`. When the landing data fetcher returns default title `'Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả'` (page.tsx:45), the rendered output becomes:

```
Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả
Chuyên nghiệp & Hiệu quả        ← duplicated
```

This was originally flagged as **K-5** in the 2026-04-11 UI audit (`ui-audit-issues-2026-04-11.md`) and listed in 2026-04-16 follow-up as P1. Re-verification in this audit confirms the bug still ships — it was not picked up by #263 (empty-states PR) and no targeted fix PR followed.

**Evidence (code, 2026-04-19):**
```tsx
// kiteclass-frontend/src/components/sections/HeroSection.tsx:26-30
<h1 className="text-4xl md:text-6xl font-bold mb-6">
  {heroTitle || 'Trung tâm giáo dục'}
  <br />
  <span className="text-theme-primary">Chuyên nghiệp & Hiệu quả</span>
</h1>
```

```ts
// kiteclass-frontend/src/app/(public)/page.tsx:45
heroTitle: 'Quản lý Trung tâm Tiếng Anh Chuyên nghiệp & Hiệu quả',
```

Dynamic tenant data MAY override `heroTitle` and avoid duplication, but the default fallback + any tenant whose title happens to include "Chuyên nghiệp & Hiệu quả" (a very common Vietnamese education tagline) will see the bug.

## Root Cause

Hero was designed with split "static prefix / dynamic title / static accent" pattern, but the default fallback title in `getLandingPageData()` already embeds the accent phrase. Two cases not separated:
- Short title like `"Trung tâm ABC"` → appends accent naturally
- Long title embedding accent phrase → duplicates

## Proposed Fix

Two viable options:

**Option 1 (cleanest):** Move accent into the data contract and remove the hardcoded span.
- Add `heroAccent: string | null` to landing data type.
- Default fallback: `heroTitle: 'Quản lý Trung tâm Tiếng Anh'`, `heroAccent: 'Chuyên nghiệp & Hiệu quả'`.
- HeroSection renders the accent only if `heroAccent` is present.

**Option 2 (minimal):** Strip accent phrase from default fallback title.
- Change `page.tsx:45` default to `'Quản lý Trung tâm Tiếng Anh'`.
- Keep HeroSection as-is (hardcoded accent line always shows).
- Risk: tenants whose backend title contains "Chuyên nghiệp & Hiệu quả" still duplicate.

Recommended: **Option 1** — data-driven, avoids regression for any tenant.

## Acceptance Criteria

- [ ] `heroAccent` field added to landing data type (or equivalent restructuring)
- [ ] Default render shows each phrase exactly once
- [ ] Render with long tenant title (containing the accent phrase) shows no duplication
- [ ] Screenshot test: `landing-light-desktop.png` contains "Chuyên nghiệp & Hiệu quả" exactly once in OCR or DOM snapshot
- [ ] Unit test covers: short title, long title, empty title

## Related

- Audit: `documents/04-quality/audits/ui/ui-review-2026-04-19.md` §Top Findings #3, §New Issues U-4
- Previous audit: `documents/04-quality/audits/ui/ui-audit-issues-2026-04-11.md` K-5 (flagged P1, not fixed)
- Code: `kiteclass-frontend/src/components/sections/HeroSection.tsx:26-30`, `(public)/page.tsx:45`

## Log

- 2026-04-19 — Re-verified from 2026-04-11 baseline; still shipping. Originally K-5.
