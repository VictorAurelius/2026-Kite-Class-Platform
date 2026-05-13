# GAP-523: Audit skill rubric review wave — apply primacy + per-check to 6 audit skills (META)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 META (force-multiplier; recurrence prevention)
**Domain:** Meta / Skills
**Found:** 2026-05-13 (Wave 71c-meta-Phase-2 — security-audit incident generalization)
**Affects:** 6 audit skills + their score deliverables across audit history

## Problem

`security-audit` skill had two flaws Wave 71b surfaced:
1. Per-category rubric vague — sub-checks not enumerated → P0 bugs hide
2. Total score averaging hides per-check FAILs → "87/100" trustworthy-looking

PR #1278 fixed both for security-audit. But 6 OTHER audit skills likely have same failure mode:

| Skill | Score | Last milestone |
|---|---|---|
| `quality/quality-audit` | /110 | Wave 53 85/110 |
| `quality/ops-readiness-audit` | /100 | Wave 40 60/100 |
| `quality/performance-audit` | /100 | Wave 54 81/100 |
| `quality/api-contract-audit` | /100 | Wave 40 72/100 |
| `quality/business-logic-audit` | /100 | Wave 40 68/100 |
| `quality/ui-review` | /128 | Wave 53 111.7/128 |

Each skill needs to be reviewed:
1. Are sub-checks enumerated explicitly?
2. Is "bug-finding > scoring" primacy applied?
3. Can averaging hide P0 within a 10-pt or 20-pt category?

## Proposed Fix

Per-skill audit (one wave bucket each = 6 buckets total, parallelizable):

1. Read skill SKILL.md current rubric
2. Identify vague categories (allow averaging hide P0)
3. Per category: enumerate sub-checks OR bind to dedicated rule (like security-audit Cat 4 → auth-hardening)
4. Add §2 "Primacy: bug-finding > scoring" mirror of security-audit pattern
5. Worked self-test: re-audit current main with new rubric, surface bugs current score hid

Estimated 2-3 days work (parallelizable into 6 agents).

## Acceptance Criteria

- [ ] All 6 skills have "bug-finding > scoring" primacy section
- [ ] All 6 skills have per-check pass/fail rubric (no averaging within category)
- [ ] Worked self-test in each skill update PR shows ≥1 new finding surfaced

## Related

- Parent: PR #1278 (security-audit fix; this gap generalizes to 6 sister skills)
- Sibling: GAP-522 (security-audit remaining 4 categories)
- Rule: `meta-gap-priority.md` §3 Meta-P0 boost; `output-review-mandate.md` §3 multiple rows affected
- Wave candidate: 72 (post-Phase 1 BETA stabilize)
