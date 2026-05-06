'use client';

/**
 * AttendanceCalendar — G8 component (Wave 28 Bucket C).
 *
 * Teacher month-view of class attendance plus a 30-day rolling streak
 * indicator. Composes a 5×7 grid (Mon-first) with status-coloured cells +
 * Vietnamese legend.
 *
 * Design source:
 *   documents/02-architecture/design-system/ui_kits/components/G8-attendance-calendar/README.md
 *   documents/02-architecture/design-system/ui_kits/components/G8-attendance-calendar/states/*.html
 *
 * Composition:
 *   <AttendanceCalendar>
 *     ├── header  (Tháng MM/YYYY + optional streak chip)
 *     ├── weekday-row   (T2 T3 T4 T5 T6 T7 CN — Vietnamese, Mon-first)
 *     ├── grid          (pad cells + day buttons)
 *     └── legend        (4 status swatches + labels)
 *
 * The component is **controlled** — parent owns `month`, `selectedDay`, and
 * (when editable) commits status changes via `onCycleStatus`. Mirrors the
 * controlled shape of <AttendanceRoster> in this package.
 *
 * Accessibility (WCAG AA):
 *  - Day cells are real <button>s with `aria-label="Ngày DD/MM, <status>"`
 *  - Selected day exposes `aria-pressed="true"` + 2px primary ring
 *  - Weekday header uses role="row" + role="columnheader"
 *  - Color is never the only signal — each cell pairs glyph + sr-only label
 *  - Arrow keys navigate between day buttons (right/left, up/down jump 7)
 *  - Editable mode: space-bar cycles PRESENT → ABSENT → LATE → EXCUSED → PRESENT
 *  - prefers-reduced-motion respected (no glow / no pulse — opacity only)
 *
 * Vietnamese-first per CLAUDE.md.
 */

import type React from 'react';
import { useCallback, useId } from 'react';
import type {
  AttendanceCalendarProps,
  AttendanceDayStatus,
  CalendarDay,
} from './types';

const COPY_VI = {
  weekdays: ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'] as const,
  monthLabel: (month: number, year: number): string =>
    `Tháng ${month}/${year}`,
  legendTitle: 'Chú thích',
  streak: (n: number): string => `Chuỗi ${n} ngày`,
  status: {
    PRESENT: 'Có mặt',
    ABSENT: 'Vắng không phép',
    LATE: 'Đi trễ',
    EXCUSED: 'Vắng có phép',
    NO_CLASS: 'Không có buổi',
    FUTURE: 'Sắp tới',
  } satisfies Record<AttendanceDayStatus, string>,
  glyph: {
    PRESENT: '✓',
    ABSENT: '✕',
    LATE: '⏱',
    EXCUSED: '◐',
    NO_CLASS: '·',
    FUTURE: '○',
  } satisfies Record<AttendanceDayStatus, string>,
} as const;

/**
 * Visible legend entries (subset of statuses — NO_CLASS / FUTURE excluded as
 * they're decorative-only states).
 */
const LEGEND_STATUSES: ReadonlyArray<AttendanceDayStatus> = [
  'PRESENT',
  'ABSENT',
  'LATE',
  'EXCUSED',
];

/**
 * Status → background colour. Uses Tailwind tokens that resolve via the
 * consumer's tailwind.config (same approach as <AttendanceRoster>).
 */
const STATUS_CLASS: Record<AttendanceDayStatus, string> = {
  PRESENT: 'bg-emerald-500/15 text-emerald-700 border-emerald-500/40',
  ABSENT: 'bg-rose-500/15 text-rose-700 border-rose-500/40',
  LATE: 'bg-amber-400/20 text-amber-800 border-amber-500/40',
  EXCUSED: 'bg-slate-300/30 text-slate-700 border-slate-400/40',
  NO_CLASS: 'bg-muted/30 text-muted-foreground border-transparent',
  FUTURE:
    'bg-card text-muted-foreground border-dashed border-border/60 opacity-70',
};

const STATUS_SWATCH_CLASS: Record<AttendanceDayStatus, string> = {
  PRESENT: 'bg-emerald-500',
  ABSENT: 'bg-rose-500',
  LATE: 'bg-amber-500',
  EXCUSED: 'bg-slate-400',
  NO_CLASS: 'bg-muted',
  FUTURE: 'bg-card border border-dashed',
};

