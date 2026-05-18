# GAP-195: Starter-Kit Bulk Retro-Sync

**Status:** 🟡 PARTIAL (Phase 1 — tooling + runbook DONE 2026-04-20; Phase 2a — triage DONE 2026-04-29; Phase 2b — upstream PR tracked in GAP-262)
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
2. **Triage checklist** — `documents/05-guides/contributing/starter-kit-retro-sync.md`
   - Rules: is it generic or project-specific?
   - Skills: same, plus "is it battle-tested?"
   - Memories: do any generalize to templates?
3. **Bulk PR to remote** — one PR per category (rules / skills / gotchas) to keep review tractable
4. **Version bump** — MAJOR on remote if restructure, else MINOR per addition (per skill-conventions semver)
5. **Changelog** — explicit log entry per imported item

## Acceptance Criteria

### Phase 1 — Tooling (DONE 2026-04-20)
- [x] Diff script produces triaged output → `scripts/starter-kit-diff.sh`
- [x] Retro-sync runbook drafted → `documents/05-guides/contributing/starter-kit-retro-sync.md`

### Phase 2a — Triage (DONE 2026-04-29)
- [x] Diff script ran cleanly against remote v2.2.0 → `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md`
- [x] Triage report produced (110 NEW local / 48 PROJECT-SPECIFIC / 0 MODIFIED / 0 NEW remote)
- [x] Top-9 rules recommended for first upstream PR (rules-only, conservative scope)
- [x] 5 open questions surfaced for user (Q1 localization, Q2 skills-index, Q3 PR split, Q4 local mirror, Q5 cadence)
- [x] Follow-up gap [GAP-262](GAP-262-starter-kit-upstream-retro-sync-pr.md) filed for Phase 2b execution

### Phase 2b — Upstream PR (tracked in GAP-262, blocked on user Q1–Q5 decisions)
- [ ] First retro-sync PR opened on remote starter-kit → GAP-262
- [ ] CHANGELOG.md on remote reflects imports → GAP-262
- [ ] VERSION bumped correctly (both local and remote in sync) → GAP-262

## Out of Scope

- Porting project-specific skills (kiteclass, kitehub-specific audits) — stay in this repo
- Memories content — review per-item; not all generalize

## Related

- action-1 §9 + §15.F
- `.claude/rules/skill-conventions.md §Remote Repo Sync`
- `.claude/rules/skill-conventions.md §Starter-Kit Version Management`
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P2)

## Log

- **2026-04-29 — Phase 2a closed (Wave Meta Phase-2 Cleanup Cluster 7 Agent C):** Triage report shipped at `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md`. Diff script (Phase 1 tooling) ran clean against remote v2.2.0; identified 110 NEW (local) candidates, 48 correctly classified PROJECT-SPECIFIC, 0 MODIFIED, 0 NEW (remote — local has no `.claude/starter-kit/` mirror folder yet). **Top-9 rules** selected for first upstream PR (rules-only, conservative scope per runbook §6): rule-change-process, output-review-mandate, skill-conventions, audit-to-gap-pipeline, meta-gap-priority, gap-done-discipline, incident-to-rule-pipeline, mcp-first-with-fallback, docs-folder-structure. Skills batches (~85 candidates) intentionally held for PR 2 + PR 3. Five user open questions surfaced for Q1 (marketing-legal localization), Q2 (skills-index template strategy), Q3 (PR split cadence), Q4 (local mirror creation), Q5 (remote cadence). Follow-up [GAP-262](GAP-262-starter-kit-upstream-retro-sync-pr.md) filed to track Phase 2b (actual upstream PR work). Status remains 🟡 PARTIAL — original AC ("First retro-sync PR opened" + "CHANGELOG on remote" + "VERSION bumped") all migrate to GAP-262 because they require cross-repo PR work blocked on user Q1–Q5 decisions. Branch: `feat/wave-meta-p2-gap-195a-starter-kit-triage`.
- 2026-04-20 — Phase 1 closed (Wave 8b-F): diff script + runbook shipped. Script classifies diff into 4 buckets (NEW local / NEW remote / MODIFIED / PROJECT-SPECIFIC); runbook documents triage 4-question checklist + semver bump rules + bulk-PR process. First retro-sync execution tracked in Phase 2. Files: `scripts/starter-kit-diff.sh`, `documents/05-guides/contributing/starter-kit-retro-sync.md`.
- 2026-04-20 — Created from action-1 §15.F.
