# Gap Peer-Review Sheet — TEMPLATE

**Purpose:** reviewer fills this in, pastes the result into the gap's `## Log` section. Do not commit the filled sheet as a separate file.

**Skill:** `.claude/skills/quality/gap-review/SKILL.md`
**Checklist:** `.claude/skills/quality/gap-review/reference/checklist.md`
**Rule:** `.claude/rules/audit-to-gap-pipeline.md` (filing) + `.claude/rules/meta-gap-priority.md` (tiering)

---

## How to use this template

1. Copy the YAML + scoring table below into a scratch buffer (do NOT commit this sheet)
2. Fill in each row PASS / FAIL / N/A with a short note
3. Paste the compact **Review Signature** (bottom) into the gap's `## Log` section
4. If any mandatory row FAILs → gap remains 🔵 OPEN; comment on the PR with required fixes
5. If all mandatory PASS → gap can move 🔵 OPEN → 🟡 PLANNED

---

## Review Sheet (scratch buffer — do NOT commit)

```yaml
gap: GAP-XXX
reviewer: @github-handle
date: YYYY-MM-DD
overall: PASS | BLOCK
```

| # | Criterion | Section | Mandatory | Result | Notes |
|---|-----------|---------|:---------:|:------:|-------|
| 1.1 | Problem observable | Clarity | ✅ | PASS/FAIL | |
| 1.2 | Evidence attached | Clarity | ✅ | PASS/FAIL | |
| 2.1 | Root cause analyzed OR deferred | Scope | ✅ | PASS/FAIL | |
| 2.2 | State-check (pipeline §2.5) | Scope | ✅ | PASS/FAIL | |
| 2.3 | Duplicate check | Scope | ✅ | PASS/FAIL | |
| 3.1 | AC check-boxable | Planning | ✅ | PASS/FAIL | |
| 3.2 | Priority tier correct (Meta/BL/Feat) | Planning | ✅ | PASS/FAIL | |
| 3.3 | P-level matches severity | Planning | ⚪ | PASS/FAIL | |
| 3.4 | Dependencies identified | Planning | ⚪ | PASS/FAIL | |
| 3.5 | ROADMAP registered | Planning | ✅ | PASS/FAIL | |
| 4.1 | Domain tagged | Metadata | ⚪ | PASS/FAIL | |

**Pass condition:** every mandatory row (✅) = PASS. Any optional (⚪) row FAIL = comment for author but not blocking.

---

## Review Signature (paste this into gap `## Log`)

**On PASS:**

```markdown
- YYYY-MM-DD — Peer review by @reviewer — PASS (11/11 mandatory criteria)
  - Notes: {optional one-line note, e.g. "AC tightened during review"}
```

**On BLOCK:**

```markdown
- YYYY-MM-DD — Peer review by @reviewer — BLOCK
  - Failed: 2.2 (state-check missing), 3.1 (ACs not measurable)
  - Action: author revises; re-review after fix
```

---

## Examples

### Example 1 — Clean PASS

```markdown
- 2026-04-22 — Peer review by @nguyenvankiet — PASS (11/11)
  - State-check confirmed (GAP has Current State section + file inventory)
  - AC includes k6 threshold for perf criterion
```

### Example 2 — BLOCK with detail

```markdown
- 2026-04-22 — Peer review by @nguyenvankiet — BLOCK
  - Failed: 2.2 state-check missing; 3.2 classified Feature but touches .claude/rules/
  - Action: add `## Current State (verified 2026-04-22)`; re-tag as Meta-P0 per meta-gap-priority §3
  - Re-review after author fix
```

---

## Related

- Skill: `.claude/skills/quality/gap-review/SKILL.md`
- Checklist: `.claude/skills/quality/gap-review/reference/checklist.md`
- Rule: `.claude/rules/audit-to-gap-pipeline.md` §2.5 (state-check)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (priority tiers)
- Rule: `.claude/rules/output-review-mandate.md` §4 VIOLATION #1 (closed by this template + skill)
