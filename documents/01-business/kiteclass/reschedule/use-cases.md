# Reschedule — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-05-25
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/`

---

## Use Cases

### UC-RESCHEDULE-01: Teacher reschedule class (đổi lịch lớp)

**Actor:** Teacher (MAIN_TEACHER hoặc ASSISTANT của class) / Admin
**Precondition:** Class status `ACTIVE` hoặc `SCHEDULED`; teacher có quyền per BR-RESCHEDULE-006.

**Steps:**

1. Teacher mở UI `(teacher)/teacher/classes/[classId]/detail` → click nút "Đổi lịch"
2. FE hiển thị `RescheduleClassDialog` modal với form:
   - DatePicker `newStartDate` (default = class.startDate hiện tại)
   - DatePicker `newEndDate` (default = class.endDate hiện tại)
   - Dropdown `reasonCategory` (6 enum values, Vietnamese display) — mandatory per BR-RESCHEDULE-002
   - Textarea `reasonNotes` — optional, max 2000 chars per BR-RESCHEDULE-003; FE highlight mandatory khi user chọn `LY_DO_KHAC`
3. Teacher điền form + click "Xác nhận đổi lịch"
4. FE call `POST /api/v1/classes/{classId}/reschedule` với `RescheduleClassRequest` body
5. BE `ClassController.rescheduleClass` → `ClassServiceImpl.rescheduleClass`:
   - Validate permission per BR-RESCHEDULE-006 (teacher member of class hoặc admin)
   - Validate `newStartDate >= today` per BR-RESCHEDULE-004
   - Validate `newEndDate > newStartDate` per BR-RESCHEDULE-005
   - Validate `reasonNotes` mandatory nếu `LY_DO_KHAC` per BR-RESCHEDULE-003
   - Check holiday overlap per BR-RESCHEDULE-009 — non-blocking warning
   - Persist update Class.startDate/endDate + RescheduleHistory append row + outbox event — same `@Transactional` per BR-RESCHEDULE-007
6. Response 200 + `ClassResponse` (updated dates + holidayWarning field if applicable)
7. FE toast "Đã đổi lịch thành công"; nếu có `holidayWarning` → toast "Lịch mới có ngày lễ <X>, vui lòng kiểm tra"
8. Async: Outbox dispatcher emit `ClassRescheduledEvent` → consumer notify parents (Phase 1.5+) hoặc no-op (Phase 1 BETA per BR-RESCHEDULE-007)

**Postcondition:**

- `classes.start_date`/`end_date` updated in-place
- 1 row append vào `reschedule_history` với old/new dates + reason + actor + timestamp
- 1 row vào outbox emit event (Phase 1.5+ consumer send email)
- Attendance + grade history giữ nguyên (BR-RESCHEDULE-001)

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 400 | `newStartDate` < today | "Ngày bắt đầu mới không được trước ngày hôm nay" |
| 400 | `newEndDate <= newStartDate` | "Ngày kết thúc phải sau ngày bắt đầu" |
| 400 | `reasonCategory` thiếu | "Vui lòng chọn lý do đổi lịch" |
| 400 | `reasonCategory = LY_DO_KHAC` nhưng `reasonNotes` rỗng | "Vui lòng nhập chi tiết lý do" |
| 400 | `reasonNotes` > 2000 chars | "Ghi chú không được vượt quá 2000 ký tự" |
| 403 | Teacher không phải member của class | "Bạn không có quyền đổi lịch lớp này" |
| 404 | Class không tồn tại / cross-tenant | "Không tìm thấy lớp học" |
| 409 | Class đã COMPLETED hoặc ARCHIVED | "Không thể đổi lịch lớp đã hoàn thành" |

**FE behavior:**

- Reschedule button visible chỉ cho TEACHER/ADMIN role; hidden cho PARENT/STUDENT
- Holiday warning toast WARN (yellow); KHÔNG block submit
- After success → refresh class detail page; reschedule history section show new entry top
- Disable submit button trong khi BE processing (loading state)
- FE format date theo VN convention `Thứ Hai, 25/05/2026` (per `vn-localization-audit-checklist.md` §1)

**Notes:**

- Reschedule history hiển thị in `RescheduleHistoryTable` component (date / actor / reason / notes) — append-only per BR-RESCHEDULE-010
- Phase 1 BETA: KHÔNG send email to parents (no-op consumer). Phase 1.5+ enable email template `class-rescheduled-parent`

### UC-RESCHEDULE-02: View reschedule history (xem lịch sử đổi lịch)

**Actor:** Teacher / Admin / Parent (own children's classes)
**Precondition:** Class có ≥1 reschedule event.

**Steps:**

1. User mở UI class detail → scroll xuống section "Lịch sử đổi lịch"
2. FE call `GET /api/v1/classes/{classId}/reschedule-history`
3. BE return list of `RescheduleHistoryResponse` rows (sorted desc by `rescheduledAt`)
4. FE render `RescheduleHistoryTable` với columns:
   - Thời điểm đổi (`rescheduledAt`, VN format)
   - Người đổi (`rescheduledBy` + denormalized name)
   - Từ ngày (`oldStartDate` - `oldEndDate`)
   - Sang ngày (`newStartDate` - `newEndDate`)
   - Lý do (`reasonCategory` Vietnamese display)
   - Ghi chú (`reasonNotes` if any, truncated 80 chars + "Xem thêm" expand)

**Postcondition:** History displayed; rows append-only per BR-RESCHEDULE-010.

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 403 | User không có quyền view class | "Bạn không có quyền xem lịch sử lớp này" |
| 404 | Class không tồn tại | "Không tìm thấy lớp học" |

**FE behavior:**

- Empty state: "Chưa có lịch sử đổi lịch" + icon
- Pagination 20 rows/page nếu >20 entries
- Filter dropdown by `reasonCategory` (Phase 1.5+ enhancement)
