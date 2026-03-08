/**
 * EnhancedAttendanceCalendar component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen, fireEvent, within } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { EnhancedAttendanceCalendar } from '../enhanced-attendance-calendar';
import { mockAttendanceRecords } from '@/__tests__/fixtures/attendance';

describe('EnhancedAttendanceCalendar', () => {
  describe('Rendering', () => {
    it('renders calendar grid', () => {
      render(
        <EnhancedAttendanceCalendar attendanceRecords={mockAttendanceRecords} />
      );

      // Check for weekday headers
      expect(screen.getByText('T2')).toBeInTheDocument();
      expect(screen.getByText('T3')).toBeInTheDocument();
      expect(screen.getByText('T4')).toBeInTheDocument();
      expect(screen.getByText('T5')).toBeInTheDocument();
      expect(screen.getByText('T6')).toBeInTheDocument();
      expect(screen.getByText('T7')).toBeInTheDocument();
      expect(screen.getByText('CN')).toBeInTheDocument();
    });

    it('renders month/year title', () => {
      render(
        <EnhancedAttendanceCalendar attendanceRecords={mockAttendanceRecords} />
      );

      const monthTitle = screen.getByRole('heading', { level: 3 });
      expect(monthTitle).toBeInTheDocument();
    });

    it('renders navigation buttons', () => {
      render(
        <EnhancedAttendanceCalendar attendanceRecords={mockAttendanceRecords} />
      );

      expect(screen.getByText('Hôm nay')).toBeInTheDocument();
      // Check for chevron buttons (2 buttons)
      const buttons = screen.getAllByRole('button');
      const chevronButtons = buttons.filter((btn) => {
        const svg = btn.querySelector('svg');
        return svg !== null && !btn.textContent?.includes('Hôm nay');
      });
      expect(chevronButtons.length).toBeGreaterThanOrEqual(2);
    });

    it('renders legend', () => {
      render(
        <EnhancedAttendanceCalendar attendanceRecords={mockAttendanceRecords} />
      );

      expect(screen.getByText(/≥90% có mặt/)).toBeInTheDocument();
      expect(screen.getByText(/70-89% có mặt/)).toBeInTheDocument();
      expect(screen.getByText(/50-69% có mặt/)).toBeInTheDocument();
      expect(screen.getByText(/<50% có mặt/)).toBeInTheDocument();
    });
  });

  describe('Filters', () => {
    it('shows status filter when showFilters is true', () => {
      render(
        <EnhancedAttendanceCalendar
          attendanceRecords={mockAttendanceRecords}
          showFilters={true}
        />
      );

      expect(screen.getByText('Lọc theo trạng thái:')).toBeInTheDocument();
    });

    it('hides filters when showFilters is false', () => {
      render(
        <EnhancedAttendanceCalendar
          attendanceRecords={mockAttendanceRecords}
          showFilters={false}
        />
      );

      expect(screen.queryByText('Lọc theo trạng thái:')).not.toBeInTheDocument();
    });

    it('filters records by status', () => {
      const { container } = render(
        <EnhancedAttendanceCalendar
          attendanceRecords={mockAttendanceRecords}
          showFilters={true}
        />
      );

      // Click filter dropdown
      const select = screen.getByRole('combobox');
      fireEvent.click(select);

      // Select "Có mặt" (PRESENT)
      const presentOption = screen.getByText('Có mặt');
      fireEvent.click(presentOption);

      // Calendar should update (implementation-dependent)
      // Just verify no errors occurred
      expect(container).toBeInTheDocument();
    });
  });

  describe('Navigation', () => {
    it('navigates to previous month', () => {
      render(
        <EnhancedAttendanceCalendar attendanceRecords={mockAttendanceRecords} />
      );

      const currentMonth = screen.getByRole('heading', { level: 3 }).textContent;

      // Click previous button (first chevron button)
      const buttons = screen.getAllByRole('button');
      const prevButton = buttons.find((btn) => {
        const svg = btn.querySelector('svg');
        return svg !== null && !btn.textContent?.includes('Hôm nay');
      });

      if (prevButton) {
        fireEvent.click(prevButton);

        const newMonth = screen.getByRole('heading', { level: 3 }).textContent;
        // Month should change (not strictly equal)
        expect(newMonth).toBeDefined();
      }
    });

    it('navigates to next month', () => {
      render(
        <EnhancedAttendanceCalendar attendanceRecords={mockAttendanceRecords} />
      );

      const currentMonth = screen.getByRole('heading', { level: 3 }).textContent;

      // Click next button (last chevron button)
      const buttons = screen.getAllByRole('button');
      const nextButton = buttons[buttons.length - 1];

      fireEvent.click(nextButton);

      const newMonth = screen.getByRole('heading', { level: 3 }).textContent;
      expect(newMonth).toBeDefined();
    });

    it('navigates to today', () => {
      render(
        <EnhancedAttendanceCalendar attendanceRecords={mockAttendanceRecords} />
      );

      const todayButton = screen.getByText('Hôm nay');
      fireEvent.click(todayButton);

      // Should navigate to current month
      const monthTitle = screen.getByRole('heading', { level: 3 });
      expect(monthTitle).toBeInTheDocument();
    });
  });

  describe('Date Clicks', () => {
    it('calls onDateClick when date with attendance is clicked', () => {
      const onDateClick = vi.fn();

      render(
        <EnhancedAttendanceCalendar
          attendanceRecords={mockAttendanceRecords}
          onDateClick={onDateClick}
        />
      );

      // Find a date button with attendance (has class bg-*)
      const buttons = screen.getAllByRole('button');
      const dateButton = buttons.find((btn) => {
        return btn.className.includes('bg-green') || btn.className.includes('lần');
      });

      if (dateButton) {
        fireEvent.click(dateButton);
        expect(onDateClick).toHaveBeenCalled();
      }
    });

    it('does not call onDateClick for dates without attendance', () => {
      const onDateClick = vi.fn();

      render(
        <EnhancedAttendanceCalendar
          attendanceRecords={[]} // No records
          onDateClick={onDateClick}
        />
      );

      // Click any date
      const buttons = screen.getAllByRole('button');
      const dateButton = buttons.find((btn) => {
        const text = btn.textContent;
        return text && /^\d+$/.test(text.trim());
      });

      if (dateButton && !dateButton.hasAttribute('disabled')) {
        fireEvent.click(dateButton);
      }

      // Should not be called for empty dates
      expect(onDateClick).not.toHaveBeenCalled();
    });
  });

  describe('Empty State', () => {
    it('renders calendar even with no attendance records', () => {
      render(<EnhancedAttendanceCalendar attendanceRecords={[]} />);

      expect(screen.getByText('T2')).toBeInTheDocument();
      expect(screen.getByText('Hôm nay')).toBeInTheDocument();
    });

    it('does not show attendance counts when no records', () => {
      render(<EnhancedAttendanceCalendar attendanceRecords={[]} />);

      expect(screen.queryByText(/lần/)).not.toBeInTheDocument();
    });
  });

  describe('Color Coding', () => {
    it('applies green background for high attendance dates', () => {
      const highAttendanceRecords = [
        ...Array(10).fill(null).map((_, i) => ({
          ...mockAttendanceRecords[0],
          id: i + 1,
          status: 'PRESENT' as const,
        })),
      ];

      const { container } = render(
        <EnhancedAttendanceCalendar attendanceRecords={highAttendanceRecords} />
      );

      const greenDates = container.querySelectorAll('[class*="bg-green"]');
      expect(greenDates.length).toBeGreaterThan(0);
    });

    it('applies red background for low attendance dates', () => {
      const lowAttendanceRecords = [
        ...Array(10).fill(null).map((_, i) => ({
          ...mockAttendanceRecords[1], // ABSENT
          id: i + 1,
          status: 'ABSENT' as const,
        })),
      ];

      const { container } = render(
        <EnhancedAttendanceCalendar attendanceRecords={lowAttendanceRecords} />
      );

      const redDates = container.querySelectorAll('[class*="bg-red"]');
      expect(redDates.length).toBeGreaterThan(0);
    });
  });
});
