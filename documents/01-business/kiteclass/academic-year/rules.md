# Academic Year — Business Rules

**Domain:** academic-year
**Last verified:** 2026-04-14
**Source:** GAP-053, ADR-002

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| BR-ACYR-001 | Academic year name unique per tenant | — | unique constraint (instance_id, name) |
| BR-ACYR-002 | endDate > startDate | enforced | DB CHECK constraint |
| BR-ACYR-003 | Only 1 CURRENT year per tenant at any time | 1 | Enforced in `AcademicYearService.setCurrent()` |
| BR-ACYR-004 | Academic year contains 1+ semesters | 1-3 | HK1, HK2, optional SUMMER |
| BR-ACYR-005 | Holidays scoped to academic year | — | FK constraint + range check |
| BR-ACYR-006 | VN national holidays auto-seeded on year creation | auto | `VnHolidayProvider.generateForAcademicYear()` |
| BR-ACYR-007 | Status transitions: UPCOMING → CURRENT → COMPLETED | unidirectional | State enforced in service |

## Config

```yaml
academic-year:
  vn-holiday-provider: vn  # Strategy: vn, en, international
  auto-seed-holidays: true
```

## Default VN Holidays (seeded per year)

| Holiday | Date | Notes |
|---------|------|-------|
| Tết Dương lịch | 1/1 | Solar |
| Tết Nguyên đán | late Jan/early Feb | Lunar, 7 days |
| Giỗ tổ Hùng Vương | 10/3 lunar (~April 18) | Lunar |
| Ngày Thống nhất | 30/4 | Solar |
| Quốc tế Lao động | 1/5 | Solar |
| Quốc khánh | 2/9 | Solar |

## State Machine

```
UPCOMING ──setCurrent()──> CURRENT ──endDate passed──> COMPLETED
                              │
                              │ demote (previous when new becomes CURRENT)
                              ▼
                          COMPLETED
```

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Luật Giáo dục 2019 (school-year structure, holidays, semester boundaries); MoET regulations on academic calendar. No PDPL trigger (no PII).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: MoET regulation amendment, Vietnamese national-holiday calendar update.

## Log
- 2026-04-14 — Initial rules (GAP-053, ADR-002)
