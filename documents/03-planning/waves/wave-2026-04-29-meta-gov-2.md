---
title: Wave Meta-Gov 2 — Maven warning gate + scaffold-as-DONE truth-up + status sync
status: active
created: 2026-04-29
updated: 2026-04-29
gaps: [GAP-245, GAP-225, GAP-224]
deferred_to_next_wave: []
deferred_separate_track: [GAP-221, GAP-256]
---

# Wave Meta-Gov 2 — Cluster Pack 6

**Wave date:** 2026-04-29 (kicked off after Wave Business Correctness SHIPPED, ~03:35 UTC)
**Cluster theme:** Meta-P1 governance — CI warning enforcement + scaffold-as-DONE docs truth-up + stale-status sync
**Strategy reference:** Per `meta-gap-priority.md` §3 (Meta-P1 ranks above Feature-P0). Three disjoint meta items bundled per `feedback_wave_pack_cross_gap_clustering.md` cluster pattern. Validates wave-pack-planner methodology a 5th time (after Wave Obs / Meta-Day-2 / DR-Backup / Business Correctness).

## Scope

| # | Gap | Title | Priority | Phase 1 slice | Agent | Disjoint files |
|:-:|-----|-------|:--------:|---------------|:-----:|----------------|
| 1 | **GAP-245** | CI enforce IDE warnings (Maven `-Xlint`) | 🟠 Meta-P1 | Phase 1 = `<profile>strict-warnings</profile>` in parent POMs + CI step uses `-P strict-warnings` (warn-mode, not Werror flip yet). Phase 2 (burndown + Werror flip) → new follow-up gap **GAP-258-werror-flipday** | A | `kiteclass/pom.xml`, `kitehub/pom.xml`, `.github/workflows/*-ci.yml` |
| 2 | **GAP-225** | Scaffolded-as-DONE governance closure umbrella | 🟠 Meta-P1 | All ACs (docs-only truth-up — no code changes per gap §"Proposed Fix — Docs-only truth-up") | B | `output-review-mandate.md` §3 line 75 + 5 gap Logs (008/009/012/015/018) — coordinator owns ROADMAP Epic 14 row |
| 3 | **GAP-224** + status-sync | `collect-state.sh` regex fix + flip GAP-202/206/207 stale Status | 🟡 Meta-P3 (224) + housekeeping | All ACs of GAP-224 + Status flip on 3 stale-IN_PROGRESS gaps to 🟢 DONE with merged-PR refs | C | `.claude/skills/workflow/start-session/scripts/collect-state.sh` + GAP-202/206/207/224 files |

## Deferred (separate track)

- **GAP-221** (P2 Meta) — GitNexus pilot. Out-of-scope: needs evaluation infra setup, not docs/config.
- **GAP-256** (P2 Meta) — Read-README-before-grep rule. Gated until 2026-05-05 per `incident-to-rule-pipeline.md` premature-rule guard (GAP-255 must be active ≥7d).
- **GAP-258-werror-flipday** (Meta-P2 follow-up) — to be filed by Agent A as Phase 2 of GAP-245 once warnings burndown inventoried. NOT this wave.

## File overlap analysis

Ran `./.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh GAP-245 GAP-225 GAP-224`:

| File | Touched by | Conflict risk | Resolution |
|------|-----------|:-------------:|------------|
| `kiteclass/pom.xml`, `kitehub/pom.xml` | A only | None | — |
| `.github/workflows/*-ci.yml` (multiple) | A only | None | Agent A picks 2-3 most relevant CI workflows |
| `.claude/rules/output-review-mandate.md` (§3 line 75 only) | B only | None | Single-line edit + Version PATCH bump |
| `documents/04-quality/gaps/GAP-008/009/012/015/018-*.md` (Log entry only, no Status flip) | B only | None | Per GAP-225 §"Proposed Fix" preserves DONE Status (audit trail) |
| `.claude/skills/workflow/start-session/scripts/collect-state.sh` | C only | None | Single-block awk regex edit |
| `documents/04-quality/gaps/GAP-{202,206,207,224}-*.md` | C only | None | Each agent edits ONLY its own gap files |
| `.claude/rules/meta-gap-priority.md` | A,B,C cite | **SOFT** | Read-only citation, no edits expected |
| `documents/04-quality/gaps/ROADMAP.md` | coordinator only | None | Coordinator handles wave-closure entry + GAP-225 Epic 14 row + 4 status-sync references |

