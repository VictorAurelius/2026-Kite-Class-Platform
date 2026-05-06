/**
 * Type definitions for AttendanceCalendar — G8 component (Wave 28 Bucket C).
 *
 * Design source: documents/02-architecture/design-system/ui_kits/components/G8-attendance-calendar/README.md
 * Component gap row: documents/02-architecture/design-system/dossier/04-component-gaps.md §G8
 * Used by screens: KiteClass `/attendance` overview, `/classes/[id]/attendance` history.
 *
 * Status codes (Vietnamese-first per CLAUDE.md):
 *   PRESENT   green   — Có mặt
 *   ABSENT    red     — Vắng không phép
 *   LATE      amber   — Đi trễ
 *   EXCUSED   gray    — Vắng có phép
 *   NO_CLASS  muted   — Không có buổi
 *   FUTURE    placeholder — Sắp tới (pending session in current month)
 *
 * Week starts Monday (Vietnamese convention). Day columns: T2/T3/T4/T5/T6/T7/CN.
 */

export type AttendanceDayStatus =
  | 'PRESENT'
  | 'ABSENT'
  | 'LATE'
  | 'EXCUSED'
  | 'NO_CLASS'
  | 'FUTURE';

/**
 * One day in the calendar grid.
 *
 * `dayOfMonth` is 1-based (1..31). `status` mirrors `AttendanceDayStatus`. The
 * caller pre-computes status per day; the component is presentation-only.
 */
export type CalendarDay = {
  /** Day-of-month, 1..31. */
  dayOfMonth: number;
  /** Attendance status for the day. */
  status: AttendanceDayStatus;
  /**
   * Optional inline label (e.g., session count, attendance % for teacher
   * heatmap variant). When absent, the cell shows the status glyph only.
   */
  label?: string;
};

/**
 * Month-view payload. `year` and `month` (1-based) drive the header label.
 *
 * `days` is the chronological list of days for the month, length 28..31. The
 * component pads weekend / first-week empty cells from the date itself — caller
 * provides only real days.
 */
export type MonthCalendarData = {
  /** Year (4-digit). */
  year: number;
  /** Month, 1..12. */
  month: number;
  /** Days in chronological order, 1..N (no padding). */
  days: ReadonlyArray<CalendarDay>;
};

/**
 * 30-day rolling streak summary (longest consecutive PRESENT-day streak in
 * the last 30 days of `days`).
 */
export type StreakInfo = {
  /** Longest consecutive PRESENT-day streak (in days, 0..30). */
  count: number;
  /**
   * When `true`, the streak calculation is unavailable (e.g., feature
   * deferred per Wave 28 Bucket C PARTIAL). Caller should hide / lazy-load.
   */
  deferred?: boolean;
};

/**
 * Public props for `<AttendanceCalendar>`.
 *
 * The component is **controlled** — caller owns `month` and emits `onSelectDay`
 * when a day cell is clicked. Optional `editable` allows space-bar status
 * cycling for the teacher entry surface; defaults to read-only (`false`).
 */
export type AttendanceCalendarProps = {
  /** Month-view payload. */
  month: MonthCalendarData;
  /**
   * 30-day streak data. When omitted, the streak indicator is hidden.
   * Caller can compute via `calculateStreak(...)` exported from this module.
   */
  streak?: StreakInfo;
  /** Selected day-of-month (1..31). When set, that cell shows `aria-pressed`. */
  selectedDay?: number;
  /**
   * Editable mode: when true, day cells respond to space-bar to cycle
   * status (PRESENT → ABSENT → LATE → EXCUSED → PRESENT). Defaults `false`.
   */
  editable?: boolean;
  /** Fired when a day cell is clicked or activated via Enter. */
  onSelectDay?: (dayOfMonth: number) => void;
  /**
   * Fired in editable mode when space-bar cycles a status. Caller controls
   * the actual data update; component does not mutate.
   */
  onCycleStatus?: (
    dayOfMonth: number,
    nextStatus: AttendanceDayStatus,
  ) => void;
  /**
   * Class name override for the wrapper. Mostly for tests / theming.
   */
  className?: string;
};
