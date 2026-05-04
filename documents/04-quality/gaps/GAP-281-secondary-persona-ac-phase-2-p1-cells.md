# GAP-281: Secondary Persona AC Phase 2 — P1 Cells (4 cells deferred)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (business-logic tier — completes secondary persona coverage; not GA blocker since P0 cells via GAP-153 cover critical paths)
**Domain:** Business / Persona / Governance
**Found:** 2026-04-30 (filed at Wave Secondary-Persona-AC closure as Phase 2 follow-up per GAP-153 §AC)
**Affects:** Persona review completeness for non-K-12 contexts, parent engagement quality at P2/P3 scale, P1 Solo Teacher student journey

## Problem

GAP-153 Phase 1 (Wave 16, 2026-04-30) shipped 8 P0 secondary persona AC cells. Per the priority matrix, 4 P1 cells deferred — these aren't critical for K-12 deployment (P5 USER PRIORITY) but block full coverage of non-K-12 personas.

**4 deferred P1 cells:**

| Cell | Why P1 (not P0) |
|------|-----------------|
| `student-in-P1.md` | Solo Teacher tenant — student receives signup link directly from teacher (much simpler than P3/P5 flow), low complexity |
| `parent-in-P2.md` | Small Center — parent engagement at this scale lighter than P5 (no MOET reporting, no GVCN), Zalo communication primary |
| `parent-in-P3.md` | Medium Center — parent engagement at organized center (more formal than P2 but no legal mandate like P5) |
| `teacher-employee-in-P2.md` | Small Center — 1-2 hired teachers, simpler workflow than P3 (no commission engine complexity) |

## Proposed Fix

### Deliverable: 4 NEW AC docs in `documents/00-brd/persona-criteria/secondary/`

Each follows GAP-151 `_TEMPLATE.md` structure (6 categories: onboarding/ops/fin/comm/edge/exit). Reuse `docs-only-skeleton-agent.md` template variant (codified Wave 14, validated Wave 15+16).

**Recommended wave-pack structure (single coordinator OR 2 parallel agents × 2 docs each):**

- Agent A: `student-in-P1.md` + `parent-in-P2.md` (lighter scope, ~10-12 ACs each)
- Agent B: `parent-in-P3.md` + `teacher-employee-in-P2.md` (medium scope, ~12-15 ACs each)

OR single-coordinator since scope smaller than P0 cells (4 docs × ~12 ACs = ~48 ACs total, ~30-45 min wall-clock).

## Acceptance Criteria

- [ ] `documents/00-brd/persona-criteria/secondary/student-in-P1.md` (10-12 ACs)
- [ ] `documents/00-brd/persona-criteria/secondary/parent-in-P2.md` (12-15 ACs)
- [ ] `documents/00-brd/persona-criteria/secondary/parent-in-P3.md` (12-15 ACs)
- [ ] `documents/00-brd/persona-criteria/secondary/teacher-employee-in-P2.md` (12-15 ACs)
- [ ] Each doc follows GAP-151 `_TEMPLATE.md` structure
- [ ] Gap linkage populated (cross-reference GAP-051..064 + GAP-180..186 where applicable)
- [ ] `secondary/README.md` updated với Phase 2 status flip
- [ ] `personas-catalog.md` Secondary Personas table updated (Phase 2 entries marked DONE)
- [ ] ROADMAP entry

## Out of Scope

- **Tier 2/3 tenant contexts** (P4/P7/P8/P9/P10) — future gap
- **P2 cells** (accountant + receptionist + IT staff + parent rep) — tracked GAP-282
- **Execute the reviews** — GAP-152 Round 1 + future quarterly cadence

## Dependencies

- GAP-151 (template — already shipped Wave 15)
- GAP-153 (Phase 1 P0 cells — already shipped Wave 16, validates pattern at scale)

## Related

- GAP-151 Phase 1 — tenant AC framework
- GAP-153 Phase 1 — secondary persona P0 cells (8 docs SHIPPED 2026-04-30)
- GAP-282 — Phase 3 P2 cells (sister follow-up)
- GAP-152 — Round 1 review consumer (will eventually consume Phase 2+3 outputs in future review rounds)

## Log

- **2026-04-30** — Created at Wave Secondary-Persona-AC closure as deferred Phase 2 follow-up per GAP-153 §AC and §Out-of-Scope. Phase 2 timing: when scope bandwidth allows post-MVP soft launch (estimated 4-6 weeks after GAP-153 lands).
