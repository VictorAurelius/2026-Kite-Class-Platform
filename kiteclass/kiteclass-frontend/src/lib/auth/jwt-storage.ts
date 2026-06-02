/**
 * JWT storage abstraction — sessionStorage backed for per-tab isolation.
 *
 * GAP-830 (cross-flow sweep of GAP-599 kitehub-frontend closure): two tabs on
 * the same KiteClass tenant origin previously collided on
 * `localStorage['accessToken']` because localStorage is shared across tabs of
 * the same origin (browser invariant). KiteClass is multi-tenant — multiple
 * actors (GVCN / student / parent) may sign in concurrently across tabs, so
 * the cross-role / cross-tenant collision risk is higher than kitehub.
 * Switching to `sessionStorage` gives per-tab native isolation.
 *
 * Two collision vectors existed in kiteclass-frontend before this fix:
 *   1. Direct `localStorage['accessToken'|'refreshToken'|'tenantId']` writes in
 *      `useAuth.ts` / `api-client.ts` / `student-register-form.tsx`.
 *   2. Zustand `auth-store.ts` `persist` middleware defaulted to localStorage
 *      (`auth-storage` key) — a SECOND shared-tab vector. That store is now
 *      switched to `createJSONStorage(() => sessionStorage)`.
 *
 * Also reconciles a pre-existing key-name inconsistency: `student-register-form`
 * wrote snake_case `access_token`/`refresh_token` while `api-client` read
 * camelCase `accessToken` — so student-register tokens were never picked up by
 * the request interceptor. All callers now go through this facade (camelCase).
 *
 * Trade-off vs localStorage: closing a tab requires re-login. Acceptable for
 * Phase 1 BETA cohort (mirrors GAP-599 Option A).
 *
 * SSR safety: every method guards `typeof window` because Next.js renders
 * server-side without browser globals.
 *
 * @since GAP-830 (cross-flow sweep of GAP-599)
 */

/** Storage key for the access token (short-lived bearer). */
export const ACCESS_TOKEN_KEY = 'accessToken';

/** Storage key for the refresh token (used by 401 retry flow). */
export const REFRESH_TOKEN_KEY = 'refreshToken';

/** Storage key for the active tenant id (KiteClass multi-tenant context). */
export const TENANT_ID_KEY = 'tenantId';

/**
 * Legacy localStorage keys swept on logout / bootstrap — includes the
 * snake_case variants written by the old student-register flow + the zustand
 * `auth-storage` blob from the pre-fix persist config.
 */
const LEGACY_LOCAL_KEYS = [
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
  TENANT_ID_KEY,
  'access_token',
  'refresh_token',
  'auth-storage',
];

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
 * @returns the active tenant id from sessionStorage, or `null` if absent or SSR.
 */
export function getTenantId(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(TENANT_ID_KEY);
}

/**
 * Extract `tenantId` claim from the current access token (no signature
 * validation — BE verifies, client decode is read-only convenience).
 *
 * @returns tenantId UUID string from JWT claim, or `null` if absent / token
 *          malformed / SSR.
 */
export function getTenantIdFromToken(): string | null {
  const token = getAccessToken();
  if (!token) return null;
  const parts = token.split('.');
  const payloadPart = parts[1];
  if (parts.length !== 3 || !payloadPart) return null;
  try {
    // base64url decode (handle padding + URL-safe chars per RFC 7515)
    const payload = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
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
 * Persists the active tenant id in sessionStorage.
 *
 * @param tenantId Tenant UUID string.
 */
export function setTenantId(tenantId: string): void {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem(TENANT_ID_KEY, tenantId);
}

/**
 * Convenience: persist tokens (+ optional tenant id) atomically — the login /
 * register flow common case.
 *
 * @param accessToken JWT bearer string.
 * @param refreshToken Refresh JWT string.
 * @param tenantId Optional tenant id to store alongside.
 * @param persist When true, ALSO mirror to localStorage so tokens survive
 *                browser-close ("remember me"). Default false (session-only,
 *                per GAP-830 tab isolation). Bootstrap reads localStorage
 *                fallback via `restorePersistedTokens()` on app load.
 */
export function setTokens(
  accessToken: string,
  refreshToken: string,
  tenantId?: string,
  persist = false,
): void {
  setAccessToken(accessToken);
  setRefreshToken(refreshToken);
  if (tenantId) setTenantId(tenantId);
  if (persist && typeof window !== 'undefined') {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    if (tenantId) localStorage.setItem(TENANT_ID_KEY, tenantId);
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
  const tenantId = localStorage.getItem(TENANT_ID_KEY);
  if (tenantId) sessionStorage.setItem(TENANT_ID_KEY, tenantId);
  return true;
}

/**
 * Removes access + refresh + tenantId from sessionStorage AND localStorage
 * (logout flow). Logout always clears both tiers regardless of remember-me,
 * and also sweeps legacy snake_case + zustand `auth-storage` keys.
 */
export function clearTokens(): void {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(TENANT_ID_KEY);
  for (const key of LEGACY_LOCAL_KEYS) {
    localStorage.removeItem(key);
  }
}

/**
 * One-time migration helper — sweeps legacy `localStorage` JWT entries left
 * over from pre-GAP-830 builds (camelCase + snake_case token keys + the
 * zustand `auth-storage` persist blob). Safe to call repeatedly; SSR no-op.
 *
 * Call early in auth bootstrap so legacy tokens do not linger across tabs.
 */
export function clearLegacyLocalStorageTokens(): void {
  if (typeof window === 'undefined') return;
  for (const key of LEGACY_LOCAL_KEYS) {
    localStorage.removeItem(key);
  }
}
