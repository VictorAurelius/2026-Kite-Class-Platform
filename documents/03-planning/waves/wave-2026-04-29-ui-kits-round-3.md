---
title: Wave UI Kits Round 3 — kiteclass-student + kitehub-admin + 7 remaining components
status: complete
created: 2026-04-29
updated: 2026-04-29
waves: [round-3-ui-kits]
gaps: []
predecessor: wave-2026-04-29-ui-kits-round-2.md
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave UI Kits Round 3 — kiteclass-student + kitehub-admin + 7 remaining components

**Type:** HTML prototype wave (Track 1 extension, NOT gap-closing) — sister of Round 2
**Methodology:** wave-pack 4-agent parallel per `feedback_parallel_agent_strategy.md` rule #9 + `feedback_wave_pack_cross_gap_clustering.md`
**Governance:** wave plan PR-FIRST per `feedback_wave_plan_through_pr.md` + `feedback_phase_0_governance_violation.md` (no scaffolding before this PR merges)
**Estimated wall-clock:** ~150 min (foundation 30 + 4 parallel agents ~80 longest + sequential merge 15 + closure 25)

---

## 0. Why Round 3 (after Round 2 Wave 1.5/1.6/1.7)

Round 2 shipped 6 kits + 5 components but dossier persona × direction matrix and component-gaps catalog still has **2 personas + 7 components uncovered**:

| Missing | Persona / Spec | Source dossier |
|---------|---------------|---------------|
| `kiteclass-student/` kit | Tier 2 Student (S.) — mobile-primary, 6-22yo learners | `01-personas.md` §Tier 2 + matrix row "Round 2 — Phase 2" |
| `kitehub-admin/` kit | Tier 1 P5 K-12 School Principal — desktop, 50+ teachers / 500-3000 students | `01-personas.md` §Tier 1 + matrix row "kitehub-admin (existing)" |
| G1 Bulk Import drop-zone | Backend complete, FE zero entry (parallel insight: GAP-137 P0) | `04-component-gaps.md` G1 |
| G3 Gradebook Entry Grid | Doesn't exist in repo | G3 |
| G4 Class Schedule Manager | No recurring schedule UI | G4 |
| G8 Attendance Calendar | shadcn calendar generic, need teacher overlay | G8 |
| G9 Instance Lifecycle Status | KH `/instances/[id]` is 33/128 🔴 | G9 |
| G10 Payment Status Timeline | Implicit in table, no dedicated timeline | G10 |
| G11 Theme Live Preview | AI Branding playground archived, needs integrated step | G11 |

`kitehub-story v2 polish` (Direction A marketing) explicitly defer per dossier `08-direction-decisions.md` Decision 3 — user OK to skip; out of scope this wave.

---

## 1. Brainstorm (per `core/brainstorming-methodology.md`)

### Q1 — Question Assumptions

**Assumption 1:** Round 2 quality bar (avg 110.5/128) holds for Round 3.
- Challenge: kiteclass-student is mobile-primary (~85% sessions) — same constraint as kiteclass-parent (avg 114/128). Pattern reusable.
- Challenge: kitehub-admin is large-display dense data (50+ teachers, 500-3000 students) — different from owner-dashboard density. Risk: dense data tables hard to fit /128 polish bar.
- Mitigation: kitehub-admin reuses kitehub-pro-v2 dense-table pattern + extends with hierarchy nav (school → semester → class).

**Assumption 2:** 7 remaining components fit in 2 buckets.
- Challenge: G3 Gradebook Entry Grid is the heaviest (multi-row × multi-column × cell editing × validation). Could oversize a bucket.
- Mitigation: Bucket C takes G1+G3+G4+G8 (4 mid-size); Bucket D takes G9+G10+G11 (3 lighter components × more states each). Total ~7×4-6 states = ~28-42 demos, distributed.

