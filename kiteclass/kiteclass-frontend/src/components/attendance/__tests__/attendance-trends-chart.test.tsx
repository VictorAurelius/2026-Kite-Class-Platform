/**
 * AttendanceTrendsChart component tests.
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AttendanceTrendsChart } from '../attendance-trends-chart';
import { mockAttendanceTrends } from '@/__tests__/fixtures/attendance';

describe('AttendanceTrendsChart', () => {
  describe('Rendering', () => {
    it('renders chart card', () => {
      render(<AttendanceTrendsChart data={mockAttendanceTrends} />);

      expect(screen.getByText('Xu hướng điểm danh')).toBeInTheDocument();
    });

    it('renders SVG element', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('renders chart with data', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      // Check for path element (line)
      const path = container.querySelector('path[fill="none"]');
      expect(path).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('shows empty state when no data', () => {
      render(<AttendanceTrendsChart data={[]} />);

      expect(screen.getByText('Chưa có đủ dữ liệu để hiển thị xu hướng')).toBeInTheDocument();
    });

    it('shows empty state icon', () => {
      render(<AttendanceTrendsChart data={[]} />);

      expect(screen.getByText('📈')).toBeInTheDocument();
    });

    it('does not render SVG when no data', () => {
      const { container } = render(<AttendanceTrendsChart data={[]} />);

      const svg = container.querySelector('svg');
      expect(svg).not.toBeInTheDocument();
    });
  });

  describe('Data Points', () => {
    it('renders data point circles', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const circles = container.querySelectorAll('circle');
      expect(circles.length).toBe(mockAttendanceTrends.length);
    });

    it('renders tooltips on data points', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const circles = container.querySelectorAll('circle');
      circles.forEach((circle, index) => {
        const title = circle.querySelector('title');
        expect(title).toBeInTheDocument();
        expect(title?.textContent).toContain(
          mockAttendanceTrends[index].attendanceRate.toFixed(1)
        );
      });
    });
  });

  describe('Grid Lines', () => {
    it('renders grid lines when showGrid is true', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} showGrid={true} />
      );

      const gridLines = container.querySelectorAll('line[stroke-dasharray]');
      expect(gridLines.length).toBeGreaterThan(0);
    });

    it('does not render grid lines when showGrid is false', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} showGrid={false} />
      );

      const gridLines = container.querySelectorAll('line[stroke-dasharray]');
      expect(gridLines.length).toBe(0);
    });
  });

  describe('Axis Labels', () => {
    it('renders Y-axis labels', () => {
      render(<AttendanceTrendsChart data={mockAttendanceTrends} />);

      expect(screen.getByText('0%')).toBeInTheDocument();
      expect(screen.getByText('25%')).toBeInTheDocument();
      expect(screen.getByText('50%')).toBeInTheDocument();
      expect(screen.getByText('75%')).toBeInTheDocument();
      expect(screen.getByText('100%')).toBeInTheDocument();
    });

    it('renders X-axis date labels', () => {
      render(<AttendanceTrendsChart data={mockAttendanceTrends} />);

      // Should show some dates in dd/mm format
      const dateLabels = screen.getAllByText(/\d{2}\/\d{2}/);
      expect(dateLabels.length).toBeGreaterThan(0);
    });
  });

  describe('Legend', () => {
    it('renders legend', () => {
      render(<AttendanceTrendsChart data={mockAttendanceTrends} />);

      expect(screen.getByText('Tỷ lệ điểm danh')).toBeInTheDocument();
    });

    it('renders legend color indicator', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const indicator = container.querySelector('.bg-green-500');
      expect(indicator).toBeInTheDocument();
    });
  });

  describe('Height Customization', () => {
    it('applies custom height', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} height={400} />
      );

      const chartContainer = container.querySelector('[style*="height: 400px"]');
      expect(chartContainer).toBeInTheDocument();
    });

    it('uses default height when not specified', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const chartContainer = container.querySelector('[style*="height: 300px"]');
      expect(chartContainer).toBeInTheDocument();
    });
  });

  describe('Line and Area', () => {
    it('renders line path', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const linePath = container.querySelector('path[fill="none"][stroke]');
      expect(linePath).toBeInTheDocument();
      expect(linePath?.getAttribute('stroke')).toContain('rgb(34, 197, 94)'); // green
    });

    it('renders area gradient', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const areaPath = container.querySelector('path[fill*="url(#areaGradient)"]');
      expect(areaPath).toBeInTheDocument();
    });

    it('defines gradient', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const gradient = container.querySelector('linearGradient#areaGradient');
      expect(gradient).toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles single data point', () => {
      const { container } = render(
        <AttendanceTrendsChart data={[mockAttendanceTrends[0]]} />
      );

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();

      const circles = container.querySelectorAll('circle');
      expect(circles.length).toBe(1);
    });

    it('handles two data points', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends.slice(0, 2)} />
      );

      const circles = container.querySelectorAll('circle');
      expect(circles.length).toBe(2);
    });

    it('handles many data points', () => {
      const manyPoints = Array.from({ length: 30 }, (_, i) => ({
        date: `2026-03-${String(i + 1).padStart(2, '0')}`,
        attendanceRate: 80 + Math.random() * 20,
        presentCount: 40 + Math.floor(Math.random() * 10),
        totalSessions: 50,
      }));

      const { container } = render(<AttendanceTrendsChart data={manyPoints} />);

      const circles = container.querySelectorAll('circle');
      expect(circles.length).toBe(30);
    });

    it('handles zero attendance rates', () => {
      const dataWithZero = [
        { ...mockAttendanceTrends[0], attendanceRate: 0 },
      ];

      const { container } = render(<AttendanceTrendsChart data={dataWithZero} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('handles 100% attendance rates', () => {
      const dataWithPerfect = [
        { ...mockAttendanceTrends[0], attendanceRate: 100 },
      ];

      const { container } = render(<AttendanceTrendsChart data={dataWithPerfect} />);

      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
    });

    it('handles varying attendance rates', () => {
      const varyingData = [
        { ...mockAttendanceTrends[0], attendanceRate: 20 },
        { ...mockAttendanceTrends[1], attendanceRate: 50 },
        { ...mockAttendanceTrends[2], attendanceRate: 80 },
        { ...mockAttendanceTrends[3], attendanceRate: 95 },
      ];

      const { container } = render(<AttendanceTrendsChart data={varyingData} />);

      const path = container.querySelector('path[fill="none"]');
      expect(path).toBeInTheDocument();
      expect(path?.getAttribute('d')).toContain('M'); // Has path data
    });
  });

  describe('Responsiveness', () => {
    it('uses viewBox for responsive SVG', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const svg = container.querySelector('svg');
      expect(svg?.getAttribute('viewBox')).toBe('0 0 100 100');
      expect(svg?.getAttribute('preserveAspectRatio')).toBe('none');
    });

    it('applies responsive classes', () => {
      const { container } = render(
        <AttendanceTrendsChart data={mockAttendanceTrends} />
      );

      const svg = container.querySelector('svg');
      expect(svg?.className.baseVal).toContain('h-full');
      expect(svg?.className.baseVal).toContain('w-full');
    });
  });

  describe('Date Formatting', () => {
    it('formats dates for X-axis labels', () => {
      render(<AttendanceTrendsChart data={mockAttendanceTrends} />);

      // Check for Vietnamese date format in labels
      const dateLabels = screen.getAllByText(/\d{2}\/\d{2}/);
      expect(dateLabels.length).toBeGreaterThan(0);
    });

    it('limits number of X-axis labels', () => {
      const manyPoints = Array.from({ length: 30 }, (_, i) => ({
        date: `2026-03-${String(i + 1).padStart(2, '0')}`,
        attendanceRate: 85,
        presentCount: 40,
        totalSessions: 50,
      }));

      render(<AttendanceTrendsChart data={manyPoints} />);

      // Should show max 7 labels even with 30 data points
      const dateLabels = screen.getAllByText(/\d{2}\/\d{2}/);
      expect(dateLabels.length).toBeLessThanOrEqual(7);
    });
  });
});
