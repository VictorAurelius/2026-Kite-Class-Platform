/**
 * 2-tab collision simulation tests (GAP-599 Wave 92 Bucket B).
 *
 * Real browsers enforce `sessionStorage` per-tab isolation natively — vitest
 * + happy-dom share a single sessionStorage instance per test file. We
 * simulate two-tab isolation by stubbing two distinct sessionStorage
 * instances and exercising the jwt-storage API against each.
 *
 * The contract this test enforces: when the API is given two distinct
 * sessionStorage instances (= "tab A" + "tab B"), tokens written to one
 * MUST NOT appear in the other. This is the property real browsers give
 * us for free; the test guards against accidental switch to a shared
 * store (e.g., localStorage) that would BREAK that property.
 */

import { describe, it, expect } from 'vitest';

/**
 * Minimal in-memory Storage-shape used to simulate per-tab `sessionStorage`.
 */
function makeFakeStorage(): Storage {
  const map = new Map<string, string>();
  return {
    get length() {
      return map.size;
    },
    clear: () => map.clear(),
    getItem: (key) => (map.has(key) ? map.get(key) ?? null : null),
    key: (i) => Array.from(map.keys())[i] ?? null,
    removeItem: (key) => {
      map.delete(key);
    },
    setItem: (key, value) => {
      map.set(key, value);
    },
  };
}

/**
 * Tab-scoped wrapper around jwt-storage. Each tab gets its own `sessionStorage`
 * instance, swapped onto `window.sessionStorage` before invoking the API and
 * restored after.
 */
async function withTabSessionStorage<T>(
  storage: Storage,
  fn: () => Promise<T> | T
): Promise<T> {
  const original = window.sessionStorage;
  Object.defineProperty(window, 'sessionStorage', {
    value: storage,
    writable: true,
    configurable: true,
  });
  try {
    return await fn();
  } finally {
    Object.defineProperty(window, 'sessionStorage', {
      value: original,
      writable: true,
      configurable: true,
    });
  }
}

describe('jwt-storage 2-tab simulation (GAP-599 closure)', () => {
  it('tab A login does NOT leak token into tab B', async () => {
    const tabA = makeFakeStorage();
    const tabB = makeFakeStorage();

    // Tab A: admin login
    await withTabSessionStorage(tabA, async () => {
      const { setTokens } = await import('../jwt-storage');
      setTokens('admin-access-jwt', 'admin-refresh-jwt');
    });

    // Tab B: tenant owner login (fresh storage)
    await withTabSessionStorage(tabB, async () => {
      const { setTokens, getAccessToken } = await import('../jwt-storage');
      // Tab B should see NO admin token at this point.
      expect(getAccessToken()).toBeNull();

      setTokens('owner-access-jwt', 'owner-refresh-jwt');
      expect(getAccessToken()).toBe('owner-access-jwt');
    });

    // Tab A still has its admin token (not overwritten by Tab B).
    await withTabSessionStorage(tabA, async () => {
      const { getAccessToken, getRefreshToken } = await import('../jwt-storage');
      expect(getAccessToken()).toBe('admin-access-jwt');
      expect(getRefreshToken()).toBe('admin-refresh-jwt');
    });

    // Tab B still has its owner token.
    await withTabSessionStorage(tabB, async () => {
      const { getAccessToken, getRefreshToken } = await import('../jwt-storage');
      expect(getAccessToken()).toBe('owner-access-jwt');
      expect(getRefreshToken()).toBe('owner-refresh-jwt');
    });
  });

  it('tab A logout does NOT clear tab B tokens', async () => {
    const tabA = makeFakeStorage();
    const tabB = makeFakeStorage();

    await withTabSessionStorage(tabA, async () => {
      const { setTokens } = await import('../jwt-storage');
      setTokens('A-access', 'A-refresh');
    });

    await withTabSessionStorage(tabB, async () => {
      const { setTokens } = await import('../jwt-storage');
      setTokens('B-access', 'B-refresh');
    });

    // Tab A logout
    await withTabSessionStorage(tabA, async () => {
      const { clearTokens, getAccessToken } = await import('../jwt-storage');
      clearTokens();
      expect(getAccessToken()).toBeNull();
    });

    // Tab B unaffected
    await withTabSessionStorage(tabB, async () => {
      const { getAccessToken, getRefreshToken } = await import('../jwt-storage');
      expect(getAccessToken()).toBe('B-access');
      expect(getRefreshToken()).toBe('B-refresh');
    });
  });

  it('clearLegacyLocalStorageTokens sweeps shared localStorage from both tabs', async () => {
    // localStorage IS shared between tabs by browser. Set a legacy token...
    localStorage.setItem('accessToken', 'legacy-from-old-build');
    localStorage.setItem('refreshToken', 'legacy-refresh-from-old-build');

    const tabA = makeFakeStorage();
    await withTabSessionStorage(tabA, async () => {
      const { clearLegacyLocalStorageTokens, getAccessToken } = await import(
        '../jwt-storage'
      );
      // Tab A's sessionStorage is empty.
      expect(getAccessToken()).toBeNull();

      clearLegacyLocalStorageTokens();

      // localStorage swept.
      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });
});
