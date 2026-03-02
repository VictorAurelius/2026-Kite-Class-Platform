/**
 * Unit tests for AttendanceCalendar component.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

import { render, screen, fireEvent } from '@testing-library/react';
import { AttendanceCalendar } from '../attendance-calendar';
import { AttendanceStatus } from '@/types/attendance';
import type { Attendance } from '@/types/attendance';

describe('AttendanceCalendar', () => {
  const mockAttendanceRecords: Attendance[] = [
    {
      id: 1,
      enrollmentId: 1,
      studentName: 'Student 1',
      sessionId: 1,
      sessionNumber: 1,
      status: AttendanceStatus.PRESENT,
      markedDate: '2024-01-15T10:00:00Z',
      pointsAwarded: 10,
      createdAt: '2024-01-15T10:00:00Z',
      updatedAt: '2024-01-15T10:00:00Z',
    },
    {
      id: 2,
      enrollmentId: 2,
      studentName: 'Student 2',
      sessionId: 1,
      sessionNumber: 1,
      status: AttendanceStatus.ABSENT,
      markedDate: '2024-01-15T10:00:00Z',
      pointsAwarded: 0,
      createdAt: '2024-01-15T10:00:00Z',
      updatedAt: '2024-01-15T10:00:00Z',
    },
  ];

  it('renders calendar with weekday headers', () => {
    render(<AttendanceCalendar attendanceRecords={[]} />);

    expect(screen.getByText('T2')).toBeInTheDocument();
    expect(screen.getByText('T3')).toBeInTheDocument();
    expect(screen.getByText('T4')).toBeInTheDocument();
    expect(screen.getByText('T5')).toBeInTheDocument();
    expect(screen.getByText('T6')).toBeInTheDocument();
    expect(screen.getByText('T7')).toBeInTheDocument();
    expect(screen.getByText('CN')).toBeInTheDocument();
  });

  it('renders navigation buttons', () => {
    render(<AttendanceCalendar attendanceRecords={[]} />);

    expect(screen.getByText('Hôm nay')).toBeInTheDocument();
    expect(screen.getAllByRole('button')).toHaveLength(3); // Today, Previous, Next
  });

  it('displays current month name', () => {
    render(<AttendanceCalendar attendanceRecords={[]} />);

    const monthElement = screen.getByRole('heading');
    expect(monthElement).toBeInTheDocument();
    // Month should be in Vietnamese format
    expect(monthElement.textContent).toMatch(/tháng/i);
  });

  it('calls onDateClick when a date with attendance is clicked', () => {
    const mockOnDateClick = jest.fn();

    render(
      <AttendanceCalendar
        attendanceRecords={mockAttendanceRecords}
        onDateClick={mockOnDateClick}
      />
    );

    // Find buttons with attendance count
    const buttonsWithAttendance = screen.getAllByText(/lần/);
    expect(buttonsWithAttendance.length).toBeGreaterThan(0);

    // Click on the first date with attendance
    const firstButton = buttonsWithAttendance[0].closest('button');
    if (firstButton) {
      fireEvent.click(firstButton);
      expect(mockOnDateClick).toHaveBeenCalled();
    }
  });

  it('shows attendance count on dates with records', () => {
    render(<AttendanceCalendar attendanceRecords={mockAttendanceRecords} />);

    // Should show "2 lần" for the date with 2 attendance records
    expect(screen.getByText(/2 lần/)).toBeInTheDocument();
  });

  it('shows present and absent counts', () => {
    render(<AttendanceCalendar attendanceRecords={mockAttendanceRecords} />);

    // Should show checkmark for present and x for absent
    expect(screen.getByText(/✓ 1/)).toBeInTheDocument();
    expect(screen.getByText(/✗ 1/)).toBeInTheDocument();
  });

  it('displays legend with attendance rate colors', () => {
    render(<AttendanceCalendar attendanceRecords={[]} />);

    expect(screen.getByText('≥90% có mặt')).toBeInTheDocument();
    expect(screen.getByText('70-89% có mặt')).toBeInTheDocument();
    expect(screen.getByText('50-69% có mặt')).toBeInTheDocument();
    expect(screen.getByText('<50% có mặt')).toBeInTheDocument();
  });

  it('navigates to next month when next button is clicked', () => {
    render(<AttendanceCalendar attendanceRecords={[]} />);

    const initialMonth = screen.getByRole('heading').textContent;
    const nextButton = screen.getAllByRole('button')[2]; // Third button is next

    fireEvent.click(nextButton);

    const newMonth = screen.getByRole('heading').textContent;
    expect(newMonth).not.toBe(initialMonth);
  });

  it('navigates to previous month when previous button is clicked', () => {
    render(<AttendanceCalendar attendanceRecords={[]} />);

    const initialMonth = screen.getByRole('heading').textContent;
    const prevButton = screen.getAllByRole('button')[1]; // Second button is previous

    fireEvent.click(prevButton);

    const newMonth = screen.getByRole('heading').textContent;
    expect(newMonth).not.toBe(initialMonth);
  });

  it('returns to current month when today button is clicked', () => {
    render(<AttendanceCalendar attendanceRecords={[]} />);

    const nextButton = screen.getAllByRole('button')[2];
    fireEvent.click(nextButton); // Go to next month

    const todayButton = screen.getByText('Hôm nay');
    fireEvent.click(todayButton);

    // Should be back to current month
    const currentMonth = new Date().toLocaleDateString('vi-VN', {
      month: 'long',
      year: 'numeric',
    });
    expect(screen.getByText(currentMonth)).toBeInTheDocument();
  });
});
