/**
 * Dashboard route-home layout — RoleGuard for the OWNER/STAFF landing page.
 *
 * Wave RBAC-Shell 1 Bucket B (GAP-1130): SECURITY fix (IDOR-by-navigation).
 * `app/dashboard` sits OUTSIDE the `(dashboard)` route group, so it never even
 * inherited that group's auth check — `app/dashboard/page.tsx` only rendered its
 * own chrome. ANY authenticated user (teacher / parent / student) could navigate
 * straight to `/dashboard` by typing the URL. RoleGuard now requires
 * OWNER / STAFF / ADMIN; everyone else is bounced to their own role-home
 * (which is also where `roleHome(OWNER|STAFF|ADMIN)` points).
 *
 * @since Wave RBAC-Shell 1 Bucket B (GAP-1130)
 */
'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { UserType } from '@/types/auth';

export default function DashboardHomeLayout({
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
