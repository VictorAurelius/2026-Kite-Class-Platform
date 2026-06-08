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
   * Logout user. Client-side only for now: the kitehub-subscription AuthController
   * exposes register/login/refresh but NO `/api/auth/logout` endpoint, so the prior
   * `POST /api/auth/logout` returned 404 and broke logout (the token-clear was gated
   * behind the mutation's onSuccess). Server-side refresh-token revocation (blacklist)
   * is deferred to GAP-1075. Until then logout = local token clear (handled by the
   * caller's onSettled), which is the de-facto behaviour for stateless JWT anyway.
   *
   * @param _refreshToken accepted for signature stability; unused until GAP-1075 ships
   *                      a server-side revocation endpoint.
   */
  logout: async (_refreshToken: string): Promise<void> => {
    // No-op network call — server revocation endpoint not implemented (GAP-1075).
    return Promise.resolve();
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
