# Test Artifact Format Standard — CSV canonical, XLSX generated

**Priority:** 🟠 MANDATORY — test artifact format governance
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (artifact-type matrix + CSV format requirements + companion-files mandate + reviewer-checklist + worked self-test on Wave 72a Bucket F incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-implicit acceptance test convention)
**Applies to:** Every test artifact lifecycle — acceptance test matrix, regression test plan, smoke test checklist, manual QA script. Scope explicitly includes folder `documents/05-guides/operations/acceptance-tests/**` and any other location storing per-release manual walkthrough docs.

---

## 1. The Rule

> **CSV là canonical format cho acceptance test matrix** (git-tracked, LLM-readable, diff-able). **XLSX là generated artifact on-demand** (gitignored, render bằng script khi cần Excel/Sheets UX). **Markdown/bash phù hợp cho test class khác (regression scenarios / executable smoke / QA report)** — đừng dùng nhầm format.

Wave 72a Bucket F (2026-05-14) ship CSV acceptance test matrix nhưng thiếu UTF-8 BOM (Excel mis-detect encoding) + thiếu XLSX render script + để file ở `operations/` root thay vì subfolder dedicated. Rule này codify convention để recurrence không xảy ra.

---

## 2. Decision matrix per artifact type

Mỗi loại test artifact có canonical format khác nhau. Đừng dùng CSV cho mọi thứ.

| Artifact type | Canonical format | Why | Companion |
|---|---|---|---|
| **Acceptance test matrix** (per-release, tester ticks status row-by-row) | **CSV** (UTF-8 BOM + RFC 4180 quoting) | Tabular structure, tick-trackable trong spreadsheet, machine-readable cho future automation | XLSX render script + README.md cho mỗi matrix |
| **Regression test scenarios** (developer reads, không tick row) | **Markdown** với table | Narrative + table flexible cho complex flow; reviewed via PR diff | None — markdown native |
| **Smoke test commands** (executable, CI runs) | **Bash script** | Code IS the test; `bash -e` exit code = test result | None |
| **QA report after-run** (post-walkthrough summary) | **Markdown** với screenshots + table | Findings narrative + per-row breakdown + evidence attached | Screenshots in `assets/` subfolder |
| **Threat model** (security review per critical flow) | **Markdown** với mermaid sequence diagrams | Narrative + structured sections (trust boundaries, abuse cases, mitigations) | None |
| **Persona-based business review report** | **Markdown** với role-play scenarios | Per-persona narrative + acceptance criteria checklist | None |

Khi unsure: đọc lại scope của test artifact. Nếu nó là **list of steps tester ticks** → CSV. Nếu nó là **prose + tables developer reads** → Markdown. Nếu nó là **shell commands CI runs** → Bash.

---

## 3. CSV format requirements (cho acceptance test matrices)

### 3.1 Required structure

- **UTF-8 với BOM (`\xef\xbb\xbf` ở 3 byte đầu)** — Excel detect encoding đúng. Verify: `head -c 3 file.csv | od -c | head -1` → `357 273 277` (octal).
- **RFC 4180 quoting** — values chứa comma/newline/quote phải quote bằng `"..."`; escape `"` bên trong bằng `""`. Python `csv.writer` default đã đúng.
- **Header row** (line 1) = column names tiếng Anh (technical identifier cross-locale stable).
- **Stable column order** — KHÔNG reorder columns sau khi matrix shipped (downstream tools/scripts có thể parse theo column index).
- **Empty cells acceptable** — đừng dùng `null` / `n/a` / `none` literal trừ khi convention rõ ràng (vd `status` column dùng enum `pass`/`fail`/`blocked`/`-`/empty).

### 3.2 Column-name vs value language

Per [`.claude/rules/dev-readable-doc-language.md`](dev-readable-doc-language.md):

