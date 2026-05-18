# GAP-282: Secondary Persona AC Phase 3 — P2 Cells (8 cells deferred to post-MVP)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (business-logic tier — extends coverage to ancillary roles; not blocker for any GA milestone)
**Domain:** Business / Persona / Governance
**Found:** 2026-04-30 (filed at Wave Secondary-Persona-AC closure as Phase 3 follow-up per GAP-153 §AC)
**Affects:** Long-tail persona coverage for ancillary roles (accountant, receptionist, IT staff, parent rep) within Medium Center + K-12 School contexts

## Problem

GAP-153 Phase 1 shipped 8 P0 cells (Wave 16, 2026-04-30). GAP-281 covers Phase 2 P1 cells (4 docs). Remaining: 8 P2 cells covering ancillary tenant roles.

**8 deferred P2 cells (4 ancillary roles × 2 tenant contexts P3/P5):**

| Role | P3 Medium Center | P5 K-12 School |
|------|------------------|----------------|
| **Accountant** | `accountant-in-P3.md` | `accountant-in-P5.md` |
| **Receptionist** | `receptionist-in-P3.md` | `receptionist-in-P5.md` |
| **IT Staff** | `it-staff-in-P3.md` | `it-staff-in-P5.md` |
| **Parent Rep** | `parent-rep-in-P3.md` | `parent-rep-in-P5.md` |

Why P2 (not P0/P1):
- **Accountant** workflows largely subsumed under admin-in-P3/P5 financial AC (Phase 1) — incremental value of dedicated AC doc lower
- **Receptionist** front-desk workflows subset of admin (lễ tân role within multi-role admin)
- **IT Staff** technical workflows mostly self-discovered via system tooling (bulk import, integrations) — minimal AC value
- **Parent Rep** (Hội phụ huynh) coordination is event-driven, not daily workflow — AC less actionable

## Proposed Fix

### Deliverable: 8 NEW AC docs in `documents/00-brd/persona-criteria/secondary/`

Each follows GAP-151 `_TEMPLATE.md` structure. Lighter scope than P0 cells (~8-12 ACs per doc, mostly Daily Operations + Communication categories).

**Recommended wave-pack structure (4 agents × 2 docs each):**

- Agent A: `accountant-in-P3.md` + `accountant-in-P5.md` (financial deep-dive — payroll BHXH/BHYT/TNCN, VAT invoicing, MOET financial reporting per TT 107/2017)
- Agent B: `receptionist-in-P3.md` + `receptionist-in-P5.md` (enrollment intake, parent first-contact, scheduling)
- Agent C: `it-staff-in-P3.md` + `it-staff-in-P5.md` (bulk import, integrations, data export per PDPL)
- Agent D: `parent-rep-in-P3.md` + `parent-rep-in-P5.md` (event coordination, parent committee comm)

**Wave wall-clock estimate:** ~50-60 min (similar to Wave 16 for 8-doc parallel).

## Acceptance Criteria

- [ ] 8 NEW AC docs populated (8-12 ACs each, total ~70-90 ACs)
- [ ] Each follows GAP-151 `_TEMPLATE.md` structure
- [ ] Gap linkage populated (cross-references where applicable)
- [ ] `secondary/README.md` Phase 3 status flip
- [ ] `personas-catalog.md` Secondary Personas table updated (Phase 3 entries marked DONE)
- [ ] ROADMAP entry
- [ ] Cross-cut acknowledgment: where AC overlaps với admin-in-P3/P5 (Phase 1), document the boundary explicitly

## Out of Scope

- **Tier 2/3 tenant contexts** — future gap
- **Execute the reviews** — GAP-152 future quarterly rounds

## Dependencies

- GAP-151 (template — already shipped Wave 15)
- GAP-153 Phase 1 (already shipped Wave 16)
- GAP-281 Phase 2 (sister follow-up — order independent)

## Related

- GAP-151 — tenant AC framework
- GAP-153 — Phase 1 P0 cells (sibling — established pattern at scale)
- GAP-281 — Phase 2 P1 cells (sister follow-up)
- GAP-152 — review consumer (future quarterly reviews will consume Phase 3 outputs)

## Log

- **2026-04-30** — Created at Wave Secondary-Persona-AC closure as deferred Phase 3 follow-up per GAP-153 §AC and §Out-of-Scope. Phase 3 timing: post-MVP launch (estimated 8-12 weeks after GAP-153 lands), when ancillary persona coverage becomes a quality-bar concern rather than a feature-gap concern.
