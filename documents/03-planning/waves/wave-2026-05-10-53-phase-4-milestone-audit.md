---
title: Wave 53 — Phase 4 Milestone Audit Suite (UI /128 + Quality /110 + Performance /100)
status: complete
created: 2026-05-10
updated: 2026-05-10
waves: [53]
gaps: [GAP-462]
parent_obligation: post-wave-audit-mandate.md §2.4.2 phase-4-kit-ports milestone
phase_reference: Phase 4 closure (Track 2)
---

# Wave 53 — Phase 4 Milestone Audit Suite

**Goal:** Đóng `phase-4-kit-ports` domain-milestone audit obligation per `post-wave-audit-mandate.md` §2.4.2 — chạy 3 audit suite (UI /128 + Quality /110 + Performance /100) → flip 7 Phase 4 kits 🟡 PARTIAL → 🟢 DONE (where scores meet thresholds) → close GAP-462. Unblock critical-path step 1 toward Phase 1 BETA launch.

**Trigger:** User chose "Wave 53 plan + spawn audit suite (3 subagents)" 2026-05-10 sau khi Wave 50 + 51 closure shipped. Audit prep checklist already produced via Explore agent earlier session.

**Estimated wall-clock:** ~1.5-2h longest path (3 agents parallel; UI /128 capture + score là longest path do 144 screens × 7 kits).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- Phase 4 Track 2 ports tất cả 7 PARTIAL sau Wave 50+51; cần per-screen audit để flip DONE
- `post-wave-audit-mandate.md` §2.4 milestone audit obligation triggered tại Wave 50 closure (deferred via `AUDIT_DEFER_DOMAIN_MILESTONE` trailer + GAP-462)
- Phase 1 BETA launch critical-path step 1 = Phase 4 milestone audit; step 2+ depend on this
- Quality /100 currently 86 ✅ Phase 2 trigger; refresh để verify post-Wave-49+50+51 không regression

**Q2 (trade-offs):**
- **Đã xét:** sequential 3 audits 1 agent → REJECT vì wall-clock ~5-6h vs ~1.5-2h parallel
- **Đã xét:** spawn 5+ agents (split UI by kit / split Quality by category) → REJECT vì max-cap 5 + audit category boundaries are natural unit; over-parallelization tăng coordinator overhead
- **Đã xét:** include Lighthouse PWA measurement → REJECT vì requires HTTPS staging (deferred per Wave 49 GAP-267a/269c follow-ups; localhost Lighthouse returns 0/100 PWA always)
- **Đã xét:** include E2E score in Quality Cat 1 → PARTIAL: 3 Playwright specs shipped Wave 51 Bucket A, GAP-268b DONE; 267a/269c PARTIAL → score Cat 1 based on coverage shipped, document deferred items
- **Chọn:** 3 buckets parallel (A UI / B Quality / C Performance); each agent produces report file in `documents/04-quality/audits/{ui,quality,performance}/`; closure PR aggregates + flips PARTIAL→DONE per scoring

