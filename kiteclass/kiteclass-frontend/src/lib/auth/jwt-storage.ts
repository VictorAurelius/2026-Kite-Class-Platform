/**
 * JWT storage abstraction — tenant-scoped localStorage (Option B, GAP-1074).
 *
 * Supersedes GAP-830 (sessionStorage per-tab isolation). GAP-830 stored tokens
 * in `sessionStorage` to avoid two tabs on different tenants clobbering a shared
 * `localStorage['accessToken']` key (cross-tenant leak). That fixed isolation but
 * forced "open URL in a new tab = re-login" because sessionStorage is per-tab.
 *
 * Option B keeps BOTH properties:
 *   1. cross-tab persistence — tokens live in `localStorage` (shared across tabs +
 *      survive browser close), so opening a URL in a new tab no longer re-logs-in.
 *   2. per-tenant isolation — keys are NAMESPACED by tenantId:
 *        kc:<tenantId>:accessToken
 *        kc:<tenantId>:refreshToken
 *        kc:<tenantId>:auth-store   (zustand persist blob, see tenantScopedStateStorage)
 *      Two tabs signed into different tenants write to disjoint namespaces, so they
 *      can never overwrite each other's token — solving the exact GAP-830 concern
 *      WITHOUT sacrificing cross-tab UX.
 *
 * Tab-to-tenant binding + fresh-tab resolution:
 *   - `sessionStorage['kc:currentTenant']` — per-tab binding. Every getter/setter
 *     resolves the active tenant from THIS value first, so a tab bound to tenant A
 *     only ever reads tenant A's namespace (never tenant B's token).
 *   - `localStorage['kc:activeTenant']` — NON-scoped pointer to the tenantId of the
 *     most recent login. A FRESH tab (no per-tab binding yet) falls back to this
 *     pointer to resolve which namespace to load, then binds itself going forward.
 *     Single-owner (the common case) resolves immediately + correctly. Multi-tenant
 *     concurrent tabs: a fresh tab follows whichever tenant logged in most recently
 *     — acceptable (production resolves the canonical tenant from the subdomain via
 *     `useTenantFromUrl`; BE validates the JWT `tenantId` claim against X-Tenant-Id).
 *
 * Security — why this does NOT leak across tenants:
 *   - getters return token + tenantId as a CONSISTENT PAIR from a single namespace;
 *     tenant A's token is never served under tenant B's identity.
 *   - clearTokens() removes ONLY the current tenant's scoped keys — a logout for
 *     tenant A leaves tenant B's tokens intact.
 *   - The worst fresh-tab case is "shows my OTHER tenant" (same owner), never another
 *     user's data — and BE re-checks the JWT claim, so cross-user leak is impossible.
 *
 * SSR safety: every method guards `typeof window` because Next.js renders
 * server-side without browser globals.
 *
 * @since GAP-1074 (supersedes GAP-830 sessionStorage facade)
 */

/** Namespace prefix for all tenant-scoped + pointer keys. */
const NS_PREFIX = 'kc';

/** Sub-key for the access token within a tenant namespace. */
const ACCESS_SUB = 'accessToken';
/** Sub-key for the refresh token within a tenant namespace. */
const REFRESH_SUB = 'refreshToken';
/** Sub-key for the zustand auth-store persist blob within a tenant namespace. */
const STORE_SUB = 'auth-store';

/**
 * Per-tab binding (sessionStorage): tenantId this tab is currently scoped to.
 * Survives same-tab reloads, NOT shared across tabs.
 */
const CURRENT_TENANT_KEY = `${NS_PREFIX}:currentTenant`;

/**
 * Cross-tab last-login pointer (localStorage): tenantId of the most recent login.
 * Used by fresh tabs (no per-tab binding) to pick the namespace to load.
 */
const ACTIVE_TENANT_KEY = `${NS_PREFIX}:activeTenant`;

/**
 * Default tenant id for guest / student self-registration flows that don't carry
 * a tenant claim (mirrors the hardcoded default in useAuth + student-register-form).
 */
const DEFAULT_TENANT_ID = '11111111-1111-1111-1111-111111111111';

/**
 * Legacy keys swept on logout / bootstrap — GAP-830-era sessionStorage keys,
 * pre-GAP-830 snake_case localStorage variants, and the old zustand `auth-storage`
 * blob. Kept here so a build upgrade does not strand stale tokens across tabs.
 */
const LEGACY_LOCAL_KEYS = [
  'accessToken',
  'refreshToken',
  'tenantId',
  'access_token',
  'refresh_token',
  'auth-storage',
];

/** Build a tenant-scoped localStorage key, e.g. `kc:<tenantId>:accessToken`. */
function scopedKey(tenantId: string, sub: string): string {
  return `${NS_PREFIX}:${tenantId}:${sub}`;
}

/**
 * Decode the `tenantId` claim from a JWT string (no signature validation — BE
 * verifies, client decode is read-only convenience).
 *
 * @returns tenantId UUID string from the JWT claim, or `null` if absent/malformed.
 */
