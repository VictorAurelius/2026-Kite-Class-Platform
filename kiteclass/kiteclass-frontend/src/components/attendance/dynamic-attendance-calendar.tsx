/**
 * Lazy `AttendanceCalendar` wrapper.
 *
 * The calendar carries its own grid + day-cell rendering and is only meaningful
 * once a class is selected. Splitting it out keeps `/attendance/reports` and
 * any future attendance dashboards from shipping the calendar code in their
 * initial First Load JS.
 *
 * GAP-236 Sub-PR B (Wave) — code-split admin + attendance + billing routes.
 *
 * @author KiteClass Team
 */

'use client';

import dynamic from 'next/dynamic';
import { Skeleton } from '@/components/ui/skeleton';
import type { Attendance } from '@/types/attendance';

interface DynamicAttendanceCalendarProps {
  attendanceRecords: Attendance[];
  onDateClick?: (date: Date) => void;
}

const LazyAttendanceCalendar = dynamic(
  () => import('./attendance-calendar').then((m) => ({ default: m.AttendanceCalendar })),
  {
    ssr: false,
    loading: () => (
      <div className="space-y-3 rounded-lg border p-4">
        <Skeleton className="h-8 w-1/3" />
        <div className="grid grid-cols-7 gap-2">
          {Array.from({ length: 35 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      </div>
    ),
  },
);

export function DynamicAttendanceCalendar(props: DynamicAttendanceCalendarProps) {
  return <LazyAttendanceCalendar {...props} />;
}
