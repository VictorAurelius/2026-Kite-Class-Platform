'use client';

/**
 * RoleGuard — declarative role-based route guard for `(customer)/**` pages.
 *
 * GAP-562b (Wave 80 Bucket C): wrap Owner-only layouts (billing, branding,
 * settings/dangerzone, staff) so that authenticated STAFF users hitting the
 * URL bar bounce to a safe fallback (default `/dashboard`) instead of
 * silently rendering Owner content.
 *
 * <p>Rendering rules:</p>
 * <ul>
 *   <li>While the auth store hydrates ({@code isLoading=true}) → render
 *     {@code <LoadingSpinner />} so the redirect doesn't fire on a stale
 *     server-render snapshot.</li>
 *   <li>Role mismatch → schedule {@code router.replace(fallbackUrl)} and
 *     return {@code null} for the current paint (prevents flicker of the
 *     forbidden page).</li>
 *   <li>Role match → render {@code children}.</li>
 * </ul>
 *
 * <p>Server-side: this component is a Client Component (`'use client'`),
 * the underlying zustand store reads from localStorage, so server render
 * always treats the user as unauthenticated → safe default-deny.</p>
 */

import { type ReactNode, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { useRole, type PlatformRole } from '@/hooks/use-role';

export interface RoleGuardProps {
  /** Roles allowed to view children. Non-empty. */
  allowedRoles: readonly PlatformRole[];
  /** URL to redirect to on mismatch. Defaults to `/dashboard`. */
  fallbackUrl?: string;
  children: ReactNode;
}

export function RoleGuard({
  allowedRoles,
  fallbackUrl = '/dashboard',
  children,
}: RoleGuardProps) {
  const router = useRouter();
  const { role, isLoading, hasRole } = useRole();

  const allowed = !isLoading && hasRole(allowedRoles);

  useEffect(() => {
    // Defer redirect until hydration completes so SSR snapshot doesn't
    // bounce the user before zustand reads localStorage.
    if (!isLoading && !allowed) {
      router.replace(fallbackUrl);
    }
  }, [isLoading, allowed, fallbackUrl, router]);

  if (isLoading) {
    return (
      <div
        className="flex min-h-screen items-center justify-center"
        data-testid="role-guard-loading"
      >
        <LoadingSpinner />
      </div>
    );
  }

  if (!allowed) {
    // Render nothing during redirect to prevent forbidden-content flash.
    return (
      <div
        className="flex min-h-screen items-center justify-center"
        data-testid="role-guard-redirecting"
      >
        <LoadingSpinner />
      </div>
    );
  }

  // Suppress unused-var warning when only `role` reference is in JSX-less branch.
  void role;

  return <>{children}</>;
}
