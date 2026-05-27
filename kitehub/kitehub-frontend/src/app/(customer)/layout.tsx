'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { restorePersistedTokens } from '@/lib/auth/jwt-storage';
import {
  CommandPalette,
  KH_DASHBOARD_COMMANDS,
  useCommandPalette,
} from '@/_shared/dashboard-foundation';

export default function Layout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [isHydrated, setIsHydrated] = useState(false);

  // Wave 31 Bucket A — ⌘K command palette wired at customer-layout level so
  // every (customer) page can trigger it via Cmd/Ctrl+K. Foundation
  // primitive imported from `_shared/dashboard-foundation` (Decision B —
  // duplicated from KC Wave 30; see `_shared/dashboard-foundation/types.ts`).
  // Root `<ThemeProvider>` (next-themes) already mounted in app/layout.tsx,
  // so dashboard-foundation's `useDashboardTheme()` hook works without an
  // additional wrapper here.
  const palette = useCommandPalette();

  // Wait for zustand to hydrate from localStorage.
  // 2026-05-28: also restore remember-me tokens (localStorage → sessionStorage)
  // before isAuthenticated check, so AuthInterceptor finds tokens in
  // sessionStorage after browser-close → reopen.
  useEffect(() => {
    restorePersistedTokens();
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    if (isHydrated && !isAuthenticated) {
      router.replace('/login');
    }
  }, [isHydrated, isAuthenticated, router]);

  // Show loading while hydrating or redirecting
  if (!isHydrated || !isAuthenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <>
      <DashboardLayout>{children}</DashboardLayout>
      <CommandPalette
        open={palette.open}
        onOpenChange={palette.setOpen}
        commands={KH_DASHBOARD_COMMANDS}
        placeholder="Tìm trang, lệnh, hoặc gõ ⌘K..."
      />
    </>
  );
}
