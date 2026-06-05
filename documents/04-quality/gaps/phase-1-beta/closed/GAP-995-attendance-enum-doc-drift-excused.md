# GAP-995: docs drift EXCUSED_ABSENCE vs enum EXCUSED (attendance)

**Status:** 🟢 DONE (Wave flow-kc5 G1 walk PASS, 2026-06-05)
**Priority:** 🟢 P3
**Domain:** Backend (docs — KC-5)
**Found:** 2026-06-05 (Wave flow-kc5 pre-walk persona simulation, FM #1)
**Affects:** `documents/01-business/kiteclass/attendance/{rules,use-cases}.md`

## Problem

rules.md BR-ATT-005 + use-cases.md ghi `EXCUSED_ABSENCE` nhưng enum thực tế (`AttendanceStatus`) chỉ có `EXCUSED`. Gửi `"EXCUSED_ABSENCE"` (theo docs) → 400 MALFORMED_REQUEST_BODY. Drift đã được flag từ GAP-232 (api-contract.md line 107) như follow-up cần fix.

## Proposed Fix

Rename `EXCUSED_ABSENCE` → `EXCUSED` trong rules.md + use-cases.md để khớp source-of-truth (enum). Cập nhật note trong api-contract.md → resolved.

## Acceptance Criteria
- [x] rules.md + use-cases.md dùng `EXCUSED`
- [x] api-contract.md note → resolved

## Related
- Closes follow-up flagged in GAP-232 (api-contract.md drift note)
- Discovered in: Wave flow-kc5 pre-walk 2026-06-05 (FM #1)

## Log

- **2026-06-05 (Wave flow-kc5 — DONE):** rename EXCUSED_ABSENCE → EXCUSED (rules.md + use-cases.md); api-contract note resolved. G1 walk: `EXCUSED_ABSENCE` → 400, `EXCUSED` → 201.
