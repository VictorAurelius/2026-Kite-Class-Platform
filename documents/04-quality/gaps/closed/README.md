# `closed/` — DONE archive

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2 row 1

## Contract

Files matching CSV row condition: `status == DONE`.

Auto-trigger move: closing PR per `gap-done-discipline.md` §2 `git mv <subdir>/GAP-NNN.md closed/GAP-NNN.md` + update CSV `filename` column.

## Sibling subdirs (per taxonomy)

- `../pending/` — PENDING (deferred legal/compliance scope)
- `../partial/` — PARTIAL or IN_PROGRESS
- `../wontfix/` — WONTFIX (decision logged)
- `../phase-1-beta/`, `../phase-1.5-paid/`, `../phase-2/`, `../phase-3/` — OPEN/PLANNED by phase
- `../unclassified/` — OPEN/PLANNED + phase `n/a`

## Notes

- 90 files have matching CSV rows (DONE status)
- ~196 historical orphan files predate CSV system (Wave 4 migration cutover) — kept for cross-link history; tracked under future cleanup gap if needed
- Cross-link from active gap → `closed/GAP-XXX.md` (relative path)

**Last Updated:** 2026-05-18
