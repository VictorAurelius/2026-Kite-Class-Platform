'use client';

/**
 * GAP-562b (Wave 80 Bucket C): /billing/* requires OWNER role.
 * STAFF users hitting any /billing URL bounce to /dashboard.
 */

import type { ReactNode } from 'react';
import { RoleGuard } from '@/components/RoleGuard';

export default function BillingLayout({ children }: { children: ReactNode }) {
  return <RoleGuard allowedRoles={['OWNER']}>{children}</RoleGuard>;
}