/**
 * Cycle order for editable mode space-bar: PRESENT → ABSENT → LATE → EXCUSED → PRESENT.
 * NO_CLASS / FUTURE are not cycled (cells in those states are inert).
 */
const CYCLE_ORDER: ReadonlyArray<AttendanceDayStatus> = [
  'PRESENT',
  'ABSENT',
  'LATE',
  'EXCUSED',
];

function nextCycleStatus(
  current: AttendanceDayStatus,
): AttendanceDayStatus {
  const idx = CYCLE_ORDER.indexOf(current);
  if (idx === -1) {
    // NO_CLASS / FUTURE → start the cycle at PRESENT
    return 'PRESENT';
  }
  // CYCLE_ORDER has length 4 — modulo arithmetic guarantees a defined result.
  // We assert non-undefined for `noUncheckedIndexedAccess`.
  const next = CYCLE_ORDER[(idx + 1) % CYCLE_ORDER.length];
  return next ?? 'PRESENT';
}

/**
 * Compute weekday index, Mon=0..Sun=6, for the 1st of (year, month-1) where
 * month is 1-based (1..12).
 *
 * `Date#getDay()` returns Sun=0..Sat=6; we shift to Mon=0..Sun=6.
 */
function leadingPadCount(year: number, month: number): number {
  const firstOfMonth = new Date(year, month - 1, 1);
  const sundayBased = firstOfMonth.getDay(); // 0..6 (Sun=0)
  // Convert: Mon=0, Tue=1, ..., Sun=6
  return (sundayBased + 6) % 7;
}

/**
 * Build a 2-digit string for date formatting.
 */
function pad2(n: number): string {
  return String(n).padStart(2, '0');
}

export function AttendanceCalendar(
  props: AttendanceCalendarProps,
): React.JSX.Element {
  const {
    month,
    streak,
    selectedDay,
    editable = false,
    onSelectDay,
    onCycleStatus,
    className,
  } = props;

  const headerId = useId();
  const padCount = leadingPadCount(month.year, month.month);
  const totalCells = padCount + month.days.length;
  const trailingPadCount =
    totalCells % 7 === 0 ? 0 : 7 - (totalCells % 7);

  const handleClick = useCallback(
    (dayOfMonth: number) => {
      if (onSelectDay) {
        onSelectDay(dayOfMonth);
      }
    },
    [onSelectDay],
  );

  const handleKeyDown = useCallback(
    (
      e: React.KeyboardEvent<HTMLButtonElement>,
      day: CalendarDay,
    ): void => {
      // Space cycles status when editable
      if (editable && (e.key === ' ' || e.key === 'Spacebar')) {
        e.preventDefault();
        if (onCycleStatus) {
          onCycleStatus(day.dayOfMonth, nextCycleStatus(day.status));
        }
        return;
      }

      // Arrow nav between days
      const idx = day.dayOfMonth;
      let target: number | null = null;
      if (e.key === 'ArrowRight') target = idx + 1;
      else if (e.key === 'ArrowLeft') target = idx - 1;
      else if (e.key === 'ArrowDown') target = idx + 7;
      else if (e.key === 'ArrowUp') target = idx - 7;

      if (target !== null) {
        e.preventDefault();
        const targetEl = document.querySelector<HTMLButtonElement>(
          `[data-testid="attendance-cal-day-${target}"]`,
        );
        if (targetEl) {
          targetEl.focus();
        }
      }
    },
    [editable, onCycleStatus],
  );

  return (
    <div
      data-testid="attendance-calendar"
      className={`flex flex-col gap-3 ${className ?? ''}`}
    >
      {/* Header row: month label + optional streak chip */}
      <header
        className="flex flex-wrap items-center justify-between gap-3"
        aria-labelledby={headerId}
      >
        <h2 id={headerId} className="text-base font-semibold">
          {COPY_VI.monthLabel(month.month, month.year)}
        </h2>
        {streak && !streak.deferred && (
          <div
            data-testid="attendance-calendar-streak"
            className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/40 bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-700"
          >
            <span aria-hidden="true">🌟</span>
            <span>{COPY_VI.streak(streak.count)}</span>
          </div>
        )}
      </header>

      {/* Weekday header row */}
      <div
        role="row"
        data-testid="attendance-calendar-weekday-header"
        className="grid grid-cols-7 gap-1 text-center text-xs font-semibold text-muted-foreground"
      >
        {COPY_VI.weekdays.map((wd, i) => (
          <div
            key={wd}
            role="columnheader"
            aria-label={wd}
            className={i >= 5 ? 'text-sky-600' : ''}
          >
            {wd}
          </div>
        ))}
      </div>

      {/* Calendar grid (pad + day cells + trailing pad) */}
      <div
        data-testid="attendance-calendar-grid"
        role="grid"
        aria-labelledby={headerId}
        className="grid grid-cols-1 sm:grid-cols-7 gap-1"
      >
        {/* Leading pads only on sm+ where 7-col grid is active.
            Mobile (< sm) stacks vertically; pads not needed. */}
        {Array.from({ length: padCount }, (_, i) => (
          <div
            key={`pad-lead-${i}`}
            data-testid={`attendance-cal-pad-lead-${i}`}
            className="hidden sm:block aspect-square rounded-md bg-muted/20"
            aria-hidden="true"
          />
        ))}

        {month.days.map((d) => (
          <DayCell
            key={d.dayOfMonth}
            day={d}
            month={month.month}
            selected={selectedDay === d.dayOfMonth}
            onClick={handleClick}
            onKeyDown={handleKeyDown}
          />
        ))}

        {Array.from({ length: trailingPadCount }, (_, i) => (
          <div
            key={`pad-trail-${i}`}
            data-testid={`attendance-cal-pad-trail-${i}`}
            className="hidden sm:block aspect-square rounded-md bg-muted/20"
            aria-hidden="true"
          />
        ))}
      </div>

      {/* Legend */}
      <div
        data-testid="attendance-calendar-legend"
        aria-label={COPY_VI.legendTitle}
        className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-md border bg-card/50 px-3 py-2 text-xs"
      >
        <span className="font-semibold text-muted-foreground">
          {COPY_VI.legendTitle}:
        </span>
        {LEGEND_STATUSES.map((s) => (
          <span key={s} className="inline-flex items-center gap-1.5">
            <span
              aria-hidden="true"
              className={`h-3 w-3 rounded ${STATUS_SWATCH_CLASS[s]}`}
            />
            <span>{COPY_VI.status[s]}</span>
          </span>
        ))}
      </div>
    </div>
  );
}

