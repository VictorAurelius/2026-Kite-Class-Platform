# GAP-994: attendanceRate sai BR-ATT-008 — chỉ tính PRESENT, thiếu LATE

**Status:** 🟢 DONE (Wave flow-kc5 G1 walk PASS, 2026-06-05)
**Priority:** 🟡 P2
**Domain:** Backend (business rule — KC-5)
**Found:** 2026-06-05 (Wave flow-kc5 pre-walk persona simulation, FM #9)
**Affects:** `AttendanceServiceImpl.getStudentAttendanceStats` + `getClassAttendanceStats`

## Problem

`attendanceRate = presentCount * 100.0 / totalSessions` — chỉ đếm PRESENT. BR-ATT-008 (rules.md:20) = `(PRESENT + LATE) / total_sessions * 100%`. Học sinh LATE bị under-report (vd 100% LATE → rate 0% thay vì 100%). `lateCount` đã được tính sẵn nhưng không cộng vào rate.

## Proposed Fix

Đổi formula 2 chỗ: `((presentCount + lateCount) * 100.0 / totalSessions)`.

## Acceptance Criteria
- [x] Student có PRESENT + LATE → rate = (PRESENT+LATE)/total (W8 live: 1 PRESENT + 1 LATE / 5 = 40%, formula cũ = 20%)
- [x] Unit test recompute (8 PRESENT + 1 LATE / 10 = 90%)

## Related
- Discovered in: Wave flow-kc5 pre-walk 2026-06-05 (FM #9)

## Log

- **2026-06-05 (Wave flow-kc5 — DONE):** rate = (PRESENT+LATE)/total; unit test 80→90; G1 stats live rate=40.0 (PRESENT 1+LATE 1)/5.
