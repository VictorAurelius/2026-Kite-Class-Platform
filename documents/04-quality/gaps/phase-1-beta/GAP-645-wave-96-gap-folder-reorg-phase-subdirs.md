# GAP-645: Wave 96 gap folder reorg per user inside-out proposal — phase subdirs + creation-time enforcement

**Status:** 🟡 PARTIAL (~30% — Wave 95 PR1.5 v2.0.0 ship + outside-in audit DONE; Buckets B/C/D queued PR2/PR3 under revised design)
**Priority:** 🟠 P1 (META force-multiplier per `meta-gap-priority.md` §3 — addresses active Rule 3 cap violation)
**Domain:** Meta
**Detected:** 2026-05-18 (user inside-out 2026-05-18 — reorganize gaps folder per phase + status + creation-time enforcement)
**Related Audits:** N/A (meta scope — process governance)

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Active gap files | `documents/04-quality/gaps/GAP-*.md` (root) | 🔴 364 files (182% Rule 3 cap 200) |
| DONE archive | `documents/04-quality/gaps/closed/` | ✅ 285 files (89 with CSV row + 196 orphan) |
| Phase subdirs | `documents/04-quality/gaps/phase-{1-beta,1.5-paid,2,3}/` | ❌ Not created |
| PARTIAL flag subdir | `documents/04-quality/gaps/partial/` | ❌ Not created |
| Creation-time enforcement | None | ❌ No rule mandate at gap file creation |

**Grep evidence:**
```bash
ls documents/04-quality/gaps/GAP-*.md 2>/dev/null | wc -l
# 338 (root-level active gap files)
awk -F',' '$4 ~ /^(OPEN|PARTIAL|IN_PROGRESS|PENDING|PLANNED)$/' documents/04-quality/gaps/gap-status.csv | wc -l
# 364 (CSV active rows — exceeds Rule 3 cap 200 = 182%)
```

## Problem

Per `docs-folder-volume-budget.md` Rule 3 §2 — **gap files active cap = 200 rows**. Hiện tại 364 active (182% over cap). User inside-out proposal 2026-05-18:

> 1. Các gaps nào đáng nhẽ phải closed thì closed luôn → move vào folder `closed/`
> 2. Các gaps defer sang phase 2 → move vào subfolder `phase-2/`
> 3. Các gaps nào đang dở → move vào subfolder `partial/` + thêm meta check ưu tiên
> 4. Các gaps còn open cho phase 1 → move vào subfolder `phase-1-beta/`
> 5. Root gaps/ chỉ có gaps không xác định
> 6. Khi tạo gaps hoặc cập nhật status → phải phân loại đúng (enforcement)

Proposal aligns với Rule 3 §4 trigger flow (semantic sub-split). ALSO addresses missed enforcement gap — no rule mandates classification at gap creation/update time.

## Context

User inside-out proposal trong cùng session 2026-05-18 với Wave 94c (Wave 92 audit suite). Wave 93 đã ship 4 new rules (docs-archival-cadence + docs-subfolder-maturity + docs-folder-volume-budget + docs-filename-prefix-convention) governing docs growth — proposal extends to gap-specific folder governance.

## Concerns + outside-in audit trigger

Per `gap-architecture-v2.md` §3 — CSV `phase` column canonical, file path advisory cache. Moving phase classification ALSO to file path → potential drift risk (file in `phase-2/` but CSV says `phase-1-beta` → which wins?).

Per `outside-in-coverage-trigger.md` v1.1.0 §2 row "Architecture-decision keywords trong gap filing" — proposal has architecture-decision scope ("subfolder organization" + "meta enforcement at creation time") → MUST fire outside-in audit BEFORE lock scope.

## Proposed Fix (Wave 96 scope — defer execution post Phase 1 BETA close)

### Bucket A — Outside-in audit (3 agents parallel, ~30 min)

