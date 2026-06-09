/**
 * E2E Authentication Helper
 *
 * Provides utilities for logging in and managing authentication
 * in Playwright E2E tests.
 *
 * @since 2026-02-24
 */

import { Page, expect } from '@playwright/test';
import { setupApiMocks } from './api-mocks';

/**
 * Test user credentials (real database user from V8 migration)
 */
export const TEST_USER = {
  email: 'owner@kiteclass.local',
  password: 'Admin@123',
  name: 'System Owner',
  role: 'OWNER',
};

/**
 * Setup API mocks for authentication endpoints.
 *
 * Since MSW doesn't run in Playwright browser context,
 * we use Playwright's route mocking instead.
 *
 * @param page - Playwright page object
 */
export async function setupAuthMocks(page: Page) {
  // Mock login endpoint — Wave 105 commit 8cc5bff4 (PR #1737) changed KH backend from
  // /api/v1/auth/login (wrapped {success, data: {...}}) to /api/auth/login (flat shape).
  // GAP-759 sync: mock URL + response shape must match real consumer (useAuth.ts line 28
  // expects flat shape with role singular, not roles[]).
  await page.route('**/api/auth/login', async (route) => {
    const request = route.request();
    const postData = request.postDataJSON();

    if (
      postData.email === TEST_USER.email &&
      postData.password === TEST_USER.password
    ) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          // Flat shape per Wave 105 contract (no {success, data} wrapper).
          // JWT with payload {"tenantId":"11111111-1111-1111-1111-111111111111"} — required for atob() in useAuth.ts
          accessToken: 'eyJhbGciOiJIUzI1NiJ9.eyJ0ZW5hbnRJZCI6IjExMTExMTExLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMSJ9.mock',
          refreshToken: 'mock-refresh-token',
          user: {
            id: 1,
            email: TEST_USER.email,
            name: TEST_USER.name,
            // Singular `role` per real KH contract (useAuth.ts line 28-29).
            role: TEST_USER.role,
          },
        }),
      });
    } else {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        // Flat error shape (no {success: false} wrapper).
        body: JSON.stringify({
          message: 'Invalid credentials',
        }),
      });
    }
  });

  // Mock logout endpoint — same path update Wave 105.
  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({}),
    });
  });
}

/**
 * Login to the application using the test user credentials.
 *
 * This function:
 * 1. Sets up API mocks
 * 2. Navigates to the login page
 * 3. Fills in email and password
 * 4. Submits the form
 * 5. Waits for redirect to dashboard
 *
 * @param page - Playwright page object
 * @param credentials - Optional custom credentials (defaults to TEST_USER)
 */
export async function login(
  page: Page,
  credentials: { email: string; password: string } = TEST_USER
) {
  // Setup API mocks before navigation
  await setupAuthMocks(page);
  await setupApiMocks(page);

  // Navigate to login page
  await page.goto('/login');

  // Wait for login form to be visible.
  // Heading is Vietnamese per CLAUDE.md communication language rule;
  // accept either VN or EN copy in case of A/B variants.
  await expect(
    page.getByRole('heading', { name: /(Chào mừng trở lại|Welcome back)/i }),
  ).toBeVisible({ timeout: 10000 });

  // Fill in credentials
  await page.fill('input[name="email"]', credentials.email);
  await page.fill('input[name="password"]', credentials.password);

  // Submit form
  await page.click('button[type="submit"]');

  // Wait for successful login - redirect can be to /, /dashboard, or any protected route
  // Just check that we're no longer on /login page
  await page.waitForURL((url) => !url.pathname.includes('/login'), {
    timeout: 15000,
  });

  // Wait for the auth store to be persisted (GAP-1074: tenant-scoped localStorage
  // `kc:<tenantId>:auth-store`, resolved via the `kc:activeTenant` pointer set by
  // setTokens()/bindTenant() — supersedes the GAP-830 `sessionStorage['auth-storage']` blob).
  await page.waitForFunction(
    () => {
      const tenantId = localStorage.getItem('kc:activeTenant');
      if (!tenantId) return false;
      const blob = localStorage.getItem(`kc:${tenantId}:auth-store`);
      if (!blob) return false;
      try {
        const parsed = JSON.parse(blob);
        return parsed.state?.isAuthenticated === true;
      } catch {
        return false;
      }
    },
    { timeout: 10000 }
  );

  // Verify we're authenticated by checking for navigation links
  // Use .first() to avoid strict mode violation (multiple matches)
  await expect(
    page.getByRole('link', { name: /students|học viên/i }).first()
  ).toBeVisible({ timeout: 5000 });
}

