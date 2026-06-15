/**
 * Tests for the gated RBAC role-overview page (Phase 1 BETA).
 *
 * The interactive assign/revoke UI was removed because user_roles is not consumed
 * by authz (deferred to Phase 3 per GAP-1119). The page is now a read-only overview
 * + an explicit Phase-3 notice. These tests pin that gated contract so the inert
 * assign form does not silently return.
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import RoleOverviewPage from '../page';

// Layout pulls in useAuth (useRouter + useQueryClient) — passthrough for render tests.
vi.mock('@/components/layout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

describe('RoleOverviewPage — gated for Phase 1 BETA', () => {
  it('shows the Phase-3 / auto-assign notice', () => {
    render(<RoleOverviewPage />);
    expect(
      screen.getByText(/Vai trò được gán tự động khi tạo tài khoản/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/Phase 3/i)).toBeInTheDocument();
  });

  it('lists the 5 fixed roles read-only with how each is granted', () => {
    render(<RoleOverviewPage />);
    ['Chủ trung tâm', 'Nhân viên', 'Giáo viên', 'Phụ huynh', 'Học sinh'].forEach((label) => {
      expect(screen.getByText(label)).toBeInTheDocument();
    });
    expect(screen.getAllByText(/Cách gán:/i)).toHaveLength(5);
  });

  it('does NOT render the inert assign form (no user picker, no assign button)', () => {
    render(<RoleOverviewPage />);
    expect(screen.queryByPlaceholderText(/Tìm theo tên hoặc email/i)).not.toBeInTheDocument();
    expect(screen.queryByPlaceholderText('VD: 1024')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Gán vai trò$/i })).not.toBeInTheDocument();
  });
});
