/**
 * AttendanceStatsOverview component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AttendanceStatsOverview } from '../attendance-stats-overview';
import {
  mockAttendanceStats,
  mockHighAttendanceStats,
  mockLowAttendanceStats,
} from '@/__tests__/fixtures/attendance';

describe('AttendanceStatsOverview', () => {
  describe('Rendering', () => {
    it('renders stats correctly', () => {
      render(<AttendanceStatsOverview stats={mockAttendanceStats} />);

      expect(screen.getByText('85.0%')).toBeInTheDocument();
      expect(screen.getByText('40')).toBeInTheDocument(); // total sessions
      expect(screen.getByText('34')).toBeInTheDocument(); // present
      expect(screen.getByText('3')).toBeInTheDocument(); // absent
      expect(screen.getByText('2')).toBeInTheDocument(); // late
      expect(screen.getByText('1')).toBeInTheDocument(); // excused
    });

    it('renders all stat labels', () => {
      render(<AttendanceStatsOverview stats={mockAttendanceStats} />);

      expect(screen.getByText('Tổng số')).toBeInTheDocument();
      expect(screen.getByText('Có mặt')).toBeInTheDocument();
      expect(screen.getByText('Vắng')).toBeInTheDocument();
      expect(screen.getByText('Đi trễ')).toBeInTheDocument();
      expect(screen.getByText('Có phép')).toBeInTheDocument();
    });
  });

  describe('Progress Bar', () => {
    it('shows progress bar when showProgress is true', () => {
      render(
        <AttendanceStatsOverview stats={mockAttendanceStats} showProgress={true} />
      );

      const progressBars = screen.getAllByRole('progressbar');
      expect(progressBars.length).toBeGreaterThan(0);
    });

    it('hides progress bar when showProgress is false', () => {
      render(
        <AttendanceStatsOverview stats={mockAttendanceStats} showProgress={false} />
      );

      const progressBars = screen.queryAllByRole('progressbar');
      expect(progressBars.length).toBe(0);
    });

    it('shows progress by default', () => {
      render(<AttendanceStatsOverview stats={mockAttendanceStats} />);

      const progressBars = screen.getAllByRole('progressbar');
      expect(progressBars.length).toBeGreaterThan(0);
    });
  });

  describe('Color Coding', () => {
    it('applies green color for high attendance (≥90%)', () => {
      render(<AttendanceStatsOverview stats={mockHighAttendanceStats} />);

      const rateElement = screen.getByText('95.0%');
      expect(rateElement).toHaveClass('text-green-600');
    });

    it('applies yellow color for medium attendance (75-89%)', () => {
      render(<AttendanceStatsOverview stats={mockAttendanceStats} />);

      const rateElement = screen.getByText('85.0%');
      expect(rateElement).toHaveClass('text-yellow-600');
    });

    it('applies red color for low attendance (<75%)', () => {
      render(<AttendanceStatsOverview stats={mockLowAttendanceStats} />);

      const rateElement = screen.getByText('60.0%');
      expect(rateElement).toHaveClass('text-red-600');
    });
  });

  describe('Makeup Count', () => {
    it('shows makeup count when showMakeup is true', () => {
      const statsWithMakeup = { ...mockAttendanceStats, makeupCount: 2 };
      render(
        <AttendanceStatsOverview stats={statsWithMakeup} showMakeup={true} />
      );

      expect(screen.getByText('Học bù')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
    });

    it('hides makeup count when showMakeup is false', () => {
      const statsWithMakeup = { ...mockAttendanceStats, makeupCount: 2 };
      render(
        <AttendanceStatsOverview stats={statsWithMakeup} showMakeup={false} />
      );

      expect(screen.queryByText('Học bù')).not.toBeInTheDocument();
    });
  });

  describe('Variants', () => {
    it('renders default variant correctly', () => {
      render(
        <AttendanceStatsOverview stats={mockAttendanceStats} variant="default" />
      );

      expect(screen.getByText('Tỷ lệ điểm danh')).toBeInTheDocument();
      expect(screen.getByText('Chi tiết')).toBeInTheDocument();
    });

    it('renders compact variant correctly', () => {
      render(
        <AttendanceStatsOverview stats={mockAttendanceStats} variant="compact" />
      );

      expect(screen.getByText('Tỷ lệ điểm danh')).toBeInTheDocument();
      // Compact variant doesn't have "Chi tiết" heading
      expect(screen.queryByText('Chi tiết')).not.toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles zero attendance rate', () => {
      const zeroStats = {
        ...mockAttendanceStats,
        attendanceRate: 0,
        presentCount: 0,
      };
      render(<AttendanceStatsOverview stats={zeroStats} />);

      expect(screen.getByText('0.0%')).toBeInTheDocument();
    });

    it('handles perfect attendance rate', () => {
      const perfectStats = {
        ...mockAttendanceStats,
        attendanceRate: 100,
        presentCount: 40,
        absentCount: 0,
      };
      render(<AttendanceStatsOverview stats={perfectStats} />);

      expect(screen.getByText('100.0%')).toBeInTheDocument();
    });
  });
});
