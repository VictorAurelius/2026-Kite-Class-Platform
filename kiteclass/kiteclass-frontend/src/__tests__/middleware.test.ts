/**
 * Unit tests for the Next.js middleware host→tenant resolver (GAP-811).
 *
 * Covers extractSlugFromHost helper (table-driven) plus the middleware itself
 * exercised against MSW BE fixtures. Direct Playwright host-header simulation
 * lives in `e2e/host-tenant-resolution.spec.ts`.
 *
 * Ported per GAP-1077 từ kitehub-frontend.
 */

import { http, HttpResponse } from 'msw';
import { NextRequest } from 'next/server';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { server } from '@/mocks/server';
import { clearCache } from '@/lib/tenant/tenantCache';

import { extractSlugFromHost, middleware } from '../middleware';

const BASE_URL = 'http://kite-gateway:9000';

function makeReq(url: string, headers: Record<string, string> = {}): NextRequest {
  return new NextRequest(new URL(url), {
    headers: new Headers(headers),
  });
}

describe('extractSlugFromHost', () => {
  it.each<[string | null | undefined, string | null]>([
    ['sky.kiteclass.com', 'sky'],
    ['SKY.kiteclass.com', 'sky'],
    ['sky.kiteclass.com:3000', 'sky'],
    ['pioneer.kiteclass.com', 'pioneer'],
    // nip.io wildcard DNS — production-accurate local walk access-mode
    // (per g1-browser-walk-before-flip.md §3.1, landing-100 G2★)
    ['co-ha-toan.127.0.0.1.nip.io:3000', 'co-ha-toan'],
    ['kiteclass.com', null],
    ['www.kiteclass.com', null],
    ['api.kiteclass.com', null],
    ['admin.kiteclass.com', null],
    ['staging.kiteclass.com', null],
    ['localhost', null],
    ['localhost:4700', null],
    ['127.0.0.1', null],
    ['192.168.1.1:4700', null],
    ['', null],
    [null, null],
    [undefined, null],
  ])('extractSlugFromHost(%j) → %j', (host, expected) => {
    expect(extractSlugFromHost(host ?? null)).toBe(expected);
  });
});

describe('middleware', () => {
  beforeEach(() => {
    clearCache();
    process.env.INTERNAL_API_URL = BASE_URL;
  });

  afterEach(() => {
    delete process.env.INTERNAL_API_URL;
    clearCache();
  });

  it('injects x-tenant-id header when subdomain resolves to ACTIVE tenant', async () => {
    const req = makeReq('https://sky.kiteclass.com/', {
      host: 'sky.kiteclass.com',
    });

    const res = await middleware(req);

    // NextResponse.next() with rewritten request headers exposes them via the
    // `x-middleware-request-*` header set on the response.
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBe(
      '11111111-1111-1111-1111-111111111111',
    );
    expect(res.headers.get('x-middleware-request-x-tenant-subdomain')).toBe('sky');
  });

  it('passes through when host is apex / no subdomain', async () => {
    const req = makeReq('https://kiteclass.com/', { host: 'kiteclass.com' });
    const res = await middleware(req);
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBeNull();
  });

  it('flags x-tenant-not-found (no x-tenant-id) when subdomain is unknown (BE 404)', async () => {
    const req = makeReq('https://ghost.kiteclass.com/', { host: 'ghost.kiteclass.com' });
    const res = await middleware(req);
    // No tenant resolved → must NOT inject a tenant id (no silent fallback brand).
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBeNull();
    // But the not-found marker IS set so page/layout render the friendly
    // "không tìm thấy trung tâm" page instead of the env/default tenant landing.
    // GAP-1200.
    expect(res.headers.get('x-middleware-request-x-tenant-not-found')).toBe('ghost');
  });

  it('does NOT flag x-tenant-not-found for apex / no subdomain (dev fallback preserved)', async () => {
    const req = makeReq('https://kiteclass.com/', { host: 'kiteclass.com' });
    const res = await middleware(req);
    expect(res.headers.get('x-middleware-request-x-tenant-not-found')).toBeNull();
  });

  it('does NOT flag x-tenant-not-found for localhost without preview param', async () => {
    const req = makeReq('http://localhost:4700/', { host: 'localhost:4700' });
    const res = await middleware(req);
    // localhost has no subdomain → slug null → early pass-through → env/default
    // fallback stays intact (1-tenant-per-deploy / dev). GAP-1200.
    expect(res.headers.get('x-middleware-request-x-tenant-not-found')).toBeNull();
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBeNull();
  });

  it('redirects to /suspended when tenant is SUSPENDED (410)', async () => {
    const req = makeReq('https://suspended.kiteclass.com/dashboard', {
      host: 'suspended.kiteclass.com',
    });
    const res = await middleware(req);

    expect(res.status).toBe(307);
    const location = res.headers.get('location');
    expect(location).toContain('/suspended');
    expect(location).toContain('slug=suspended');
    expect(location).toContain('status=suspended');
  });

  it('passes through on /suspended itself — no redirect loop (GAP-1199)', async () => {
    const req = makeReq(
      'https://suspended.kiteclass.com/suspended?slug=suspended&status=suspended',
      { host: 'suspended.kiteclass.com' },
    );
    const res = await middleware(req);

    // Without the loop guard this returned 307 → /suspended again →
    // ERR_TOO_MANY_REDIRECTS in the browser.
    expect(res.status).not.toBe(307);
    expect(res.headers.get('location')).toBeNull();
  });

  it('honours ?tenant= preview query param when host has no subdomain', async () => {
    const req = makeReq('https://localhost:4700/?tenant=sky', {
      host: 'localhost:4700',
    });
    const res = await middleware(req);
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBe(
      '11111111-1111-1111-1111-111111111111',
    );
  });

  it('preview query param overrides host subdomain', async () => {
    const req = makeReq('https://sky.kiteclass.com/?tenant=pioneer', {
      host: 'sky.kiteclass.com',
    });
    const res = await middleware(req);
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBe(
      '22222222-2222-2222-2222-222222222222',
    );
  });

  it('degrades gracefully when BE is unreachable (5xx → flag header, no crash)', async () => {
    server.use(
      http.get('*/api/v1/public/tenants/by-subdomain/:slug', () =>
        HttpResponse.json({ error: 'INTERNAL' }, { status: 503 }),
      ),
    );

    const req = makeReq('https://sky.kiteclass.com/', { host: 'sky.kiteclass.com' });
    const res = await middleware(req);

    // No tenant id injected (couldn't resolve)
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBeNull();
    // But the warning header IS set so downstream can show fallback banner.
    expect(res.headers.get('x-middleware-request-x-tenant-resolve-error')).toBe(
      'upstream-unavailable',
    );
  });

  it('passes through cleanly for localhost without preview param', async () => {
    const req = makeReq('http://localhost:4700/', { host: 'localhost:4700' });
    const res = await middleware(req);
    expect(res.headers.get('x-middleware-request-x-tenant-id')).toBeNull();
  });
});
