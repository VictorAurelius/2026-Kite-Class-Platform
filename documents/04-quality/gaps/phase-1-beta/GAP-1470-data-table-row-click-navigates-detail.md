# GAP-1470: Data table — click cả dòng để mở chi tiết (UX enhancement)

**Status:** 🔵 OPEN (DEFERRED — "section sau làm" per user 2026-06-17)
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-17 (KC-3 walk — user UX question)
**Affects:** `kiteclass-frontend` data tables (bắt đầu từ class list `components/tables/columns/class-columns.tsx`; cân nhắc lan sang student/course tables)

## Problem

Trang danh sách lớp (`(dashboard)/classes/page.tsx`) hiện chỉ mở chi tiết lớp khi click **icon "Xem chi tiết" (👁)** trong cột "Thao tác" (`class-columns.tsx:115` `<Link href="/classes/{id}">`). User muốn **click cả dòng** → mở chi tiết (click target lớn hơn, trực giác hơn — pattern phổ biến Gmail/Linear/Notion/GitHub Issues/Jira/Airtable).

## Proposed Fix

Làm row clickable → navigate `/classes/{id}`, NHƯNG đúng cách (tránh bug):
- **`stopPropagation` trên action buttons** (👁/✏️/🗑) → click 🗑 KHÔNG vô tình mở detail (bug nguy hiểm: định xóa lại mở trang).
- **Giữ tên lớp là `<Link href>` thật** → Ctrl/middle-click mở tab mới + keyboard/screen-reader nav được (row `onClick` thuần mất 2 cái này).
- Row thêm `cursor-pointer` + hover highlight để báo hiệu clickable.
- Bỏ icon 👁 "Xem chi tiết" (thừa khi cả row clickable); giữ ✏️ sửa + 🗑 xóa.

**Consistency (cân nhắc scope):** nếu đổi bảng lớp → nên đổi đồng nhất các bảng data khác (Học viên, Khóa học...) — quyết định toàn UI, có thể tách shared pattern/component thay vì sửa từng bảng.

## Acceptance Criteria

- [ ] Click body dòng (ngoài action buttons) → mở `/classes/{id}` chi tiết.
- [ ] Click ✏️/🗑 KHÔNG trigger navigation (stopPropagation verified).
- [ ] Tên lớp vẫn là Link (Ctrl+click mở tab mới + accessibility giữ).
- [ ] (Nếu làm scope B) pattern áp dụng đồng nhất các bảng data khác.

## Related

- Discovered: phiên 2026-06-17 KC-3 walk (sau khi ship GAP-1468 session-management UI)
- `class-columns.tsx:112-132` (cột Thao tác hiện tại: 👁 xem + ✏️ sửa + 🗑 xóa)
- Decision: DEFER (option C) — user "để section sau làm"; không phải walk-blocker
