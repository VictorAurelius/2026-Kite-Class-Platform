---
paths:
  - "documents/03-planning/waves/**"
  - ".claude/skills/quality/wave-pack-planner/**"
---

# Wave Tag-based Numbering Convention — `wave-{tag}-{counter}` format

**Priority:** 🟠 MANDATORY — wave naming + history schema governance
**Version:** 1.0.0
**Created:** 2026-05-23
**Last-Reviewed:** 2026-05-23
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test trên Wave thesis-1 — chính wave này làm self-test) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies new naming scheme prospectively, existing Wave 01-107 grandfathered per §5 migration policy)
**Applies to:** Mọi wave plan file mới (sau 2026-05-23), commit message format, branch naming, `wave-history.jsonl` schema, ROADMAP §🎯 entries. Out-of-scope: Wave 01-107 cũ (giữ sequential numbering — không backfill tag).

---

## 1. The Rule

> **Wave mới (từ 2026-05-23) PHẢI dùng format `wave-{tag_primary}-{counter}` với 1 primary tag + N secondary tags (frontmatter).** Counter tăng monotonic per tag (thesis-1, thesis-2, thesis-3, ...). Wave cũ 01-107 giữ sequential number, KHÔNG backfill.

Format `wave-{counter}` (Wave 1, Wave 2, ..., Wave 107) đã dùng từ early project (~Wave 01 2026-04-19 → Wave 107 2026-05-22). Vấn đề:

- Counter chạy linear → mất signal về **scope/theme** của wave (Wave 73 = meta context optimization? Wave 88 = Vercel decommission? phải tra wave-history.jsonl)
- Cross-cutting waves (vd thesis-related work span Wave 100/100.5/100.7/102/102.6-9/108-…) khó group bằng counter
- Future query "tất cả wave thesis" require grep wave-history.jsonl với keyword fuzzy

Tag-based scheme `wave-thesis-1` solves:
- Reader thấy `wave-thesis-1-closure` → biết ngay scope = thesis
- Query "tất cả wave thesis" = grep `tag_primary=thesis` trong history
- Future scope split (vd `wave-beta-1`, `wave-security-1`) tổ chức tự nhiên

User direction 2026-05-23 chốt scheme này per AskUserQuestion 3 chiều (format / multi-tag / migration).

---

## 2. Format specification

### 2.1 Wave identifier

```
wave-{tag_primary}-{counter}[-{descriptor}]
```

| Component | Rule |
|---|---|
| `wave-` | Fixed prefix |
| `{tag_primary}` | lowercase-kebab-case slug, ≥3 ký tự, semantic theme (thesis / beta / security / meta / ops / ...) |
| `{counter}` | Monotonic integer per tag, start từ 1, no skip (thesis-1, thesis-2, thesis-3...) |
| `{descriptor}` | Optional — kebab-case mô tả scope (closure / hardening / rollout / fix-bugs) |

**Examples:**
- `wave-thesis-1-closure` ✅
- `wave-thesis-2-nfr-evidence` ✅
- `wave-beta-1-cohort-execution` ✅
- `wave-security-1-cve-batch` ✅
- `wave-meta-1-context-optimization` ✅ (retroactive label cho Wave 73 — chỉ documentation, không rename file)

**Banned:**
- `wave-Thesis-1` (UPPERCASE) ❌ — lowercase-kebab-case mandate per `docs-filename-prefix-convention.md` Tier 3
- `wave-thesis-01` (zero-padded counter) ❌ — integer only, không pad
- `wave-thesis-1.5` (decimal counter) ❌ — sub-wave dùng descriptor (`wave-thesis-1-fix-bundle`) hoặc next integer (`wave-thesis-2`)
- `wave-thesis` (missing counter) ❌ — counter mandatory
- `wave-thesis/1` (slash separator) ❌ — dash separator để khớp filename + branch convention

### 2.2 Wave plan filename

Per `docs-filename-prefix-convention.md` Tier 3 typed prefix + date:

```
wave-{YYYY-MM-DD}-{tag_primary}-{counter}-{descriptor}.md
```

**Examples:**
- `documents/03-planning/waves/wave-2026-05-23-thesis-1-closure.md` ✅
- `documents/03-planning/waves/wave-2026-06-15-thesis-2-nfr-evidence.md` ✅

### 2.3 Branch + commit message

