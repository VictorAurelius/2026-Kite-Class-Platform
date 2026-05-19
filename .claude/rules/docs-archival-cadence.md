---
paths:
  - "documents/**/*.md"
  - "documents/**/*.json"
---

# Docs Archival Cadence — auto-archive time-bound artifacts to prevent docs accumulation

**Priority:** 🟠 MANDATORY — docs lifecycle + volume governance
**Version:** 1.0.1
**Created:** 2026-05-18
**Last-Reviewed:** 2026-05-19
**Reviewer-Approver:** @nguyenvankiet (solo-dev — v1.0.1 PATCH self-approve per `rule-change-process.md` §5; Wave 99C META-META GAP-675 SHIP-NOW — `scripts/check-docs-archival-stale.sh` shipped + wired in `script-quality.yml` job `docs-scaling-detectors`; self-test PASS on 3 synthetic fixtures (audit + handoff stale, 1 fresh-exempt); WARN-mode initially; closes deferred-detector debt §4.3 within 1 day of rule landing per `incident-to-rule-pipeline.md` Stage 3 paired-enforcement. No constraint change — detector enforces existing §2 cadence prospectively. v1.0.0 (kept): MINOR self-approve per §5; new rule with built-in enforcement (reviewer-checklist + worked self-test on 2026-05-18 baseline) per §6.5)
**Applies to:** Time-bound artifacts trong `documents/04-quality/audits/**` + `documents/03-planning/session-handoffs/**` + `documents/03-planning/waves/**` + `documents/03-planning/pr-logs/**`. KHÔNG áp dụng cho: `ROADMAP.md`, gap files (`documents/04-quality/gaps/**` — đã có separate flow per `audit-to-gap-pipeline.md`), READMEs, canonical living docs (CLAUDE.md, business/*/rules.md).

---

## 1. The Rule

> **Time-bound artifacts có ngày sản xuất xác định (audit, session-handoff, wave plan, PR-log) PHẢI được archive khi vượt ngưỡng tuổi theo §2 Cadence table. Archive destination tuân theo §3 convention. Reviewer enforce per-merge; CI detector script `scripts/check-docs-archival-stale.sh` defer ≥7 ngày per `incident-to-rule-pipeline.md` premature-rule guard.**

Rationale: `documents/04-quality/` đang có 865 files (39% của 2,201 total documents trong project). Audit reports + PR-logs + session-handoffs là **time-bound** — giá trị cao trong ngắn hạn (≤90 ngày) cho debug + retro, value giảm exponentially sau đó. Khi không archive, cognitive load tăng + grep noise + git diff bloat + parent context burn (Claude session đọc nhầm stale audit thay vì latest).

Force-multiplier: 1 cadence chuẩn → mọi artifact subsequent auto-comply → eliminate retroactive cleanup cost mỗi quý.

---

## 2. Cadence table (mandatory)

| Artifact type | Folder pattern | Cadence (≥ N ngày tuổi) | Tuổi tính từ | Archive destination |
|---|---|---|---|---|
| **Audit reports** | `documents/04-quality/audits/**/*.md` có date-prefix `YYYY-MM-DD-*.md` HOẶC date trong filename | **90 ngày** | Date prefix trong filename | `documents/04-quality/audits/{category}/closed/{year}-Q{N}/` |
| **Session handoffs** | `documents/03-planning/session-handoffs/*.md` | **30 ngày** | Date prefix trong filename | `documents/07-archived/planning-{year}/session-handoffs/` |
| **Wave plans (closure status:complete)** | `documents/03-planning/waves/*.md` có frontmatter `status: complete` HOẶC `status: closed` | **60 ngày POST closure date** | `closed_at` field trong frontmatter HOẶC last git commit date sau status flip | `documents/07-archived/planning-{year}/waves/` |
| **PR-logs** | `documents/03-planning/pr-logs/PR-*.json` | **180 ngày** | Git log creation date (`git log --diff-filter=A --format='%ad' --date=short -- <file>`) | `documents/07-archived/pr-logs-{year}/` |

### 2.1 Tuổi tính như thế nào (precedence)

1. **Frontmatter date field** (preferred — `created`, `closed_at`, `archived_at`)
2. **Date prefix trong filename** (`YYYY-MM-DD-*.md`) — pattern phổ biến của audit + session-handoff
3. **Git log creation date** (`git log --diff-filter=A`) — fallback cho PR-logs JSON + waves không có frontmatter date

### 2.2 Cadence tuning rationale

| Cadence | Lý do |
|---|---|
| 30 ngày (session-handoffs) | Session context lifetime ngắn; sau 30 ngày, info đã chuyển vào ROADMAP / memory / wave plans canonical |
| 60 ngày POST closure (wave plans) | Wave plans cần reference window 60 ngày cho retro + post-wave audit suite refresh; sau 60 ngày retro xong, archive |
| 90 ngày (audit reports) | Audit reports value cao cho quarterly retro + cross-wave trend; 90 ngày = 1 quarter coverage |
| 180 ngày (PR-logs) | PR-logs value tra cứu lịch sử merge; 180 ngày = 2 quarter, đủ cho compliance audit + incident RCA |

Re-evaluate cadence khi: (a) Phase 1 BETA → Phase 2 transition (volume tăng 2-3x), (b) team grows beyond solo (cần reference window dài hơn), (c) compliance audit yêu cầu retention dài hơn (PDPL/ISO27001).

---

## 3. Archive destination convention

### 3.1 Naming pattern

```
documents/04-quality/audits/{category}/closed/{year}-Q{N}/   # cho audits
documents/07-archived/planning-{year}/session-handoffs/      # cho session-handoffs
documents/07-archived/planning-{year}/waves/                 # cho wave plans
documents/07-archived/pr-logs-{year}/                        # cho PR-logs JSON
```

### 3.2 Quarter mapping

| Quarter | Tháng |
|---|---|
| Q1 | January, February, March |
| Q2 | April, May, June |
| Q3 | July, August, September |
| Q4 | October, November, December |

Ví dụ: audit file dated `2026-02-15` archive khi ≥90 ngày (2026-05-15+) vào `documents/04-quality/audits/quality/closed/2026-Q1/`.

### 3.3 Folder creation

Khi archive folder chưa tồn tại → tạo cùng `README.md` 1-line:

```markdown
# {Year}-Q{N} {Type} Archive

Closed artifacts từ Q{N} {year} theo `.claude/rules/docs-archival-cadence.md` §3.

Total files: <auto-count>
Archive date: <YYYY-MM-DD>
```

### 3.4 Cross-references preservation

Khi archive 1 file:
- Update internal links: `documents/04-quality/audits/quality/audit.md` → `documents/04-quality/audits/quality/closed/2026-Q1/audit.md`
- Search outbound references: `grep -rl "<original-path>" documents/ .claude/ README.md ROADMAP.md`
- Update top 5 high-value references (audits-index.csv, ROADMAP, related rules); accept link rot cho low-value references (one-off mentions trong old gaps)

---

## 4. Enforcement

### 4.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching `documents/04-quality/audits/**`, `documents/03-planning/session-handoffs/**`, `documents/03-planning/waves/**`, `documents/03-planning/pr-logs/**`:

- [ ] PR thêm file mới — check folder có files quá tuổi theo §2 không (manual `find ... -mtime +N` hoặc filename date check)
- [ ] Nếu có ≥5 files quá tuổi → file follow-up gap GAP-XXX "Q{N} {year} archive batch" + schedule trong wave hiện tại
- [ ] PR archive-batch (move files vào `closed/` hoặc `07-archived/`) — verify destination tuân §3 + README created + cross-references updated cho top 5 high-value

### 4.2 Cadence audit (quarterly retro)

Mỗi quarterly retro PHẢI run heuristic check:

```bash
# Audits >90d
find documents/04-quality/audits -type f -name "*.md" | while read f; do
  filedate=$(basename "$f" | grep -oE '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
  [ -n "$filedate" ] && [[ "$filedate" < "$(date -d '90 days ago' +%Y-%m-%d)" ]] && echo "$f"
done | wc -l

# Session handoffs >30d
# Wave plans >60d post-closure (cần inspect frontmatter)
# PR-logs >180d (git log date)
```

Pattern stale count >20 files → file batch-archive gap trong wave kế tiếp.

### 4.3 CI detector script (SHIPPED Wave 99C GAP-675)

Script `scripts/check-docs-archival-stale.sh` shipped 2026-05-19 per Wave 99C META-META audit (GAP-675 SHIP-NOW verdict — trivial bash <130 LOC, low FP risk, complements §2 cadence). Wired vào CI job `docs-scaling-detectors` trong `script-quality.yml`:
- WARN-mode initially (existing files grandfathered; rule prospective)
- Self-test PASS 3 synthetic fixtures (100d audit stale + 40d handoff stale + 10d fresh exempt)
- Override trailer: `DOCS_ARCHIVAL_OVERRIDE: <path> — <reason>` per §5
- HARD STOP target Wave 100+ after 30-day grace period + first batch archive PR triages legacy violations

Re-enable HARD STOP follow-up tracked GAP-679 (Wave 99C META-META Steps 3-4).

### 4.4 Memory auto-load (optional, deferred)

Memory entry `feedback_docs_archival_cadence.md` có thể remind tại session start trước khi accumulating audit/session-handoff/wave. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test đủ cho v1.0.0.

---

## 5. Override mechanism

Genuine exception (artifact giá trị reference dài hạn vượt cadence — ví dụ landmark audit, milestone wave plan, compliance evidence):

```
git commit -m "...
DOCS_ARCHIVAL_OVERRIDE: <artifact path> — <reason — e.g. landmark Wave 40 milestone retain in-place cho cross-quarter trend reference; compliance evidence audit retain cho PDPL Art 11>"
```

Trailer logged trong quarterly retro. Pattern frequency >10% artifacts override mỗi quý → meta-review thresholds (có thể cadence quá ngắn).

Common valid override cases:
- Milestone audit (e.g. v1.0.0-rc landing audit) — retain in-place cho cross-quarter trend reference
- Compliance evidence (PDPL Art 11 audit log, ISO27001 audit trail) — retain theo retention policy >180 ngày
- Active reference (rule cross-link tới audit Wave-N — retain cho rule lifetime)

---

## 6. Worked self-test — retroactive cadence check 2026-05-18

Apply §2 cadence retroactively vào current repo state:

### 6.1 Baseline numbers

| Artifact type | Total files | Cadence threshold (today 2026-05-18) | Files quá tuổi | Sample (oldest) |
|---|---|---|---|---|
| Audit reports | 204 | 2026-02-17 (90 ngày) | **0** | `2026-02-27 architecture-gaps-analysis-2026-02-27.md` |
| Session handoffs | 11 | 2026-04-18 (30 ngày) | **0** | `2026-05-14-eod-session-handoff.md` |
| Wave plans | 107 (22 không có date filename) | 2026-03-19 (60 ngày) | **0 by filename heuristic** | `2026-04-28-gap-122-platform-alerts.md` |
| PR-logs | 116 | 2025-11-19 (180 ngày) | **0 by git log date** | `PR-295.json` git date 2026-04-17 |

### 6.2 Sample 5 oldest per loại (sẽ hit cadence sớm nhất)

**Audits (sẽ hit 90d threshold ≥2026-05-27, ~9 ngày nữa cho file 2026-02-27):**
1. `documents/04-quality/audits/architecture/architecture-gaps-analysis-2026-02-27.md` (2026-02-27, age 80d, threshold 2026-05-27)
2. `documents/04-quality/audits/quality/quality-audit-2026-03-22-kitehub.md` (2026-03-22, age 56d)
3. `documents/04-quality/audits/business/business-gap-check-2026-03-23-kiteclass.md` (2026-03-23, age 55d)
4. `documents/04-quality/audits/business/business-gap-check-2026-03-23-kitehub.md` (2026-03-23, age 55d)
5. `documents/04-quality/audits/quality/quality-audit-2026-03-23-kiteclass.md` (2026-03-23, age 55d)

**Session-handoffs (sẽ hit 30d threshold ≥2026-06-13, mọi files hiện ≤4 ngày old):**
1. `2026-05-14-eod-session-handoff.md` (age 4d)
2. `2026-05-14-post-wave-78-handoff.md` (age 4d)
3. `2026-05-14-session-handoff.md` (age 4d)
4. `2026-05-15-post-wave-79-handoff.md` (age 3d)
5. `2026-05-15-post-wave-80-handoff.md` (age 3d)

**Wave plans (sẽ hit 60d threshold POST closure ≥2026-06-27 cho closed waves; 22 files no-date-filename cần manual frontmatter inspection):**
1. `wave-2026-04-28-gap-122-platform-alerts.md` (filename 2026-04-28, age 20d)
2. `wave-2026-04-29-business-correctness.md` (age 19d)
3. `wave-2026-04-29-dr-backup.md` (age 19d)
4. `wave-2026-04-29-legal-brd-phase1-5.md` (age 19d)
5. `wave-2026-04-29-legal-brd-phase1.md` (age 19d)

**PR-logs (sẽ hit 180d threshold ≥2026-10-14 cho file git-date 2026-04-17):**
1. `PR-295.json` (git date 2026-04-17, age 31d)
2. `PR-296.json` (git date 2026-04-17, age 31d)
3. `PR-297.json` (git date 2026-04-17, age 31d)
4. `PR-298.json` (git date 2026-04-17, age 31d)
5. `PR-299.json` (git date 2026-04-17, age 31d)

### 6.3 Verdict

**Self-test PASS ✅** — rule fires correctly:
- Baseline = 0 files quá tuổi vì project bắt đầu active history 2026-02-27 (≤80 ngày)
- Rule applies prospectively — first batch archive sẽ trigger ~2026-05-27 (file `architecture-gaps-analysis-2026-02-27.md` hit 90d)
- **Counterfactual**: nếu rule không tồn tại, accumulation projected:
  - +Q3 2026 (90 ngày): ~70 audit files >90d (toàn bộ Q1-Q2 audits trừ landmark refs)
  - +Q3 2026: ~5 session-handoffs >30d
  - +Q4 2026: ~30 wave plans >60d post-closure
  - +Q3 2027: ~100 PR-logs >180d
- Tổng accumulation 12 tháng: ~200+ files giá trị low-reference chiếm cognitive load + grep noise

**Action signal:** prep first batch archive trong wave 2026-05-27 (Wave 95+) khi `architecture-gaps-analysis-2026-02-27.md` hit threshold.

---

## 7. Relationship to other rules

- **`docs-folder-structure.md`** §3 — folder layout convention cho `documents/`; rule này extend với lifecycle/cadence dimension
- **`planning-docs-structure.md`** — frontmatter + archival rules specific cho 03-planning; rule này provide chung cadence threshold; `planning-docs-structure.md` chi tiết frontmatter format
- **`audit-to-gap-pipeline.md`** — gap closure flow (gaps có separate archive pattern `documents/04-quality/gaps/closed/`); rule này NOT cover gaps (per scope clarification §Applies to)
- **`output-review-mandate.md`** §3 — sẽ thêm row "Docs archival cadence" sau merge atomic 4 rules (coordinator sync, NOT this PR)
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier; rule này fix accumulation 1 lần → force-multiplier mọi artifact subsequent
- **`context-budget-mandate.md`** §3.1 — `paths:` frontmatter scope rule deferred-load only khi `documents/**` trong context; tránh per-session token cost
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test ship same PR; detector script defer per premature-rule guard
- **`incident-to-rule-pipeline.md`** — rule này direct output user-flagged 2026-05-18 docs scaling miss; 5-stage applied (Detect ✓ Classify ✓ Rule+Enforce ✓ Self-Test ✓ Retro Log ✓)
- **`dev-readable-doc-language.md`** §2/§3 — rule body Vietnamese narrative + English identifier per convention
- **Rule 2/3/4 cùng pack (docs scaling)** — sister rules ship trong same wave (coordinator merge sequential): per-folder cap (Rule 2), gap auto-link rotation (Rule 3), audit lifecycle frontmatter (Rule 4). Cadence (rule này) là Rule 1/4 foundation.

---

## 8. Log

- **2026-05-19 (v1.0.1):** PATCH — Wave 99C META-META GAP-675 SHIP-NOW closure of deferred-detector debt §4.3. `scripts/check-docs-archival-stale.sh` shipped (124 LOC bash; 3-fixture self-test PASS); wired CI job `docs-scaling-detectors` in `script-quality.yml`. WARN-mode initially per `incident-to-rule-pipeline.md` premature-rule guard tightened §3 conditions (Stage 3 paired-enforcement compliance achieved within 1 day of rule landing — cycle time benchmark). No constraint change; detector enforces existing §2 cadence prospectively. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — additive enforcement, no constraint loosening; HARD STOP follow-up GAP-679 tracking).
- **2026-05-18 (v1.0.0):** Rule created in response to user-flagged 2026-05-18 docs scaling miss — `documents/04-quality/` accumulating 865 files (39% của 2,201 total); audit + PR-logs + session-handoffs accumulating without cadence guard. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged "documents/04-quality has 865 files, cognitive load too high") → Classify ✓ (existing rules cover format/structure `docs-folder-structure.md` + planning-specific archival `planning-docs-structure.md` NHƯNG KHÔNG cover generic cadence cho time-bound artifacts cross-folder; `audit-to-gap-pipeline.md` covers gap closure flow only) → Rule+Enforce ✓ (this file + reviewer-checklist §4.1 + worked self-test §6 + paired same-PR sister rules Rule 2/3/4 docs scaling pack — coordinator sync atomic post-merge cho `rules-index.csv` + `output-review-mandate.md` per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên 2026-05-18 baseline — 4 artifact types × cadence table = 0 quá tuổi today (project mới, active history start 2026-02-27); rule applies prospectively từ ~2026-05-27 first batch trigger; counterfactual: 12-month accumulation projected ~200+ low-value files) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix cadence 1 lần → force-multiplier mọi artifact subsequent (Wave 92+ audit + session-handoff + wave + PR-log). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered lifecycle governance for time-bound artifacts; no constraint loosening; existing artifacts grandfathered (baseline 0 quá tuổi today); rule applies prospectively từ first cadence trigger 2026-05-27 forward). Detector script `scripts/check-docs-archival-stale.sh` deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; reviewer-checklist §4.1 + quarterly retro check §4.2 + worked self-test §6 đủ cho v1.0.0. Path-scoped per `context-budget-mandate.md` §3.1 — `paths: ["documents/**/*.md", "documents/**/*.json"]` deferred-load chỉ khi `documents/**` trong context.