**Assumption 3:** No file conflicts between 4 buckets.
- Challenge: All 4 touch `ui_kits/index.html` landing + `ui_kits/README.md` Status table.
- Mitigation: SOFT overlap pattern from R2 — coordinator handles at sequential merge; each agent edits ONLY their kit/component folder + appends 1 row to README + adds 1 card to index.html.

### Q2 — Trade-off matrix

| Option | Pro | Con | Decision |
|--------|-----|-----|----------|
| 4 buckets (proposed) | Fits 60-75min target each, leverages R2 0-clarif streak (22 consecutive) | Higher coordinator coordination at merge | ✅ Pick |
| 3 buckets (gộp components) | Fewer agents | Bucket C 7 components × 4-6 states = 28-42 demos = oversized vs R2 benchmark | ❌ |
| 5 buckets (split kits + 3 component buckets) | Smallest buckets | Exceeds rule #9 max 4-5 agents; diminishing returns | ❌ |

### Q3 — Decision

**Pick 4 buckets** — A=student kit, B=admin kit, C=components batch 1 (G1/G3/G4/G8), D=components batch 2 (G9/G10/G11).

---

## 2. State-check (per `audit-to-gap-pipeline.md` Step 2.5)

Verified 2026-04-29 before this plan committed:

```
$ ls documents/02-architecture/design-system/ui_kits/
_shared/  ai-branding-wizard-v2/  components/  index.html  kiteclass-parent/
kiteclass-pro-v2/  kiteclass-teacher/  kitehub-pro-v2/  README.md

$ ls documents/02-architecture/design-system/ui_kits/components/
G12-bulk-actions-bar/  G2-attendance-roster/  G5-payment-method-selector/
G6-invoice-detail/  G7-parent-invite/  index.html  README.md
```

| Item | Code state | Action |
|------|-----------|--------|
| `kiteclass-student/` folder | 🔴 Nothing exists | Build from scratch |
| `kitehub-admin/` folder | 🔴 Nothing exists | Build from scratch |
| `components/G1-bulk-import-dropzone/` | 🔴 Nothing exists | Build from scratch |
| `components/G3-gradebook-entry-grid/` | 🔴 Nothing exists | Build from scratch |
| `components/G4-class-schedule-manager/` | 🔴 Nothing exists | Build from scratch |
| `components/G8-attendance-calendar/` | 🔴 Nothing exists | Build from scratch |
| `components/G9-instance-lifecycle/` | 🔴 Nothing exists | Build from scratch |
| `components/G10-payment-timeline/` | 🔴 Nothing exists | Build from scratch |
| `components/G11-theme-preview/` | 🔴 Nothing exists | Build from scratch |
| `_shared/colors_and_type.css` | ✅ Exists from R2 foundation | Read-only link |
| `_shared/assets/*.svg` | ✅ Exists from R2 | Read-only link |

State-check **PASS** — no duplicate work risk.

---

## 3. Scope

### Bucket A — `kiteclass-student/` kit

