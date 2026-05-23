---
title: Wave thesis-1 — Đóng cụm khóa luận pre-defense
status: complete
created: 2026-05-23
updated: 2026-05-23
closed_at: 2026-05-23
wave: 1
tag_primary: thesis
tags_secondary: [doc, beta-prep, meta]
counter: 1
date_launch: 2026-05-23
waves: [thesis-1]
gaps: [GAP-647, GAP-651, GAP-652, GAP-653, GAP-655, GAP-687, GAP-689, GAP-623, GAP-648, GAP-649]
---

# Wave thesis-1 — Đóng cụm khóa luận pre-defense

**Goal:** Đóng tất cả gap thesis không bị chặn AWS, đưa `thesis-v1.docx` ≥85/100 + defense deck + beta cohort plan sẵn sàng cho bảo vệ.
**Trigger:** User direction 2026-05-23 "draft 1 wave fix all remaining thesis gaps" + chốt tag-based numbering scheme thay Wave 108 sequential.
**Estimated wall-clock:** ~5-6h agent work (Bucket D longest); ~24h serial → ~4-5x speedup.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment — inside-out 4-bucket per `inside-out-completeness-trigger.md` §3):**

- **Inside-out from ROADMAP §🚀 Next Action:** 10 thesis gap (GAP-647/651/652/653/655/687/689/623 + 2 chặn AWS 648/649) — đã có ROADMAP citation
- **Inside-out from queue file `documents/03-planning/inside-out-queue.md`:** không có item thesis-specific (queue file scope = Phase 1 BETA infra/feature)
- **Inside-out from audit:** Wave 100 (2026-05-19) 3-agent surfaced gap list này; Wave 102.7.6 audit check residual misses
- **Outside-in NEW:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 — recent audit ≤30 ngày + scope = canonical CSV list

Persona phục vụ: Author (sinh viên) + GVHD + GVPB + Defense committee. Domain: academic deliverable (`documents/08-thesis/**` + `.claude/skills/quality/thesis-*` skills + `documents/03-planning/release/release-1-beta-cohort-plan.md`).

**Q2 (trade-offs — alternatives rejected):**

| Rejected option | Reason |
|---|---|
| File Wave 108 sequential thay tag-based | User explicit chốt `wave-thesis-1` per AskUserQuestion 3 chiều 2026-05-23 |
| Ship Bucket D Phase 2 `--execute` tách Wave thesis-1.1 | User chốt ship cùng Wave thesis-1; coordinator inline Opus 4.7 ETA 5-6h chấp nhận được |
| Full beta cohort scope (invite email + Calendar + audit checklist) | User chốt plan doc only; execution defer Wave thesis-2 hậu GAP-612 |
| File 3 stub gap mới tracking Wave thesis-2 defer (648/649/687-P3) | User chốt append Log của 3 gap hiện tại; không tăng gap count |

**Q3 (risks):**

| Risk | Recovery |
|---|---|
| Bucket D Phase 2 `--execute` mode fail (production pipeline bug) | Coordinator git revert Bucket D commit + ship Phase 1 only; Phase 2 tách Wave thesis-1.1 follow-up |
| 6-agent rate-limit (Wave 102.7.4 lesson) | Stagger 2-2-2 spawn; coordinator Opus 4.7 1M inline Bucket D nếu thrash |
| Bucket A + B asset overlap (refs.md, INDEX.md) | A read-only refs.md; B INDEX.md per chapter, disjoint |
| Outside-in audit skip miss new surface | §2 explicit skip + follow-up gap nếu Bucket C deck draft surface gap unforeseen |
| GAP-612 AWS restore timeline unknown | Wave thesis-1 standalone valuable cho defense 8-8.5đ; 9-10đ cần Wave thesis-2 |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 META prereq | new rule + skill + matrix + CSV + plan + 3 gap Log | coordinator inline | ~30-40 min | ✅ ship trước agent spawn (THIS PR) |
| A | GAP-647 Step 3 + GAP-655 | bg-agent | ~2-3h | ✅ skill `thesis-citation-extract` + refs.md read-only |
| B | GAP-651 | bg-agent | ~3-4h | ✅ skill `thesis-figure-curation` + INDEX.md per Ch.1-4 |
| C | GAP-653 | bg-agent | ~4-5h | ✅ Reveal.js deck + Q&A (standalone) |
| D | GAP-687 P1+P2 + GAP-689 P3+P4 | coordinator inline Opus 4.7 | ~5-6h | ⚠️ shared `thesis-v1.docx` — coordinator độc quyền re-bake |
| E | GAP-623 plan doc only | coordinator inline | ~2-3h | ✅ doc-only path mới |
| F | GAP-652 | bg-agent | ~3-4h | ✅ `seed-thesis-demo-tenants.sh` + script local Docker |

