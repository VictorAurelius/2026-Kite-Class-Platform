/**
 * Axios API client with authentication and tenant context.
 *
 * Features:
 * - Automatic Bearer token injection
 * - Tenant ID header (X-Tenant-Id)
 * - Token refresh on 401
 * - Error handling
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';

export const apiClient: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept-Language': 'vi',
  },
});

// Request interceptor (add auth token + tenant ID)
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Add access token
    const accessToken = localStorage.getItem('accessToken');
    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    // Add tenant ID
    const tenantId = localStorage.getItem('tenantId');
    if (tenantId && config.headers) {
      config.headers['X-Tenant-Id'] = tenantId;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor (handle token refresh)
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Attempt token refresh
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await axios.post(
          `${process.env.NEXT_PUBLIC_API_URL}/api/auth/refresh`,
          { refreshToken }
        );

        const { accessToken: newAccessToken } = response.data.data;
        localStorage.setItem('accessToken', newAccessToken);

        // Retry original request with new token
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        return apiClient(originalRequest);

      } catch (refreshError) {
        // Refresh failed - logout user
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
