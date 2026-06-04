# Git Pre-Commit Hooks (Opt-In)

Lightweight local validators that catch CI-class failures BEFORE commit, saving
push → CI fail → revision-PR round-trips.

## Install (one-time per clone)

```bash
git config core.hooksPath scripts/hooks
```

This points git at this folder instead of the default `.git/hooks/`. Setting is
per-clone (not tracked by git), so each contributor opts in independently.

Verify:

```bash
git config --get core.hooksPath
# expected output: scripts/hooks
```

## Uninstall

```bash
git config --unset core.hooksPath
```

## What's enforced

### `pre-commit`

Runs targeted validators on STAGED files only (fast — no full repo scan):

| Validator | Triggered when | Catches |
|---|---|---|
| `check-wave-plan-completeness.sh` | Any `documents/03-planning/waves/*.md` staged (except `_TEMPLATE.md`) | Missing canonical sections (`## 1. Brainstorm` … `## 8. Log`) or required frontmatter (`title`/`status`/`created`/`waves`) |

More validators can be added incrementally — see `scripts/hooks/pre-commit`.

## Bypass

If you must commit despite a hook failure (rare — e.g. WIP stash):

```bash
git commit --no-verify
```

Reviewer will catch via CI canonical anyway. Don't make this a habit.

## Why opt-in (not auto-install)

- Setting `core.hooksPath` is per-clone state; we can't enforce it via the
  repo. Auto-installing would require shipping a Node/husky dependency that the
  project deliberately avoids.
- Opt-in keeps the workflow flexible for ad-hoc environments (CI agents,
  shallow clones, scratchspace) where hooks would be friction.

## Provenance

Created 2026-06-04 in response to recurrence #2 of wave-plan-completeness CI
failure class:

- PR #2141 (2026-06-03 Wave 14): "docs(wave-14): restructure plan to canonical
  template headings (unblock docs CI)"
- PR #2148 (2026-06-04 Wave flow-kh3 stub): failed `Wave plan completeness`
  job, required commit `c615131b` to fix

Per `.claude/rules/incident-to-rule-pipeline.md` §3.1 SHIP-NOW eligibility
(trivial bash detector + recurrence ≥2 confirmed → detector ship-now mandate).
Sister rule: `.claude/skills/quality/wave-pack-planner/SKILL.md` §Step 4.
