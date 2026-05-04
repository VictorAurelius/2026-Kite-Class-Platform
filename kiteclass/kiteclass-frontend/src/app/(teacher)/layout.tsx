/**
 * Teacher route-group layout (Phase 1B v1).
 *
 * Mirrors the auth gate from the (dashboard) layout. Phase 1B v1 only ships
 * the per-tiết attendance route at /attendance/period/[classId]/[periodNo]/
 * [date]; richer GVCN navigation shell is Phase 1B follow-up scope.
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/common/loading-spinner';

export default function TeacherLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isHydrated, setIsHydrated] = useState(false);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const accessToken = useAuthStore((state) => state.accessToken);

  useEffect(() => {
    setIsHydrated(true);
  }, []);

  useEffect(() => {
    if (!isHydrated) return;
    if (!isAuthenticated || !accessToken) {
      router.push('/login');
    }
  }, [isHydrated, isAuthenticated, accessToken, router]);

  if (!isHydrated || !isAuthenticated || !accessToken) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return <main className="min-h-screen bg-background">{children}</main>;
}