Per `outside-in-coverage-trigger.md` v1.1.0:
- Persona audit — dev workflow walkthrough (creating new gap / updating status / closing gap)
- External benchmark — how other SaaS projects organize gap-tracking taxonomy
- Failure-mode matrix — drift scenarios + CI validator coverage gaps

### Bucket B — Migrate 364 active gaps into 6 phase subdirs (~2h)

```
documents/04-quality/gaps/
├── README.md                  # Index updated
├── _TEMPLATE.md               # Template (kept at root)
├── _REVIEW-TEMPLATE.md        # Review template (kept at root)
├── gap-status.csv             # Canonical CSV (root)
├── closed/                    # DONE archive (existing, 285 files)
├── pending/                   # PENDING status (existing, ~29 files)
├── phase-1-beta/              # ~223 phase-1-beta gaps active
├── phase-1.5-paid/            # 36 phase-1.5-paid gaps active
├── phase-2/                   # 78 phase-2 gaps active
├── phase-3/                   # 70 phase-3 gaps active
├── partial/                   # PARTIAL/IN_PROGRESS flag (cross-cuts phase)
└── n/a/                       # 30 meta gaps no phase classification
```

Each subdir created với README.md per `docs-folder-structure.md` §3 template. Volume criterion ≥5 files per subdir thỏa Rule 2 §2.

### Bucket C — Update CSV `filename` column 364 rows (~30 min batch script)

`scripts/migrate-gaps-to-phase-subdirs.py` — read CSV phase column → write filename column với new subdir prefix.

### Bucket D — Update cross-references (~1h grep + Edit)

Internal links `GAP-NNN-...md` trong other docs (rules, skills, ROADMAP) auto-resolve nếu use bare filename (no subdir prefix). NHƯNG explicit path references cần update — `grep -rl "documents/04-quality/gaps/GAP-"` to identify.

### Bucket E — New rule `gap-phase-classification-enforcement.md` v1.0.0 (~1h)

Mandate: every new gap file MUST be placed in correct phase subdir matching CSV `phase` column at creation. CI validator `scripts/check-gap-phase-classification.sh` — verify file path matches CSV phase row.

### Bucket F — Outside-in audit findings consolidation + new gap filings if findings (~30 min)

## Acceptance Criteria

