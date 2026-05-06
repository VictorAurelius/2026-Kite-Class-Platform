/**
 * Integration smoke test — (dashboard)/page.tsx renders KPI grid using foundation.
 *
 * Verifies:
 *   - Page renders without throwing
 *   - KPI grid renders 6 cards
 *   - Each KPI card has a sparkline
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

import DashboardHomePage from '@/app/(dashboard)/overview/page';

describe('(dashboard)/overview/page.tsx integration', () => {
  it('renders the page header', () => {
    render(<DashboardHomePage />);
    expect(screen.getByRole('heading', { name: /Tổng quan/ })).toBeInTheDocument();
  });

  it('renders six KPI cards', () => {
    render(<DashboardHomePage />);
    expect(screen.getAllByTestId('kpi-card')).toHaveLength(6);
  });

  it('renders one sparkline per KPI card', () => {
    render(<DashboardHomePage />);
    expect(screen.getAllByTestId('sparkline')).toHaveLength(6);
  });
});