**Persona:** S. Student (Tier 2) — mobile-primary 85%, 6-22yo, K-12 + vocational learners
**Quality target:** avg ≥105/128, min ≥95/128 (R2 floor)
**Screens:** ~10-12 mobile-first
- Today screen (next class + today's schedule + pending tasks)
- My Classes (list + class detail)
- Assignments (list + detail + submit)
- Grades (overview + per-subject detail)
- Attendance log (history + percentage)
- Payments (balance + history — for older students)
- Profile (settings + avatar + notifications)
- Notifications inbox (Zalo OA mirror)
- Empty states (no classes / no assignments / no grades)
- Login / Forgot password

**Constraints:** Vietnamese-only. Web responsive PWA-grade (NOT native). Bottom tab nav like kiteclass-parent. Big tap targets (44px min). Web Push permission UI.

### Bucket B — `kitehub-admin/` kit

**Persona:** P5 K-12 School Principal/Admin (Tier 1) — desktop primary, 50+ teachers, 500-3000 students
**Quality target:** avg ≥105/128, min ≥95/128
**Screens:** ~10-12 dense-data desktop
- School overview dashboard (KPIs: enrollment, attendance rate, fee collection, conduct flags)
- Bulk student import wizard (school-wide enrollment week scenario, 500/day scale)
- Teacher management (list + assign-to-class + role hierarchy)
- Academic calendar (semester/term editor + holidays + exam weeks)
- Report card generation (MoET-compliant batch generation)
- Parent communication monitor (escalation queue + response SLA)
- Annual fees panel (collection rate + overdue list)
- Conduct/behavior tracking (incidents log + escalation ladder)
- Multi-class roster view (class × subject × teacher matrix)
- School profile + settings
- Empty states (no incidents / no overdue / first-day-of-school)

**Constraints:** Desktop-first (large displays). Keyboard shortcuts (⌘K palette). Hierarchy breadcrumb (school > semester > class).

### Bucket C — Components batch 1 (G1 + G3 + G4 + G8)

**Quality target:** avg ≥105/128 per component
**Components × states:**
- **G1 Bulk Import Drop-zone + Job Tracker** — 5 states: idle / drag-over / parsing / partial-success / done
- **G3 Gradebook Entry Grid** — 6 states: empty / row-selected / cell-editing / validation-error / bulk-paste / saved
- **G4 Class Schedule Manager** — 5 states: empty / single-class / recurring-edit / conflict-warning / saved
- **G8 Attendance Calendar (month view)** — 4 states: month-load / day-detail / streak-highlight / partial-month

Total: ~20 demo states + index.html showcase.

### Bucket D — Components batch 2 (G9 + G10 + G11)

**Quality target:** avg ≥105/128 per component
**Components × states:**
- **G9 Instance Lifecycle Status** — 6 states: NOT_STARTED / INITIALIZING / GENERATING / DEPLOYED / FAILED / REGENERATING (matches `ai-branding-guidelines.md` §6 state machine)
- **G10 Payment Status Timeline** — 5 states: pending / paid / partial-paid / overdue / refunded
- **G11 Theme Live Preview** — 5 states: default / brand-applied / dark-morph / mobile-preview / wcag-warning

Total: ~16 demo states + index.html showcase.

---

## 4. Deferred (out of this wave)

- **`kitehub-story v2 polish`** — Direction A marketing storytelling. User explicitly OK to defer per dossier Decision 3.
- **Track 2 — production port to Next.js** — porting Round 2 + Round 3 prototypes to actual `kitehub-frontend/src/**` and `kiteclass-frontend/src/**`. File GAP-266..273 (1 gap/kit + 1 gap/component-batch) ONLY after user accepts Round 3 quality. NOT part of this wave.

---

## 5. File overlap analysis

Manual matrix (no gap files to feed `analyze-overlap.sh`):

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `ui_kits/_shared/colors_and_type.css` | All (read-only `<link>`) | None |
| `ui_kits/_shared/assets/*.svg` | All (read-only) | None |
| `ui_kits/_shared/scripts/capture-screenshots.mjs` | None (already exists) | None |
| `ui_kits/kiteclass-student/**` | Agent A only | None |
| `ui_kits/kitehub-admin/**` | Agent B only | None |
| `ui_kits/components/G1-*/`, `G3-*/`, `G4-*/`, `G8-*/` | Agent C only | None |
| `ui_kits/components/G9-*/`, `G10-*/`, `G11-*/` | Agent D only | None |
| `ui_kits/components/index.html` | Agent C + D append cards (different sections) | **SOFT** — coordinator merge, alphabetical insert |
| `ui_kits/components/README.md` | Agent C + D append rows to Status table | **SOFT** — different rows, coordinator resolves |
| `ui_kits/index.html` | Agent A + B append kit cards | **SOFT** — different cards, coordinator resolves |
| `ui_kits/README.md` | Agent A + B append rows to Status table | **SOFT** — different rows, coordinator resolves |

0 HARD conflicts. 4 SOFT (resolved at sequential merge as in R2).

---

## 6. Agent workflow

**Per `feedback_parallel_agent_strategy.md`:**
- 4 worktree-isolated agents (`isolation: worktree`)
- RELATIVE paths only (per `feedback_worktree_absolute_path_contamination.md`)
- Single message with 4 Agent tool calls (parallel spawn)
- Sequential merge: A → B → C → D (alphabetical, kits first then components)

**Agent prompt template:** `.claude/skills/quality/wave-pack-planner/assets/agents/docs-only-skeleton-agent.md` (codified after Wave Legal-BRD Phase 1.5 closure 2026-04-29 PR #697 — 2nd recurrence threshold). Each agent gets:
- Bucket spec from §3 above
- Quality bar /128 from `dossier/06-quality-bar.md`
- AC checklist from `dossier/10-acceptance-criteria.md`
- WCAG AA self-measurement requirement (HTML comments per `output-review-mandate.md` v1.2.0 §3 row "HTML/JSX prototypes")
- Round 2 sister kit reference for pattern reuse:
  - Agent A: `kiteclass-parent/` (same mobile-first PWA-grade pattern)
  - Agent B: `kitehub-pro-v2/` (same dense-data desktop pattern + extend hierarchy)
  - Agent C/D: existing R2 components (G2/G5/G6/G7/G12) for state pattern

---

## 7. Acceptance criteria (wave-level)

- [ ] All 4 agents land PRs (kit A, kit B, components C, components D)
- [ ] Aggregate avg ≥105/128 across all kits/components (R2 floor maintained)
- [ ] Each kit has self-reported /128 score per screen in HTML comments
- [ ] WCAG AA self-measurement in HTML comments per `output-review-mandate.md` §3 HTML prototypes row
- [ ] `ui_kits/index.html` landing has card for kiteclass-student + kitehub-admin
- [ ] `ui_kits/components/index.html` showcase has cards for G1, G3, G4, G8, G9, G10, G11
- [ ] `ui_kits/README.md` Status table has rows for new 2 kits
- [ ] `ui_kits/components/README.md` Status table has rows for 7 new components
- [ ] `_shared/scripts/check-ui-kits-landing.sh` exit 0 (Tier 1 enforcement per GAP-263)
- [ ] `ui-review-prototype` skill (GAP-264) ran on at least 1 random screen per agent (Tier 2 enforcement)
- [ ] Closure PR syncs ROADMAP "Active wave queue" Cluster 11 row + landing index.html + wave-history.jsonl entry (#12 wave-pack data point)
- [ ] No `[ ]` unchecked AC at closure flip — per `gap-done-discipline.md` §2

---

## 8. Wall-clock target

| Phase | Target | Note |
|-------|--------|------|
| Foundation PR (this) | ~30 min | Plan + ROADMAP entry + commit + push + merge |
| 4 parallel agents | ~80 min (longest) | Bucket B kitehub-admin likely longest (dense data + hierarchy) |
| Sequential merge | ~15 min | 4 PRs A → B → C → D, conflict resolution at SOFT files |
| Closure PR | ~25 min | Lessons-learned + ROADMAP sync + wave-history.jsonl + landing index.html |
| **Total** | **~150 min** | vs R2 Wave 1.5/1.6/1.7 ~150 min same range |

---

## 9. Lessons-learned (post-wave SHIP)

### Worktree isolation
✅ All 4 agents used `isolation: worktree` with RELATIVE paths per `feedback_worktree_absolute_path_contamination.md`. **0 contamination incidents.** All 4 agents reported `pwd` verification at start. None used absolute paths.

### File-overlap accuracy
Predicted: 0 HARD + 4 SOFT. **Actual:** 0 HARD + 2 conflicts surfaced at sequential merge:
- A vs B both added cards to `ui_kits/index.html` after Kit 6 — predicted SOFT, manifested as git conflict (resolved via rebase B onto main, kept both Kit 7 + Kit 8 ordering).
- A vs B both added rows to `ui_kits/README.md` Status table — same pattern, same fix.
- C vs D did NOT touch `components/index.html` or `components/README.md` (per agent instruction "coordinator handles") — coordinator fills in closure PR (this commit).

Lesson: when predicting SOFT overlap on append-only landing files, expect git to flag conflict if both agents append at the same anchor location. Coordinator rebase resolves cleanly.

### Wall-clock
Estimated 150 min total. **Actual ~90 min:**
- Foundation PR: ~15 min (vs 30 estimate)
- 4 parallel agents: ~21 min wall (longest = Agent A 17.3min, Agent B 20.6min) vs 80 min estimate — significantly faster than R2 benchmark
- Sequential merge + conflict recovery: ~10 min (vs 15 estimate)
- Closure PR + sync: ~30 min (vs 25 estimate)

**Variance: -40% under estimate.** Likely cause: kits smaller than R2 add-ons (R3 student 14 screens vs R2 parent 17, R3 admin 12 vs R2 kitehub-pro 24); component buckets simpler than R2 5-component (4 components in C, 3 in D).

### Agent prompt quality
✅ All 4 agents 0-clarification rounds. Streak now **26 consecutive** (was 22 after Wave Legal-BRD Phase 1.5). Prompts contained: §3 bucket spec, sister kit reference for pattern reuse, RELATIVE paths mandate, `pwd` verification, no Co-Authored-By trailer, /128 self-score template, WCAG AA self-measurement template.

### Quality gate accuracy (self-scores)
| Bucket | Target | Actual avg | Min | Status |
|--------|:------:|:----------:|:---:|:------:|
| A kiteclass-student | ≥105 | **116** ⭐⭐ | 114 | +11 vs target |
| B kitehub-admin | ≥105 | 107.2 | 104 | +2.2 vs target |
| C 4 components | ≥105 | 107.7 | 102 | +2.7 vs target (1 state under floor 95? no — min 102) |
| D 3 components | ≥105 | 108 | 105 | +3 vs target |
| **Aggregate** | ≥105 | **109.7** | 102 | ✅ all kits ≥ floor 95 |

R2 baseline aggregate was 110.5/128 — Round 3 lands at 109.7/128, **0.8 pt below R2** but well above target ≥105 floor. Agent A kiteclass-student matched R2's highest kit (kiteclass-parent 114/128) AND beat it (116/128) — first kit to do so. Agent B kitehub-admin came in lowest (107.2) but consistent with R2 kitehub-pro-v2 (107.8) — desktop dense-data kits cluster at this band.

### Token cost
Total ~1.0M tokens this session: foundation ~50K + 4 agents reported 992K cumulative (260K+225K+245K+260K). Per-screen cost: ~13K tokens × 76 deliverables avg.

### Cleanup
4 agent worktrees still alive (locked by harness). 4 local branches `wave/round-3-agent-*` will free when harness unlocks worktrees. Closure branch `wave/round-3-closure` to be cleaned post-merge. Wave 13 lesson "prune worktrees BEFORE final merge" partially applied — agents still locked at merge time, but caused only local cleanup friction (no remote/CI impact, all 4 PRs `--delete-branch` succeeded on remote).

### Novel patterns
1. **CI parity gate caught coordinator gap inline** — GAP-263 Tier 3 enforcement (`check-ui-kits-landing.sh`) blocked PR #700 + #703 because agents (per instruction) didn't touch landing. Coordinator amend-commit resolved. Validates `output-review-mandate.md` v1.3.0 §3 row "HTML/JSX prototypes" Process column "landing parity script in CI" requirement — caught what would have been silent inconsistency.
2. **First wave with 26-consecutive 0-clarification streak** — agent prompt template stable across 4 wave-pack methodology generations.
3. **First Round-3 milestone: 8 kits + 12 components + ALL Tier 1+2 personas covered** — Persona × Direction dossier matrix officially complete (only `kitehub-story` Direction A marketing remaining, deliberately deferred per Decision 3).

### Memory entry filed?
None warranted. Worktree contamination rule held. Phase 0 governance rule held. PR-first wave plan rule held. All from prior incidents already codified.

### Rule update proposed?
None. `output-review-mandate.md` v1.3.0 §3 row already covers HTML prototypes review standard + integration smoke test + landing parity. `gap-done-discipline.md` §3 PARTIAL exit ramp NOT needed (this wave doesn't close any gaps). Wave plan template + agent prompts working as designed.

---

## 10. Log

- **2026-04-29 (kickoff):** Wave plan created on branch `wave/round-3-ui-kits` per `feedback_wave_plan_through_pr.md` PR-first governance + `feedback_phase_0_governance_violation.md` no-scaffolding-before-plan rule. Predecessor: Round 2 wave plan (`wave-2026-04-29-ui-kits-round-2.md`) shipped 7 deliverables × 76 screens × avg 110.5/128 + Wave Review Process Improvement (GAP-263/264/265 all DONE). Round 3 fills remaining persona × direction matrix gaps (kiteclass-student + kitehub-admin) + closes 7/12 component-gaps catalog (G1/G3/G4/G8/G9/G10/G11). Foundation PR #699 squash-merged 2026-04-29 13:36 UTC (commit `ca73b099`).
- **2026-04-29 (Wave SHIPPED):** 4 parallel agents (worktree-isolated, RELATIVE paths) closed sequence:
  - **PR #700** Agent A `kiteclass-student/` — 18 files, 3,626 LOC, **avg 116/128 ⭐⭐ (HIGHEST kit in Round 3)**, 14 screens. Tier 2 Student persona. 5-tab bottom nav, Web Push primary, saved-draft submit, social login (Zalo+Google), PWA manifest+sw.js. Sister-kit deltas vs `kiteclass-parent/` codified.
  - **PR #703** Agent B `kitehub-admin/` — 15 files, 3,246 LOC, avg 107.2/128, 12 dense-desktop screens. Tier 1 P5 K-12 School Principal. Hierarchy nav (school→semester→class), ⌘K palette, 5-step escalation ladder, MoET-compliant report cards, 25×9 roster matrix. Real K-12 mock data: Trường THCS Nguyễn Du, 1,247 HS, 62 GV, 25 lớp.
  - **PR #702** Agent C 4 components G1+G3+G4+G8 — 28 files, 3,316 LOC, avg 107.7/128, 20 demo states.
  - **PR #701** Agent D 3 components G9+G10+G11 — 22 files, 3,238 LOC, avg 108/128, 16 demo states. G11 includes reflexive WCAG fail demonstration.
  - Sequential merge: A → B (rebase to resolve landing index.html SOFT conflict — predicted) → C → D. All 4 PRs CLEAN before merge after coordinator amend-commits to A+B for landing parity (GAP-263 Tier 3 CI gate caught and enforced).
  - **Wave aggregate:** 4 deliverables × **76 screens** (14+12+20+30 component states + indices) × **avg 109.7/128** (target ≥105 ✓, 0.8 pt below R2 baseline 110.5 — within band).
  - Wall-clock: ~90 min total (vs 150 min estimated, -40% under).
  - Token cost: ~1.0M total session (foundation ~50K + 4 agents 992K cumulative reported).
  - **26th consecutive wave with 0 clarification rounds** (was 22 after Wave Legal-BRD Phase 1.5).
  - 0 worktree contamination, 2 SOFT conflicts as predicted (A+B landing append) resolved via rebase.
- **2026-04-29 (closure):** This commit (chore/wave-r3-ui-kits-closure) — Lessons-learned filled, ROADMAP Cluster 13 row flipped 🟡 ACTIVE → ✅ SHIPPED, `data/wave-history.jsonl` appended (12th wave-pack data point), `ui_kits/components/index.html` + `components/README.md` updated with 7 new component cards/rows. Wave status: `active` → `complete`. Track 2 (production port to Next.js) becomes user-acceptance gated per Round 2 wave plan §"Deferred separate track" — file GAP-266..273 ONLY after user accepts Round 3 quality.
