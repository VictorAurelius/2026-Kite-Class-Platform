/**
 * Sparkline tests — covers empty state + normal data path.
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Sparkline } from '../Sparkline';

describe('Sparkline', () => {
  it('renders dashed baseline when data is empty', () => {
    render(<Sparkline data={[]} />);
    const empty = screen.getByTestId('sparkline-empty');
    expect(empty).toBeInTheDocument();
    expect(empty.querySelector('line')).not.toBeNull();
  });

  it('renders an SVG path when data has values', () => {
    render(<Sparkline data={[1, 3, 2, 5, 4]} />);
    const svg = screen.getByTestId('sparkline');
    const path = svg.querySelector('path');
    expect(path).not.toBeNull();
    // Path command starts at M and includes L segments for subsequent points.
    expect(path?.getAttribute('d') ?? '').toMatch(/^M /);
    expect(path?.getAttribute('d') ?? '').toContain('L ');
  });

  it('respects width and height props', () => {
    render(<Sparkline data={[1, 2, 3]} width={120} height={40} />);
    const svg = screen.getByTestId('sparkline');
    expect(svg.getAttribute('width')).toBe('120');
    expect(svg.getAttribute('height')).toBe('40');
  });
});
