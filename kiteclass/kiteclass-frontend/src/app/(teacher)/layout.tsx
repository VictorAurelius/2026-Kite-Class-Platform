/**
 * Teacher route-group layout.
 *
 * Phase 1B v1 (Wave 18b2): minimal auth gate for the per-tiết attendance
 * route at `/attendance/period/[classId]/[periodNo]/[date]`.
 *
 * Wave 49 Bucket B (GAP-268): expanded into the GVCN + subject-teacher full
 * shell. The kc-teacher production port consolidates all teacher routes
 * under this `(teacher)/*` route group (canonical, replaces the partial
 * `(dashboard)/teacher/*` shape from Wave 18b2 bridging — see PR description
 * §Route consolidation decision). The shell ships the top-nav + identity
 * badge defined in the kiteclass-teacher Round-2 prototype.
 *
 * @since 4.x.x (Wave 18b2 Bucket A); Wave 49 Bucket B GVCN shell
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { TeacherShell } from '@/components/teacher/teacher-shell';
import { TEACHER_PROFILE } from '@/components/teacher/teacher-mock-data';
import { UserType } from '@/types/auth';

export default function TeacherLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isHydrated, setIsHydrated] = useState(false);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const accessToken = useAuthStore((state) => state.accessToken);
  const userType = useAuthStore((state) => state.user?.userType);

  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    if (!isHydrated) return;
    if (!isAuthenticated || !accessToken) {
      router.push('/login');
      return;
    }
    // GAP-758: explicit TEACHER-only persona require (REQUIRE not bypass).
    // Bounces undefined-userType (KH JWT Owner/Admin shape mismatch per GAP-725
    // architectural defer) + non-Teacher userType back to /dashboard.
    if (userType !== UserType.TEACHER) {
      router.replace('/dashboard');
    }
  }, [isHydrated, isAuthenticated, accessToken, userType, router]);

  if (!isHydrated || !isAuthenticated || !accessToken || userType !== UserType.TEACHER) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <TeacherShell
      teacherName={TEACHER_PROFILE.fullName}
      teacherSubtitle={`GVCN ${TEACHER_PROFILE.homeroomClass}`}
      teacherInitials={TEACHER_PROFILE.initials}
    >
      {children}
    </TeacherShell>
  );
}
