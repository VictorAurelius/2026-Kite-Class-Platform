/**
 * useAuth login role-redirect tests — Wave RBAC-Shell 1 Bucket A (GAP-1122).
 *
 * Verifies the login success handler routes each role to its own home (instead of
 * the previous hardcoded `/dashboard`) and normalizes BE role vocabularies first.
 *
 * @author KiteClass Team
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { AllTheProviders } from '@/test/utils';
import { useAuth } from '../useAuth';
import { useAuthStore } from '@/stores/auth-store';

const pushMock = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock, replace: vi.fn() }),
}));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast: vi.fn(), toasts: [], dismiss: vi.fn() }),
  toast: vi.fn(),
}));

const loginMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  authApi: {
    login: (...args: unknown[]) => loginMock(...args),
    logout: vi.fn(),
  },
}));

// Build a token whose middle segment is a real base64 JSON payload — useAuth
// decodes the `tenantId` claim (atob + JSON.parse) before redirecting.
function jwtWithTenant(tenantId: string): string {
  const payload = btoa(JSON.stringify({ tenantId }));
  return `aGVhZGVy.${payload}.sig`;
}

function loginResponse(role: string) {
  return {
    accessToken: jwtWithTenant('11111111-1111-1111-1111-111111111111'),
    refreshToken: 'refresh',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: { id: 1, email: 'u@test.vn', name: 'Người Dùng', role },
  };
}

describe('useAuth login role-redirect', () => {
  beforeEach(() => {
    pushMock.mockClear();
    loginMock.mockReset();
    useAuthStore.getState().clearAuth();
    localStorage.clear();
  });

  it('redirects a TEACHER to /teacher', async () => {
    loginMock.mockResolvedValue(loginResponse('TEACHER'));
    const { result } = renderHook(() => useAuth(), { wrapper: AllTheProviders });
    result.current.login({ email: 'u@test.vn', password: 'secret123' });
    await waitFor(() => expect(pushMock).toHaveBeenCalledWith('/teacher'));
    expect(useAuthStore.getState().user?.userType).toBe('TEACHER');
  });

  it('redirects a PARENT to /parent', async () => {
    loginMock.mockResolvedValue(loginResponse('PARENT'));
    const { result } = renderHook(() => useAuth(), { wrapper: AllTheProviders });
    result.current.login({ email: 'p@test.vn', password: 'secret123' });
    await waitFor(() => expect(pushMock).toHaveBeenCalledWith('/parent'));
  });

  it('normalizes a BE hierarchical role and redirects accordingly', async () => {
    loginMock.mockResolvedValue(loginResponse('TENANT_OWNER'));
    const { result } = renderHook(() => useAuth(), { wrapper: AllTheProviders });
    result.current.login({ email: 'o@test.vn', password: 'secret123' });
    await waitFor(() => expect(pushMock).toHaveBeenCalledWith('/dashboard'));
    expect(useAuthStore.getState().user?.userType).toBe('OWNER');
  });
});
