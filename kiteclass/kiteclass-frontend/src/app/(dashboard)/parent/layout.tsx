/**
 * Parent portal route-group layout — persona guard cho /parent/* tree.
 *
 * Wave 758 — GAP-758: previously parent guard chỉ ở page.tsx root level
 * (`(dashboard)/parent/page.tsx`). Sibling routes `/parent/billing`,
 * `/parent/attendance`, `/parent/grades`, `/parent/settings` không có
 * persona guard → Owner JWT (per GAP-725 architectural — KH issues
 * `user.role` không có `user.userType` field) bypass guard via undefined
 * userType → page renders broken state.
 *
 * Fix: layout-level explicit PARENT-only REQUIRE bounces non-Parent
 * (including undefined-userType Owner JWT) back to /dashboard. Mirrors
 * sibling pattern in `(dashboard)/student/layout.tsx`.
 *
 * Long-term architectural fix: GAP-725 Phase 2 (KC Parent auth path
 * issuing JWT với `userType: PARENT` field).
 *
 * @since Wave 758 (GAP-758)
 */
'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';

export default function ParentLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const userType = useAuthStore((state) => state.user?.userType);

  useEffect(() => {
    // Explicit PARENT REQUIRE — undefined userType (Owner JWT) bounces too.
    if (userType !== UserType.PARENT) {
      router.replace('/dashboard');
    }
  }, [userType, router]);

  if (userType !== UserType.PARENT) {
    return null;
  }

  return <>{children}</>;
}
