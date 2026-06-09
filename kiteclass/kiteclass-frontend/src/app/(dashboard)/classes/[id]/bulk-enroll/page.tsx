/**
 * Bulk-enroll page (GAP-1104) — thin Next.js wrapper.
 *
 * Resolves the route params then renders {@link BulkEnrollPanel} inside the
 * dashboard layout. The wizard logic lives in the panel so it stays unit-testable
 * independent of `use(params)` (which is RTL-incompatible at the page level).
 *
 * @author KiteClass Team
 * @since 3.x (Wave KC enrollment)
 */

'use client';

export const dynamic = 'force-dynamic';

import { use } from 'react';
import { DashboardLayout } from '@/components/layout';
import { BulkEnrollPanel } from '@/components/enrollment/bulk-enroll-panel';

export default function BulkEnrollPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  return (
    <DashboardLayout>
      <BulkEnrollPanel classId={Number(id)} />
    </DashboardLayout>
  );
}
