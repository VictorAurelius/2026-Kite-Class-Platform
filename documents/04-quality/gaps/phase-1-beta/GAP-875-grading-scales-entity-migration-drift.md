# GAP-875: `grading_scales` entity ↔ migration drift — không cột nào trùng tên

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KiteClass attendance-grading)
**Affects:** `kiteclass-core` module grading; entity `GradingScale.java` vs `grading_scales` table

## Problem

Entity `GradingScale.java` map: `scale_name`, `letter_grade`, `min_score`, `max_score`, `gpa_value`, `is_default`, `is_passing` + BaseEntity `deleted`/`updated_at`/`updated_by`.

Migration `grading_scales` có: `grade`, `min_percentage`, `max_percentage`, `gpa`, `description` + `created_at`/`created_by`/`updated_by`/`version`.

→ **KHÔNG có một cột nghiệp vụ nào trùng tên** giữa entity và DB. Entity còn thiếu `deleted`/`updated_at`/`is_default`/`is_passing` trong DB. Drift "câm" — chưa có migration align. Rủi ro cao tương tự `attendance` (GAP-874).

## Proposed Fix

Migration mới align entity column names + add missing entity fields. Document deprecation cho cột legacy DB. Verify tầng service đang dùng entity nào (entity field vs raw SQL theo migration name).

## Acceptance Criteria

- [ ] Migration V## rename/add cột khớp `GradingScale.java`
- [ ] Entity load + save verify Postgres
- [ ] Reference cluster doc 03-attendance-grading §C

## Discovered in

`documents/02-architecture/database/kiteclass/03-attendance-grading.md` §C
