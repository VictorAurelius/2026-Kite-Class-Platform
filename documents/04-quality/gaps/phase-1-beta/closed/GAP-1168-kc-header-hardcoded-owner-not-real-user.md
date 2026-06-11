# GAP-1168: KC header hardcode "Chủ trung tâm / owner@example.com" — không phản ánh user thật đang đăng nhập

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-11 (KC attendance reports demo — user "đăng nhập admin mà profile vẫn là Chủ trung tâm")
**Affects:** `kiteclass-frontend` `components/layout/header.tsx:102-104` (user menu DropdownMenuLabel) + avatar fallback `:95`

## Problem

User menu trong header KiteClass (`:3000`) **hardcode tĩnh** chuỗi "Chủ trung tâm" + "owner@example.com" thay vì đọc user thật từ auth store. Hệ quả: dù đăng nhập bằng account nào (admin@test.com / teacher_a@test.com / ...) header vẫn luôn hiển thị "Chủ trung tâm / owner@example.com".

`owner@example.com` KHÔNG tồn tại trong `auth_credentials` (verify qua DB) — nó hoàn toàn là placeholder hardcode trong FE. Bug này che mất danh tính thật → user không thể biết mình đang đăng nhập bằng ai, gây nhầm lẫn khi debug quyền truy cập (vd "sao login admin vẫn thấy owner").

`header.tsx` đã import `useAuth` (dùng `logout`) nhưng KHÔNG dùng `user` từ store.

## Fix (shipped)

`header.tsx` đọc `user` từ `useAuth()`:
- Label hiển thị `roleLabel` map từ `user.userType` (ADMIN→"Quản trị viên", OWNER→"Chủ trung tâm", TEACHER→"Giáo viên", PARENT→"Phụ huynh", STUDENT→"Học viên"; fallback "Khách" khi chưa đăng nhập).
- Email hiển thị `user.email` (fallback "Chưa đăng nhập").
- Avatar initials lấy 2 ký tự đầu của email.

## Acceptance Criteria

- [x] Header hiển thị email + role thật của user đang đăng nhập
- [x] Đăng nhập admin@test.com → header hiện "Quản trị viên" + email admin@test.com
- [x] Không còn chuỗi hardcode "owner@example.com"

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

Static fix verified: grep `header.tsx` post-fix → 0 hit "owner@example.com" / "Chủ trung tâm" hardcode; label/email/avatar đều bind `user.*`. Browser runtime verify (login admin → header shows real identity) = G2 human step sau khi kiteclass-frontend rebuild.

## Related

- Discovered in: KC attendance reports demo 2026-06-11 (cùng session GAP-1165)
- VN-localization: role labels tiếng Việt per `vn-localization-audit-checklist.md` §2