- **Column names = English** (technical identifier — `flow_id`, `persona`, `step_num`, `expected_result`, `verify_via`, `status`, `blocker_gap`). KHÔNG dịch sang Việt — break cross-locale parsing.
- **Codes/identifiers/enums = English** giữ nguyên (`PUB-LAND-001`, `P2_Center_Owner`, `GAP-519`, `BETA_APPROVE`).
- **Sample input_data fields** chứa Vietnamese names/locations → giữ Việt (đã VN-friendly tự nhiên).
- **Narrative cells** (`step_title`, `action`, `expected_result`, `verify_via`, `notes`) = **Vietnamese** cho dev đọc tự nhiên.
- **Technical terms inside Vietnamese sentences** (HTTP, JWT, CORS, SSM, DevTools, ...) giữ English — natural code-switching trong Vietnamese tech writing.

### 3.3 Recommended columns cho acceptance test matrix

Pattern shipped Wave 72a (proven format):

```
flow_id,persona,phase,step_num,step_title,prerequisite,action,input_data,expected_result,verify_via,status,blocker_gap,notes
```

Mỗi cột:
- `flow_id` — unique step ID (vd `PUB-LAND-001`), prefix theo persona/scope
- `persona` — enum theo personas-catalog (`Anonymous` / `Pre-tenant` / `P2_Center_Owner` / `Teacher` / `Pa_Parent` / `Platform_Admin` / `Student` / `All`)
- `phase` — bucket phân loại (`Setup` / `Auth` / `Provisioning` / `Class_Mgmt` / `Attendance` / `Grade` / `Payment` / `Settings` / `Admin_Ops` / `Data_Export` / `Off-boarding`)
- `step_num` — số thứ tự trong flow
- `step_title` — Vietnamese, 1 câu ngắn mô tả bước
- `prerequisite` — flow_id hoặc `none`
- `action` — Vietnamese, hành động user/tester thực hiện
- `input_data` — semicolon-separated `key=value`; sample values pre-filled
- `expected_result` — Vietnamese, kết quả mong đợi (HTTP code + UI behavior + DB state)
- `verify_via` — Vietnamese, cách verify (UI / DB query / Network tab / inbox)
- `status` — empty / `pass` / `fail` / `blocked` / `-` (skip)
- `blocker_gap` — `GAP-NNN` nếu bị block
- `notes` — Vietnamese, free-form

Pattern này SHOULD áp dụng cho future per-release matrix; deviations cần documented inline (vd Phase 1.5 PAID có thể thêm column `payment_method`).

---

## 4. XLSX render requirements (companion artifact)

XLSX là **render-on-demand** từ canonical CSV. Yêu cầu:

| Feature | Mandatory | Why |
|---|:---:|---|
| Header row bold + colored background | ✅ | Quickly scan column layout |
| Frozen header pane (`A2`) | ✅ | Header sticky khi cuộn |
| Auto-fit column widths (cap ≤60 chars) | ✅ | Vietnamese chars wide; avoid super-narrow + super-wide |
| Wrap text trong narrative cells | ✅ | `expected_result` cells dài, wrap thay vì cut |
| Tab name = filename không đuôi | ✅ | Self-describing khi open multi-XLSX |
| Alternating row stripes | ⚠️ Optional | Nice-to-have, không required |
| Color-coded `status` column (pass=green, fail=red) | ⚠️ Optional | Phase 1.5+ enhancement |

### 4.1 Required render script per folder

Mỗi folder chứa acceptance test CSVs phải có script trỏ tới + README hướng dẫn. Project-wide canonical: [`scripts/render-acceptance-test-xlsx.sh`](../../scripts/render-acceptance-test-xlsx.sh).

Engines hỗ trợ:
1. **Python openpyxl** (preferred — full control over formatting)
2. **LibreOffice headless** (`--convert-to xlsx`) — fallback, formatting limited

Script phải tolerate missing engine và surface install hint, KHÔNG crash silent.

### 4.2 XLSX gitignored

XLSX là generated artifact — KHÔNG commit. Mỗi folder chứa CSVs có `.gitignore` exclude `*.xlsx`:

```gitignore
# XLSX là artifact generated từ CSV canonical — không commit
*.xlsx
```

Lý do: XLSX binary diff không meaningful; CSV diff đầy đủ thông tin. XLSX regenerate dễ.

---

## 5. Companion files mandate

Mỗi acceptance test CSV phải đi kèm:

