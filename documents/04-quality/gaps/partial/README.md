# `partial/` — PARTIAL + IN_PROGRESS (cross-phase priority surface)

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2 row 3

## Contract

Files matching CSV row condition: `status == PARTIAL` OR `status == IN_PROGRESS`.

Cross-cuts phase — gap can be `partial/` regardless of phase. CSV `phase` column remains canonical (per `gap-architecture-v2.md` §3) for phase classification.

## Why a dedicated subdir?

User flagged 2026-05-18: PARTIAL gaps need priority surface — quick `ls partial/` access to push them DONE before opening new OPEN work.

## Auto-trigger moves

- New status `OPEN → PARTIAL` (work started): `git mv phase-X/GAP-NNN.md partial/GAP-NNN.md` + update CSV `filename`
- New status `PARTIAL → DONE`: `git mv partial/GAP-NNN.md closed/GAP-NNN.md` + update CSV `filename`
- Re-scope `PARTIAL phase reclassify`: file stays in `partial/`, only CSV `phase` column changes

## Sibling subdirs

See `../closed/README.md` for full taxonomy map.

## Volume budget

Per `docs-folder-volume-budget.md` Rule 3 cap 200. Current target: ~121 files at PR2 mass migration. Under cap.

**Last Updated:** 2026-05-18
