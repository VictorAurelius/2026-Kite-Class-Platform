'use client';

import { useEffect, useState, type ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import { useAuthStore } from '@/stores/auth-store';
import { useRouter, usePathname } from 'next/navigation';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { isPlatformAdmin } from '@/lib/auth-helpers';
import { clearTokens, clearLegacyLocalStorageTokens } from '@/lib/auth/jwt-storage';
import { BetaDisclaimerBanner } from '@/components/beta-disclaimer';
import { SkipToContent } from '@/components/a11y/SkipToContent';

/**
 * 2026-05-28: staff-invitations is per-tenant scope (Wave meta-6 canonical
 * impl in kiteclass-core). Owner role MUST be able to access `/admin/staff/*`
 * sub-routes to invite teachers. Other `/admin/*` routes remain
 * PLATFORM_ADMIN-only (instances, revenue, payments — system-level).
 */
function hasAdminLayoutAccess(role: string | undefined, pathname: string | null): boolean {
  if (isPlatformAdmin(role)) return true;
  if (role === 'OWNER' && pathname?.startsWith('/admin/staff')) return true;
  return false;
}

export function AdminLayout({ children }: { children: ReactNode }) {
  const { isAuthenticated, user, clearAuth } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();
  const [isHydrated, setIsHydrated] = useState(false);

  // Wait for zustand to hydrate from localStorage
  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    // GAP-518: accept PLATFORM_ADMIN (canonical) + legacy ADMIN role.
    // 2026-05-28: also allow OWNER for /admin/staff/* sub-routes (Wave meta-6
    // staff-invitations per-tenant scope).
    if (isHydrated && (!isAuthenticated || !hasAdminLayoutAccess(user?.role, pathname))) {
      router.replace('/login');
    }
  }, [isHydrated, isAuthenticated, user, pathname, router]);

  const handleLogout = () => {
    clearAuth();
    // GAP-599 Wave 92 Bucket B: sessionStorage-backed tokens + sweep legacy localStorage.
    clearTokens();
    clearLegacyLocalStorageTokens();
    router.push('/login');
  };

  // Show loading while hydrating or redirecting
  if (!isHydrated || !isAuthenticated || !hasAdminLayoutAccess(user?.role, pathname)) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen">
      {/* Skip to main content (accessibility — WCAG 2.4.1) */}
      <SkipToContent />
      <Sidebar variant="admin" />
      <div className="flex-1 flex flex-col">
        <header className="border-b h-16 flex items-center justify-between px-6">
          <h2 className="text-sm font-medium text-muted-foreground">Quản trị hệ thống</h2>
          <div className="flex items-center gap-4">
            <span className="text-sm text-muted-foreground">{user?.email}</span>
            <button
              onClick={handleLogout}
              className="text-sm text-muted-foreground hover:text-foreground"
            >
              Đăng xuất
            </button>
          </div>
        </header>
        <main id="main-content" role="main" className="flex-1 p-6">
          {/* Wave 98 Bucket B3 GAP-539 finishing stroke — admin dashboard banner.
              Dismissible via cookie (1y); see BetaDisclaimerBanner.tsx. */}
          <div className="mb-4">
            <BetaDisclaimerBanner />
          </div>
          {children}
        </main>
      </div>
    </div>
  );
}
