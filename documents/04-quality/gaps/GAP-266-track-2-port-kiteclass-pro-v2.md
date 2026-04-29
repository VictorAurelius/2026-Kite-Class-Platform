# GAP-266: Track 2 Port — kiteclass-pro v2 → production Next.js

**Status:** 🔵 OPEN
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
