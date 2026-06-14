/**
 * E2E spec — Cross-product SSO (KiteHub → KiteClass) callback regression guard.
 *
 * Walks the KiteClass `/sso/callback` route end-to-end (route-mocked exchange),
 * the KC half of the SSO chain per ADR-040 Option A / GAP-1138:
 *
 *   KH dashboard → "Mở quản lý trường" → issue-code → redirect
 *   :3000/sso/callback?code=<opaque> → exchange → KC session → role-home (no re-login)
 *
 * WHY this spec lives in kiteclass-frontend (NOT kitehub-frontend, where the
 * sister beta-funnel chain spec lives):
 *   `/sso/callback` is a kiteclass-frontend route (`src/app/sso/callback/page.tsx`)
 *   served by the KC dev server (:4700). A kitehub-frontend Playwright spec runs
 *   against the KH dev server (:4701) which does NOT serve `/sso/callback` → the
 *   page would 404. The beta-funnel pattern's core technique is "navigate to the
 *   FE route that actually renders the page"; honoring it forces a SPLIT:
 *     - this spec  → KC callback (exchange + sad paths + StrictMode guard) ← bulk
 *     - kitehub-frontend/e2e/sso/sso-issue-redirect.spec.ts → KH issue→redirect shape
 *
 * Regression-guard rationale (assertions reference incident IDs like the
 * beta-funnel spec):
 *   - GAP-1138 AC#5 (single-use code): a replayed/consumed code → 401 → error UI
 *     (NOT a silent re-login or crash).
 *   - GAP-1305 (deterministic SSO walk): the opaque one-time code in the URL is
 *     forwarded verbatim to exchange — asserted NOT a JWT (ADR-040 §35: "KHÔNG
 *     đặt JWT thô trên URL"). The single-use + StrictMode `ranRef` guard (page.tsx:47)
 *     prevents the React double-mount from replaying the code.
 *
 * CI integration: kiteclass-frontend `test:e2e:gates` (frontend-ci.yml job runs
 * `pnpm test:e2e:gates`) targets `e2e/sso/` after this PR — auto-picked up, no
 * workflow edit. Playwright `webServer` config auto-boots `pnpm dev` (:4700).
 *
 * Local stack walk (full BE + Redis one-time-code consume + real JWT mint):
 * deferred to GAP-1138 AC#1/AC#5 human G2★ browser walk — this route-mocked spec
 * is the PR-time regression guard, NOT the production-parity walk.
 *
 * @see GAP-1138 (cross-product SSO KH→KC — ADR-040 Option A)
 * @see GAP-1305 (deterministic SSO walk — dedicated single-instance owner seed)
 * @since SSO hardening (feature/sso-hardening-e2e) 2026-06-14
 */

import { test, expect, Request } from '@playwright/test';

// Unique-per-run opaque code so mock state stays isolated between specs.
const RUN_ID = String(Date.now());
const OPAQUE_CODE = `sso-onetime-${RUN_ID}-${Math.random().toString(36).slice(2, 10)}`;

/** JWT structure: exactly 3 dot-separated base64url segments. */
const JWT_SHAPE = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/;

/**
 * Build a fake HS512-shaped KiteHub JWT carrying a `tenantId` claim, so the
 * callback's `tenantIdFromToken(accessToken)` (page.tsx:27) decodes a real tenant
 * (not the DEFAULT_TENANT_ID fallback). Signature segment is a stub — the FE only
 * reads the payload; the BE verifies signatures (out of scope for a route-mock).
 */
function buildFakeKhJwt(tenantId: string, role = 'OWNER'): string {
  const b64url = (obj: unknown) =>
    Buffer.from(JSON.stringify(obj)).toString('base64url');
  const header = b64url({ alg: 'HS512', typ: 'JWT' });
  const payload = b64url({
    sub: '00000000-0000-0000-0000-0000000000aa',
    email: 'sso.owner@skytest.test',
    role,
    tenantId,
    type: 'access',
  });
  return `${header}.${payload}.stub-signature-not-verified-by-fe`;
}

const TENANT_ID = 'aaaabbbb-0000-0000-0000-000000000001';

/** Flat shape returned by `POST /api/v1/auth/sso/exchange` (SsoExchangeResponse). */
function buildExchangeResponse(role = 'OWNER') {
  return {
    accessToken: buildFakeKhJwt(TENANT_ID, role),
    refreshToken: 'sso-refresh-token-stub',
    user: {
      id: '00000000-0000-0000-0000-0000000000aa',
      email: 'sso.owner@skytest.test',
      name: 'SSO Owner Skytest',
      role,
    },
  };
}

