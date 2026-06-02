/**
 * Unit tests for the JWT sessionStorage facade (GAP-830).
 *
 * Verifies per-tab isolation semantics + key-name reconcile + SSR guards +
 * remember-me localStorage mirror + 2-tab no-collision simulation.
 */

import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  ACCESS_TOKEN_KEY,
  REFRESH_TOKEN_KEY,
  TENANT_ID_KEY,
  getAccessToken,
  getRefreshToken,
  getTenantId,
  getTenantIdFromToken,
  setAccessToken,
  setRefreshToken,
  setTenantId,
  setTokens,
  restorePersistedTokens,
  clearTokens,
  clearLegacyLocalStorageTokens,
} from '../jwt-storage';

// Helper: build a fake JWT with given payload (header.payload.signature).
function fakeJwt(payload: Record<string, unknown>): string {
  const b64 = (o: unknown) =>
    btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64(payload)}.sig`;
}

describe('jwt-storage facade (GAP-830)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });
  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  describe('basic get/set via sessionStorage', () => {
    it('setTokens writes access + refresh + tenantId to sessionStorage', () => {
      setTokens('a-token', 'r-token', 'tenant-1');
      expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBe('a-token');
      expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBe('r-token');
      expect(sessionStorage.getItem(TENANT_ID_KEY)).toBe('tenant-1');
      expect(getAccessToken()).toBe('a-token');
      expect(getRefreshToken()).toBe('r-token');
      expect(getTenantId()).toBe('tenant-1');
    });

    it('setTokens does NOT write to localStorage by default (per-tab only)', () => {
      setTokens('a-token', 'r-token', 'tenant-1');
      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    });

    it('individual setters work', () => {
      setAccessToken('a2');
      setRefreshToken('r2');
      setTenantId('t2');
      expect(getAccessToken()).toBe('a2');
      expect(getRefreshToken()).toBe('r2');
      expect(getTenantId()).toBe('t2');
    });

    it('getters return null when absent', () => {
      expect(getAccessToken()).toBeNull();
      expect(getRefreshToken()).toBeNull();
      expect(getTenantId()).toBeNull();
    });
  });

  describe('clearTokens', () => {
    it('removes session + localStorage tokens + legacy snake_case + auth-storage blob', () => {
      setTokens('a', 'r', 't', true); // also mirrors to localStorage
      // simulate legacy + zustand persist residue
      localStorage.setItem('access_token', 'legacy-a');
      localStorage.setItem('refresh_token', 'legacy-r');
      localStorage.setItem('auth-storage', '{"state":{}}');

      clearTokens();

      expect(getAccessToken()).toBeNull();
      expect(getRefreshToken()).toBeNull();
      expect(getTenantId()).toBeNull();
      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem('access_token')).toBeNull();
      expect(localStorage.getItem('refresh_token')).toBeNull();
      expect(localStorage.getItem('auth-storage')).toBeNull();
    });
  });

  describe('remember-me localStorage mirror + restore', () => {
    it('persist=true mirrors tokens to localStorage', () => {
      setTokens('a', 'r', 't', true);
      expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('a');
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe('r');
      expect(localStorage.getItem(TENANT_ID_KEY)).toBe('t');
    });

    it('restorePersistedTokens rehydrates session from localStorage when session empty', () => {
      localStorage.setItem(ACCESS_TOKEN_KEY, 'a');
      localStorage.setItem(REFRESH_TOKEN_KEY, 'r');
      localStorage.setItem(TENANT_ID_KEY, 't');
      expect(getAccessToken()).toBeNull(); // session empty (fresh tab)

      const restored = restorePersistedTokens();

      expect(restored).toBe(true);
      expect(getAccessToken()).toBe('a');
      expect(getRefreshToken()).toBe('r');
      expect(getTenantId()).toBe('t');
    });

    it('restorePersistedTokens is no-op when session already has a token', () => {
      setAccessToken('active');
      localStorage.setItem(ACCESS_TOKEN_KEY, 'stale');
      localStorage.setItem(REFRESH_TOKEN_KEY, 'stale-r');
      expect(restorePersistedTokens()).toBe(false);
      expect(getAccessToken()).toBe('active'); // not overwritten
    });

    it('restorePersistedTokens is no-op when no remembered tokens', () => {
      expect(restorePersistedTokens()).toBe(false);
    });
  });

  describe('key-name reconcile (GAP-830 — camelCase canonical)', () => {
    it('facade uses camelCase keys, not snake_case', () => {
      setTokens('a', 'r');
      // canonical camelCase keys present
      expect(sessionStorage.getItem('accessToken')).toBe('a');
      expect(sessionStorage.getItem('refreshToken')).toBe('r');
      // snake_case (old student-register drift) NOT used
      expect(sessionStorage.getItem('access_token')).toBeNull();
      expect(sessionStorage.getItem('refresh_token')).toBeNull();
    });

    it('clearLegacyLocalStorageTokens sweeps both camelCase + snake_case', () => {
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

  describe('getTenantIdFromToken (JWT claim decode)', () => {
    it('extracts tenantId claim from a valid token', () => {
      setAccessToken(fakeJwt({ sub: 'u1', tenantId: 'abc-123' }));
      expect(getTenantIdFromToken()).toBe('abc-123');
    });

    it('returns null for malformed token', () => {
      setAccessToken('not.a.jwt.with.too.many.parts');
      expect(getTenantIdFromToken()).toBeNull();
    });

    it('returns null when no token', () => {
      expect(getTenantIdFromToken()).toBeNull();
    });

    it('returns null when tenantId claim absent', () => {
      setAccessToken(fakeJwt({ sub: 'u1' }));
      expect(getTenantIdFromToken()).toBeNull();
    });
  });
});
