# `closed/` (root) — LEGACY archive

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2.3 + §2.1 last row

## Contract (LEGACY scope only)

This root-level `closed/` contains **historical orphan files** (~196 files) created **before** the phase-based CSV migration (per `gap-architecture-v2.md` Phase 2 bulk migration 2026-05-11). These files do NOT have CSV rows in `gap-status.csv` — they predate the canonical-CSV system.

## DEPRECATED for NEW DONE archives

**Per rule v2.0.0:** new DONE gaps → `phase-X/closed/` matching their CSV phase. **Do NOT add new DONE gaps here.**

## What's here

- ~196 orphan files: pre-CSV DONE gaps preserved for cross-link history
- These files were tracked under v1.0.0 design (PR #1532) but rule v2.0.0 moves new DONE archives to per-phase scope
- Wave 95 PR2 mass migration moves 90 CSV-tracked DONE from here → `phase-X/closed/`; orphans stay

## Sibling per-phase archives

- `../phase-1-beta/closed/`
- `../phase-1.5-paid/closed/`
- `../phase-2/closed/`
- `../phase-3/closed/`
- `../unclassified/closed/`

## Future cleanup (separate gap)

If 196 orphans need full classification: file follow-up gap to manually phase-tag each + migrate. Currently grandfathered per rule §2.1 last row.

**Last Updated:** 2026-05-18
