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
 * role-home).
 *
 * Wave rbac-lms-student-fe (GAP-1119/1113): KC-9 student-auth shipped
 * (`POST /api/v1/tenant-auth/login` → JWT role=STUDENT), so the KC-9 scaffold
 * banner is removed — the student shell + LMS player + assignment-submit are
 * now functional. Student login lands here via {@link RoleGuard} + roleHome.
 *
 * @since Wave 49 Bucket C (GAP-269); Wave RBAC-Shell 1 RoleGuard; Wave rbac-lms-student-fe (KC-9 ungated)
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
