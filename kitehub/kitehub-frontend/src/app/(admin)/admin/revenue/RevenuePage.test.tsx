/**
 * Admin Revenue Page Tests (GAP-1441).
 *
 * Verifies the revenue page is wired to `useAdminRevenue` and renders real
 * data from the backend RevenueReport shape (not the old hardcoded "0đ" stub).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@/test/test-utils';
import RevenuePage from './page';
import { mockRevenueReport } from '@/test/mocks/admin-data';

const mockUseAdminRevenue = vi.fn();

vi.mock('@/hooks/use-admin', () => ({
  useAdminRevenue: () => mockUseAdminRevenue(),
}));

describe('RevenuePage (GAP-1441)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAdminRevenue.mockReturnValue({
      data: mockRevenueReport,
      isLoading: false,
      error: null,
    });
  });

  it('renders the page title', () => {
    render(<RevenuePage />);
    expect(screen.getByText('Doanh thu')).toBeInTheDocument();
  });

  it('renders real totalRevenue + MRR from the hook (not hardcoded 0đ)', () => {
    render(<RevenuePage />);
    // totalRevenue = mrr = 89.000.000 → at least two matches
    expect(screen.getAllByText(/89\.000\.000/).length).toBeGreaterThan(0);
    // The old stub hardcoded "0đ" — ensure it is gone.
    expect(screen.queryByText('0đ')).not.toBeInTheDocument();
  });

  it('renders revenue-by-tier breakdown', () => {
    render(<RevenuePage />);
    expect(screen.getByText(/BASIC/)).toBeInTheDocument();
    expect(screen.getByText(/PREMIUM/)).toBeInTheDocument();
  });

  it('shows loading skeleton while fetching', () => {
    mockUseAdminRevenue.mockReturnValue({ data: undefined, isLoading: true, error: null });
    render(<RevenuePage />);
    const skeletons = document.querySelectorAll('[class*="animate-pulse"]');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('shows error message on failure', () => {
    mockUseAdminRevenue.mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('boom'),
    });
    render(<RevenuePage />);
    expect(screen.getByText(/lỗi/i)).toBeInTheDocument();
  });

  it('shows empty chart state when no daily revenue', () => {
    mockUseAdminRevenue.mockReturnValue({
      data: { ...mockRevenueReport, dailyRevenue: [], revenueByTier: [] },
      isLoading: false,
      error: null,
    });
    render(<RevenuePage />);
    expect(screen.getByText(/Chưa có dữ liệu doanh thu/)).toBeInTheDocument();
  });
});
