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
 * Auth strategy:
 * - Phase 1 BETA (Wave 758 GAP-758): route group HIDDEN — bounces all
 *   authenticated personas (OWNER / STAFF / ADMIN / PLATFORM_ADMIN) back to
 *   /dashboard. Reason: SCHOOL_ADMIN role chưa exist trong KH PlatformRole
 *   enum; K-12 SCHOOL_ADMIN persona deferred Phase 3 per CLAUDE.md
 *   "Phase 3 (+8-12 tuần post-counsel): + K-12 P5".
 * - Phase 3 K-12: re-enable gate cho SCHOOL_ADMIN role (file follow-up gap
 *   khi backend exposes role).
 * - PLATFORM admin layout (`(admin)/admin/layout`) keeps its
 *   `user.role === 'ADMIN'` check unchanged → not affected.
 *
 * @since Wave 50 Bucket A (Phase 4 Track 2 production port);
 *        Wave 758 GAP-758 (Phase 1 BETA bounce-all guard)
 */

'use client';

import { useEffect, useState, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';

export default function SchoolAdminLayout({ children: _children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuthStore();
  const router = useRouter();
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    if (!isHydrated) return;
    if (!isAuthenticated) {
      router.replace('/login');
      return;
    }
    // GAP-758: Phase 1 BETA persona-route-restrict — SCHOOL_ADMIN role không
    // tồn tại trong KH PlatformRole enum. Bounce all authenticated users
    // back to /dashboard. Re-enable Phase 3 K-12 khi role landed.
    router.replace('/dashboard');
  }, [isHydrated, isAuthenticated, router]);

  // Always render loading state — page never reaches children in Phase 1 BETA.
  return (
    <div className="flex min-h-screen items-center justify-center">
      <LoadingSpinner />
    </div>
  );
}
