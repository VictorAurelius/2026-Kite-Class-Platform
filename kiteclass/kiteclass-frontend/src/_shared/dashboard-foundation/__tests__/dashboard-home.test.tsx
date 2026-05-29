/**
 * Integration smoke test — (dashboard)/overview/page.tsx renders KPI grid.
 *
 * GAP-805 Bucket C (2026-05-28): page rewritten to derive KPI counts from real
 * API data (useStudents / useCourses react-query hooks) instead of hardcoded
 * literals. Test now wraps in QueryClientProvider + mocks the hooks so the
 * smoke test stays deterministic without a live backend.
 *
 * Verifies:
 *   - Page renders without throwing
 *   - KPI grid renders 6 cards
 *   - Real-data counts surface from the mocked hooks (not hardcoded literals)
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

// Mock the data hooks so the page renders deterministic counts.
vi.mock('@/hooks/use-students', () => ({
  useStudents: () => ({ data: { totalElements: 428 }, isLoading: false, isError: false }),
}));
vi.mock('@/hooks/use-courses', () => ({
  useCourses: () => ({ data: { totalElements: 24 }, isLoading: false, isError: false }),
}));

import DashboardHomePage from '@/app/(dashboard)/overview/page';

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <DashboardHomePage />
    </QueryClientProvider>,
  );
}

describe('(dashboard)/overview/page.tsx integration', () => {
  it('renders the page header', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /Tổng quan/ })).toBeInTheDocument();
  });

  it('renders six KPI cards', () => {
    renderPage();
    expect(screen.getAllByTestId('kpi-card')).toHaveLength(6);
  });

  it('surfaces real-data counts from the mocked hooks', () => {
    renderPage();
    // Student total (428) + course total (24) come from mocked hooks, not literals.
    expect(screen.getByText('428')).toBeInTheDocument();
    expect(screen.getByText('24')).toBeInTheDocument();
  });
});
