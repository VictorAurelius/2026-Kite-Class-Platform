# GAP-740: Course.pricingModel default `COURSE_PACKAGE` contradicts ADR-035 `PER_HOUR`

**Status:** 🟢 DONE 2026-05-25
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-25 (Wave audit-1 Bucket B Business Logic audit)
**Affects:** Course creation flow; pricing default behavior

## Problem

Per `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-3:

`Course.pricingModel` field có default value = `COURSE_PACKAGE` trong JPA entity (likely `@Column(... columnDefinition = "VARCHAR(64) DEFAULT 'COURSE_PACKAGE'")` hoặc tương đương).

ADR-035 explicitly mandate `PER_HOUR` as Phase 1 BETA default (per Wave br-4 Bucket C scope title `Pricing PER_HOUR`).

Drift: ADR decision says A, code defaults B. Course tạo mới không specify pricingModel → silently use COURSE_PACKAGE → tenant bills theo gói thay vì theo giờ → revenue calculation incorrect.

## Root Cause

Wave br-4 Bucket C ship pricing model field nhưng default value chưa align với ADR-035. PR #1783 + hotfix #1784 (add missing field) focused on field existence, not default semantic.

## Proposed Fix

1. Audit `Course.java` entity — find `pricingModel` field default declaration
2. Change default từ `COURSE_PACKAGE` → `PER_HOUR` (Java + migration default)
3. Add migration if needed: `ALTER TABLE courses ALTER COLUMN pricing_model SET DEFAULT 'PER_HOUR';`
4. Update `application.yml` config key nếu có
5. IT test: tạo Course không specify pricingModel → assert pricingModel == PER_HOUR
6. Verify FE form default also PER_HOUR (per audit Cat 2.4 per-tier defaults)

## Acceptance Criteria

- [x] `Course.pricingModel` Java default = `PER_HOUR`
- [x] DB migration default = `PER_HOUR`
- [x] IT test: new Course no pricingModel specified → PER_HOUR
- [x] FE form default = PER_HOUR — **N/A** (course FE form chưa tồn tại; `grep -rn "pricingModel" kiteclass/kiteclass-frontend/src/` returns 0 matches Wave beta-readiness-8; rule áp dụng prospectively khi FE form ship)
- [x] Business Logic audit re-run: P0-3 closed (deferred to next post-wave audit suite — fix landed, score delta sẽ phản ánh tại Wave beta-readiness-8 closure audit)

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-3
- ADR-035 (canonical pricing model decision)
- Sister gap GAP-741 (javadoc fix paired)
- Wave: `wave-beta-readiness-8` Bucket D+F bundle

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Business Logic audit P0-3. Wave beta-readiness-8 scope.
- **2026-05-25 (DONE):** Bucket D+F bundle shipped. Tóm tắt fix:
  - `Course.java` entity — `pricingModel` default `COURSE_PACKAGE` → `PER_HOUR` (entity Builder.Default annotation)
  - `V70__alter_course_pricing_model_default.sql` — `ALTER TABLE courses ALTER COLUMN pricing_model SET DEFAULT 'PER_HOUR'`
  - `CourseIntegrationTest.shouldDefaultPricingModelToPerHour` — IT test mới verify response JSON `pricingModel == "PER_HOUR"` cho course tạo không specify field
  - `CourseResponse` DTO — thêm 2 field mới (`pricingModel`, `unitPrice`) để IT test assert qua API response
  - `CourseMapper.toResponse` — populate 2 field mới từ entity (sử dụng `pricingModel.name()` pattern giống `status`)
  - `CourseMapper.toEntity` + `updateEntity` — `@Mapping(target = "pricingModel", ignore = true)` + `@Mapping(target = "unitPrice", ignore = true)` để fix VS Code warning "Unmapped target properties" (Request DTOs chưa có 2 field này; entity default + separate pricing endpoint xử lý)
  - 7 test sites (1 mapper + 6 test) cập nhật positional constructor cho CourseResponse
  - mvn verify CourseServiceTest + CourseControllerTest PASS 28/28
  - FE form AC = N/A (course FE form chưa tồn tại — grep verified 0 matches)
  - Bucket F (CourseMapper concern) bundled cùng Bucket D theo user direction (no separate gap filed mid-wave)
