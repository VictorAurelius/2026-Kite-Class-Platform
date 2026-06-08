/**
 * E2E tests for the host→tenant resolution middleware (GAP-811).
 *
 * Simulates 5 scenarios per GAP-811 §AC by route-mocking the Public Tenant
 * Resolve endpoint so the spec is hermetic and does NOT require the BE
 * (GAP-813) to be live.
 *
 * Scenarios:
 *   (a) Valid preview `?tenant=sky`      → 200 OK → middleware injects `x-tenant-id`
 *   (b) Unknown slug                     → 404      → pass through, app handles
 *   (c) Suspended slug                   → 410      → 307 redirect /suspended
 *   (d) Preview mode `?tenant=pioneer`   → 200 OK → resolves via query param
 *   (e) BE-down (mock 500)               → 5xx      → graceful pass-through
 *
 * Note on host simulation: Playwright respects the URL's host when running
 * against `baseURL`. For real subdomain testing we'd need `/etc/hosts` entries
 * or a wildcard DNS — here we use the `?tenant=` preview signal as the primary
 * mechanism + assert via the proxy that the middleware path runs end-to-end.
 *
 * Ported per GAP-1077 từ kitehub-frontend — host→tenant middleware thuộc về
 * kiteclass-frontend.
 */

import { test, expect, type Route } from '@playwright/test';

const TENANT_ENDPOINT = '**/api/v1/public/tenants/by-subdomain/*';

/**
 * Set up a route mock for the Public Tenant Resolve endpoint.
 *
 * @param respond callback returning the desired status + body per slug
 */
async function mockTenantEndpoint(
  page: import('@playwright/test').Page,
  respond: (slug: string) => { status: number; body?: unknown },
) {
  await page.route(TENANT_ENDPOINT, async (route: Route) => {
    const url = new URL(route.request().url());
    const segments = url.pathname.split('/');
    const slug = segments[segments.length - 1] ?? '';
    const { status, body } = respond(slug);
    await route.fulfill({
      status,
      contentType: 'application/json',
      body: body !== undefined ? JSON.stringify(body) : '',
    });
  });
}

test.describe('GAP-811 — host→tenant resolution', () => {
  test('(a) preview ?tenant=sky → page renders without redirect', async ({ page }) => {
    await mockTenantEndpoint(page, (slug) => {
      if (slug === 'sky') {
        return {
          status: 200,
          body: {
            id: '11111111-1111-1111-1111-111111111111',
            subdomain: 'sky',
            name: 'Trung tâm Anh ngữ Sky Education',
            status: 'ACTIVE',
          },
        };
      }
      return { status: 404, body: { error: 'TENANT_NOT_FOUND' } };
    });

    const response = await page.goto('/?tenant=sky');
    // Page should render normally — middleware injects header without redirect.
    expect(response?.status()).toBeLessThan(400);
    // No redirect to /suspended.
    expect(page.url()).not.toContain('/suspended');
  });

  test('(b) unknown subdomain (BE 404) → pass through, no redirect', async ({ page }) => {
    await mockTenantEndpoint(page, () => ({
      status: 404,
      body: { error: 'TENANT_NOT_FOUND', message: 'no such tenant' },
    }));

    const response = await page.goto('/?tenant=ghost');
    expect(response?.status()).toBeLessThan(500);
    expect(page.url()).not.toContain('/suspended');
  });

  test('(c) suspended tenant (BE 410) → redirect to /suspended', async ({ page }) => {
    await mockTenantEndpoint(page, () => ({
      status: 410,
      body: {
        error: 'TENANT_SUSPENDED',
        message: "Tenant 'suspended' is currently suspended.",
        status: 'SUSPENDED',
      },
    }));

    await page.goto('/?tenant=suspended', { waitUntil: 'domcontentloaded' });
    // After middleware redirect, URL should include /suspended path
    await expect.poll(() => page.url(), { timeout: 5000 }).toContain('/suspended');
    expect(page.url()).toContain('slug=suspended');
    expect(page.url()).toContain('status=suspended');
  });

  test('(d) preview mode resolves via ?tenant= query param', async ({ page }) => {
    let calls = 0;
    await page.route(TENANT_ENDPOINT, async (route) => {
      calls += 1;
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '22222222-2222-2222-2222-222222222222',
          subdomain: 'pioneer',
          name: 'Trung tâm Pioneer',
          status: 'ACTIVE',
        }),
      });
    });

    await page.goto('/?tenant=pioneer');
    expect(calls).toBeGreaterThan(0); // middleware invoked the resolver
    expect(page.url()).not.toContain('/suspended');
  });

  test('(e) BE down (mock 500) → graceful pass-through, no crash', async ({ page }) => {
    await mockTenantEndpoint(page, () => ({
      status: 503,
      body: { error: 'INTERNAL', message: 'upstream unavailable' },
    }));

    const response = await page.goto('/?tenant=sky');
    // App should still render — 5xx from the tenant endpoint MUST NOT crash
    // the marketing site (per GAP-811 §AC graceful fallback).
    expect(response?.status()).toBeLessThan(500);
    expect(page.url()).not.toContain('/suspended');
  });
});
