# `phase-1-beta/` — OPEN + PLANNED, phase = phase-1-beta

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2 row 5

## Contract

Files matching CSV row condition: `status ∈ {OPEN, PLANNED}` AND `phase == phase-1-beta`.

Phase 1 BETA scope = solo dev + invite-only beta tenants per `documents/03-planning/roadmap/release-1-plan-2026.md` §3.

## Auto-trigger moves

- New gap detected in Phase 1 BETA scope → file lands here at creation
- Status flip OPEN → PARTIAL: `git mv phase-1-beta/GAP-NNN.md partial/GAP-NNN.md`
- Status flip OPEN → DONE: `git mv phase-1-beta/GAP-NNN.md closed/GAP-NNN.md`
- Phase re-scope (phase-1-beta → phase-2): `git mv phase-1-beta/GAP-NNN.md phase-2/GAP-NNN.md` + update CSV `phase`

## Volume budget

Per `docs-folder-volume-budget.md` Rule 3 cap 200. Current target ~70 files at PR2 mass migration. Headroom for Phase 1 BETA close-out wave growth.

## Sibling subdirs

See `../closed/README.md` for full taxonomy map.

**Last Updated:** 2026-05-18
