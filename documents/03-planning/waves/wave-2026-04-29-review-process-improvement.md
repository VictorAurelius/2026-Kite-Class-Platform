---
title: Wave Review Process Improvement — close landing-page parity gap discovered post Wave UI Kits Round 2
status: active
created: 2026-04-29
updated: 2026-04-29
gaps: [GAP-263, GAP-264, GAP-265]
deferred_to_next_wave: []
deferred_separate_track: []
---

# Wave Review Process Improvement — Cluster Pack 12

**Wave date:** 2026-04-29 (kicked off after PR #679 landing-page hotfix exposed coverage gap in review process)
**Cluster theme:** Plug "cross-PR integration smoke test" coverage gap — review process didn't catch landing-page parity miss
**Strategy reference:** `incident-to-rule-pipeline.md` 5-stage pipeline applied to user-flagged miss "đã có UI của trang kitehub đâu nhỉ, tôi vẫn thấy 3 repo"

---

## Brainstorm (Stage 1-2 of incident-to-rule-pipeline)

### The miss
Closure PR #678 updated `ui_kits/README.md` Status table với 6 kits SHIPPED scores, NHƯNG quên `ui_kits/index.html` (landing page). User mở browser → vẫn thấy 3 cards (Wave 1) → catch.

### Why review didn't catch
| Layer | Status |
|-------|:------:|
| `output-review-mandate.md` v1.2.0 §3 row "HTML/JSX prototypes" Process | ⚠️ Manual, không enforce integration smoke test |
| `dossier/10-acceptance-criteria.md` 100-item checklist | ⚠️ Per-screen scope, không có cross-PR integration check |
| Review report PR #677 | ⚠️ Spot-check 1 file per PR, KHÔNG actually click landing page in browser |
| CLAUDE.md "use feature in browser before reporting complete" | ⚠️ Top-level rule, ad-hoc enforcement |
| Closure PR template | ❌ Không có index.html parity check |

**Coverage gap:** "Cross-PR integration smoke test" exists ở rule level (CLAUDE.md), KHÔNG có enforcement mechanism (skill / hook / script).

### Decision (chosen approach)

**3-tier solution paired with same-session enforcement parity per `rule-change-process.md` §6.5:**

- **Tier 1 (Foundation, this PR):** Quick-fix governance — script + rule extension + review template + memory update
- **Tier 2 (Agent A):** `ui-review-prototype` skill (Phase 2 of GAP-263, file as GAP-264) — automated link checker + landing parity + state coverage
- **Tier 3 (Agent B):** Hook + CI + lefthook enforcement (Phase 3 of GAP-263, file as GAP-265)

Per `feedback_wave_plan_through_pr.md`: wave plan ships first, agents spawn after merge.

---

## Scope

| # | Tier | Deliverable | Agent | Files |
|:-:|:----:|-------------|:-----:|-------|
| 1 | Tier 1 | `_shared/scripts/check-ui-kits-landing.sh` parity check | Foundation (coordinator inline) | 1 script |
| 2 | Tier 1 | `output-review-mandate.md` v1.2.0 → v1.3.0 (§3 row Process column extension) | Foundation | 1 rule edit |
| 3 | Tier 1 | `documents/04-quality/audits/ui-review/_REVIEW-TEMPLATE.md` (carved from PR #677 + Integration smoke test mandatory section) | Foundation | 1 template |
| 4 | Tier 1 | Extend memory `feedback_post_merge_doc_sync.md` — landing pages = user-facing artifacts | Foundation | 1 memory edit + MEMORY.md |
| 5 | Tier 1 | `GAP-264-ui-review-prototype-skill.md` placeholder (full scope shipped by Agent A) | Foundation | 1 gap file |
| 6 | Tier 1 | `GAP-265-ui-kits-hook-ci-enforcement.md` placeholder (full scope shipped by Agent B) | Foundation | 1 gap file |
| 7 | Tier 2 | `.claude/skills/quality/ui-review-prototype/` skill + 3 scripts + reference docs | Agent A | ~10 files |
| 8 | Tier 3 | `audit-gate.py` AUDIT_RULES extension + `.github/workflows/ui-kits-integration.yml` + lefthook config | Agent B | ~3 files |

## Deferred (separate track)
- None — all 3 tiers ship this wave.

---

## File overlap analysis

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh` | Foundation only | None |
| `.claude/rules/output-review-mandate.md` | Foundation only | None |
| `documents/04-quality/audits/ui-review/_REVIEW-TEMPLATE.md` (NEW) | Foundation only | None |
| `documents/04-quality/gaps/GAP-264-*.md` (NEW) | Foundation creates placeholder; Agent A fills | **SOFT** — Agent A appends Log + flips status; coordinator resolves at A merge |
| `documents/04-quality/gaps/GAP-265-*.md` (NEW) | Foundation creates placeholder; Agent B fills | **SOFT** — same as GAP-264 with Agent B |
| `documents/04-quality/gaps/GAP-263-*.md` | Agent A appends Phase 2 ship Log; Agent B appends Phase 3 ship Log | **SOFT** — different Log entries, sequential merge auto-merges |
| `.claude/skills/quality/ui-review-prototype/**` | Agent A only | None |
| `.claude/skills/_README-skills-index.md` | Agent A only (add new skill row) | None |
| `.claude/hooks/audit-gate.py` | Agent B only | None |
| `.github/workflows/ui-kits-integration.yml` (NEW) | Agent B only | None |
| `lefthook.yml` (extend) | Agent B only | None |
| `documents/04-quality/gaps/ROADMAP.md` | Foundation seeds row #12; coordinator wave-closure entry | None (single-author) |
| `~/.claude/projects/.../memory/feedback_post_merge_doc_sync.md` | Foundation only | None (memory location, host filesystem) |

**Net:** 0 HARD, 3 SOFT (all GAP-263/264/265 Log appends — easily resolved at sequential merge).

---

## Agent workflow

Per `feedback_parallel_agent_strategy.md`:

1. Foundation PR ships first (this PR) — wave plan + Tier 1 + GAP-264/265 placeholders. Merge before agents spawn.
2. **2 worktree-isolated agents** spawn AFTER foundation merges, branch off `main`.
3. Agent A (Tier 2) — branch `feat/wave-rev-tier-2-ui-review-prototype-skill`
4. Agent B (Tier 3) — branch `feat/wave-rev-tier-3-hook-ci-enforcement`
5. Each agent uses `isolation: "worktree"` + RELATIVE paths only.
6. Each agent verifies `pwd | grep worktrees` before any Write/commit.
7. Each agent creates own PR + reports back PR # + summary.
8. Coordinator merges sequential A → B (no HARD conflicts).
9. After both merged: closure PR (wave plan → status:complete + Lessons-learned + ROADMAP closure).

---

## Acceptance criteria (wave-level)

- [ ] Foundation PR merged with green CI
- [ ] 2 deliverable PRs (Tier 2 + Tier 3) merged sequentially with green CI
- [ ] All 3 gap files transitioned per `gap-done-discipline.md` §2:
  - GAP-263 stays 🔵 OPEN with Phase 2 Log entry referencing GAP-264 ship + Phase 3 Log entry referencing GAP-265 ship
  - GAP-264 (NEW) `🔵 OPEN → 🟢 DONE` after Agent A ships skill
  - GAP-265 (NEW) `🔵 OPEN → 🟢 DONE` after Agent B ships hook+CI+lefthook
- [ ] Tier 1 Self-test: `check-ui-kits-landing.sh` runs on current `ui_kits/` → reports 6/6 parity ✓
- [ ] Tier 2 Self-test: Agent A's 3 scripts run against current `ui_kits/` → all PASS
- [ ] Tier 3 Self-test: Agent B's hook + CI + lefthook trigger correctly on synthetic test diff
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry
- [ ] Worktrees + branches cleaned post-merge
- [ ] `data/wave-history.jsonl` entry appended

---

## Wall-clock target

- Foundation PR (this doc + Tier 1 + 2 gap placeholders): ~25 min
- 2 parallel agents: ~45-60 min wall (Agent A skill ~50 min, Agent B hook/CI ~40 min — parallel)
- Sequential merge + closure: ~20 min
- **Total wave: ~90-105 min**

---

## Lessons-learned (filled AFTER wave merges)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold for 2 agents?

### File-overlap accuracy
- [ ] Predicted SOFT (3 GAP files): {actual?}
- [ ] Unpredicted: {?}

### Wall-clock
- [ ] Estimated: 90-105 min; actual: {?}; variance source: {?}

### Self-test results
- [ ] Tier 1 script: {result on 6 kits}
- [ ] Tier 2 scripts: {result}
- [ ] Tier 3 hook/CI: {result on synthetic}

### Did the new tooling catch the original miss?
- [ ] Reproduce 2026-04-29 incident: revert `index.html` to 3-card state → run Tier 1 + Tier 2 scripts → expect FAIL
- [ ] Document outcome here

---

## Log

- **2026-04-29 (kickoff):** Wave plan created. Triggered by user-flagged miss in PR #678 closure (landing `index.html` not synced with 6 kits) → fix shipped PR #679. User: "Để miss bug này rõ ràng 3 PR chưa được Review + test đầy đủ, cần co phương án bổ sung, hoàn thiện quy trình". `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ Classify ✓ Rule+Enforce 🟡 (this wave) Self-test 🟡 (each tier includes) Retro Log 🟡 (closure).
- **(after foundation merge):** spawn 2 parallel agents (Tier 2 + Tier 3).
- **(after agents):** sequential merge, wave closure, Lessons-learned filled.