**Q3 (rủi ro):**
- **R1 — Dev server boot dependency**: UI capture cần `pnpm dev` running on port 4700 (kiteclass) + 4701 (kitehub). → AC Bucket A: agent boot dev servers trong worktree; nếu collision với existing Phase 1 BETA dev session, agent uses ephemeral high port + override env. Recovery: nếu boot fail, fallback static prototype HTML capture (still valuable for delta vs production).
- **R2 — Capture script Playwright dependency**: cần chromium cached. → AC Bucket A: `npx playwright install chromium` step nếu chưa cached.
- **R3 — Audit reports conflict on file paths**: 3 agents ghi 3 reports vào `documents/04-quality/audits/{ui,quality,performance}/` cùng commit cycle → potential merge conflict trong closure PR. → AC: each bucket commits to its own subdir + own filename `YYYY-MM-DD-wave-53-<scope>.md`; closure PR aggregates without overlap.
- **R4 — Score regression from Wave 40 baseline**: refresh có thể reveal regression (broken AC hoặc CVE accumulation). → AC: closure PR honest about delta; file P0/P1 follow-up gaps per `audit-to-gap-pipeline.md` §3 nếu regressed below threshold (Quality <80 or Performance <70).
- **R5 — Persona Coverage Cat 11 = 5/10 placeholder**: GAP-152 chưa ship first persona reports → Quality /110 có tối đa 105/110. → AC Bucket B: document Cat 11 = 5/10 in report (data-pending); aggregate score = X/100 with Cat 11 explicitly N/A or placeholder.
- **R6 — Phase 4 PARTIAL → DONE threshold**: per Wave 49+50+51 plan §3, threshold ≥105/128 per screen for kit DONE. → AC Bucket A: report includes per-kit per-screen scoring + DONE-eligibility verdict; closure PR uses verdicts to flip GAP-267..272 PARTIAL → DONE where ALL screens ≥105.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | UI /128 — 7 Phase 4 kits, ~144 screens, capture + score + report | bg-agent | ~1.5-2h | ✅ writes only `documents/04-quality/audits/ui/` |
| B | Quality /110 — cross-system refresh delta vs Wave 40 86/100 | bg-agent | ~45-60min | ✅ writes only `documents/04-quality/audits/quality/` |
| C | Performance /100 — DB grep + bundle analysis + cache config | bg-agent | ~30-45min | ✅ writes only `documents/04-quality/audits/performance/` |

**Disjoint check:** 3 different audit subdirs → zero file overlap ✅. All read-only on source code (no edits to `kiteclass-*` / `kitehub-*` / shared-ui).

---

## 3. Scope (compact schema)

**Stake tier:** **HIGH** — milestone audit obligation governance + Phase 1 BETA gating. Model: **Opus 4.7** mỗi agent (audit interpretation requires deep reasoning).
**Cross-layer? (per `contract-first-for-cross-layer.md`):** **NO** — pure audit; reads source + writes reports.

| # | Bucket | Scope | Files (output glob) | Spawn order |
|:-:|--------|-------|---------------------|:-----------:|
| 1 | **A — UI /128** | 7 kits × ~144 screens; per-kit per-screen scoring; aggregate kit verdict | `documents/04-quality/audits/ui/2026-05-10-wave-53-phase-4-milestone.md` + `manifest.md` | parallel |
| 2 | **B — Quality /110** | 11 categories refresh kitehub + kiteclass; delta vs Wave 40 86/100 | `documents/04-quality/audits/quality/2026-05-10-wave-53-quality-refresh.md` | parallel |
| 3 | **C — Performance /100** | 5 categories: DB Query / API Response / FE Bundle / Caching / Resource Utilization | `documents/04-quality/audits/performance/2026-05-10-wave-53-performance-refresh.md` | parallel |

### Bucket A — UI /128 audit (7 kits)

- Skill: `.claude/skills/quality/ui-review/SKILL.md`
- Capture script: `scripts/capture-ui-all.sh --label wave-53-milestone` (or per-app: `kiteclass-frontend/scripts/capture-screenshots.ts` + `kitehub-frontend/scripts/capture-screenshots.ts`)
- 7 kits to audit (per audit prep checklist):

| # | Kit | Gap | Route prefix | Baseline /128 |
|---|-----|-----|--------------|--------------|
| 1 | kc-parent | GAP-267 | `(dashboard)/parent/*` | 114.4 |
| 2 | kc-teacher | GAP-268 | `(teacher)/teacher/*` | 107.8 |
| 3 | kc-student | GAP-269 | `(dashboard)/student/*` | 116.2 |
| 4 | kc-owner-pro | GAP-266 | `/dashboard` + owner CRUD | 108.4 |
| 5 | kh-pro | GAP-270 | `(customer)/*` | 107.8 |
| 6 | kh-admin | GAP-271 | `(school-admin)/*` | 117.5 |
| 7 | ai-branding-wizard-v2 | GAP-272 | `(customer)/branding/wizard/**` | 116.0 |

