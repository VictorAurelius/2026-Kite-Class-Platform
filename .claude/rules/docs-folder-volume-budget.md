---
paths:
  - "documents/**"
---

# Docs Folder Volume Budget — per-folder active file caps

**Priority:** 🟠 MANDATORY — docs scaling governance
**Version:** 1.0.1
**Created:** 2026-05-18
**Last-Reviewed:** 2026-05-19
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.0.1 PATCH self-approve per `rule-change-process.md` §5; Wave 99C META-META GAP-675 SHIP-NOW — `scripts/check-docs-folder-volume.sh` shipped (138 LOC bash) + wired CI job `docs-scaling-detectors`; self-test PASS (60 over-cap + 30 safe counter logic); real-repo scan surfaces 2 confirmed over-cap (pr-logs 148/50 + gap-active 385/200) — actionable; closes deferred-detector debt §6.3 within 1 day of rule landing per `incident-to-rule-pipeline.md` Stage 3 paired-enforcement. No constraint change — detector enforces existing §2 caps prospectively. v1.0.0 (kept): MINOR self-approve per §5; new rule with built-in enforcement (reviewer-checklist + worked self-test trên hiện trạng `documents/03-planning/` + `documents/04-quality/`) per §6.5)
**Applies to:** Mọi folder dưới `documents/**` chứa artifact markdown / JSON; mọi PR thêm file mới vào folder đã > cap; mọi reviewer kiểm tra folder hygiene

---

## 1. The Rule

> **Mỗi folder dưới `documents/**` PHẢI tôn trọng active file cap tương ứng với folder class (§2). Khi count vượt cap → mandatory archive trigger HOẶC sub-split theo semantic dimension (year/quarter/category), KHÔNG được phép thêm file mới trước khi xử lý.**

Folder volume cap là **early-warning signal** chống lại tình trạng folder un-browsable. User-flagged 2026-05-18: `documents/04-quality` 865 files + `documents/03-planning` 331 files → impossible to browse, ngay cả Claude cũng struggle khi locate specific artifact. Rule này codify ngưỡng cứng để force-multiplier mọi PR subsequent: thêm 1 file vào folder > cap → block + đề xuất archive/split TRƯỚC khi merge.

Force-multiplier rationale per `meta-gap-priority.md`: rule này META P1 — fix 1 ngưỡng → mọi folder under `documents/**` auto-comply prospectively → eliminate retroactive "tìm file mất 5 phút" cost mỗi session.

---

## 2. Threshold per folder class

| Folder class | Cap | Action when exceeded |
|---|---|---|
| **Time-bound artifact** (`audits/*/`, `session-handoffs/`, `pr-logs/`, `waves/`, gap files theo date) | **50 active files per leaf folder** | Mandatory archive trigger (per `docs-archival-cadence.md` Rule 1) HOẶC sub-split by year/quarter (e.g., `audits/aws-verification/2026-Q2/`) |
| **Static doc** (`rules/`, `skills/`, `runbooks/`, `adr/`, `deploy/`, `operations/`) | **100 files per leaf folder** | Mandatory consolidate (merge similar rules — per `rule-change-process.md` §5.1 atomic-unique-bar) HOẶC sub-split by category |
| **Gap files** (`documents/04-quality/gaps/*.md` active) | **200 active rows** (status OPEN + PARTIAL + IN_PROGRESS + PENDING + PLANNED trong `gap-status.csv`) | Mandatory triage: DONE → `closed/` per `gap-architecture-v2.md` §3; WONTFIX → archive; file follow-up gap để address backlog |
| **Reference / archived** (`*/archived/`, `*/closed/`, `07-archived/**`) | **NO cap** | Archived = read-only history; volume OK. Read-time cost amortized vì rarely accessed. |

### 2.1 Folder class identification

| Indicator | Class assignment |
|---|---|
| Files chứa date-prefix `YYYY-MM-DD-*.md` hoặc tên file embed wave/release tag (audits, session, wave plans) | Time-bound artifact |
| Files không embed date, tồn tại long-lived (rules, skills, ADRs, runbooks) | Static doc |
| Folder dưới `documents/04-quality/gaps/` (excl `closed/` subdir) | Gap files (status driven by CSV) |
| Folder name match `archived` / `closed` / `07-archived` | Reference / archived (no cap) |