- [ ] Outside-in audit 3 agents shipped (persona + benchmark + failure-mode)
- [ ] 364 active gaps migrated to 6 phase subdirs (CSV filename column synced)
- [ ] 6 subdir READMEs created với 4 sections per docs-folder-structure.md §3
- [ ] Root `documents/04-quality/gaps/` under Rule 3 200-row cap post-migration
- [ ] Each phase subdir under Rule 3 200-row cap (largest = phase-1-beta ~223 — exceeds; may need further split)
- [ ] CSV `filename` column 100% sync với actual file location (per `gap-architecture-v2.md` §3)
- [ ] New rule `gap-phase-classification-enforcement.md` v1.0.0 shipped với CI validator
- [ ] Cross-references updated trong rules + skills + ROADMAP
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md` §2.x

## Defer condition

**Execute Wave 96** only after:
1. Phase 1 BETA gate close (GAP-619 ✅ + GAP-612 AWS restore + score ≥80)
2. Wave 94c findings (GAP-637 P0 admin auth) addressed
3. User explicit confirm scope lock post outside-in audit findings

## Related

- **User proposal:** 2026-05-18 inside-out
- **Parent rule:** `docs-folder-volume-budget.md` Rule 3 (200 active gap cap)
- **Sister rules:** `docs-subfolder-maturity.md` Rule 2 + `docs-archival-cadence.md` Rule 1 + `docs-filename-prefix-convention.md` Rule 4
- **Conflict consideration:** `gap-architecture-v2.md` §3 CSV canonical authority
- **Outside-in trigger:** `outside-in-coverage-trigger.md` v1.1.0 §2 architecture-decision keywords
- **Wave 96 plan stub:** `documents/03-planning/waves/wave-2026-05-18-96-gap-folder-reorg-stub.md`

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (Wave 95 gap-folder reorg v2.0.0; meta/process).
- **2026-05-18 (Wave 95 PR1.5 — design pivot post outside-in audit ~30%):** User post-merge inspection of PR #1532 v1.0.0 triggered correct outside-in audit (recurrence #3 of pattern per memory `feedback_outside_in_recurring_miss.md` — Claude skipped outside-in in PR1 per §4 internal-refactor exception; user caught design concern post-ship). 3 parallel agents (Persona simulation + External benchmark + Failure-mode matrix) all independently concluded v1.0.0 status-driven design wrong: Agent 1 ❌ REVERT (~1,200 moves over 6 months vs 0 in status-stable, fights Git natural model); Agent 2 ❌ REVERT (9/9 industry tools + ADR precedent immutable filenames); Agent 3 ⚠️ RISKY (Class 4 tooling blockers in `.claude/skills/**` globs). **User direction "bỏ việc move file vào closed và partial đi, chỉ phân loại theo phase"** → clarification dialogue extended với per-phase closed/ archive (volume cap satisfied: largest phase-1-beta 151 active + 81 closed both <200). **PR1.5 v2.0.0 forward-fix shipped:** rule MAJOR rewrite v1.0.0 → v2.0.0 phase-only design (5 phase subdirs + per-phase closed/ one-way archive). Cost reduction: ~1,200 moves → ~120-150 (24x). Deleted partial/+wontfix/ subdirs (created PR1, never populated). Added 5 phase-X/closed/ scaffolding READMEs. Rewrote phase-X/ + unclassified/ + closed/ (root LEGACY 196 orphans) READMEs. CI script rewrite under v2.0.0 logic + legacy-orphan tolerance. output-review-mandate §3 row v1.10.0 → v1.11.0. **Bucket A outside-in audit DONE** (was SKIPPED in PR1). Buckets B/C/D queued PR2/PR3 under revised v2.0.0: PR2 mass migration of 466 files into phase-X/[closed/] + 90 CSV-tracked DONE migration root closed/ → phase-X/closed/ + 29 PENDING migration pending/ → phase-X/ matching CSV phase; PR3 cross-link sweep (Agent 3 noted only ~6 explicit path citations in rules — low cross-link density). Updated CSV row: completion_pct 0→30, status PARTIAL, notes reflect v2.0.0 design.
- **2026-05-18 (Wave 95 PR1 — Bucket E v1.0.0 status-driven, DEPRECATED ~25%):** Scope pulled forward Wave 96 → Wave 95 per user direction same session. PR1 (#1532) shipped: rule v1.0.0 (status-driven 9-subdir taxonomy) + CI script `--warn` + scaffolding (8 subdir READMEs: partial/, wontfix/, phase-1-beta/, phase-1.5-paid/, phase-2/, phase-3/, unclassified/, closed/) + CI workflow wire + rules-index.csv row + output-review-mandate §3 row v1.10.0 + GAP-645 status flip. Bucket A outside-in audit SKIPPED per user direction (`outside-in-coverage-trigger.md` §4 internal-refactor exception). v1.0.0 reverted by PR1.5 v2.0.0 within hours after user post-merge inspection triggered correct outside-in audit (see entry above). v1.0.0 scaffolding partially recoverable: partial/+wontfix/ deleted (never populated), phase-X/+unclassified/+closed/ retained + extended.
- **2026-05-18 (filed):** Filed from user inside-out proposal 2026-05-18 same session as Wave 94c audit suite consolidation. Proposal addresses real Rule 3 violation (364 active vs 200 cap = 182%). Outside-in audit trigger fired per `outside-in-coverage-trigger.md` v1.1.0 §2 architecture-decision keywords. Wave 96 execution deferred post Phase 1 BETA gate close + GAP-637 P0 admin auth fix. Scope ~5h (6 buckets).
