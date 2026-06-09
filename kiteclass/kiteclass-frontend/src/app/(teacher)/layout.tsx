/**
 * Teacher route-group layout.
 *
 * Phase 1B v1 (Wave 18b2): minimal auth gate for the per-tiết attendance
 * route at `/attendance/period/[classId]/[periodNo]/[date]`.
 *
 * Wave 49 Bucket B (GAP-268): expanded into the GVCN + subject-teacher full
 * shell. The kc-teacher production port consolidates all teacher routes
 * under this `(teacher)/*` route group (canonical, replaces the partial
 * `(dashboard)/teacher/*` shape from Wave 18b2 bridging).
 *
 * Wave RBAC-Shell 1 Bucket A (GAP-1122): the inline hydration + TEACHER-only
 * persona guard (GAP-758) is replaced by the reusable {@link RoleGuard}. Same
 * behaviour — non-teacher (incl. undefined-userType per GAP-725) bounces to its
 * own role-home — but the logic now lives in ONE place shared with the parent /
 * student / admin route groups.
 *
 * @since 4.x.x (Wave 18b2 Bucket A); Wave 49 Bucket B GVCN shell; Wave RBAC-Shell 1 RoleGuard
 */

'use client';

import { RoleGuard } from '@/components/auth/role-guard';
import { TeacherShell } from '@/components/teacher/teacher-shell';
import { TEACHER_PROFILE } from '@/components/teacher/teacher-mock-data';
import { UserType } from '@/types/auth';

export default function TeacherLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <RoleGuard allow={[UserType.TEACHER]}>
      <TeacherShell
        teacherName={TEACHER_PROFILE.fullName}
        teacherSubtitle={`GVCN ${TEACHER_PROFILE.homeroomClass}`}
        teacherInitials={TEACHER_PROFILE.initials}
      >
        {children}
      </TeacherShell>
    </RoleGuard>
  );
}
