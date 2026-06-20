# Course Pricing — Business Rules

**Domain:** KiteClass Core (`module.course` — pricing model subdomain)
**Version:** 1.0
**Updated:** 2026-05-25
**Source code:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/course/`
**ADR:** [`ADR-035-pricing-model-taxonomy.md`](../../../02-architecture/adr/ADR-035-pricing-model-taxonomy.md)

---

## 1. Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-PRICING-001 | 4-value taxonomy `PricingModel` | Enum `PER_HOUR` / `MONTHLY` / `COURSE_PACKAGE` / `FREE` cover VN edu market (per ADR-035). |
| BR-PRICING-002 | `PER_HOUR` default cho new courses | Per ADR-035: VN TT Anh ngữ market bán theo giờ là chính (Apollo 257-344k/giờ, ILA 195-368k/giờ). DB column default `pricing_model = 'PER_HOUR'` per V70 migration. |
| BR-PRICING-003 | `unitPrice` semantics phụ thuộc `pricingModel` | `PER_HOUR`: đồng/giờ (vd 200.000đ/giờ); `MONTHLY`: đồng/tháng (vd 1.500.000đ/tháng); `COURSE_PACKAGE`: đồng/khoá (1-shot, vd 8.000.000đ/khoá); `FREE`: 0. |
| BR-PRICING-004 | Backfill existing courses → `COURSE_PACKAGE` | V67 migration set existing rows (pre-Wave-br-4) đến `pricing_model = 'COURSE_PACKAGE'` để preserve legacy `price` field semantics (full course price). |
| BR-PRICING-005 | `unitPrice >= 0` | DB CHECK constraint; service-layer validation reject negative. NULL coerced to ZERO trong entity Builder. |
| BR-PRICING-006 | `FREE` model → `unitPrice = 0` | Service-layer enforce: nếu `pricingModel = FREE`, `unitPrice` MUST be 0 (else 400). |
| BR-PRICING-007 | `PricingCalculator` strategy per model | Service `PricingCalculator.calculate(course, sessionHours, monthSpan)` dispatch theo `pricingModel`: PER_HOUR → unitPrice × hours; MONTHLY → unitPrice × months; COURSE_PACKAGE → unitPrice (flat); FREE → 0. |
| BR-PRICING-008 | Legacy `price` field deprecated nhưng retained | Soft-deprecated Wave br-4; KHÔNG remove vì backward compat với `CourseMapper` + IT fixtures. New code MUST use `pricingModel` + `unitPrice`. |
| BR-PRICING-009 | Status transition + pricing update | DRAFT: free to edit pricing. PUBLISHED: chỉ admin có quyền update pricing (BR-COURSE-002 inherit). ARCHIVED: read-only. |
| BR-PRICING-010 | Multi-tenant isolation | Mọi query filter theo `instance_id` qua TenantContext interceptor. |

### BR-PRICING-001: 4-value taxonomy (LOCKED ADR-035)

- **Value:** `PricingModel` enum 4 values: `PER_HOUR`, `MONTHLY`, `COURSE_PACKAGE`, `FREE`.
- **Rationale:** VN edu market research Wave br-4 surveyed Apollo, ILA, ACET, Yola, Vietopia, AMA — 95%+ pricing model fall vào 4 categories. PER_HOUR dominant (English/STEM tutoring trung tâm dạy thêm); MONTHLY common (kindergarten-adjacent music); COURSE_PACKAGE suits IELTS/certification prep; FREE cho trial/demo class. Adding more enums (vd SUBSCRIPTION, USAGE_BASED) defer Phase 2+ when persona surfaces need.
- **Source:** ADR-035 + Wave beta-readiness-4 Bucket C external benchmark (Apollo/ILA published rate sheets).
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-25). Stakeholder review queued via GAP-156.
- **Compliance check:** **Considered** — Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (price transparency); Luật Quản lý Thuế 2019 (invoice line-item structure derives từ pricing model).
- **Review cadence:** Annual (stable taxonomy). **Next review:** 2027-05-25. Event triggers: persona feedback "thiếu SUBSCRIPTION model", competitor pivot to new model class, BE persona expanding scope outside trung tâm dạy thêm.

### BR-PRICING-002: PER_HOUR default (LOCKED Wave br-4 GAP-292a)

- **Value:** New `Course` rows default `pricingModel = PricingModel.PER_HOUR`; V70 migration set DB column default `'PER_HOUR'`.
- **Rationale:** VN TT Anh ngữ + STEM dạy thêm market bán theo giờ là chính (>70% trung tâm surveyed). MONTHLY common cho kindergarten-adjacent only (~15%). COURSE_PACKAGE cho IELTS prep niche (~10%). FREE trial (~5%). Default reflect majority case + reduce form friction onboarding.
- **Source:** ADR-035 §Default Choice + Wave br-4 Bucket C inside-out + market data.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-25).
- **Compliance check:** N/A — default value selection.
- **Review cadence:** Quarterly cho first 2 quarters (Phase 1 BETA validate default fit), sau đó Annual. **Next review:** 2026-08-25. Event triggers: analytics surface <50% courses use PER_HOUR (default mis-aligned), persona complaint "default sai".

### BR-PRICING-007: PricingCalculator strategy (LOCKED Wave br-4)

- **Value:** Service `PricingCalculator.calculate(course, sessionHours, monthSpan)` dispatch theo `pricingModel`:
  - `PER_HOUR` → `unitPrice × sessionHours` (cumulative invoice line)
  - `MONTHLY` → `unitPrice × monthSpan` (e.g., 3-month enrollment = unitPrice × 3)
  - `COURSE_PACKAGE` → `unitPrice` flat (1-shot, ignore hours/months)
  - `FREE` → `BigDecimal.ZERO`
- **Rationale:** Polymorphic dispatch via strategy pattern (per `design-patterns.md` §2 mandate cho "Multiple implementations") rõ ràng + testable. Avoid if/switch trên `pricingModel` rải rác trong calling code (BR-PRICING-007 enforce strategy boundary).
- **Source:** Wave br-4 Bucket C implementation `PricingCalculator.java`; ADR-035 §Implementation Strategy.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-05-25).
- **Compliance check:** N/A — implementation invariant.
- **Review cadence:** Annual (stable design). **Next review:** 2027-05-25. Event triggers: new pricing model enum added (BR-PRICING-001 changed) → strategy class needs new method.

---

## 2. Flow

### Create Course Pricing Flow

1. Owner/Admin mở UI `(owner)/owner/courses/create` → fill course form
2. Form expose pricing section:
   - Dropdown `pricingModel` (4 options Vietnamese display: "Theo giờ" / "Theo tháng" / "Trọn gói khoá học" / "Miễn phí") — default "Theo giờ" per BR-PRICING-002
   - Input `unitPrice` (BigDecimal, VND format `1.500.000đ` per `vn-localization-audit-checklist.md` §1) — hidden khi `FREE`
   - Helper text giải thích semantics per BR-PRICING-003 (vd "Số tiền mỗi giờ" / "Số tiền mỗi tháng" / "Tổng số tiền cả khoá")
3. Owner submit → FE call `POST /api/v1/courses` với `CreateCourseRequest` body
4. BE `CourseServiceImpl.createCourse`:
   - Validate per BR-PRICING-005/006
   - Persist `Course` entity với `pricingModel` + `unitPrice` (legacy `price` field nullable)
5. Response 201 + `CourseResponse`

### Invoice Generation Flow (Phase 1.5+)

1. Owner trigger billing run (manual hoặc cron) → `InvoiceGenerationService.generateForCourse(courseId, enrollmentPeriod)`
2. Service `PricingCalculator.calculate(course, sessionHours, monthSpan)` dispatch theo `pricingModel` per BR-PRICING-007
3. Invoice line item: `description` = course name + pricing model display, `amount` = calculated value
4. Persist `Invoice` row + outbox event `InvoiceGeneratedEvent`

### Update Course Pricing Flow

1. Owner mở UI `(owner)/owner/courses/[id]/edit`
2. Form pre-fill current `pricingModel` + `unitPrice`
3. Validation rules per BR-PRICING-009 (DRAFT/PUBLISHED/ARCHIVED gate)
4. Submit → `PUT /api/v1/courses/{id}` → `CourseServiceImpl.updateCourse`
5. Persist update; outbox event `CoursePricingChangedEvent` (Phase 1.5+) notify enrolled parents

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned Phase 1.5+) Pricing changed sau khi PUBLISHED | `course-pricing-changed-parent` | Enrolled parents |
| (Planned Phase 1.5+) Free course trial expired | `course-trial-expired` | Student |

Phase 1 BETA: KHÔNG send email (avoid spam during beta).

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `kiteclass.course.pricing.default-model` | `PER_HOUR` | Default `pricingModel` cho new courses (per BR-PRICING-002) |
| `kiteclass.course.pricing.unit-price-max` | `100000000` | Max VND per unit (100M = 100 triệu, safeguard) |
| `kiteclass.course.pricing.allow-zero-non-free` | `false` | Reject `unitPrice = 0` khi `pricingModel != FREE` (BR-PRICING-006 enforce) |

### Database Schema

- `courses.pricing_model` VARCHAR(32) NOT NULL DEFAULT 'PER_HOUR' (V70 migration)
- `courses.unit_price` NUMERIC(19, 2) DEFAULT 0 (V67 migration)
- `courses.price` NUMERIC(15, 2) — LEGACY soft-deprecated per BR-PRICING-008

### Database Indexes

- `idx_courses_pricing_model` — Filter courses by pricing model (analytics)
- `idx_courses_status` — Existing (status filter)

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules trong file này derive từ: ADR-035 (LOCKED taxonomy decision), Wave beta-readiness-4 Bucket C external benchmark (Apollo/ILA/ACET published rate sheets), VN edu market norms.
- **Rationale:** Rule values reflect VN TT Anh ngữ + STEM dạy thêm market dominant model (PER_HOUR). Adding pricing model classes deferred until persona surfaces need (YAGNI). Detailed per-rule rationale backfilled during GAP-156 Phase 2.
- **Reviewer:** @nguyenvankiet (acting Product Owner + Tech Lead, solo-dev, 2026-05-25). Formal stakeholder + tax/legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3.
- **Compliance check:** **Considered (self-assessed, counsel pending GAP-156 AC-D)** — per `documents/00-brd/compliance-checklist.md` L3: **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023** (Luật 19/2023/QH15 — price-display transparency; VND inc/exc VAT must be unambiguous given `unitPrice` semantics BR-PRICING-003); **Luật Quản lý Thuế 2019** (invoice line-item structure derived from pricing model). Nghị định 123/2020/NĐ-CP governs e-invoice line semantics downstream (deferred GAP-185). No counsel verification of price-display / VAT-inclusion wording yet.
- **Review cadence:** Quarterly cho Phase 1 BETA (validate model fit), sau đó Annual. **Next review:** 2026-08-25. Event triggers: VN edu pricing regulation amendment, ≥5 tenant complaints về model coverage, analytics surface unused model class.

## Log

- **2026-05-25** Initial 3-layer business docs filed per GAP-738 (Wave beta-readiness-8 Bucket B). Closes Wave br-4 Bucket C code-doc sync gap (PR #1783 + #1800 ship code but skip 3-layer docs). Rules extracted từ `Course.java` (pricingModel/unitPrice fields) + `PricingModel.java` + `PricingCalculator.java` + ADR-035.
