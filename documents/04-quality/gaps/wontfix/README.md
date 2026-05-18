# `wontfix/` — WONTFIX (decision logged, not implemented)

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2 row 4

## Contract

Files matching CSV row condition: `status == WONTFIX`.

Decision to NOT fix is recorded in gap file Log section per `gap-done-discipline.md` §3 (rationale, alternatives considered, revisit criteria).

## Auto-trigger move

Status flip to WONTFIX → `git mv <prior-subdir>/GAP-NNN.md wontfix/GAP-NNN.md` + update CSV `filename`.

## Sibling subdirs

See `../closed/README.md` for full taxonomy map.

## Volume budget

Target ~4 files. Well under Rule 3 cap.

**Last Updated:** 2026-05-18
