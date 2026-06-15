# GAP-1415: Grade pass-threshold + grade/invoice business constants hardcoded (should be config keys)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-15 (hardcode-mock state-check, BE agent — C2 business constants)
**Affects:** `kiteclass-core` grade + invoice services

## Problem

Business rule values baked as literals instead of config keys (rules.md defines them as business rules; HARDCODE class — extract to config):
- `grade/service/GradeServiceImpl.java:96,503` + `grade/entity/Grade.java:143` — passThreshold `BigDecimal.valueOf(50.0)` (pass mark hardcoded; should be tenant/center-configurable)
- `GradeServiceImpl.java:361` — credit `3.0` (tracked GAP-1001; pair here for the grade-config cluster)
- `invoice/service/InvoiceServiceImpl.java:65` — `LATE_FEE_RATE = 0.001` (0.1%/day; should be config — GAP-108 family)
- `invoice/service/InvoiceBatchServiceImpl.java:58` — `DUE_DAYS_FROM_MONTH_START = 7` (billing due-date rule)

These are FUNCTIONAL (business values vary per center/policy) but value-works (P1, not P0).

## Proposed Fix

Extract to config keys per `business-logic-review` (e.g. `kiteclass.grade.pass-threshold:50.0`, `kiteclass.invoice.late-fee-rate:0.001`, `kiteclass.invoice.due-days:7`) driven by `@Value` + documented in domain `rules.md`. Pair grade-config (credit 3.0 GAP-1001 + pass 50.0) as one cluster; invoice constants extend GAP-108.

## Acceptance Criteria

- [ ] passThreshold + late-fee-rate + due-days read from config keys (no literals)
- [ ] Config keys documented in `documents/01-business/**/rules.md`
- [ ] GAP-1001 (credit) + GAP-108 (invoice) cross-referenced/consolidated

## Related

- Umbrella: GAP-1410 · Audit: `2026-06-15-hardcode-mock-state-check.md`
- GAP-1001 (grade credit 3.0), GAP-108 (payment/invoice config keys); `business-logic-review`, `design-patterns` §3.2 primitive-obsession
