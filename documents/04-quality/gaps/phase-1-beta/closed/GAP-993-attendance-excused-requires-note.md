# GAP-993: EXCUSED status không bắt buộc notes (BR-ATT-005 chưa enforce)

**Status:** 🟢 DONE (Wave flow-kc5 G1 walk PASS, 2026-06-05)
**Priority:** 🟡 P2
**Domain:** Backend (business rule — KC-5)
**Found:** 2026-06-05 (Wave flow-kc5 pre-walk persona simulation, FM #5)
**Affects:** `AttendanceServiceImpl.markAttendance`

## Problem

POST mark với `{"status":"EXCUSED"}` không notes → 201 thay vì 400. BR-ATT-005 (rules.md) + use-cases.md UC-ATT-01 ghi "EXCUSED requires note" nhưng code không check conditional-required.

## Proposed Fix

Trong `markAttendance`: nếu `status == EXCUSED && (notes == null || notes.isBlank())` → `ValidationException("EXCUSED_REQUIRES_NOTE")` → 400.

## Acceptance Criteria
- [x] EXCUSED không notes → 400 EXCUSED_REQUIRES_NOTE (W3)
- [x] EXCUSED có notes → 201 (W1e)
- [x] Unit cover (AttendanceServiceTest +1 test)

## Related
- Discovered in: Wave flow-kc5 pre-walk 2026-06-05 (FM #5)

## Log

- **2026-06-05 (Wave flow-kc5 — DONE):** EXCUSED-requires-note guard; G1 walk PASS — no notes→400, with notes→201.