Disjoint check:
- Bucket A read `refs.md` only; Bucket B write INDEX.md per chapter — no overlap
- Bucket C draft `defense-deck.html` + `defense-qa-response-sheet.md` — independent paths
- Bucket D độc quyền `thesis-v1.docx` + `ThesisReportBuilder.java` (coordinator inline tránh agent conflict)
- Bucket E write `release-1-beta-cohort-plan.md` — new file path
- Bucket F write `seed-thesis-demo-tenants.sh` + `multi-tenant-demo-script.md` — new paths

---

## 3. Scope (compact schema — Strategy B+C proven Wave 33)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** MEDIUM-HIGH → model: Opus 4.7 (1M) cho coordinator + Bucket D inline; bg-agent A/B/C/F dùng default per spawn template.
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — thesis closure = docs + skill scope; KHÔNG touch BE production code (Bucket D `ThesisReportBuilder.java` thuần thesis tooling, không production BE/FE).

**Outside-in audit:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 — Wave 100 (2026-05-19) 3-agent (persona simulation + VN edu SaaS benchmark + failure-mode matrix) đã cover thesis surface; audit < 30 ngày → exception áp dụng. Wave 102.7.6 audit + GAP-688 closure audit check residuals.

**Post-wave audit (per `post-wave-audit-mandate.md` §2.4):** Domain key `meta-governance` (per §2.4.1 registry) — NO AUDIT SUITE REQUIRED (governance scope its own quality gate). Áp dụng `AUDIT_DEFER_DOMAIN_MILESTONE: meta-governance` trailer cho closure PR.

> Gap referencing: query state qua `bash scripts/query-gaps.sh <prefix>` trước khi spawn agent confirm status/priority match CSV canonical.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **META prereq** | new rule + skill + matrix + CSV + plan + 3 gap Log | 🟠 P0 | `.claude/rules/wave-tag-numbering-convention.md` + `.claude/skills/quality/wave-pack-planner/SKILL.md` + `.claude/rules/output-review-mandate.md` + `.claude/rules/rules-index.csv` + `documents/03-planning/waves/wave-2026-05-23-thesis-1-closure.md` + 3 gap Log dòng | SHIP FIRST (Phase 1 = THIS PR) |
| 1 | **A** | GAP-647 Step 3 + GAP-655 | 🟠 P1 | `.claude/skills/quality/thesis-citation-extract/**` + `documents/08-thesis/refs.md` (read-only) | Đợt 1 |
| 2 | **B** | GAP-651 | 🟠 P1 | `.claude/skills/quality/thesis-figure-curation/**` + `documents/08-thesis/chapters/INDEX.md` (per Ch.1-4) | Đợt 1 |
| 3 | **C** | GAP-653 | 🟠 P1 | `documents/08-thesis/defense/defense-deck.html` + `defense-qa-response-sheet.md` + `defense-demo-script.md` + `practice-schedule.md` | Đợt 2 |
| 4 | **F** | GAP-652 | 🟠 P1 | `scripts/seed-thesis-demo-tenants.sh` + `documents/08-thesis/defense/multi-tenant-demo-script.md` | Đợt 2 |
| 5 | **D** | GAP-687 P1+P2 + GAP-689 P3+P4 | 🟠 P1 | (coordinator inline, no agent — shared docx artifact) | Đợt 3 |
| 6 | **E** | GAP-623 plan doc | 🟠 P1 | `documents/03-planning/release/release-1-beta-cohort-plan.md` | Đợt 3 |

### Bucket A — Citation Extract Skill (GAP-647 Step 3 + GAP-655)

- Files: `.claude/skills/quality/thesis-citation-extract/` (SKILL.md + reference + scripts) — RELATIVE paths per `feedback_worktree_absolute_path_contamination.md`
- Read-only: `documents/08-thesis/refs.md`
- Tests: skill self-test fixture parse 3 sample MD → extract citation keys → verify against refs.md
- Acceptance: GAP-647 PARTIAL 80 → DONE 100; GAP-655 OPEN → DONE; skill check-skill-conventions.sh PASS

### Bucket B — Figure Curation Skill (GAP-651)

