/**
 * Lazy `InvoiceDetailPanels` wrapper — splits items + adjustments + history
 * out of `/billing/[id]` initial chunk.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/skeleton';
import type { InvoiceDetailPanelsProps } from './invoice-detail-panels';

export type { InvoiceDetailPanelsProps } from './invoice-detail-panels';

const LazyInvoiceDetailPanels = dynamic(
  () =>
    import('./invoice-detail-panels').then((m) => ({
      default: m.InvoiceDetailPanels,
    })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-6">
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    ),
  },
);

export function DynamicInvoiceDetailPanels(props: InvoiceDetailPanelsProps) {
  return <LazyInvoiceDetailPanels {...props} />;
}
