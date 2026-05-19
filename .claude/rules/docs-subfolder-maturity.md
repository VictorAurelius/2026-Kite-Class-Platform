---
paths:
  - "documents/**"
---

# Docs Subfolder Maturity — chỉ tạo subdir khi thresholds match

**Priority:** 🟠 MANDATORY — folder structure discipline preventing subdir sprawl
**Version:** 1.0.1
**Created:** 2026-05-18
**Last-Reviewed:** 2026-05-19
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.0.1 PATCH self-approve per `rule-change-process.md` §5; Wave 99C META-META GAP-675 SHIP-NOW — `scripts/check-docs-subfolder-maturity.sh` shipped (115 LOC bash) + wired CI job `docs-scaling-detectors`; self-test PASS (2 below-threshold flagged + 5-file/README exempted); real-repo scan surfaces 177 grandfathered subdirs as WARN (rule prospective per §4 migration policy); closes deferred-detector debt §5.3 within 1 day of rule landing per `incident-to-rule-pipeline.md` Stage 3 paired-enforcement. No constraint change. v1.0.0 (kept): MINOR self-approve per §5; new rule với built-in enforcement (path-scoped auto-load + reviewer-checklist + PR template + worked self-test trên `documents/05-guides/` 17 subdirs retroactive) per §6.5)
**Applies to:** Mọi PR thêm subdirectory mới (hoặc nested subdirectory) dưới `documents/**`. Scope = subdir creation discipline; KHÔNG cover file placement bên trong subdir (đó là `docs-folder-structure.md` §file-placement-rules).

---

## 1. The Rule

> **Subdirectory chỉ được tạo dưới `documents/**` khi MỘT trong các threshold §2 thỏa.** Single-file subdir = ANTI-PATTERN — tăng cognitive load điều hướng folder mà không add navigation value. Mặc định: place file in-place tại parent folder cho tới khi threshold reached.

Force-multiplier rationale: 17 subdirs trong `documents/05-guides/` mà nhiều subdir chỉ có 1-2 files = reader phải mở folder, đọc README/list files, rồi mới biết "à chỉ có 1 file". Trade-off cognitive load > organization value. YAGNI principle (per `design-patterns.md` §1.1) applies — đừng pre-emptive organize "for future".

---

## 2. Threshold criteria (subdir allowed when ANY satisfied)

Subdir creation chỉ được phép khi **ít nhất MỘT** criterion sau thỏa:

| Criterion | Detail | Cách verify |
|---|---|---|
| **Volume** | ≥5 files trong scope HOẶC ≥5 files planned trong 30 ngày tới | Count current files + cite planned wave items / gap files / queue items |
| **Cross-author** | ≥2 distinct contributors trong scope | `git log --format=%an documents/<subdir>/ \| sort -u \| wc -l` ≥ 2 |
| **Reviewer approval** | Lead/architect explicit OK trong PR body với rationale | PR description includes "subdir maturity override: <reason>" + reviewer-approver |
| **Sister-pattern** | Mirror existing subdir convention rõ ràng | Existing parallel subdir đã có ≥5 files (vd: `dev/` đã có → `professional-manual/` cho dev audience parallel OK khi planned ≥5 files) |

**Quyết định decision flow:**

1. Bạn muốn tạo `documents/<parent>/<new-subdir>/`?
2. Đếm files sẽ thuộc `<new-subdir>` (hiện tại + planned 30 ngày). Nếu ≥5 → ✅ Volume criterion → tạo subdir.
3. Nếu <5 files → check cross-author / reviewer approval / sister-pattern.
4. Nếu KHÔNG criterion nào thỏa → ❌ Place file in-place tại `documents/<parent>/<filename>.md` thay vì tạo subdir.

---

