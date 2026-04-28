/**
 * Lazy `AttendanceFormList` wrapper for the class attendance page.
 *
 * The form list (and its row sub-tree) only renders once the user picks a
 * session. Lazy-loading defers the form-row + per-status select primitives
 * until they are needed — keeping the initial paint focused on the session
 * picker + stats cards.
 *
 * GAP-236 Sub-PR B Agent C — code-splitting heavy detail panels.
 *
 * @author KiteClass Team
 */

'use client';

import nextDynamic from 'next/dynamic';
import type { ComponentProps } from 'react';
import { Skeleton } from '@/components/ui/skeleton';
import type { AttendanceFormList as AttendanceFormListComponent } from '@/components/attendance/attendance-form-list';

const FormListSkeleton = () => (
  <div className="space-y-3">
    <Skeleton className="h-12 w-full" />
    <Skeleton className="h-12 w-full" />
    <Skeleton className="h-12 w-full" />
    <Skeleton className="h-12 w-full" />
  </div>
);

const LazyAttendanceFormList = nextDynamic(
  () =>
    import('@/components/attendance/attendance-form-list').then((m) => ({
      default: m.AttendanceFormList,
    })),
  {
    ssr: false,
    loading: FormListSkeleton,
  },
) as unknown as typeof AttendanceFormListComponent;

export const AttendanceFormList = LazyAttendanceFormList;
export type AttendanceFormListProps = ComponentProps<
  typeof AttendanceFormListComponent
>;
