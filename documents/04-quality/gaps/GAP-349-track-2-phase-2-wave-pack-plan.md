# GAP-349: Track 2 Phase 2 Wave-Pack Plan — 5 Priority Components

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (kicks off Track 2 production port — multi-week initiative)
**Domain:** Frontend / Design System
**Found:** 2026-05-04 (session audit — Track 2 umbrella has Phase outline but no wave-pack breakdown)
**Affects:** `packages/shared-ui/` (workspace package created in PR #713) + ports of 5 highest-priority components from `ui_kits/components/`

## Problem

Track 2 umbrella plan (`wave-track-2-ui-kits-port-umbrella.md`) lists Phase 2 as "5 highest-priority components × ~3-5 days" but has **no concrete wave-pack plan**: no per-bucket scope, no test infra plan, no dependency wiring, no agent prompts, no acceptance gate per component.

Without a wave-pack plan, kicking off Phase 2 means:
- Risk of serial single-PR work (anti-pattern per `feedback_wave_plan_before_serial_prs.md` — incident GAP-229: 90 min serial vs 30 min parallel)
- Risk of test infra drift (Vitest + Playwright config for `packages/shared-ui/` doesn't exist yet — must land in foundation, not per-component)
- Risk of CSS token mismatch (Round 2 HTML prototypes use `_shared/colors_and_type.css` Round 1 source; `packages/shared-ui/src/styles/tokens.css` is empty)

## Current State (verified 2026-05-04)

| Artifact | Status |
|---|---|
| `pnpm-workspace.yaml` + `packages/shared-ui/` scaffolding | ✅ DONE PR #713 (2026-04-30) |
| `packages/shared-ui/src/styles/tokens.css` | ⚠️ created but empty — needs Round 1 tokens migrated |
| Vitest config in shared-ui | ❌ not created |
| Playwright/visual-regression config in shared-ui | ❌ not created |
| 5 priority component HTML prototypes | ✅ live in `ui_kits/components/G2/G5/G6/G7/G12` (Wave 1 PR #671) |
| Round 3 kits external review | ⚠️ NOT done — see GAP-348 (BLOCKS THIS for student/admin kit ports, not for shared components) |

## Proposed Fix

Decompose Phase 2 into 1 foundation bucket + 4 component buckets, **5 buckets in 1 wave-pack** (parallel agents per `feedback_parallel_agent_strategy.md` rule #9 max 5 concurrent).

### Wave-pack structure

| Bucket | Scope | Output | Agent type | Duration |
|---|---|---|:---:|:---:|
| **A — Foundation** | Vitest + Playwright config in `packages/shared-ui/`, design tokens migration from `ui_kits/_shared/colors_and_type.css` to `packages/shared-ui/src/styles/tokens.css`, Storybook (or equivalent isolated viewer), test fixture template | shared-ui infra ready for components | general-purpose | ~90 min |
| **B — G2 Attendance Roster** | Port `ui_kits/components/G2-attendance-roster/` HTML to React component in `packages/shared-ui/src/components/AttendanceRoster/`. WCAG AA, dark mode, VN mock data, Vitest unit + Playwright visual | exported component | feature-tdd-agent | ~120 min |
| **C — G6 Invoice Detail** | Port `G6-invoice-detail/` → `InvoiceDetail/` | exported component | feature-tdd-agent | ~120 min |
| **D — G5 Payment Method Selector** | Port `G5-payment-method-selector/` → `PaymentMethodSelector/` | exported component | feature-tdd-agent | ~120 min |
| **E — G7 Parent Invite + D1 Confirm Dialog** | Port `G7-parent-invite/` → `ParentInvite/` + create generic `ConfirmDialog/` (D1 inferred from Round 3 modals catalog) | 2 exported components | feature-tdd-agent | ~150 min |

**Total wall-clock estimate:** ~150 min (max bucket E) + ~30 min coordinator merge + closure = ~3 hours for 5 components.

### Disjointness check

- Foundation (A) blocks B/C/D/E (test infra needed) → Sequence: A first, then B/C/D/E parallel
- B/C/D/E touch separate `packages/shared-ui/src/components/{Name}/` folders → no file overlap
- All 4 component buckets export through `packages/shared-ui/src/index.ts` → coordinator merges this file last

### Acceptance gate per component

Each bucket B-E must satisfy:
1. Component renders in isolated viewer matching prototype within ±5% pixel diff
2. Vitest unit tests cover props variants + state transitions (≥80% line coverage)
3. Playwright visual regression baseline captured for default + dark + 3 viewports
4. WCAG AA contrast measured + commented in source
5. VN mock data only (per `dossier/10-acceptance-criteria.md` rule)
6. Exported through `packages/shared-ui/src/index.ts`
7. Imported successfully in both `kiteclass-frontend` + `kitehub-frontend` test build
8. Bundle size impact < 5KB gzip per component (recorded in PR description)

## Acceptance Criteria

- [ ] Wave plan doc `documents/03-planning/waves/wave-2026-05-XX-track-2-phase-2-components.md` written (5-bucket wave-pack, agent prompts, file conflict matrix predicting 0 HARD)
- [ ] Foundation bucket A merged FIRST (test infra + tokens before component work)
- [ ] 5 components (G2 / G6 / G5 / G7 / D1) ported, each in own PR following acceptance gate above
- [ ] `packages/shared-ui/src/index.ts` exports all 5 components
- [ ] Both `kiteclass-frontend` + `kitehub-frontend` `pnpm build` strict-mode pass with shared-ui imported
- [ ] Closure PR updates `wave-track-2-ui-kits-port-umbrella.md` Phase 2 status to ✅ DONE
- [ ] `wave-history.jsonl` append (per `session-docs-check` Rule 15)
- [ ] Bundle size delta documented in closure (target <25KB gzip total for 5 components)

## Dependencies

- ✅ **GAP-273 Phase 1** DONE PR #713 (workspace + scaffold)
- ⚠️ **GAP-348 Round 3 review** does NOT block Phase 2 — components are kit-agnostic; only blocks Phase 4 student/admin kit ports
- ❌ **Production port for kits using these 5 components** (GAP-266 / GAP-267 / GAP-271) BLOCKED by this gap

## Why P1 (not P0)

P0 reserved for K-12 LEGAL trio Phase 1C + production code blockers. Track 2 Phase 2 is **MVP-essential** (per umbrella §"Trigger Phase 2 khi MVP-essential blockers từ Wave 17 review findings cần real components") but not currently blocking ship — P1 = kick within next 1-2 waves.

## Related

- Umbrella plan: `documents/03-planning/waves/wave-track-2-ui-kits-port-umbrella.md`
- ADR: `documents/02-architecture/adr/ADR-024-shared-ui-lib-strategy.md`
- HTML prototypes: `ui_kits/components/` (Wave 1 PR #671, avg 106.7/128)
- Sister: GAP-273 (12-component shared lib) — this gap fulfills Phase 2 of GAP-273
- Downstream: GAP-266 / GAP-267 / GAP-271 (kit ports consume these components)
- Methodology: `feedback_wave_pack_cross_gap_clustering.md`, `feedback_parallel_agent_strategy.md`

## Effort estimate

~3 hours wave-pack execution (5 parallel buckets) + ~2 hours plan PR + closure PR = **~half-day total** for Phase 2 deliverable. Phase 3 (7 more components + 10 dialogs) follows in 2-3 subsequent waves.

## Log

- **2026-05-04:** Filed after session audit found Track 2 umbrella has Phase 2 outline but no concrete wave-pack plan. Ports of 5 priority components (G2/G6/G5/G7/D1) need explicit bucket scoping + foundation infra plan before agents spawn — otherwise risk serial PRs anti-pattern (per GAP-229 incident 2026-04-26: 90 min serial vs 30 min parallel).
