/**
 * Parent portal route-group layout — persona guard cho /parent/* tree.
 *
 * Wave 758 — GAP-758: parent guard moved to layout level so sibling routes
 * (`/parent/billing`, `/parent/attendance`, `/parent/grades`, `/parent/settings`)
 * are all guarded, not just `/parent` root.
 *
 * Wave RBAC-Shell 1 Bucket A (GAP-1122): the inline PARENT-only check is replaced
 * by the shared {@link RoleGuard}. A non-parent (incl. undefined-userType Owner
 * JWT per GAP-725) now bounces to its OWN role-home instead of a hardcoded
 * `/dashboard`. Long-term auth fix: GAP-725 Phase 2.
 *
 * @since Wave 758 (GAP-758); Wave RBAC-Shell 1 RoleGuard
 */
'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { UserType } from '@/types/auth';

export default function ParentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <RoleGuard allow={[UserType.PARENT]}>{children}</RoleGuard>;
}