| Artifact | Format | Example |
|---|---|---|
| Branch | `wave/{tag_primary}-{counter}-{descriptor}` | `wave/thesis-1-closure` |
| Plan PR commit | `plan(wave-{tag_primary}-{counter}): {summary}` | `plan(wave-thesis-1): closure 6 bucket parallel + META convention prereq` |
| Bucket PR commit | `feat(wave-{tag_primary}-{counter}-bucket-{X}): {summary}` | `feat(wave-thesis-1-bucket-A): citation-extract skill + GAP-647 step 3` |
| Closure PR commit | `chore(wave-{tag_primary}-{counter}-closure): {summary}` | `chore(wave-thesis-1-closure): sync csv + roadmap + handoff` |

### 2.4 Frontmatter trong wave plan file

```yaml
---
wave: 1
tag_primary: thesis
tags_secondary: [doc, beta-prep, meta]
counter: 1
date_launch: 2026-05-23
status: planning | in-progress | shipped | abandoned
---
```

**Rules:**
- `wave` field = `counter` (duplicate cho backward-compat với existing wave-history schema)
- `tag_primary` SINGLE string (1 primary tag mandate per user direction)
- `tags_secondary` ARRAY of strings (0+ secondary tags, optional)
- `counter` mirrors `wave` field — kept for explicit query
- `date_launch` ISO 8601 (date plan locked, not docx generation)
- `status` enum tracking lifecycle

### 2.5 `wave-history.jsonl` schema (extended)

Existing entries (Wave 01-107 sequential) **giữ nguyên format** (per user direction "không backfill"):

```json
{"wave":"104","date":"2026-05-22","theme":"Wave 104 Fix Follow-up Bugs",...}
```

New entries (từ 2026-05-23) MUST add new fields:

```json
{
  "wave": "thesis-1",
  "tag_primary": "thesis",
  "tags_secondary": ["doc", "beta-prep", "meta"],
  "counter": 1,
  "date": "2026-05-23",
  "theme": "Thesis closure 6 bucket parallel",
  "gaps": ["GAP-647", "GAP-651", "..."],
  "...": "..."
}
```

**Query patterns:**

```bash
# Tất cả wave thesis (new format)
jq 'select(.tag_primary == "thesis")' .claude/skills/quality/wave-pack-planner/data/wave-history.jsonl

# Tất cả wave có secondary tag "doc" (new format)
jq 'select((.tags_secondary // []) | index("doc"))' wave-history.jsonl

# Wave cũ sequential (legacy entries)
jq 'select(.wave | test("^[0-9]+(\\.[0-9]+)*$"))' wave-history.jsonl
```

### 2.6 ROADMAP entry

ROADMAP §🎯 Current Status Snapshot entry format:

```markdown
- **Wave thesis-1** (2026-05-23): Thesis closure 6 bucket parallel — DONE 7/7 gap (647/651/653/655/689/623/652) + 1 PARTIAL (687 Phase 1+2 ship)
```

Wave cũ entries giữ "Wave NNN" sequential format.

---

## 3. Counter rules

### 3.1 Monotonic per tag

Counter PHẢI tăng monotonic 1, 2, 3, ... cho mỗi `tag_primary`. Tags độc lập:

- `thesis-1`, `thesis-2`, `thesis-3` (counter trong tag thesis)
- `beta-1`, `beta-2` (counter trong tag beta — independent của thesis counter)
- `meta-1`, `meta-2` (independent)

### 3.2 No skip, no reset

- `thesis-3` rồi nhảy `thesis-5` ❌ — counter no skip
- Abandoned wave vẫn count: nếu `thesis-2` filed plan rồi abort, `thesis-3` next wave vẫn dùng counter 3 (không reuse 2)
- Counter NOT reset theo quarter/year — total monotonic per tag

### 3.3 Sub-wave naming

Cho fix bundles / amendments sau khi wave ship, dùng descriptor thay vì decimal:

- ❌ `wave-thesis-1.1` (decimal sub-wave)
- ✅ `wave-thesis-1-fix-bundle` (descriptor variation, cùng counter 1)
- ✅ `wave-thesis-2-amendment` (next integer + descriptor)

**Decision rule:** nếu work là cleanup/fix cho previously-shipped wave → cùng counter + descriptor. Nếu work là new scope → next counter.

