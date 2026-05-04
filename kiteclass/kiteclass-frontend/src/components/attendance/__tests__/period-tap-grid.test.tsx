/**
 * Tests for PeriodTapGrid (Phase 1B v1, GAP-323b).
 *
 * @since 4.x.x (Wave 18b2 Bucket A)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PeriodTapGrid } from '../period-tap-grid';

const students = [
  { studentId: 101, fullName: 'Nguyễn Văn A' },
  { studentId: 102, fullName: 'Trần Thị B' },
];

describe('PeriodTapGrid', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders one row per student with the student name visible', () => {
    render(
      <PeriodTapGrid
        students={students}
        statuses={{}}
        onStatusChange={vi.fn()}
      />,
    );

    expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    expect(screen.getByText('Trần Thị B')).toBeInTheDocument();
  });

  it('exposes 4 status buttons per student (P / Có phép / Vắng / Trễ)', () => {
    render(
      <PeriodTapGrid
        students={students}
        statuses={{}}
        onStatusChange={vi.fn()}
      />,
    );

    // Each student row has 4 buttons → 2 students × 4 = 8 buttons total.
    const present = screen.getAllByRole('button', { name: /Có mặt/ });
    const excused = screen.getAllByRole('button', { name: /Có phép/ });
    const absent = screen.getAllByRole('button', { name: /^Vắng - / });
    const late = screen.getAllByRole('button', { name: /Trễ/ });

    expect(present).toHaveLength(2);
    expect(excused).toHaveLength(2);
    expect(absent).toHaveLength(2);
    expect(late).toHaveLength(2);
  });

  it('calls onStatusChange with the studentId + chosen status', () => {
    const onStatusChange = vi.fn();
    render(
      <PeriodTapGrid
        students={students}
        statuses={{}}
        onStatusChange={onStatusChange}
      />,
    );

    const firstStudentRow = screen.getByTestId('period-tap-row-101');
    const absentBtn = firstStudentRow.querySelector(
      'button[data-status="ABSENT"]',
    ) as HTMLButtonElement;
    fireEvent.click(absentBtn);

    expect(onStatusChange).toHaveBeenCalledWith(101, 'ABSENT');
  });

  it('marks the active status with aria-pressed="true"', () => {
    render(
      <PeriodTapGrid
        students={students}
        statuses={{ 101: 'PRESENT', 102: 'LATE' }}
        onStatusChange={vi.fn()}
      />,
    );

    const row1 = screen.getByTestId('period-tap-row-101');
    const presentBtn1 = row1.querySelector(
      'button[data-status="PRESENT"]',
    ) as HTMLButtonElement;
    expect(presentBtn1.getAttribute('aria-pressed')).toBe('true');

    const row2 = screen.getByTestId('period-tap-row-102');
    const lateBtn2 = row2.querySelector(
      'button[data-status="LATE"]',
    ) as HTMLButtonElement;
    expect(lateBtn2.getAttribute('aria-pressed')).toBe('true');
  });
});
