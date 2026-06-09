/**
 * Student PWA route group — auth + persona guard.
 *
 * Wave 49 Bucket C (Track 2 Phase 4 — GAP-269). Sits under `(dashboard)`,
 * inheriting the parent's auth check + ⌘K palette mount. Pages own their
 * mobile shell via {@link StudentMobileShell} so the desktop sidebar from
 * the surrounding `(dashboard)/layout.tsx` is bypassed for student routes.
 *
 * Wave RBAC-Shell 1 Bucket A (GAP-1122): the inline STUDENT-only check is
 * replaced by the shared {@link RoleGuard} (non-student bounces to its own
 * role-home). Student login itself is still gated by KC-9 — the route group
 * shell is scaffolded but the KC-native student auth path is not yet functional.
 *
 * @since Wave 49 Bucket C (GAP-269); Wave RBAC-Shell 1 RoleGuard
 */
'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { UserType } from '@/types/auth';

export default function StudentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <RoleGuard allow={[UserType.STUDENT]}>{children}</RoleGuard>;
}