### 3.4 Multi-tag membership

1 wave có 1 `tag_primary` driving counter + 0+ `tags_secondary` (descriptive only, no counter):

```yaml
tag_primary: thesis    # drives counter (thesis-1)
tags_secondary: [doc, beta-prep, meta]    # descriptive, no counter
```

Query "tất cả wave touching beta-prep" = match `tag_primary=beta-prep` OR `tags_secondary contains beta-prep`:

```bash
jq 'select(.tag_primary == "beta-prep" or ((.tags_secondary // []) | index("beta-prep")))' wave-history.jsonl
```

---

## 4. Tag taxonomy (recommend, không hard-enforce v1.0.0)

Khuyến nghị tag_primary semantic taxonomy:

| Tag | Use case |
|---|---|
| `thesis` | Khóa luận / academic deliverable scope |
| `beta` | Beta tenant cohort / invite execution |
| `security` | CVE / pen-test / auth hardening |
| `meta` | Skills / rules / workflow governance |
| `ops` | Infrastructure / deploy / observability |
| `feature-{domain}` | Product feature scope (vd `feature-billing`, `feature-attendance`) |
| `hotfix` | P0 production incident response |
| `release-{N}` | Release-specific scope (vd `release-1-rc`) |

Mở rộng tag mới ad-hoc OK — không cần update rule. Tag name lowercase-kebab-case, ≥3 ký tự.

---

## 5. Migration path (no backfill)

Wave 01-107 sequential **giữ nguyên** — KHÔNG rename file, KHÔNG update history entry, KHÔNG add tag retroactively. Lý do per user direction:
- Cost migration cao (107+ entry × manual tag classification)
- Risk wrong-assign cao (Wave 102.7.6 = thesis-? hay polish-?; ambiguous)
- Wave cũ đã ship → history immutable per `output-review-mandate.md` audit artifact principle

**Hybrid query handle:** `wave-history.jsonl` 2 format coexist. Query scripts MUST handle both:

```bash
# Legacy sequential: wave="104", wave="102.7.6"
# New tag-based: wave="thesis-1", tag_primary="thesis", counter=1

# Combined query "all thesis-related waves":
jq 'select(
  .tag_primary == "thesis" or
  (.tags_secondary // [] | index("thesis")) or
  (.theme | ascii_downcase | test("thesis|khóa luận|khoa luan"))
)' wave-history.jsonl
```

Per `outside-in-coverage-trigger.md` §3 — if future query tooling can't handle hybrid format gracefully → file follow-up gap track schema unification.

---

## 6. Worked self-test — Wave thesis-1 (THIS wave)

Apply rule prospectively to Wave thesis-1 (the very wave shipping this rule):

| Artifact | Expected per rule | Actual |
|---|---|---|
| Wave identifier | `wave-thesis-1-closure` | ✅ |
| Plan filename | `wave-2026-05-23-thesis-1-closure.md` | ✅ (created same PR) |
| Branch | `wave/thesis-1-closure` | ✅ (checked out same session) |
| Plan PR commit | `plan(wave-thesis-1): closure 6 bucket parallel + META convention prereq` | ✅ (this PR commit message) |
| Bucket commit format | `feat(wave-thesis-1-bucket-{A-F}): ...` | ✅ (will apply Phase 2 spawn) |
| Frontmatter | wave=1, tag_primary=thesis, tags_secondary=[doc, beta-prep, meta], counter=1, date_launch=2026-05-23 | ✅ (plan file frontmatter) |
| wave-history.jsonl entry | New format với tag_primary + tags_secondary + counter | ✅ (will append Phase 3 closure) |
| ROADMAP entry | "Wave thesis-1 (2026-05-23): ..." | ✅ (will update Phase 3 closure) |

**Counterfactual without rule:** Wave thesis-1 would have shipped as `Wave 108` per sequential pattern. Future scope split (vd Wave thesis-2 NFR + beta) would inherit Wave 109 counter, losing semantic group signal. Query "tất cả wave thesis" require fuzzy grep keywords — high false-positive risk.

