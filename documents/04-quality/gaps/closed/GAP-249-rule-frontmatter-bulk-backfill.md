# GAP-249: Rule files missing mandatory frontmatter (Version / Last-Reviewed / Reviewer-Approver)

**Status:** 🟢 DONE 2026-04-28 — All 8 rules backfilled + verified by GAP-250 detector (PR pending)
**Priority:** 🟠 P1 (Meta — force-multiplier per `meta-gap-priority.md`)
**Domain:** Governance / Rules
**Detected:** 2026-04-28 (ecosystem audit)
**Affects:** 8 of 14 rule files in `.claude/rules/`; every future rule edit relies on these fields existing per `rule-change-process.md` §3

## Problem

`rule-change-process.md` §3 mandates every rule file open with frontmatter containing `Version`, `Last-Reviewed`, and `Reviewer-Approver`. The §3 backfill-on-next-edit policy means rules edited recently DO have it, but rules not edited since v1.0.0 of the process (2026-04-20) still lack it.

State-check 2026-04-28: 8 of 14 rules are non-compliant.

| Rule | Has Version? | Has Last-Reviewed? | Has Reviewer-Approver? |
|------|:-:|:-:|:-:|
| `audit-to-gap-pipeline.md` | ✅ v1.0 | ❌ | ❌ |
| `docs-folder-structure.md` | ❌ | ❌ | ❌ |
| `logs-format-standard.md` | ❌ | ❌ | ❌ |
| `mcp-first-with-fallback.md` | ❌ | ❌ | ❌ |
| `meta-gap-priority.md` | ❌ | ❌ | ❌ |
| `planning-docs-structure.md` | ❌ | ❌ | ❌ |
| `post-wave-audit-mandate.md` | ❌ | ❌ | ❌ |
| `skill-conventions.md` | ❌ | ❌ | ❌ |

Compliant: `output-review-mandate.md`, `design-patterns.md`, `ai-branding-guidelines.md`, `gap-done-discipline.md`, `incident-to-rule-pipeline.md`, `rule-change-process.md`.

## Root Cause

`rule-change-process.md` v1.0.0 introduced the frontmatter requirement on 2026-04-20 with backfill-on-next-edit policy. Rules above haven't been edited since → still non-compliant. No CI gate enforces frontmatter on merge → no detection layer.

## Proposed Fix

Bulk PATCH bump on all 8 rules in a single PR:
1. Add `**Version:** 1.0.0` (or 1.0.x if rule has changed materially since creation)
2. Add `**Last-Reviewed:** 2026-04-28`
3. Add `**Reviewer-Approver:** @nguyenvankiet (solo-dev — backfill per `rule-change-process.md` §3)`
4. Add `**Applies to:** {scope}` per existing convention
5. Append Log entry per rule: `**2026-04-28 (vX.Y.Z PATCH):** Frontmatter backfill per GAP-249. No content change.`

Pair with **GAP-250** (CI gate) — backfill alone doesn't prevent regression.

## Acceptance Criteria

- [x] All 8 rules above have Version + Last-Reviewed + Reviewer-Approver + Applies-to fields
- [x] Each rule has a 2026-04-28 PATCH log entry citing GAP-249
- [x] No content changes — only frontmatter (verify via `git diff` should show only header + log additions)
- [x] `scripts/check-rule-frontmatter.sh` (from GAP-250) returns exit-0 on all 14 rules

## Out-of-scope

- Adding `## Related` sections (separate gap, P2)
- Adding `## Enforcement` sections to rules that lack them (separate gap)
- Changing rule content / semver bump beyond PATCH

## Related

- `rule-change-process.md` §3 (mandate) + §5 (review matrix — solo-dev PATCH self-approve)
- GAP-250 (CI gate — paired in same wave)
- GAP-171 (rules-docs ADR process — closed; this is its enforcement layer)
- `output-review-mandate.md` §3 row "Rules docs (meta)"

## Log

- **2026-04-28** ✅ Closed by Wave Meta-Gov 1 Agent A (`feature/wave-meta-gov-1-A-rule-frontmatter`). All 8 rules backfilled with Version 1.0.0 + Last-Reviewed 2026-04-28 + Reviewer-Approver + Applies-to. `output-review-mandate.md` also picked up missing `Applies to` field (PATCH bump 1.1.0 → 1.1.1) — caught by paired GAP-250 detector during self-test, fixed in same PR. `scripts/check-rule-frontmatter.sh --all` returns exit-0 on all 14 rules. Diff is purely additive frontmatter + log entries; verified via `git diff --stat .claude/rules/`.
- **2026-04-28** Filed during ecosystem audit (Wave Meta-Gov 1). Force-multiplier P1 per `meta-gap-priority.md` §3 (Meta > Feature). Paired with GAP-250.
