# GAP-1043: Reschedule chấp nhận past-date — thiếu `@FutureOrPresent` (data integrity)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core) — validation/data-integrity
**Found:** 2026-06-06 (KC-12 G1 walk, FM-6)
**Affects:** `RescheduleClassRequest.java` (`newStartDate` validation) + `ClassServiceImpl.validateDates:580-584`

## Problem

KC-12 G1 walk: `POST /api/v1/classes/{id}/reschedule` chấp nhận `newStartDate` trong **quá khứ** — mâu thuẫn javadoc "newStartDate must be ≥ today".

**Walk evidence:**
```
POST /api/v1/classes/4/reschedule (ADMIN, class SCHEDULED)
  {"newStartDate":"2020-01-01","newEndDate":"2020-03-30","reasonCategory":"GV_OM_BAN_DOT_XUAT"}
→ 200 success, startDate=2020-01-01 endDate=2020-03-30  ← reschedule class vào quá khứ
```

Cho phép reschedule lớp về ngày đã qua → lịch học không hợp lệ, attendance/billing/report sai timeline.

## Root Cause

`RescheduleClassRequest.newStartDate` chỉ `@NotNull` — KHÔNG có `@FutureOrPresent`. `ClassServiceImpl.validateDates:580-584` chỉ check `newEndDate > newStartDate`, không check `newStartDate ≥ today`. Javadoc (`RescheduleClassRequest:19` "must be ≥ today") không được enforce trong code.

## Proposed Fix

1. Thêm `@FutureOrPresent(message = "Ngày bắt đầu mới phải từ hôm nay trở đi")` lên `newStartDate` trong `RescheduleClassRequest`.
2. (Defense-in-depth) `validateDates` thêm guard `newStartDate >= LocalDate.now()`.
3. Test: reschedule past-date → 400.

## Acceptance Criteria

- [ ] `POST reschedule` với `newStartDate` quá khứ → 400 (không 200)
- [ ] `newStartDate = today` → accepted (≥ today, không > today)
- [ ] `newEndDate > newStartDate` vẫn enforce (no regression)

## Related

- Discovered in: KC-12 G1 walk (Wave flow-kc12), pre-walk FM-6
- Validation-gap class — low priority (data integrity, không security). Batch với reschedule hardening hoặc next clazz module wave.
