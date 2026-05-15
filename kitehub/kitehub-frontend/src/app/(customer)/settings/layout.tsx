'use client';

/**
 * GAP-562b (Wave 80 Bucket C): /settings/* requires OWNER role for Wave 80 scope.
 *
 * <p>Per business rule split (GAP-562b §scope): general read could be OWNER+STAFF,
 * but Wave 80 ships full-OWNER default-deny first; OWNER+STAFF read split for
 * specific sub-resources lands Wave 81+. DangerZone (delete tenant / reset / transfer)
 * stays OWNER-only permanently.</p>
 *
 * <p>STAFF users hitting any /settings URL bounce to /dashboard.</p>
 */

import type { ReactNode } from 'react';
import { RoleGuard } from '@/components/RoleGuard';

export default function SettingsLayout({ children }: { children: ReactNode }) {
  return <RoleGuard allowedRoles={['OWNER']}>{children}</RoleGuard>;
}
