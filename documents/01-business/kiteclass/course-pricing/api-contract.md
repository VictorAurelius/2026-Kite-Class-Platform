# Course Pricing — API Contract

> Extracted from: `CourseController`, `CreateCourseRequest`, `UpdateCourseRequest`, `CourseResponse`, `PricingModel`
> Base path: `/api/v1/courses`
> ADR reference: [`ADR-035-pricing-model-taxonomy.md`](../../../02-architecture/adr/ADR-035-pricing-model-taxonomy.md)

## Endpoints

### POST `/api/v1/courses`

Tạo khoá học mới với pricing model.

- **Auth:** Required (`OWNER` / `ADMIN` / `TEACHER` roles)
- **Request:** `CreateCourseRequest` (relevant pricing fields)
  ```json
  {
    "name": "Tiếng Anh Giao Tiếp B1",
    "code": "ENG-B1-001",
    "description": "Khoá tiếng Anh giao tiếp trình độ B1",
    "pricingModel": "PER_HOUR",
    "unitPrice": 250000.00,
    "durationWeeks": 12,
    "totalSessions": 36
  }
  ```
  - `pricingModel` (enum `PricingModel`, required per BR-PRICING-001): `PER_HOUR` | `MONTHLY` | `COURSE_PACKAGE` | `FREE`
  - `unitPrice` (BigDecimal, required, ≥ 0 per BR-PRICING-005; MUST be 0 nếu `pricingModel = FREE` per BR-PRICING-006)
  - Other course fields per existing `CreateCourseRequest`
- **Response:** `201 Created` + `ApiResponse<CourseResponse>`
- **Errors:**
  | Code | Condition | Message |
  |------|-----------|---------|
  | `400` | `pricingModel` null/invalid | "Vui lòng chọn mô hình giá" |
  | `400` | `unitPrice` null | "Vui lòng nhập đơn giá" |
  | `400` | `unitPrice < 0` | "Đơn giá không được âm" |
  | `400` | `unitPrice > 100.000.000` | "Đơn giá quá lớn (tối đa 100 triệu)" |
  | `400` | `FREE` + `unitPrice > 0` | "Khoá học miễn phí phải có đơn giá = 0" |
  | `409` | `code` đã tồn tại | "Mã khoá học đã tồn tại" |

### PUT `/api/v1/courses/{id}`

Cập nhật course pricing (subject to BR-COURSE-002 status gate).

- **Auth:** Required (`OWNER` / `ADMIN` / course CREATOR teacher)
- **Path:** `id` (Long, required)
- **Request:** `UpdateCourseRequest`
  ```json
  {
    "pricingModel": "MONTHLY",
    "unitPrice": 1500000.00
  }
  ```
- **Response:** `200 OK` + `ApiResponse<CourseResponse>`
- **Errors:**
  | Code | Condition | Message |
  |------|-----------|---------|
  | `400` | Same validation as POST | (same) |
  | `403` | User không có quyền (PUBLISHED course non-admin/non-creator) | "Chỉ admin có thể chỉnh giá khoá đã xuất bản" |
  | `404` | Course không tồn tại | "Không tìm thấy khoá học" |
  | `409` | Course status `ARCHIVED` | "Không thể chỉnh khoá đã lưu trữ" |

### GET `/api/v1/courses/{id}`

Lấy thông tin khoá học bao gồm pricing.

- **Auth:** Required
- **Response:** `200 OK` + `ApiResponse<CourseResponse>` với `pricingModel` + `unitPrice` fields
- **Errors:** `404` not found

### GET `/api/v1/courses`

Search courses (existing endpoint), response include pricing fields.

## DTOs

### CreateCourseRequest / UpdateCourseRequest (pricing-relevant fields)

| Field | Type | Required | Description |
|-------|------|---------|-------------|
| pricingModel | PricingModel | ✅ | Enum 4 values per BR-PRICING-001 |
| unitPrice | BigDecimal | ✅ | VND, precision 19/2; MUST be 0 if pricingModel = FREE |

### CourseResponse (pricing-relevant fields)

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/dto/CourseResponse.java`

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Course ID |
| name | String | Course name |
| code | String | Course code |
| pricingModel | String | Enum raw value (PER_HOUR / MONTHLY / COURSE_PACKAGE / FREE) |
| pricingModelDisplay | String | Vietnamese display ("Theo giờ" / "Theo tháng" / "Trọn gói khoá học" / "Miễn phí") |
| unitPrice | BigDecimal | Unit price in VND |
| unitPriceFormatted | String | Pre-formatted VND string (vd "250.000đ") cho FE |
| price | BigDecimal | LEGACY field (BR-PRICING-008 soft-deprecated, retained backward compat) |
| status | String | DRAFT / PUBLISHED / ARCHIVED |

### PricingModel enum

Source: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/entity/PricingModel.java`

| Enum value | Display (VN) | Semantics |
|-----------|--------------|-----------|
| `PER_HOUR` | Theo giờ | unitPrice × session hours; market norm cho TT Anh ngữ / STEM |
| `MONTHLY` | Theo tháng | unitPrice × months; kindergarten-adjacent / music programs |
| `COURSE_PACKAGE` | Trọn gói khoá học | Flat-rate cả khoá; IELTS / certification prep |
| `FREE` | Miễn phí | Trial / demo / scholarship; unitPrice = 0 |

## Cross-references

- **Use Cases:** UC-PRICING-01, UC-PRICING-02 (see `use-cases.md`)
- **Business Rules:** BR-PRICING-001..010 (see `rules.md`)
- **ADR:** `ADR-035-pricing-model-taxonomy.md` — LOCKED taxonomy decision
- **Service:** `PricingCalculator.calculate(course, sessionHours, monthSpan)` — strategy dispatch per BR-PRICING-007
- **Migrations:**
  - V67: add `pricing_model` + `unit_price` columns; backfill existing rows → `COURSE_PACKAGE` (per BR-PRICING-004)
  - V70: set DB column default `pricing_model = 'PER_HOUR'` (per BR-PRICING-002)
- **Future:** Invoice generation (Phase 1.5+) consume `PricingCalculator` per UC-PRICING via `InvoiceGenerationService`
