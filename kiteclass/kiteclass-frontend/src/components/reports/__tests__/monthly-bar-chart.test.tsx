/**
 * GAP-1378 — MonthlyBarChart screen-reader accessibility tests.
 *
 * Verifies the chart exposes its per-month values to assistive tech via an
 * sr-only data table (WCAG 1.1.1 Non-text Content), and that the decorative
 * SVG is hidden from the accessibility tree (no masked <title> regression).
 */

import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { MonthlyBarChart } from '../monthly-bar-chart';

const data = [
  { month: '2026-01', value: 1000 },
  { month: '2026-02', value: 2500 },
  { month: '2026-03', value: 0 },
];

describe('MonthlyBarChart — a11y (GAP-1378)', () => {
  it('renders an accessible data table with month → value rows', () => {
    render(
      <MonthlyBarChart
        data={data}
        formatValue={(v) => `${v}đ`}
        label="Doanh thu theo tháng"
      />
    );

    // Table is the screen-reader alternative to the visual bars.
    const table = screen.getByRole('table', { name: 'Doanh thu theo tháng' });
    expect(table).toBeInTheDocument();

    // Each month's real value is reachable as a table cell.
    expect(within(table).getByText('T1')).toBeInTheDocument();
    expect(within(table).getByText('1000đ')).toBeInTheDocument();
    expect(within(table).getByText('T2')).toBeInTheDocument();
    expect(within(table).getByText('2500đ')).toBeInTheDocument();
  });

  it('does not expose the decorative SVG as an image role', () => {
    render(<MonthlyBarChart data={data} formatValue={(v) => `${v}`} />);
    // After the fix the SVG is aria-hidden (decorative); no role="img" leaks.
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('falls back to a generic caption when no label is passed', () => {
    render(<MonthlyBarChart data={data} formatValue={(v) => `${v}`} />);
    expect(
      screen.getByRole('table', { name: 'Biểu đồ cột theo tháng' })
    ).toBeInTheDocument();
  });
});