| File | Purpose | Required |
|---|---|:---:|
| `<stem>.csv` | Canonical matrix | ✅ |
| `<stem>.md` | README giải thích cách dùng matrix + scope coverage + blocker gaps + iteration cadence | ✅ |
| `README.md` (folder-level) | Index folder + render script reference + format conventions | ✅ |
| `.gitignore` (folder-level) | Exclude `*.xlsx` | ✅ |
| `<stem>.xlsx` | XLSX render | ❌ gitignored (generated on-demand) |

Folder-level README + companion `<stem>.md` ensure new tester có thể onboard without questions.

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Ship CSV không có UTF-8 BOM → Excel decode lỗi tiếng Việt | `printf '\xef\xbb\xbf' > tmp && cat csv >> tmp && mv tmp csv` |
| Commit XLSX vào git | Add to `.gitignore`; render on-demand |
| Translate column names sang Việt (`flow_id` → `ma_buoc`) | Column names = English (technical, cross-locale stable) |
| Để narrative cells English (`Open landing page`) khi project Vietnamese | Translate narrative theo `dev-readable-doc-language.md` |
| Dùng markdown checkbox cho acceptance test 100+ rows | CSV — markdown checkbox không scale cho spreadsheet workflow |
| Reorder columns sau khi matrix shipped | Append columns cuối; existing tools parse theo index |
| Ship acceptance test trong `operations/` root cùng với runbook | Dedicated subfolder `operations/acceptance-tests/` |
| Skip companion README "vì CSV self-explanatory" | README giải thích scope coverage + blocker gaps + iteration cadence — không obvious từ CSV |
| Hard-code render logic trong README "copy paste this Python" | Ship script `scripts/render-acceptance-test-xlsx.sh` reusable |
| Mix smoke test commands + acceptance matrix trong cùng file | Smoke = bash script; matrix = CSV. Different artifacts. |

---

## 7. Worked self-test — Wave 72a Bucket F incident (2026-05-14)

**Scenario:** Wave 72a Bucket F PR #1288 ship `phase-1-beta-acceptance-self-test.csv` (126 rows pre-filled cho Phase 1 BETA acceptance). User mở CSV trong spreadsheet và flagged 4 issues:

1. **Folder placement** — file ở `documents/05-guides/operations/` root, mix với runbooks
2. **Old Plan 1 chưa archive** — `documents/03-planning/end-user/plan-1-self-test-e2e.md` chỉ có banner "superseded" nhưng file vẫn tồn tại
3. **English content** — narrative cells (`step_title`, `action`, `expected_result`, `verify_via`) đều English, vi phạm CLAUDE.md "ALWAYS communicate in Vietnamese" cho dev-readable docs
4. **No UTF-8 BOM** — `head -c 3 file.csv` → `f l o` thay vì `EF BB BF` → Excel mis-detect encoding

**Apply rule §3 + §4 + §5 retroactively:**

| Check | Wave 72a state | Required by rule | Verdict |
|---|---|---|---|
| §3.1 UTF-8 BOM | ❌ missing | ✅ mandatory | FAIL |
| §3.2 Narrative cells language | ❌ English | ✅ Vietnamese | FAIL |
| §3.2 Column names | ✅ English | ✅ English | PASS |
| §4.1 XLSX render script | ❌ missing | ✅ mandatory | FAIL |
| §4.2 XLSX gitignored | ❌ no `.gitignore` | ✅ mandatory | FAIL |
| §5 Folder placement | ❌ in `operations/` root | ✅ dedicated `acceptance-tests/` subfolder | FAIL |
| §5 Folder-level README | ❌ missing | ✅ mandatory | FAIL |
| §5 Companion README per matrix | ✅ exists | ✅ mandatory | PASS |

→ Rule fires correctly: 6/8 checks FAIL. Counterfactual: nếu rule landed trước Wave 72a Bucket F, agent sẽ ship CSV với BOM + Vietnamese narrative + dedicated subfolder + render script + .gitignore từ đầu, eliminate user retro round-trip.

**Verdict:** rule fires correctly trên originating incident. Self-test PASS ✅

---

## 8. Enforcement (per `rule-change-process.md` §6.5)

### 8.1 Reviewer-checklist

Pre-merge review cho PR touching acceptance test artifacts (`documents/05-guides/operations/acceptance-tests/**` hoặc tương đương):