Edge case: nếu folder mix 2 classes (rare) → áp class strictest (smaller cap). Reviewer judgment per §6.

---

## 3. Active file definition

"Active" file = file đáp ứng TẤT CẢ các tiêu chí:

1. **Status không phải DONE/closed/archived**:
   - Gap files: `gap-status.csv` status ∈ {OPEN, PARTIAL, IN_PROGRESS, PENDING, PLANNED}
   - Audit/session-handoff: file chưa bị move xuống `closed/` hoặc `archived/` subdir
2. **Không phải infrastructure file**:
   - `README.md` (folder index) — KHÔNG counted
   - `_TEMPLATE.md`, `_REVIEW-TEMPLATE.md`, `_*` (underscore-prefix internals) — KHÔNG counted
   - `.gitkeep` / hidden files — KHÔNG counted
3. **Là markdown / JSON artifact**: `*.md` hoặc `*.json` (pr-logs scope). Other extensions ngoài scope.

### 3.1 Date-prefix artifact special case

File `YYYY-MM-DD-*.md` (audit, session-handoff) counted ACTIVE cho đến khi:
- Moved sang `archived/YYYY-QN/` subdir per `docs-archival-cadence.md` cadence rule, HOẶC
- Moved sang `closed/` subdir nếu artifact represents closed scope

Không tự động archive theo age — phải qua `docs-archival-cadence.md` mandate.

### 3.2 Count command reference (for self-test + monitoring)

```bash
# Time-bound: audits subdir count
find documents/04-quality/audits/<subdir> -maxdepth 1 -type f -name "*.md" \
  -not -name "README*" -not -name "_*" 2>/dev/null | wc -l

# Time-bound: pr-logs
ls documents/03-planning/pr-logs/PR-*.json 2>/dev/null | wc -l

# Time-bound: session-handoffs
find documents/03-planning/session-handoffs -maxdepth 1 -type f -name "*.md" \
  -not -name "README*" -not -name "_*" 2>/dev/null | wc -l

# Static doc: rules
find .claude/rules -maxdepth 1 -type f -name "*.md" \
  -not -name "README*" -not -name "_*" 2>/dev/null | wc -l

# Gap files active (canonical via CSV per gap-architecture-v2.md)
awk -F',' 'NR>1 && $4 != "DONE" && $4 != "WONTFIX" && $4 != "" && $4 != "status"' \
  documents/04-quality/gaps/gap-status.csv | wc -l
```

---

## 4. Trigger flow

Khi count vượt cap (reviewer phát hiện trong PR review HOẶC self-check qua §3.2 commands):

```
Step 1: Identify folder class (§2.1)
        ↓
Step 2: Check whether archival cadence rule covers this class
        - If time-bound + age threshold met per `docs-archival-cadence.md` → RUN archive script
        - If gap files → run triage per `gap-architecture-v2.md` §3 (DONE → closed/)
        ↓
Step 3: If STILL > cap after archive:
        - Sub-split by SEMANTIC dimension (year/quarter/category/persona/wave-range)
        - DO NOT artificially split (e.g., "audits-a-m/" vs "audits-n-z/") — must reflect natural grouping
        ↓
Step 4: If neither archive nor split applies (e.g., static rules > 100):
        - Consolidate similar files per atomic-unique-bar (`rule-change-process.md` §5.1)
        - HOẶC file gap để address backlog systematically
        ↓
Step 5: ONLY THEN add the new file. Otherwise BLOCK pre-merge.
```

### 4.1 Mandatory pre-add check (reviewer-checklist active now)

Trước khi merge PR thêm artifact vào `documents/**`:

- [ ] Run count command từ §3.2 cho target folder
- [ ] Nếu count + 1 (file mới) > cap → §4 trigger flow MUST run trong cùng PR HOẶC follow-up gap filed

### 4.2 Sub-split convention examples