Net: 0 HARD, 1 SOFT (read-only citation). Safe to spawn 3 agents parallel.

## Agent workflow

Per `feedback_parallel_agent_strategy.md`:

1. Each agent gets `isolation: "worktree"` (separate git checkout)
2. Branches off `main` (after this foundation PR merges)
3. Commits + creates own PR. Branch naming:
   - Agent A: `feat/wave-metagov2-gap-245-maven-werror-profile`
   - Agent B: `feat/wave-metagov2-gap-225-scaffold-truth-up`
   - Agent C: `feat/wave-metagov2-gap-224-collect-state-fix`
4. Reports back PR number + scope summary + which ACs flipped
5. Coordinator merges sequentially: A → B → C (smallest blast first; A may take longer due to CI Maven test runs)
6. Conflict resolution: 0 HARD predicted; SOFT on `meta-gap-priority.md` is read-only — no merge conflict expected
7. Wave closure ROADMAP entry after all 3 merge

## Acceptance criteria (wave-level)

- [ ] 3 PRs merged (one per gap) with green CI
- [ ] GAP-245 → 🟡 PARTIAL (Phase 1 strict-warnings profile shipped; Phase 2 → GAP-258-werror-flipday filed by Agent A)
- [ ] GAP-225 → 🟢 DONE (all docs-only ACs satisfied; Phase 2-4 are explicitly future-scope per gap §"Future scope" — no need to flip 5 affected gap Statuses)
- [ ] GAP-224 → 🟢 DONE (single regex fix, all 5 ACs satisfied)
- [ ] GAP-202/206/207 status flipped from 🟠 IN_PROGRESS → 🟢 DONE with merged-PR refs (truth-up — no own gap file)
- [ ] All gap closures pass `gap-done-discipline.md` §2 (no banned phrases)
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry (counts updated, Cluster 6/Wave Meta-Gov 2 → SHIPPED)
- [ ] ROADMAP Epic 14 Quality Governance gains GAP-225 row (per GAP-225 AC #4)
- [ ] Worktrees + branches cleaned post-merge
- [ ] `data/wave-history.jsonl` entry appended

## Wall-clock target

- Foundation PR (this doc + ROADMAP active-queue update): ~10 min (part of Wave Business Correctness closure PR — same branch, save 1 PR overhead)
- 3 parallel agents: ~30-40 min wall-clock (each ~25-30 min agent-time; A may run longer due to Maven CI Java compile)
- Sequential merge + conflict resolution: ~10 min (A goes first, may need CI re-run)
- Closure (ROADMAP + cleanup + retrospective): ~15 min
- **Total wave: ~65-75 min** (matches prior 4 waves benchmark)

## Lessons-learned (filled AFTER wave merges)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold?
- [ ] Contamination details if any:

### File-overlap accuracy
- [ ] Predicted SOFT: `meta-gap-priority.md` (citation only); actual:
- [ ] Predicted HARD: 0; actual:
- [ ] Unpredicted conflicts:

### Wall-clock
- [ ] Estimated: 65-75 min; actual: ; variance source:

### Agent prompt quality
- [ ] Clarification rounds: A=, B=, C=

### Token cost
- [ ] Total tokens: ; per gap:

### Cleanup
- [ ] Worktrees + local + remote branches cleaned

### Novel patterns
- [ ] New memory entry filed?
- [ ] Rule update proposed?

## Log

- 2026-04-29 — Wave plan created in same closure PR as Wave Business Correctness. Cluster 6 = 3 disjoint Meta-P1+P3 items. Foundation PR also closes Wave Business Correctness (#651/#652/#653 already merged) + adds Cluster 6 row to §"Active wave queue" IN_PROGRESS + appends `wave-history.jsonl` for Wave Business Correctness. After this PR merges, 3 worktree-isolated agents spawn from main per `wave-pack-planner` SKILL.md Step 5.
