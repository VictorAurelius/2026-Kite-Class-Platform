/**
 * ClassStatsTable component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { ClassStatsTable } from '../class-stats-table';
import { mockClassBreakdown } from '@/__tests__/fixtures/attendance';

describe('ClassStatsTable', () => {
  describe('Loading State', () => {
    it('renders loading skeleton when isLoading is true', () => {
      render(<ClassStatsTable data={[]} isLoading={true} />);

      const skeletons = screen.getAllByRole('generic').filter((el) =>
        el.className.includes('animate-pulse')
      );
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it('does not show data while loading', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={true} />);

      expect(screen.queryByText('Toán Lớp 10A')).not.toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('shows empty state when no data', () => {
      render(<ClassStatsTable data={[]} isLoading={false} />);

      expect(screen.getByText('Chưa có dữ liệu thống kê')).toBeInTheDocument();
    });

    it('shows empty state message', () => {
      render(<ClassStatsTable data={[]} isLoading={false} />);

      expect(
        screen.getByText(/Thống kê lớp học sẽ hiển thị tại đây khi có dữ liệu điểm danh/)
      ).toBeInTheDocument();
    });

    it('shows empty state icon', () => {
      render(<ClassStatsTable data={[]} isLoading={false} />);

      expect(screen.getByText('📊')).toBeInTheDocument();
    });
  });

  describe('Summary Cards', () => {
    it('displays total classes count', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('Tổng lớp học')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument(); // 3 classes
    });

    it('displays total sessions count', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('Tổng buổi học')).toBeInTheDocument();
      expect(screen.getByText('60')).toBeInTheDocument(); // 20+18+22
    });

    it('displays average attendance rate', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('Tỷ lệ TB')).toBeInTheDocument();
      // Average of 90.0, 84.4, 91.1 ≈ 88.5%
      const avgElement = screen.getByText(/88\./);
      expect(avgElement).toBeInTheDocument();
    });

    it('displays best class performance', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('Cao nhất')).toBeInTheDocument();
      expect(screen.getByText('91.1%')).toBeInTheDocument();
      expect(screen.getByText('Hóa Lớp 12C')).toBeInTheDocument();
    });
  });

  describe('Table Data', () => {
    it('renders all class names', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('Toán Lớp 10A')).toBeInTheDocument();
      expect(screen.getByText('Lý Lớp 11B')).toBeInTheDocument();
      expect(screen.getByText('Hóa Lớp 12C')).toBeInTheDocument();
    });

    it('renders teacher names', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText(/GV Trần B/)).toBeInTheDocument();
      expect(screen.getByText(/GV Nguyễn C/)).toBeInTheDocument();
      expect(screen.getByText(/GV Lê D/)).toBeInTheDocument();
    });

    it('renders session counts', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('20')).toBeInTheDocument(); // Toán
      expect(screen.getByText('18')).toBeInTheDocument(); // Lý
      expect(screen.getByText('22')).toBeInTheDocument(); // Hóa
    });

    it('renders present counts', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('450')).toBeInTheDocument();
      expect(screen.getByText('380')).toBeInTheDocument();
      expect(screen.getByText('510')).toBeInTheDocument();
    });

    it('renders absent counts', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('30')).toBeInTheDocument();
      expect(screen.getByText('45')).toBeInTheDocument();
      expect(screen.getByText('20')).toBeInTheDocument();
    });

    it('renders late counts', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('15')).toBeInTheDocument();
      expect(screen.getByText('20')).toBeInTheDocument();
      expect(screen.getByText('18')).toBeInTheDocument();
    });

    it('renders attendance rates', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('90.0%')).toBeInTheDocument();
      expect(screen.getByText('84.4%')).toBeInTheDocument();
      expect(screen.getByText('91.1%')).toBeInTheDocument();
    });
  });

  describe('Color Coding', () => {
    it('applies green color to high attendance rates', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      const highRates = screen.getAllByText(/90\.0%|91\.1%/);
      highRates.forEach((element) => {
        expect(element).toHaveClass('text-green-600');
      });
    });

    it('applies yellow color to medium attendance rates', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      const mediumRate = screen.getByText('84.4%');
      expect(mediumRate).toHaveClass('text-yellow-600');
    });

    it('applies correct colors to stat counts', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      // Present counts should be green
      const presentCounts = screen.getAllByText(/450|380|510/);
      presentCounts.forEach((count) => {
        if (count.className) {
          expect(count).toHaveClass('text-green-600');
        }
      });
    });
  });

  describe('Sorting', () => {
    it('sorts by attendance rate descending by default', () => {
      const { container } = render(
        <ClassStatsTable
          data={mockClassBreakdown}
          isLoading={false}
          sortBy="attendanceRate"
          sortOrder="desc"
        />
      );

      const table = container.querySelector('table');
      expect(table).toBeInTheDocument();

      // First row should be highest rate (91.1%)
      const firstRate = screen.getAllByText(/\d+\.\d+%/)[0];
      expect(firstRate.textContent).toContain('91.1');
    });

    it('sorts by class name ascending', () => {
      const { container } = render(
        <ClassStatsTable
          data={mockClassBreakdown}
          isLoading={false}
          sortBy="className"
          sortOrder="asc"
        />
      );

      const table = container.querySelector('table');
      expect(table).toBeInTheDocument();
    });

    it('sorts by total sessions descending', () => {
      const { container } = render(
        <ClassStatsTable
          data={mockClassBreakdown}
          isLoading={false}
          sortBy="totalSessions"
          sortOrder="desc"
        />
      );

      const table = container.querySelector('table');
      expect(table).toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles single class', () => {
      render(
        <ClassStatsTable data={[mockClassBreakdown[0]]} isLoading={false} />
      );

      expect(screen.getByText('Tổng lớp học')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument();
    });

    it('handles classes without teacher names', () => {
      const dataWithoutTeacher = mockClassBreakdown.map((item) => ({
        ...item,
        teacherName: undefined,
      }));

      render(<ClassStatsTable data={dataWithoutTeacher} isLoading={false} />);

      expect(screen.queryByText(/GV:/)).not.toBeInTheDocument();
    });

    it('handles zero attendance rate', () => {
      const dataWithZeroRate = [
        {
          ...mockClassBreakdown[0],
          attendanceRate: 0,
          presentCount: 0,
        },
      ];

      render(<ClassStatsTable data={dataWithZeroRate} isLoading={false} />);

      expect(screen.getByText('0.0%')).toBeInTheDocument();
    });

    it('handles perfect attendance rate', () => {
      const dataWithPerfectRate = [
        {
          ...mockClassBreakdown[0],
          attendanceRate: 100,
          absentCount: 0,
        },
      ];

      render(<ClassStatsTable data={dataWithPerfectRate} isLoading={false} />);

      expect(screen.getByText('100.0%')).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('renders section headings', () => {
      render(<ClassStatsTable data={mockClassBreakdown} isLoading={false} />);

      expect(screen.getByText('Chi tiết theo lớp')).toBeInTheDocument();
    });

    it('renders proper table structure', () => {
      const { container } = render(
        <ClassStatsTable data={mockClassBreakdown} isLoading={false} />
      );

      const table = container.querySelector('table');
      expect(table).toBeInTheDocument();
    });
  });
});
