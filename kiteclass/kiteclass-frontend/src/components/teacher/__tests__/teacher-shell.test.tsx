/**
 * Smoke tests for the kc-teacher production port (Wave 49 Bucket B / GAP-268).
 *
 * Verifies:
 *   - TeacherShell renders the four primary nav items + identity badge
 *   - Active state highlights the current pathname
 *   - Vietnamese-only nav labels
 *
 * @since Wave 49 Bucket B
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TeacherShell } from '../teacher-shell';

vi.mock('next/navigation', () => ({
  usePathname: () => '/teacher/attendance',
}));

// TeacherShell consumes useAuth() for its logout button. The real hook pulls in
// useRouter + useQueryClient (needs a QueryClientProvider) — out of scope for
// these render smoke tests, so stub it to just the logout affordance.
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ logout: vi.fn() }),
}));

describe('TeacherShell', () => {
  it('renders the four primary nav items in Vietnamese', () => {
    render(
      <TeacherShell>
        <p>nội dung</p>
      </TeacherShell>,
    );
    // Nav items appear twice (desktop + mobile) — getAllByRole link
    expect(screen.getAllByRole('link', { name: /Điểm danh/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /Vào điểm/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /Lịch dạy/i }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('link', { name: /Báo cáo/i }).length).toBeGreaterThan(0);
  });

  it('renders teacher identity badge', () => {
    render(
      <TeacherShell teacherName="Cô Trần Thu Hà" teacherInitials="TH">
        <p>x</p>
      </TeacherShell>,
    );
    expect(screen.getByText('Cô Trần Thu Hà')).toBeInTheDocument();
    expect(screen.getByText('TH')).toBeInTheDocument();
  });

  it('highlights the active nav link based on pathname', () => {
    render(
      <TeacherShell>
        <p>x</p>
      </TeacherShell>,
    );
    const activeLinks = screen.getAllByRole('link', { name: /Điểm danh/i });
    const activeLink = activeLinks.find((l) => l.getAttribute('aria-current') === 'page');
    expect(activeLink).toBeTruthy();
  });

  it('renders the children content', () => {
    render(
      <TeacherShell>
        <p>nội dung kiểm tra</p>
      </TeacherShell>,
    );
    expect(screen.getByText('nội dung kiểm tra')).toBeInTheDocument();
  });
});
