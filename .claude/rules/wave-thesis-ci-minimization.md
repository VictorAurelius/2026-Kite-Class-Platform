---
paths:
  - "documents/03-planning/waves/wave-*thesis*.md"
  - "documents/08-thesis/**"
  - ".claude/rules/wave-thesis-ci-minimization.md"
---

# Wave Thesis CI Minimization — batch all bucket commits trong 1 PR + render docx cuối

**Priority:** 🟠 MANDATORY — wave thesis ship pattern governance
**Version:** 1.0.0
**Created:** 2026-05-26
**Last-Reviewed:** 2026-05-26
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test trên Wave thesis-2 batch PR — chính PR này làm self-test) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies CI-minimization pattern for thesis waves where dev review surface = rendered docx only)
**Applies to:** Mọi wave có `tag_primary: thesis` trong frontmatter HOẶC tên file `wave-*thesis*.md`. Out-of-scope: non-thesis waves (vẫn dùng pattern parallel bg-agents per `feedback_parallel_agent_strategy.md`).

---

## 1. The Rule

> **Wave thesis PHẢI batch all bucket commits (rule META + pipeline + chapter MDs + docx rebake) vào 1 PR thay vì 1 PR per bucket. Final commit = re-bake thesis-v*.docx với all fixes integrated. CI triggers một lần cho cả batch.**

Dev chỉ có thể review thủ công file `thesis-v*.docx` rendered (binary docx) — không review source MD/Python/CSV raw. Vì vậy:
- 1 PR per bucket → N CI triggers + N docx re-bakes + N user review cycles (N×~10-15 min each) = inefficient
- 1 batch PR → 1 CI trigger + 1 final docx re-bake + 1 user review = efficient

Force-multiplier per `meta-gap-priority.md` §3 — 1 batch pattern → mọi thesis wave subsequent (V2, V3, ...) auto-minimize CI cost.

---

## 2. Required pattern

### 2.1 Batch structure

| Commit type | Purpose |
|---|---|
| 1+ commits: META rule update (nếu wave touch rule) | Governance change, lands first trong batch |
| 1+ commits: pipeline rip/edit | `create_thesis_v1.py` (hoặc equivalent) functions update |
| 1+ commits: chapter MD restructure | `chapter-N-*.md` rename/merge/split |
| 1+ commits: chapter content fixes | Narrative scrub (jargon / văn nói / acronym / etc.) |
| 1+ commits: phụ lục + danh mục updates | Bibliography / danh mục viết tắt / phụ lục cleanup |
| **Final commit: re-bake thesis-v*.docx** | Generated artifact với all fixes integrated |

Mỗi commit có descriptive message theo bucket scope. Total commits có thể 5-15+ trong 1 batch PR.

### 2.2 Why "final commit = docx rebake"

Dev review surface = docx binary diff khó so sánh source-level. Last commit chứa rendered docx → reviewer download docx + open Word/LibreOffice → visual review → confirm hoặc request changes. Source diffs informational only.

### 2.3 CI trigger semantics

Single PR = single workflow run trên HEAD commit. Mọi check chạy 1 lần:
- `rule-frontmatter` (nếu META rule changed)
- `rules-index-csv` (nếu CSV updated)
- `dev-readable-doc-language` (nếu chapter MDs changed)
- `wave-plan-completeness` (nếu wave plan changed)
- `docs-archival-cadence` etc.

Vs alternative: 6 buckets × 22 checks per push = 132 check runs nếu split.

---

## 3. Allowed exceptions (split into multiple PRs)

