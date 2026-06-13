/**
 * Cross-product SSO exchange (KiteHub → KiteClass) — ADR-040 Option A, GAP-1138.
 *
 * The KiteClass `/sso/callback` route receives a one-time opaque code from KiteHub
 * and exchanges it here for a real KiteHub-minted session (access + refresh JWT)
 * scoped to the owner/staff's tenant. The minted JWT is HS512-signed with the
 * shared `JWT_SECRET`, so the gateway validates it + injects `X-Tenant-Id`
 * downstream exactly like a KC-native login.
 *
 * @author KiteClass Team
 * @since GAP-1138
 */

import axios from 'axios';

/** Flat shape returned by `POST /api/v1/auth/sso/exchange` (mirrors KH LoginResponse). */
export interface SsoExchangeResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    name: string;
    role: string;
  };
}

/**
 * Bare HTTP client — intentionally WITHOUT the shared `apiClient` response
 * interceptor. The callback has no KC session yet, so a 401 from the exchange
 * must NOT trigger the interceptor's token-refresh → `/login` force-redirect
 * (it would abort the SSO flow + hide the real error). Mirrors the `loginClient`
 * rationale in `auth.ts`.
 */
const ssoClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json', 'Accept-Language': 'vi' },
});

/**
 * Exchange the one-time SSO code for a KiteHub-minted KiteClass session.
 *
 * @param code the opaque one-time code from `:3000/sso/callback?code=...`
 * @throws if the code is invalid / expired / already consumed (BE returns 401)
 */
export async function exchangeSsoCode(code: string): Promise<SsoExchangeResponse> {
  const res = await ssoClient.post('/api/v1/auth/sso/exchange', { code });
  return res.data as SsoExchangeResponse;
}
