# GAP-266: Track 2 Port — kiteclass-pro v2 → production Next.js

**Status:** 🟡 PARTIAL — Wave 30 SHIPPED 2026-05-06: foundation + 4 page-clusters ported (PR #871 A foundation + #872 B classes/courses + #873 C students/teachers + #874 D billing/settings/branding). Remaining ACs (Storybook/demo route at `/overview` partial, ≥105/128 production verification, visual regression baseline, drag-drop persistence, E2E test, bundle size verify) → follow-up gaps GAP-266b/c/d.
**Priority:** 🟡 P2 (UX growth, design system fidelity)
**Domain:** Frontend
**Found:** 2026-04-29 (post-Round 3 user accept; per Round 2 wave plan §"Deferred separate track" + Round 3 §4)
**Affects:** `kiteclass-frontend/src/app/(dashboard)/` — owner dashboard routes

## Problem

HTML prototype `documents/02-architecture/design-system/ui_kits/kiteclass-pro-v2/` (avg 108.4/128, 10 screens, R2 PR #669) describes the target design but production code in `kiteclass-frontend` is the pre-Round-2 baseline (~73/128 R1 reference). Gap = production code does not reflect Round 2 design system.

## Root Cause

Wave UI Kits Round 2 was deliberately scoped as Track 1 (HTML prototypes only). Track 2 (production port) was deferred until user accepted prototype quality. User accepted 2026-04-29 (this gap's filing trigger).

## Current State (verified 2026-04-29)

```
kiteclass-frontend/src/app/(dashboard)/
├── admin/
├── attendance/
├── billing/
├── branding/
├── classes/
├── courses/
├── parent/
├── settings/
├── students/
├── teacher/
├── teachers/
└── layout.tsx
```

Owner dashboard routes EXIST (admin/, classes/, courses/, students/, teachers/, billing/, settings/) — port = redesign existing pages, not build from scratch.

## Proposed Fix

Port HTML prototype's 10 owner-dashboard screens to production React/Next.js code.

**Scope per HTML prototype** (`kiteclass-pro-v2/screens/*.html` + `_v1-baseline/` reference):
- Owner home dashboard (KPIs + sparklines)
- Class management (drag-drop reorder)
- Course catalog
- Student management (table + bulk actions)
- Teacher management
- Billing overview
- Settings (theme + branding gateway)
- ⌘K command palette (cross-page)
- Dark mode polish (smooth morph)
- Toast confetti (success animations)

**Tech direction:**
- Reuse existing routes; refactor page bodies to match HTML prototype design
- Extract shared primitives (sparkline, command palette, toast confetti) to `_shared/components/`
- Apply `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` tokens via CSS modules or Tailwind extension
- Maintain WCAG AA contrast ratios documented in HTML comments

## Acceptance Criteria

- [ ] All 10 owner-dashboard screens visually match HTML prototype (per-screen UI review ≥105/128)
- [ ] ⌘K command palette accessible from every dashboard page (keyboard shortcut)
- [ ] Dark mode parity (smooth color morph)
- [ ] Sparklines render real data from existing backend endpoints
- [ ] Drag-drop reorder persists to backend (existing PUT endpoints)
- [ ] No regression in existing parent/teacher routes (those have own port gaps)
- [ ] WCAG AA contrast preserved in production (axe DevTools clean)
- [ ] Bundle size <300KB First Load JS for owner dashboard pages
- [ ] E2E test: owner login → dashboard → manage class → drop class → see toast
- [ ] Visual regression baseline captured (`scripts/capture-screenshots.ts`)

## Related

- HTML prototype source: `documents/02-architecture/design-system/ui_kits/kiteclass-pro-v2/`
- Round 2 wave plan: `documents/03-planning/waves/wave-2026-04-29-ui-kits-round-2.md`
- Round 3 wave plan §4: `documents/03-planning/waves/wave-2026-04-29-ui-kits-round-3.md`
- Predecessor gap-DONE: GAP-265 (Tier 3 enforcement layer ensures landing parity for HTML kits)
- Sister Track 2 gaps: GAP-267 (parent), GAP-268 (teacher), GAP-269 (student), GAP-273 (12 components shared)

## Effort estimate

~1-2 weeks. Slice into sub-PRs by screen-cluster (home+navigation / class+course / student+teacher / billing+settings). Wave-pack candidate when ≥3 sub-PRs disjoint.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality (per Round 2 wave plan §"Deferred separate track" pattern + Round 3 §4 GAP-266..273 trigger condition).
- **2026-05-06 — Wave 30 Bucket A (PR #871):** Foundation primitives shipped. 21 tests pass. ThemeProvider (next-themes wrapper with `useDashboardTheme()`) + ⌘K CommandPalette (Radix Dialog, fuzzy match, ↑↓ nav) + KPICard + Sparkline (pure SVG, no charting deps) + canvas confetti (vanilla, respects `prefers-reduced-motion`). Scope adjustment: home page moved `(dashboard)/page.tsx` → `(dashboard)/overview/page.tsx` (Next.js route-group conflict với `(public)/page.tsx`). NO new npm deps. Status: PARTIAL.
- **2026-05-06 — Wave 30 Bucket B (PR #872):** Classes + Courses page-cluster shipped. 13 tests pass (643 total / 206 skipped). Tokens applied to classes + classes/[id] + courses + courses/[id]. DragDropList HTML5 DnD primitive (no new deps). G4 ClassScheduleManager (Wave 28) integration smoke test verified. Persistence call deferred to caller (parent owns `onReorder` → PUT). Status: PARTIAL.
- **2026-05-06 — Wave 30 Bucket C (PR #873):** Students + Teachers page-cluster shipped. 8 tests pass. G12 BulkActionsBar wired to students+teachers tables (4 actions: Xuất CSV / Lưu trữ / Phân lớp / Xóa; destructive Xóa via D1 ConfirmDialog identity). G1 BulkImportDropzone tab switcher in `/students/new` (presentational stub — real CSV parse + batch insert mutation = follow-up). students/[id]/page.tsx left unchanged (already correct, avoid gold-plating). Status: PARTIAL.
- **2026-05-06 — Wave 30 Bucket D (PR #874):** Billing + Settings + Branding page-cluster shipped. 10 new tests pass (640 total). G6 InvoiceDetail + formatVNCurrency (Wave 27) integrated in billing/[id]. G10 PaymentStatusTimeline (Wave 28) wired with KC Payment-derived events. G11 ThemePreview (Wave 29) added "Theme preview" tab in settings. Branding gateway page với CTA → `/branding/wizard` placeholder. Note: `PaymentTimelineState` not exported via `@kite/shared-ui` barrel — locally derived `PaymentStatusTimelineProps['state']`; small follow-up patch candidate. Status: PARTIAL — kit foundation + 4 clusters shipped; remaining: visual regression baseline (GAP-266b), E2E test (GAP-266c), bundle size verify <300KB First Load JS (GAP-266d).

- **2026-05-11 (Wave 53 Phase 4 milestone audit — UI /128 ❌ NOT DONE-eligible):** Bucket A static-analysis audit (PR #1106) avg 108.4/128 (range 102-115); 1 screen <105 (error-state UX). Carry-forward to existing GAP-429 umbrella (transient-state UX pattern: loading skeletons + empty states + error recovery) — coordinator confirmed NO new gap needed. Status stays 🟡 PARTIAL pending GAP-429 cluster closure.
