/**
 * Lazy `DataTable` wrapper.
 *
 * Pulls @tanstack/react-table out of every dashboard page bundle that lists
 * entities. The underlying `DataTable` ships in its own chunk and hydrates on
 * mount, which keeps the initial page payload small even when several pages
 * render the same component.
 *
 * GAP-127 Wave 7-Perf — code-splitting for table-heavy dashboard routes.
 *
 * @author KiteClass Team
 */

'use client';

import dynamic from 'next/dynamic';
import type { ColumnDef, PaginationState, SortingState } from '@tanstack/react-table';
import { Skeleton } from '@/components/ui/skeleton';

interface DataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[];
  data: TData[];
  pageCount?: number;
  pageSize?: number;
  onPaginationChange?: (pagination: PaginationState) => void;
  onSortingChange?: (sorting: SortingState) => void;
}

// Cast the dynamic component to preserve the generic API of the underlying
// `DataTable<TData, TValue>` — `next/dynamic` returns a non-generic component.
const LazyDataTable = dynamic(
  () => import('./data-table').then((m) => ({ default: m.DataTable })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-3">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    ),
  },
) as <TData, TValue>(props: DataTableProps<TData, TValue>) => React.ReactElement | null;

export const DataTable = LazyDataTable;
