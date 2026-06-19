# GAP-665: Wave 98 `/legal/terms` 15-section wall-of-text restructure — TOC + anchor nav + mobile collapse

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend (UX information architecture)
**Found:** 2026-05-19 (Wave 98 post-closure audit suite — GAP-661 UI /128 audit)
**Affects:** `kitehub/kitehub-frontend/src/app/legal/terms/page.tsx` + `/legal/privacy` (likely same pattern) / anonymous prospect persona / PDPL compliance comprehension

## Problem

UI /128 audit sample 5 màn hình — `/legal/terms` scored **103/128 B+** (lowest trong sample). Cluster B Wave 98 polish khác đạt 110-116 (A range). Drilling down:

15-section TOS page render thành wall-of-text:
- KHÔNG có Table of Contents (TOC) ở đầu page
- KHÔNG có anchor links cho từng section (cannot deep-link `/legal/terms#data-processing`)
- KHÔNG có mobile collapse (375px viewport scroll dài ~15 màn hình)
- Reading time ước tính ~12 phút continuous scroll
- Persona Anonymous Prospect (Vy) sẽ bounce trước khi đọc xong → impact PDPL Art 9 consent validity (user phải đọc trước khi accept)

Carry-forward từ Wave 23 ship (TOS v1 disclaimer) — Wave 98 B4 chỉ touched i18n catalog (Vietnamese translation pass), KHÔNG structural redesign. Audit GAP-661 phát hiện gap structural còn open.

Per `user-manual-content-standard.md` §2 row 5 "TL;DR box + scannable structure" mandate (paired pattern cho user-facing docs).

## Root Cause

TOS legal text inherent dài (~3500 từ tiếng Việt + cross-references). Initial ship Wave 23 prioritize "ship MVP content" → defer UX polish. Wave 98 B4 i18n scope không bao gồm restructure (atomic-unique principle — i18n bucket KHÔNG nên creep sang IA redesign).

## Proposed Fix

### Step 1: Add Table of Contents component

```tsx
// app/legal/terms/page.tsx
<TableOfContents
  sections={[
    { id: 'introduction', label: '1. Giới thiệu' },
    { id: 'definitions', label: '2. Định nghĩa' },
    { id: 'account-registration', label: '3. Đăng ký tài khoản' },
    // ... 12 sections more
  ]}
  collapsible={true}     // mobile: collapsed by default
  sticky={true}          // desktop: sticky sidebar
/>
```

### Step 2: Wrap each section với anchor + collapsible (mobile)

```tsx
<TermsSection id="data-processing" title="9. Xử lý dữ liệu cá nhân" defaultExpanded={isDesktop}>
  <p>...PDPL Art 9 compliance text...</p>
</TermsSection>
```

### Step 3: Add reading-time estimate banner

```tsx
<ReadingTimeEstimate words={3500} />  // → "~12 phút đọc"
```

### Step 4: Add print-friendly CSS

```tsx
// @media print: expand all sections, hide TOC sidebar, optimize for A4
```

### Step 5: Verify /legal/privacy parallel page same pattern

Likely same wall-of-text issue. Apply same restructure component.

### Step 6: Accessibility check

- Heading hierarchy h1 → h2 (sections) → h3 (subsections) — verify no skip
- Keyboard navigation TOC → section anchor → expand → focus content
- Screen reader landmark roles

## Acceptance Criteria

- [ ] TOC component shipped + integrated `/legal/terms`
- [ ] All 15 sections wrapped với `<TermsSection id=...>` (anchor links)
- [ ] Mobile (375px viewport) collapse default + tap-to-expand
- [ ] Reading time estimate banner visible top
- [ ] `/legal/privacy` parallel restructure if same pattern
- [ ] Print CSS preserves all content readable on A4
- [ ] WCAG 2.5.5 touch target ≥44×44px (TOC items + collapse triggers)
- [ ] UI /128 audit refresh next wave: `/legal/terms` ≥115/128 A (vs current 103/128 B+)

## Related

- **Parent audit:** `documents/04-quality/audits/ui/2026-05-19-wave-98-cluster-b-sample.md`
- **Sibling gap:** GAP-541 (Wave 98 B4 i18n DONE — this gap covers structural follow-up)
- **Rule:** `user-manual-content-standard.md` §2 scannable structure
- **Compliance angle:** PDPL Art 9 — user phải đọc trước accept; UX UX-friendly enables informed consent
- **Carry-forward source:** Wave 23 initial TOS ship
