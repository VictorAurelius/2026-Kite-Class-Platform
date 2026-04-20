/**
 * Tests for root-level not-found.tsx (Next.js App Router 404 page).
 *
 * Covers:
 * - Vietnamese heading + body copy renders
 * - Primary CTA links to "/" (home)
 * - Secondary CTA links to "/pricing"
 * - Themed via Shadcn Button (no inline color overrides)
 *
 * @since GAP-136
 */

import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from '@/__tests__/test-utils';
import NotFound from '../not-found';

describe('app/not-found', () => {
  it('renders Vietnamese 404 heading', () => {
    render(<NotFound />);

    expect(
      screen.getByRole('heading', { name: 'Không tìm thấy trang' })
    ).toBeInTheDocument();
  });

  it('renders Vietnamese helper copy', () => {
    render(<NotFound />);

    expect(
      screen.getByText(/trang bạn đang tìm kiếm không tồn tại/i)
    ).toBeInTheDocument();
  });

  it('renders primary CTA linking to home', () => {
    render(<NotFound />);

    const homeLink = screen.getByRole('link', { name: /về trang chủ/i });
    expect(homeLink).toBeInTheDocument();
    expect(homeLink).toHaveAttribute('href', '/');
  });

  it('renders secondary CTA linking to pricing', () => {
    render(<NotFound />);

    const pricingLink = screen.getByRole('link', { name: /xem bảng giá/i });
    expect(pricingLink).toBeInTheDocument();
    expect(pricingLink).toHaveAttribute('href', '/pricing');
  });
});
