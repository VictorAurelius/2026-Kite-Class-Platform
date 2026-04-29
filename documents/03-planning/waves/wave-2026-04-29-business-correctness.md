---
title: Wave Business Correctness — Cluster 5 Phase-1 slicing
status: active
created: 2026-04-29
updated: 2026-04-29
gaps: [GAP-049, GAP-050, GAP-150]
deferred_to_next_wave: []
deferred_separate_track: [GAP-151, GAP-152, GAP-153, GAP-154, GAP-155, GAP-156]
---

# Wave Business Correctness — Cluster Pack 5

**Wave date:** 2026-04-29 (kicked off ~02:55 UTC)
**Cluster theme:** Business-Logic-P0+P1 governance — review process rule + persona cadence + BRD skeletons
**Strategy reference:** Cluster 5 in ROADMAP §"Active wave queue" (BL-P0+P1 ranks above feature-P0 per `meta-gap-priority.md` §3). Originally cluster looked oversized (~7-9h serial), so each gap is sliced to **Phase 1** (~25-30 min each, ~75 min wave wall-clock target). Phase 2 work spawns as follow-up gaps (GAP-155/156 etc.) after stakeholder engagement.

## Scope

| # | Gap | Title | Priority | Phase 1 slice | Agent | Disjoint files |
|:-:|-----|-------|:--------:|---------------|:-----:|----------------|
| 1 | **GAP-150** | BRD Documents Completion (5 skeleton docs) | 🟠 BL-P1 | All ACs (skeleton ship-able now; Phase 2 = stakeholder fill = separate gaps) | A | `documents/00-brd/` (5 NEW + README) |
| 2 | **GAP-049** | Business Logic Correctness Review process | 🔴 BL-P0 | Rule file only (`.claude/rules/business-logic-review.md`) + matrix-row flip in `output-review-mandate.md` §3. Phase 2 (compliance audit, stakeholder sign-offs) spawns as **GAP-156**. | B | `.claude/rules/business-logic-review.md` (NEW) + `output-review-mandate.md` |
| 3 | **GAP-050** | Persona-Based Business Review framework | 🔴 BL-P0 | 3 remaining ACs: cadence note + pre-flight integration + quality-audit category. Execution (GAP-152) stays out-of-scope. | C | `.claude/skills/quality/persona-based-business-review.md` + `pre-flight-check` skill + `quality-audit/SKILL.md` |

## Deferred (next wave / separate track)

- **GAP-151** — Persona AC template (sibling to GAP-050, separate scope per `meta-gap-priority.md`)
- **GAP-152** — Persona review execution (consumes BRD output of this wave; round 1 = next quarterly)
- **GAP-153** — Secondary persona AC (extends 151)
- **GAP-154** — BRD scope expansion umbrella (22 additional docs)
- **GAP-155** — Stakeholder fill of BRD skeletons (Phase 2 of GAP-150)
- **GAP-156** — Compliance audit + stakeholder approvals (Phase 2 of GAP-049). Will be filed by Agent B's PR.

## File overlap analysis

Ran `./.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh GAP-049 GAP-050 GAP-150`:

| File | Touched by | Conflict risk | Resolution |
|------|-----------|:-------------:|------------|
| `documents/00-brd/business-objectives.md` (NEW) | A only | None | — |
| `documents/00-brd/compliance-scope.md` (NEW) | A only | None | — |
| `documents/00-brd/pricing-model.md` (NEW) | A only | None | — |
| `documents/00-brd/nfr-catalog.md` (NEW) | A only | None | — |
| `documents/00-brd/go-to-market.md` (NEW) | A only | None | — |
| `documents/00-brd/README.md` | A only | None | — |
| `.claude/rules/business-logic-review.md` (NEW) | B only | None | — |
| `.claude/rules/output-review-mandate.md` | B only | None | — |
| `.claude/skills/quality/persona-based-business-review.md` | C only | None | — |
| `.claude/skills/workflow/pre-flight-check/...` | C only | None | — |
| `.claude/skills/quality-audit/SKILL.md` | C only | None | — |
| `.claude/rules/meta-gap-priority.md` | A,B,C cite | **SOFT** | Read-only citation, no edits expected |
| `documents/04-quality/gaps/ROADMAP.md` | coordinator only | None | Agents do NOT touch ROADMAP. Coordinator handles wave-closure entry per `wave-pack-planner` skill |
| `documents/04-quality/gaps/GAP-{049,050,150}-*.md` | A→GAP-150, B→GAP-049, C→GAP-050 | None | Each agent edits ONLY its own gap file |

