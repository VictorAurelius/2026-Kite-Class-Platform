# GAP-273: Track 2 Port — 12 components → shared component library

**Status:** 🟡 PARTIAL — Phase 1 (ADR-024 + pnpm workspace + `@kite/shared-ui` scaffolding) SHIPPED 2026-04-30 PR #713. **Phase 2 (G2/G6/G5/G7) SHIPPED Wave 27 + Phase 3 (G3/G4/G8/G10/D1 + G1/G9/G11/G12) SHIPPED Waves 28/29 — total 12/12 G* + 1/N D* ported, all consumed by both frontends via `@kite/shared-ui` imports** (verified 2026-05-10 state-check: 23 test files × 307 tests PASS in `packages/shared-ui`). **Remaining AC**: D2..D10 dialogs (9 dialog ports), Storybook/demo route, ≥105/128 production-usage verification, visual regression baseline. Per `gap-done-discipline.md` §2 status stays PARTIAL until those AC verified. Track 2 Phase 4 kit ports (GAP-266..272) **fully unblocked from component-dependency standpoint** — kc-pro v2 (Wave 30) + kh-pro v2 (Wave 31) already PARTIAL via shared-ui consumption; 5 kits remain OPEN (GAP-267/268/269/271/272).
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
- **2026-05-06** — Wave 27 Bucket A: G2 Attendance Roster ported. 1/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 27 Bucket B: G6 Invoice Detail + VN currency utils ported. 2/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 27 Bucket C: G5 Payment Method Selector ported. 3/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 27 Bucket D: G7 Parent Invite Flow ported. 4/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 28 Bucket A: G3 Gradebook Entry Grid ported. 5/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 28 Bucket B: G4 Class Schedule Manager ported. 6/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 28 Bucket C: G8 Attendance Calendar ported. 7/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 28 Bucket D: G10 Payment Status Timeline (re-uses formatVNCurrency from G6) ported. 8/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 28 Bucket E: D1 Confirm Dialog Radix port (NEW @radix-ui/react-dialog workspace dep). 1/N D* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 29 Bucket A (PR #867): G1 Bulk Import Drop-zone + Job Tracker ported (31 tests = 11 component + 20 utils; CSV parse with UTF-8 BOM + VN names + quoted-field; phone `0\d{9,10}` + dd/mm/yyyy validation; root-container drag handlers; 6th synthetic 'error' state). 9/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 29 Bucket B (PR #864): G9 Instance Lifecycle Status ported (28 tests = 12 component + 16 utils; state machine matches `ai-branding-guidelines.md` §6 verbatim — NOT_STARTED/INITIALIZING/GENERATING/DEPLOYED/REGENERATING/FAILED; STATE_VISUAL lookup map (no switch cascades); FAILED→GENERATING retry path). 10/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 29 Bucket C (PR #865): G11 Theme Customization Live Preview ported (23 tests = 13 component + 10 utils; W3C WCAG 2.1 luminance verbatim; suggestFix deterministic AA-compliant; **reflexive coverage red→green cycle asserted** — component shows + auto-fixes its own contrast violations). 11/12 G* shipped. Status stays PARTIAL.
- **2026-05-06** — Wave 29 Bucket D (PR #866): G12 Bulk Actions Bar ported (15 tests; sticky `top|bottom|none`; cross-component re-use D1 ConfirmDialog identity preserved; closed enum `BulkAction` for TS exhaustiveness). **12/12 G* shipped** post-Wave-29. **Status stays 🟡 PARTIAL** because remaining ACs unchecked: Storybook/demo route, ≥105/128 production-usage verification, visual regression baseline, ~10 D* dialogs (only D1 shipped Wave 28 — D2..D10 deferred Wave 30+). Per `gap-done-discipline.md` §2: cannot flip DONE while AC checkboxes unchecked. Track 2 Phase 4 kit ports (GAP-266..272) all unblocked from G* dependency standpoint.
- **2026-05-10** — Doc-drift sync per `audit-to-gap-pipeline.md` §2.5/§2.7. Status header (line 3) đã stale: nói "Phase 2-6 trigger pending" trong khi Log entries (2026-05-06) ghi rõ Phase 2+3 đã ship qua Waves 27/28/29. State-check session 2026-05-10 trước khi spawn wave-pack mới đã catch drift này (rule §2.5 fired correctly — counterfactual: nếu skip state-check, sẽ duplicate ~3-4h work re-port 5 component đã có sẵn). Cập nhật Status line phản ánh chính xác hiện trạng + làm rõ remaining AC + làm rõ Phase 4 kit ports đã unblock dependency. Triggered by user request "Hoàn tất 8 cổng Track 2 (FE production)" → state-check session phát hiện umbrella plan + GAP-273 status line stale relative to actual code state. Wave 28 actually shipped 5 components (G3/G4/G8/G10/D1) per ADR-024 cluster reading — Bucket E là D1 không phải skip. Reviewer: solo-dev session 2026-05-10.
