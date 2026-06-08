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

    // Multipart file uploads (branding logo/asset upload via useUploadAsset):
    // drop the instance-default 'Content-Type: application/json' (and any caller-set
    // boundary-less 'multipart/form-data') so the browser sets multipart/form-data
    // WITH the correct boundary — otherwise BE @RequestPart parsing fails.
    // Cross-flow sweep of GAP-1073 (kiteclass-frontend had the same bug class).
    if (config.data instanceof FormData && config.headers) {
      delete config.headers['Content-Type'];
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// GAP-924 (2026-06-04): Auth-flow endpoints return 401 as part of normal flow
// validation (wrong password, invalid TOTP, expired challenge token) — NOT
// session expiry. The refresh-then-redirect path masks the error from the
// component catch block and leaves the user with a silent UI failure. Skip
// the auto-refresh path for these URLs and let the caller handle the 401.
const AUTH_FLOW_401_PASSTHROUGH = [
  '/api/auth/login',
  '/api/auth/2fa/verify',
  '/api/auth/2fa/enroll-init',
  '/api/auth/2fa/enroll-confirm',
  '/api/auth/2fa/setup',
];

function isAuthFlowPassthrough(url: string | undefined): boolean {
  if (!url) return false;
  return AUTH_FLOW_401_PASSTHROUGH.some((p) => url === p || url.endsWith(p));
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !isAuthFlowPassthrough(originalRequest.url)
    ) {
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