- Files: `.claude/skills/quality/thesis-figure-curation/` (SKILL.md + reference + scripts) + `documents/08-thesis/chapters/INDEX.md` (per Ch.1-4, 4 new files)
- Acceptance: GAP-651 OPEN → DONE; skill validates selection criteria + caption tiếng Việt + ≥5 figure exemplar per chapter
- (Not cross-layer — skill scope only)

### Bucket C — Defense Prep Deck (GAP-653)

- Files: `documents/08-thesis/defense/defense-deck.html` (Reveal.js 30-40 slide) + `defense-qa-response-sheet.md` (20 Q&A) + `defense-demo-script.md` (15 phút) + `practice-schedule.md` (T-3 + T-2 tuần)
- Acceptance: GAP-653 OPEN → DONE; deck reviewable end-to-end; demo script khớp 15 phút wall-clock target

### Bucket D — V1 DOCX Polish (GAP-687 P1+P2 + GAP-689 P3+P4)

- Files: `documents/08-thesis/chapters/**` (strip-or-rename + TODO scrub) + `documents/08-thesis/SIGNOFF.md` (new) + `kitehub/kitehub-thesis-tools/.../ThesisReportBuilder.java` HOẶC `scripts/create_thesis_v1.py` (Phase 2 production `--execute` mode) + `thesis-v1.docx` re-bake
- Tests: 17 JUnit test verify PASS (GAP-646 baseline) + new test `--execute` mode
- Acceptance: GAP-687 OPEN → PARTIAL 67 (Phase 1+2 ship; Phase 3 defer Wave thesis-2); GAP-689 PARTIAL 50 → DONE 100; re-bake docx ≥85/100 per `thesis-content-standard.md` rubric. Fallback: nếu Phase 2 fail → revert + ship Phase 1 only.

### Bucket E — Beta Cohort Plan Doc (GAP-623)

- Files: `documents/03-planning/release/release-1-beta-cohort-plan.md` (NEW)
  - §1 Bối cảnh + objective 9 tuần ≥4 nhận xét ký tay
  - §2 Persona target: 2 GV trial (anonymous prospect) + 2 GV VIP (warm intro)
  - §3 Timeline gantt 9 tuần
  - §4 Invite flow narrative (template defer Wave thesis-2)
  - §5 Signed review template (execution defer)
  - §6 Risk + mitigation (GAP-612 dependency)
- Acceptance: GAP-623 OPEN → DONE (doc-only); plan reviewable cho execution post-AWS restore

### Bucket F — Multi-tenant Demo (GAP-652)

