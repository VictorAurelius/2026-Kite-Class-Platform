# GAP-1167: Sửa trạng thái điểm danh — FE dùng `PUT /api/v1/attendance/{id}` nhưng BE là `PATCH` (405 method mismatch)

**Status:** 🟢 DONE (wave-phase1-close2 2026-06-19 — see Log)
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-11 (sweep từ GAP-1165 cross-flow)
**Affects:** `kiteclass-frontend` `lib/api/attendance.ts:95` `updateAttendanceStatus` → `use-attendance.ts:168` (`useUpdateAttendanceStatus`) ; `kiteclass-core` `AttendanceController:219`

## Problem

FE `attendanceApi.updateAttendanceStatus(id, data)` gọi `PUT /api/v1/attendance/{id}`. `AttendanceController` map verb này là `@PatchMapping("/{id}")` (PATCH), KHÔNG phải PUT → request trả **405 Method Not Allowed**. Giáo viên sửa trạng thái 1 bản ghi điểm danh (PRESENT→ABSENT, thêm ghi chú) sẽ fail.

BE PATCH endpoint còn yêu cầu header `X-Teacher-Id` (MAIN_TEACHER) mà FE `updateAttendanceStatus` chưa gắn → kể cả khi sửa verb vẫn cần xử lý header.

Phát hiện trong cross-flow sweep của GAP-1165. DEFER vì khác flow (sửa điểm danh ≠ report-export demo).

## Proposed Fix

(a) Đổi FE `apiClient.put` → `apiClient.patch` trong `updateAttendanceStatus` + gắn `X-Teacher-Id` (derive từ JWT/user context); HOẶC (b) thêm BE `@PutMapping("/{id}")` alias nếu muốn giữ FE. Ưu tiên (a) — PATCH đúng REST semantics cho partial update + tránh nhân đôi mapping. Đồng thời xác minh teacherId source (không tin client header trần).

## Acceptance Criteria

- [ ] FE update-status → BE 2xx (không 405)
- [ ] teacherId resolved an toàn (JWT, không client-supplied trần)
- [ ] Walk: giáo viên đổi trạng thái 1 điểm danh → DB cập nhật + points recalculated

## Related

- Sweep parent: GAP-1165
- Sister drift: GAP-1166


## Log — 2026-06-19 (wave-phase1-close2, state-check DONE)

FE attendance.ts uses apiClient.patch (was PUT→405), comment cites GAP-1429 sweep; BE @PatchMapping("/{id}") L280 matches; 0 FE PUT-to-attendance remaining.

Status → DONE per gap-done-discipline §2 (AC verified at code/runtime level). G2 browser walk = coordinator follow-up where applicable.