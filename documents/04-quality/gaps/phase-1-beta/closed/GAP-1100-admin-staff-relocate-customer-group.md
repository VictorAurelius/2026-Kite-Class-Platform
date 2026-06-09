# GAP-1100: Relocate `/admin/staff` ra khỏi `(admin)` platform-admin group sang `(customer)` group

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Frontend
**Found:** 2026-06-09 (KiteHub `kitehub-frontend` :3001 — route-group UX review)
**Affects:** `kitehub/kitehub-frontend/src/app/(admin)/admin/staff/**` → `(customer)/admin/staff/**`

## Problem

Route `/admin/staff` + `/admin/staff/invite` (quản lý lời mời nhân viên — OWNER của
trung tâm, theo `TENANT_OWNER` ADR-003) nằm trong route group `(admin)`, nên render
qua `(admin)/layout.tsx` → `AdminLayout` = chrome dành cho **platform-admin**
(sidebar Beta/Instances/Payments/Revenue + nav-link không liên quan tới OWNER).

Đây **KHÔNG** phải lỗ hổng bảo mật: `hasAdminLayoutAccess` trong `AdminLayout` chỉ
cho OWNER (+ PLATFORM_ADMIN/ADMIN) vào `/admin/staff*`, và backend
`StaffInvitationController` enforce `@PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")`
trên cả 4 endpoint (list/create/resend/revoke). Vấn đề là **UX sai**: một OWNER là
khách hàng lại thấy giao diện quản trị hệ thống + các nav-link chết.

Sidebar customer (`Sidebar.tsx` line 31, `testId: customer-nav-staff`,
`requiresRole: ['OWNER']`) đã trỏ `/admin/staff` như một tính năng của OWNER khách
hàng — chỉ có route GROUP bị đặt sai chỗ.

## Proposed Fix

`git mv` cây thư mục `(admin)/admin/staff` sang `(customer)/admin/staff` (Next.js route
group có dấu ngoặc là URL-transparent → URL `/admin/staff` + `/admin/staff/invite`
giữ nguyên). Thêm `(customer)/admin/staff/layout.tsx` bọc `<RoleGuard allowedRoles={['OWNER']}>`
để giữ nguyên authz: `(customer)` `DashboardLayout` chỉ check `isAuthenticated` (không
role), nên RoleGuard cần thiết để STAFF / non-OWNER bị bounce về `/dashboard` —
`useRole` map PLATFORM_ADMIN/ADMIN → canonical OWNER nên parity với
`hasAdminLayoutAccess` được bảo toàn. Theo đúng pattern sẵn có
`(customer)/{billing,branding,settings}/layout.tsx`.

## Acceptance Criteria

- [x] `/admin/staff` + `/admin/staff/invite` render qua customer layout (DashboardLayout), không còn admin chrome
- [x] URL `/admin/staff` không đổi (route group URL-transparent)
- [x] Authz parity: OWNER + PLATFORM_ADMIN pass; STAFF / non-OWNER bị chặn (RoleGuard → `/dashboard`); backend `@PreAuthorize` không đổi
- [x] `pnpm --filter kitehub-frontend build` exit 0
- [x] Không có import nào vỡ (các file staff dùng absolute `@/` import — không phải relative)

## Related

- Discovered in: branch `agent/gap-1100-admin-staff-relocate`
- Pattern reference: `(customer)/billing/layout.tsx` + `RoleGuard.tsx` (GAP-562b Wave 80 Bucket C)
- Backend authz: `kitehub-subscription` `StaffInvitationController` `@PreAuthorize` `OWNER_AUTHZ`
- Sister: `AdminLayout.tsx` `hasAdminLayoutAccess` OWNER branch giờ là dead code cho `/admin/staff` (no route under `(admin)` matches) — cleanup nhỏ có thể làm sau, không trong scope gap này.
