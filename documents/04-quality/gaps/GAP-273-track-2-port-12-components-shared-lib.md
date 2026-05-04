# GAP-273: Track 2 Port — 12 components → shared component library

**Status:** 🟡 PARTIAL — Phase 1 (ADR-024 + pnpm workspace + `@kite/shared-ui` scaffolding) SHIPPED 2026-04-30 via PR #713. Phase 2-6 (12 G* component port + ~10 D* dialogs) tracked under Track 2 umbrella wave plan: [`documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md`](../../../documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md). Phase 2 trigger: MVP-essential blockers from Wave 17 persona review findings, OR explicit user kick-off.
**Priority:** 🟡 P2 (UX growth — cross-cutting shared lib, blocking 7 kit gaps)
**Domain:** Frontend
**Found:** 2026-04-29
**Affects:** `kiteclass-frontend/src/components/` + `kitehub-frontend/src/components/` (or shared package)

## Problem

12 HTML component prototypes across R2 + R3 (G1..G12) are reusable cross-cutting primitives. Production code does not have these as React components yet. **Blocks 7 kit-port gaps** (GAP-266..272) which all import these components.

## Current State (verified 2026-04-29)

Components scattered across kits in HTML form. No shared React component lib in production. Existing FE component folders reuse shadcn primitives but lack the 12 domain-specific components.

## Proposed Fix

Port all 12 components to React/Next.js as shared library.

**Components to port:**

| Gap (HTML) | Component | States | Round | Notes |
|-----------|-----------|:------:|:-----:|-------|
| G1 | Bulk Import Drop-zone + Job Tracker | 5 | R3 | CSV upload + parse + partial-success summary |
| G2 | Attendance Roster (P/V/M/L) | 5 | R2 | Per-student toggle + save bar + sticky summary |
| G3 | Gradebook Entry Grid | 6 | R3 | VN 10pt scale + cell editing + bulk paste |
| G4 | Class Schedule Manager | 5 | R3 | Recurring rules + conflict warning + VN week-start Mon |
| G5 | Payment Method Selector | 6 | R2 | VNPay/MoMo/ZaloPay/Bank/Cash/QR |
| G6 | Invoice Detail | 6 | R2 | VN tax format + print-friendly |
| G7 | Parent Invite Flow | 6 | R2 | Email/token + Zalo OA share |
| G8 | Attendance Calendar (month) | 4 | R3 | Teacher month-view + 30-day streak |
| G9 | Instance Lifecycle Status | 6 | R3 | 6-state machine per `ai-branding-guidelines.md` §6 |
| G10 | Payment Status Timeline | 5 | R3 | VN currency 1.500.000đ format |
| G11 | Theme Live Preview | 5 | R3 | Light/dark morph + WCAG warning + auto-fix |
| G12 | Bulk Actions Bar | 6 | R2 | Sticky multi-select + Export CSV/Archive/Assign/Delete |

**Tech direction:**
- Decision needed: shared package vs per-app duplication
  - **Option A:** Create `packages/shared-ui/` workspace package, both frontends consume
  - **Option B:** Duplicate components into each frontend's `src/components/` (simpler, eventual divergence risk)
  - **Option C:** Server component + client component split where applicable
- Each component exports TypeScript types per `dossier/04-component-gaps.md` spec
- Storybook OR demo route for visual testing
- Reflexive WCAG coverage on G11 (theme preview must demonstrate fail + auto-fix)

## Acceptance Criteria

- [ ] All 12 components ported with TypeScript types
- [ ] Decision A/B/C documented + ADR if shared package
- [ ] Each component has spec.md mirror of `dossier/04-component-gaps.md` entry
- [ ] Storybook (or `/dev/components/` route) showcasing all states
- [ ] Each component reaches ≥105/128 in production usage (verify via UI review skill)
- [ ] G3 VN 10pt validation working + bulk paste from Excel
- [ ] G5 + G6 VN currency + tax format
- [ ] G9 state machine matches `ai-branding-guidelines.md` §6 transitions
- [ ] G11 WCAG fail demonstration + auto-suggested fixes (reflexive coverage)
- [ ] Vietnamese-only labels
- [ ] WCAG AA per component
- [ ] Unit tests per component (states + props edge cases)
- [ ] Visual regression baseline captured

## Related

- HTML prototypes: `ui_kits/components/G1..G12-*/`
- Dossier spec: `documents/02-architecture/design-system/dossier/04-component-gaps.md`
- Blocks: GAP-266 (owner uses ⌘K palette), GAP-267 (parent uses G7), GAP-268 (teacher uses G2/G3/G4/G8), GAP-269 (student uses G6/G8/G10), GAP-270 (kitehub-pro uses G9/G10/G11), GAP-271 (kitehub-admin uses G1/G3/G4/G8/G10), GAP-272 (wizard uses G11)

## Effort estimate

~2-3 weeks (12 components × spec + impl + tests + Storybook). Wave-pack candidate sliced into 3 buckets:
- **Bucket 1:** G1+G2+G3+G4 (4 components — KC teacher-facing)
- **Bucket 2:** G5+G6+G7+G8 (4 components — payment + invite + calendar)
- **Bucket 3:** G9+G10+G11+G12 (4 components — KH lifecycle + payment + theme + bulk)

**This gap should land FIRST in Track 2 sequence** because 7 kit gaps depend on it.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality. Shared lib decision (Option A/B/C) deferred to wave kickoff.
- **2026-04-30:** Layer 3 (詳細設計) blocker resolved — **ADR-024 PROPOSED** picks **Option A (pnpm workspace package `@kite/shared-ui`)**. Per `design-layer-coverage.md` §2.4 4-layer check applied to this gap. ADR ships separately for user review → ACCEPTED → wave-pack kickoff. Phase 1 (workspace bootstrap, ~1-2h) becomes prerequisite to Phases 2-5 (component port wave-pack, 3 buckets × 4 components each).
