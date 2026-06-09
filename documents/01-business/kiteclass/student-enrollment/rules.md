# Student & Enrollment — Business Rules

**Domain:** KiteClass Core
**Version:** 1.0
**Updated:** 2026-03-24

---

## 1. Rules

### Student Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-STU-001 | Name required | Max 100 characters, not blank |
| BR-STU-002 | Email unique per tenant | Checked within same `instance_id` |
| BR-STU-003 | Phone unique globally | Vietnamese format, 10 digits starting with 0 |
| BR-STU-004 | Default status ACTIVE | New students start as ACTIVE |
| BR-STU-005 | Soft delete only | `deleted` flag, never hard delete |
| BR-STU-006 | Multi-tenant isolation | All queries filtered by `instance_id` |

**Student statuses:** PENDING, ACTIVE, INACTIVE, GRADUATED, DROPPED

### Enrollment Rules

| ID | Rule | Detail |
|----|------|--------|
| BR-ENROLL-001 | Class capacity check | Active enrollment count < class `maxStudents` |
| BR-ENROLL-002 | No duplicate enrollment | Same student + same class = rejected (unique constraint) |
| BR-ENROLL-003 | Auto-calculate final amount | `final_amount = tuition_amount * (1 - discount_percent / 100)` |
| BR-ENROLL-004 | Discount range 0-100 | `discount_percent` must be between 0.00 and 100.00 |
| BR-ENROLL-005 | Cannot enroll in ARCHIVED courses | Class's course must not be ARCHIVED |
| BR-ENROLL-006 | Default status PENDING_PAYMENT | New enrollments require payment confirmation |
| BR-ENROLL-007 | Single-enroll UI dialog | Admin/Teacher thêm 1 học sinh vào lớp qua dialog FE (GAP-1103); FE gọi `POST /api/v1/enrollments` — không thêm endpoint mới, tái dùng single-enroll. Lỗi 409 (đã ghi danh) / 400 (lớp đầy, discount) hiển thị qua toast |
| BR-ENROLL-008 | Bulk-enroll xlsx-only + giới hạn dòng | Ghi danh hàng loạt (GAP-1104) chỉ nhận file `.xlsx`; tối đa **1000 dòng/lần** (HTTP 413 nếu vượt) — mirror BR-BI-003 của student bulk-import |
| BR-ENROLL-009 | Bulk-enroll resolve học sinh + lớp theo human key | Mỗi dòng resolve học sinh theo `student_email` (ưu tiên) rồi `student_phone`, resolve lớp theo `class_code` — đều **tenant-scoped** (theo `X-Tenant-Id`); không tìm thấy → báo lỗi dòng |
| BR-ENROLL-010 | Bulk-enroll tái dùng validation single-enroll + skip-and-report | Mỗi dòng hợp lệ gọi lại `EnrollmentService.enrollStudent` (BR-ENROLL-001..005 áp dụng nguyên); dòng lỗi bị bỏ qua + báo cáo, dòng hợp lệ vẫn được ghi danh (transaction riêng từng dòng). Phát hiện trùng trong file (cùng học sinh + lớp) |

**Enrollment statuses:** PENDING_PAYMENT, ACTIVE, COMPLETED, WITHDRAWN

---

## 2. Flow

### Student Creation Flow
1. Validate name (required)
2. Check email uniqueness within tenant
3. Check phone uniqueness globally
4. Set `instance_id` for multi-tenant isolation
5. Save with default status ACTIVE
6. Cache invalidated on update/delete

### Enrollment Flow
1. Validate student exists and is not deleted
2. Validate class exists and is not deleted
3. Check duplicate enrollment (BR-ENROLL-002)
4. Check class capacity (BR-ENROLL-001)
5. Calculate `final_amount` via `@PrePersist` (BR-ENROLL-003)
6. Save enrollment with status PENDING_PAYMENT
7. Publish `EnrollmentCreatedEvent` (triggers invoice generation)

### Withdrawal Flow
1. Validate enrollment exists
2. Check enrollment is not already WITHDRAWN
3. Update status to WITHDRAWN

---

## 3. Emails

| Trigger | Template | Recipient |
|---------|----------|-----------|
| (Planned) Enrollment confirmation | enrollment-confirmation | Student email |
| (Planned) Payment reminder | payment-reminder | Student email |
| (Planned) Withdrawal notice | withdrawal-notice | Student email |

> Email templates are not yet implemented. Planned for future PRs.

---

## 4. Config

| Key | Default | Description |
|-----|---------|-------------|
| `student.cache.name` | `students` | Redis cache name for student data |
| `student.cache.key-generator` | `multiTenantKeyGenerator` | Tenant-aware cache key |
| `enrollment.default-discount` | `0` | Default discount percent |
| `enrollment.status.initial` | `PENDING_PAYMENT` | Initial enrollment status |

### Database Indexes
- `idx_students_email` — Student email lookup
- `idx_students_phone` — Student phone lookup
- `idx_students_status` — Filter by status
- `idx_enrollments_student_id` — Enrollments per student
- `idx_enrollments_class_id` — Enrollments per class
- `idx_enrollments_status` — Filter by enrollment status
- `uk_enrollments_student_class_instance` — Unique constraint (student + class + instance + deleted)

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật Giáo dục 2019 (enrollment obligations); Luật Trẻ em 2016 (under-16 parental consent); PDPL Decree 13/2023 Art 17.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: MoET enrollment regulation update, Luật Trẻ em amendment.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
