'use client';

/**
 * GAP-562b (Wave 80 Bucket C): /branding/* requires OWNER role.
 * STAFF users hitting any /branding URL bounce to /dashboard.
 */

import type { ReactNode } from 'react';
import { RoleGuard } from '@/components/RoleGuard';

export default function BrandingLayout({ children }: { children: ReactNode }) {
  return <RoleGuard allowedRoles={['OWNER']}>{children}</RoleGuard>;
}
