/**
 * (school-admin) route group layout — K-12 School Principal scope.
 *
 * Wave 50 Bucket A (GAP-271): kh-admin K-12 Principal port.
 *
 * Distinct from `(admin)/admin/**` (PLATFORM admin: beta-requests, instances,
 * payments, revenue) — same kh-frontend Next.js app but a different persona +
 * different sidebar/feature set. Both route groups coexist; auth middleware in
 * the existing app does not yet differentiate `SCHOOL_ADMIN` vs `ADMIN` roles.
 *
 * Auth strategy this PR:
 * - Reuse existing `useAuthStore` (auth gate redirects to /login if not authed)
 * - Accept any authenticated session (no `SCHOOL_ADMIN` role gate yet) so the
 *   port can ship without a backend role rollout. Once backend exposes
 *   `SCHOOL_ADMIN` role, tighten the check here (filed as follow-up sub-gap if
 *   needed).
 * - PLATFORM admin layout (`(admin)/admin/layout`) keeps its `user.role === 'ADMIN'`
 *   check unchanged → not affected by this port.
 *
 * @since Wave 50 Bucket A (Phase 4 Track 2 production port)
 */

'use client';

import { useEffect, useState, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { SchoolAdminShell } from '@/components/school-admin/school-admin-shell';

export default function SchoolAdminLayout({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    if (isHydrated && !isAuthenticated) {
      router.replace('/login');
    }
  }, [isHydrated, isAuthenticated, router]);

  if (!isHydrated || !isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  return <SchoolAdminShell>{children}</SchoolAdminShell>;
}
