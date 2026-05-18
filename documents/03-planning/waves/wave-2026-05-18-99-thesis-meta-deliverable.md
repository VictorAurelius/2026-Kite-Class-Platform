---
title: Wave 99 — Thesis META Academic Deliverable (5-bucket A1 cluster)
status: draft
created: 2026-05-18
updated: 2026-05-18
waves: [99]
gaps: [GAP-646, GAP-647, GAP-650, GAP-651, GAP-655]
---

# Wave 99 — Thesis META Academic Deliverable

**Goal:** Ship academic-deliverable foundation cho graduation thesis — DOCX pipeline + IEEE bibliography + Chapter 1 lit review + image/citation curation skills. 5 parallel buckets, doc/skill scope only, zero AWS dependency.

**Trigger:** Wave 98 closure expected ~1.5 day post-spawn (2026-05-19/20). Wave 99 = next-wave pre-positioned per user request 2026-05-18 with thesis META priority. Outside-in audit reused (3 thesis audits from Wave 97 morning 2026-05-18).

**Estimated wall-clock:** ~2-3 agent days (longest T3 Ch.1 lit review ~10h; parallel compress to ~1.5 day with 5 concurrent agents).

---

## 1. Brainstorm (per `inside-out-completeness-trigger.md` 4-source pull + `outside-in-coverage-trigger.md` reused)

### Q1 (alignment)

**Inside-out — from ROADMAP §🚀 Next Action (canonical):**
- Post-Wave-98 closure ROADMAP §🚀 candidates expected to surface (Wave 99 plan WILL be revised at PR-open time to incorporate Wave 98 follow-up gaps per `wave-closure-scope-completeness.md` v1.0.0 §3 table)

**Inside-out — from `inside-out-queue.md`:**
- No active queued items relevant to thesis scope (queue items either consumed or Phase 1.5+ pivot decisions)

**Inside-out — from gap-status.csv P0 phase-1-beta thesis cluster:**
- 7 thesis-relevant gaps active: GAP-646/647/648/649/650/651/652/653/655
- Wave 99 scope = 5 sub-cluster A1 (META academic deliverable, AWS-independent):
  - GAP-646 P0 OPEN — Thesis DOCX pipeline (template + chapter assembly + IEEE bibliography)
  - GAP-647 P0 PARTIAL 50% — Thesis bibliography IEEE citation style + refs.md canonical
  - GAP-650 P0 OPEN — Chapter 1 literature review (5-competitor + 4 AI approach + VN law)
  - GAP-651 P1 OPEN — Image curation skill (META)
  - GAP-655 P1 OPEN — Citation-extract skill (Wave 98+ tooling)
- Wave 99 OUT-OF-SCOPE (defer Wave 100+ AWS-restore unblock):
  - GAP-648 NFR data capture (CloudWatch + AWS cost) — needs GAP-612 restored
  - GAP-649 Beta cohort ≥4 signed reviews — needs tenants live
  - GAP-652 Multi-tenant isolation demo (P1) — depends GAP-649 staging
  - GAP-653 Defense prep deck (P1) — better Wave 101 closer to defense T-3 weeks

**Outside-in — REUSED from 3 Wave 97 morning audits (2026-05-18):**
- `documents/04-quality/audits/persona-review/2026-05-18-thesis-persona-demo-audit.md` — P1+P2 walkthrough surfaced demo-readiness blockers (GAP-297/287/293/562 — deferred Wave 100 demo-enabling)
- `documents/04-quality/audits/persona-review/2026-05-18-thesis-vn-saas-benchmark.md` — 5 VN CS thesis examples + 3 industry refs; strategic verdict AMBITIOUS top 5-10%
- `documents/04-quality/audits/persona-review/2026-05-18-thesis-defense-failure-mode-matrix.md` — 4 examiner archetypes × 5 questions, 20 challenges + top 10 P0 + 3 strategic narrative moves

→ Wave 99 = META academic deliverable foundation. Demo-enabling product gaps + defense prep defer to Wave 100/101 per audit findings.

### Q2 (trade-offs)

