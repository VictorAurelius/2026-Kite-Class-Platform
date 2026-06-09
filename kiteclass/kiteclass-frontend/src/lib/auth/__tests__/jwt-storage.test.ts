/**
 * Unit tests for the tenant-scoped JWT localStorage facade (GAP-1074, Option B).
 *
 * Verifies cross-tab persistence (localStorage), per-tenant namespace isolation
 * (the GAP-830 cross-tenant-leak concern), per-tab binding, fresh-tab activeTenant
 * resolution, legacy sweep, and the zustand persist storage adapter.
 *
 * jsdom shares ONE global sessionStorage + localStorage. We simulate distinct
 * browser tabs by switching the per-tab binding (`kc:currentTenant` in
 * sessionStorage) to represent whichever tab's perspective the assertion is from.
 */

import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  getAccessToken,
  getRefreshToken,
  getTenantId,
  getTenantIdFromToken,
  getCurrentTenantId,
  setAccessToken,
  setRefreshToken,
  setTenantId,
  setTokens,
  restorePersistedTokens,
  clearTokens,
  clearLegacyLocalStorageTokens,
  tenantScopedStateStorage,
  __testing,
} from '../jwt-storage';

const {
  ACCESS_SUB,
  REFRESH_SUB,
  STORE_SUB,
  CURRENT_TENANT_KEY,
  ACTIVE_TENANT_KEY,
  DEFAULT_TENANT_ID,
  scopedKey,
} = __testing;

const TENANT_A = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
const TENANT_B = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';

