/**
 * Authentication API functions.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

import axios from 'axios';
import { apiClient } from '@/lib/api-client';
import type { LoginRequest, AuthResponse } from '@/types/auth';

/**
 * Bare HTTP client for the login probe — intentionally WITHOUT the shared
 * `apiClient` response interceptor.
 *
 * KiteClass `:3000` authenticates tenant-scoped roles (TEACHER/PARENT/STUDENT)
 * via KC-native tenant-auth (Wave auth-1, GAP-725/GAP-1122) and owner/staff via
 * KH subscription login (cross-product SSO). The flow PROBES tenant-auth first;
 * an owner's email is not a tenant credential, so that probe returns 401. The
 * `apiClient` interceptor reacts to ANY 401 by clearing tokens + force-redirecting
 * to `/login` (its token-refresh path) — which would abort the owner's KH fallback.
 * A bare client keeps the probe side-effect-free.
 */
/**
 * Browser-side baseURL must PRESERVE the tenant Host (GAP-1207, mirrored from
 * `public.ts`). Tenant-auth login resolves the tenant from the gateway's
 * Host-based resolver (client X-Tenant-Id is stripped — GAP-814). A static
 * `NEXT_PUBLIC_API_URL=http://localhost:9000` sends Host=localhost → gateway
 * can't resolve the subdomain tenant → tenant-auth login fails. When the page is
 * served from a tenant subdomain (prod `*.kitehub.me` or local `*.nip.io` walk),
 * call the gateway on the SAME hostname (only the port comes from the env URL).
 */
function loginBaseUrl(): string {
  const configured = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000';
  if (typeof window === 'undefined') return configured;
  const { hostname, protocol } = window.location;
  const isIp = /^\d+\.\d+\.\d+\.\d+$/.test(hostname);
  const hasSubdomain = !isIp && hostname !== 'localhost' && hostname.split('.').length >= 3;
  if (!hasSubdomain) return configured;
  // Port suffix from configured URL. Empty (prod https://api.kitehub.me → 443) →
  // no suffix so login hits the tenant host on 443 (nginx /api → gateway, Host
  // preserved for tenant resolution). Old `port || '9000'` → <tenant>:9000 broke
  // prod (CSP + unreachable). Mirrors public.ts browserBaseUrl fix.
  let portSuffix = ':9000';
  try {
    const p = new URL(configured).port;
    portSuffix = p ? `:${p}` : '';
  } catch {
    /* keep default gateway port */
  }
  return `${protocol}//${hostname}${portSuffix}`;
}

const loginClient = axios.create({
  timeout: 10000,
  headers: { 'Content-Type': 'application/json', 'Accept-Language': 'vi' },
});
// Resolve baseURL per-request so window.location (tenant subdomain Host) is read
// at call time, not module-load (GAP-1207 host-preservation for tenant-auth login).
loginClient.interceptors.request.use((config) => {
  config.baseURL = loginBaseUrl();
  return config;
});

/** KC-native tenant-auth login payload (carried inside the ApiResponse wrapper). */
interface TenantAuthLogin {
  accessToken: string;
  tokenType?: string;
  expiresInSeconds?: number;
  role: string;
  referenceId: number;
  tenantId: string;
}

export const authApi = {
  /**
   * Login user with email and password.
   *
   * KiteClass `:3000` authenticates tenant roles (TEACHER/PARENT/STUDENT) via
   * KC-native `/api/v1/tenant-auth/login` (Wave auth-1, GAP-1122/GAP-725);
   * owner/staff fall back to KH subscription `/api/auth/login` (cross-product SSO).
   * Both server shapes are adapted to the unified {@link AuthResponse} the store
   * consumes (`useAuth.onSuccess` normalizes the role + routes to its role-home).
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    // 1) KC-native tenant-auth (TEACHER/PARENT/STUDENT). ApiResponse-wrapped.
    try {
      const res = await loginClient.post('/api/v1/tenant-auth/login', credentials);
      const data = (res.data?.data ?? res.data) as TenantAuthLogin;
      if (data?.accessToken && data?.role) {
        return {
          accessToken: data.accessToken,
          refreshToken: '', // tenant-auth issues an access token only (no refresh)
          tokenType: data.tokenType ?? 'Bearer',
          expiresIn: data.expiresInSeconds ?? 0,
          user: {
            // referenceId = the domain entity id (teacher/parent/student row).
            id: data.referenceId as unknown as number,
            email: credentials.email,
            name: credentials.email,
            // useAuth reads roles[0] then normalizes the BE token (GAP-1122).
            roles: [data.role],
          },
        };
      }
    } catch {
      // Not a tenant credential (401) or KC core unavailable — fall through to KH.
    }

    // 2) KH subscription owner/staff login. FLAT shape (no ApiResponse wrapper).
    // Wave 105 RST UI 2026-05-23 GAP-724: KH returns response.data directly.
    try {
      const response = await loginClient.post<AuthResponse>('/api/auth/login', credentials);
      return response.data;
    } catch {
      // Uniform failure (anti-enumeration); useAuth.onError renders the VN toast.
      throw new Error('Email hoặc mật khẩu không đúng');
    }
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
   *
   * GAP-1335: path matches kitehub-subscription `PasswordResetController`
   * `POST /api/auth/password-reset-request` (always 202, anti-enumeration).
   * kiteclass-core has no native reset endpoint; owner/staff credentials live
   * in the KiteHub `users` table, reached through the gateway `/api/auth/**`.
   */
  forgotPassword: async (email: string): Promise<void> => {
    await apiClient.post('/api/auth/password-reset-request', { email });
  },

  /**
   * Reset password with token from email.
   *
   * GAP-1335: path matches kitehub-subscription `PasswordResetController`
   * `POST /api/auth/password-reset-confirm` (body `{ token, newPassword }`).
   */
  resetPassword: async (token: string, newPassword: string): Promise<void> => {
    await apiClient.post('/api/auth/password-reset-confirm', { token, newPassword });
  },

  /**
   * Verify email with token from email.
   */
  verifyEmail: async (token: string): Promise<void> => {
    await apiClient.post('/api/auth/verify-email', { token });
  },
};