| Class | Pattern | Example |
|---|---|---|
| Time-bound audit | `<category>/YYYY-Qn/` | `audits/aws-verification/2026-Q2/2026-05-15-...md` |
| Wave plans | `waves/<wave-range>/` | `waves/wave-80-90/wave-85-plan.md` (nếu top-level > 50) |
| Static rules | `rules/<topic-cluster>/` | `rules/governance/`, `rules/deploy/` (nếu rules/ > 100) |
| Gap files | per `gap-architecture-v2.md` §3 | `gaps/closed/` for DONE; root for active |

Split MUST happen trong cùng PR (atomic refactor) hoặc qua dedicated follow-up gap; KHÔNG được phép half-split (partial subdirs).

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Thêm file vào folder đã > cap mà không address | Chạy archive script trước, sau đó thêm |
| Sub-split single subdir "to game the cap" (e.g., `audits-batch-1/` `audits-batch-2/`) | Split MUST semantic (year/quarter/category), không artificial |
| Set folder-specific cap > 200 để "avoid the problem" | Threshold = signal, không phải target để game |
| Move file vào `archived/` chỉ để giảm count (file chưa thực sự closed) | Archive chỉ khi artifact đã closed scope per `docs-archival-cadence.md` cadence |
| Treat README.md / _TEMPLATE.md as counted | Per §3 — infrastructure files KHÔNG counted |
| Skip count check "vì PR chỉ thêm 1 file" | Recursion: mỗi PR thêm 1 = compound; check mỗi lần |
| Bury folder volume issue trong gap backlog | Address trong PR thêm file đó, không defer |
| Consolidate rules bằng cách delete nội dung quan trọng | Atomic-unique bar applies — chỉ consolidate khi overlap thực sự |

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 Reviewer-checklist (active now — primary enforcement)

Khi review PR thêm file vào `documents/**`:

- [ ] Identify target folder class per §2.1
- [ ] Run count command từ §3.2
- [ ] If `count + (files added in PR) > cap` → §4 trigger flow executed in PR OR follow-up gap referenced
- [ ] If sub-split happens → verify split is semantic, not artificial

### 6.2 Override mechanism

Genuine exception (hotfix incident artifact must land immediately, no time for archive cycle):

```
git commit -m "...
DOCS_VOLUME_OVERRIDE: <folder-path> <count>/<cap> — <reason>
DOCS_VOLUME_FOLLOWUP: <link to gap scheduling archive/split within Ndays>"
```

Trailer logged trong quarterly retro. Pattern frequency > 5%/quarter triggers meta-review of caps.

### 6.3 Monitoring script (SHIPPED Wave 99C GAP-675)

Script `scripts/check-docs-folder-volume.sh` shipped 2026-05-19 per Wave 99C META-META audit (GAP-675 SHIP-NOW verdict — trivial bash ~138 LOC, hardcoded folder→cap table, low FP risk). Wired CI job `docs-scaling-detectors` trong `script-quality.yml`:
- WARN-mode initially (current 2 over-cap folders surface as actionable WARN, no merge block)
- Self-test PASS (counter logic verified: 60 over-cap + 30 safe)
- Real-repo scan surfaces: `documents/03-planning/pr-logs` 148/50 (296%) + gap-active 385/200 (192%)
- Override trailer: `DOCS_VOLUME_OVERRIDE: <folder> <count>/<cap> — <reason>`
- HARD STOP target Wave 100+ after triage PRs reduce known over-cap folders

Re-enable HARD STOP follow-up tracked GAP-679.

### 6.4 Memory auto-load (deferred)

Memory entry `feedback_docs_folder_volume_budget.md` có thể remind tại session start trước khi thêm artifact vào `documents/**`. Defer per premature-rule guard ≥ 7 ngày; reviewer-checklist + worked self-test §8 đủ cho v1.0.0.

---

## 7. Override mechanism (extended cases)

| Case | Required trailer | Follow-up |
|---|---|---|
| Hotfix incident artifact must land immediately | `DOCS_VOLUME_OVERRIDE: <path> + reason` | File gap to archive within 7 days |
| Bulk migration PR (e.g., gap CSV initial seed) | `DOCS_VOLUME_BULK: <path> + scope` | One-time; trigger sub-split in same wave |
| Folder class genuinely ambiguous (mixed time-bound + static) | Reviewer + author manual judgment, log in PR body | Re-classify trong next refresh |
| Cap genuinely too low for legitimate use case | File meta-gap to revise §2 caps | Rule change per `rule-change-process.md` |