| Case | Why exempt | Example |
|---|---|---|
| **META rule update standalone** (no content) | Governance change first, applied to subsequent batch | Wave thesis-2 rule v2.0.0 (PR #1866) shipped riêng vì content depends on rule landed first |
| **Wave plan PR** | Plan PR per `feedback_wave_plan_through_pr.md` — coordinator handoff documentation | Wave thesis-2 plan PR #1860 shipped riêng before bucket execution |
| **Hotfix prod incident** | Speed priority, batch later | Thesis docx has compile error blocking academic submission deadline |
| **User explicit "ship sub-bucket first for review"** | User-controlled exception | User want preview Bucket A before B starts |
| **Bucket scope exceeds reasonable PR size** (>50 files OR >2000 lines) | Reviewer cognitive load | Split into 2-3 sub-batches; document split rationale |

Khi invoke exception, document inline trong wave plan §3 Scope OR commit body: "Bucket X ships standalone per `wave-thesis-ci-minimization.md` §3 exception <row>".

---

## 4. Decision flow

Trước khi tạo PR cho thesis wave bucket:

```
1. Wave có tag_primary=thesis HOẶC filename wave-*thesis*? 
   YES → continue
   NO → use parallel bg-agents pattern (different rule applies)

2. Bucket scope = content/pipeline/docx (not META rule, not wave plan)?
   YES → continue
   NO → §3 exception (META rule + wave plan ship riêng)

3. Reasonable PR size (≤50 files, ≤2000 lines)?
   YES → batch tất cả remaining buckets vào 1 PR
   NO → §3 exception split (document rationale)

4. Final commit = re-bake docx artifact?
   YES → ready to push + open PR
   NO → re-bake trước final push
```

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Ship Bucket A.2 → wait CI → merge → ship Bucket A.3 → wait CI → merge → ... | Batch A.2 + A.3 + A.4 + B + C + D + E + F vào 1 PR; CI triggers 1 lần |
| Open separate PR cho từng chapter MD fix | Group all chapter fixes vào batch PR |
| Skip docx rebake commit "vì source diffs đủ visible" | Final commit MUST chứa rendered docx — dev review surface |
| Force-push between commits mid-batch | Append commits; force-push only nếu rebase needed for clean diff |
| Batch META rule với content "for efficiency" | META rule ships riêng (§3 exception row 1) — governance must land first |
| Trigger CI mid-batch via force-push every commit | Push once at end with all commits |
| Skip wave plan PR "to save 1 CI run" | Wave plan PR mandatory per `feedback_wave_plan_through_pr.md` — different rule |

---

## 6. Worked self-test — Wave thesis-2 batch PR (THIS PR)

**Wave thesis-2 buckets pre-batch (planned per wave plan §3):**
- A.1 rule v2.0.0 META → PR #1866 (separate per §3 exception row 1) ✅ MERGED 2026-05-26T09:06:17Z
- A.2 pipeline rip → initially PR #1867 (CLOSED for re-batch)
- A.3 Ch.1 restructure
- A.4 docx rebake
- B Phụ lục cleanup
- C ABC sort + F9
- D Văn nói→văn viết
- E Project-jargon scrub
- F Misc action-2.md

**Applied pattern (this batch PR):**

| Commit | Purpose |
|---|---|
| Cherry-pick A.2 pipeline rip | Rip 4 non-khung functions |
| Add this META rule | wave-thesis-ci-minimization.md v1.0.0 |
| A.3 Ch.1 restructure | Merge 3-file → chapter-1-tong-quan.md |
| Bucket B Phụ lục cleanup | Verify state + finalize |
| Bucket C ABC sort + F9 workflow doc | Danh mục sort + new pre-defense doc |
| Bucket D văn nói→văn viết | Ch.2 listing rewrite + Nhóm pattern |
| Bucket E project-jargon scrub | BETA/GA/Phase/Wave/GAP strip |
| Bucket F misc action-2.md | Figure ID + bìa verify + release-2 focus |
| **Final: re-bake thesis-v1.docx** | Rendered artifact with all fixes |

**CI trigger budget:** 1 (vs 8 if split per bucket — 87.5% reduction).

**Self-test verdict:** Rule fires correctly on the originating wave (this PR). PASS ✅

**Counterfactual without rule:** Wave thesis-2 would ship 8 PRs × ~22 CI checks each = 176 check runs + 8 user review cycles. With rule: 2 PRs (META rule + content batch) × ~22 checks = 44 check runs + 1 user review (final docx).

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching `documents/08-thesis/**` hoặc `wave-*thesis*.md`:

- [ ] Wave có tag_primary=thesis? Identify per §4 step 1
- [ ] PR scope = content/pipeline/docx (not META rule, not wave plan)?
- [ ] Multiple bucket commits batched trong 1 PR (theo §2.1 structure)?
- [ ] Final commit = re-bake docx artifact?
- [ ] PR description list explicit bucket coverage?
- [ ] §3 exception cited inline nếu split (rare case)?

### 7.2 Cross-reference với `output-review-mandate.md`

Paired same-PR — new matrix row "Wave thesis ship pattern (CI minimization)" tracking this rule.

### 7.3 CI detector (deferred per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

- **Detector complexity:** Detect "thesis wave + bucket scope" requires NLP classification of commit messages + wave plan tag. Moderate scope (~60 LOC).
- **Recurrence count:** 0 post-merge (rule shipped 2026-05-26)
- **FP risk:** Moderate — legitimate exceptions per §3 may trigger false positive
- **Decision:** Reviewer-checklist §7.1 + worked self-test §6 (this PR) sufficient cho v1.0.0; revisit detector khi 2nd wave thesis-3+ subsequent ships violation pattern

Future heuristic regex (when implemented, WARN-mode):

```bash
# Scan wave-thesis PRs for split-bucket pattern violation
gh pr list --search "wave-thesis" --state merged --limit 20 --json number,title,commits \
  | jq -r '.[] | "\(.number) commits:\(.commits|length) title:\(.title)"' \
  | awk '$3 ~ /commits:[1-2]$/ && $4 !~ /META|plan|hotfix/ {print "WARN: PR", $1, "may violate batch pattern"}'
```

### 7.4 Memory auto-load (optional, deferred)

Memory entry `feedback_wave_thesis_ci_minimization.md` could remind tại session start trước thesis wave execution. Defer per premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test §6 đủ cho v1.0.0.

### 7.5 Override mechanism

Genuine exception ngoài §3 list:

```
git commit -m "...
WAVE_THESIS_CI_OVERRIDE: <reason — e.g., 'reviewer requested incremental delivery for risk mitigation'>"
```

Trailer logged. Pattern frequency >5% trong wave thesis-N sequence triggers meta-review của §3 exception list.

---

## 8. Relationship to other rules

- **`thesis-content-standard.md`** v2.0.0 — sister rule covering thesis content quality standard; this rule extends với ship pattern discipline
- **`feedback_wave_plan_through_pr.md`** — wave plan PR-first mandate; this rule §3 exception row 2 explicitly carves out wave plan PR
- **`feedback_parallel_agent_strategy.md`** — wave-pack parallel bg-agents pattern (non-thesis waves); this rule applies WHEN wave is thesis tag (different ship pattern)
- **`docs-only-pr-auto-merge.md`** — `.py` + `.docx` files out of docs-only auto-merge scope; this rule's batch PR will NOT auto-merge (per docs-only scope) but ship via user review of rendered docx
- **`output-review-mandate.md`** §3 — paired same-PR with new matrix row "Wave thesis ship pattern (CI minimization)"
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (1 batch pattern → mọi thesis wave subsequent auto-minimize CI cost)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 (this batch PR) + rules-index.csv row all ship same PR
- **`incident-to-rule-pipeline.md`** — applied 5-stage: Detect ✓ (user direction 2026-05-26 "dev chỉ review docx, batch all fixes 1 PR, trigger CI ít nhất có thể") → Classify ✓ (no existing rule codifies thesis ship pattern; sister `feedback_parallel_agent_strategy.md` covers parallel pattern non-thesis) → Rule+Enforce ✓ (this file + paired same-PR) → Self-Test ✓ (§6 this batch PR) → Retro Log ✓ (§9 below)
- **`wave-tag-numbering-convention.md`** v1.0.0 — tag_primary=thesis identifier; this rule references trong §4 decision flow
- **`agent-model-opus-default.md`** v1.0.0 — Opus default cho agents; thesis batch coordinator-inline (no agent spawn) → orthogonal rule, không conflict
- **`context-budget-mandate.md`** §3.2 — path-scoped frontmatter `documents/03-planning/waves/wave-*thesis*.md` + `documents/08-thesis/**` — deferred-load only khi thesis wave context

---

## 9. Log

- **2026-05-26 (v1.0.0):** Rule created in response to user direction 2026-05-26 mid Wave thesis-2 execution: "dev chỉ có thể review thủ công file docx, hãy fix hết 1 lượt vào nhiều commit trong 1 PR sau đó render lại docx và trigger CI => thêm rule wave thesis trigger ci ít nhất có thể". Triggered by realization that thesis ship pattern differs from non-thesis waves: dev review surface = rendered docx binary (not source diffs), so 1 PR per bucket = inefficient (8× CI cost + 8× user review cycles). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged ship pattern inefficiency mid Wave thesis-2 batch decision) → Classify ✓ (no existing rule codifies thesis-specific ship pattern; `feedback_parallel_agent_strategy.md` covers parallel non-thesis waves; `docs-only-pr-auto-merge.md` covers docs scope ≠ thesis batch which includes .py + .docx; gap surfaced when A.2 PR #1867 created standalone before batch decision) → Rule+Enforce ✓ (this file + paired same-PR with: A.2 cherry-pick + A.3 Ch.1 restructure + B/C/D/E/F bucket commits + A.4 docx rebake + output-review-mandate §3 row + rules-index.csv row + self-test §6 on this batch PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on this very batch PR — 8 buckets × CI trigger 1 = 87.5% CI cost reduction vs split pattern) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix 1 ship pattern → mọi thesis wave subsequent (V2 V3 ...) auto-minimize CI cost. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered thesis ship pattern; no constraint loosening; existing PR #1866 (META rule) + #1860 (wave plan) grandfathered per §3 exception rows 1+2; rule applies prospectively từ Wave thesis-2 content batch forward 2026-05-26). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: thesis batch ship pattern) ✅ unique (no overlap với existing wave ship rules) ✅ widely applicable (every thesis wave content PR) ✅ body discipline §1 ≤2 "and" conjunctions. CI detector §7.3 + memory auto-load §7.4 deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (moderate complexity + recurrence count 0 + reviewer-checklist sufficient + revisit when wave thesis-3+ subsequent ships).
