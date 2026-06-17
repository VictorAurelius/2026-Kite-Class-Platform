# GAP-1468: KC-3 — UI quản lý buổi học sau khi tạo lớp (post-creation session management)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P2
**Domain:** Frontend
**Found:** 2026-06-17 (KC-3 G2 walk)
**Affects:** `kiteclass/kiteclass-frontend/src/app/(dashboard)/classes/[id]/page.tsx`

## Problem

Sau khi tạo lớp học, KHÔNG có affordance nào trên trang chi tiết lớp để tạo/quản lý buổi học (sessions). Panel chọn lịch lặp lại (recurrence) CHỈ tồn tại trong form **tạo lớp** (`courses/[id]/classes/new/page.tsx` — checkbox "Lặp lại theo lịch (tuần)" + `<RecurrenceForm>`). Nếu owner/teacher bỏ qua checkbox đó lúc tạo lớp, hoặc muốn thêm buổi học sau, thì trang chi tiết lớp (`classes/[id]/page.tsx`) chỉ có:
- Đổi lịch (Reschedule — dời cả lớp, không tạo buổi)
- Xem danh sách sessions (`useClassSessions`) + empty state

→ Thiếu hoàn toàn nút "tạo buổi học theo lịch" sau khi lớp đã tạo. Phát hiện trong G2 walk KC-3 (tạo lớp). Hạ tầng BE đã sẵn (`ClassController.generateFromRecurrence` + `POST /api/v1/classes/{id}/sessions/generate-from-recurrence`) và FE hook `useGenerateSessionsFromRecurrence` + component `RecurrenceForm` đã tồn tại — chỉ thiếu wire vào trang chi tiết.

## Proposed Fix

Thêm section "Quản lý lịch học / Buổi học" vào `classes/[id]/page.tsx`: tái dùng `<RecurrenceForm>` trong dialog (Card pattern theo convention reschedule/cancel hiện có) → submit gọi `useGenerateSessionsFromRecurrence` (hook đã có sẵn, invalidate sessions + classes query + toast) → list buổi học refresh. Nút CTA ở empty state + nút "Tạo lại buổi học theo lịch" ở header khi đã có sessions. Chỉ hiển thị khi lớp còn active (DRAFT/SCHEDULED/IN_PROGRESS).

## Acceptance Criteria

- [x] Trang chi tiết lớp có nút mở dialog `<RecurrenceForm>` tạo buổi học theo lịch tuần
- [x] Submit → `generateFromRecurrence` → invalidate sessions query → list refresh + toast
- [x] Empty state hiển thị CTA "Tạo buổi học theo lịch"; reschedule + các phần khác giữ nguyên
- [ ] Coordinator docker-rebuild kiteclass-frontend + human G2 re-walk xác minh live (per `feature-ship-runtime-walk-mandate.md` — chưa flip DONE)

## Follow-up

- **Specific-date individual session picking** (chọn/thêm từng ngày riêng lẻ, không theo recurrence tuần) NẰM NGOÀI scope GAP này — backend hiện chỉ support weekday WEEKLY recurrence (`generate-from-recurrence`), không có endpoint thêm 1 session đơn lẻ với ngày tùy ý. Cần BE support (`POST /api/v1/classes/{id}/sessions` single-session create) trước khi build UI picker. File gap riêng nếu cần.

## Related

- Discovered in: KC-3 G2 walk (Flow Verification Campaign), 2026-06-17
- Reuses: `useGenerateSessionsFromRecurrence` (GAP-290 Wave 18a), `RecurrenceForm` component
- Note: hook `useGenerateFromRecurrence` đã tồn tại dưới tên `useGenerateSessionsFromRecurrence` trong `use-classes.ts` — tái dùng, không tạo mới
