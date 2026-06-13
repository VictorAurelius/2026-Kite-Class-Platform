/**
 * Cross-product SSO client (KiteHub → KiteClass) — ADR-040 Option A, GAP-1138.
 *
 * The owner/staff is already authenticated at KiteHub (`:3001`). To enter the
 * KiteClass owner-shell (`:3000`) without re-login we request a short-lived
 * one-time code from kitehub-subscription, then redirect the browser to the
 * KiteClass callback carrying ONLY that opaque code (never a raw JWT).
 */

import { apiClient } from '@/lib/api/client';

export interface SsoIssueCodeResponse {
  /** Single-use opaque code (256-bit); consumed on first exchange. */
  code: string;
  /** Code TTL in seconds (≤60 per ADR-040). */
  expiresIn: number;
}

/**
 * Request a one-time SSO exchange code from kitehub-subscription.
 *
 * The shared `apiClient` request interceptor attaches the current Bearer access
 * token automatically, so the BE can identify the authenticated owner/staff.
 */
export async function issueSsoCode(): Promise<SsoIssueCodeResponse> {
  const res = await apiClient.post<SsoIssueCodeResponse>('/api/v1/auth/sso/issue-code');
  return res.data;
}

/**
 * Build the KiteClass SSO callback URL carrying ONLY the opaque one-time code.
 * Defaults to the local KiteClass FE (`:3000`) when the env var is unset.
 *
 * @param code the one-time code from {@link issueSsoCode}
 */
export function buildKiteClassSsoCallbackUrl(code: string): string {
  const base = (process.env.NEXT_PUBLIC_KITECLASS_URL || 'http://localhost:3000').replace(/\/+$/, '');
  return `${base}/sso/callback?code=${encodeURIComponent(code)}`;
}
