/**
 * Lazy `EnhancedAttendanceCalendar` wrapper for student attendance page.
 *
 * The enhanced calendar (315 LOC) pulls `react-day-picker` + `date-fns` and
 * renders below-the-fold (after stats + filters). Lazy boundary keeps the
 * `/students/[id]/attendance` initial paint focused on header + stats.
 *
 * GAP-236 Sub-PR B Agent C — code-splitting heavy detail panels.
 *
 * @author KiteClass Team
 */

'use client';

import nextDynamic from 'next/dynamic';
import type { ComponentProps } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import type { EnhancedAttendanceCalendar as EnhancedAttendanceCalendarComponent } from '@/components/attendance/enhanced-attendance-calendar';

const CalendarSkeleton = () => (
  <div className="space-y-3 rounded-lg border p-6">
    <Skeleton className="h-6 w-32" />
    <Skeleton className="h-72 w-full" />
  </div>
);

const LazyEnhancedAttendanceCalendar = nextDynamic(
  () =>
    import('@/components/attendance/enhanced-attendance-calendar').then((m) => ({
      default: m.EnhancedAttendanceCalendar,
    })),
  {
    ssr: false,
    loading: CalendarSkeleton,
  },
) as unknown as typeof EnhancedAttendanceCalendarComponent;

export const EnhancedAttendanceCalendar = LazyEnhancedAttendanceCalendar;
export type EnhancedAttendanceCalendarProps = ComponentProps<
  typeof EnhancedAttendanceCalendarComponent
>;