| Alternative | Rejected because |
|---|---|
| Combine A1 META + A2 demo-enabling (9 gaps) | Exceeds 5-agent cap per `feedback_parallel_agent_strategy.md` rule #9; mega-wave anti-pattern |
| Defer all thesis to post-Wave-100 | Personal academic deadline pressure; META foundation independent of product readiness |
| Skip image + citation skills (P1) | They're force-multipliers cho subsequent thesis content waves (per `meta-gap-priority.md` §3); cheap to ship now |
| Ship Chapter 1 lit review serial after DOCX pipeline (sequential) | Independent content; parallel-safe. Sequential adds 5-day delay no benefit |

### Q3 (risks)

| Risk | Bucket | Recovery |
|---|---|---|
| T1 DOCX pipeline scope creep — Pandoc + custom CSS + cross-refs | T1 | Time-box: ship minimum (Pandoc CLI wrapper + 7-chapter template + IEEE bib placeholder); defer fancy CSS Wave 101 |
| T3 Chapter 1 lit review — 5-competitor research depth | T3 | Use existing audit data (`thesis-vn-saas-benchmark.md`) as starting point; cite ≥5 sources/competitor |
| T2 IEEE bibliography — depends T5 citation-extract skill | T2 + T5 | Decouple: T5 ships skill standalone; T2 uses MANUAL refs.md compilation for Wave 99 (~30 sources from session history); T5 skill applied retroactively Wave 100 |
| T4 image curation skill — figure numbering convention TBD | T4 | Adopt IEEE thesis figure convention (Fig. N.M = chapter N figure M); document trong skill SKILL.md |
| Wave 98 closure surfaces follow-up gaps changing Wave 99 priority | All | Plan PR open AFTER Wave 98 closure → revise §3 Scope per closure-surfaced gaps |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| **T1** | GAP-646 | bg-agent | ~8h | ✅ `documents/08-thesis/` + Python scripts |
| **T2** | GAP-647 | bg-agent | ~4h | ✅ `documents/08-thesis/refs.md` only |
| **T3** | GAP-650 | bg-agent | ~10h | ✅ `documents/08-thesis/chapter-1/*` |
| **T4** | GAP-651 | bg-agent | ~3h | ✅ `.claude/skills/thesis/image-curation/` |
| **T5** | GAP-655 | bg-agent | ~3h | ✅ `.claude/skills/thesis/citation-extract/` |

**Disjoint check:** All 5 buckets parallel-safe — different folders. T2 + T5 have semantic dependency (T2 outputs feed T5; T5 tool processes T2) but file-disjoint per Wave 99 scope (T2 manual refs.md; T5 skill standalone for Wave 100 application).

**Parallel cap per `feedback_parallel_agent_strategy.md` rule #9:** 5 concurrent = at cap. Single-wave execution.

---

## 3. Scope (compact schema)

**Stake tier:** MEDIUM (academic deliverable, personal deadline). Model: Opus medium default. Opus full reserved cho T3 (lit review = highest content-quality stake).
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — 100% docs + skill scope, no code touch.

> **Gap referencing convention** per `gap-architecture-v2.md`: CSV verified `bash scripts/query-gaps.sh GAP-64` + `GAP-65` 2026-05-18.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **T1** Thesis DOCX pipeline | GAP-646 | 🔴 P0 | `documents/08-thesis/scripts/render-thesis-docx.sh`, `documents/08-thesis/templates/{reference.docx,thesis-template.md}`, `documents/08-thesis/Makefile` | parallel |
| 2 | **T2** IEEE bibliography canonical | GAP-647 (50% → 100%) | 🔴 P0 | `documents/08-thesis/refs.md`, `documents/08-thesis/refs-ieee.bib` | parallel |
| 3 | **T3** Chapter 1 literature review | GAP-650 | 🔴 P0 | `documents/08-thesis/chapter-1/{introduction,competitor-analysis,ai-approach-comparison,vn-law-mapping,scope-objectives}.md` | parallel |
| 4 | **T4** Image curation skill | GAP-651 | 🟠 P1 | `.claude/skills/thesis/image-curation/SKILL.md`, `.claude/skills/thesis/image-curation/reference/{figure-numbering,caption-convention,selection-criteria}.md` | parallel |
| 5 | **T5** Citation-extract skill | GAP-655 | 🟠 P1 | `.claude/skills/thesis/citation-extract/SKILL.md`, `.claude/skills/thesis/citation-extract/scripts/extract-citations.py` | parallel |

