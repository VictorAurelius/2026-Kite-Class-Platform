# GAP-1130: Owner/staff school-management dashboard routes thiếu RoleGuard (IDOR-by-navigation)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-10 (Wave RBAC-Shell 1 Bucket B — per-role shell implementation)
**Affects:** `kiteclass-frontend` `(dashboard)/{courses,teachers,students,classes,billing,reports,branding,settings}/*` + `app/dashboard/page.tsx`

## Problem

Bucket A (GAP-1122) chỉ gắn `RoleGuard` ở các leaf layout có scope nhạy cảm rõ ràng: `(dashboard)/admin/*` (OWNER/ADMIN), `(teacher)`, `(dashboard)/parent`, `(dashboard)/student`. Các route "school-management" còn lại của OWNER/STAFF — `/courses`, `/teachers`, `/students`, `/classes`, `/billing`, `/reports`, `/branding`, `/settings` — chỉ kế thừa auth-check ở `(dashboard)/layout.tsx`, KHÔNG có role-guard. `app/dashboard/page.tsx` (route-home, nằm NGOÀI route group `(dashboard)`) cũng chỉ có chrome `DashboardLayout`, không guard.

→ Một user đã đăng nhập với role TEACHER/PARENT/STUDENT có thể gõ URL thẳng tới `/courses` hoặc `/teachers` và xem được (IDOR-by-navigation) — cùng lớp lỗi mà Bucket A đã đóng cho `/admin/payroll`.

**Vì sao không guard ngay ở group root:** route group `(dashboard)` được CHIA SẺ với cây con `/parent` + `/student`. Đặt `RoleGuard allow={[OWNER,STAFF,ADMIN]}` ở `(dashboard)/layout.tsx` sẽ chặn PARENT/STUDENT khỏi chính shell của họ (outer guard bounce trước khi inner leaf guard render). Đây là lý do Bucket A guard ở leaf thay vì root.

## Proposed Fix

Gom các route school-management của OWNER/STAFF vào 1 route subgroup riêng (vd `(dashboard)/(school-mgmt)/...`) có 1 layout `RoleGuard allow={[OWNER, STAFF, ADMIN]}`, hoặc gắn `RoleGuard` per-route. Guard cả `app/dashboard` (thêm `app/dashboard/layout.tsx` với RoleGuard). Cân nhắc tách quyền OWNER-only (teachers/branding/reports) vs STAFF-shared (students/classes/attendance/billing) ở tầng guard, không chỉ ở nav display.

## Acceptance Criteria

- [ ] TEACHER/PARENT/STUDENT gõ URL `/courses` `/teachers` `/dashboard` → bị bounce về role-home (không xem được)
- [ ] OWNER/STAFF vẫn vào được các route trong quyền của mình
- [ ] OWNER-only routes (teachers/branding/reports/payroll/roles) chặn STAFF

## Related

- Discovered in: Wave RBAC-Shell 1 Bucket B (branch `wave/rbac-shell-1-b-shell`)
- GAP-1122 (Bucket A RoleGuard foundation — leaf-level guards)
- GAP-1119 (umbrella — IDOR-by-navigation noted as Risk #3 / P1)
