/**
 * Lazy `AttendanceDetailDialog` wrapper for student attendance page.
 *
 * The dialog only mounts when a date is clicked — no need to ship its bundle
 * with the initial page. The radix-dialog primitive + dialog body content
 * load on demand.
 *
 * GAP-236 Sub-PR B Agent C — code-splitting heavy detail panels.
 *
 * @author KiteClass Team
 */

'use client';

import nextDynamic from 'next/dynamic';
import type { ComponentProps } from 'react';
import type { AttendanceDetailDialog as AttendanceDetailDialogComponent } from '@/components/attendance/attendance-detail-dialog';

const LazyAttendanceDetailDialog = nextDynamic(
  () =>
    import('@/components/attendance/attendance-detail-dialog').then((m) => ({
      default: m.AttendanceDetailDialog,
    })),
  {
    ssr: false,
  },
) as unknown as typeof AttendanceDetailDialogComponent;

export const AttendanceDetailDialog = LazyAttendanceDetailDialog;
export type AttendanceDetailDialogProps = ComponentProps<
  typeof AttendanceDetailDialogComponent
>;
