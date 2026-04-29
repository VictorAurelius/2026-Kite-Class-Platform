---
title: Wave UI Kits Round 2 — kiteclass-pro v2 + kiteclass-parent + 5 components
status: active
created: 2026-04-29
updated: 2026-04-29
gaps: []
deferred_to_next_wave: []
deferred_separate_track: [kiteclass-teacher, ai-branding-wizard-v2, kitehub-story-v2]
---

# Wave UI Kits Round 2 — Cluster Pack 8

**Wave date:** 2026-04-29 (kicked off after Phase 0 rollback per Option A)
**Cluster theme:** Round 2 UI kit prototypes for human review — Plan B route after Claude Design access blocked
**Strategy reference:** `documents/02-architecture/design-system/dossier/` (PR #667 merged 2026-04-29) + Round 1 bundle in `/tmp/anthropic-design/` (gzipped tar from claude.ai/design)

This wave is the **first wave that doesn't close gaps** — it ships new prototype deliverables for user review. Track 2 (production port → Next.js code) will be filed as separate gaps GAP-263..267 *after* user accepts Round 2 quality.

---

## Brainstorm (per `core/brainstorming-methodology.md`)

### Q1 — Question Assumptions

**Problem:** Round 1 Claude Design bundle shipped 6 throwaway kits but missed personas, business rules, VN UX patterns, screen inventory, quality bar. Dossier (PR #667, 12 files, 2,469 LOC) augmented context. Now need Round 2 deliverables that actually USE that context to hit ≥105/128 quality target.

**Why now:**
- User chose Plan B (Claude Code) since Claude Design access is blocked by region/account gate
- User momentum — should ship Round 2 same session before context flushes
- Dossier merged 2026-04-29; freshest possible context

**Who uses output:**
- Round 2 prototypes are for **human vibe-check review** (user open browser → eyeball)
- Then port to production via Track 2 GAP-263..267 (deferred until Round 2 accepted)
- Personas served: P2 Center Owner (kiteclass-pro v2), Pa. Parent (kiteclass-parent), all (5 components used cross-kit)

**Success criteria:**
- 3 deliverables shipped (kiteclass-pro v2 + kiteclass-parent + 5 components)
- Each kit avg ≥105/128, no screen <95/128
- Mobile 320 / Tablet 768 / Desktop 1440 viewports + dark mode
- VN mock data only (per `dossier/02-vietnamese-ux-musts.md`)
- Local preview server works (`http://127.0.0.1:9999/...`)
- Auto-capture script captures screenshots for self-scoring
- 0 file conflicts at merge (worktree isolation holds)
- Wall-clock ≤90 min (vs ~3-4h serial estimate)

**Constraints:**
- Solo dev session
- No Claude Design (Plan B route)
- Stack lock per `dossier/09-tech-constraints.md` (Next.js 15 / React 19 / Tailwind 3.4 / shadcn / Radix / lucide / Framer Motion KH-only)
- HTML prototypes only — DO NOT touch `kitehub-frontend/src/**` or `kiteclass-frontend/src/**`
- Wave plan must ship via PR FIRST per `feedback_wave_plan_through_pr.md` (Phase 0 anti-pattern incident 2026-04-29)
- Dossier content fixed (12 files merged in PR #667)

### Q2 — Trade-off matrix

| Criterion | Weight | A: Wave-pack 3 agents | B: Serial coordinator | C: Hybrid (foundation + 3 agents) | D: Defer until Claude Design fixed |
|-----------|:------:|:--------------------:|:---------------------:|:---------------------------------:|:----------------------------------:|
| Wall-clock speed | 25% | 9 (~75 min) | 3 (~3-4h) | 7 (~90 min) | 0 (∞ blocking) |
| Token efficiency | 15% | 5 (3× spawn cost) | 8 | 6 | 10 |
| Aesthetic consistency cross-kit | 20% | 5 (3 separate eyes) | 9 (single coordinator) | 8 (foundation enforces) | 9 |
| User momentum | 15% | 9 | 7 | 8 | 1 (lost) |
| Methodology validation | 10% | 9 (8th wave-pack data point) | 4 | 6 | 0 |
| Worktree contamination risk | 15% | 5 (mitigated by RELATIVE paths rule) | 9 | 7 | 10 |
| **Weighted total** | **100%** | **6.85** | **6.55** | **7.10** | **5.20** |

**Choice: Alt C — Hybrid (foundation PR enforces shared infra, then 3 parallel agents).**

Rationale:
- Highest weighted score (7.10)
- Foundation PR ships `_shared/colors_and_type.css` + `_shared/assets/` + `index.html` landing + capture script — enforces cross-kit consistency
- 3 parallel agents work on disjoint kit folders (kiteclass-pro-v2 / kiteclass-parent / components) — speed benefit
- Worktree contamination mitigated by RELATIVE paths rule (`feedback_worktree_absolute_path_contamination.md`) + agent prompt template requires `pwd | grep worktrees` self-check before Write
- Aesthetic consistency: foundation locks shared tokens; agents reference same dossier files; coordinator merge sequential reviews each kit before next merge

**Rejected:**
- **A** (pure wave-pack): no foundation phase risks aesthetic drift between kits
- **B** (serial coordinator): too slow; loses parallel benefit
- **D** (defer): indefinite blocker; loses user momentum + dossier freshness

### Q3 — Decision

**Chosen approach: Hybrid — Foundation PR + 3 parallel worktree-isolated agents.**

Differs from canonical wave-pack-planner SKILL only in: this wave doesn't close gaps (new deliverable creation). Methodology applies; `analyze-overlap.sh` skipped (no gap files to parse — manual overlap matrix below).

---

## State-check (per `audit-to-gap-pipeline.md` Step 2.5)

| Path checked | What's there | Decision |
|--------------|-------------|----------|
| `documents/02-architecture/design-system/ui_kits/` (repo) | Empty (Phase 0 rolled back per user Option A) | Scaffold from foundation PR |
| `/tmp/anthropic-design/kite-design-system/project/ui_kits/kiteclass-pro/` | Round 1 v1: 21.5K JSX + 16K CSS + 1K index.html | **Copy to `ui_kits/kiteclass-pro-v2/_v1-baseline/`** — Agent A extends this |
| `/tmp/anthropic-design/.../ui_kits/kitehub-story/` | Round 1 v1: 24.5K JSX + 22.2K CSS | **Archive to `documents/07-archived/design-round-1-2026-04-29/kitehub-story/`** — preserve for future Direction A revival, not Wave 1 |
| `/tmp/anthropic-design/.../ui_kits/{ai-branding,mobile-app}/` | Round 1 v1 (Direction C playground + Direction D mobile) | **Archive same path** — Round 2 redoes from scratch (per dossier `08-direction-decisions.md`) |
| `/tmp/anthropic-design/.../ui_kits/{kiteclass,kitehub}/` | Round 1 v1 production code recreations | **Discard** — production Next.js code is authoritative, no archive value |
| `/tmp/anthropic-design/.../colors_and_type.css` | Shared design tokens | **Copy to `ui_kits/_shared/colors_and_type.css`** — single source of truth |
| `/tmp/anthropic-design/.../assets/*.svg` | kite-mark, kiteclass-logo, kitehub-logo | **Copy to `ui_kits/_shared/assets/`** |

**Net:** Round 1 bundle in `/tmp/` is at risk (system restart wipes). This wave preserves what's needed (kiteclass-pro v1 baseline) + archives what's historically useful (kitehub-story, ai-branding, mobile-app v1) + discards what's redundant.

---

## Scope

| # | Deliverable | Persona | Agent | Disjoint files |
|:-:|-------------|---------|:-----:|----------------|
| 1 | **kiteclass-pro v2** — owner dashboard with ⌘K palette + sparkline + drag-drop + dark mode polish | P2 Center Owner | A | `ui_kits/kiteclass-pro-v2/**` (5 base screens × ~4 states ≈ 20 files + README/styles/index/app.jsx) |
| 2 | **kiteclass-parent** — mobile-first 320–414px PWA-grade parent dashboard | Pa. Parent | B | `ui_kits/kiteclass-parent/**` (5 routes × ~5 states ≈ 25 files + manifest.json + sw.js + README/styles/index/app.jsx) |
| 3 | **5 Components** (G2 + G5 + G6 + G7 + G12) | All | C | `ui_kits/components/{G2-attendance-roster,G5-payment-method-selector,G6-invoice-detail,G7-parent-invite,G12-bulk-actions-bar}/` (each: 1 demo HTML × 4 states + 1 spec.md ≈ 25 files) |

## Deferred (next wave — Wave 2)

- **kiteclass-teacher** — homeroom/subject teacher dashboard (Direction B sister kit). Defer because: (a) keeps Wave 1 scope tight at 3 buckets max per `feedback_parallel_agent_strategy.md` rule #9, (b) Wave 2 better timed after kiteclass-pro v2 patterns proven.
- **ai-branding-wizard-v2** — Direction C 6-step wizard refactor. Defer for Wave 2 because: 6 wizard steps × 4 states = 24+ screens, oversized vs Wave 1 budget.
- **kitehub-story v2 polish** — Direction A marketing storytelling. Defer because: marketing track separate from product UI; user explicitly OK to defer per dossier `08-direction-decisions.md` Decision 3.

## Deferred (separate track)

- **Track 2 production port** — porting Round 2 prototypes to actual `kitehub-frontend/src/**` and `kiteclass-frontend/src/**`. File GAP-263..267 ONLY after user accepts Round 2 quality. NOT part of this wave.

---

## File overlap analysis

Manual matrix (no gap files to feed `analyze-overlap.sh`):

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` | Foundation only (read-only by A/B/C via `<link>`) | None |
| `documents/02-architecture/design-system/ui_kits/_shared/assets/*.svg` | Foundation only (linked by A/B/C) | None |
| `documents/02-architecture/design-system/ui_kits/_shared/scripts/capture-screenshots.mjs` | Foundation only | None |
| `documents/02-architecture/design-system/ui_kits/index.html` | Foundation only | None |
| `documents/02-architecture/design-system/ui_kits/README.md` | Foundation seeds; A/B/C update Status table only | **SOFT** — different rows, coordinator resolves at sequential merge |
| `documents/02-architecture/design-system/ui_kits/kiteclass-pro-v2/**` | A only | None |
| `documents/02-architecture/design-system/ui_kits/kiteclass-parent/**` | B only | None |
| `documents/02-architecture/design-system/ui_kits/components/**` | C only | None |
| `documents/03-planning/waves/wave-2026-04-29-ui-kits-round-2.md` | Foundation seeds; coordinator updates Log + Lessons-learned post-wave | None (single-author) |
| `documents/04-quality/gaps/ROADMAP.md` "Active wave queue" | Foundation seeds; coordinator wave-closure entry post-merge | None (single-author) |
| `documents/04-quality/gaps/GAP-263-html-prototype-review-standard.md` | Foundation only (NEW gap) | None |
| `documents/07-archived/design-round-1-2026-04-29/**` | Foundation only (archive of /tmp Round 1) | None |

**Net: 0 HARD conflicts, 1 SOFT conflict (`README.md` status table — trivial resolve).**

---

## Agent workflow

Per `feedback_parallel_agent_strategy.md` + `feedback_wave_plan_through_pr.md`:

1. **Foundation PR ships first** — wave plan + folder skeleton + shared infra + new gap GAP-263 + Round 1 archive. Merge before agents spawn.
2. **3 worktree-isolated agents** spawn AFTER foundation merges, branch off `main`.
3. Each agent uses `isolation: "worktree"` + RELATIVE paths only (per `feedback_worktree_absolute_path_contamination.md`).
4. Agent verifies `pwd | grep worktrees` before any Write/commit (worktree-cwd guard rule).
5. Each agent creates own PR — branch naming: `feat/wave-r2-ui-{deliverable-slug}`:
   - Agent A: `feat/wave-r2-ui-kiteclass-pro-v2`
   - Agent B: `feat/wave-r2-ui-kiteclass-parent`
   - Agent C: `feat/wave-r2-ui-components`
6. Agents report PR number + per-screen score self-estimate `/128` + acceptance check report (per `dossier/10-acceptance-criteria.md` 100-item).
7. Coordinator (me) merges sequential A → B → C — resolves SOFT conflict on `README.md` status table at C merge.
8. After all 3 merged: wave-closure ROADMAP entry + Lessons-learned section + worktree/branch cleanup.

---

## Acceptance criteria (wave-level)

- [ ] Foundation PR merged with green CI
- [ ] 3 deliverable PRs merged sequentially with green CI
- [ ] Each kit `README.md` reports avg score `/128` ≥ 105
- [ ] No screen across all 3 kits scores < 95/128
- [ ] All 3 kits cover required states: default / loading / empty / error / success / dark
- [ ] All 3 kits responsive at 320 / 768 / 1440 viewports
- [ ] All mock data Vietnamese (no Lorem ipsum, no John Doe, no $)
- [ ] WCAG AA contrast measurements documented in HTML comments per screen
- [ ] Local preview server runbook documented (`_shared/server-runbook.md`)
- [ ] Auto-capture script (`_shared/scripts/capture-screenshots.mjs`) runs without errors
- [ ] Round 1 v1 archive landed at `documents/07-archived/design-round-1-2026-04-29/`
- [ ] GAP-263 (HTML prototype review standard) filed
- [ ] No worktree contamination (per Phase 2b lessons-learned)
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry
- [ ] Worktrees + local + remote branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6
- [ ] `data/wave-history.jsonl` entry appended
- [ ] Lessons-learned section filled

---

## Wall-clock target

- Foundation PR (this doc + folder skeleton + shared infra + GAP-263 + Round 1 archive): ~30-45 min
- 3 parallel agents: ~60-90 min wall (each ~45-90 min agent-time, parallel)
- Sequential merge + SOFT conflict resolution at C: ~15-20 min
- Closure (ROADMAP + cleanup + retrospective + Lessons-learned): ~15 min
- **Total wave: ~120-180 min** (vs estimated ~3-4h serial single-coordinator = ~1.5x speedup; lower than Wave Obs 5x because Round 2 has higher per-deliverable LOC)

---

## Lessons-learned (filled AFTER wave merges)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold?
- [ ] Contamination details if any: {agents, files, recovery steps}

### File-overlap accuracy
- [ ] Predicted SOFT (`README.md` status table): {actual?}
- [ ] Predicted HARD: 0; actual: {?}
- [ ] Unpredicted conflicts: {?}

### Wall-clock
- [ ] Estimated: 120-180 min; actual: {?}; variance source: {?}

### Agent prompt quality
- [ ] Clarification rounds: A={?}, B={?}, C={?}
- [ ] Template updates needed: {?}

### Quality gate accuracy
- [ ] Self-estimate score `/128` matched user vibe-check? {?}
- [ ] WCAG AA contrast measurements verified? {?}

### Token cost
- [ ] Total tokens: {?}; per deliverable: {?}

### Cleanup
- [ ] Worktrees removed
- [ ] Local + remote branches deleted
- [ ] HTTP preview server still running? Documented?

### Novel patterns
- [ ] First wave-pack for non-gap-closing deliverable — methodology adjustments needed? {?}
- [ ] HTML prototype review standard (GAP-263) filed and applied? {?}

---

## Log

- **2026-04-29 (kickoff):** Wave plan created on branch `wave/round-2-ui-kits`. Phase 0 (uncommitted scaffold + HTTP server) was rolled back after user (Option A) flagged Superpowers compliance violations: skipped brainstorm + skipped task breakdown + violated `feedback_wave_plan_through_pr.md` (started work without wave plan PR). This wave plan is the corrective foundation — ships via PR before any agent spawns.
- **2026-04-29 (foundation PR target):** Foundation PR will land: this wave plan + folder skeleton + `_shared/` infra (colors_and_type.css + assets + capture script + server runbook) + Round 1 archive in `07-archived/` + GAP-263 (HTML prototype review standard).
- **(after merge):** spawn 3 worktree-isolated agents.
- **(after agents):** sequential merge, wave closure, Lessons-learned filled, ROADMAP updated.
