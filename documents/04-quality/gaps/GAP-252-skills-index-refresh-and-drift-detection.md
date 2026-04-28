# GAP-252: Skills index `_README-skills-index.md` is 12 days stale + no drift detector

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Meta — index drift erodes skill discoverability per `skill-conventions.md` §3)
**Domain:** Governance / Skills / Documentation
**Detected:** 2026-04-28 (ecosystem audit)
**Affects:** `.claude/skills/_README-skills-index.md`; every developer/Claude session relying on index for skill discovery

## Problem

`_README-skills-index.md` header reads `**Updated:** 2026-04-16` — 12 days stale on detection day. State-check by scanning `find .claude/skills -name SKILL.md`:

| Skill present in folder | Listed in index? |
|-------------------------|:----------------:|
| `quality/ai-branding-quality-gate/SKILL.md` | ❌ |
| `quality/rework-audit/SKILL.md` | ⚠️ (mentioned but row format inconsistent) |
| `quality/gap-review/SKILL.md` | ⚠️ (mentioned without row in main table) |
| `quality/rule-review/SKILL.md` | ⚠️ (mentioned without row in main table) |
| `document-generation/excel/SKILL.md` | ❌ |
| `document-generation/pdf/SKILL.md` | ❌ |
| `document-generation/word/SKILL.md` | ❌ |
| `workflow/docs-freshness/SKILL.md` | ❌ |
| `workflow/quality-plan/SKILL.md` | ❌ |
| `workflow/session-docs-check/SKILL.md` | ❌ |

Total folder count: 27 SKILL.md. Total table rows in current index: ~18. Gap: 9+ skills missing.

## Root Cause

Index is hand-maintained. No detector emits a warning when a new SKILL.md folder lands without a corresponding row. Skill PRs ship without index update because nothing flags the omission.

## Proposed Fix

### Layer 1: Manual rebuild (one-shot, this PR)

Rewrite `_README-skills-index.md`:
- Bump header `**Updated:** 2026-04-28`
- Re-section index by category (Core / Quality & Audit / Workflow / Document Generation / Backend / Frontend / DevOps / Reference / Testing)
- Each row: `path` + 1-line description sourced from SKILL.md frontmatter `description` field
- Total: all 27 SKILL.md listed

### Layer 2: Drift detector (sub-section in `scripts/check-skill-conventions.sh` from GAP-251)

Add a check inside `check-skill-conventions.sh`:
1. `find .claude/skills -name SKILL.md | wc -l` → folder count
2. Count rows in `_README-skills-index.md` matching skill-path pattern (`\| .*SKILL.md \|`)
3. If counts differ → emit WARN with diff list (which folders missing from index)
4. Exit non-zero if `--strict` flag set; advisory WARN otherwise

This piggybacks on GAP-251's script — no separate file.

## Acceptance Criteria

- [ ] `_README-skills-index.md` rebuilt; all 27 SKILL.md files have a row
- [ ] Header timestamp `**Updated:** 2026-04-28`
- [ ] Categories match folder structure (no orphan rows for non-existent files)
- [ ] Drift detector added to `scripts/check-skill-conventions.sh` (GAP-251) — diff list emitted on mismatch
- [ ] Self-test: temporarily remove a row → script reports "missing in index: X"; restore row → exit-0

## Out-of-scope

- Auto-regenerating the index via cron/CI (manual + CI WARN is enough for now)
- Maintaining `description` consistency across SKILL.md vs index (separate gap if needed)
- Renaming/restructuring skills folder hierarchy (separate gap)

## Related

- GAP-251 (sister — skill-conventions CI lint hosts the drift detector)
- `skill-conventions.md` §3 (description = trigger condition; index is downstream artifact)
- `output-review-mandate.md` §3 row "Skills (meta)"
- Index file: `.claude/skills/_README-skills-index.md`

## Log

- **2026-04-28** Filed during ecosystem audit (Wave Meta-Gov 1, Move 2). 12-day staleness; 9+ skills not listed. Drift detector reuses GAP-251 script — no new file added.
