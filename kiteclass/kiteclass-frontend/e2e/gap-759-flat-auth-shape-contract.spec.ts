/**
 * GAP-759 — KC login flat-shape contract regression guard.
 *
 * Assert E2E `setupAuthMocks` helper mocks login với URL + flat response shape
 * khớp consumer expectation (`useAuth.ts` mutation onSuccess + `authApi.login`):
 *   - URL: `/api/auth/login` (KHÔNG `/api/v1/auth/login`)
 *   - Response: flat `{accessToken, refreshToken, user: {id, email, name, role}}`
 *     (KHÔNG wrapper `{success, data: {...}}`)
 *   - User shape: `role` singular (KHÔNG `roles[]` array)
 *
 * Wave 105 commit 8cc5bff4 (PR #1737) đổi KH backend contract; nếu mock auth
 * helper drift lại lần nữa khi backend shape changes → spec này fail immediately
 * trước khi affect class-lifecycle gate.
 *
 * Per `.claude/rules/e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion:
 * mỗi RST/CI flake finding phải convert thành regression-guard spec same PR fix.
 *
 * @since GAP-759 (2026-05-27)
 */
import { test, expect } from '@playwright/test';
import { setupAuthMocks, TEST_USER } from './helpers/auth';

test.describe('GAP-759 — KC login flat auth shape contract', () => {
  test('mock intercepts /api/auth/login (NOT /api/v1/...) + returns flat shape', async ({
    page,
  }) => {
    await setupAuthMocks(page);

    // Spy on the actual mocked response shape — verify mock fires + shape matches
    // what useAuth.ts expects (flat: accessToken at root, NOT data.accessToken).
    const responseCapture = page.waitForResponse(
      (resp) => resp.url().includes('/api/auth/login'),
      { timeout: 5000 },
    );

    // Trigger login via direct fetch (bypass UI to isolate mock-contract check)
    await page.goto('about:blank');
    const fetchResult = await page.evaluate(
      async ({ email, password }) => {
        const res = await fetch('http://localhost:3000/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password }),
        });
        return { status: res.status, body: await res.json() };
      },
      { email: TEST_USER.email, password: TEST_USER.password },
    );

    await responseCapture;

    // Assertion 1: mock fired with HTTP 200
    expect(fetchResult.status).toBe(200);

    // Assertion 2: response is FLAT shape (no wrapper)
    expect(fetchResult.body).toHaveProperty('accessToken');
    expect(fetchResult.body).toHaveProperty('refreshToken');
    expect(fetchResult.body).toHaveProperty('user');
    // Banned: wrapped shape from old contract
    expect(fetchResult.body).not.toHaveProperty('success');
    expect(fetchResult.body).not.toHaveProperty('data');

    // Assertion 3: user shape uses singular `role` (matches useAuth.ts line 28-29)
    expect(fetchResult.body.user).toHaveProperty('role');
    expect(fetchResult.body.user.role).toBe(TEST_USER.role);
    // Banned: legacy array form
    expect(fetchResult.body.user).not.toHaveProperty('roles');

    // Assertion 4: JWT contains tenantId claim (useAuth.ts atob() decode path)
    const jwtPayload = JSON.parse(
      Buffer.from(fetchResult.body.accessToken.split('.')[1] || '', 'base64').toString(),
    );
    expect(jwtPayload).toHaveProperty('tenantId');
  });

  test('login UI flow stores accessToken in sessionStorage after mock fires', async ({
    page,
  }) => {
    await setupAuthMocks(page);

    // Stub other dashboard APIs to prevent network noise
    await page.route('**/api/v1/**', (route) =>
      route.fulfill({ status: 200, body: JSON.stringify({ data: [] }) }),
    );

    await page.goto('/login');
    await expect(
      page.getByRole('heading', { name: /(Chào mừng trở lại|Welcome back)/i }),
    ).toBeVisible({ timeout: 10000 });

    await page.fill('input[name="email"]', TEST_USER.email);
    await page.fill('input[name="password"]', TEST_USER.password);
    await page.click('button[type="submit"]');

    // Wait for redirect away from /login (mock should fire + useAuth.ts pushes to /dashboard)
    await page.waitForURL((url) => !url.pathname.includes('/login'), {
      timeout: 15000,
    });

    // Verify sessionStorage has accessToken (set by useAuth.ts via jwt-storage facade — GAP-830)
    const accessToken = await page.evaluate(() =>
      sessionStorage.getItem('accessToken'),
    );
    expect(accessToken).toBeTruthy();
    expect(accessToken).toMatch(/^eyJ/); // JWT prefix
  });
});