## 3. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Tạo `documents/05-guides/professional-manual/` cho 1 file `professional-manual.md` | Place file at `documents/05-guides/professional-manual.md` (in-place) cho tới khi reach ≥5 files |
| Pre-emptive subdir "for future organization" (planned files chưa scoped cụ thể) | YAGNI per `design-patterns.md` §1.1 — create subdir khi đã có ≥5 files OR concrete plan ≤30 ngày |
| Subdir cho transient artifact (audit reports, retro notes) | Time-bound artifacts dùng date-prefix trong shared dir: `documents/04-quality/audits/2026-05-18-<topic>.md` |
| Tạo nested subdir `documents/05-guides/user-manual/p2-owner/onboarding/` khi `onboarding/` chỉ có 1 file | In-place `documents/05-guides/user-manual/p2-owner/onboarding.md` cho tới khi reach threshold |
| Subdir "vì sister-pattern có" mà không match volume/criterion | Sister-pattern criterion yêu cầu sister subdir đã ≥5 files (proven pattern, không phải aspirational) |
| Single-file subdir để "tách concern" | Concern separation trong markdown = `## H2 section` headings, không phải folder split |

---

## 4. Migration path (existing single-file subdirs)

Rule **applies prospectively** từ Wave 92 forward. Existing single-file subdirs grandfathered per `rule-change-process.md` convention — KHÔNG migrate retroactively trong rule landing PR.

**Khi nào migrate existing single-file subdir:**

- **Khi subdir đã grandfathered + Volume criterion vẫn fail sau 30+ ngày** → file follow-up gap "Flatten <subdir> back to in-place" (P2 priority, batch với cleanup wave)
- **Khi user/reviewer flag explicit** → migrate trong sweep PR (cùng cleanup batch)
- **Tự nhiên consolidate khi reach threshold** → subdir matures (≥5 files) → no migration needed
- **KHÔNG migrate** trong PR landing rule này — separate scope, avoid scope creep

**Migration mechanics (when triggered):**

```bash
# Flatten subdir back to parent với prefix rename
git mv documents/<parent>/<subdir>/<file>.md \
       documents/<parent>/<subdir>-<file>.md
rmdir documents/<parent>/<subdir>/
# Update inbound links via grep + Edit tool
```

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Reviewer-checklist (active immediately)

Pre-merge review cho PR thêm subdirectory mới dưới `documents/**`:

- [ ] PR thêm directory mới (path ending `/`) under `documents/**`?
- [ ] Nếu CÓ → đếm files trong new subdir (hiện tại + planned ≤30 ngày trong PR description / wave plan):
  - ≥5 files → ✅ Volume criterion thỏa
  - <5 files → check §2 criteria khác
- [ ] Nếu KHÔNG criterion nào thỏa → request rewrite: "place file in-place tại parent folder thay vì tạo subdir"
- [ ] Subdir creation phải có inline rationale trong PR body cite §2 criterion nào thỏa

### 5.2 PR template (active — extend existing checklist)

Thêm row vào `.github/PULL_REQUEST_TEMPLATE.md` Output Review Checklist:

