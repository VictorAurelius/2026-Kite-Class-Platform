/**
 * Reports route-group layout — RoleGuard for OWNER-level school-management.
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1130): SECURITY fix (IDOR-by-navigation).
 * Before this layout, `(dashboard)/reports/*` only inherited the outer
 * `(dashboard)` auth check, so ANY authenticated user (teacher / parent /
 * student) could navigate straight to `/reports` by typing the URL — the same
 * class of bug Bucket A (GAP-1122) closed for `/admin/*`.
 *
 * School-wide reporting is OWNER-scoped (GAP-1130 AC #3): STAFF is blocked too —
 * only OWNER / ADMIN (principal) may view aggregate reports. Everyone else is
 * bounced to their own role-home.
 *
 * @since Wave RBAC-Shell 1 Bucket B (GAP-1130)
 */
'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { UserType } from '@/types/auth';

export default function ReportsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <RoleGuard allow={[UserType.OWNER, UserType.ADMIN]}>{children}</RoleGuard>
  );
}
