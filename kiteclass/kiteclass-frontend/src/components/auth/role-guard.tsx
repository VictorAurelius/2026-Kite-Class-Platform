/**
 * RoleGuard — route-group RBAC wrapper for KiteClass.
 *
 * Wave RBAC-Shell 1 Bucket A (GAP-1122). This is a SECURITY fix, not just UX:
 * before Bucket A every route group only checked "is authenticated", so any
 * logged-in user could navigate to any route (IDOR-by-navigation — e.g. a teacher
 * typing `/admin/payroll`). RoleGuard requires the actor's normalized role to be
 * in the route group's allow-list, else it bounces them to their own role-home.
 *
 * Consolidates the per-layout guard logic that was previously duplicated inline in
 * `(teacher)`, `(dashboard)/parent` and `(dashboard)/student` layouts.
 *
 * Works both standalone (top-level route group like `(teacher)`) and nested under
 * `(dashboard)` — it performs its own hydration + auth check, so the double-check
 * when nested is harmless.
 *
 * @author KiteClass Team
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { normalizeRole, roleHome, canAccess, type KcRole } from '@/lib/auth/roles';

export interface RoleGuardProps {
  /** Roles permitted to view the wrapped subtree. */
  allow: readonly KcRole[];
  children: React.ReactNode;
  /** Optional placeholder shown while resolving / when blocked (default: spinner). */
  fallback?: React.ReactNode;
}

/**
 * Gate the wrapped subtree behind a role allow-list.
 *
 * Decision flow (after Zustand hydration):
 *   1. not authenticated      -> redirect `/login`
 *   2. role not in `allow`     -> redirect to the actor's own role-home
 *      (unresolved role        -> `/login`, never guess)
 *   3. role in `allow`         -> render children
 */
export function RoleGuard({ allow, children, fallback }: RoleGuardProps) {
  const router = useRouter();
  const [isHydrated, setIsHydrated] = useState(false);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const accessToken = useAuthStore((s) => s.accessToken);
  const rawRole = useAuthStore((s) => s.user?.userType);

  const role = normalizeRole(rawRole);
  // Stable primitive dep so the redirect effect does not re-fire on each render
  // (the `allow` array prop is a fresh reference every render).
  const allowKey = allow.join(',');

  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    // Only decide AFTER hydration — Zustand restores from localStorage async,
    // so role/auth are not trustworthy on the first paint.
    if (!isHydrated) return;
    if (!isAuthenticated || !accessToken) {
      router.replace('/login');
      return;
    }
    if (!canAccess(role, allow)) {
      router.replace(role ? roleHome(role) : '/login');
    }
    // `allow` covered via allowKey; `role` derived from rawRole.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isHydrated, isAuthenticated, accessToken, role, allowKey, router]);

  const allowed =
    isHydrated && isAuthenticated && !!accessToken && canAccess(role, allow);

  if (!allowed) {
    return (
      <>
        {fallback ?? (
          <div className="flex min-h-screen items-center justify-center">
            <LoadingSpinner size="lg" />
          </div>
        )}
      </>
    );
  }

  return <>{children}</>;
}