Banned override: "we'll address later" without follow-up gap link.

---

## 8. Worked self-test — hiện trạng repo (2026-05-18)

Apply rule §3.2 count commands lên repo hiện tại, identify folders exceed cap:

### 8.1 Time-bound artifact (cap 50)

| Folder | Count | Status | Action required |
|---|---|---|---|
| `documents/04-quality/audits/aws-verification/` | 40 | 🟡 80% cap — WARN | Plan archive 2026-Q2 trong next wave |
| `documents/04-quality/audits/quality/` | 25 | ✅ 50% — OK | None |
| `documents/04-quality/audits/security/` | 19 | ✅ OK | None |
| `documents/04-quality/audits/persona-review/` | 16 | ✅ OK | None |
| `documents/04-quality/audits/waves/` | 11 | ✅ OK | None |
| `documents/04-quality/audits/ui/` | 10 | ✅ OK | None |
| `documents/04-quality/audits/performance/` | 10 | ✅ OK | None |
| Other audit subdirs (< 10 each) | ≤ 9 | ✅ OK | None |
| `documents/03-planning/session-handoffs/` | 10 | ✅ OK | None |
| `documents/03-planning/pr-logs/` (PR-*.json) | 116 | 🔴 232% cap — FAIL | Mandatory archive — split by quarter HOẶC move closed-PR logs sang `archived/` |
| `documents/03-planning/waves/` | 106 | 🔴 212% cap — FAIL | Mandatory split by wave-range subdir (`wave-1-30/`, `wave-31-60/`, `wave-61-90/`, `wave-91+/`) |
| `documents/03-planning/implementation/` | 20 | ✅ OK | None |
| `documents/03-planning/quality/` | 19 | ✅ OK | None |
| `documents/03-planning/testing/` | 11 | ✅ OK | None |
| `documents/03-planning/roadmap/` | 11 | ✅ OK | None |

### 8.2 Static doc (cap 100)

| Folder | Count | Status | Action required |
|---|---|---|---|
| `.claude/rules/` | 64 | ✅ 64% — OK | None (under cap; closer to threshold means watch) |
| `.claude/skills/` SKILL.md recursive | 36 | ✅ OK | None |
| `documents/05-guides/operations/` | 27 | ✅ OK | None |
| `documents/05-guides/deploy/` | 29 | ✅ OK | None |
| `documents/02-architecture/adr/` | 31 | ✅ OK | None |

### 8.3 Gap files (cap 200 active)

| Folder | Count | Status | Action required |
|---|---|---|---|
| `gap-status.csv` active rows (OPEN+PARTIAL+IN_PROGRESS+PENDING+PLANNED) | 351 | 🔴 175% cap — FAIL | Mandatory triage: 89 DONE rows → move file gaps sang `closed/`; review 118 PARTIAL — close if AC met or split |
| `documents/04-quality/gaps/` active *.md files (filesystem) | 408 | 🔴 204% cap — FAIL | Mirror CSV triage — orphan files not in CSV → file follow-up |
| `documents/04-quality/gaps/closed/` | 200 | N/A (archived, no cap) | OK |

### 8.4 Aggregate verdict

**3 folder classes vượt cap ngay tại merge time của rule này:**

1. 🔴 `documents/03-planning/pr-logs/` — 116 files (232%) → Rule 1 `docs-archival-cadence.md` (paired wave) sẽ enforce age-based archive; expected post-archive count ≤ 50
2. 🔴 `documents/03-planning/waves/` — 106 files (212%) → Sub-split bằng wave-range subdir trong follow-up gap
3. 🔴 Gap files active 351 (175%) → Rule 2 `gap-architecture-v2.md` v1.0.4 đã ship; triage backlog tracked qua existing wave plan

**Worked self-test verdict:** Rule fires correctly on 3 confirmed over-cap classes. Rule applies prospectively — these 3 don't block existing artifacts but trigger §4 flow trong next wave touching the folders. Self-test PASS ✅

