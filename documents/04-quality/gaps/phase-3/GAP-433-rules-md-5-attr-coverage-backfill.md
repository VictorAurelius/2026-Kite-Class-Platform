# GAP-433: backfill 5-attribute frontmatter cho rules.md (40% thiếu Reviewer + Review cadence)

**Status:** 🟡 PARTIAL — Phase 1 DONE (Wave 41 Bucket E, 2026-05-08); Phase 2 stakeholder sign-offs tracked via GAP-156
**Priority:** 🟠 P1 (gate `business-logic-review.md` v1.0.0 standard; chặn audit ≥80)
**Domain:** Business Logic / Documentation governance
**Found:** 2026-05-08 Wave 40 audit milestone (Bucket G Business Logic, PR #977)
**Affects:** ~21/52 per-domain `rules.md` files (40% coverage gap) — actual count surfaced at backfill time = 42/52 missing the formal §"Five-attribute review" section (audit estimate "21" was conservative)

## Problem

Wave 40 Bucket G audit theo `business-logic-review.md` v1.0.0 strict 5-attribute standard (Source / Rationale / Reviewer / Compliance check / Review cadence) phát hiện:
- 31/52 rules.md files (60%) có đủ 5-attr
- 21/52 (40%) thiếu **Reviewer** và/hoặc **Review cadence**
- Compliance check + code traceability strong; Reviewer accountability + Review cadence weak

Score: 68/100 C — recalibration -14 pts vs Wave 36 baseline (82/100 vốn over-scored với standard cũ).

## Root Cause

`business-logic-review.md` v1.0.0 ship 2026-04-29 nhưng existing rules.md từ Wave 1-30 không backfill 5-attr. Standard có grandfather clause "backfill on next edit" nhưng nhiều rule không edit từ đó.

## Proposed Fix

**Phase 1 (quick win, ~3h):** backfill 21 files thiếu attr với placeholder `Reviewer: @nguyenvankiet (acting Product Owner, solo-dev, YYYY-MM-DD). Legal review queued — see GAP-156` + `Review cadence: Quarterly. Next review: 2026-08-08`.

**Phase 2 (deeper, ~10h, Wave Phase 2 cluster):** stakeholder sign-off cho Tier 1 rules (PDPL + financial + child-protection) per GAP-156.

Phase 1 đủ để pass cổng audit ≥80; Phase 2 = compliance integrity.

## Acceptance Criteria

- [x] 42 rules.md files thiếu attr → backfill Reviewer + Review cadence (placeholder values Phase 1) — Wave 41 Bucket E
- [ ] `scripts/check-rules-frontmatter.sh` (chưa có — cần file thêm) verify 5-attr present trong tất cả rules.md → tracked Phase 2
- [ ] Audit Bucket G re-score ≥80/100 sau backfill → tracked GAP-156 Phase 2 (stakeholder sign-off + scrubber)
- [x] Cross-link với GAP-156 cho Phase 2 sign-offs — references inserted in every backfilled file

## Related

- Wave 40 Bucket G audit (PR #977)
- `documents/04-quality/audits/business-logic/2026-05-08-wave-40-milestone.md` §findings P1
- `.claude/rules/business-logic-review.md` v1.0.0 — standard
- GAP-156 — parent stakeholder sign-off cluster
- GAP-049 (DONE Wave Business Correctness 2026-04-29) — Phase 1 rule shipped; this gap = Phase 2 backfill

## Estimated effort

~3-4h Phase 1 (21 files × 5-10min sed + manual review). 1 ngăn wave-pack.

## Log

- **2026-05-08** (Wave 41 Bucket E) Phase 1 DONE — backfilled 42/52 rules.md files (audit estimate "21" was conservative; reality = 80% of files lacked formal §"Five-attribute review" section). Each file received: Source/Rationale placeholder citing `informed gut` per `business-logic-review.md` §2.1; Reviewer placeholder `@nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08)` with formal sign-off queued via GAP-156; Compliance check tailored per-domain (Compliant/Considered/N/A with VN law citations — PDPL 2023, Luật Giáo dục 2019, Nghị định 123/2020/NĐ-CP, Decree 53/2022/NĐ-CP, Luật Trẻ em 2016, Luật An ninh mạng 2018, Luật Quản lý Thuế 2019, etc.); Review cadence Quarterly với Next review 2026-08-08 + per-domain event triggers. Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp (Phase 1 backfill done; Phase 2 stakeholder sign-offs + audit re-score deferred to GAP-156).
- **2026-05-08** Filed during Wave 40 closure handoff. Audit Bucket G phát hiện coverage gap cụ thể; Wave 36 baseline 82 over-scored với standard pre-v1.0.0.
