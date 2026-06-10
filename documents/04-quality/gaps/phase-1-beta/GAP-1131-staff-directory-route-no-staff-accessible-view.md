# GAP-1131: STAFF "staff directory" thiếu route truy cập được cho role STAFF

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-10 (Wave RBAC-Shell 1 Bucket B — per-role shell implementation)
**Affects:** `kiteclass-frontend` STAFF nav + `(dashboard)/admin/staff` (chưa tồn tại / guarded)

## Problem

Bảng role GAP-1119 mô tả STAFF bundle = "enrollment + attendance + invoice + **staff**". Nhưng route quản lý nhân viên hiện chỉ nằm dưới `(dashboard)/admin/*` (guard `RoleGuard allow={[OWNER, ADMIN]}` từ Bucket A) — STAFF KHÔNG truy cập được. Nếu thêm mục "Nhân viên" vào sidebar STAFF trỏ tới `/admin/staff` thì click sẽ bị RoleGuard bounce → nav gãy.

Bucket B vì vậy **bỏ mục "staff" khỏi nav STAFF** (chỉ giữ Tổng quan / Học viên / Lớp học / Điểm danh / Học phí / Cài đặt) để tránh nav gãy — nhưng như vậy lệch với bundle GAP-1119.

## Proposed Fix

Quyết định 1 trong 2:
- (a) Dựng route staff-directory STAFF-accessible (read-only danh sách đồng nghiệp) ngoài `/admin/*` guard, rồi thêm vào nav STAFF; HOẶC
- (b) Chốt beta STAFF bundle KHÔNG gồm staff-directory (chỉ owner/admin xem nhân viên) → cập nhật `role-hierarchy/rules.md` + GAP-1119 bảng role cho khớp.

## Acceptance Criteria

- [ ] Quyết định (a) hoặc (b) ghi vào `role-hierarchy/rules.md`
- [ ] Nếu (a): STAFF có nav "Nhân viên" trỏ route truy cập được, không bị bounce
- [ ] Nếu (b): GAP-1119 bảng role STAFF cập nhật bỏ "staff"

## Related

- Discovered in: Wave RBAC-Shell 1 Bucket B (branch `wave/rbac-shell-1-b-shell`)
- GAP-1119 (umbrella — bảng role STAFF bundle)
- GAP-1122 (Bucket A — admin RoleGuard OWNER/ADMIN)