- Scoring rubric: 5 dimensions /128 (Technical /20 + Design /40 + Aesthetics /28 + UX /20 + WCAG /20)
- Acceptance:
  - Per-screen scores in report
  - Per-kit aggregate (avg + min + max)
  - DONE-eligibility verdict per kit (ALL screens ≥105/128 = eligible flip PARTIAL→DONE)
  - Sub-gaps filed per `audit-to-gap-pipeline.md` §3 cho any screen <105
  - Manifest `documents/screenshots/wave-53-milestone/manifest.md` committed (PNGs gitignored)
  - Lighthouse DEFERRED note (HTTPS staging follow-up per GAP-267a/269c)

### Bucket B — Quality /110 audit refresh

- Skill: `.claude/skills/quality-audit/SKILL.md`
- Categories: 1 E2E + 2 Security + 3 Backend Tests + 4 Frontend Tests + 5 CI/CD + 6 UI/UX + 7 DevOps + 8 Docs + 9 Code Quality + 10 PM + 11 Persona Coverage
- Scope: kitehub + kiteclass + shared-ui
- Acceptance:
  - Per-category /10 score
  - Aggregate /110 (or /100 mapping if Cat 11 marked placeholder)
  - Delta vs Wave 40 baseline 86/100
  - Note Cat 11 Persona Coverage = 5/10 placeholder (GAP-152 data-pending) per Wave 53 plan §1 Q3 R5
  - Sub-gaps filed cho findings <70/100 per category
  - FE-heavy Phase 4 focus on Cat 4 (Frontend Tests: vitest + Playwright Wave 51 specs) + Cat 6 (UI/UX)

### Bucket C — Performance /100 audit refresh

- Skill: `.claude/skills/quality/performance-audit/SKILL.md`
- 5 categories × /20 each: DB Query Efficiency / API Response Time / FE Bundle / Caching Strategy / Resource Utilization
- Methods:
  - DB grep: `grep -rn "N+1\|findAll" kiteclass-core kitehub --include="*.java"` + scan repository methods cho missing pagination
  - Bundle analysis: `pnpm -F kiteclass-frontend build` + `pnpm -F kitehub-frontend build` extract route-size summary
  - Cache config: `grep -rn "spring.data.redis\|@Cacheable" kitehub --include="*.yml" --include="*.java"`
  - API response: review Spring controller patterns (no live API call needed since AWS stack stopped)
- Acceptance:
  - Per-category /20 score
  - Aggregate /100
  - Delta vs Wave 40 baseline 75/100
  - Sub-gaps filed for findings <12/20 per category
  - FE bundle size table per app + per route

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

Verified 2026-05-10 trước khi draft plan:

| Symbol | Type | Verification | Verdict |
|--------|------|------|---------|
| `.claude/skills/quality/ui-review/SKILL.md` | Skill | (per CLAUDE.md skills index) | ✅ exists |
| `.claude/skills/quality-audit/SKILL.md` | Skill | (per CLAUDE.md skills index) | ✅ exists |
| `.claude/skills/quality/performance-audit/SKILL.md` | Skill | (per CLAUDE.md skills index) | ✅ exists |
| `scripts/capture-ui-all.sh` | Capture script | per audit prep checklist | ✅ exists (reference) |
| `kiteclass-frontend/scripts/capture-screenshots.ts` | Per-app capture | per audit prep checklist | ✅ exists |
| `kitehub-frontend/scripts/capture-screenshots.ts` | Per-app capture | per audit prep checklist | ✅ exists |
| `documents/04-quality/audits/ui/` | Output dir | per existing convention | ✅ exists (ROADMAP refs Wave 40 reports) |
| `documents/04-quality/audits/quality/` | Output dir | per Wave 40 baseline reference | ✅ exists |
| `documents/04-quality/audits/performance/` | Output dir | per Wave 40 baseline reference | ✅ exists |
| 7 Phase 4 production routes (kc-parent + kc-teacher + kc-student + kc-owner + kh-pro + kh-admin + ai-branding-wizard) | FE routes | per Wave 49+50 SHIPPED ROADMAP §🚀 | ✅ exists |
| Wave 40 baselines (Quality 86, Performance 75, Security 87, Ops 60, API Contract 72, Business Logic 68, UI 111.3) | Audit reports | per ROADMAP §🚀 reference | ✅ exists (delta target) |