function tenantIdFromTokenString(token: string | null): string | null {
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
 * Resolve the tenantId this tab should operate under.
 *
 * Resolution order:
 *   1. Per-tab binding (`sessionStorage['kc:currentTenant']`) — authoritative once set.
 *   2. Fresh-tab fallback: the cross-tab last-login pointer
 *      (`localStorage['kc:activeTenant']`). On a hit, the tab BINDS itself to that
 *      tenant so all subsequent reads/writes are stable + scoped.
 *
 * The fresh-tab self-binding side effect is intentional: it is what gives a newly
 * opened tab a consistent tenant context without re-login (the GAP-1074 goal).
 *
 * @returns the active tenantId, or `null` when no login has happened (SSR / logged out).
 */
export function getCurrentTenantId(): string | null {
  if (typeof window === 'undefined') return null;
  const bound = sessionStorage.getItem(CURRENT_TENANT_KEY);
  if (bound) return bound;
  const active = localStorage.getItem(ACTIVE_TENANT_KEY);
  if (active) {
    // Fresh tab — bind to the last-login tenant so future reads stay scoped.
    sessionStorage.setItem(CURRENT_TENANT_KEY, active);
    return active;
  }
  return null;
}

/**
 * Bind this tab to a tenant + update the cross-tab last-login pointer.
 * Called whenever a tenant context is established (login / register / token set).
 */
function bindTenant(tenantId: string): void {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem(CURRENT_TENANT_KEY, tenantId);
  localStorage.setItem(ACTIVE_TENANT_KEY, tenantId);
}

/**
 * @returns the access token for THIS tab's tenant namespace, or `null` if absent/SSR.
 */
export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  const tenantId = getCurrentTenantId();
  if (!tenantId) return null;
  return localStorage.getItem(scopedKey(tenantId, ACCESS_SUB));
}

/**
 * @returns the refresh token for THIS tab's tenant namespace, or `null` if absent/SSR.
 */
export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  const tenantId = getCurrentTenantId();
  if (!tenantId) return null;
  return localStorage.getItem(scopedKey(tenantId, REFRESH_SUB));
}

/**
 * @returns the active tenant id for THIS tab (drives the `X-Tenant-Id` header),
 *          or `null` if no login / SSR. Always consistent with the token returned
 *          by {@link getAccessToken} (same namespace).
 */
export function getTenantId(): string | null {
  return getCurrentTenantId();
}

/**
 * Extract `tenantId` claim from the current access token (read-only convenience).
 *
 * @returns tenantId UUID string from JWT claim, or `null` if absent/malformed/SSR.
 */
export function getTenantIdFromToken(): string | null {
  return tenantIdFromTokenString(getAccessToken());
}

/**
 * Persist the access token in the current tenant's localStorage namespace.
 * Used by the 401 refresh flow. No-op if no tenant is bound yet.
 *
 * @param token JWT bearer string.
 */
export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') return;
  const tenantId = getCurrentTenantId();
  if (!tenantId) return;
  localStorage.setItem(scopedKey(tenantId, ACCESS_SUB), token);
}

/**
 * Persist the refresh token in the current tenant's localStorage namespace.
 * No-op if no tenant is bound yet.
 *
 * @param token Refresh JWT string.
 */
export function setRefreshToken(token: string): void {
  if (typeof window === 'undefined') return;
  const tenantId = getCurrentTenantId();
  if (!tenantId) return;
  localStorage.setItem(scopedKey(tenantId, REFRESH_SUB), token);
}

/**
 * Bind this tab to the given tenant (sets per-tab binding + cross-tab pointer).
 *
 * @param tenantId Tenant UUID string.
 */
export function setTenantId(tenantId: string): void {
  bindTenant(tenantId);
}

/**
 * Convenience: persist tokens (+ resolve tenant) atomically — the login / register
 * common case. Tokens go to the tenant-scoped localStorage namespace (cross-tab
 * persistent by design), and the tab binds to that tenant.
 *
 * Tenant resolution order: explicit `tenantId` arg → JWT `tenantId` claim →
 * current binding → {@link DEFAULT_TENANT_ID} (guest flows).
 *
 * @param accessToken JWT bearer string.
 * @param refreshToken Refresh JWT string.
 * @param tenantId Optional explicit tenant id; falls back to the JWT claim / default.
 * @param _persist Deprecated (Option B always persists cross-tab). Kept for signature
 *                 compatibility with the GAP-830 facade; ignored.
 */
export function setTokens(
  accessToken: string,
  refreshToken: string,
  tenantId?: string,
  _persist = false,
): void {
  if (typeof window === 'undefined') return;
  const resolved =
    tenantId ??
    tenantIdFromTokenString(accessToken) ??
    getCurrentTenantId() ??
    DEFAULT_TENANT_ID;
  bindTenant(resolved);
  localStorage.setItem(scopedKey(resolved, ACCESS_SUB), accessToken);
  localStorage.setItem(scopedKey(resolved, REFRESH_SUB), refreshToken);
}

