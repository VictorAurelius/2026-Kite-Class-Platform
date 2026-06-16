# GAP-1425: Sĩ số lớp trên header không cập nhật sau khi ghi danh (thiếu invalidate class-detail query)

**Status:** 🟢 DONE
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-15 (KC-4 G2 browser re-walk — Playwright headless qua nip.io)
**Affects:** `kiteclass-frontend` — `useCreateEnrollment` (`src/hooks/use-enrollments.ts`) + class-detail page `(dashboard)/classes/[id]`

## Problem

Browser walk Bước 2 (ghi danh học sinh qua modal "Thêm học sinh vào lớp"): submit → **POST /api/v1/enrollments 201** + toast "Thành công — Đã thêm học sinh vào lớp" + DB row created + counter `current_enrolled` tăng đúng (verified 3 rows / counter=3). **NHƯNG** "Sĩ số" trên header trang class-detail vẫn hiển thị "2/15" (cũ) cho tới khi F5 reload.

Root cause: `useCreateEnrollment` onSuccess chỉ `invalidateQueries(['enrollments'])` (refresh roster list), KHÔNG invalidate `['classes', classId]` — query của `useClass(id)` cấp `current_enrolled` cho header. Mutation enrollment thay đổi `current_enrolled` của class nhưng không báo React Query → header stale.

Cosmetic, không block flow (enroll thật thành công + toast xác nhận), nhưng gây mild confusion ("đã thêm chưa?"). Phát hiện qua browser re-walk thật — API walk không thể thấy (chỉ FE render).

## Proposed Fix

Trong `useCreateEnrollment` onSuccess, thêm `queryClient.invalidateQueries({ queryKey: ['classes', variables.classId] })` (dùng `variables` từ mutation = `CreateEnrollmentRequest` có `classId`) → class-detail re-fetch → header sĩ số cập nhật ngay.

## Acceptance Criteria

- [x] Sau enroll qua UI → header "Sĩ số" cập nhật ngay (2/15 → 3/15) không cần F5.
- [x] Roster list vẫn refresh (invalidate `['enrollments']` giữ nguyên).
- [x] Browser re-walk verify: enroll → sĩ số 3/15 hiển thị live.

## Related

- Discovered in: KC-4 G2 browser re-walk 2026-06-15 (Playwright headless, goal "rewalk lại hết flow để dev làm G2 thuận lợi").
- Fix per `small-gap-inline-fix.md` (4 tiêu chí nhỏ pass) + `discovery-to-gap-inline-filing.md`.
