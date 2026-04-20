# GAP-195: Starter-Kit Bulk Retro-Sync

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (meta tier — multiplier for future projects)
**Domain:** Meta / Skills / Starter-Kit
**Found:** 2026-04-20 (action-1 §9 + §15.F)
**Wave:** Wave 8b (meta)
**Affects:** `github.com/VictorAurelius/claude-starter-kit` — source of truth for future projects using the kit

## Problem

After 100+ PRs and 8 waves in this project, the remote starter-kit has not absorbed the learnings:

- New rules born here (meta-gap-priority, post-wave-audit-mandate, audit-to-gap-pipeline, docs-folder-structure) exist only in this repo's `.claude/rules/`
- New skills and skill refinements (ui-review /128, business-logic-audit, ops-readiness-audit, performance-audit, api-contract-audit) — same
- New gotchas in `skill-conventions.md` not reflected in remote
- Memories (`feedback_*`) that generalize well — no export path

User question (action-1 line 653): "kế hoạch để update cho starter-kit sau rất nhiều PR/WAVE của dự án là gì?"

Existing sync rule (`skill-conventions.md §Remote Repo Sync`) handles per-change sync, but no **retro-sync** process for the accumulated delta.

## Context

Low urgency (P2) but high value — locking in learnings protects them for the next project. Best done between waves when backlog is quieter.

## Proposed Fix

1. **Diff script** — `scripts/starter-kit-diff.sh`
   - Clone remote `claude-starter-kit` to `/tmp/kit`
   - Compare `.claude/rules/`, `.claude/skills/` (ignore project-specific files)
   - Output classified diff: NEW / MODIFIED / PROJECT-SPECIFIC-OMIT
2. **Triage checklist** — `documents/05-guides/starter-kit-retro-sync.md`
   - Rules: is it generic or project-specific?
   - Skills: same, plus "is it battle-tested?"
   - Memories: do any generalize to templates?
3. **Bulk PR to remote** — one PR per category (rules / skills / gotchas) to keep review tractable
4. **Version bump** — MAJOR on remote if restructure, else MINOR per addition (per skill-conventions semver)
5. **Changelog** — explicit log entry per imported item

## Acceptance Criteria

- [ ] Diff script produces triaged output
- [ ] Retro-sync runbook drafted
- [ ] First retro-sync PR opened on remote starter-kit
- [ ] CHANGELOG.md on remote reflects imports
- [ ] VERSION bumped correctly (both local and remote in sync)

## Out of Scope

- Porting project-specific skills (kiteclass, kitehub-specific audits) — stay in this repo
- Memories content — review per-item; not all generalize

## Related

- action-1 §9 + §15.F
- `.claude/rules/skill-conventions.md §Remote Repo Sync`
- `.claude/rules/skill-conventions.md §Starter-Kit Version Management`
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P2)

## Log

- 2026-04-20 — Created from action-1 §15.F.
