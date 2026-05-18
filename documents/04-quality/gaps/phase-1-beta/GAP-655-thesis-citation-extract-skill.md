# GAP-655: Thesis citation-extract skill (Wave 98+ tooling)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META — automation for thesis citation workflow)
**Domain:** Meta (Skills + Tooling)
**Detected:** 2026-05-18 (Wave 97 closure orphan-cleanup — splits GAP-647 Step 3 deferred portion)
**Parent:** [GAP-647](phase-1-beta/GAP-647-thesis-bibliography-ieee.md) PARTIAL 50% Wave 97 (PR #1541)

## Current State (verified 2026-05-18)

| Piece | Path | Status |
|---|---|---|
| CITATION-STYLE.md (IEEE format) | `documents/08-thesis/references/CITATION-STYLE.md` | ✅ shipped Wave 97 (PR #1541) |
| bibliography.md (~30 refs seeded) | `documents/08-thesis/references/bibliography.md` | ✅ shipped Wave 97 (PR #1541) |
| Citation-extract skill | `.claude/skills/quality/citation-extract/SKILL.md` | ❌ missing |
| Auto-extract metadata workflow (WebFetch → IEEE format) | (skill internal) | ❌ pending |

## Problem

GAP-647 thesis bibliography Phase 1 (Step 1 IEEE style + Step 2 ~30 refs seed) shipped Wave 97 PR #1541 PARTIAL 50%. Step 3 (citation-extract skill) deferred without follow-up gap → orphan per `gap-done-discipline.md` §3 + `wave-closure-scope-completeness.md` §3. This file fixes orphan.

Step 3 scope:
- Skill auto-extract citation metadata từ WebFetch URL
- Format IEEE per `CITATION-STYLE.md`
- Append to matching `## Chapter N` section trong `bibliography.md`
- Return `[N]` ref number cho in-text usage

## Proposed Fix

### Step 1: Create skill file

`.claude/skills/quality/citation-extract/SKILL.md`:
- Trigger: "cite this URL", "add reference", "format citation IEEE", "thêm tham khảo"
- Input: URL (single or list)
- Workflow:
  1. WebFetch URL → extract metadata (title, author, year, source)
  2. Detect source type per CITATION-STYLE.md §"Bibliography entry formats" (web tech / academic / VN law / book / industry)
  3. Format IEEE according to template
  4. Compute next `[N]` từ `bibliography.md` (last N + 1, global increment)
  5. Determine matching `## Chapter N` section per user spec
  6. Append entry; return `[N]` ref number cho in-text usage

### Step 2: Skill reference docs

`.claude/skills/quality/citation-extract/reference/`:
- `entry-templates.md` — 5 entry-type templates copy từ CITATION-STYLE.md §"Bibliography entry formats"
- `chapter-assignment-heuristics.md` — guidance which chapter a ref belongs to (e.g., AWS docs → Chapter 4 Architecture; PDPL → Chapter 3 Requirements)

### Step 3: Self-test fixture

`.claude/skills/quality/citation-extract/test/`:
- Sample URL fixtures (5 different source types)
- Expected IEEE output snippets
- Verify skill produces correct format

### Step 4: PR template + memory

- Add row `output-review-mandate.md` §3 "Thesis citations" reviewer-checklist
- Optional memory `feedback_thesis_citation_workflow.md` (defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days)

### Step 5: Update GAP-647 reference

After this gap DONE → GAP-647 PARTIAL 50% → PARTIAL 100% (all 3 steps done) → flip DONE.

## Acceptance Criteria

- [ ] Skill file shipped `.claude/skills/quality/citation-extract/SKILL.md`
- [ ] Skill activated từ trigger phrases (manual or auto-detected)
- [ ] Skill extracts metadata + formats IEEE correctly per CITATION-STYLE.md
- [ ] Skill appends to matching Chapter section trong bibliography.md
- [ ] Skill returns `[N]` ref number cho in-text usage
- [ ] Self-test on 5 sample URLs PASS
- [ ] GAP-647 reference Status updated PARTIAL 50% → 100% → DONE flip

## Effort estimate

~0.5-1 wave bucket scope: 4-6 file creates + reference docs. Wave 98+ candidate as thesis tooling priority.

## Related

- **Parent gap:** [GAP-647](GAP-647-thesis-bibliography-ieee.md) PARTIAL 50% Wave 97 PR #1541
- **Style spec:** [CITATION-STYLE.md](../../08-thesis/references/CITATION-STYLE.md) — IEEE format definitions
- **Bibliography file:** [bibliography.md](../../08-thesis/references/bibliography.md) — append target
- **Sister gap:** [GAP-646](GAP-646-thesis-docx-pipeline.md) thesis DOCX pipeline (bibliography section injection consumes skill output)
- **Rules:**
  - `.claude/rules/skill-conventions.md` — skill folder structure mandate
  - `.claude/rules/rule-change-process.md` §6.5 Enforcement Parity Mandate

## Log

- **2026-05-18 (created):** Filed per `gap-done-discipline.md` §3 + `wave-closure-scope-completeness.md` §3 — orphan-cleanup for GAP-647 Step 3 deferred portion (Wave 97 closure compliance fix).
