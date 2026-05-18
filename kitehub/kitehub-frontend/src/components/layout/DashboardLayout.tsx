'use client';

import type { ReactNode } from 'react';
import { Sidebar } from './Sidebar';
import { useAuthStore } from '@/stores/auth-store';
import { useRouter } from 'next/navigation';
import { clearTokens, clearLegacyLocalStorageTokens } from '@/lib/auth/jwt-storage';
import { BetaDisclaimerBanner } from '@/components/beta-disclaimer';

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
        <main className="flex-1 p-6">
          {/* Wave 98 Bucket B3 GAP-539 finishing stroke — dashboard-wide beta banner.
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
