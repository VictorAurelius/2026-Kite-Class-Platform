# Persona Review Reports

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md), [`.claude/rules/output-review-mandate.md`](../../../.claude/rules/output-review-mandate.md) §3

Review reports produced by role-playing each persona end-to-end against the system, scoring every Acceptance Criterion (AC) PASS / PARTIAL / FAIL with concrete evidence.

**Audience:** PM, business stakeholders, GA readiness reviewers, future audit cycle. Closes the gap between "we have AC docs" (GAP-151 / GAP-153) and "we know what works for whom."

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `P{N}-{name}-round-{R}-YYYY-MM-DD.md` | One review report per (persona × round) | grows over time |

**Naming convention:** `P{tier1-id}-{persona-shortname}-round-{round-number}-YYYY-MM-DD.md`

Examples:
- `P1-solo-teacher-round-1-2026-05-04.md`
- `P5-k12-school-round-2-2026-Q3.md`

---

## File Placement Rules

- ✅ **Belongs here:** scored review reports per persona, per round
- ✅ Reports include: total AC count, PASS/PARTIAL/FAIL breakdown, coverage score /100, evidence pointers (file paths, line numbers, API endpoints, screenshots), list of new gaps filed
- ❌ **Does NOT belong here:**
  - Persona AC definitions → [`../persona-criteria/`](../persona-criteria/)
  - Persona catalog metadata → [`../personas-catalog.md`](../personas-catalog.md)
  - Filed-gap files → [`../../04-quality/gaps/`](../../04-quality/gaps/)
  - Feature plans → [`../../03-planning/`](../../03-planning/)

---

## Review Cadence

| Round | When | Who | Output |
|---|---|---|---|
| 1 (initial baseline) | After AC docs ship (GAP-152) | Tier-1 only (P1/P2/P3/P5) | Measured coverage replaces estimates in catalog |
| 2 (post-Wave fixes) | Quarterly OR after major feature wave merges | Tier-1 + Tier-2 if catalog claims coverage | Delta vs baseline; close-out for fixed gaps |
| Event-driven | New persona added; major business pivot; legal/compliance change | Affected personas only | Scoped re-review |

Per `business-logic-review.md` §5 — review cadence linked to AC update cadence.

---

## Report Template

Every report MUST follow this structure (per [GAP-152](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md) §Report template):

```markdown
---
title: Persona Review — P<N> <Name> — Round <R>
status: draft | approved
created: YYYY-MM-DD
reviewer: <name/role>
persona: P<N>
scale: <numbers — actors × volumes>
ac_doc_version: <hash/date of persona-criteria/P<N>-*.md used>
secondary_acs_consumed: [<list of secondary AC files if applicable>]
gap_range_reserved: GAP-XXX..YYY
---

# Review — P<N> <Name>

## Summary

- Total ACs scored: <N>
- PASS: <X> (<X%>)
- PARTIAL: <Y> (<Y%>)
- FAIL: <Z> (<Z%>)
- Overall coverage: <score>/100
- New gaps filed: <list of GAP-XXX>

## Detailed Results

### 1. {Journey section name}

| AC ID | Status | Evidence | Gap |
|-------|:------:|----------|-----|
| AC-XXX-001 | PASS | Screenshot or file:line or endpoint | — |
| AC-XXX-002 | FAIL | "No bulk import UI; tested at /settings/users" | GAP-051 |
| ...

### 2. ...

## Critical Findings

<Top 3-5 most damaging gaps for this persona, ordered by severity>

## Recommendations

<Priority-reordering suggestion based on findings — meta gaps before feature gaps per `meta-gap-priority.md` §3>

## Log
- YYYY-MM-DD — Review completed by <reviewer>
```

---

## Active Reports

_(populated as Wave 17 Phase 2 ships each bucket)_

| Persona | Round | Status | File | Score | New gaps |
|---|:---:|:---:|---|:---:|:---:|
| P1 Solo Teacher | 1 | pending | `P1-solo-teacher-round-1-2026-05-04.md` | TBD | TBD |
| P2 Small Center | 1 | pending | `P2-small-center-round-1-2026-05-04.md` | TBD | TBD |
| P3 Medium Center | 1 | pending | `P3-medium-center-round-1-2026-05-04.md` | TBD | TBD |
| P5 K-12 School | 1 | pending | `P5-k12-school-round-1-2026-05-04.md` | TBD | TBD |

---

## Archive Policy

Reports stay in this directory permanently — historical record of "what worked when" per persona. Reports superseded by newer rounds get a `superseded_by:` frontmatter field pointing to the new round, but the file stays for delta comparison.

Bulk archival to `../../07-archived/` only when persona itself is deprecated (e.g., persona pivots out of strategy).

---

## Related

- [`../personas-catalog.md`](../personas-catalog.md) — persona definitions + Tier 1/2/3 classification + Coverage Review Status table (updated by closure PR after each round)
- [`../persona-criteria/`](../persona-criteria/) — input AC docs (Tier-1 + secondary)
- [`../../04-quality/gaps/GAP-050.md`](../../04-quality/gaps/GAP-050-persona-based-business-review-process.md) — review process definition (parent gap)
- [`../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md`](../../04-quality/gaps/GAP-152-execute-persona-review-round-1.md) — round 1 execution gap
- [`../../03-planning/waves/wave-2026-05-04-persona-review-round-1.md`](../../03-planning/waves/wave-2026-05-04-persona-review-round-1.md) — Wave 17 plan
- [`.claude/skills/quality/persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md) — execution methodology
