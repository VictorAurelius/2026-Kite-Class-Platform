/**
 * KPICard tests — verify rendering of label / value / delta / sparkline slot.
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { KPICard } from '../KPICard';

describe('KPICard', () => {
  it('renders label and value', () => {
    render(<KPICard label="Học viên" value="428" />);
    expect(screen.getByText('Học viên')).toBeInTheDocument();
    expect(screen.getByText('428')).toBeInTheDocument();
  });

  it('formats positive delta with leading + and percent', () => {
    render(<KPICard label="Doanh thu" value="₫42M" delta={8.2} tone="positive" />);
    expect(screen.getByTestId('kpi-delta')).toHaveTextContent('+8.2%');
  });

  it('formats negative delta without extra sign', () => {
    render(<KPICard label="Vắng" value="3" delta={-1.5} tone="negative" />);
    expect(screen.getByTestId('kpi-delta')).toHaveTextContent('-1.5%');
  });

  it('renders sparkline when sparkline data provided', () => {
    render(<KPICard label="Lớp" value="12" sparkline={[1, 2, 3]} />);
    expect(screen.getByTestId('sparkline')).toBeInTheDocument();
  });
});