**Counterfactual without rule:** không có ngưỡng cứng → folders tiếp tục grow vô hạn → 2026-Q3 dự kiến `pr-logs/` > 200, `waves/` > 150, gap active > 500 → search time exponential. Rule + paired Rule 1 cadence + Rule 2 gap architecture = compound force-multiplier.

---

## 9. Relationship to other rules

- **`docs-archival-cadence.md`** (Rule 1, paired same-wave) — provides age-based archive cadence; rule này provides volume-based trigger. Time-bound folders gặp cap → archive cadence is the natural response.
- **`gap-architecture-v2.md`** v1.0.4 (Rule 2, đã ship) — defines gap canonical via CSV; rule này references for gap class cap (200 active) + triage path DONE → `closed/`.
- **`rule-change-process.md`** §5.1 atomic-unique bar — static doc folders > 100 trigger consolidate; consolidate respects atomic-unique bar.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test all paired same PR.
- **`docs-folder-structure.md`** — generic README/structure rule; rule này extends với volume cap layer.
- **`incident-to-rule-pipeline.md`** — rule này direct output của user-flagged 2026-05-18 miss "865 files không thể browse" applied through 5-stage pipeline.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier; fix 1 ngưỡng → mọi PR subsequent auto-comply.
- **`output-review-mandate.md`** §3 — matrix row "Docs folder volume" addition deferred per scope constraint (Rule 4 of pack covers cross-cutting matrix updates).
- **`context-budget-mandate.md`** §3 — rule này path-scoped (`documents/**`) per frontmatter; KHÔNG always-load; deferred-load chỉ khi PR touch documents/.

---

## 10. Log

- **2026-05-19 (v1.0.1):** PATCH — Wave 99C META-META GAP-675 SHIP-NOW closure of deferred-detector debt §6.3. `scripts/check-docs-folder-volume.sh` shipped (138 LOC bash; self-test 60/30 counter logic PASS); wired CI job `docs-scaling-detectors`. WARN-mode initially per `incident-to-rule-pipeline.md` premature-rule guard tightened §3 conditions (Stage 3 paired-enforcement compliance). Real-repo scan surfaces 2 actionable over-cap folders (pr-logs 296% + gap-active 192%) — triage follow-up tracked. No constraint change; detector enforces existing §2 caps prospectively. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — additive enforcement, no constraint loosening).
- **2026-05-18 (v1.0.0):** Rule created in response to user-flagged 2026-05-18 miss: `documents/04-quality` 865 files + `documents/03-planning` 331 files → folder volume scale impossible to browse, ngay cả Claude struggle khi locate specific artifact. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged volume miss) → Classify ✓ (no existing rule mandates per-folder volume cap; `gap-architecture-v2.md` covers gap-specific only; `docs-folder-structure.md` covers structure not volume; `docs-archival-cadence.md` Rule 1 paired addresses age-based archive but no volume cap) → Rule+Enforce ✓ (this file + reviewer-checklist + paired same-wave với Rule 1 `docs-archival-cadence.md` + Rule 2 `gap-architecture-v2.md` đã ship + Rule 4 cross-cutting matrix per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§8 worked example trên hiện trạng repo — 3 folder classes confirmed over-cap: pr-logs 116/50, waves 106/50, gap active 351/200; rule fires correctly + counterfactual demonstrates force-multiplier value) → Retro Log ✓ (this entry). Rule 3 of 4 trong docs scaling pack (Rule 1 `docs-archival-cadence.md` paired wave parallel; Rule 2 `gap-architecture-v2.md` v1.0.4 đã ship; Rule 4 pending). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix ngưỡng 1 lần → mọi folder under `documents/**` auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint adding previously-uncovered per-folder volume cap; no constraint loosening for prior artifacts; existing over-cap folders grandfathered until next wave touching them; rule applies prospectively từ this PR forward). Monitoring script (§6.3) + memory auto-load (§6.4) deferred ≥ 7 days per premature-rule guard; reviewer-checklist + count commands inline §3.2 + worked self-test §8 đủ cho v1.0.0. Path-scoped (`documents/**`) per `context-budget-mandate.md` §3 — deferred-load only khi PR touch `documents/`.