/** Build a fake JWT with given payload (header.payload.signature). */
function fakeJwt(payload: Record<string, unknown>): string {
  const b64 = (o: unknown) =>
    btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64(payload)}.sig`;
}

/** Simulate switching the active browser tab by re-binding this "tab" to a tenant. */
function switchTabTo(tenantId: string): void {
  sessionStorage.setItem(CURRENT_TENANT_KEY, tenantId);
}

/** Simulate a brand-new tab: per-tab binding gone, localStorage (cross-tab) intact. */
function openFreshTab(): void {
  sessionStorage.removeItem(CURRENT_TENANT_KEY);
}

describe('jwt-storage facade (GAP-1074 tenant-scoped localStorage)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });
  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  describe('basic set/get — tenant-scoped localStorage', () => {
    it('setTokens writes scoped localStorage keys + binds the tab', () => {
      setTokens('a-token', 'r-token', TENANT_A);

      // Cross-tab persistent store = localStorage, namespaced by tenant.
      expect(localStorage.getItem(scopedKey(TENANT_A, ACCESS_SUB))).toBe('a-token');
      expect(localStorage.getItem(scopedKey(TENANT_A, REFRESH_SUB))).toBe('r-token');
      // Tab bound + last-login pointer set.
      expect(sessionStorage.getItem(CURRENT_TENANT_KEY)).toBe(TENANT_A);
      expect(localStorage.getItem(ACTIVE_TENANT_KEY)).toBe(TENANT_A);

      expect(getAccessToken()).toBe('a-token');
      expect(getRefreshToken()).toBe('r-token');
      expect(getTenantId()).toBe(TENANT_A);
      expect(getCurrentTenantId()).toBe(TENANT_A);
    });

    it('setTokens does NOT write the legacy flat sessionStorage keys', () => {
      setTokens('a-token', 'r-token', TENANT_A);
      expect(sessionStorage.getItem('accessToken')).toBeNull();
      expect(sessionStorage.getItem('refreshToken')).toBeNull();
    });

    it('setTokens resolves tenant from JWT claim when arg omitted', () => {
      setTokens(fakeJwt({ sub: 'u1', tenantId: TENANT_B }), 'r-token');
      expect(getCurrentTenantId()).toBe(TENANT_B);
      expect(localStorage.getItem(scopedKey(TENANT_B, ACCESS_SUB))).not.toBeNull();
    });

    it('setTokens falls back to DEFAULT_TENANT_ID for tenant-less guest flow', () => {
      setTokens('plain-access', 'plain-refresh'); // no arg, no JWT claim
      expect(getCurrentTenantId()).toBe(DEFAULT_TENANT_ID);
      expect(localStorage.getItem(scopedKey(DEFAULT_TENANT_ID, ACCESS_SUB))).toBe('plain-access');
    });

    it('individual setters scope to the bound tenant', () => {
      setTenantId(TENANT_A);
      setAccessToken('a2');
      setRefreshToken('r2');
      expect(getAccessToken()).toBe('a2');
      expect(getRefreshToken()).toBe('r2');
      expect(localStorage.getItem(scopedKey(TENANT_A, ACCESS_SUB))).toBe('a2');
    });

    it('getters return null when no tenant is bound', () => {
      expect(getAccessToken()).toBeNull();
      expect(getRefreshToken()).toBeNull();
      expect(getTenantId()).toBeNull();
    });

    it('setAccessToken is a no-op when no tenant bound (refresh-before-login guard)', () => {
      setAccessToken('orphan');
      expect(getAccessToken()).toBeNull();
    });
  });

  describe('cross-tab persistence (the GAP-1074 fix)', () => {
    it('a fresh tab loads tokens without re-login via the activeTenant pointer', () => {
      // Tab 1 logs in.
      setTokens('a-token', 'r-token', TENANT_A);

      // A brand-new tab opens (per-tab binding gone, localStorage intact).
      openFreshTab();
      expect(sessionStorage.getItem(CURRENT_TENANT_KEY)).toBeNull();

      // Fresh tab resolves the last-login tenant + can read the token — no re-login.
      expect(getCurrentTenantId()).toBe(TENANT_A);
      expect(getAccessToken()).toBe('a-token');
      // ...and the fresh tab self-binds going forward.
      expect(sessionStorage.getItem(CURRENT_TENANT_KEY)).toBe(TENANT_A);
    });
  });

  describe('2-tenant isolation (OWASP A01 — no cross-tenant leak)', () => {
    it('a tab bound to tenant A reads A token, never B token', () => {
      // Tab A logs into tenant A.
      switchTabTo(TENANT_A);
      setTokens('tokenA', 'refreshA', TENANT_A);

      // Tab B (different tab) logs into tenant B — shares the same localStorage.
      switchTabTo(TENANT_B);
      setTokens('tokenB', 'refreshB', TENANT_B);

      // Back in Tab A's perspective: must still see ONLY tenant A's token.
      switchTabTo(TENANT_A);
      expect(getAccessToken()).toBe('tokenA');
      expect(getRefreshToken()).toBe('refreshA');
      expect(getTenantId()).toBe(TENANT_A);

      // Tab B's perspective: only tenant B's token.
      switchTabTo(TENANT_B);
      expect(getAccessToken()).toBe('tokenB');
      expect(getTenantId()).toBe(TENANT_B);

      // Namespaces are physically disjoint — no clobber.
      expect(localStorage.getItem(scopedKey(TENANT_A, ACCESS_SUB))).toBe('tokenA');
      expect(localStorage.getItem(scopedKey(TENANT_B, ACCESS_SUB))).toBe('tokenB');
    });

    it('getTenantId always matches the namespace getAccessToken read from', () => {
      switchTabTo(TENANT_A);
      setTokens(fakeJwt({ tenantId: TENANT_A }), 'rA', TENANT_A);
      // Header tenant id + token come from the SAME namespace (consistent pair).
      expect(getTenantId()).toBe(TENANT_A);
      expect(getTenantIdFromToken()).toBe(TENANT_A);
    });

    it('logout of tenant A does NOT remove tenant B tokens', () => {
      switchTabTo(TENANT_A);
      setTokens('tokenA', 'refreshA', TENANT_A);
      switchTabTo(TENANT_B);
      setTokens('tokenB', 'refreshB', TENANT_B);

      // Logout from Tab A.
      switchTabTo(TENANT_A);
      clearTokens();

      // Tenant A wiped...
      expect(localStorage.getItem(scopedKey(TENANT_A, ACCESS_SUB))).toBeNull();
      expect(localStorage.getItem(scopedKey(TENANT_A, REFRESH_SUB))).toBeNull();
      // ...tenant B fully intact.
      expect(localStorage.getItem(scopedKey(TENANT_B, ACCESS_SUB))).toBe('tokenB');
      expect(localStorage.getItem(scopedKey(TENANT_B, REFRESH_SUB))).toBe('refreshB');

      // Tab B still reads tenant B.
      switchTabTo(TENANT_B);
      expect(getAccessToken()).toBe('tokenB');
    });

    it('fresh-tab activeTenant resolution never mixes A token with B id', () => {
      // Owner logs into A then B sequentially (activeTenant ends pointing at B).
      setTokens('tokenA', 'refreshA', TENANT_A);
      setTokens('tokenB', 'refreshB', TENANT_B);
      expect(localStorage.getItem(ACTIVE_TENANT_KEY)).toBe(TENANT_B);

      // Fresh tab resolves to B and reads a CONSISTENT (token + id) pair for B.
      openFreshTab();
      expect(getCurrentTenantId()).toBe(TENANT_B);
      expect(getAccessToken()).toBe('tokenB'); // B's token, never A's
      expect(getTenantId()).toBe(TENANT_B); // B's id — pair stays consistent
    });
  });

  describe('clearTokens — current-tenant + legacy sweep', () => {
    it('drops binding + sweeps GAP-830 sessionStorage + pre-830 localStorage keys', () => {
      switchTabTo(TENANT_A);
      setTokens('a', 'r', TENANT_A);
      // simulate legacy residue
      sessionStorage.setItem('accessToken', 'legacy-sess');
      localStorage.setItem('access_token', 'legacy-a');
      localStorage.setItem('refresh_token', 'legacy-r');
      localStorage.setItem('auth-storage', '{"state":{}}');

      clearTokens();

      expect(getCurrentTenantId()).toBeNull(); // binding + pointer gone
      expect(sessionStorage.getItem(CURRENT_TENANT_KEY)).toBeNull();
      expect(sessionStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('access_token')).toBeNull();
      expect(localStorage.getItem('refresh_token')).toBeNull();
      expect(localStorage.getItem('auth-storage')).toBeNull();
      expect(localStorage.getItem(scopedKey(TENANT_A, STORE_SUB))).toBeNull();
    });

    it('clears activeTenant pointer only when it referenced the logged-out tenant', () => {
      setTokens('tokenA', 'refreshA', TENANT_A);
      setTokens('tokenB', 'refreshB', TENANT_B); // activeTenant -> B
      // Logout tenant A (NOT the active pointer).
      switchTabTo(TENANT_A);
      clearTokens();
      expect(localStorage.getItem(ACTIVE_TENANT_KEY)).toBe(TENANT_B); // preserved
    });
  });

  describe('clearLegacyLocalStorageTokens', () => {
    it('sweeps camelCase + snake_case + auth-storage legacy keys', () => {
      localStorage.setItem('accessToken', 'a');
      localStorage.setItem('access_token', 'a2');
      localStorage.setItem('refresh_token', 'r2');
      localStorage.setItem('auth-storage', 'blob');
      clearLegacyLocalStorageTokens();
      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('access_token')).toBeNull();
      expect(localStorage.getItem('refresh_token')).toBeNull();
      expect(localStorage.getItem('auth-storage')).toBeNull();
    });
  });

  describe('restorePersistedTokens (compat)', () => {
    it('binds a fresh tab to the last-login tenant when tokens exist', () => {
      setTokens('a', 'r', TENANT_A);
      openFreshTab();
      expect(restorePersistedTokens()).toBe(true);
      expect(getCurrentTenantId()).toBe(TENANT_A);
      expect(getAccessToken()).toBe('a');
    });

    it('returns false when session already active', () => {
      setTokens('a', 'r', TENANT_A);
      expect(restorePersistedTokens()).toBe(false); // already bound + has token
    });

    it('returns false when no remembered tokens', () => {
      expect(restorePersistedTokens()).toBe(false);
    });
  });

  describe('getTenantIdFromToken (JWT claim decode)', () => {
    it('extracts tenantId claim from the current access token', () => {
      setTokens(fakeJwt({ sub: 'u1', tenantId: TENANT_A }), 'r', TENANT_A);
      expect(getTenantIdFromToken()).toBe(TENANT_A);
    });

    it('returns null for malformed token', () => {
      switchTabTo(TENANT_A);
      setAccessToken('not.a.jwt.with.too.many.parts');
      expect(getTenantIdFromToken()).toBeNull();
    });

    it('returns null when no token', () => {
      expect(getTenantIdFromToken()).toBeNull();
    });

    it('returns null when tenantId claim absent', () => {
      switchTabTo(TENANT_A);
      setAccessToken(fakeJwt({ sub: 'u1' }));
      expect(getTenantIdFromToken()).toBeNull();
    });
  });

  describe('tenantScopedStateStorage (zustand persist adapter)', () => {
    it('namespaces the auth-store blob per bound tenant', () => {
      switchTabTo(TENANT_A);
      tenantScopedStateStorage.setItem('auth-storage', '{"a":1}');
      expect(localStorage.getItem(scopedKey(TENANT_A, STORE_SUB))).toBe('{"a":1}');
      expect(tenantScopedStateStorage.getItem('auth-storage')).toBe('{"a":1}');

      // A different tab/tenant sees its OWN (empty) blob, not tenant A's.
      switchTabTo(TENANT_B);
      expect(tenantScopedStateStorage.getItem('auth-storage')).toBeNull();
      tenantScopedStateStorage.setItem('auth-storage', '{"b":2}');
      expect(localStorage.getItem(scopedKey(TENANT_B, STORE_SUB))).toBe('{"b":2}');

      // Tenant A blob untouched by tenant B write.
      expect(localStorage.getItem(scopedKey(TENANT_A, STORE_SUB))).toBe('{"a":1}');
    });

    it('falls back to the anon namespace before any login', () => {
      tenantScopedStateStorage.setItem('auth-storage', '{"anon":true}');
      expect(localStorage.getItem(scopedKey('anon', STORE_SUB))).toBe('{"anon":true}');
    });
  });
});
