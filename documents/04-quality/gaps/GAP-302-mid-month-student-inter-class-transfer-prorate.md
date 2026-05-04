# GAP-302: Mid-month student inter-class transfer with pro-rated tuition + preserved attendance/grade history

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Feature-P2 — episodic edge case but high pain when triggered, ~5% student volume per quarter)
**Domain:** Backend / Data Model
**Found:** 2026-05-04 (P2 Small Center persona review round 1)
**Persona blocked:** P2 Small Tutoring Center; also P3, P5
**Wave:** TBD

## Problem

**AC-EDGE-002 (P2 owner):** Student transfers from "Toán 9A" (1M VND/month) to "Toán 9B" (1.2M VND/month) on the 15th. Expected behaviour:
- 9A invoice for the month pro-rates to 500K (50% of month before transfer)
- 9B invoice charges 600K (50% of month after transfer)
- Student's prior 9A attendance + grade entries remain visible in student profile (history preserved across class change)

Today: grep `transfer\|reEnroll` in `kiteclass-core/.../module/enrollment/` returns 0 hits. Owner workaround = drop student from 9A + re-enrol in 9B. Two side-effects:
1. Tuition is fully charged twice OR fully waived (no pro-rate) — owner must adjust manually via `InvoiceController.adjustments`.
2. Attendance/grade history attached to the enrollment may not be visible from the new enrollment context (FE shows only current enrollment's records by default — needs verification).

## Root Cause

`Enrollment` entity tracks (student, class, status). Transfer is just two enrollment changes, but pro-rate logic + cross-enrollment history view weren't built. Likely never enumerated until persona review.

## Proposed Fix

| Sub-task | Surface | Estimate |
|---|---|---|
| `POST /api/v1/students/{id}/transfer` — atomic: deactivate enrollment A, create enrollment B, pro-rate invoice A, pre-create pro-rated invoice B | Backend | 1.5d |
| `Enrollment` adds `transferred_to_enrollment_id` + `transferred_from_enrollment_id` chain so history-walk works | Backend + migration | 0.5d |
| Student profile attendance + grade view walks the enrollment chain (not just current) | Backend + Frontend | 1d |
| Commission engine (GAP-057) handles pro-rated invoice cleanly | Backend | (covered by GAP-057 spec) |

## Acceptance Criteria

- [ ] Transfer endpoint runs in single transaction, both enrollments + both invoice rows committed atomically
- [ ] Pro-rate calculation uses days-elapsed-in-billing-period heuristic (configurable rule, default = days/30)
- [ ] Student profile in 9B shows previous 9A attendance + grade entries with class label
- [ ] AC-EDGE-002 (P2 owner) flips PASS in next P2 review
- [ ] Commission engine, when shipped, attributes the right amount to each teacher pro-rata

## Related

- Audit: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` §5
- Dependency: GAP-057 (commission must consume pro-rated invoices)
- Sibling concept: GAP-300 (batch invoice generator must skip already-pro-rated cases)
- Reference AC: `documents/00-brd/persona-criteria/P2-small-center.md` AC-EDGE-002
