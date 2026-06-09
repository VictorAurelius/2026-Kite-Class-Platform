/**
 * Authentication API functions.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import { apiClient } from '@/lib/api-client';
import type { LoginRequest, AuthResponse } from '@/types/auth';

export const authApi = {
  /**
   * Login user with email and password.
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    // KH subscription /api/auth/login returns FLAT shape (not ApiResponse wrapper).
    // Wave 105 RST UI 2026-05-23 GAP-724 fix: use response.data directly.
    const response = await apiClient.post<AuthResponse>('/api/auth/login', credentials);
    return response.data;
  },

  /**
   * Logout user. Calls the server-side revocation endpoint (GAP-1075) to blacklist the
   * refresh token, then the caller's `onSettled` clears local tokens. The endpoint is
   * idempotent + fail-open, so any network/server error is swallowed here — local logout
   * MUST always complete (the access token is stateless and expires on its own).
   *
   * @param refreshToken the refresh token to revoke server-side
   */
  logout: async (refreshToken: string): Promise<void> => {
    try {
      await apiClient.post('/api/auth/logout', { refreshToken });
    } catch {
      // Best-effort revocation — never block local logout on a server/network error.
    }
  },

  /**
   * Refresh access token using refresh token.
   */
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    // KH subscription /api/auth/refresh same flat shape — see login() above.
    const response = await apiClient.post<AuthResponse>('/api/auth/refresh', {
      refreshToken,
    });
    return response.data;
  },

  /**
   * Request password reset email.
   */
  forgotPassword: async (email: string): Promise<void> => {
    await apiClient.post('/api/auth/forgot-password', { email });
  },

  /**
   * Reset password with token from email.
   */
  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await apiClient.post('/api/auth/reset-password', { token, newPassword });
  },

  /**
   * Verify email with token from email.
   */
  verifyEmail: async (token: string): Promise<void> => {
    await apiClient.post('/api/auth/verify-email', { token });
  },
};
