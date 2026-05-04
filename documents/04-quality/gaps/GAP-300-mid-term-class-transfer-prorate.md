# GAP-300: Mid-Term Student Class Transfer + Pro-Rate Tuition

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core enrollment + invoice modules)
**Found:** 2026-05-04 (Wave 17 Bucket B — P2 persona review)
**Affects:** P2 Small Center, P3 Medium Center (frequent transfer between classes); P5 K-12 less common

---

## Problem

Student transfers from class "Toán 9A" (1M VND/month) to "Toán 9B" (1.2M VND/month) on the 15th of the month. Expected:

- 9A invoice charges 500K (50% of month)
- 9B invoice charges 600K (50% of month)
- 9A attendance preserved + visible in 9B's student profile (continuous learning history)

Currently:

- Enrollment module exists but `grep -ri prorate kiteclass-core/src/main/java` returns 0 matches
- Likely full-month-charged-twice OR zero-charged behavior
- Attendance history portability unknown

P2 review evidence: AC-EDGE-002 FAIL.

## Root Cause

Enrollment domain assumed term-aligned start/end. Mid-term transition not modeled. Invoice amount fixed at enrollment time, no recalculation when enrollment ends mid-cycle.

## Proposed Fix

1. Service `EnrollmentService.transferStudent(studentId, fromClassId, toClassId, transferDate)`:
   - End old enrollment: `enrollment.endDate = transferDate - 1 day`
   - Create new enrollment: `startDate = transferDate`
   - Recompute pending invoice for current month: prorate based on day count
   - Generate adjustment row for old class (credit) + new class (debit)
2. Cross-class attendance view: student profile shows merged history (with class label per entry)
3. UI: owner picks "Transfer student" on student detail → modal (target class + transfer date) → preview prorate calculation → confirm
4. Audit log per transfer

## Acceptance Criteria

- [ ] `EnrollmentService.transferStudent()` method implemented with prorate calculation
- [ ] Invoice adjustments emit `InvoiceAdjusted` event to outbox
- [ ] Student profile attendance view spans across class transitions (no data loss)
- [ ] UI workflow: ≤4 clicks; preview before commit
- [ ] Pro-rata edge cases tested: transfer on 1st (full new month), transfer on last day (full old month), transfer on 15th (50/50)
- [ ] Test: integration test transfers student mid-month → asserts invoice amounts + attendance history

## Related

- Parent review: `documents/00-brd/persona-reviews/P2-small-center-round-1-2026-05-04.md` AC-EDGE-002
- Soft-depends on: [GAP-297](GAP-297-batch-monthly-invoice-generation.md) for prorate during batch
- Soft-depends on: [GAP-057](GAP-057-payroll-teacher-commission.md) — commission must NOT double-count when prorating