```markdown
- [ ] **Docs subfolder maturity** — nếu PR thêm new subdirectory under `documents/**`, MỘT trong §2 thresholds thỏa (Volume ≥5 files / cross-author ≥2 / reviewer approval / sister-pattern) per `.claude/rules/docs-subfolder-maturity.md`
```

### 5.3 CI detector script (SHIPPED Wave 99C GAP-675)

Script `scripts/check-docs-subfolder-maturity.sh` shipped 2026-05-19 per Wave 99C META-META audit (GAP-675 SHIP-NOW verdict — trivial bash ~115 LOC, low FP risk). Wired CI job `docs-scaling-detectors`:
- WARN-mode initially (177 existing grandfathered subdirs surface — rule prospective)
- Self-test PASS (single-file + 3-file flagged; 5-file at threshold + README+5 exempted)
- Excludes `archived/`, `closed/`, `07-archived/` per §2 exemptions
- Override trailer: `DOCS_SUBFOLDER_MATURITY_OVERRIDE: <subdir> — <reason>`
- HARD STOP target Wave 100+ after consolidation pass for grandfathered violations

Re-enable HARD STOP follow-up tracked GAP-679.

### 5.4 Memory auto-load (optional, deferred)

Memory entry `feedback_docs_subfolder_maturity.md` could remind session start trước khi tạo subdir. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; path-scoped auto-load + reviewer-checklist + worked self-test đủ cho v1.0.0.

### 5.5 Override mechanism

Genuine exception ngoài §2 criteria:

```
git commit -m "...
DOCS_SUBFOLDER_MATURITY_OVERRIDE: <subdir-path> — <reason — vd 'isolated scope cho legal compliance docs, độc lập với parent shared concerns'>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 6. Override mechanism (detailed)

Khi nào dùng override:

| Case | Why exempt |
|---|---|
| Legal/compliance isolation | Subdir riêng cho PDPL/MPS docs để audit trail clear, dù chỉ 1-2 files |
| Vendor-specific scope | Subdir cho từng vendor (vd `vendor-aws/`, `vendor-cloudflare/`) khi clearly partitioned ownership |
| Generated artifact scope | Subdir cho auto-generated docs (vd `generated/`) — clearly distinct từ hand-written |
| Phase boundary | Subdir cho `phase-2/` content được phát triển isolated trước cutover |

Documenting override:
- Inline trong PR body với rationale rõ ràng
- Plus commit trailer `DOCS_SUBFOLDER_MATURITY_OVERRIDE: <path> — <reason>`
- Reviewer xác nhận inline trong PR review

---

## 7. Worked self-test — retroactive apply trên `documents/05-guides/` 17 subdirs

Apply rule retroactively (rule **prospective only** — không migrate; chỉ để verify rule fires correctly):

### 7.1 Subdir verdicts

| # | Subdir | Files | Verdict | §2 criterion |
|---|---|:---:|:---:|---|
| 1 | `account-prep/` | 10 | ✅ PASS | Volume ≥5 |
| 2 | `branding/` | 3 | ❌ FAIL threshold | None — should be in-place OR grow to ≥5 |
| 3 | `contributing/` | 4 | ❌ FAIL threshold | <5, borderline; check planned items |
| 4 | `deploy/` | 28 | ✅ PASS | Volume ≥5 |
| 5 | `dev/` | 5 | ✅ PASS | Volume ≥5 (exactly threshold) |
| 6 | `infrastructure/` | 4 | ❌ FAIL threshold | <5, borderline |
| 7 | `local-dev/` | 6 | ✅ PASS | Volume ≥5 |
| 8 | `monitoring/` | 5 | ✅ PASS | Volume ≥5 (exactly threshold) |
| 9 | `operations/` | 28 | ✅ PASS | Volume ≥5 |
| 10 | `remote-access/` | 3 | ❌ FAIL threshold | <5 |
| 11 | `rtk-pilot/` | 2 | ❌ FAIL threshold | <5; transient pilot artifact — should consolidate hoặc archive |
| 12 | `scripts/` | 0 + 1 nested | ❌ FAIL threshold | Empty parent — files only in `ssh-mobile-migration/` nested; should flatten |
| 13 | `security/` | 1 | ❌ FAIL threshold | Classic single-file subdir anti-pattern |
| 14 | `templates/` | 2 | ❌ FAIL threshold | <5; sister-pattern check needed |
| 15 | `tenant-lifecycle/` | 3 | ❌ FAIL threshold | <5 |
| 16 | `user-manual/` | 1 + 4 nested persona dirs | ✅ PASS | Sister-pattern (persona split per `user-manual-content-standard.md` §3) + planned Volume ≥5 each persona |
| 17 | `vietnamese/` | 8 | ✅ PASS | Volume ≥5 |

### 7.2 Verdict summary

- **8/17 PASS** — Volume criterion thỏa hoặc sister-pattern justified
- **9/17 FAIL threshold** retroactive — would have been blocked by rule if applied at creation time

### 7.3 Sample 5 violations (retroactive — grandfathered, not migrated)

Top 5 candidates cho future consolidation pass (P2 priority, batch với cleanup wave):

| # | Subdir | Files | Recommendation |
|---|---|:---:|---|
| 1 | `documents/05-guides/security/` | 1 | Flatten → `documents/05-guides/security-<topic>.md` in-place |
| 2 | `documents/05-guides/rtk-pilot/` | 2 | Archive nếu pilot done → `documents/07-archived/rtk-pilot-2026/` |
| 3 | `documents/05-guides/scripts/` | 0 (empty parent) | Flatten nested `ssh-mobile-migration/` 4 files lên parent OR rename parent từ `scripts/` thành descriptive name |
| 4 | `documents/05-guides/templates/` | 2 | Check sister-pattern OR flatten |
| 5 | `documents/05-guides/branding/` | 3 | Borderline — check planned files; nếu <5 in 30 ngày → flatten |

### 7.4 Counterfactual

Nếu rule áp dụng từ đầu khi `documents/05-guides/` đầu tiên được structure:

- 9 subdirs trên đã KHÔNG được tạo → 9 single-file/single-purpose docs in-place tại `documents/05-guides/<topic>.md`
- Reader cognitive load giảm: từ "17 subdirs scattered" xuống "8 mature subdirs + 9 in-place top-level docs"
- File discoverability via `ls documents/05-guides/` reveals immediate file names thay vì nested folder names

→ **Rule fires correctly trên retroactive sample.** 9/17 subdirs would have been rejected by §2 criteria. Self-test PASS ✅

---

## 8. Relationship to other rules

- **`docs-folder-structure.md`** §3 README Template — sister rule covering shape OF folder (README mandatory + 4 sections); THIS rule covers WHEN to create folder. Both apply: §2 thresholds thỏa → create subdir → MUST also create README per docs-folder-structure.md
- **`planning-docs-structure.md`** — specific cho `documents/03-planning/` (frontmatter + archival); THIS rule generic across `documents/**`
- **`design-patterns.md`** §1.1 YAGNI — codify "don't pre-emptive organize"; THIS rule áp dụng YAGNI vào folder structure decisions
- **`docs-only-pr-auto-merge.md`** — auto-merge eligible khi diff docs-only; rule này check applies tới subdir creation PRs (auto-merge OK nếu §2 criterion documented in PR body)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + PR template + worked self-test all ship same PR
- **`incident-to-rule-pipeline.md`** — rule này là direct output 2026-05-18 user-flagged miss: "documents/05-guides có 17 subdirs, nhiều subdir chỉ 1-2 files (vd `professional-manual/` mới có 1 file đã tách subdir)" applied through 5-stage pipeline
- **`output-review-mandate.md`** §3 — review standard preserved cho docs scope; THIS rule adds gate at subdir-creation moment
- **`meta-gap-priority.md`** §3 — Meta-P0 force-multiplier (folder structure decisions affect mọi future reader navigation)

---

## 9. Log

- **2026-05-19 (v1.0.1):** PATCH — Wave 99C META-META GAP-675 SHIP-NOW closure of deferred-detector debt §5.3. `scripts/check-docs-subfolder-maturity.sh` shipped (115 LOC bash; 3-fixture self-test PASS); wired CI job `docs-scaling-detectors`. WARN-mode initially per `incident-to-rule-pipeline.md` premature-rule guard tightened §3 conditions; 177 grandfathered subdirs surface as actionable WARN. No constraint change; detector enforces existing §2 threshold prospectively. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — additive enforcement, no constraint loosening).
- **2026-05-18 (v1.0.0):** Rule created — Rule 2 of 4 docs scaling pack. Triggered by user-flagged 2026-05-18 miss "documents/05-guides có 17 subdirs nhưng nhiều subdir chỉ 1-2 files (vd `professional-manual/` mới có 1 file đã tách subdir); subdir sprawl tăng cognitive load không add navigation value". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged sprawl pattern) → Classify ✓ (no existing rule codifies subdir maturity criteria; `docs-folder-structure.md` covers WHAT subdir contains, không covers WHEN subdir warranted; `planning-docs-structure.md` specific cho 03-planning only) → Rule+Enforce ✓ (this file + path-scoped frontmatter `documents/**` + reviewer-checklist + PR template row + worked self-test trên 17 retroactive subdirs per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example — 9/17 subdirs would FAIL threshold retroactive, validates rule fires correctly on originating concern) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying folder structure discipline; no constraint loosening for prior work; existing 17 subdirs grandfathered per `rule-change-process.md` convention; rule applies prospectively từ Wave 92 forward). CI grep detector + memory auto-load deferred per premature-rule guard ≥7 ngày; v1.0.0 enforcement = path-scoped auto-load + reviewer-checklist + PR template + worked self-test sufficient. Atomic-unique-bar §5.1 check passed: atomic concept (subdir creation discipline) / unique scope (no overlap với `docs-folder-structure.md` shape rules) / widely applicable (mọi `documents/**` subdir creation) / body discipline ≤2 "and" conjunctions in §1.
