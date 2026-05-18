# `phase-1-beta/closed/` — DONE archive within phase scope

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../../.claude/rules/gap-folder-organization.md) §2.3 (per-phase closed archive)

## Contract

Files matching CSV row condition: `phase == phase-1-beta` AND `status == DONE`.

**One-way archive** — files enter via `git mv ../GAP-NNN.md closed/GAP-NNN.md` on PARTIAL→DONE flip; never moved back (per `gap-done-discipline.md` DONE doesn't re-open).

## Why per-phase (not single root `closed/`)?

- Preserves phase scope semantic ("this gap shipped in Phase 1 BETA")
- Retro query: `ls phase-1-beta/closed/` answers "what shipped in P1B?"
- Volume cap: phase-1-beta total 232 splits into 151 active + 81 closed, both <200

## Target count (post Wave 95 PR2 mass migration)

~81 DONE gaps with phase=phase-1-beta.

**Last Updated:** 2026-05-18
