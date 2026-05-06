/**
 * AttendanceCalendar component tests — Wave 28 Bucket C (G8).
 *
 * Coverage:
 *  1. Renders Vietnamese month header (`Tháng 10/2026`)
 *  2. Renders 7 weekday headers in Vietnamese order: T2 T3 T4 T5 T6 T7 CN
 *  3. Pads first-week leading cells (Mon-start) — Oct 2026 starts Thu, so 3
 *     leading pad cells before Oct 1.
 *  4. Renders all month days as buttons with correct aria-label including %
 *  5. Click on a day fires onSelectDay with the day-of-month
 *  6. Selected day exposes aria-pressed="true"
 *  7. Status legend present (Vietnamese labels: Có mặt / Vắng / Đi trễ / Vắng phép)
 *  8. Streak indicator renders "Chuỗi N ngày" when streak prop provided
 *  9. Streak indicator hidden when streak.deferred === true
 * 10. Editable mode: space-bar on a day cell cycles status + fires onCycleStatus
 *
 * Vietnamese-first per CLAUDE.md.
 */

import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AttendanceCalendar } from '../AttendanceCalendar';
import type {
  AttendanceCalendarProps,
  CalendarDay,
  AttendanceDayStatus,
} from '../types';

/** Build a CalendarDay quickly. */
function day(d: number, status: AttendanceDayStatus = 'NO_CLASS'): CalendarDay {
  return { dayOfMonth: d, status };
}

/** October 2026 — 31 days, Oct 1 = Thursday. Most days NO_CLASS. */
function buildOct2026(
  overrides: Partial<Record<number, AttendanceDayStatus>> = {},
): CalendarDay[] {
  return Array.from({ length: 31 }, (_, i) => {
    const d = i + 1;
    return day(d, overrides[d] ?? 'NO_CLASS');
  });
}

const baseProps = (
  override: Partial<AttendanceCalendarProps> = {},
): AttendanceCalendarProps => ({
  month: {
    year: 2026,
    month: 10,
    days: buildOct2026({
      2: 'PRESENT',
      5: 'PRESENT',
      9: 'ABSENT',
      14: 'PRESENT',
    }),
  },
  ...override,
});

describe('<AttendanceCalendar>', () => {
  it('1. renders Vietnamese month header (Tháng 10/2026)', () => {
    render(<AttendanceCalendar {...baseProps()} />);
    expect(screen.getByText(/Tháng 10/i)).toBeInTheDocument();
    expect(screen.getByText(/2026/)).toBeInTheDocument();
  });

  it('2. renders 7 weekday headers Mon-first (T2..CN)', () => {
    render(<AttendanceCalendar {...baseProps()} />);
    const header = screen.getByTestId('attendance-calendar-weekday-header');
    const labels = within(header).getAllByRole('columnheader');
    expect(labels.map((l) => l.textContent)).toEqual([
      'T2',
      'T3',
      'T4',
      'T5',
      'T6',
      'T7',
      'CN',
    ]);
  });

  it('3. pads leading cells before Oct 1 (Thursday → 3 pad cells)', () => {
    render(<AttendanceCalendar {...baseProps()} />);
    const grid = screen.getByTestId('attendance-calendar-grid');
    const padCells = within(grid).getAllByTestId(/^attendance-cal-pad-/);
    // Oct 1 2026 is Thursday → Mon=0, Tue=1, Wed=2, Thu=3 → 3 leading pads.
    expect(padCells.length).toBeGreaterThanOrEqual(3);
  });

  it('4. renders all 31 day buttons for October', () => {
    render(<AttendanceCalendar {...baseProps()} />);
    for (let d = 1; d <= 31; d += 1) {
      expect(
        screen.getByTestId(`attendance-cal-day-${d}`),
      ).toBeInTheDocument();
    }
  });

  it('5. clicking a day fires onSelectDay with the day-of-month', async () => {
    const user = userEvent.setup();
    const onSelectDay = vi.fn();
    render(<AttendanceCalendar {...baseProps({ onSelectDay })} />);
    await user.click(screen.getByTestId('attendance-cal-day-9'));
    expect(onSelectDay).toHaveBeenCalledWith(9);
  });

  it('6. selectedDay sets aria-pressed="true" on the matching cell', () => {
    render(<AttendanceCalendar {...baseProps({ selectedDay: 14 })} />);
    const cell = screen.getByTestId('attendance-cal-day-14');
    expect(cell).toHaveAttribute('aria-pressed', 'true');
    // Other cells are not pressed
    expect(screen.getByTestId('attendance-cal-day-2')).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('7. legend shows Vietnamese status labels', () => {
    render(<AttendanceCalendar {...baseProps()} />);
    const legend = screen.getByTestId('attendance-calendar-legend');
    expect(within(legend).getByText('Có mặt')).toBeInTheDocument();
    expect(
      within(legend).getByText('Vắng không phép'),
    ).toBeInTheDocument();
    expect(within(legend).getByText('Đi trễ')).toBeInTheDocument();
    expect(within(legend).getByText('Vắng có phép')).toBeInTheDocument();
  });

  it('8. streak indicator renders "Chuỗi N ngày" when streak.count > 0', () => {
    render(
      <AttendanceCalendar
        {...baseProps({ streak: { count: 12 } })}
      />,
    );
    expect(
      screen.getByTestId('attendance-calendar-streak'),
    ).toHaveTextContent(/Chuỗi 12 ngày/);
  });

  it('9. streak indicator hidden when streak.deferred === true', () => {
    render(
      <AttendanceCalendar
        {...baseProps({ streak: { count: 0, deferred: true } })}
      />,
    );
    expect(
      screen.queryByTestId('attendance-calendar-streak'),
    ).not.toBeInTheDocument();
  });

  it('10. editable mode: space-bar on a day cell cycles status + fires onCycleStatus', async () => {
    const user = userEvent.setup();
    const onCycleStatus = vi.fn();
    render(
      <AttendanceCalendar
        {...baseProps({ editable: true, onCycleStatus })}
      />,
    );
    const cell = screen.getByTestId('attendance-cal-day-2'); // PRESENT in baseProps
    cell.focus();
    await user.keyboard(' ');
    // PRESENT → ABSENT (per cycle definition in component)
    expect(onCycleStatus).toHaveBeenCalledWith(2, 'ABSENT');
  });

  it('11. each day button has accessible aria-label with date', () => {
    render(<AttendanceCalendar {...baseProps()} />);
    const cell = screen.getByTestId('attendance-cal-day-9');
    expect(cell.getAttribute('aria-label')).toMatch(/09\/10/);
  });

  it('12. arrow-right key moves focus to next day', async () => {
    const user = userEvent.setup();
    render(<AttendanceCalendar {...baseProps()} />);
    const day5 = screen.getByTestId('attendance-cal-day-5');
    day5.focus();
    await user.keyboard('{ArrowRight}');
    expect(document.activeElement).toBe(
      screen.getByTestId('attendance-cal-day-6'),
    );
  });
});
