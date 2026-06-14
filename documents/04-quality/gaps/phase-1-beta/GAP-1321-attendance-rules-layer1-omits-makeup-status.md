# GAP-1321: attendance/rules.md Layer-1 omits MAKEUP status (present in code + Layer-2/3)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Docs (KiteClass attendance 3-layer consistency)
**Found:** 2026-06-14 (Business Logic full audit post wave-p0-closeout-1)
**Affects:** `documents/01-business/kiteclass/attendance/rules.md` §1 (status list + Permission Matrix)

## Problem

`AttendanceStatus` enum có **5 giá trị** trong code: PRESENT / ABSENT / LATE / EXCUSED / **MAKEUP** (`AttendanceStatus.java:22-26`, "Học bù", −0 điểm deduction `:58`; dùng tại `AttendanceServiceImpl.java:387,437` + `AttendancePeriod.java:47`).

MAKEUP được document đầy đủ ở **Layer-2/3** + domain láng giềng:
- `attendance/api-contract.md:104,123,516,536` (status enum + makeupCount)
- `attendance/use-cases.md:204` (cell edit)
- toàn bộ `period-attendance/{rules,use-cases,api-contract}.md`

NHƯNG **Layer-1 `attendance/rules.md:23`** ("Attendance statuses: PRESENT, LATE, ABSENT, EXCUSED") chỉ có **4 giá trị, thiếu MAKEUP**; Permission Matrix §1 cũng không nhắc MAKEUP.

Impact: rules.md là source-of-truth per CLAUDE.md §"3-Layer Structure", nhưng stale vs Layer-2/3 + code. Reader dựa rules.md sẽ tưởng chỉ có 4 status. Rubric §2.4 Cat 4.1 (no cross-layer contradiction).

## Root Cause

MAKEUP thêm vào code + api-contract/use-cases ở wave sau (period-attendance K-12 + class-overview batch) nhưng `attendance/rules.md` §1 status list + permission matrix không được sync cùng PR.

## Proposed Fix

Thêm MAKEUP vào `attendance/rules.md`:
- §1 "Attendance statuses": `PRESENT, LATE, ABSENT, EXCUSED, MAKEUP`.
- Point-deduction note (MAKEUP = −0, giống EXCUSED) khớp `AttendanceStatus.java:58`.
- Permission Matrix: làm rõ MAKEUP do teacher/admin set thủ công (không tự động).

## Acceptance Criteria

- [ ] `attendance/rules.md` §1 status list = 5 giá trị khớp `AttendanceStatus.java`.
- [ ] Permission matrix + point-deduction note phản ánh MAKEUP.
- [ ] Cross-layer: rules.md ↔ api-contract.md ↔ use-cases.md ↔ code nhất quán 5 status.

## Related

- **Parent audit:** `documents/04-quality/audits/business-logic/2026-06-14-business-logic-full-audit.md` (Finding 2)
- **Code:** `kiteclass/kiteclass-core/.../common/constant/AttendanceStatus.java:22-26`
- **Sibling:** GAP-1320 (attendance QR/config drift)
