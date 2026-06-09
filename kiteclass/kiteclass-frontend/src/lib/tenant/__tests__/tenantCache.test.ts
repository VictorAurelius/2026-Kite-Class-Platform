/**
 * Unit tests for {@link tenantCache} (GAP-811).
 *
 * Covers: set/get, TTL expiry, cached-null distinction (vs cache miss),
 * clearCache helper.
 *
 * Ported per GAP-1077 từ kitehub-frontend.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { CACHE_TTL_MS, clearCache, getCached, setCached } from '../tenantCache';
import type { TenantResolveResult } from '../resolveTenant';

const SKY: TenantResolveResult = {
  id: '11111111-1111-1111-1111-111111111111',
  subdomain: 'sky',
  name: 'Trung tâm Anh ngữ Sky Education',
  status: 'ACTIVE',
};

describe('tenantCache', () => {
  beforeEach(() => {
    clearCache();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-06-01T00:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
    clearCache();
  });

  it('returns undefined on cache miss', () => {
    expect(getCached('sky')).toBeUndefined();
  });

  it('returns cached value after set', () => {
    setCached('sky', SKY);
    expect(getCached('sky')).toEqual(SKY);
  });

  it('distinguishes cached-null from cache-miss (negative cache)', () => {
    setCached('unknown', null);
    expect(getCached('unknown')).toBeNull();
    expect(getCached('never-set')).toBeUndefined();
  });

  it('expires entries after TTL', () => {
    setCached('sky', SKY);
    vi.advanceTimersByTime(CACHE_TTL_MS - 1);
    expect(getCached('sky')).toEqual(SKY);

    vi.advanceTimersByTime(2); // cross the TTL boundary
    expect(getCached('sky')).toBeUndefined();
  });

  it('replaces existing entry on re-set', () => {
    setCached('sky', SKY);
    const renamed: TenantResolveResult = { ...SKY, name: 'Sky English (renamed)' };
    setCached('sky', renamed);
    expect(getCached('sky')).toEqual(renamed);
  });

  it('clearCache removes all entries', () => {
    setCached('sky', SKY);
    setCached('pioneer', { ...SKY, id: 'p', subdomain: 'pioneer', name: 'Pioneer' });
    clearCache();
    expect(getCached('sky')).toBeUndefined();
    expect(getCached('pioneer')).toBeUndefined();
  });
});
