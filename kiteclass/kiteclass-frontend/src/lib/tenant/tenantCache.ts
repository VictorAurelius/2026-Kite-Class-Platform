/**
 * In-memory cache for tenant resolution results (GAP-811).
 *
 * 5-minute TTL per `documents/01-business/kitehub/marketing/api-contract.md` §9.2.
 *
 * Cache is per-Next-process — every cold start or after TTL expires, middleware
 * re-fetches from BE endpoint. Edge runtime compatible (no Node globals; uses
 * standard `Map` + `Date.now()`).
 *
 * Cached values include both `null` (= "BE confirmed not found") and full
 * `TenantResolveResult` (= "BE confirmed found + ACTIVE"). Suspended/410 errors
 * are NOT cached — middleware re-checks each request so a tenant transitioning
 * back to ACTIVE recovers within one request rather than TTL window.
 *
 * Ported per GAP-1077 từ kitehub-frontend — host→tenant middleware thuộc về
 * kiteclass-frontend (mỗi tenant = 1 trang học, resolve theo Host).
 *
 * @author KiteClass Team
 */

import type { TenantResolveResult } from './resolveTenant';

const TTL_MS = 5 * 60 * 1000; // 5 minutes per api-contract.md §9.2 FE cache row.

interface CacheEntry {
  /** Resolved tenant (when BE returned 200) or `null` (when BE returned 404). */
  value: TenantResolveResult | null;
  /** Epoch ms — entry expires when `Date.now()` ≥ this value. */
  expiresAt: number;
}

const store = new Map<string, CacheEntry>();

/**
 * Look up a cached tenant resolution by slug.
 *
 * Returns:
 * - `undefined` — no cached entry OR entry expired (caller should re-fetch)
 * - `null`      — BE previously returned 404 for this slug; cached miss
 * - `TenantResolveResult` — BE previously returned 200 + ACTIVE
 *
 * Distinguishing `undefined` vs `null` matters: cached-miss avoids re-hitting
 * BE for known-invalid slugs (cheap negative cache to mitigate enumeration).
 */
export function getCached(slug: string): TenantResolveResult | null | undefined {
  const entry = store.get(slug);
  if (!entry) return undefined;
  if (Date.now() >= entry.expiresAt) {
    store.delete(slug);
    return undefined;
  }
  return entry.value;
}

/**
 * Cache a tenant resolution result for the configured TTL (5 minutes).
 *
 * Pass `null` to record "BE confirmed 404 for this slug" — subsequent lookups
 * within the TTL window short-circuit without hitting BE.
 */
export function setCached(slug: string, value: TenantResolveResult | null): void {
  store.set(slug, {
    value,
    expiresAt: Date.now() + TTL_MS,
  });
}

/**
 * Clear all cached entries. Test-only helper; production code should rely on
 * TTL expiry rather than manual eviction.
 *
 * @internal
 */
export function clearCache(): void {
  store.clear();
}

/**
 * Cache TTL exposed for tests to fast-forward via `vi.useFakeTimers()`.
 *
 * @internal
 */
export const CACHE_TTL_MS = TTL_MS;