- [ ] CSV có UTF-8 BOM (`head -c 3 file.csv | od -c` → `357 273 277`)?
- [ ] Column names tiếng Anh + narrative cells tiếng Việt per `dev-readable-doc-language.md`?
- [ ] Folder dedicated `acceptance-tests/` (KHÔNG ship vào `operations/` root)?
- [ ] Companion README `<stem>.md` exists explaining usage + scope coverage?
- [ ] Folder-level `README.md` exists với render script reference?
- [ ] `.gitignore` exclude `*.xlsx`?
- [ ] Render script `scripts/render-acceptance-test-xlsx.sh` referenced (project-wide canonical)?

### 8.2 Future enhancement: pre-commit hook (deferred ≥7 ngày per `incident-to-rule-pipeline.md` premature-rule guard)

`.husky/pre-commit` step check CSV trong `documents/05-guides/operations/acceptance-tests/` có BOM:

```bash
for f in $(git diff --cached --name-only --diff-filter=AM | grep '^documents/05-guides/operations/acceptance-tests/.*\.csv$'); do
  if ! head -c 3 "$f" | od -An -c | grep -q '357 273 277'; then
    echo "ERROR: $f missing UTF-8 BOM (per test-artifact-format-standard.md §3.1)" >&2
    exit 1
  fi
done
```

Track follow-up gap khi rule stabilize.

### 8.3 Override mechanism

Genuine exception (vd: matrix migrated từ external tool xuất XLSX gốc, không có CSV source):

```
git commit -m "...
TEST_ARTIFACT_FORMAT_OVERRIDE: <reason — explain why CSV-canonical không feasible>"
```

Trailer logged trong quarterly retro. Pattern frequency >5%/quarter triggers meta-review của decision matrix §2.

---

## 9. Relationship to other rules

- **`dev-readable-doc-language.md`** (sister rule, same PR) — governs column-name vs value-content language split per §3.2
- **`deployment-naming-convention.md`** — sister taxonomy for deploy/operations folders; this rule extends to per-release acceptance test placement
- **`docs-folder-structure.md`** — generic README rule; this rule specializes for acceptance test subfolders
- **`pre-handoff-self-test-completeness.md`** — verifies FLOW completeness; this rule formalizes the FORMAT for the verification artifact
- **`audit-to-gap-pipeline.md`** §3 — every `fail` row in matrix triggers gap filing per pipeline
- **`output-review-mandate.md`** §3 — adds row "Acceptance test CSVs" tracking review standard
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + §7 self-test + §8 reviewer-checklist + meta-CSV-index row all ship same PR
- **`incident-to-rule-pipeline.md`** — this rule = direct output of Wave 72a Bucket F user-flagged 4-issue retro applied through 5-stage pipeline
- **`meta-csv-index-pattern.md`** — pattern that proven gap-status.csv inspires this rule's CSV-canonical mandate cho test artifacts

---

## 10. Log

- **2026-05-14 (v1.0.0):** Rule created. Triggered by Wave 72a Bucket F user-flagged 4 issues on `phase-1-beta-acceptance-self-test.csv` (folder placement, old Plan 1 not archived, English content, no UTF-8 BOM). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged 4 specific issues in same matrix) → Classify ✓ (no existing rule covers test artifact format; `deployment-naming-convention.md` covers deploy/operations folders but not acceptance test specifically; `docs-folder-structure.md` generic) → Rule+Enforce ✓ (this file + sister rule `dev-readable-doc-language.md` + paired same-PR with: CSV BOM + Vietnamese translation of 126 rows + render script `scripts/render-acceptance-test-xlsx.sh` + folder-level README + `.gitignore` + dedicated subfolder relocation + Plan 1 archive + `output-review-mandate.md` §3 row + rules-index.csv 2 new rows per `rule-change-process.md` §6.5) → Self-Test ✓ (§7 worked example — 6/8 checks FAIL retroactively on Wave 72a state) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint codifying previously-implicit convention; no constraint loosening; existing acceptance test matrices grandfathered until next refresh; rule applies prospectively to new matrices). Pre-commit hook deferred per premature-rule guard ≥7 ngày.
