/**
 * Wave 30 Bucket B — token-application smoke test for ClassesPage.
 *
 * The legacy integration suite (`classes-list.integration.test.tsx`) is
 * `describe.skip`'d due to Radix Select / JSDOM PointerCapture incompat. This
 * lightweight render test verifies the kc-pro-v2 design-token classes are
 * actually present in the markup so the kit-port doesn't silently revert.
 *
 * @since Wave 30 (2026-05-06)
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import ClassesPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  usePathname: () => '/classes',
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock('@/components/layout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

// Stub useCourses / useClasses to avoid MSW dance for this smoke test.
vi.mock('@/hooks/use-courses', () => ({
  useCourses: () => ({ data: { content: [] }, isLoading: false, error: null }),
}));
vi.mock('@/hooks/use-classes', () => ({
  useClasses: () => ({ data: undefined, isLoading: false, error: null }),
  useDeleteClass: () => ({ mutate: vi.fn(), isPending: false }),
}));

describe('ClassesPage — kc-pro-v2 token application (Wave 30 Bucket B)', () => {
  it('renders the page heading with kc-pro-v2 typography tokens', () => {
    render(<ClassesPage />);
    const heading = screen.getByRole('heading', { name: /lớp học/i, level: 1 });
    expect(heading).toBeInTheDocument();
    // Token check: kc-pro-v2 spec uses tracking-tight + font-semibold (NOT font-bold).
    expect(heading.className).toMatch(/tracking-tight/);
    expect(heading.className).toMatch(/font-semibold/);
  });

  it('renders the subtitle with muted-foreground token', () => {
    render(<ClassesPage />);
    const subtitle = screen.getByText(
      /quản lý danh sách lớp học theo từng khóa học/i,
    );
    expect(subtitle.className).toMatch(/text-muted-foreground/);
  });
});