**Verdict:** Rule fires correctly trên originating wave (Wave thesis-1 itself). Self-test PASS ✅.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Backfill tag retroactively cho Wave 01-107 | Wave cũ giữ sequential — `tag_primary` field null/missing OK trong query |
| Skip counter (`thesis-1` → `thesis-5`) | Monotonic 1, 2, 3, ... no skip |
| Reuse counter (`thesis-2` abandoned → next wave reuses `thesis-2`) | Abandoned wave consume counter; next wave = `thesis-3` |
| Decimal sub-wave (`thesis-1.1`) | Descriptor variation (`thesis-1-fix-bundle`) |
| Multiple `tag_primary` (`tag_primary: [thesis, beta]`) | Single primary, multi secondary (per §3.4) |
| Branch `wave/thesis/1-closure` (slash) | `wave/thesis-1-closure` (dash) |
| Filename `wave-thesis-1.md` thiếu date | `wave-2026-05-23-thesis-1-closure.md` per `docs-filename-prefix-convention.md` Tier 3 |
| Counter zero-padded `wave-thesis-01` | Integer no pad (`wave-thesis-1`, `wave-thesis-10`) |
| Wave cũ "Wave 105" rename to "Wave persona-1" retroactively | KHÔNG — preserve history per §5 migration policy |
| Drop `tag_primary` field trong new entries | Mandatory cho new entries (post 2026-05-23) |

---

## 8. Enforcement (per `rule-change-process.md` §6.5)

### 8.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching wave plan file / wave-history.jsonl / wave-related commit:

- [ ] Wave identifier format `wave-{tag}-{counter}` per §2.1?
- [ ] Filename format `wave-{date}-{tag}-{counter}-{descriptor}.md` per §2.2?
- [ ] Branch + commit format per §2.3?
- [ ] Frontmatter có `tag_primary` + `counter` (new format) per §2.4?
- [ ] Counter monotonic (không skip / không reuse) per §3.1/3.2?
- [ ] Sub-wave dùng descriptor không decimal per §3.3?
- [ ] Single `tag_primary` (không multi) per §3.4?
- [ ] Wave cũ KHÔNG bị backfill per §5?

### 8.2 Cross-reference với `docs-filename-prefix-convention.md`

Tier 3 typed prefix mandate (`wave-{YYYY-MM-DD}-{N}-`) extended với tag insertion (`wave-{YYYY-MM-DD}-{tag}-{counter}-`). Filename rule (this rule §2.2) + Tier 3 docs filename rule cùng wear một guard:
- This rule: tag + counter format trong filename
- That rule: date prefix + descriptive slug

Both ship same Wave thesis-1 PR per Enforcement Parity Mandate.

### 8.3 Cross-reference với `wave-pack-planner` skill

Skill `.claude/skills/quality/wave-pack-planner/SKILL.md` Section "Wave numbering" cập nhật cùng PR — reference rule này + show concrete examples. Skill agent prompts từ Wave thesis-1+ MUST follow tag-based format.

### 8.4 CI grep detector (deferred per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

- **Detector complexity:** Tag taxonomy validation + counter monotonic check + JSONL schema validation across 2 formats (legacy + new) — moderate scope (~80 LOC)
- **Recurrence count:** 0 post-merge (rule shipped 2026-05-23)
- **FP risk:** Moderate — legacy entries (Wave 01-107) WILL fail new schema; detector PHẢI handle hybrid gracefully
- **Decision:** Reviewer-checklist §8.1 + worked self-test §6 sufficient cho v1.0.0; revisit detector khi recurrence-count ≥2 OR query tooling fails to handle hybrid format

Future heuristic regex (when implemented, WARN-mode):

```bash
# Detect new wave file thiếu tag_primary frontmatter
find documents/03-planning/waves/ -name "wave-2026-05-2[3-9]*.md" -newer documents/03-planning/waves/wave-2026-05-22-105-persona-walk-beta-readiness.md \
  | xargs -I {} sh -c 'grep -L "^tag_primary:" "{}"' \
  && { echo "WARN: new wave file thiếu tag_primary frontmatter per wave-tag-numbering-convention.md §2.4"; exit 0; }

# Detect counter skip trong tag
jq -r 'select(.tag_primary) | "\(.tag_primary) \(.counter)"' wave-history.jsonl \
  | sort -k1,1 -k2,2n \
  | awk 'prev_tag == $1 && $2 != prev_counter+1 {print "FAIL: counter skip in", $1, "at", $2; exit 1} {prev_tag=$1; prev_counter=$2}'
```

### 8.5 Override mechanism

