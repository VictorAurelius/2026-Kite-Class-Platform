---
name: GAP-170 — Gap reports review template
description: Formal peer-review template + process for gap files; gap không được PLANNED cho đến khi peer-reviewed
type: gap
---

# GAP-170: Gap Reports Review Template

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (meta — governance)
**Domain:** Process / Governance
**Found:** 2026-04-14 (output-review-mandate §4 Violation #1)
**Affects:** Every gap file created; quality of audit-to-gap pipeline

## Problem

Per `.claude/rules/output-review-mandate.md` §4: "Gap reports — Ironic — gap queue has no gap review process!" Gap files are created by audit agents without peer review, leading to:
- False positives landing (GAP-107 false positive retracted 2026-04-20)
- Unclear acceptance criteria
- Missing dependencies / root cause
- Duplicate gaps created when existing ones would do

## Root Cause

No review standard or review process for gap files. `audit-to-gap-pipeline.md` §3 defines gap file template but no review gate before gap enters backlog.

## Proposed Fix

1. Add "Gap Review Template" section to `audit-to-gap-pipeline.md` with checklist:
   - [ ] Problem statement clear (end-user observable, not implementation detail)
   - [ ] Root cause analyzed or explicitly "needs investigation"
   - [ ] Acceptance criteria measurable (check-boxable)
   - [ ] Dependencies identified (blocked-by, blocks, related)
   - [ ] Priority level justified (P0/P1/P2/P3)
   - [ ] Domain tagged
   - [ ] Duplicate check performed (grep history)
   - [ ] No false-positive (source code reference verified)
2. New skill `.claude/skills/quality/gap-review-checklist.md` — enforces review before status can move 🔵 OPEN → 🟡 PLANNED
3. Audit-gate hook: warn if gap file created without review checkpoint in PR description

## Acceptance Criteria

- [ ] `audit-to-gap-pipeline.md` has Review Template section
- [ ] Skill `.claude/skills/quality/gap-review-checklist.md` exists
- [ ] Audit-gate hook updated
- [ ] 10 existing gap files retrospectively reviewed as proof of process

## Related

- Parent violation: `.claude/rules/output-review-mandate.md` §4 #1
- GAP-107 false positive (retracted 2026-04-20) — motivator
- GAP-150 (audit skill grep scope) — prevents similar FP
