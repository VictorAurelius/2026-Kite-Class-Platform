# GAP-292: Per-session pricing model (200K/buổi instead of fixed monthly)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — blocks P1 Solo Teacher financial workflow (AC-FIN-001 FAIL)
**Domain:** Backend (kiteclass-core/module/course + invoice + payment)
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher (per-session billing dominant); P2 Small Center (some classes per-session); secondary to GAP-185 VAT scope

## Problem

Theo AC-FIN-001, teacher PHẢI có thể track tuition theo per-session pricing (ví dụ: 200K/buổi × 8 buổi = 1.6M/tháng) — KHÔNG force monthly subscription pricing model.

Hiện trạng: `Course.java` chỉ có `price BigDecimal` (single price field, semantics ambiguous: per-course? per-month? per-session?). KHÔNG có pricingModel enum để switch billing logic.

**State-check (verified 2026-05-04):**
- `kiteclass-core/module/course/entity/Course.java`: `private BigDecimal price` (line ~chuyển nội dung không hiển thị nhưng grep `price` confirmed single field, no model enum)
- Grep `PerSession|per_session|pricingModel|PRICING_MODEL` ở `kiteclass-core` = 0 hits
- `module/invoice/` exists nhưng InstallmentPlan.java (5.3K) cho monthly fixed plans — không có per-session billing variant

## Root Cause

Course pricing thiết kế cho online course platform model (1 course = 1 price). Tutoring per-session pricing là use case khác chưa modeled. Solo persona dominant là per-session billing (cash + transfer).

## Proposed Fix

1. **Backend (kiteclass-core/module/course):**
   - Thêm `PricingModel` enum: `PER_SESSION` / `MONTHLY_SUBSCRIPTION` / `COURSE_PACKAGE` / `FREE`
   - Thêm field `pricing_model PricingModel` + `unit_price BigDecimal` (semantics depends on model)
   - Migration V60+ với default = `COURSE_PACKAGE` cho existing rows
2. **Invoice generation logic:**
   - PER_SESSION model: invoice = sum(attended_sessions × unit_price) trong period
   - MONTHLY: invoice = unit_price × N months
   - COURSE_PACKAGE: invoice = unit_price (1-shot)
3. **API contract:** add `pricingModel` to `CreateCourseRequest` + `CourseResponse`
4. **Business rules:** update `documents/01-business/kiteclass/course/rules.md` với BR-COURSE-PRICING-001..003 (5 attributes per `business-logic-review.md`)
5. **FE:** course settings form → radio "Loại học phí: Theo buổi / Theo tháng / Trọn gói khoá học"

## Acceptance Criteria

- [ ] PricingModel enum + field on Course entity
- [ ] Invoice generation respects pricing model
- [ ] Migration backwards-compatible (existing courses default = COURSE_PACKAGE)
- [ ] FE form supports model switch (only at course creation, not edit — preserves billing history)
- [ ] Unit tests cover all 4 models × invoice scenarios
- [ ] Business docs (3-layer) updated: rules.md + use-cases.md + api-contract.md
- [ ] Telemetry: track pricing model distribution per persona

## Related

- AC-FIN-001 (P1 review 2026-05-04)
- GAP-185 (Billing terms VAT TCT compliance) — broader scope; this gap is per-session pricing model only
- `.claude/rules/business-logic-review.md` — 5-attribute requirement for new business rule

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1.
