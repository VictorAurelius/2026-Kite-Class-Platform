/**
 * Admin route-group layout — role guard for privileged admin tooling.
 *
 * Wave RBAC-Shell 1 Bucket A (GAP-1122): SECURITY fix. Before this layout, the
 * `(dashboard)/admin/*` tree (payroll, staff vetting, bulk-import, attendance
 * overrides) only inherited the outer `(dashboard)` auth check — ANY authenticated
 * user (teacher / parent / student) could navigate straight to `/admin/payroll`
 * (IDOR-by-navigation). RoleGuard now requires OWNER or ADMIN; everyone else is
 * bounced to their own role-home.
 *
 * @since Wave RBAC-Shell 1 Bucket A (GAP-1122)
 */
'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { UserType } from '@/types/auth';

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <RoleGuard allow={[UserType.OWNER, UserType.ADMIN]}>{children}</RoleGuard>;
}