type DayCellProps = {
  day: CalendarDay;
  month: number;
  selected: boolean;
  onClick: (dayOfMonth: number) => void;
  onKeyDown: (
    e: React.KeyboardEvent<HTMLButtonElement>,
    day: CalendarDay,
  ) => void;
};

function DayCell({
  day,
  month,
  selected,
  onClick,
  onKeyDown,
}: DayCellProps): React.JSX.Element {
  const statusLabel = COPY_VI.status[day.status];
  const aria = `Ngày ${pad2(day.dayOfMonth)}/${pad2(month)}, ${statusLabel}`;
  const inert = day.status === 'NO_CLASS' || day.status === 'FUTURE';

  return (
    <button
      type="button"
      data-testid={`attendance-cal-day-${day.dayOfMonth}`}
      data-status={day.status}
      aria-label={aria}
      aria-pressed={selected}
      onClick={() => onClick(day.dayOfMonth)}
      onKeyDown={(e) => onKeyDown(e, day)}
      tabIndex={0}
      className={[
        'relative flex min-h-[44px] flex-col items-start justify-between rounded-md border p-2 text-left text-sm transition-opacity',
        'focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-1',
        STATUS_CLASS[day.status],
        selected ? 'ring-2 ring-primary' : '',
        inert ? 'cursor-default' : 'hover:opacity-90',
        'aspect-square sm:aspect-square',
      ].join(' ')}
    >
      <span className="text-sm font-semibold tabular-nums">
        {day.dayOfMonth}
      </span>
      <span aria-hidden="true" className="self-end text-xs">
        {COPY_VI.glyph[day.status]}
      </span>
      <span className="sr-only">{statusLabel}</span>
      {day.label && (
        <span className="absolute bottom-1 right-1 text-[10px] font-medium tabular-nums">
          {day.label}
        </span>
      )}
    </button>
  );
}

export default AttendanceCalendar;
