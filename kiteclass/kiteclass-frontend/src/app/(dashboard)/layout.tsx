/**
 * Dashboard Layout with Auth Protection.
 * Redirects to login if not authenticated.
 *
 * @author KiteClass Team
 * @since 3.9.0
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/loading-spinner';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isHydrated, setIsHydrated] = useState(false);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const accessToken = useAuthStore((state) => state.accessToken);

  // Wait for Zustand hydration from localStorage
  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    // Only check auth AFTER hydration completes
    if (!isHydrated) return;

    // Check if user is authenticated
    if (!isAuthenticated || !accessToken) {
      // Redirect to login page
      router.push('/login');
    }
  }, [isHydrated, isAuthenticated, accessToken, router]);

  // Show loading while hydrating or checking auth
  if (!isHydrated || !isAuthenticated || !accessToken) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return <>{children}</>;
}
