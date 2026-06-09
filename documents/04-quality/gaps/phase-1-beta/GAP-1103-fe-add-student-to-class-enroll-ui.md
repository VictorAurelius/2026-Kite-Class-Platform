# GAP-1103: FE "Thêm học sinh vào lớp" — single-enroll dialog UI

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-10 (Wave KC enrollment — single-enroll BE đã có nhưng không có UI ghi danh)
**Affects:** `kiteclass-frontend` — `lib/api/enrollments.ts`, `hooks/use-enrollments.ts`, `app/(dashboard)/classes/[id]/page.tsx`, `components/enrollment/add-student-to-class-dialog.tsx`

## Problem

BE single-enroll đã tồn tại đầy đủ: `EnrollmentController` `POST /api/v1/enrollments` + `EnrollmentService.enrollStudent` (capacity BR-ENROLL-001, duplicate BR-ENROLL-002, discount BR-ENROLL-004, auto finalAmount, status PENDING_PAYMENT, publish `EnrollmentCreatedEvent`). NHƯNG FE chỉ có read hooks (`useActiveEnrollmentsByClass` dùng trong attendance) — KHÔNG có UI nào để admin/teacher ghi danh học sinh vào lớp. Tính năng ghi danh không thể truy cập từ UI.

## Proposed Fix

Thêm `createEnrollment` API + `useCreateEnrollment` mutation (invalidate roster query on success) + dialog "Thêm học sinh vào lớp" trên trang chi tiết lớp (chọn học sinh search-filter + nhập học phí/discount/ghi chú), surface lỗi 409/400 từ BE qua toast (không bare-catch).

## Acceptance Criteria

- [x] `enrollmentsApi.createEnrollment(req)` → `POST /api/v1/enrollments`
- [x] `useCreateEnrollment` mutation invalidate enrollment query (roster/attendance refresh)
- [x] Dialog reuse shadcn Dialog/Select/Input/Textarea/Button; chọn học sinh hiện có + tuition + discount (0-100) + notes
- [x] Nút "Thêm học sinh vào lớp" trên `/classes/[id]` (hiển thị khi SCHEDULED / IN_PROGRESS)
- [x] Success toast + error toast surface BE message (409 đã ghi danh / 400 lớp đầy / discount)
- [x] FE test: dialog renders + submit gọi createEnrollment mocked + success toast (3 test PASS)
- [ ] Runtime-walk pending coordinator (browser: login → mở lớp → thêm học sinh → roster refresh + sad path 409)

## Related

- Discovered in: Wave KC enrollment build 2026-06-10
- Sister GAP-1104 (bulk-enroll xlsx)
- BE: `EnrollmentController` / `EnrollmentService` (UC-STU-05, UC-STU-09)
