/**
 * Unit tests for {@link resolveTenant} (GAP-811).
 *
 * MSW handlers under `src/mocks/tenant-handlers.ts` stub the BE endpoint with
 * fixtures `sky` / `pioneer` (ACTIVE), `suspended` (410 GONE), and any other
 * slug → 404.
 *
 * Tests assert:
 *   - 200 OK → returns TenantResolveResult
 *   - 404    → returns null
 *   - 410    → throws TenantSuspendedError with status + code
 *   - 5xx    → throws TenantResolveNetworkError
 *   - cache  → second call within TTL skips the network
 *   - cache  → entry expires after TTL
 *   - invalid slug → returns null without hitting the network
 *
 * Ported per GAP-1077 từ kitehub-frontend.
 */

import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { server } from '@/mocks/server';

import { CACHE_TTL_MS, clearCache } from '../tenantCache';
import {
  TenantResolveNetworkError,
  resolveTenant,
} from '../resolveTenant';

const BASE_URL = 'http://kite-gateway:9000';

describe('resolveTenant', () => {
  beforeEach(() => {
    clearCache();
    process.env.INTERNAL_API_URL = BASE_URL;
  });

  afterEach(() => {
    delete process.env.INTERNAL_API_URL;
    clearCache();
  });

  it('returns a tenant for the ACTIVE fixture (sky)', async () => {
    const tenant = await resolveTenant('sky');
    expect(tenant).toEqual({
      id: '11111111-1111-1111-1111-111111111111',
      subdomain: 'sky',
      name: 'Trung tâm Anh ngữ Sky Education',
      status: 'ACTIVE',
    });
  });

  it('returns null when BE returns 404', async () => {
    const tenant = await resolveTenant('nonexistent');
    expect(tenant).toBeNull();
  });

  it('throws TenantSuspendedError when BE returns 410', async () => {
    await expect(resolveTenant('suspended')).rejects.toMatchObject({
      name: 'TenantSuspendedError',
      slug: 'suspended',
      status: 'SUSPENDED',
      code: 'TENANT_SUSPENDED',
    });
  });

  it('throws TenantResolveNetworkError on 5xx', async () => {
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', () =>
        HttpResponse.json({ error: 'INTERNAL' }, { status: 503 }),
      ),
    );
    await expect(resolveTenant('sky')).rejects.toBeInstanceOf(
      TenantResolveNetworkError,
    );
  });

  it('throws TenantResolveNetworkError on fetch failure', async () => {
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', () =>
        HttpResponse.error(),
      ),
    );
    await expect(resolveTenant('sky')).rejects.toBeInstanceOf(
      TenantResolveNetworkError,
    );
  });

  it('returns null for malformed slugs without hitting the network', async () => {
    let calls = 0;
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', () => {
        calls += 1;
        return HttpResponse.json({}, { status: 200 });
      }),
    );

    expect(await resolveTenant('')).toBeNull();
    expect(await resolveTenant('-bad')).toBeNull();
    expect(await resolveTenant('UPPER')).toBeNull();
    expect(await resolveTenant('bad--leading-trail-')).toBeNull();
    expect(calls).toBe(0);
  });

  it('caches the result for repeated lookups (200 path)', async () => {
    let calls = 0;
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', ({ params }) => {
        calls += 1;
        return HttpResponse.json({
          id: '11111111-1111-1111-1111-111111111111',
          subdomain: params.slug,
          name: 'Sky',
          status: 'ACTIVE',
        });
      }),
    );

    await resolveTenant('sky');
    await resolveTenant('sky');
    await resolveTenant('sky');
    expect(calls).toBe(1);
  });

  it('caches 404 (negative cache) — second call short-circuits', async () => {
    let calls = 0;
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', () => {
        calls += 1;
        return HttpResponse.json({ error: 'TENANT_NOT_FOUND' }, { status: 404 });
      }),
    );

    expect(await resolveTenant('ghost')).toBeNull();
    expect(await resolveTenant('ghost')).toBeNull();
    expect(calls).toBe(1);
  });

  it('does NOT cache errors — transient failures recover on next call', async () => {
    let calls = 0;
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', () => {
        calls += 1;
        if (calls === 1) return HttpResponse.error();
        return HttpResponse.json({
          id: '11111111-1111-1111-1111-111111111111',
          subdomain: 'sky',
          name: 'Sky',
          status: 'ACTIVE',
        });
      }),
    );

    await expect(resolveTenant('sky')).rejects.toBeInstanceOf(
      TenantResolveNetworkError,
    );
    // Retry — must hit network again, then succeed.
    const tenant = await resolveTenant('sky');
    expect(tenant?.subdomain).toBe('sky');
    expect(calls).toBe(2);
  });

  it('refreshes from BE after TTL expires', async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-06-01T00:00:00Z'));

    let calls = 0;
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', () => {
        calls += 1;
        return HttpResponse.json({
          id: '11111111-1111-1111-1111-111111111111',
          subdomain: 'sky',
          name: 'Sky',
          status: 'ACTIVE',
        });
      }),
    );

    await resolveTenant('sky');
    vi.advanceTimersByTime(CACHE_TTL_MS + 1);
    await resolveTenant('sky');
    expect(calls).toBe(2);

    vi.useRealTimers();
  });

  it('uses options.baseUrl when supplied (override env)', async () => {
    let lastUrl = '';
    server.use(
      http.get('https://example.com/api/v1/public/tenants/by-subdomain/:slug', ({ request }) => {
        lastUrl = request.url;
        return HttpResponse.json({
          id: '11111111-1111-1111-1111-111111111111',
          subdomain: 'sky',
          name: 'Sky',
          status: 'ACTIVE',
        });
      }),
    );

    await resolveTenant('sky', { baseUrl: 'https://example.com' });
    expect(lastUrl).toContain('https://example.com/api/v1/public/tenants/by-subdomain/sky');
  });
});
