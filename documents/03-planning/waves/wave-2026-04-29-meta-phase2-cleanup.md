---
title: Wave Meta Phase-2 Cleanup — session-lock hook + lefthook pre-commit + starter-kit triage
status: active
created: 2026-04-29
updated: 2026-04-29
gaps: [GAP-193, GAP-194, GAP-195]
deferred_to_next_wave: [GAP-195-phase-2b]
deferred_separate_track: [GAP-198, GAP-199, GAP-201, GAP-261]
---

# Wave Meta Phase-2 Cleanup — Cluster Pack 7

**Wave date:** 2026-04-29 (3rd wave today; kicked off after Wave Meta-Gov 2 SHIPPED ~03:55 UTC)
**Cluster theme:** Phase-2 follow-throughs of 3 Meta-P1+P2 gaps that have shipped Phase 1 — close the deferred work
**Strategy reference:** Per `meta-gap-priority.md` Phase-2 of meta gaps still ranks at meta tier. Validates wave-pack-planner methodology a 6th time, this time on **mixed code/config/docs** work (prior 5 waves were docs-heavy).

## Scope

| # | Gap | Title | Priority | Phase 2 slice | Agent | Disjoint files |
|:-:|-----|-------|:--------:|---------------|:-----:|----------------|
| 1 | **GAP-193 P2** | Session-lock hook enforcement + telemetry | 🟠 Meta-P1 | Hook blocks commits on locked branch from different session + session-lock archival on session-end + turn-count telemetry in `PR-{N}.json` via audit-gate.py + `/end-session` skill | A | `.claude/hooks/*.py` + `.claude/skills/workflow/end-session/` (NEW) + `.claude/skills/workflow/start-session/scripts/collect-state.sh` (small extension) |
| 2 | **GAP-194 P2** | Pre-commit hook gate (lefthook, since no `.husky/`) | 🟠 Meta-P1 | `lefthook.yml` (NEW) running shellcheck + ruff on changed files locally; doc snippet in README + skill update; small README install instructions | B | `lefthook.yml` (NEW) + `documents/05-guides/local-dev-pre-commit.md` (NEW) + `.claude/skills/quality/script-review-checklist.md` (note CI + local) |
| 3 | **GAP-195 P2a** | Starter-kit retro-sync triage report (PR upstream → 2b) | 🟡 Meta-P2 | Run `scripts/starter-kit-diff.sh`, produce triage report classifying delta into 4 buckets per runbook, recommend top-N items for first upstream PR. **DOES NOT open upstream PR** — that's Phase 2b which needs human judgement on cross-repo scope | C | `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md` (NEW) + `documents/05-guides/starter-kit-retro-sync.md` (small update — cite triage report as Phase 2a output) |

## Phase-2 slicing rationale

**GAP-193 P2** is fully implementable in single PR — well-scoped enforcement work.

**GAP-194 P2** uses **lefthook** (single-binary, language-agnostic) instead of husky (npm-only; project uses Maven/pnpm). Per gap §"Proposed Fix" #1 says "`.husky/pre-commit` or equivalent" — lefthook is the equivalent.

**GAP-195 Phase 2** raw scope = open upstream PR on `VictorAurelius/claude-starter-kit`. That's cross-repo + needs human judgement on what to import (5+ rules, 30+ skills). **Sliced to Phase 2a (triage report only)** to fit 60-75 min wave-pack target. Phase 2b (actual upstream PR) tracked as **GAP-195-phase-2b** to be filed by Agent C.

## Deferred

- **GAP-195 Phase 2b** — open upstream PR after triage. Cross-repo, requires human review. Filed as new gap by Agent C.
- **GAP-198 Phase 2** — FE-BE contract test impl. Out-of-scope: ~2-3h, mixes FE+BE; better single-track.
- **GAP-199 Phase 2** — rework-audit pilot. Depends on GAP-193 P2 (turn-count telemetry) — must land first; out-of-scope this wave.
- **GAP-201 Phase 2** — tenant off-boarding impl. Multi-week saga work; not wave-pack-eligible.
- **GAP-261** (Werror flip-day) — Phase 2 of GAP-245. ~2-3h burndown; out-of-scope this wave.

## File overlap analysis

Ran `./.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh GAP-193 GAP-194 GAP-195`:

