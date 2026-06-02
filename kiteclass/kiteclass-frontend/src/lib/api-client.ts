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
import { toast } from '@/hooks/use-toast';

/**
 * Backend ErrorResponse shape (per kiteclass-core GlobalExceptionHandler).
 * Fields: code (machine-readable), message (i18n VN), path, timestamp.
 */
interface BackendErrorResponse {
  code?: string;
  message?: string;
  path?: string;
  timestamp?: string;
}

/**
 * Render Vietnamese error toast from BE structured error.
 * Falls back to generic message if shape không match (per GAP-777 AC b).
 */
function renderErrorToast(error: AxiosError) {
  // Skip toast for canceled requests
  if (axios.isCancel(error)) return;

  const data = error.response?.data as BackendErrorResponse | undefined;
  const status = error.response?.status;

  // Structured error from BE
  if (data && typeof data === 'object' && (data.message || data.code)) {
    toast({
      title: status && status >= 500 ? 'Lỗi hệ thống' : 'Lỗi',
      description: data.message || data.code || 'Đã xảy ra lỗi không xác định',
      variant: 'destructive',
    });
    return;
  }

  // Network / timeout / unstructured fallback
  if (!error.response) {
    toast({
      title: 'Lỗi kết nối',
      description: 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.',
      variant: 'destructive',
    });
    return;
  }

  toast({
    title: 'Lỗi',
    description: 'Đã xảy ra lỗi không xác định',
    variant: 'destructive',
  });
}

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

    // Render Vietnamese error toast for all non-401 (and 401 after refresh-retry exhausted)
    // Per GAP-777 AC (b): FE catches structured error + renders Vietnamese toast
    renderErrorToast(error);

    return Promise.reject(error);
  }
);

export default apiClient;
