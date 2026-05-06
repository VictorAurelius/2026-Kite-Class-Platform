/**
 * Sparkline tests — Wave 31 Bucket A foundation primitive smoke tests.
 *
 * Mirrors Wave 30 KC test surface with KH-shaped fixtures.
 */

import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import { Sparkline } from '../Sparkline';

describe('Sparkline (KH foundation)', () => {
  it('renders empty skeleton when data is empty', () => {
    render(<Sparkline data={[]} />);
    expect(screen.getByTestId('sparkline-empty')).toBeInTheDocument();
  });

  it('renders SVG path when data points are provided', () => {
    render(<Sparkline data={[1, 5, 3, 8, 4]} />);
    const svg = screen.getByTestId('sparkline');
    expect(svg).toBeInTheDocument();
    // Sparkline contains a single path child for the line
    expect(svg.querySelector('path')).toBeInTheDocument();
  });

  it('uses provided ariaLabel for screen readers', () => {
    render(<Sparkline data={[1, 2, 3]} ariaLabel="Trend doanh thu" />);
    expect(screen.getByLabelText('Trend doanh thu')).toBeInTheDocument();
  });
});
