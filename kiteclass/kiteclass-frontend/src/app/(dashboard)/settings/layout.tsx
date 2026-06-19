/**
 * Settings route-group layout — RoleGuard for OWNER/STAFF school-management.
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1130): SECURITY fix (IDOR-by-navigation).
 * Before this layout, `(dashboard)/settings/*` only inherited the outer
 * `(dashboard)` auth check, so ANY authenticated user (teacher / parent /
 * student) could navigate straight to `/settings` by typing the URL — the same
 * class of bug Bucket A (GAP-1122) closed for `/admin/*`. RoleGuard now requires
 * OWNER / STAFF / ADMIN; everyone else is bounced to their own role-home.
 * (Parent/student settings live under their own guarded subtrees.)
 *
 * @since Wave RBAC-Shell 1 Bucket B (GAP-1130)
 */
'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { UserType } from '@/types/auth';

export default function SettingsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <RoleGuard allow={[UserType.OWNER, UserType.STAFF, UserType.ADMIN]}>
      {children}
    </RoleGuard>
  );
}