/**
 * Logout from the application.
 *
 * @param page - Playwright page object
 */
export async function logout(page: Page) {
  // Click on user avatar button — Avatar with AvatarFallback "KC" wrapped in a ghost button.
  // Scoped to the banner landmark (<header> element) to avoid false matches from sidebar buttons.
  const userMenu = page.getByRole('banner').getByRole('button').filter({ hasText: 'KC' });
  await userMenu.click();

  // Click logout menu item — actual VN text "Đăng xuất" rendered as DropdownMenuItem.
  // DropdownMenuItem renders as role="menuitem"; accept VN or EN copy.
  const logoutButton = page.getByRole('menuitem', { name: /đăng xuất|logout/i });
  await logoutButton.click();

  // Wait for redirect to login page
  await page.waitForURL('/login', { timeout: 5000 });
  // Login heading is VN-first: "Chào mừng trở lại"; accept EN "Welcome back" for A/B resilience.
  await expect(
    page.getByRole('heading', { name: /(Chào mừng trở lại|Welcome back)/i })
  ).toBeVisible();
}

/**
 * Check if user is currently authenticated.
 *
 * @param page - Playwright page object
 * @returns true if authenticated, false otherwise
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  // GAP-1074: token lives at `localStorage['kc:<tenantId>:accessToken']` (tenant-scoped),
  // resolved via the `kc:activeTenant` pointer. Supersedes `sessionStorage['accessToken']`.
  const hasToken = await page.evaluate(() => {
    const tenantId = localStorage.getItem('kc:activeTenant');
    if (!tenantId) return false;
    return localStorage.getItem(`kc:${tenantId}:accessToken`) !== null;
  });
  return hasToken;
}

/**
 * Inject authentication state directly into storage (bypass the login UI).
 *
 * GAP-1074 (supersedes GAP-830): tokens + the zustand persist blob are tenant-scoped
 * in localStorage (`kc:<tenantId>:accessToken` / `:refreshToken` / `:auth-store`),
 * resolved per-tab via `sessionStorage['kc:currentTenant']` + the cross-tab
 * `localStorage['kc:activeTenant']` pointer. Mirror exactly what
 * useAuth.onSuccess → setTokens()/setAuth() writes so the dashboard auth guard
 * (reads zustand `isAuthenticated`) sees an authenticated state.
 *
 * @param page - Playwright page object
 */
export async function injectAuthTokens(page: Page) {
  await page.goto('/');

  await page.evaluate(() => {
    const TENANT_ID = '11111111-1111-1111-1111-111111111111';
    const TOKEN =
      'eyJhbGciOiJIUzI1NiJ9.eyJ0ZW5hbnRJZCI6IjExMTExMTExLTExMTEtMTExMS0xMTExLTExMTExMTExMTExMSJ9.mock';

    // Bind this tab + the cross-tab last-login pointer (GAP-1074 bindTenant()).
    sessionStorage.setItem('kc:currentTenant', TENANT_ID);
    localStorage.setItem('kc:activeTenant', TENANT_ID);

    // Tenant-scoped tokens (used by the API client via jwt-storage getAccessToken()).
    localStorage.setItem(`kc:${TENANT_ID}:accessToken`, TOKEN);
    localStorage.setItem(`kc:${TENANT_ID}:refreshToken`, 'mock-refresh-token');

    // Zustand auth-store persist blob (dashboard layout auth guard reads this).
    const authStore = {
      state: {
        user: {
          id: 1,
          email: 'owner@kiteclass.local',
          name: 'System Owner',
          userType: 'OWNER',
          referenceId: '1',
        },
        accessToken: TOKEN,
        refreshToken: 'mock-refresh-token',
        tenantId: TENANT_ID,
        isAuthenticated: true,
      },
      version: 0,
    };
    localStorage.setItem(`kc:${TENANT_ID}:auth-store`, JSON.stringify(authStore));
  });

  // Reload to apply auth state
  await page.reload();
}
