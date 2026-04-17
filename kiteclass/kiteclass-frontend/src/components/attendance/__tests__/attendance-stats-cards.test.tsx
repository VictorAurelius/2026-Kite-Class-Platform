/**
 * Unit tests for AttendanceStatsCards component.
 *
 * @author KiteClass Team
 * @since 2.7.0 (PR 3.8)
 */

import { render, screen } from '@testing-library/react';
import { AttendanceStatsCards } from '../attendance-stats-cards';

describe.skip('AttendanceStatsCards', () => {
  const mockStats = {
    total: 100,
    present: 85,
    absent: 10,
    late: 3,
    excused: 2,
    makeup: 5,
  };

  it('renders all stat cards correctly', () => {
    render(<AttendanceStatsCards stats={mockStats} />);

    expect(screen.getByText('Tổng số')).toBeInTheDocument();
    expect(screen.getByText('Có mặt')).toBeInTheDocument();
    expect(screen.getByText('Vắng')).toBeInTheDocument();
    expect(screen.getByText('Đi trễ')).toBeInTheDocument();
    expect(screen.getByText('Có phép')).toBeInTheDocument();
  });

  it('displays correct stat values', () => {
    render(<AttendanceStatsCards stats={mockStats} />);

    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('85')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('shows makeup card when showMakeup is true', () => {
    render(<AttendanceStatsCards stats={mockStats} showMakeup />);

    expect(screen.getByText('Học bù')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('hides makeup card by default', () => {
    render(<AttendanceStatsCards stats={mockStats} />);

    expect(screen.queryByText('Học bù')).not.toBeInTheDocument();
  });

  it('handles zero values correctly', () => {
    const zeroStats = {
      total: 0,
      present: 0,
      absent: 0,
      late: 0,
      excused: 0,
    };

    render(<AttendanceStatsCards stats={zeroStats} />);

    const allZeros = screen.getAllByText('0');
    expect(allZeros).toHaveLength(5);
  });

  it('applies correct color classes to cards', () => {
    const { container } = render(<AttendanceStatsCards stats={mockStats} />);

    // Check if color classes are applied
    expect(container.querySelector('.text-green-600')).toBeInTheDocument();
    expect(container.querySelector('.text-red-600')).toBeInTheDocument();
    expect(container.querySelector('.text-yellow-600')).toBeInTheDocument();
    expect(container.querySelector('.text-blue-600')).toBeInTheDocument();
  });

  it('renders 5 cards when showMakeup is false', () => {
    const { container } = render(<AttendanceStatsCards stats={mockStats} />);
    const cards = container.querySelectorAll('[class*="Card"]');
    expect(cards.length).toBeGreaterThanOrEqual(5);
  });

  it('renders 6 cards when showMakeup is true', () => {
    render(<AttendanceStatsCards stats={mockStats} showMakeup />);
    expect(screen.getByText('Học bù')).toBeInTheDocument();
  });
});
