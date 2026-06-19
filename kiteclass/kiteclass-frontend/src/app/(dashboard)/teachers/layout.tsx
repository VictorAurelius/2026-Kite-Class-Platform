/**
 * Teachers route-group layout — RoleGuard for OWNER-level school-management.
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1130): SECURITY fix (IDOR-by-navigation).
 * Before this layout, `(dashboard)/teachers/*` only inherited the outer
 * `(dashboard)` auth check, so ANY authenticated user (teacher / parent /
 * student) could navigate straight to `/teachers` by typing the URL — the same
 * class of bug Bucket A (GAP-1122) closed for `/admin/*`.
 *
 * Staff management is OWNER-scoped (GAP-1130 AC #3): STAFF is blocked too — only
 * OWNER / ADMIN (principal) may manage teacher records. Everyone else is bounced
 * to their own role-home.
 *
 * @since Wave RBAC-Shell 1 Bucket B (GAP-1130)
 */
'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { UserType } from '@/types/auth';

export default function TeachersLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <RoleGuard allow={[UserType.OWNER, UserType.ADMIN]}>{children}</RoleGuard>
  );
}
