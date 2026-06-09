/**
 * RoleGuard tests — KiteClass route-group RBAC (Wave RBAC-Shell 1 Bucket A, GAP-1122).
 *
 * RoleGuard is the security fix for IDOR-by-navigation: before Bucket A, any
 * authenticated user could reach any route group (e.g. a teacher typing `/admin`).
 * RoleGuard requires the actor's normalized role to be in the route's allow-list,
 * else it bounces them to their own role-home.
 *
 * @author KiteClass Team
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { RoleGuard } from '../role-guard';
import { useAuthStore } from '@/stores/auth-store';
import { UserType } from '@/types/auth';

const replaceMock = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: replaceMock, push: vi.fn() }),
}));

function seedAuth(userType: UserType | string | undefined, authed = true) {
  useAuthStore.setState({
    user: userType
      ? { id: 1, email: 'u@test.vn', name: 'Người Dùng', userType: userType as UserType }
      : null,
    accessToken: authed ? 'header.payload.sig' : null,
    refreshToken: authed ? 'refresh' : null,
    tenantId: 't',
    isAuthenticated: authed,
  });
}

describe('RoleGuard', () => {
  beforeEach(() => {
    replaceMock.mockClear();
    useAuthStore.getState().clearAuth();
  });

  it('renders children when the role is allowed', async () => {
    seedAuth(UserType.TEACHER);
    render(
      <RoleGuard allow={[UserType.TEACHER]}>
        <div>nội dung bảo vệ</div>
      </RoleGuard>,
    );
    expect(await screen.findByText('nội dung bảo vệ')).toBeInTheDocument();
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it('redirects to /login when not authenticated', async () => {
    seedAuth(undefined, false);
    render(
      <RoleGuard allow={[UserType.TEACHER]}>
        <div>nội dung bảo vệ</div>
      </RoleGuard>,
    );
    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith('/login'));
    expect(screen.queryByText('nội dung bảo vệ')).not.toBeInTheDocument();
  });

  it('bounces a non-allowed role to its own role-home (IDOR-by-navigation guard)', async () => {
    seedAuth(UserType.TEACHER);
    render(
      <RoleGuard allow={[UserType.OWNER, UserType.ADMIN]}>
        <div>chỉ dành cho admin</div>
      </RoleGuard>,
    );
    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith('/teacher'));
    expect(screen.queryByText('chỉ dành cho admin')).not.toBeInTheDocument();
  });

  it('normalizes BE hierarchical role literals before checking access', async () => {
    // BE may hand the FE a hierarchical role name (ADR-003) — RoleGuard must
    // normalize SUBJECT_TEACHER -> TEACHER before deciding.
    seedAuth('SUBJECT_TEACHER');
    render(
      <RoleGuard allow={[UserType.TEACHER]}>
        <div>nội dung bảo vệ</div>
      </RoleGuard>,
    );
    expect(await screen.findByText('nội dung bảo vệ')).toBeInTheDocument();
    expect(replaceMock).not.toHaveBeenCalled();
  });
});