test.describe('Cross-product SSO — KiteClass callback regression (ADR-040 / GAP-1138)', () => {
  test('happy path: opaque code → exchange (once) → role-home, no re-login (guards GAP-1138 + GAP-1305)', async ({
    page,
  }) => {
    let exchangeCalls = 0;
    let exchangeBody: { code?: string } = {};
    let exchangeContentType: string | null = null;

    await page.route('**/api/v1/auth/sso/exchange', (route) => {
      exchangeCalls += 1;
      exchangeBody = route.request().postDataJSON() ?? {};
      exchangeContentType = route.request().headers()['content-type'] ?? null;
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(buildExchangeResponse('OWNER')),
      });
    });

    await page.goto(`/sso/callback?code=${encodeURIComponent(OPAQUE_CODE)}`);

    // Spinner copy proves the in-flight (non-error) branch rendered.
    await expect(
      page.getByText(/đang đăng nhập vào trang quản lý trường/i),
    ).toBeVisible({ timeout: 10000 });

    // No re-login: success path redirects to OWNER role-home (/dashboard), NOT
    // back to /login. Leaving /sso/callback at all proves the exchange resolved.
    await page.waitForURL('**/dashboard**', { timeout: 15000 });
    expect(page.url()).not.toContain('/sso/callback');
    expect(page.url()).not.toContain('/login');

    // Assertion (StrictMode guard, page.tsx:47 ranRef): exchange called EXACTLY
    // once even though React StrictMode double-invokes the effect in dev. A 2nd
    // call would replay the single-use code → 401 (GAP-1138 AC#5).
    expect(exchangeCalls).toBe(1);

    // Assertion (ADR-040 §35 + GAP-1305): the URL carries the OPAQUE one-time
    // code, forwarded verbatim to exchange — NOT a raw JWT.
    expect(exchangeBody.code).toBe(OPAQUE_CODE);
    expect(exchangeBody.code).not.toMatch(JWT_SHAPE);

    // Assertion (CSRF guard, SsoController.java:129 consumes=application/json):
    // the KC ssoClient always sends JSON so the BE's `consumes` guard accepts it.
    // A cross-site form auto-submit cannot set this header → BE returns 415. The
    // FE-side guarantee asserted here is the half that prevents legit 415s.
    expect(exchangeContentType).toContain('application/json');
  });

  test('sad path — replayed/consumed code → 401 → error UI (single-use, GAP-1138 AC#5)', async ({
    page,
  }) => {
    // A code already consumed by a prior exchange (single-use) → BE returns 401
    // on the replay. The callback must surface the distinct error UI, NOT crash
    // or silently re-login.
    await page.route('**/api/v1/auth/sso/exchange', (route) => {
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'SSO_UNAUTHORIZED',
          message: 'Mã SSO không hợp lệ hoặc đã hết hạn',
        }),
      });
    });

    await page.goto(`/sso/callback?code=${encodeURIComponent(OPAQUE_CODE)}-replayed`);

    await expect(
      page.getByRole('heading', { name: /đăng nhập sso thất bại/i }),
    ).toBeVisible({ timeout: 10000 });
    await expect(
      page.getByText(/mã đăng nhập không hợp lệ hoặc đã hết hạn/i),
    ).toBeVisible();
    // Recovery affordance present (not a dead end).
    await expect(page.getByRole('button', { name: /đến trang đăng nhập/i })).toBeVisible();
    // Did NOT redirect to role-home — error path stays on the callback route.
    expect(page.url()).toContain('/sso/callback');
  });

  test('sad path — expired/invalid code → 401 → clear message (GAP-1138)', async ({ page }) => {
    // Distinct framing from replay: a code that never validated (expired past
    // its ≤60s TTL or tampered). Same FE branch (catch → error UI), asserted
    // to render a clear actionable message rather than a generic crash.
    await page.route('**/api/v1/auth/sso/exchange', (route) => {
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'SSO_UNAUTHORIZED', message: 'expired' }),
      });
    });

    await page.goto('/sso/callback?code=expired-or-tampered-code');

    await expect(
      page.getByText(/mã đăng nhập không hợp lệ hoặc đã hết hạn/i),
    ).toBeVisible({ timeout: 10000 });
    expect(page.url()).toContain('/sso/callback');
  });

  test('sad path — missing code → error UI, exchange NOT called (no crash)', async ({ page }) => {
    let exchangeCalled = false;
    await page.route('**/api/v1/auth/sso/exchange', (route) => {
      exchangeCalled = true;
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });

    await page.goto('/sso/callback');

    await expect(
      page.getByText(/thiếu mã đăng nhập sso/i),
    ).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: /đến trang đăng nhập/i })).toBeVisible();
    // The guard (page.tsx:53-57) short-circuits before any exchange request.
    expect(exchangeCalled).toBe(false);
  });

  test('sad path — empty code (?code=) → same missing-code guard, exchange NOT called', async ({
    page,
  }) => {
    let exchangeCalled = false;
    await page.route('**/api/v1/auth/sso/exchange', (route) => {
      exchangeCalled = true;
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });

    // `params.get('code')` returns '' (falsy) → same missing-code branch as no param.
    await page.goto('/sso/callback?code=');

    await expect(
      page.getByText(/thiếu mã đăng nhập sso/i),
    ).toBeVisible({ timeout: 10000 });
    expect(exchangeCalled).toBe(false);
  });

  test('StrictMode double-exchange guard — code never replayed within one page load (GAP-1138 AC#5)', async ({
    page,
  }) => {
    // Explicit guard test: even though React StrictMode double-invokes the
    // useEffect in dev, the `ranRef` (page.tsx:47-51) ensures the single-use
    // code is exchanged at most once. We assert exactly one request reaches the
    // exchange endpoint regardless of double-mount.
    const exchangeRequests: Request[] = [];
    await page.route('**/api/v1/auth/sso/exchange', (route) => {
      exchangeRequests.push(route.request());
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(buildExchangeResponse('OWNER')),
      });
    });

    await page.goto(`/sso/callback?code=${encodeURIComponent(OPAQUE_CODE)}-strictmode`);
    await page.waitForURL('**/dashboard**', { timeout: 15000 });

    // Settle: give any (guarded) second effect invocation a window to (not) fire.
    await page.waitForTimeout(500);
    expect(exchangeRequests.length).toBe(1);
  });
});
