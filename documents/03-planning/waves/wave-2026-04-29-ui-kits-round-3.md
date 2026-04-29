---
title: Wave UI Kits Round 3 — kiteclass-student + kitehub-admin + 7 remaining components
status: active
created: 2026-04-29
updated: 2026-04-29
waves: [round-3-ui-kits]
gaps: []
predecessor: wave-2026-04-29-ui-kits-round-2.md
---

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

## 9. Lessons-learned (filled post-wave SHIP)

### Worktree isolation
_(filled post-wave)_

### File-overlap accuracy
_(filled post-wave)_

### Wall-clock
_(filled post-wave)_

### Agent prompt quality
_(filled post-wave)_

### Quality gate accuracy (self-scores)
_(filled post-wave)_

### Token cost
_(filled post-wave)_

### Cleanup
_(filled post-wave)_

### Novel patterns
_(filled post-wave)_

### Memory entry filed?
_(filled post-wave)_

### Rule update proposed?
_(filled post-wave)_

---

## 10. Log

- **2026-04-29 (kickoff):** Wave plan created on branch `wave/round-3-ui-kits` per `feedback_wave_plan_through_pr.md` PR-first governance + `feedback_phase_0_governance_violation.md` no-scaffolding-before-plan rule. Predecessor: Round 2 wave plan (`wave-2026-04-29-ui-kits-round-2.md`) shipped 7 deliverables × 76 screens × avg 110.5/128 + Wave Review Process Improvement (GAP-263/264/265 all DONE). Round 3 fills remaining persona × direction matrix gaps (kiteclass-student + kitehub-admin) + closes 7/12 component-gaps catalog (G1/G3/G4/G8/G9/G10/G11). After this PR merges, 4 parallel agents spawn per §6.
