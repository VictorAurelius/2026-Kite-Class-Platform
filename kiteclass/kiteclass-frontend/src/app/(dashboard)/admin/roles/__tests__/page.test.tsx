/**
 * Tests for the RBAC role-assignment page user picker (UX fix).
 *
 * Replaces the raw numeric "user id" input with a searchable directory picker
 * (merged teachers + students, value = entity reference id). Also resolves the
 * assignments roster `userId` → human name/email when the directory knows them.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import RoleAssignmentPage from '../page';

// Layout pulls in useAuth (useRouter + useQueryClient) — passthrough for render tests.
vi.mock('@/components/layout', () => ({
  DashboardLayout: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

const assignMutate = vi.fn();
const revokeMutate = vi.fn();
const seedMutate = vi.fn();

vi.mock('@/hooks/use-roles', () => ({
  useRoleTemplates: () => ({
    data: [
      { name: 'TEACHER', description: 'GV', seeded: true },
      { name: 'STAFF', description: 'NV', seeded: true },
    ],
    isLoading: false,
  }),
  useRoleAssignments: () => ({ data: [{ userId: 14, roles: ['TEACHER'] }], isLoading: false }),
  useSeedRoleTemplates: () => ({ mutate: seedMutate, isPending: false }),
  useAssignRole: () => ({ mutate: assignMutate, isPending: false }),
  useRevokeRole: () => ({ mutate: revokeMutate, isPending: false }),
}));

vi.mock('@/hooks/use-teachers', () => ({
  useTeachers: () => ({
    data: { content: [{ id: 14, name: 'An Nguyễn', email: 'an.nguyen+074901@skyedu.vn' }] },
    isLoading: false,
  }),
}));

vi.mock('@/hooks/use-students', () => ({
  useStudents: () => ({
    data: { content: [{ id: 167, name: 'Mai Phạm', email: 'mai.pham+074901@gmail.com' }] },
    isLoading: false,
  }),
}));

describe('RoleAssignmentPage — searchable user picker', () => {
  beforeEach(() => {
    assignMutate.mockReset();
  });

  it('renders a search input instead of a raw numeric id field', () => {
    render(<RoleAssignmentPage />);
    expect(screen.getByPlaceholderText(/Tìm theo tên hoặc email/i)).toBeInTheDocument();
    // The old raw-id placeholder must be gone.
    expect(screen.queryByPlaceholderText('VD: 1024')).not.toBeInTheDocument();
  });

  it('filters the directory by name and assigns the picked user by reference id', () => {
    render(<RoleAssignmentPage />);
    const search = screen.getByPlaceholderText(/Tìm theo tên hoặc email/i);
    fireEvent.change(search, { target: { value: 'An' } });

    // Matching option appears (teacher An Nguyễn) and student Mai Phạm filtered out.
    // Query by email — unique to the picker option (the roster revoke button's
    // aria-label carries the name but not the email).
    const option = screen.getByRole('button', { name: /an\.nguyen\+074901@skyedu\.vn/i });
    expect(option).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Mai Phạm/i })).not.toBeInTheDocument();

    fireEvent.click(option);
    fireEvent.click(screen.getByRole('button', { name: /^Gán vai trò$/i }));

    expect(assignMutate).toHaveBeenCalledTimes(1);
    expect(assignMutate).toHaveBeenCalledWith(
      expect.objectContaining({ userId: 14, roleName: 'TEACHER' }),
      expect.anything(),
    );
  });

  it('resolves the assignment roster userId to a human name when known', () => {
    render(<RoleAssignmentPage />);
    // Roster row for userId 14 should show the resolved name, not just "#14".
    expect(screen.getByText('An Nguyễn')).toBeInTheDocument();
  });
});
