# GAP-645: Wave 96 gap folder reorg per user inside-out proposal — phase subdirs + creation-time enforcement

**Status:** 🔵 OPEN
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

- **2026-05-18 (filed):** Filed from user inside-out proposal 2026-05-18 same session as Wave 94c audit suite consolidation. Proposal addresses real Rule 3 violation (364 active vs 200 cap = 182%). Outside-in audit trigger fired per `outside-in-coverage-trigger.md` v1.1.0 §2 architecture-decision keywords. Wave 96 execution deferred post Phase 1 BETA gate close + GAP-637 P0 admin auth fix. Scope ~5h (6 buckets).
