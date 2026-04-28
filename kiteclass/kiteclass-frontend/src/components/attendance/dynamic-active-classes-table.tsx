/**
 * Lazy `ActiveClassesTable` wrapper — keeps the table chrome (rows, status
 * pills, action buttons) out of `/attendance` initial First Load JS.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/skeleton';
import type { ActiveClassesTableProps } from './active-classes-table';

export type { ActiveClassesTableProps } from './active-classes-table';

const LazyActiveClassesTable = dynamic(
  () =>
    import('./active-classes-table').then((m) => ({
      default: m.ActiveClassesTable,
    })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-3">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    ),
  },
);

export function DynamicActiveClassesTable(props: ActiveClassesTableProps) {
  return <LazyActiveClassesTable {...props} />;
}
