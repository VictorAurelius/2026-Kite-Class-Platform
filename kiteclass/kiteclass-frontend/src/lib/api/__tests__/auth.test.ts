/**
 * Authentication API Tests
 *
 * @author KiteClass Team
 * @since 2026-02-23
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { authApi } from '../auth';
import { apiClient } from '@/lib/api-client';
import type { LoginRequest, AuthResponse } from '@/types/auth';

// Bare login client (axios.create) used by authApi.login — see auth.ts. Hoisted
// so the module-load `axios.create(...)` returns this mock. GAP-1416: auth.ts now
// registers a request interceptor (per-request host-preserved baseURL), so the
// mock must expose `interceptors.request.use` or module load throws.
const { loginPost } = vi.hoisted(() => ({ loginPost: vi.fn() }));
vi.mock('axios', () => ({
  default: {
    create: () => ({ post: loginPost, interceptors: { request: { use: vi.fn() } } }),
  },
}));

// Mock apiClient (logout / refresh / forgot / reset / verify still route through it)
vi.mock('@/lib/api-client', () => ({
  apiClient: {
    post: vi.fn(),
  },
}));

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    loginPost.mockReset();
  });

  describe('login', () => {
    // Wave RBAC-Shell 1 (GAP-1122): KC :3000 probes KC-native tenant-auth first
    // (TEACHER/PARENT/STUDENT) then falls back to KH owner/staff login.
    it('authenticates a tenant role via KC tenant-auth and adapts the shape', async () => {
      const credentials: LoginRequest = { email: 'teacher_a@test.com', password: 'Walk@1234' };

      // ApiResponse-wrapped tenant-auth payload (role + referenceId + tenantId).
      loginPost.mockResolvedValueOnce({
        data: {
          success: true,
          data: {
            accessToken: 'kc-access',
            tokenType: 'Bearer',
            expiresInSeconds: 43200,
            role: 'TEACHER',
            referenceId: 1,
            tenantId: 'aaaabbbb-0000-0000-0000-000000000001',
          },
        },
      });

      const result = await authApi.login(credentials);

      expect(loginPost).toHaveBeenCalledWith('/api/v1/tenant-auth/login', credentials);
      expect(result.accessToken).toBe('kc-access');
      expect(result.refreshToken).toBe(''); // tenant-auth issues no refresh token
      expect(result.user.roles).toEqual(['TEACHER']);
      expect(result.user.email).toBe('teacher_a@test.com');
    });

    it('falls back to KH owner login when tenant-auth rejects (owner email not a tenant credential)', async () => {
      const credentials: LoginRequest = { email: 'owner.test@test.vn', password: 'Test@1234' };

      loginPost
        .mockRejectedValueOnce({ response: { status: 401 } }) // tenant-auth probe → 401
        .mockResolvedValueOnce({
          // KH flat AuthResponse (role singular, has refreshToken)
          data: {
            accessToken: 'kh-access',
            refreshToken: 'kh-refresh',
            tokenType: 'Bearer',
            expiresIn: 3600,
            user: { id: 'owner-uuid', email: 'owner.test@test.vn', name: 'Test Owner', role: 'OWNER' },
          },
        });

      const result = await authApi.login(credentials);

      expect(loginPost).toHaveBeenNthCalledWith(1, '/api/v1/tenant-auth/login', credentials);
      expect(loginPost).toHaveBeenNthCalledWith(2, '/api/auth/login', credentials);
      expect(result.accessToken).toBe('kh-access');
      expect((result.user as unknown as { role: string }).role).toBe('OWNER');
    });

    it('throws a uniform VN error when both endpoints reject', async () => {
      loginPost.mockRejectedValue({ response: { status: 401 } });

      await expect(
        authApi.login({ email: 'nobody@test.com', password: 'bad' }),
      ).rejects.toThrow('Email hoặc mật khẩu không đúng');
    });
  });

  describe('logout', () => {
    it('should logout successfully', async () => {
      const refreshToken = 'refresh-token-123';

      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { success: true } });

      await authApi.logout(refreshToken);

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/logout', { refreshToken });
    });

    it('should swallow server error (fail-open per GAP-1075) so local logout always completes', async () => {
      const refreshToken = 'refresh-token-123';

      vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('Logout failed'));

      // Revocation is best-effort: any server/network error is swallowed so the
      // caller's onSettled can clear local tokens unconditionally. logout resolves.
      await expect(authApi.logout(refreshToken)).resolves.toBeUndefined();
      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/logout', { refreshToken });
    });
  });

  describe('refreshToken', () => {
    it('should refresh token successfully', async () => {
      const oldRefreshToken = 'old-refresh-token';

      const mockAuthResponse: AuthResponse = {
        accessToken: 'new-access-token-123',
        refreshToken: 'new-refresh-token-456',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: {
          id: 1,
          email: 'test@example.com',
          name: 'Test User',
          roles: ['STUDENT'],
        },
      };

      // Wave 105 GAP-724 fix: KH /api/auth/refresh returns flat AuthResponse.
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: mockAuthResponse });

      const result = await authApi.refreshToken(oldRefreshToken);

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/refresh', {
        refreshToken: oldRefreshToken,
      });
      expect(result).toEqual(mockAuthResponse);
    });

    it('should handle refresh token error', async () => {
      const refreshToken = 'invalid-token';

      vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('Token expired'));

      await expect(authApi.refreshToken(refreshToken)).rejects.toThrow('Token expired');
    });
  });

  describe('forgotPassword', () => {
    it('should send forgot password email successfully', async () => {
      const email = 'test@example.com';

      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { success: true } });

      await authApi.forgotPassword(email);

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/password-reset-request', { email });
    });

    it('should handle forgot password error', async () => {
      const email = 'invalid@example.com';

      vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('User not found'));

      await expect(authApi.forgotPassword(email)).rejects.toThrow('User not found');
    });
  });

  describe('resetPassword', () => {
    it('should reset password successfully', async () => {
      const token = 'reset-token-123';
      const newPassword = 'newPassword123';

      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { success: true } });

      await authApi.resetPassword(token, newPassword);

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/password-reset-confirm', {
        token,
        newPassword,
      });
    });

    it('should handle reset password error', async () => {
      const token = 'invalid-token';
      const newPassword = 'newPassword123';

      vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('Invalid or expired token'));

      await expect(authApi.resetPassword(token, newPassword)).rejects.toThrow(
        'Invalid or expired token'
      );
    });
  });

  describe('verifyEmail', () => {
    it('should verify email successfully', async () => {
      const token = 'verify-token-123';

      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { success: true } });

      await authApi.verifyEmail(token);

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/verify-email', { token });
    });

    it('should handle verify email error', async () => {
      const token = 'invalid-token';

      vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('Invalid verification token'));

      await expect(authApi.verifyEmail(token)).rejects.toThrow('Invalid verification token');
    });
  });
});
