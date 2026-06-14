/**
 * E2E spec — Cross-product SSO (KiteHub → KiteClass) issue-code + redirect shape.
 *
 * The KiteHub half of the SSO chain per ADR-040 Option A / GAP-1138: an
 * authenticated owner on the KH dashboard clicks "Mở quản lý trường"
 * (`OpenSchoolManagementButton`, mounted at `(customer)/dashboard/page.tsx:403`),
 * which requests a one-time opaque code from kitehub-subscription
 * (`POST /api/v1/auth/sso/issue-code`) then redirects the browser to
 * `:3000/sso/callback?code=<opaque>`.
 *
 * SPLIT rationale (see kiteclass-frontend/e2e/sso/sso-callback-regression.spec.ts
 * header): the `/sso/callback` page is a KC route → exercised in the KC spec.
 * This spec owns the KH side: issue-code call + the ADR-040 §35 security property
 * "KHÔNG đặt JWT thô trên URL" — the redirect URL carries an OPAQUE one-time code,
 * NOT a raw JWT. The cross-origin navigation to :3000 is route-intercepted (no KC
 * server runs in the KH test); we assert the captured navigation URL shape.
 *
 * Regression-guard rationale (assertions reference incident IDs like the sister
 * beta-funnel chain spec):
 *   - GAP-1138 / ADR-040 §35: redirect carries opaque code, asserted NOT a JWT
 *     (a 3-segment dotted token leaking on the URL would be the bug this guards).
 *   - GAP-1305: deterministic SSO walk — the button is the chain's entry point
 *     a human G2★ walk drives; this guards its happy + error affordance at PR time.
 *
 * CI integration: kitehub-frontend `test:e2e:gates` (CI job runs
 * `pnpm test:e2e:gates:ci`) targets `e2e/sso/` after this PR — auto-picked up,
 * no workflow edit. Route-mocked: `page.route` intercepts all `/api/**` +
 * the cross-origin `/sso/callback` navigation.
 *
 * @see GAP-1138 (cross-product SSO KH→KC — ADR-040 Option A)
 * @see GAP-1305 (deterministic SSO walk)
 * @since SSO hardening (feature/sso-hardening-e2e) 2026-06-14
 */

import { test, expect } from '@playwright/test';
import { setupMockAuth, mockInstancesAPI } from '../utils/test-helpers';

const RUN_ID = String(Date.now());
const OPAQUE_CODE = `sso-onetime-${RUN_ID}-${Math.random().toString(36).slice(2, 10)}`;

/** JWT structure: exactly 3 dot-separated base64url segments. */
const JWT_SHAPE = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/;

test.describe('Cross-product SSO — KiteHub issue-code + redirect (ADR-040 / GAP-1138)', () => {
  test('happy path: click "Mở quản lý trường" → issue-code → redirect carries OPAQUE code, NOT JWT (ADR-040 §35)', async ({
    page,
  }) => {
    await setupMockAuth(page, 'OWNER');
    // Button renders inside the owner-instances loop → need ≥1 instance.
    await mockInstancesAPI(page);

    await page.route('**/api/v1/auth/sso/issue-code', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: OPAQUE_CODE, expiresIn: 60 }),
      });
    });

    // The button does `window.location.href = :3000/sso/callback?code=...` —
    // a cross-origin top-level navigation. No KC server runs here, so intercept
    // + fulfill a stub so the nav doesn't error; capture the requested URL.
    await page.route('**/sso/callback**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'text/html',
        body: '<html><body>KC SSO callback stub (intercepted in KH E2E)</body></html>',
      });
    });

    await page.goto('/dashboard');

    const button = page.getByRole('button', { name: /mở quản lý trường/i });
    await expect(button).toBeVisible({ timeout: 15000 });

    const navPromise = page.waitForRequest('**/sso/callback**', { timeout: 15000 });
    await button.click();
    const navReq = await navPromise;

    // Redirect targets the KC callback carrying the one-time code.
    const navUrl = navReq.url();
    expect(navUrl).toContain('/sso/callback?code=');

    // Assertion (ADR-040 §35 + GAP-1305): the code on the URL is the OPAQUE
    // one-time code, NOT a raw JWT (no 3-segment dotted token leaking on URL).
    const code = new URL(navUrl).searchParams.get('code');
    expect(code).toBe(OPAQUE_CODE);
    expect(code).not.toMatch(JWT_SHAPE);
  });

  test('sad path A — issue-code 401 (expired KH session) → global refresh→/login, NOT cross-origin redirect to KC', async ({
    page,
  }) => {
    await setupMockAuth(page, 'OWNER');
    await mockInstancesAPI(page);

    // `issue-code` is NOT in the apiClient AUTH_FLOW_401_PASSTHROUGH allowlist
    // (client.ts:57, GAP-924), so a 401 → the global response interceptor
    // attempts refresh, fails, then `window.location.href = '/login'`
    // (client.ts:103). The owner re-authenticates at KiteHub — the SSO chain
    // is NOT advanced with a bad/absent code. This guards against a failed
    // issue-code leaking a broken redirect to the KC callback.
    await page.route('**/api/v1/auth/sso/issue-code', (route) => {
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'SSO_UNAUTHORIZED', message: 'token expired' }),
      });
    });
    // Refresh fails deterministically → interceptor falls through to /login.
    await page.route('**/api/auth/refresh', (route) => {
      route.fulfill({ status: 401, contentType: 'application/json', body: '{}' });
    });

    let callbackNavigated = false;
    await page.route('**/sso/callback**', (route) => {
      callbackNavigated = true;
      route.fulfill({ status: 200, contentType: 'text/html', body: '<html></html>' });
    });

    await page.goto('/dashboard');
    const button = page.getByRole('button', { name: /mở quản lý trường/i });
    await expect(button).toBeVisible({ timeout: 15000 });
    await button.click();

    // 401 → re-login at KH, NOT a cross-origin hop to KC with a bad code.
    await page.waitForURL('**/login**', { timeout: 15000 });
    expect(callbackNavigated).toBe(false);
  });

  test('sad path B — issue-code 500 (server error) → inline alert, NO redirect (button catch branch)', async ({
    page,
  }) => {
    await setupMockAuth(page, 'OWNER');
    await mockInstancesAPI(page);

    // Non-401 errors are NOT intercepted by the refresh→/login path; they
    // reject through to issueSsoCode()'s caller → the component catch
    // (OpenSchoolManagementButton.tsx:37-40) renders an inline role="alert"
    // error and stays on the dashboard (no cross-origin redirect).
    await page.route('**/api/v1/auth/sso/issue-code', (route) => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'INTERNAL', message: 'boom' }),
      });
    });

    let callbackNavigated = false;
    await page.route('**/sso/callback**', (route) => {
      callbackNavigated = true;
      route.fulfill({ status: 200, contentType: 'text/html', body: '<html></html>' });
    });

    await page.goto('/dashboard');
    const button = page.getByRole('button', { name: /mở quản lý trường/i });
    await expect(button).toBeVisible({ timeout: 15000 });
    await button.click();

    await expect(
      page.getByRole('alert').filter({ hasText: /không thể mở trang quản lý trường/i }),
    ).toBeVisible({ timeout: 10000 });
    // Stayed on dashboard; no cross-origin redirect with a bad code.
    expect(callbackNavigated).toBe(false);
    expect(page.url()).toContain('/dashboard');
  });
});
