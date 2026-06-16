'use client';

import type { ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import { useAuthStore } from '@/stores/auth-store';
import { useRouter } from 'next/navigation';
import { clearTokens, clearLegacyLocalStorageTokens } from '@/lib/auth/jwt-storage';
import { OnboardingCoordinator } from '@/components/onboarding/OnboardingCoordinator';
import { SkipToContent } from '@/components/a11y/SkipToContent';

export function DashboardLayout({ children }: { children: ReactNode }) {
  const { isAuthenticated, user, clearAuth } = useAuthStore();
  const router = useRouter();

  const handleLogout = () => {
    clearAuth();
    // GAP-599 Wave 92 Bucket B: sessionStorage-backed tokens + sweep legacy localStorage.
    clearTokens();
    clearLegacyLocalStorageTokens();
    router.push('/login');
  };

  if (!isAuthenticated) {
    router.push('/login');
    return null;
  }

  return (
    <div className="flex min-h-screen">
      {/* Skip to main content (accessibility — WCAG 2.4.1) */}
      <SkipToContent />
      <div className="hidden md:block">
        <Sidebar variant="customer" />
      </div>
      <div className="flex-1 flex flex-col">
        <header className="border-b bg-background/95 backdrop-blur h-16 flex items-center justify-between px-6">
          <h2 className="text-sm font-medium text-muted-foreground">Quản lý trung tâm</h2>
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
          {/* GAP-1443 — OnboardingCoordinator orchestrates banner + SupportMenu
              (feedback/support/help floating entry) so owner has a support
              affordance. Replaces standalone BetaDisclaimerBanner (Wave 98 B3
              GAP-539); coordinator renders the same banner (dismissible cookie 1y)
              plus the `?` SupportMenu (GAP-656). */}
          <div className="mb-4">
            <OnboardingCoordinator />
          </div>
          {children}
        </main>
      </div>
    </div>
  );
}