| File | Touched by | Conflict risk | Resolution |
|------|-----------|:-------------:|------------|
| `.claude/hooks/*.py` (NEW + modify audit-gate.py) | A only | None | — |
| `.claude/skills/workflow/end-session/` (NEW) | A only | None | — |
| `.claude/skills/workflow/start-session/scripts/collect-state.sh` | A only (small extension for telemetry) | None | — |
| `lefthook.yml` (NEW) | B only | None | — |
| `documents/05-guides/local-dev-pre-commit.md` (NEW) | B only | None | — |
| `.claude/skills/quality/script-review-checklist.md` | B only (small note) | None | — |
| `documents/04-quality/audits/starter-kit/retro-sync-triage-2026-04-29.md` (NEW) | C only | None | — |
| `documents/05-guides/starter-kit-retro-sync.md` | C only (small update) | None | — |
| `documents/04-quality/gaps/GAP-{193,194,195}-*.md` | A→193, B→194, C→195 | None | each agent owns own gap file |
| `documents/04-quality/gaps/GAP-195-phase-2b-*.md` (NEW) | C only | None | new follow-up gap |
| `.claude/rules/meta-gap-priority.md` | A,B,C cite | **SOFT** | Read-only citation, no edits |
| `.claude/rules/skill-conventions.md` | A,C cite | **SOFT** | Read-only citation, no edits |
| `documents/04-quality/gaps/ROADMAP.md` | coordinator only | None | wave-closure |

Net: 0 HARD, 2 SOFT (both read-only citations). Safe to spawn 3 agents parallel.

## Agent workflow

Per `feedback_parallel_agent_strategy.md`:

1. Each agent gets `isolation: "worktree"` (separate git checkout)
2. Branches off `main` (after this foundation PR merges)
3. Commits + creates own PR. Branch naming:
   - Agent A: `feat/wave-meta-p2-gap-193-session-lock-hook`
   - Agent B: `feat/wave-meta-p2-gap-194-lefthook-precommit`
   - Agent C: `feat/wave-meta-p2-gap-195a-starter-kit-triage`
4. Reports back PR number + scope summary + ACs flipped + follow-up gap IDs (Agent C files GAP-195 Phase 2b — must verify next-free GAP ID per Wave Meta-Gov 2 lesson; current next-free = check before agent file)
5. Coordinator merges sequentially: A → B → C (B simplest — config; A code-heaviest; C docs-heaviest)
6. Conflict resolution: 0 HARD predicted; SOFT on rules read-only — no merge conflict expected
7. Wave closure ROADMAP entry after all 3 merge

## Acceptance criteria (wave-level)

- [ ] 3 PRs merged with green CI
- [ ] GAP-193 → 🟢 DONE (Phase 2 ACs all met) — both Phase 1 + Phase 2 closed = full DONE per gap-done-discipline.md §2
- [ ] GAP-194 → 🟢 DONE (Phase 1 + Phase 2 closed; uses lefthook = "or equivalent" per gap §"Proposed Fix")
- [ ] GAP-195 → 🟡 PARTIAL (Phase 1 + Phase 2a done; Phase 2b → new GAP-{next-free} filed for upstream PR work)
- [ ] All gap closures pass `gap-done-discipline.md` §2 (no banned phrases)
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry
- [ ] Worktrees + branches cleaned post-merge
- [ ] `data/wave-history.jsonl` entry appended

## Wall-clock target

- Foundation PR (this doc + ROADMAP active-queue update): ~10 min
- 3 parallel agents: ~50-70 min wall-clock (Agent A heaviest at ~60-90 min agent-time due to hook code + tests; B ~30 min config; C ~45 min triage)
- Sequential merge + conflict resolution: ~10 min
- Closure (ROADMAP + cleanup + retrospective): ~15 min
- **Total wave: ~85-105 min** (longer than prior waves due to Agent A's code scope — first non-docs wave-pack)

## Lessons-learned (filled AFTER wave merges)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold?

### File-overlap accuracy
- [ ] Predicted 0 HARD, 2 SOFT (both citations); actual:

### Wall-clock
- [ ] Estimated: 85-105 min; actual: ; variance source:

### Agent prompt quality (mixed code/config/docs)
- [ ] Clarification rounds: A=, B=, C=
- [ ] First non-docs wave — does feature-tdd-agent template hold for hook code?

### Token cost
- [ ] Total tokens: ; per gap:

### Cleanup
- [ ] Worktrees + branches cleaned

### Novel patterns
- [ ] New memory entry filed?
- [ ] Rule update proposed?

## Log

- 2026-04-29 — Wave plan created. 3rd wave today (after Wave Business Correctness + Wave Meta-Gov 2). First wave with non-docs work (Agent A hook code, Agent B lefthook config, Agent C audit triage). Validates wave-pack-planner methodology on mixed work types. GAP-195 P2 sliced to 2a (triage report) + 2b (cross-repo upstream PR deferred).
