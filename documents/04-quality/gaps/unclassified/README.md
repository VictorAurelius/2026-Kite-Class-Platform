# `unclassified/` — OPEN + PLANNED, phase = n/a

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2 row 9

## Contract

Files matching CSV row condition: `status ∈ {OPEN, PLANNED}` AND `phase == n/a`.

Scope = meta gaps (skills, rules, workflow) + foundation work that doesn't tie to a specific release phase. Examples: gap on documentation review process, rule about agent behavior, etc.

## Auto-trigger moves

- New meta gap detected → file lands here at creation
- If gap gains phase assignment later (e.g., user decides this only matters in Phase 2): `git mv unclassified/GAP-NNN.md phase-2/GAP-NNN.md` + update CSV `phase`
- Other transitions same as `phase-1-beta/`

## Why "unclassified/" not "n-a/" or "meta/"?

- POSIX folder name (no slash in `n/a`)
- "unclassified" describes the CSV phase value `n/a` more accurately than "meta" (some unclassified gaps aren't meta)

## Volume budget

Target ~22 files. Well under Rule 3 cap.

## Sibling subdirs

See `../closed/README.md` for full taxonomy map.

**Last Updated:** 2026-05-18
