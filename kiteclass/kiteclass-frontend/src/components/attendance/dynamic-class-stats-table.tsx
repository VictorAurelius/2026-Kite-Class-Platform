/**
 * Lazy `ClassStatsTable` wrapper.
 *
 * Per-class breakdown table renders many rows with sort handlers and progress
 * bars. Splitting keeps `/admin/attendance/stats` slim until the table is
 * actually mounted below the trends chart.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/skeleton';
import type { ClassAttendanceBreakdown } from '@/types/attendance';

interface DynamicClassStatsTableProps {
  data: ClassAttendanceBreakdown[];
  isLoading?: boolean;
  sortBy?: 'className' | 'attendanceRate' | 'totalSessions';
  sortOrder?: 'asc' | 'desc';
}

const LazyClassStatsTable = dynamic(
  () =>
    import('./class-stats-table').then((m) => ({
      default: m.ClassStatsTable,
    })),
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
);

export function DynamicClassStatsTable(props: DynamicClassStatsTableProps) {
  return <LazyClassStatsTable {...props} />;
}