Genuine exception (vd emergency hotfix, retroactive labeling cho cross-cutting work):

```
git commit -m "...
WAVE_TAG_NUMBERING_OVERRIDE: <reason — vd 'emergency hotfix, defer tag classification to follow-up'>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review của taxonomy.

---

## 9. Relationship to other rules

- **`docs-filename-prefix-convention.md`** Tier 3 typed prefix — sister rule; this rule extends Tier 3 với tag insertion. Both ship cùng wave Wave thesis-1.
- **`wave-pack-planner` skill** — operationalizes wave numbering; cập nhật cùng PR.
- **`meta-csv-index-pattern.md`** — wave-history.jsonl không phải CSV (JSONL native) nhưng pattern parity (canonical source for query). Tag-based query mirrors CSV-based query convention.
- **`output-review-mandate.md`** §3 — paired same-PR với new matrix row "Wave naming convention" tracking this rule's review standard.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 + skill update + matrix row + rules-index.csv row + plan file (self-test artifact) all ship same PR (Wave thesis-1 plan PR).
- **`incident-to-rule-pipeline.md`** — applied 5-stage: Detect ✓ (user direction 2026-05-23 "đánh tag thay vì 108") → Classify ✓ (no existing rule codifies wave naming; emergent sequential pattern Wave 01-107 chưa formal) → Rule+Enforce ✓ (this file + skill update + matrix row + paired self-test wave thesis-1 plan) → Self-Test ✓ (§6 Wave thesis-1 itself) → Retro Log ✓ (§10 below).
- **`context-budget-mandate.md`** §3.2 — path-scoped `paths: ["documents/03-planning/waves/**", ".claude/skills/quality/wave-pack-planner/**"]` per §3.1 (rule không global auto-load, save context budget).
- **`outside-in-coverage-trigger.md`** §4 row 4 — Wave thesis-1 plan SKIP outside-in audit per recent audit ≤30 ngày (Wave 100 3-agent 2026-05-19 covered thesis surface).

---

## 10. Log

- **2026-05-23 (v1.0.0 self-test PASS):** Wave thesis-1 SHIPPED 6 bucket parallel + closure PR — §6 self-test confirmation 8/8 expected artifacts match (wave identifier + plan filename + branch + plan PR commit + frontmatter + 6 bucket commits + wave-history.jsonl entry mới format + ROADMAP entry). First wave dùng tag-based scheme; ~3.5h wall-clock vs ~5-6h estimate (6.9x speedup). 7 PR merged (#1748-#1754 + closure); 7 thesis gap DONE + 1 PARTIAL; 3 gap defer Wave thesis-2 chờ GAP-612. No rule revisions needed — convention applied cleanly. Reviewer: @nguyenvankiet (solo-dev — self-test PASS, no version bump per `rule-change-process.md` §5 — confirmation entry only).
- **2026-05-23 (v1.0.0):** Rule created per user direction 2026-05-23 AskUserQuestion 3 chiều: format `wave-thesis-1` selected (Option 1), multi-tag "1 primary + N secondary" selected, no backfill selected. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged "thay vì đánh 108, đánh tag + số 1") → Classify ✓ (no existing rule codifies wave naming convention; emergent sequential pattern Wave 01-107 informal; cross-cutting wave grouping pain point user-flagged) → Rule+Enforce ✓ (this file + paired same-PR: `docs-filename-prefix-convention.md` Tier 3 cross-link + `wave-pack-planner` SKILL.md Section "Wave numbering" + `output-review-mandate.md` §3 matrix row "Wave naming convention" + `rules-index.csv` row + Wave thesis-1 plan file as worked self-test per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên Wave thesis-1 itself — rule fires correctly + 8/8 expected artifacts match) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered wave naming class; no constraint loosening; Wave 01-107 grandfathered per §5 migration policy; rule applies prospectively từ Wave thesis-1 forward 2026-05-23). Atomic-unique-bar §5.1 check: ✅ atomic (single concept: wave naming + counter scheme) / ✅ unique (no existing rule covers wave naming) / ✅ widely applicable (every future wave) / ✅ body discipline §1 ≤2 "and" conjunctions. CI grep detector (§8.4) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (moderate complexity + recurrence 0 + FP risk hybrid format); reviewer-checklist + worked self-test §6 sufficient cho v1.0.0.
