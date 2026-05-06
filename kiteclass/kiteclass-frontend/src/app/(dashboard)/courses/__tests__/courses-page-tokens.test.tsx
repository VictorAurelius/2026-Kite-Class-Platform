/**
 * Wave 30 Bucket B — token-application smoke test for CoursesPage.
 *
 * Lightweight render test verifying kc-pro-v2 design tokens applied so the
 * kit-port doesn't silently revert. Existing integration suite is
 * `describe.skip`'d.
 *
 * @since Wave 30 (2026-05-06)
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@/test/utils';
import CoursesPage from '../page';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn() }),
  usePathname: () => '/courses',
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock('@/components/layout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock('@/hooks/use-courses', () => ({
  useCourses: () => ({ data: undefined, isLoading: false, error: null }),
  useDeleteCourse: () => ({ mutate: vi.fn(), isPending: false }),
}));

describe('CoursesPage — kc-pro-v2 token application (Wave 30 Bucket B)', () => {
  it('renders the page heading with kc-pro-v2 typography tokens', () => {
    render(<CoursesPage />);
    const heading = screen.getByRole('heading', {
      name: /khóa học/i,
      level: 1,
    });
    expect(heading).toBeInTheDocument();
    expect(heading.className).toMatch(/tracking-tight/);
    expect(heading.className).toMatch(/font-semibold/);
  });

  it('renders the subtitle with muted-foreground token', () => {
    render(<CoursesPage />);
    const subtitle = screen.getByText(
      /quản lý danh sách khóa học của trung tâm/i,
    );
    expect(subtitle.className).toMatch(/text-muted-foreground/);
  });

  it('renders the "Thêm khóa học" CTA link', () => {
    render(<CoursesPage />);
    const link = screen.getByRole('link', { name: /thêm khóa học/i });
    expect(link).toHaveAttribute('href', '/courses/new');
  });
});
