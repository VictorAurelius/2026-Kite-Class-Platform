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

// Mock apiClient
vi.mock('@/lib/api-client', () => ({
  apiClient: {
    post: vi.fn(),
  },
}));

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('login', () => {
    it('should login successfully and return auth response', async () => {
      const mockCredentials: LoginRequest = {
        email: 'test@example.com',
        password: 'password123',
      };

      const mockAuthResponse: AuthResponse = {
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-456',
        tokenType: 'Bearer',
        expiresIn: 3600,
        user: {
          id: 1,
          email: 'test@example.com',
          name: 'Test User',
          roles: ['STUDENT'],
        },
      };

      // Wave 105 GAP-724 fix: KH /api/auth/login returns flat AuthResponse (no ApiResponse wrapper).
      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: mockAuthResponse });

      const result = await authApi.login(mockCredentials);

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/login', mockCredentials);
      expect(result).toEqual(mockAuthResponse);
    });

    it('should throw error when login fails', async () => {
      const mockCredentials: LoginRequest = {
        email: 'test@example.com',
        password: 'wrong-password',
      };

      vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('Invalid credentials'));

      await expect(authApi.login(mockCredentials)).rejects.toThrow('Invalid credentials');
    });
  });

  describe('logout', () => {
    it('should logout successfully', async () => {
      const refreshToken = 'refresh-token-123';

      vi.mocked(apiClient.post).mockResolvedValueOnce({ data: { success: true } });

      await authApi.logout(refreshToken);

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/logout', { refreshToken });
    });

    it('should handle logout error', async () => {
      const refreshToken = 'refresh-token-123';

      vi.mocked(apiClient.post).mockRejectedValueOnce(new Error('Logout failed'));

      await expect(authApi.logout(refreshToken)).rejects.toThrow('Logout failed');
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

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/forgot-password', { email });
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

      expect(apiClient.post).toHaveBeenCalledWith('/api/auth/reset-password', {
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
