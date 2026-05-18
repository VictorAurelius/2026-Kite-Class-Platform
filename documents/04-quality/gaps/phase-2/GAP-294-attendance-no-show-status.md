# GAP-294: Add NO_SHOW attendance status (separate from EXCUSED)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 — UX gap; AC-EDGE-001 FAIL
**Domain:** Backend (kiteclass-core/common/constant/AttendanceStatus.java)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher (track no-show pattern → drop student decision); P2/P3 Center (attendance analytics)

## Problem

Theo AC-EDGE-001, teacher PHẢI mark "no-show" cho student vắng mặt không báo trước, system track riêng vs "excused absent". Cần ≥3 status options: Present / Absent (excused) / Absent (no-show) / Late.

Hiện trạng: `AttendanceStatus.java` có 4 values: PRESENT/ABSENT/LATE/EXCUSED. KHÔNG phân biệt NO_SHOW vs ABSENT vs EXCUSED. Solo teacher không track được pattern no-show để decide có drop student không.

**State-check (verified 2026-05-04):**
- `kiteclass-core/src/main/java/com/kiteclass/core/common/constant/AttendanceStatus.java`: enum values PRESENT/ABSENT/LATE/EXCUSED + scoreImpact mapping
- KHÔNG có NO_SHOW value — semantic ABSENT ambiguous (excused or unexcused?)
- Gamification scoring: ABSENT = -10, EXCUSED = 0 — implies ABSENT = unexcused, but EXCUSED ≠ NO_SHOW

## Root Cause

AttendanceStatus enum thiết kế simple 4-state. Real-world solo tutoring requires distinguishing planned-absence (báo trước, EXCUSED) vs unexpected-absence (no-show, no notification) — different business signal cho retention decision.

## Proposed Fix

1. **Backend:**
   - Add `NO_SHOW` to `AttendanceStatus` enum với `scoreImpact = -15` (worse than ABSENT -10)
   - Display: "Vắng không báo" / shortCode "VK" / color "bg-orange-600"
   - Migration V60+ Flyway: default = no migration needed (new enum value, existing rows = ABSENT preserved)
2. **FE:**
   - `attendance/page.tsx` + `dynamic-attendance-form-list.tsx` add NO_SHOW button option
   - Reorder: Present / Late / Absent (excused) / NO_SHOW
3. **Analytics:**
   - Student profile show NO_SHOW count + ratio per month
   - Trigger gentle alert ("Học sinh X đã no-show 3 lần trong 30 ngày") để teacher decide
4. **Documentation:** update `documents/01-business/kiteclass/attendance/rules.md` BR-ATTEND-NOSHOW-001 với 5 attributes

## Acceptance Criteria

- [ ] `AttendanceStatus.NO_SHOW` enum value added
- [ ] FE supports 5 status options (incl NO_SHOW)
- [ ] Migration backward-compatible
- [ ] Student profile shows NO_SHOW count
- [ ] Business rule documented per `business-logic-review.md` 5-attribute standard
- [ ] Gamification scoring updated
- [ ] Unit + IT tests for new state

## Related

- AC-EDGE-001 (P1 review 2026-05-04)
- GAP-295 (Late-cancel policy — paired edge case workflow)

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
