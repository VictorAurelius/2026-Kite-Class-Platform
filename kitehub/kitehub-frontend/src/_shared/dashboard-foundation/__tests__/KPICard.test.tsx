/**
 * KPICard tests — Wave 31 Bucket A.
 */

import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { KPICard } from '../KPICard';

describe('KPICard (KH foundation)', () => {
  it('renders label and value', () => {
    render(<KPICard label="Doanh thu" value="82,4M đ" tone="positive" />);
    expect(screen.getByText('Doanh thu')).toBeInTheDocument();
    expect(screen.getByText('82,4M đ')).toBeInTheDocument();
  });

  it('formats positive delta with leading +', () => {
    render(<KPICard label="API" value="1620" delta={24.4} tone="positive" />);
    expect(screen.getByTestId('kpi-delta')).toHaveTextContent('+24.4%');
  });

  it('omits delta when not provided', () => {
    render(<KPICard label="Quota" value="7/10" tone="neutral" />);
    expect(screen.queryByTestId('kpi-delta')).not.toBeInTheDocument();
  });

  it('renders an SVG sparkline when sparkline data is provided', () => {
    render(
      <KPICard
        label="Lớp đang vận hành"
        value="62"
        delta={8.2}
        sparkline={[42, 48, 51, 47, 55, 58, 62]}
        tone="positive"
      />,
    );
    expect(screen.getByTestId('sparkline')).toBeInTheDocument();
  });
});
