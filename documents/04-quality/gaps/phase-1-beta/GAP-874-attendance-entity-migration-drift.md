# GAP-874: `attendance` entity ↔ migration drift — runtime column errors

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KiteClass attendance-grading)
**Affects:** `kiteclass-core` module `attendance`; entity `Attendance.java` vs `attendance` table (V1+V26+V63)

## Problem

`Attendance.java` map sang bộ cột hoàn toàn khác với migration `attendance`:

| Entity field | DB column status |
|---|---|
| `enrollment_id` NOT NULL | KHÔNG có cột (DB chỉ có `student_id`) |
| `marked_date` NOT NULL | DB có `marked_at` (tên khác) |
| `points_awarded` | KHÔNG có cột |
| `deleted` (BaseEntity) | KHÔNG có cột |
| UK `(enrollment_id, session_id, instance_id, deleted)` | DB UK `(session_id, student_id)` |

Chạy entity trên DB real sẽ lỗi `column enrollment_id/marked_date/points_awarded/deleted does not exist`. Khác với `grades` đã có V64 align — `attendance` chưa có migration tương đương.

## Proposed Fix

Ship migration mới tương tự V64 cho grades: add cột entity-side, deprecate cột legacy (nullable), align UK. Trước đó verify bằng RST walk luồng điểm danh trung tâm.

## Acceptance Criteria

- [ ] Migration V## add `enrollment_id`, `marked_date`, `points_awarded`, `deleted` columns
- [ ] UK realign khớp entity declaration
- [ ] Mockito/IT test verify Attendance.save() works on Postgres (not H2)
- [ ] Reference cluster doc 03-attendance-grading §A

## Discovered in

`documents/02-architecture/database/kiteclass/03-attendance-grading.md` §A
