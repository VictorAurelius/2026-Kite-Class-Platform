import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';

import {
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  clearTokens,
  getTenantIdFromToken,
} from '@/lib/auth/jwt-storage';

export const apiClient: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    'Accept-Language': 'vi',
  },
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // GAP-599 Wave 92 Bucket B: JWT stored in sessionStorage (per-tab isolation).
    const accessToken = getAccessToken();
    if (accessToken && config.headers) {
      config.headers.Authorization = `Bearer ${accessToken}`;
      // Bug #21 (Wave A Bucket B walk 2026-05-28): gateway TenantResolver
      // requires X-Tenant-Id header on tenant-scoped paths (e.g. POST
      // /api/v1/staff-invitations). Extract tenantId from JWT claim and
      // propagate. Caller-set X-Tenant-Id takes precedence (manual override).
      if (!config.headers['X-Tenant-Id']) {
        const tenantId = getTenantIdFromToken();
        if (tenantId) {
          config.headers['X-Tenant-Id'] = tenantId;
        }
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = getRefreshToken();
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await axios.post(
          `${process.env.NEXT_PUBLIC_API_URL}/api/auth/refresh`,
          { refreshToken }
        );

        const { accessToken: newAccessToken } = response.data;
        setAccessToken(newAccessToken);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        return apiClient(originalRequest);
      } catch {
        clearTokens();
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