### Bucket T1 — Thesis DOCX pipeline

- Files: `documents/08-thesis/{scripts/render-thesis-docx.sh, templates/reference.docx, templates/thesis-template.md, Makefile, README.md}` (🆕 create)
- Pandoc-based pipeline: `pandoc chapter-{1..7}.md -o thesis.docx --reference-doc=reference.docx --bibliography=refs-ieee.bib --csl=ieee.csl`
- 7-chapter template structure: Ch.1 Intro+Lit / Ch.2 Architecture / Ch.3 Methodology / Ch.4 Implementation / Ch.5 Testing+Validation / Ch.6 Results / Ch.7 Discussion+Conclusion
- Output: `thesis.docx` + `thesis.pdf` (LaTeX fallback)
- Acceptance: `make thesis` produces valid DOCX with TOC + bibliography + figure references resolved

### Bucket T2 — IEEE bibliography canonical

- Files: `documents/08-thesis/refs.md` (extend từ GAP-647 PARTIAL 50% — ~30 refs IEEE format), `documents/08-thesis/refs-ieee.bib` (🆕 BibTeX export)
- Add missing refs: Anthropic Claude paper + Spring Boot docs + Next.js docs + PDPL 2023 + VN Cybersecurity Law 2018 + Misa/KiotViet/Haravan benchmarks (from external benchmark audits)
- IEEE citation format: `[1] J. Smith, "Title," Journal, vol. X, no. Y, pp. Z-Z, Year.`
- Acceptance: ≥40 refs total (current 30 + Wave 99 additions), all IEEE format, BibTeX exports clean

### Bucket T3 — Chapter 1 literature review

- Files: `documents/08-thesis/chapter-1/{introduction,competitor-analysis,ai-approach-comparison,vn-law-mapping,scope-objectives}.md` (🆕 create)
- **introduction.md** (~1500 words): vấn đề trung tâm giáo dục VN + multi-tenant SaaS solution + thesis contribution statement
- **competitor-analysis.md** (~2000 words): 5-competitor table (Misa LopHoc / KiotViet edu / Hoclieu.vn / EduFit / 1 international) — feature × pricing × VN-fit × tech stack matrix
- **ai-approach-comparison.md** (~2000 words): 4 AI approach (Strategy 1 prompt-only / Strategy 2 RAG / Strategy 3 fine-tune / Strategy 4 agent-orchestration) — KiteHub picks Strategy 4 (Claude Agent SDK)
- **vn-law-mapping.md** (~1500 words): PDPL 2023 + Decree 13/2023 + Luật An ninh mạng 2018 + Decree 53/2022 → KiteHub compliance design choices
- **scope-objectives.md** (~1000 words): thesis scope + 5 SMART objectives + delivery timeline
- Acceptance: ≥8000 words Vietnamese narrative + ≥30 inline citations to `refs.md` IDs

### Bucket T4 — Image curation skill (META)

- Files: `.claude/skills/thesis/image-curation/SKILL.md` (🆕 create), `.claude/skills/thesis/image-curation/reference/{figure-numbering-convention,caption-vn,selection-criteria,index-per-chapter}.md` (🆕 create)
- IEEE thesis figure convention: `Hình N.M` (chapter N figure M), Vietnamese caption + English alt text
- Selection criteria: source-cited / WCAG AA contrast / 300 DPI minimum / no proprietary brand without permission
- Per-chapter INDEX.md cataloging all figures
- Acceptance: skill SKILL.md follows `skill-conventions.md` rules + 4 reference docs

### Bucket T5 — Citation-extract skill (META)

