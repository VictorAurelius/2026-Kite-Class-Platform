/**
 * Student PWA route group — auth + persona guard.
 *
 * Wave 49 Bucket C (Track 2 Phase 4 — GAP-269). Sits under `(dashboard)`,
 * inheriting the parent's auth check + ⌘K palette mount. Pages own their
 * mobile shell via {@link StudentMobileShell} so the desktop sidebar from
 * the surrounding `(dashboard)/layout.tsx` is bypassed for student routes.
 *
 * Persona guard: only `STUDENT` users land here; `PARENT`/`TEACHER`/`ADMIN`
 * bounce back to `/dashboard` (mirrors the parent-portal guard pattern in
 * `(dashboard)/parent/page.tsx`). Non-authenticated users are already
 * redirected to `/login` by the outer dashboard layout.
 */
'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';

export default function StudentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const userType = useAuthStore((state) => state.user?.userType);

  useEffect(() => {
    // GAP-758: explicit STUDENT-only REQUIRE (not !== bypass). Bounces
    // undefined-userType (KH JWT shape mismatch per GAP-725) + non-Student
    // userType back to /dashboard.
    if (userType !== UserType.STUDENT) {
      router.replace('/dashboard');
    }
  }, [userType, router]);

  if (userType !== UserType.STUDENT) {
    return null;
  }

  return <>{children}</>;
}
