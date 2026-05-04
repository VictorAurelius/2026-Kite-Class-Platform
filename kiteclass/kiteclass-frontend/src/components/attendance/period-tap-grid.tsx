/**
 * PeriodTapGrid — mobile tap-grid for K-12 per-tiết attendance (Phase 1B v1).
 *
 * 4 status buttons per student (Có mặt / Có phép / Vắng / Trễ). Optimistic
 * local state lives in the parent route shell — this component is a
 * controlled grid that calls `onStatusChange` and renders `aria-pressed` on
 * the currently-selected status.
 *
 * Mapping to backend enums (from `attendance-period.ts` /
 * `documents/01-business/kiteclass/period-attendance/api-contract.md`):
 *
 * | UI label | Status enum |
 * | -------- | ----------- |
 * | Có mặt   | PRESENT     |
 * | Có phép  | EXCUSED     |
 * | Vắng     | ABSENT      |
 * | Trễ      | LATE        |
 *
 * `MAKEUP` (học bù) is intentionally not exposed in the v1 tap-grid — it is
 * a rare correction action and lives in the future detail dialog (UC-PERIOD-
 * ATT-W-002, deferred).
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

'use client';

import * as React from 'react';
import type { AttendancePeriodStatus } from '@/lib/api/attendance-period';
import { cn } from '@/lib/utils';

export interface PeriodTapGridStudent {
  studentId: number;
  fullName: string;
  /** Optional roll-call number to display next to the name. */
  rollNumber?: number;
}

interface ButtonSpec {
  status: AttendancePeriodStatus;
  label: string;
  shortLabel: string;
  className: string;
}

const STATUS_BUTTONS: readonly ButtonSpec[] = [
  {
    status: 'PRESENT',
    label: 'Có mặt',
    shortLabel: 'P',
    className:
      'data-[active=true]:bg-green-600 data-[active=true]:text-white border-green-300',
  },
  {
    status: 'EXCUSED',
    label: 'Có phép',
    shortLabel: 'CP',
    className:
      'data-[active=true]:bg-blue-600 data-[active=true]:text-white border-blue-300',
  },
  {
    status: 'ABSENT',
    label: 'Vắng',
    shortLabel: 'V',
    className:
      'data-[active=true]:bg-red-600 data-[active=true]:text-white border-red-300',
  },
  {
    status: 'LATE',
    label: 'Trễ',
    shortLabel: 'T',
    className:
      'data-[active=true]:bg-yellow-500 data-[active=true]:text-white border-yellow-300',
  },
];

export interface PeriodTapGridProps {
  /** Roster of students for this class+period. ≤42 per K-12 class. */
  students: readonly PeriodTapGridStudent[];
  /** Current status per studentId (optimistic local state). */
  statuses: Readonly<Record<number, AttendancePeriodStatus | undefined>>;
  /** Called when a teacher taps a status button for a student. */
  onStatusChange: (studentId: number, status: AttendancePeriodStatus) => void;
  /** Disables interaction (e.g. during in-flight save). */
  disabled?: boolean;
}

export function PeriodTapGrid({
  students,
  statuses,
  onStatusChange,
  disabled = false,
}: PeriodTapGridProps) {
  return (
    <ul
      role="list"
      data-testid="period-tap-grid"
      className="divide-y divide-border rounded-md border bg-card"
    >
      {students.map((student) => {
        const current = statuses[student.studentId];
        return (
          <li
            key={student.studentId}
            data-testid={`period-tap-row-${student.studentId}`}
            className="flex flex-col gap-2 p-3 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex min-w-0 items-center gap-3">
              {student.rollNumber !== undefined && (
                <span className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold tabular-nums text-muted-foreground">
                  {student.rollNumber}
                </span>
              )}
              <span className="truncate text-sm font-medium">
                {student.fullName}
              </span>
            </div>

            <div
              className="grid grid-cols-4 gap-1.5 sm:gap-2"
              role="group"
              aria-label={`Trạng thái điểm danh ${student.fullName}`}
            >
              {STATUS_BUTTONS.map((spec) => {
                const isActive = current === spec.status;
                return (
                  <button
                    key={spec.status}
                    type="button"
                    data-status={spec.status}
                    data-active={isActive}
                    aria-pressed={isActive}
                    aria-label={`${spec.label} - ${student.fullName}`}
                    disabled={disabled}
                    onClick={() => onStatusChange(student.studentId, spec.status)}
                    className={cn(
                      'inline-flex h-11 min-w-[3rem] items-center justify-center gap-1 rounded-md border bg-background px-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50',
                      'hover:bg-accent',
                      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                      spec.className,
                    )}
                  >
                    <span aria-hidden="true" className="text-xs sm:hidden">
                      {spec.shortLabel}
                    </span>
                    <span className="hidden sm:inline">{spec.label}</span>
                  </button>
                );
              })}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
