# GAP-989: enrollStudent thiếu class-status guard — enroll vào lớp COMPLETED/CANCELLED → 201

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (business rule — KC-4)
**Found:** 2026-06-05 (Wave flow-kc4 pre-walk persona simulation)
**Affects:** `EnrollmentServiceImpl.enrollStudent` (`/api/v1/enrollments` POST)

## Problem

`enrollStudent` không đọc `clazz.getStatus()` trước khi ghi danh → học sinh có thể được enroll vào lớp đã **COMPLETED / CANCELLED** (trả 201 thay vì 400). Vi phạm business rule: chỉ enroll vào lớp đang SCHEDULED/IN_PROGRESS (còn nhận học sinh). Hệ quả downstream: attendance/grade/invoice gắn vào lớp đã đóng.

## Proposed Fix

Thêm guard trong `enrollStudent`: nếu `clazz.getStatus()` ∈ {COMPLETED, CANCELLED} (hoặc không phải trạng thái nhận-học-sinh) → `ValidationException` "CLASS_NOT_ENROLLABLE" → 400. Xác nhận tập trạng thái hợp lệ từ `rules.md` (course-class domain).

## Acceptance Criteria
- [ ] Enroll vào lớp COMPLETED/CANCELLED → 400 CLASS_NOT_ENROLLABLE
- [ ] Enroll vào lớp SCHEDULED/IN_PROGRESS → 201
- [ ] IT cover cả 2 nhánh

## Related
- Discovered in: Wave flow-kc4 pre-walk 2026-06-05 (FM #3)