**Banned shortcut compliance:** không dùng `| head` trên grep/find; symbols verified via existing references.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| A | Report file exists + manifest.md committed + per-kit DONE-eligibility verdict documented | none (docs-only) |
| B | Report file exists + 11 category scores + aggregate documented | none |
| C | Report file exists + 5 category scores + bundle table documented | none |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- **Buckets A/B/C**: spawn `run_in_background: true` ngay sau plan PR merged
- `isolation: worktree` mỗi bucket
- RELATIVE paths trong agent prompts
- Coordinator merge tuần tự A → B → C sau khi 3 background completion notifications đến
- Stake HIGH → Opus 4.7 full mỗi agent
- Max-cap 5 respected: 3 ≤ 5 ✅

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `post-wave-audit-mandate.md` §3 milestone closure:

- Mỗi bucket PR file 1 audit report
- Closure PR aggregates findings + cập nhật `output-review-mandate.md` §3 matrix rows
- **Per-kit DONE flips**: closure PR processes Bucket A verdicts → flips GAP-267..272 + GAP-266 + GAP-270 PARTIAL → DONE where ALL screens ≥105/128
- **GAP-462 closure**: status 🔵 OPEN → 🟢 DONE với references tới 3 audit reports
- **DOMAIN_MILESTONE_AUDIT trailer**: closure PR commit body includes `DOMAIN_MILESTONE_AUDIT: phase-4-kit-ports documents/04-quality/audits/ui/2026-05-10-wave-53-phase-4-milestone.md, documents/04-quality/audits/quality/2026-05-10-wave-53-quality-refresh.md, documents/04-quality/audits/performance/2026-05-10-wave-53-performance-refresh.md`
- ROADMAP §🚀 Next Action update với Phase 4 DONE verdict + Phase 1 BETA critical-path step 1 ✅
- Wave plan frontmatter `status: draft → complete`
- `wave-history.jsonl` append entry (Rule 15)
- Sub-gaps filed for findings <threshold per `audit-to-gap-pipeline.md` §3
- `bash scripts/prune-merged-worktrees.sh --yes` post-merge

### Phase 4 Track 2 progress dự kiến sau Wave 53

| Item | Trước Wave 53 | Sau Wave 53 (best case all ≥105) | Sau Wave 53 (worst case findings <105) |
|------|---------------|----------------------------------|----------------------------------------|
| Phase 4 kit DONE | 0/7 | **7/7** | varies (kit-by-kit) |
| Phase 4 kit PARTIAL | 7/7 | 0/7 | remaining + sub-gaps filed |
| Phase 1 BETA §3.6 row #1 | 7/8 | **8/8** ✅ | 7/8 + sub-gap follow-up |
| Phase 1 BETA critical-path step 1 | pending | **DONE** ✅ → unblock step 2+ |

---

## 8. Log

- **2026-05-10 (draft)**: Wave 53 plan filed sau Wave 50+51 closure SHIPPED. User chose "Wave 53 plan + spawn audit suite (3 subagents)" làm Phase 1 BETA critical-path step 1. Plan tuân thủ `audit-to-gap-pipeline.md` §2.6 State-Check Evidence + `contract-first-for-cross-layer.md` (NO cross-layer; pure audit) + `gap-done-discipline.md` PARTIAL exit-ramp ready (sub-gaps filed cho findings <threshold) + `post-wave-audit-mandate.md` §3 milestone closure trailer + `post-wave-cleanup.md` cleanup script. Stake HIGH → Opus 4.7 full mỗi agent. Wall-clock estimate ~1.5-2h. **Status: draft — chờ user review + approve trước khi merge plan PR + spawn agents.**
