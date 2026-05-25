# Course Pricing — Use Cases

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-05-25
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/`

---

## Use Cases

### UC-PRICING-01: Create course với pricing model

**Actor:** Owner / Admin / Teacher (CREATOR role)
**Precondition:** User có permission `course:create` trong center hiện tại.

**Steps:**

1. Owner mở UI `(owner)/owner/courses/create`
2. FE render `CourseCreateForm` với pricing section:
   - Dropdown `pricingModel` (default "Theo giờ" per BR-PRICING-002)
   - Input `unitPrice` (BigDecimal, VND format `1.500.000đ`) — hidden khi `FREE`
   - Helper text dynamic theo `pricingModel` chọn:
     - PER_HOUR: "Số tiền mỗi giờ học (ví dụ: 250.000đ/giờ)"
     - MONTHLY: "Số tiền học phí mỗi tháng (ví dụ: 1.500.000đ/tháng)"
     - COURSE_PACKAGE: "Tổng số tiền trọn gói cả khoá (ví dụ: 8.000.000đ/khoá)"
     - FREE: "Khoá học miễn phí"
3. Owner điền form + click "Tạo khoá học"
4. FE call `POST /api/v1/courses` với `CreateCourseRequest`
5. BE `CourseServiceImpl.createCourse`:
   - Validate `pricingModel` enum value valid per BR-PRICING-001
   - Validate `unitPrice >= 0` per BR-PRICING-005
   - Validate `unitPrice = 0` khi `pricingModel = FREE` per BR-PRICING-006
   - Persist `Course` entity với `pricingModel` + `unitPrice`; legacy `price` field nullable
6. Response 201 + `CourseResponse`
7. FE redirect to course detail page với toast "Đã tạo khoá học thành công"

**Postcondition:**

- 1 row `courses` được tạo với `pricing_model` + `unit_price`
- 1 row `teacher_courses` auto-created với role=CREATOR (per BR-COURSE-003)
- Course status = DRAFT

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 400 | `pricingModel` null/invalid | "Vui lòng chọn mô hình giá" |
| 400 | `unitPrice` null | "Vui lòng nhập đơn giá" |
| 400 | `unitPrice < 0` | "Đơn giá không được âm" |
| 400 | `unitPrice > 100.000.000` | "Đơn giá quá lớn (tối đa 100 triệu)" |
| 400 | `pricingModel = FREE` + `unitPrice > 0` | "Khoá học miễn phí phải có đơn giá = 0" |
| 409 | `code` đã tồn tại | "Mã khoá học đã tồn tại" |

**FE behavior:**

- Pricing helper text update dynamic khi user thay đổi `pricingModel` dropdown
- `unitPrice` input format VND theo `vn-localization-audit-checklist.md` §1 (dấu chấm thousands separator, đuôi `đ`)
- Disable submit button trong khi BE processing
- Sample data VN-friendly per `vn-localization-audit-checklist.md` §3 (KHÔNG dùng "$60.00")

### UC-PRICING-02: Update course pricing

**Actor:** Owner / Admin
**Precondition:** Course exists; user có permission; course status không phải ARCHIVED.

**Steps:**

1. Owner mở UI `(owner)/owner/courses/[id]/edit`
2. FE pre-fill form với current `pricingModel` + `unitPrice`
3. Owner điều chỉnh fields + submit
4. FE call `PUT /api/v1/courses/{id}` với `UpdateCourseRequest`
5. BE `CourseServiceImpl.updateCourse`:
   - Validate permission per BR-COURSE-002 (PUBLISHED: chỉ admin/creator update pricing; ARCHIVED: read-only)
   - Validate BR-PRICING-005/006
   - Persist update
6. Response 200 + `CourseResponse`
7. FE toast "Đã cập nhật giá khoá học"

**Postcondition:**

- `courses.pricing_model` + `unit_price` updated
- Phase 1.5+: outbox event `CoursePricingChangedEvent` emit → email enrolled parents

**Errors:**

| Code | Condition | Message |
|------|-----------|---------|
| 400 | Same as UC-PRICING-01 | (same) |
| 403 | User không có quyền update PUBLISHED course | "Chỉ admin có thể chỉnh giá khoá đã xuất bản" |
| 404 | Course không tồn tại | "Không tìm thấy khoá học" |
| 409 | Course status = ARCHIVED | "Không thể chỉnh khoá đã lưu trữ" |

**FE behavior:**

- WARN modal khi user thay đổi pricing trên PUBLISHED course: "Đổi giá có thể ảnh hưởng học sinh đã ghi danh. Bạn có chắc?"
- Display "Giá hiện tại: X" + "Giá mới: Y" diff trước khi confirm
