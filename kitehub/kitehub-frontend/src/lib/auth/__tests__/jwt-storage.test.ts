/**
 * Unit tests for JWT storage abstraction (GAP-599).
 *
 * Closes 2-tab collision: each tab MUST have isolated JWT. sessionStorage
 * is per-tab native (browser invariant) so two tabs cannot collide on the
 * same origin.
 *
 * @since Wave 92 Bucket B (GAP-599 Option A)
 */

import { describe, it, expect, beforeEach } from 'vitest';

import {
  setAccessToken,
  setRefreshToken,
  setTokens,
  getAccessToken,
  getRefreshToken,
  clearTokens,
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
} from '../jwt-storage';

describe('jwt-storage (GAP-599 — sessionStorage per-tab isolation)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  describe('setAccessToken / getAccessToken', () => {
    it('stores access token in sessionStorage (not localStorage)', () => {
      setAccessToken('jwt-access');

      expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBe('jwt-access');
      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    });

    it('returns stored access token', () => {
      setAccessToken('jwt-access');
      expect(getAccessToken()).toBe('jwt-access');
    });

    it('returns null when no token set', () => {
      expect(getAccessToken()).toBeNull();
    });
  });

  describe('setRefreshToken / getRefreshToken', () => {
    it('stores refresh token in sessionStorage', () => {
      setRefreshToken('jwt-refresh');

      expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBe('jwt-refresh');
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    });

    it('returns stored refresh token', () => {
      setRefreshToken('jwt-refresh');
      expect(getRefreshToken()).toBe('jwt-refresh');
    });
  });

  describe('setTokens (atomic pair)', () => {
    it('stores both access + refresh tokens in single call', () => {
      setTokens('access-x', 'refresh-y');

      expect(getAccessToken()).toBe('access-x');
      expect(getRefreshToken()).toBe('refresh-y');
    });
  });

  describe('clearTokens', () => {
    it('removes both access + refresh tokens from sessionStorage', () => {
      setTokens('a', 'r');
      clearTokens();

      expect(getAccessToken()).toBeNull();
      expect(getRefreshToken()).toBeNull();
    });

    it('does not throw when called with no tokens present', () => {
      expect(() => clearTokens()).not.toThrow();
    });
  });

  describe('SSR safety (typeof window check)', () => {
    it('getAccessToken returns null in SSR-like env (no window)', () => {
      const originalWindow = global.window;
      // @ts-expect-error - intentional SSR simulation
      delete global.window;

      expect(getAccessToken()).toBeNull();

      global.window = originalWindow;
    });

    it('setAccessToken no-ops in SSR-like env', () => {
      const originalWindow = global.window;
      // @ts-expect-error - intentional SSR simulation
      delete global.window;

      expect(() => setAccessToken('x')).not.toThrow();

      global.window = originalWindow;
    });
  });

  describe('2-tab collision simulation (GAP-599 root case)', () => {
    /**
     * Simulate 2-tab walkthrough: tab A logs in admin, tab B logs in tenant
     * owner. sessionStorage is per-tab native in real browsers, so the test
     * environment can only DEMONSTRATE the contract — the actual isolation
     * is enforced by the browser, not by our code.
     *
     * What we CAN verify: every write touches sessionStorage and zero
     * writes touch localStorage. This is the contract that closes the
     * collision (per-tab native isolation).
     */
    it('writes never touch localStorage (cross-tab shared store)', () => {
      setTokens('admin-jwt', 'admin-refresh');
      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();

      clearTokens();
      setTokens('tenant-jwt', 'tenant-refresh');
      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    });

    it('clearTokens DOES remove from localStorage (logout cleanup)', () => {
      // Per jwt-storage.ts docstring: "Removes both access + refresh tokens
      // from sessionStorage AND localStorage (logout flow). Logout always
      // clears both tiers regardless of remember-me." Logout discipline
      // ensures stale legacy localStorage tokens cannot survive a logout
      // event and be reused cross-tab. The dedicated
      // clearLegacyLocalStorageTokens() helper remains as a one-time
      // migration sweep callable at app bootstrap (no auth context required).
      localStorage.setItem(ACCESS_TOKEN_KEY, 'legacy');

      clearTokens();

      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    });
  });

  describe('storage key constants', () => {
    it('exposes ACCESS_TOKEN_KEY constant', () => {
      expect(ACCESS_TOKEN_KEY).toBe('accessToken');
    });

    it('exposes REFRESH_TOKEN_KEY constant', () => {
      expect(REFRESH_TOKEN_KEY).toBe('refreshToken');
    });
  });
});

describe('clearLegacyLocalStorageTokens (migration helper)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it('removes legacy localStorage tokens from previous build', async () => {
    const { clearLegacyLocalStorageTokens } = await import('../jwt-storage');

    localStorage.setItem(ACCESS_TOKEN_KEY, 'legacy-access');
    localStorage.setItem(REFRESH_TOKEN_KEY, 'legacy-refresh');

    clearLegacyLocalStorageTokens();

    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
  });

  it('does not throw when no legacy tokens exist', async () => {
    const { clearLegacyLocalStorageTokens } = await import('../jwt-storage');
    expect(() => clearLegacyLocalStorageTokens()).not.toThrow();
  });

  it('does not affect sessionStorage tokens', async () => {
    const { clearLegacyLocalStorageTokens } = await import('../jwt-storage');

    setTokens('session-access', 'session-refresh');
    clearLegacyLocalStorageTokens();

    expect(getAccessToken()).toBe('session-access');
    expect(getRefreshToken()).toBe('session-refresh');
  });
});
