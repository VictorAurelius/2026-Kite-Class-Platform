/**
 * Lazy `AttendanceTrendsChart` wrapper.
 *
 * Trends rendering is heavyweight (per-day SVG path generation, memoized
 * geometry) and only matters on the admin stats route. Splitting it keeps
 * `/admin/attendance/stats` initial JS smaller while still allowing
 * SSR-disabled hydration once the chart container mounts.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/skeleton';
import type { AttendanceTrendPoint } from '@/types/attendance';

interface DynamicAttendanceTrendsChartProps {
  data: AttendanceTrendPoint[];
  height?: number;
  showGrid?: boolean;
}

const LazyAttendanceTrendsChart = dynamic(
  () =>
    import('./attendance-trends-chart').then((m) => ({
      default: m.AttendanceTrendsChart,
    })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-3 rounded-lg border p-4">
        <Skeleton className="h-6 w-1/4" />
        <Skeleton className="h-[300px] w-full" />
      </div>
    ),
  },
);

export function DynamicAttendanceTrendsChart(props: DynamicAttendanceTrendsChartProps) {
  return <LazyAttendanceTrendsChart {...props} />;
}
