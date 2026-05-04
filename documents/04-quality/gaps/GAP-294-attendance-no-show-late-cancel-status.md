# GAP-294: Attendance NO_SHOW status + late-cancel charge variants

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core/attendance) + Frontend (attendance UI) + Business rules
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher (pattern detection for student churn), all personas with cancellation policies

## Problem

P1 AC-EDGE-001 requires ≥3 status options to differentiate "Present / Absent (excused) / Absent (no-show) / Late". State-check `AttendanceStatus.java:20-26` shows 5 statuses: **PRESENT / ABSENT / LATE / EXCUSED / MAKEUP**. The "ABSENT" lacks a no-show variant — currently ABSENT and EXCUSED are sibling statuses, but **no-show without notice** (most useful for retention/churn analysis) is conflated with general ABSENT.

P1 AC-EDGE-002 requires late-cancel handling: "<2h trước class → system cho phép teacher quyết định: charge full / charge partial / waive". State-check `late.*cancel|noShow|charge.*partial|waive` → **0 hits** in `kiteclass-core/src/main/java`. No late-cancel charge policy logic exists.

## Root Cause

Original attendance model focused on K-12 academic context (PRESENT/ABSENT for compliance reporting), not solo-tutor business context (cancellation policies, charge decisions per absence).

## Proposed Fix

1. Backend: extend `AttendanceStatus` enum:
   - `NO_SHOW` (vắng không báo trước) — new value
   - Optional: `LATE_CANCEL` (báo cancel <2h) — new value
   - Add color + display name VN.
2. Migration to add new enum values (no breaking change — existing rows stay).
3. Business rule: per-class "late-cancel window" config (default 2h); UI prompts charge-decision when student marked LATE_CANCEL.
4. Per-attendance `chargeDecision` field (FULL / PARTIAL / WAIVE) + `decisionReason` text. Free for non-LATE_CANCEL statuses.
5. Frontend attendance UI: when status = LATE_CANCEL → modal "Charge: Full / Partial / Waive" + reason field.
6. Reports: solo dashboard view "students with NO_SHOW pattern" (≥3 in last 4 weeks) for retention triage.
7. Document new statuses + business rules per `business-logic-review.md` §2 (5 attributes: source, rationale, reviewer, compliance, cadence).

## Acceptance Criteria

- [ ] AttendanceStatus enum has NO_SHOW + LATE_CANCEL values
- [ ] Migration + tests
- [ ] `chargeDecision` field added; LATE_CANCEL forces decision capture
- [ ] Frontend modal + UI updated
- [ ] "NO_SHOW pattern" report in solo dashboard
- [ ] Business rules documented (§2 5 attributes)
- [ ] AC-EDGE-001 + AC-EDGE-002 PASS

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §5
- AC: AC-EDGE-001, AC-EDGE-002

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check: `AttendanceStatus.java:20-26` has 5 statuses (PRESENT/ABSENT/LATE/EXCUSED/MAKEUP), no NO_SHOW.
