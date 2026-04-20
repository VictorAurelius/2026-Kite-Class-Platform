---
name: GAP-171 — Rules docs review (ADR-like process)
description: Meta governance — rules docs change without formal review; add ADR-like template + reviewer + changelog
type: gap
---

# GAP-171: Rules Docs ADR-Like Review Process

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (meta — governance)
**Domain:** Process / Governance / `.claude/rules/`
**Found:** 2026-04-14 (output-review-mandate §4 Violation #2)
**Affects:** Every rule created/modified; project-wide convention quality

## Problem

`.claude/rules/*.md` files set project-wide conventions but have no formal review process. Changes to `meta-gap-priority.md`, `output-review-mandate.md`, `audit-to-gap-pipeline.md`, etc. land with single-author commits and no architectural review. Risk: rules drift, become inconsistent, or contradict each other.

Example: `output-review-mandate.md` itself was created without review.

## Root Cause

Meta governance without meta review — ironic. Rules are project's DNA but treated as low-ceremony doc edits.

## Proposed Fix

1. Create `.claude/rules/_rule-changelog-template.md` — mandatory changelog per rule file
2. Add to each rule frontmatter: `version`, `last-reviewed`, `reviewer-approver`
3. Lead + 1 dev review required for rule changes (enforced via CODEOWNERS or PR checklist)
4. `rule-review-checklist` skill covers:
   - Does rule conflict with existing rules?
   - Are enforcement mechanisms defined?
   - Is it testable / auditable?
   - Log entry in rule file's §Log section

## Acceptance Criteria

- [ ] Changelog template in `.claude/rules/`
- [ ] All existing rules backfilled with version + last-reviewed date
- [ ] CODEOWNERS or PR template enforces 2-reviewer minimum for `.claude/rules/**`
- [ ] `rule-review-checklist.md` skill exists
- [ ] At least 3 rule changes post-acceptance follow process

## Related

- Parent violation: output-review-mandate §4 #2
- Similar: GAP-172 (architecture ADR) — parallel governance
- Applies to: skill-conventions.md, meta-gap-priority.md, post-wave-audit-mandate.md, etc.
