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
    const response = await apiClient.post<any>('/api/v1/auth/login', credentials);
    return response.data.data; // Unwrap ApiResponse wrapper
  },

  /**
   * Logout user and invalidate refresh token.
   */
  logout: async (refreshToken: string): Promise<void> => {
    await apiClient.post('/api/v1/auth/logout', { refreshToken });
  },

  /**
   * Refresh access token using refresh token.
   */
  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const response = await apiClient.post<any>('/api/v1/auth/refresh', {
      refreshToken,
    });
    return response.data.data; // Unwrap ApiResponse wrapper
  },

  /**
   * Request password reset email.
   */
  forgotPassword: async (email: string): Promise<void> => {
    await apiClient.post('/api/v1/auth/forgot-password', { email });
  },

  /**
   * Reset password with token from email.
   */
  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await apiClient.post('/api/v1/auth/reset-password', { token, newPassword });
  },

  /**
   * Verify email with token from email.
   */
  verifyEmail: async (token: string): Promise<void> => {
    await apiClient.post('/api/v1/auth/verify-email', { token });
  },
};
