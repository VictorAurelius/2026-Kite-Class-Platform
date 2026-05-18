# `phase-1-beta/` — gaps scoped to Phase 1 BETA (any non-DONE status)

**Rule:** [`.claude/rules/gap-folder-organization.md`](../../../.claude/rules/gap-folder-organization.md) §2 v2.0.0 phase-only design

## Contract

Files matching CSV row condition: `phase == phase-1-beta` AND `status != DONE`.

Statuses included: `OPEN`, `PARTIAL`, `IN_PROGRESS`, `PENDING`, `PLANNED`, `WONTFIX`.

**Status changes (OPEN → PARTIAL → PENDING etc.) DO NOT move file.** Only CSV row updates. File stays at `phase-1-beta/GAP-NNN-*.md`.

**Status → DONE:** `git mv` to `closed/` subdir (one-way archive).

## Subdir

- `closed/` — DONE archive (see `closed/README.md`)

## Target count (post Wave 95 PR2 migration)

~151 active gaps (Total phase-1-beta 232 - 81 DONE in closed/ = 151).

## Sibling phase folders

- `../phase-1.5-paid/`, `../phase-2/`, `../phase-3/`, `../unclassified/` (phase=n/a)

## Lifecycle event mapping

See [parent rule §3 Required actions per lifecycle event](../../../.claude/rules/gap-folder-organization.md).

**Last Updated:** 2026-05-18
