# Reschedule — API Contract

> Extracted from: `ClassController.rescheduleClass`, `RescheduleClassRequest`, `RescheduleReasonCategory`, `ClassResponse`
> Base path: `/api/v1/classes`
> ADR reference: [`ADR-033-class-reschedule-pattern.md`](../../../02-architecture/adr/ADR-033-class-reschedule-pattern.md)

## Endpoints

### POST `/api/v1/classes/{classId}/reschedule`

Đổi lịch học của lớp (preserves attendance + grade history per BR-RESCHEDULE-001).

- **Auth:** Required (`TEACHER` / `ADMIN` / `PLATFORM_ADMIN` roles)
- **Path:**
  - `classId` (Long, required) — target class ID
- **Request:** `RescheduleClassRequest`
  ```json
  {
    "newStartDate": "2026-06-01",
    "newEndDate": "2026-08-31",
    "reasonCategory": "LE_TET_NGHI_CHINH_THUC",
    "reasonNotes": "Nghỉ Tết Nguyên Đán, dời lịch sang sau Tết"
  }
  ```
  - `newStartDate` (LocalDate ISO-8601, required, ≥ today per BR-RESCHEDULE-004)
  - `newEndDate` (LocalDate ISO-8601, required, > `newStartDate` per BR-RESCHEDULE-005)
  - `reasonCategory` (enum `RescheduleReasonCategory`, required per BR-RESCHEDULE-002):
    - `GV_OM_BAN_DOT_XUAT` — Giáo viên ốm/bận đột xuất
    - `PHONG_HOC_KHONG_KHA_DUNG` — Phòng học không khả dụng
    - `MAT_DIEN_INTERNET` — Mất điện / mất Internet
    - `LE_TET_NGHI_CHINH_THUC` — Lễ Tết / nghỉ chính thức
    - `HOC_SINH_XIN_NGHI_TAP_THE` — Học sinh xin nghỉ tập thể
    - `LY_DO_KHAC` — Lý do khác (BẮT BUỘC nhập `reasonNotes`)
  - `reasonNotes` (String, optional, max 2000 chars per BR-RESCHEDULE-003; mandatory nếu `reasonCategory = LY_DO_KHAC`)
- **Response:** `200 OK` + `ApiResponse<ClassResponse>` (updated dates + optional `holidayWarning` field)
- **Side-effects:**
  - `classes.start_date` + `end_date` updated in-place
  - `reschedule_history` table append row (old/new dates + reason + actor + timestamp)
  - `outbox_event` row appended với routing key `class.rescheduled` — consumer notify parents (Phase 1.5+) hoặc no-op (Phase 1 BETA)
- **Errors:**
  | Code | Condition | Message |
  |------|-----------|---------|
  | `400` | `newStartDate` < today | "Ngày bắt đầu mới không được trước ngày hôm nay" |
  | `400` | `newEndDate <= newStartDate` | "Ngày kết thúc phải sau ngày bắt đầu" |
  | `400` | `reasonCategory` null | "Vui lòng chọn lý do đổi lịch" |
  | `400` | `reasonCategory = LY_DO_KHAC` + `reasonNotes` empty | "Vui lòng nhập chi tiết lý do" |
  | `400` | `reasonNotes` > 2000 chars | "Ghi chú không được vượt quá 2000 ký tự" |
  | `403` | Teacher không phải member class | "Bạn không có quyền đổi lịch lớp này" |
  | `404` | Class không tồn tại (hoặc cross-tenant) | "Không tìm thấy lớp học" |
  | `409` | Class status `COMPLETED` / `ARCHIVED` | "Không thể đổi lịch lớp đã hoàn thành" |

### GET `/api/v1/classes/{classId}/reschedule-history`

Liệt kê lịch sử đổi lịch của một lớp.

- **Auth:** Required (`TEACHER` / `ADMIN` / `PARENT` của students enrolled)
- **Path:** `classId` (Long, required)
- **Query:**
  - `page` (Integer, default 0)
  - `size` (Integer, default 20)
  - `sort` (String, default `rescheduledAt,desc`)
- **Response:** `200 OK` + `ApiResponse<PageResponse<RescheduleHistoryResponse>>`
- **Errors:**
  | Code | Condition | Message |
  |------|-----------|---------|
  | `403` | User không có quyền view class | "Bạn không có quyền xem lịch sử lớp này" |
  | `404` | Class không tồn tại | "Không tìm thấy lớp học" |

## DTOs

### RescheduleClassRequest

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/dto/RescheduleClassRequest.java`

| Field | Type | Required | Description |
|-------|------|---------|-------------|
| newStartDate | LocalDate | ✅ | New start date (must be ≥ today) |
| newEndDate | LocalDate | ✅ | New end date (must be > newStartDate) |
| reasonCategory | RescheduleReasonCategory | ✅ | Dropdown enum, 6 values |
| reasonNotes | String | ⚠️ optional | Max 2000 chars; mandatory when reasonCategory = LY_DO_KHAC |

### ClassResponse (relevant fields post-reschedule)

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Class ID |
| startDate | LocalDate | New start date (post-reschedule) |
| endDate | LocalDate | New end date (post-reschedule) |
| status | String | Unchanged (ACTIVE/SCHEDULED) per BR-RESCHEDULE-001 |
| holidayWarning | String | Optional WARN message nếu newStartDate/newEndDate overlap holiday VN (per BR-RESCHEDULE-009) |

### RescheduleHistoryResponse

| Field | Type | Description |
|-------|------|-------------|
| id | Long | History row ID |
| classId | Long | Reference class ID |
| oldStartDate | LocalDate | Date before reschedule |
| oldEndDate | LocalDate | Date before reschedule |
| newStartDate | LocalDate | Date after reschedule |
| newEndDate | LocalDate | Date after reschedule |
| reasonCategory | String | Enum value (raw) |
| reasonCategoryDisplay | String | Vietnamese display name (per `RescheduleReasonCategory.getDisplayNameVi()`) |
| reasonNotes | String | Optional notes |
| rescheduledBy | Long | Actor user ID |
| rescheduledByName | String | Denormalized actor name |
| rescheduledAt | Instant | Timestamp |

### RescheduleReasonCategory enum

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/RescheduleReasonCategory.java`

| Enum value | Display (VN) |
|-----------|--------------|
| `GV_OM_BAN_DOT_XUAT` | Giáo viên ốm/bận đột xuất |
| `PHONG_HOC_KHONG_KHA_DUNG` | Phòng học không khả dụng |
| `MAT_DIEN_INTERNET` | Mất điện / mất Internet |
| `LE_TET_NGHI_CHINH_THUC` | Lễ Tết / nghỉ chính thức |
| `HOC_SINH_XIN_NGHI_TAP_THE` | Học sinh xin nghỉ tập thể |
| `LY_DO_KHAC` | Lý do khác |

## Cross-references

- **Use Cases:** UC-RESCHEDULE-01, UC-RESCHEDULE-02 (see `use-cases.md`)
- **Business Rules:** BR-RESCHEDULE-001..010 (see `rules.md`)
- **ADR:** `ADR-033-class-reschedule-pattern.md` — LOCKED design decision
- **Event:** `ClassRescheduledEvent` (outbox emit per BR-RESCHEDULE-007)
- **Consumers:** `ClassRescheduledNoOpConsumer` (Phase 1 BETA), `ClassRescheduledEmailConsumer` (Phase 1.5+)
- **Migration:** Holiday lookup leverages Wave 2 `Holiday` entity (per BR-RESCHEDULE-009)