- Files: `.claude/skills/thesis/citation-extract/SKILL.md` (🆕 create), `.claude/skills/thesis/citation-extract/scripts/extract-citations.py` (🆕 create)
- Python script: parses `*.md` chapter files for inline citations `[refs.md#refN]` → outputs BibTeX `refs-ieee.bib` entries + warns unresolved citations
- Skill SKILL.md follows `skill-conventions.md`
- Acceptance: script runs clean on Wave 99 Chapter 1 output (T3 dogfoods T5)

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `documents/08-thesis/` | Doc folder | `ls documents/08-thesis/ 2>/dev/null` | will verify in agent | ✅ exists (verify content extent at spawn) |
| `documents/08-thesis/refs.md` | Bibliography | `ls documents/08-thesis/refs.md` | (verify at spawn — PARTIAL 50% per GAP-647) | ✅ exists (extends per T2) |
| `documents/08-thesis/scripts/render-thesis-docx.sh` | Build script | `ls documents/08-thesis/scripts/` | 0 matches expected | 🆕 to-be-created (T1) |
| `documents/08-thesis/chapter-1/` | Doc subfolder | `ls documents/08-thesis/chapter-1/ 2>/dev/null` | 0 matches expected | 🆕 to-be-created (T3) |
| `.claude/skills/thesis/image-curation/` | Skill folder | `ls .claude/skills/thesis/image-curation/ 2>/dev/null` | 0 matches | 🆕 to-be-created (T4) |
| `.claude/skills/thesis/citation-extract/` | Skill folder | `ls .claude/skills/thesis/citation-extract/ 2>/dev/null` | 0 matches | 🆕 to-be-created (T5) |
| Pandoc availability | CLI dependency | `which pandoc` | will verify at spawn (system has pandoc?) | ⚠️ verify at spawn (if missing, document in PR + ship without DOCX render only — Makefile target placeholder) |
| GAP-646/647/650/651/655 | Gap CSV rows | `bash scripts/query-gaps.sh GAP-64` + `GAP-65` | 5 rows confirmed | ✅ canonical CSV verified |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| T1 | `cd documents/08-thesis && make thesis 2>&1` (Pandoc → DOCX) | docs-only path (no Java/pnpm CI; meta-csv-indexes job validates if .claude/skills touched) |
| T2 | `python3 -c "import bibtexparser; bibtexparser.load(open('documents/08-thesis/refs-ieee.bib'))"` | docs-only |
| T3 | `wc -w documents/08-thesis/chapter-1/*.md` (verify ≥8000 words total) | docs-only |
| T4 | `bash scripts/check-skill-conventions.sh` (skill-conventions check) | skill-conventions CI job |
| T5 | `python3 .claude/skills/thesis/citation-extract/scripts/extract-citations.py --dry-run documents/08-thesis/chapter-1/*.md` | skill-conventions CI job |

---

## 6. Agent Spawn Pattern

Per `agent-background-spawn-default.md` v1.0.1 + `feedback_parallel_agent_strategy.md`:
- All 5 buckets spawned `run_in_background: true`
- Worktree isolation (`isolation: "worktree"`)
- RELATIVE paths in agent prompts
- Single-wave execution (5 concurrent at cap)
- Coordinator merges sequentially: T2 + T5 first (low-risk meta-foundation) → T1 (DOCX pipeline consumes T2 refs) → T4 (image skill standalone) → T3 (lit review consumes T2 refs)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` v1.0.0 + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:

- Each bucket PR updates affected GAP file Log + CSV row status + completion_pct
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan frontmatter `status: complete` flip
- `wave-history.jsonl` append per Rule 15
- **Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3** — every §3 bucket categorized ✅/🟡/❌ + follow-up gap link
- Sub-gaps filed cho any deferral (especially Chapter 2-7 content waves)
- `bash scripts/prune-merged-worktrees.sh --yes` after all bucket PRs merged
- `## Release Plan Progress` section in closure PR

**Post-wave audit suite (per `post-wave-audit-mandate.md` §2.4 domain-milestone):** Wave 99 = `meta-governance` domain (NO AUDIT REQUIRED per §2.4.1 registry) — governance is its own quality gate. Skill-conventions CI job is sufficient.

---

## 8. Log

- **2026-05-18** (draft): Plan created. Pre-positioned per user request 2026-05-18 while Wave 98 5 agents running. Outside-in audit REUSED from 3 Wave 97 thesis audits 2026-05-18. Cross-layer = NO (100% docs + skills). 5-bucket parallel at cap.
- **PENDING** (open PR): defer plan PR open until AFTER Wave 98 closure to incorporate any follow-up gaps surfaced by Wave 98 closure §3 Scope-Completeness Reconciliation table.