/**
 * Bootstrap helper (compat with the GAP-830 facade). Under Option B tokens already
 * live in localStorage, so "restore" just verifies the last-login tenant has tokens
 * and binds the tab to it. No session↔local copy is needed anymore.
 *
 * @returns true if a remembered token exists for the last-login tenant, else false.
 */
export function restorePersistedTokens(): boolean {
  if (typeof window === 'undefined') return false;
  // Read the per-tab binding DIRECTLY (not via getCurrentTenantId, whose fresh-tab
  // fallback would bind the tab as a side effect and mask the "needs restore" state).
  const bound = sessionStorage.getItem(CURRENT_TENANT_KEY);
  // Active session in THIS tab already → nothing to restore.
  if (bound && localStorage.getItem(scopedKey(bound, ACCESS_SUB))) return false;
  // Fresh tab: restore from the last-login pointer if it has remembered tokens.
  const active = localStorage.getItem(ACTIVE_TENANT_KEY);
  if (!active) return false;
  if (!localStorage.getItem(scopedKey(active, ACCESS_SUB))) return false;
  bindTenant(active);
  return true;
}

/**
 * Logout sweep. Removes the CURRENT tenant's scoped tokens + store blob from
 * localStorage, drops this tab's binding, and clears the last-login pointer only if
 * it referenced the tenant being logged out. Other tenants' tokens are preserved
 * (logging out of tenant A must not log out tenant B). Also sweeps GAP-830-era
 * sessionStorage keys + pre-GAP-830 legacy localStorage keys.
 */
export function clearTokens(): void {
  if (typeof window === 'undefined') return;
  const tenantId = getCurrentTenantId();

  // Drop this tab's binding.
  sessionStorage.removeItem(CURRENT_TENANT_KEY);

  // GAP-830-era sessionStorage keys.
  sessionStorage.removeItem('accessToken');
  sessionStorage.removeItem('refreshToken');
  sessionStorage.removeItem('tenantId');

  // Current tenant's scoped keys ONLY — never touch other tenants' namespaces.
  if (tenantId) {
    localStorage.removeItem(scopedKey(tenantId, ACCESS_SUB));
    localStorage.removeItem(scopedKey(tenantId, REFRESH_SUB));
    localStorage.removeItem(scopedKey(tenantId, STORE_SUB));
    if (localStorage.getItem(ACTIVE_TENANT_KEY) === tenantId) {
      localStorage.removeItem(ACTIVE_TENANT_KEY);
    }
  }

  // Pre-GAP-830 + GAP-830 legacy localStorage keys.
  for (const key of LEGACY_LOCAL_KEYS) {
    localStorage.removeItem(key);
  }
}

/**
 * One-time migration helper — sweeps legacy `localStorage` JWT entries left over
 * from pre-GAP-1074 builds (camelCase + snake_case token keys + the zustand
 * `auth-storage` persist blob). Safe to call repeatedly; SSR no-op.
 *
 * Call early in auth bootstrap so legacy tokens do not linger across tabs.
 */
export function clearLegacyLocalStorageTokens(): void {
  if (typeof window === 'undefined') return;
  for (const key of LEGACY_LOCAL_KEYS) {
    localStorage.removeItem(key);
  }
}

/**
 * Custom zustand `persist` storage backing the auth-store. Resolves the current
 * tenant per call and reads/writes the tenant-scoped `kc:<tenantId>:auth-store` key
 * in localStorage, so two tabs on different tenants keep disjoint persisted state
 * (same isolation guarantee as the token namespaces). Falls back to `kc:anon:auth-store`
 * before any login. SSR-safe (no-op getters return null).
 *
 * Shape matches zustand's `StateStorage` (string get/set/remove).
 */
export const tenantScopedStateStorage = {
  getItem: (_name: string): string | null => {
    if (typeof window === 'undefined') return null;
    const tenantId = getCurrentTenantId() ?? 'anon';
    return localStorage.getItem(scopedKey(tenantId, STORE_SUB));
  },
  setItem: (_name: string, value: string): void => {
    if (typeof window === 'undefined') return;
    const tenantId = getCurrentTenantId() ?? 'anon';
    localStorage.setItem(scopedKey(tenantId, STORE_SUB), value);
  },
  removeItem: (_name: string): void => {
    if (typeof window === 'undefined') return;
    const tenantId = getCurrentTenantId() ?? 'anon';
    localStorage.removeItem(scopedKey(tenantId, STORE_SUB));
  },
};

/**
 * Test/diagnostic exports — internal key helpers surfaced for unit tests asserting
 * the namespace scheme. Not part of the runtime contract.
 */
export const __testing = {
  NS_PREFIX,
  ACCESS_SUB,
  REFRESH_SUB,
  STORE_SUB,
  CURRENT_TENANT_KEY,
  ACTIVE_TENANT_KEY,
  DEFAULT_TENANT_ID,
  scopedKey,
};