**Original overlap script flagged HARD on `documents/00-brd/compliance-checklist.md`** — Agent B's compliance-checklist creation moved to **GAP-156 (Phase 2)**, removed from Phase 1 scope. Re-bucketed → 0 HARD remaining.

Net: 0 HARD, 1 SOFT (read-only citation). Safe to spawn 3 agents parallel.

## Agent workflow

Per `feedback_parallel_agent_strategy.md`:

1. Each agent gets `isolation: "worktree"` (separate git checkout)
2. Branches off `main` (after this foundation PR merges)
3. Commits + creates own PR. Branch naming:
   - Agent A: `feat/wave-bizcorrect-gap-150-brd-skeletons`
   - Agent B: `feat/wave-bizcorrect-gap-049-review-rule`
   - Agent C: `feat/wave-bizcorrect-gap-050-persona-cadence`
4. Reports back PR number + scope summary + which ACs flipped
5. Coordinator merges sequentially: A → B → C (smallest blast radius first)
6. Conflict resolution: 0 HARD predicted; SOFT on `meta-gap-priority.md` is read-only — no merge conflict expected
7. Wave closure ROADMAP entry after all 3 merge

## Acceptance criteria (wave-level)

- [ ] 3 PRs merged (one per gap) with green CI
- [ ] GAP-150 → 🟢 DONE (5 skeleton docs + README updates shipped, Phase 2 deferred to GAP-155)
- [ ] GAP-049 → 🟡 PARTIAL (Phase 1 rule shipped; Phase 2 → new GAP-156 filed by Agent B)
- [ ] GAP-050 → 🟡 PARTIAL (3 framework AC done; execution remains in GAP-152 already-tracked)
- [ ] All gap closures pass `gap-done-discipline.md` §2 (no banned phrases)
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry (counts updated, Cluster 5 → SHIPPED)
- [ ] `output-review-mandate.md` §3 matrix row "Business logic CORRECTNESS" updated by Agent B (❌ → ⚠️ PARTIAL pointing to new rule + GAP-156)
- [ ] Worktrees + branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6
- [ ] `data/wave-history.jsonl` entry appended with wall-clock + lessons
- [ ] Lessons-learned section filled below

## Wall-clock target

- Foundation PR (this doc + ROADMAP active-queue update): ~10 min
- 3 parallel agents: ~25-35 min wall-clock (each ~25-30 min agent-time)
- Sequential merge + conflict resolution: ~10 min
- Closure (ROADMAP + GAP-156 file + cleanup + retrospective): ~15 min
- **Total wave: ~60-70 min** (matches Wave Obs / Wave DR-Backup ~75 min benchmark)

## Lessons-learned (filled AFTER wave merges)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold?
- [ ] Contamination details if any:

### File-overlap accuracy
- [ ] Predicted SOFT: `meta-gap-priority.md` (citation only); actual:
- [ ] Predicted HARD: 0 after re-bucket; actual:
- [ ] Unpredicted conflicts:

### Wall-clock
- [ ] Estimated: 60-70 min; actual: ; variance source:

### Agent prompt quality
- [ ] Clarification rounds: A=, B=, C=
- [ ] Template updates needed:

### Token cost
- [ ] Total tokens: ; per gap:

### Cleanup
- [ ] Worktrees removed
- [ ] Local branches deleted
- [ ] Remote branches deleted

### Novel patterns
- [ ] New memory entry filed?
- [ ] Rule update proposed?

## Log

- 2026-04-29 — Wave plan created. Cluster 5 sliced into Phase-1 slices to fit wave-pack-planner ~75 min target. Foundation PR will land this doc + ROADMAP §"Active wave queue" Cluster 5 row IN_PROGRESS. After merge, 3 worktree-isolated agents spawn from main.