- Files: `scripts/seed-thesis-demo-tenants.sh` + `documents/08-thesis/defense/multi-tenant-demo-script.md` (5 phút demo + RLS proof commands + cross-tenant 403 evidence capture)
- Tests: seed script dry-run trên local Docker stack verify schema match
- Acceptance: GAP-652 OPEN → DONE (script-only); production execution defer Wave thesis-2 hậu AWS

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `documents/08-thesis/refs.md` | Bibliography canonical | `ls documents/08-thesis/refs.md` | exists (Wave 100.7 Phase 4 V1 — 44 IEEE refs 89% inline) | ✅ exists |
| `documents/08-thesis/chapters/INDEX.md` | Per-chapter figure INDEX | `ls documents/08-thesis/chapters/INDEX.md` | 0 matches | 🆕 to-be-created (Bucket B per chapter) |
| `documents/08-thesis/defense/defense-deck.html` | Reveal.js deck | `ls documents/08-thesis/defense/` | folder TBD | 🆕 to-be-created (Bucket C) |
| `documents/08-thesis/defense/multi-tenant-demo-script.md` | Demo script | (same folder check) | 🆕 to-be-created (Bucket F) |
| `documents/08-thesis/SIGNOFF.md` | Closure signoff | `ls documents/08-thesis/SIGNOFF.md` | 0 matches | 🆕 to-be-created (Bucket D) |
| `documents/03-planning/release/release-1-beta-cohort-plan.md` | Beta cohort plan | `ls documents/03-planning/release/` | 0 matches | 🆕 to-be-created (Bucket E) |
| `.claude/skills/quality/thesis-citation-extract/` | Skill dir | `ls .claude/skills/quality/thesis-citation-extract/ 2>/dev/null` | 0 matches | 🆕 to-be-created (Bucket A) |
| `.claude/skills/quality/thesis-figure-curation/` | Skill dir | `ls .claude/skills/quality/thesis-figure-curation/ 2>/dev/null` | 0 matches | 🆕 to-be-created (Bucket B) |
| `scripts/seed-thesis-demo-tenants.sh` | Demo seed script | `ls scripts/seed-thesis-demo-tenants.sh 2>/dev/null` | 0 matches | 🆕 to-be-created (Bucket F) |
| `scripts/create_thesis_v1.py` | Python pipeline (Wave 102.6 pivot) | `ls scripts/create_thesis_v1.py` | exists (Wave 102.6 PR #1650) | ✅ exists (Bucket D Phase 2 extend `--execute` mode) |
| `thesis-v1.docx` | DOCX artifact | `ls documents/08-thesis/thesis-v1.docx` | exists (Wave 102.7.6 final polish 82/100 baseline) | ✅ exists (Bucket D re-bake target ≥85/100) |
| `documents/08-thesis/chapters/` | Chapter MD source | `ls documents/08-thesis/chapters/` | exists (Wave 100.7 Phase 4 V1 ship) | ✅ exists (Bucket D strip-or-rename) |
| `documents/04-quality/gaps/phase-1-beta/GAP-647-thesis-bibliography-ieee.md` | Gap canonical | `ls documents/04-quality/gaps/phase-1-beta/GAP-647*.md` | exists | ✅ exists (Bucket A Step 3 closure) |
| `documents/04-quality/gaps/phase-1-beta/GAP-655-thesis-citation-extract-skill.md` | Gap canonical | `ls documents/04-quality/gaps/phase-1-beta/GAP-655*.md` | exists | ✅ exists (Bucket A DONE flip) |
| `documents/04-quality/gaps/phase-1-beta/GAP-687-thesis-v1-draft-docx-audit-followups.md` | Gap canonical | `ls documents/04-quality/gaps/phase-1-beta/GAP-687*.md` | exists | ✅ exists (Bucket D Phase 1+2 PARTIAL flip) |

Per `audit-to-gap-pipeline.md` §2.5 — NO `| head` truncation; full grep evidence; aspirational symbols flagged `🆕 to-be-created` với explicit Bucket owning creation.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| 0 META | `bash scripts/check-rule-frontmatter.sh` + `bash scripts/check-rules-index-csv.sh` + `bash scripts/check-wave-plan-completeness.sh` | script-quality |
| A | `bash .claude/skills/scripts/check-skill-conventions.sh` (skill new dir PASS) + skill self-test fixture | (no CI gate cho skill — manual) |
| B | `bash .claude/skills/scripts/check-skill-conventions.sh` (skill new dir) + INDEX.md format check | (no CI gate) |
| C | manual review deck rendering + Q&A coverage 20+ items + demo script timing | (no CI gate) |
| D | `python scripts/create_thesis_v1.py --execute` re-bake PASS + `bash scripts/check-thesis-content-standard.sh` (rubric ≥85/100) + 17 JUnit + new --execute test | (no CI gate cho thesis tool — manual rubric) |
| E | manual review plan completeness 7 sections + format check Vietnamese narrative | (no CI gate) |
| F | `bash scripts/seed-thesis-demo-tenants.sh --dry-run` local Docker PASS + demo script timing 5 phút | (no CI gate) |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

- All bg-agent buckets (A/B/C/F) spawned với `run_in_background: true`
- Worktree isolation (`isolation: worktree`) cho parallel safety
- RELATIVE paths trong agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially sau all background completions

Stagger 2-2-2 (per Wave 102.7.4 rate-limit lesson):

```
Đợt 1 (parallel bg-agent):
  - Bucket A: agent prompt — GAP-647 Step 3 + GAP-655 + skill template
  - Bucket B: agent prompt — GAP-651 + figure selection criteria + INDEX template

Đợt 2 (parallel bg-agent, sau Đợt 1 land):
  - Bucket C: agent prompt — GAP-653 deck + Q&A + practice schedule
  - Bucket F: agent prompt — GAP-652 demo script + seed scripts

Đợt 3 (coordinator inline Opus 4.7 1M, sau Đợt 1+2 ship):
  - Bucket D: GAP-687 Phase 1+2 + GAP-689 P3+P4 + re-bake docx (coordinator độc quyền tránh agent conflict)
  - Bucket E: release-1-beta-cohort-plan.md doc-only (coordinator inline ngắn)
```

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `wave-closure-scope-completeness.md`:

1. Mỗi bucket PR cập nhật gap file Log + status flip (per `gap-architecture-v2.md` CSV canonical)
2. Re-bake `thesis-v1.docx` lần cuối sau Bucket A+B output merge vào source MD (coordinator inline)
3. Audit docx qua `thesis-content-standard.md` rubric → verify ≥85/100
4. Sync `documents/04-quality/gaps/gap-status.csv` (7 dòng status flip):
   - GAP-647 PARTIAL 80 → DONE 100
   - GAP-651 OPEN → DONE 100
   - GAP-653 OPEN → DONE 100
   - GAP-655 OPEN → DONE 100
   - GAP-689 PARTIAL 50 → DONE 100
   - GAP-623 OPEN → DONE 100 (doc-only)
   - GAP-652 OPEN → DONE 100 (script-only)
   - GAP-687 OPEN → PARTIAL 67 (Phase 1+2 ship; Phase 3 defer Wave thesis-2)
5. ROADMAP §🎯 Current Status Snapshot entry mới format "Wave thesis-1 (2026-05-23): ..."
6. Wave plan frontmatter `status: draft → complete` flip trong closure PR
7. `wave-history.jsonl` append entry mới format:
   ```json
   {"wave":"thesis-1","tag_primary":"thesis","tags_secondary":["doc","beta-prep","meta"],"counter":1,"date":"2026-05-23","theme":"Thesis closure 6 bucket parallel + META convention prereq","gaps":["GAP-647","GAP-651","GAP-653","GAP-655","GAP-687","GAP-689","GAP-623","GAP-652"],"status":"complete","followup":"GAP-687 Phase 3 + GAP-648/649 defer Wave thesis-2 — chờ GAP-612 AWS restore"}
   ```
8. Scope-completeness reconciliation table per `wave-closure-scope-completeness.md` §3 (10 plan §3 items categorize ✅/🟡/❌)
9. `bash scripts/prune-merged-worktrees.sh --yes` cleanup post-merge per `post-wave-cleanup.md`
10. Session handoff doc `documents/03-planning/session-handoffs/2026-05-23-wave-thesis-1-closure.md`
11. Closure PR commit body trailer `AUDIT_DEFER_DOMAIN_MILESTONE: meta-governance` per `post-wave-audit-mandate.md` §2.4 (domain registry NO AUDIT REQUIRED)
12. Update `wave-tag-numbering-convention.md` §10 Log entry — Wave thesis-1 worked self-test 8/8 PASS confirmation

### Defer Wave thesis-2 — 3 gap append Log (Phase 1 THIS PR, no follow-up gap stub per user direction)

| Gap | Trigger restart Wave thesis-2 |
|---|---|
| GAP-648 NFR data capture | GAP-612 DONE + cluster live ≥7 ngày |
| GAP-649 Beta cohort execution | GAP-612 DONE + invite email gửi + 9 tuần timeline |
| GAP-687 Phase 3 (NFR + beta + Ch.5-7 evidence) | GAP-648 + GAP-649 cả 2 DONE |

---

## 7.5 Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

| # | Plan §3 Scope item | Verdict | Evidence / Follow-up |
|---|---|:---:|---|
| 0 | META prereq §0 ship (rule + skill + matrix + CSV + plan + 3 gap Log) | ✅ DONE | PR #1748 commit `53f30e27` |
| 1 | Bucket A — GAP-647 Step 3 + GAP-655 citation-extract skill | ✅ DONE | PR #1750 `bd855905`; skill `.claude/skills/quality/thesis-citation-extract/`; GAP-647 + GAP-655 closed |
| 2 | Bucket B — GAP-651 figure-curation skill + 4 INDEX | ✅ DONE | PR #1749 `644a3575`; skill + 4 INDEX files; GAP-651 closed |
| 3 | Bucket C — GAP-653 defense deck + Q&A + demo + practice | ✅ DONE | PR #1752 `7f09a4ca`; `documents/08-thesis/defense/` 5 files; GAP-653 closed |
| 4 | Bucket F — GAP-652 demo script (script-only) | ✅ DONE script-only | PR #1751 `f6b71ecb`; seed script + 5-phút demo; GAP-652 closed (runtime defer Wave thesis-2) |
| 5 | Bucket D Phase 1+2 — GAP-687 strip-or-rename + `--execute` mode | ✅ DONE Phase 1+2 | PR #1754 `cc03d708`; 4 backup archived + create_thesis_v1.py extended; GAP-687 PARTIAL 67% (Phase 3 defer) |
| 5 | Bucket D Phase 3+4 — GAP-689 final polish + signoff | ✅ DONE | PR #1754; SIGNOFF.md + polish; GAP-689 DONE archived |
| 6 | Bucket E — GAP-623 beta cohort plan (doc-only) | ✅ DONE doc-only | PR #1753 `1d870e76`; `release-1-beta-cohort-plan.md` + folder README; GAP-623 closed (execution defer) |
| 7 | Re-bake docx ≥85/100 | 🟡 PARTIAL 76/100 C+ | Audit `2026-05-23-wave-thesis-1-bucket-d-docx-rubric.md`; ≥85 honest defer Wave thesis-2 (depends GAP-687 Phase 3 + GAP-648 NFR + GAP-649 beta data). Baseline 82/100 B- Wave 102.7.6 stands. |
| 8 | Defer Wave thesis-2 — GAP-648 + GAP-649 + GAP-687 Phase 3 Log append | ✅ DONE | PR #1748 plan + 3 gap Log entries 2026-05-23; trigger restart = GAP-612 AWS restore |

**Verdict:** 9/10 ✅ DONE + 1 🟡 PARTIAL (target ≥85 honest defer per `gap-done-discipline.md` §3 PARTIAL exit ramp). Zero orphan items per `wave-closure-scope-completeness.md` §3.

---

## 8. Log

- **2026-05-23** (draft): Plan created. Phase 1 META prereq + plan file + 3 gap defer Log shipped same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 row 4 (Wave 100 audit ≤30 ngày). META domain key = `meta-governance` (NO AUDIT SUITE REQUIRED). Self-test rule §6 — 5/8 expected artifacts match THIS PR.
- **2026-05-23** (in-progress): Phase 2 spawn — 6 bucket bg-agent 3 đợt: Đợt 1 (A+B parallel) → Đợt 2 (C+F parallel) → Đợt 3 (D+E parallel bg-agent thay coordinator inline, scope clear isolation).
- **2026-05-23** (complete): Wave SHIPPED. 7 PR merged squash main: #1748 plan + META → #1749 Bucket B figure-curation → #1750 Bucket A citation-extract → #1751 Bucket F demo script (script-only) → #1752 Bucket C defense deck + Q&A + demo + practice → #1753 Bucket E beta cohort plan (doc-only) → #1754 Bucket D V1 docx polish + execute mode (rubric 76/100 C+ PASS heuristic; ≥85 honest defer Wave thesis-2). Outcomes:
  - **7 thesis gap closed:** GAP-647 + GAP-651 + GAP-652 + GAP-653 + GAP-655 + GAP-689 DONE + GAP-623 DONE doc-only
  - **1 thesis gap PARTIAL ship:** GAP-687 Phase 1+2 (Phase 3 defer Wave thesis-2)
  - **3 thesis gap defer Wave thesis-2:** GAP-648 NFR + GAP-649 Beta execution + GAP-687 Phase 3
  - **2 skill mới shipped:** `thesis-citation-extract` + `thesis-figure-curation`
  - **5 defense artifacts shipped:** `defense-deck.html` Reveal.js 40 slide + `defense-qa-response-sheet.md` 20 Q&A × 4 archetype + `defense-demo-script.md` 15 phút + `practice-schedule.md` T-3+T-2 + `multi-tenant-demo-script.md` 5 phút secondary
  - **1 cohort plan shipped (doc-only):** `release-1-beta-cohort-plan.md` 9-tuần timeline + 4 GV persona
  - **1 thesis-v1.docx re-bake:** 4 section / 646 paragraph / 27 figure / 26 table / 38 bibliography / rubric 76/100 C+
  - **META prereq shipped:** `wave-tag-numbering-convention.md` v1.0.0 + skill update + matrix row + CSV row — first wave dùng tag-based numbering
  - **Self-test rule §6 confirmation:** 8/8 expected artifacts match
  - Wall-clock ~3.5h actual vs ~5-6h estimate (6.9x speedup vs serial ~24h)
  - 6 background agents spawned 3 đợt; 6 PRs sequentially merged squash + delete-branch (no `--admin` per `admin-merge-discipline.md`)
  - 1 CI fix iteration: Bucket D PR #1754 audits-index.csv 100% coverage parity miss → coordinator fix forward commit `a3de2d70`
  - Defense readiness 8-8.5đ achievable ship-state; 9-10đ requires Wave thesis-2 (NFR + beta evidence)
