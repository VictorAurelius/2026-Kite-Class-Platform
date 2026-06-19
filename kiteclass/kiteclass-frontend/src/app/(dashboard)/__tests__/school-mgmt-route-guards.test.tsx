/**
 * School-management route-guard tests — Wave RBAC-Shell 1 Bucket B (GAP-1130).
 *
 * Bucket A (GAP-1122) guarded only the leaf groups (`/admin`, `(teacher)`,
 * `/parent`, `/student`). The OWNER/STAFF school-management routes under
 * `(dashboard)` (`/courses`, `/teachers`, `/students`, `/classes`, `/billing`,
 * `/reports`, `/branding`, `/settings`, `/attendance`, `/overview`) plus the
 * `app/dashboard` route-home only inherited the outer auth check — a logged-in
 * TEACHER / PARENT / STUDENT could reach them by typing the URL
 * (IDOR-by-navigation). Each route now ships a leaf `layout.tsx` wrapping its
 * subtree in <RoleGuard>. These tests assert the allow-lists actually bounce the
 * wrong role and admit the right one.
 *
 * @author KiteClass Team
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';

// Staff-shared routes (allow OWNER/STAFF/ADMIN)
import CoursesLayout from '../courses/layout';
import StudentsLayout from '../students/layout';
import ClassesLayout from '../classes/layout';
import BillingLayout from '../billing/layout';
import AttendanceLayout from '../attendance/layout';
import SettingsLayout from '../settings/layout';
import OverviewLayout from '../overview/layout';
// Owner-only routes (allow OWNER/ADMIN — STAFF blocked, AC #3)
import TeachersLayout from '../teachers/layout';
import BrandingLayout from '../branding/layout';
import ReportsLayout from '../reports/layout';
// Route-home outside the (dashboard) group
import DashboardHomeLayout from '../../dashboard/layout';

const replaceMock = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: replaceMock, push: vi.fn() }),
}));

function seedAuth(userType: UserType | undefined, authed = true) {
  useAuthStore.setState({
    user: userType
      ? { id: 1, email: 'u@test.vn', name: 'Người Dùng', userType }
      : null,
    accessToken: authed ? 'header.payload.sig' : null,
    refreshToken: authed ? 'refresh' : null,
    tenantId: 't',
    isAuthenticated: authed,
  });
}

const MARKER = 'nội dung trang quản trị';

const STAFF_SHARED: Array<[string, React.ComponentType<{ children: React.ReactNode }>]> = [
  ['CoursesLayout', CoursesLayout],
  ['StudentsLayout', StudentsLayout],
  ['ClassesLayout', ClassesLayout],
  ['BillingLayout', BillingLayout],
  ['AttendanceLayout', AttendanceLayout],
  ['SettingsLayout', SettingsLayout],
  ['OverviewLayout', OverviewLayout],
  ['DashboardHomeLayout', DashboardHomeLayout],
];

const OWNER_ONLY: Array<[string, React.ComponentType<{ children: React.ReactNode }>]> = [
  ['TeachersLayout', TeachersLayout],
  ['BrandingLayout', BrandingLayout],
  ['ReportsLayout', ReportsLayout],
];

describe('GAP-1130 school-management route guards', () => {
  beforeEach(() => {
    replaceMock.mockClear();
    useAuthStore.getState().clearAuth();
  });

  describe.each(STAFF_SHARED)('%s (OWNER/STAFF/ADMIN)', (_name, Layout) => {
    it('renders for STAFF', async () => {
      seedAuth(UserType.STAFF);
      render(<Layout><div>{MARKER}</div></Layout>);
      expect(await screen.findByText(MARKER)).toBeInTheDocument();
      expect(replaceMock).not.toHaveBeenCalled();
    });

    it('bounces TEACHER to /teacher (IDOR-by-nav blocked)', async () => {
      seedAuth(UserType.TEACHER);
      render(<Layout><div>{MARKER}</div></Layout>);
      await waitFor(() => expect(replaceMock).toHaveBeenCalledWith('/teacher'));
      expect(screen.queryByText(MARKER)).not.toBeInTheDocument();
    });

    it('bounces STUDENT to /student (IDOR-by-nav blocked)', async () => {
      seedAuth(UserType.STUDENT);
      render(<Layout><div>{MARKER}</div></Layout>);
      await waitFor(() => expect(replaceMock).toHaveBeenCalledWith('/student'));
      expect(screen.queryByText(MARKER)).not.toBeInTheDocument();
    });
  });

  describe.each(OWNER_ONLY)('%s (OWNER/ADMIN only)', (_name, Layout) => {
    it('renders for OWNER', async () => {
      seedAuth(UserType.OWNER);
      render(<Layout><div>{MARKER}</div></Layout>);
      expect(await screen.findByText(MARKER)).toBeInTheDocument();
      expect(replaceMock).not.toHaveBeenCalled();
    });

    it('bounces STAFF to /dashboard (AC #3 — owner-only)', async () => {
      seedAuth(UserType.STAFF);
      render(<Layout><div>{MARKER}</div></Layout>);
      await waitFor(() => expect(replaceMock).toHaveBeenCalledWith('/dashboard'));
      expect(screen.queryByText(MARKER)).not.toBeInTheDocument();
    });

    it('bounces TEACHER to /teacher', async () => {
      seedAuth(UserType.TEACHER);
      render(<Layout><div>{MARKER}</div></Layout>);
      await waitFor(() => expect(replaceMock).toHaveBeenCalledWith('/teacher'));
      expect(screen.queryByText(MARKER)).not.toBeInTheDocument();
    });
  });
});
