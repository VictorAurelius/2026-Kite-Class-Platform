'use client';

/**
 * GAP-1100: /admin/staff/* relocated từ `(admin)` (platform-admin chrome) sang
 * `(customer)` group để render đúng customer layout (DashboardLayout). URL
 * `/admin/staff` giữ nguyên vì Next.js route group là URL-transparent.
 *
 * Authz parity: `(admin)/layout.tsx` AdminLayout dùng `hasAdminLayoutAccess`
 * cho phép OWNER + PLATFORM_ADMIN/ADMIN vào `/admin/staff*`. `(customer)`
 * DashboardLayout chỉ check `isAuthenticated` (không role), nên cần RoleGuard
 * này để giữ nguyên restriction — STAFF / non-OWNER bounce về /dashboard.
 * `useRole` map PLATFORM_ADMIN/ADMIN -> canonical OWNER (OWNER_ALIASES), nên
 * `allowedRoles={['OWNER']}` accept đúng tập như `hasAdminLayoutAccess`.
 * Backend `@PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")` trên
 * StaffInvitationController là lớp enforce chính (defense-in-depth).
 *
 * Theo pattern sẵn có: (customer)/{billing,branding,settings}/layout.tsx.
 */

import type { ReactNode } from 'react';
import { RoleGuard } from '@/components/RoleGuard';

export default function AdminStaffLayout({ children }: { children: ReactNode }) {
  return <RoleGuard allowedRoles={['OWNER']}>{children}</RoleGuard>;
}
