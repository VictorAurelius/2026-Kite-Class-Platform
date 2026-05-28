/**
 * JWT storage abstraction — sessionStorage backed for per-tab isolation.
 *
 * GAP-599 closure (Wave 92 Bucket B): two tabs on the same origin previously
 * collided on `localStorage['accessToken']` because localStorage is shared
 * across tabs of the same origin (browser invariant). Switching to
 * `sessionStorage` gives per-tab native isolation.
 *
 * Trade-off vs localStorage: closing a tab requires re-login. Acceptable for
 * Phase 1 BETA cohort (per GAP-599 Proposed Fix Option A).
 *
 * Design rationale (per `.claude/rules/design-patterns.md`):
 * - Facade pattern — single API surface (`setTokens`, `getAccessToken`, etc.)
 *   so callers cannot reach into storage directly.
 * - All callers MUST go through this module. Direct `localStorage`/`sessionStorage`
 *   access for JWT is banned (Wave 92+).
 *
 * SSR safety: every method guards `typeof window` because Next.js renders
 * server-side without browser globals.
 *
 * @since Wave 92 Bucket B (closes GAP-599)
 */

/** Storage key for the access token (short-lived bearer). */
export const ACCESS_TOKEN_KEY = 'accessToken';

/** Storage key for the refresh token (used by 401 retry flow). */
export const REFRESH_TOKEN_KEY = 'refreshToken';

/**
 * @returns the access token from sessionStorage, or `null` if absent or SSR.
 */
export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

/**
 * @returns the refresh token from sessionStorage, or `null` if absent or SSR.
 */
export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(REFRESH_TOKEN_KEY);
}

/**
 * Extract `tenantId` claim from the current access token (no signature
 * validation — BE verifies, client decode is read-only convenience).
 *
 * Bug #21 (Wave A Bucket B walk 2026-05-28): apiClient previously attached
 * only `Authorization: Bearer` but NOT `X-Tenant-Id` header. Gateway
 * TenantResolver requires `X-Tenant-Id` for tenant-scoped paths → 403 on
 * staff invitation POST. This helper lets the request interceptor propagate
 * tenantId from JWT → X-Tenant-Id header automatically.
 *
 * @returns tenantId UUID string from JWT claim, or `null` if absent / token
 *          malformed / SSR.
 */
export function getTenantIdFromToken(): string | null {
  const token = getAccessToken();
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    // base64url decode (handle padding + URL-safe chars per RFC 7515)
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = payload + '='.repeat((4 - (payload.length % 4)) % 4);
    const decoded = JSON.parse(atob(padded));
    return typeof decoded.tenantId === 'string' ? decoded.tenantId : null;
  } catch {
    return null;
  }
}

/**
 * Persists access token in sessionStorage (per-tab native isolation).
 *
 * @param token JWT bearer string.
 */
export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

/**
 * Persists refresh token in sessionStorage.
 *
 * @param token Refresh JWT string.
 */
export function setRefreshToken(token: string): void {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem(REFRESH_TOKEN_KEY, token);
}

/**
 * Convenience: persist both tokens atomically (login flow common case).
 *
 * @param accessToken JWT bearer string.
 * @param refreshToken Refresh JWT string.
 * @param persist When true, ALSO mirror tokens to localStorage so they survive
 *                browser-close ("remember me"). Default false (session-only,
 *                per GAP-599 tab isolation). Bootstrap reads localStorage
 *                fallback via `restorePersistedTokens()` on app load.
 */
export function setTokens(accessToken: string, refreshToken: string, persist = false): void {
  setAccessToken(accessToken);
  setRefreshToken(refreshToken);
  if (persist && typeof window !== 'undefined') {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

/**
 * Restores tokens from localStorage to sessionStorage if session was cleared
 * (browser close → reopen) and "remember me" was checked at last login.
 *
 * Call at app bootstrap. No-op when sessionStorage already has tokens (active
 * session) OR localStorage has no remembered tokens.
 *
 * @returns true if tokens were restored, false otherwise.
 */
export function restorePersistedTokens(): boolean {
  if (typeof window === 'undefined') return false;
  if (sessionStorage.getItem(ACCESS_TOKEN_KEY)) return false;
  const access = localStorage.getItem(ACCESS_TOKEN_KEY);
  const refresh = localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!access || !refresh) return false;
  sessionStorage.setItem(ACCESS_TOKEN_KEY, access);
  sessionStorage.setItem(REFRESH_TOKEN_KEY, refresh);
  return true;
}

/**
 * Removes both access + refresh tokens from sessionStorage AND localStorage
 * (logout flow). Logout always clears both tiers regardless of remember-me.
 *
 * Does NOT touch legacy non-prefixed entries — that is the responsibility of
 * `clearLegacyLocalStorageTokens()` (one-time migration sweep).
 */
export function clearTokens(): void {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

/**
 * One-time migration helper — sweeps legacy `localStorage` JWT entries left
 * over from Wave 91 and earlier builds where tokens were persisted to
 * localStorage.
 *
 * Call this early in the auth bootstrap (e.g. RootLayout client-side effect
 * or login page load) so legacy tokens do not silently linger across tabs.
 *
 * Safe to call repeatedly; safe in SSR (no-ops if no `window`).
 */
export function clearLegacyLocalStorageTokens(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
