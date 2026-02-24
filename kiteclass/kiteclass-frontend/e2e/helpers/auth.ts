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
  // Mock login endpoint - use glob pattern for matching
  await page.route('**/api/v1/auth/login', async (route) => {
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
          success: true,
          data: {
            accessToken: 'mock-access-token',
            refreshToken: 'mock-refresh-token',
            user: {
              id: 1,
              email: TEST_USER.email,
              name: TEST_USER.name,
              roles: [TEST_USER.role],
              profile: { id: 1 },
            },
          },
        }),
      });
    } else {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          message: 'Invalid credentials',
        }),
      });
    }
  });

  // Mock logout endpoint
  await page.route('**/api/v1/auth/logout', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true }),
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
  // No need for mocks - using real backend
  // Navigate to login page
  await page.goto('/login');

  // Wait for login form to be visible
  await expect(page.getByText('Welcome back')).toBeVisible();

  // Fill in credentials
  await page.fill('input[name="email"]', credentials.email);
  await page.fill('input[name="password"]', credentials.password);

  // Submit form
  await page.click('button[type="submit"]');

  // Wait for successful login - should redirect to dashboard
  // Use URL pattern instead of specific path since it might redirect to /dashboard or /
  // Increased timeout for slower systems or network
  await page.waitForURL(/\/(dashboard)?$/, { timeout: 15000 });

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
  // Click on user avatar button (shows "KC")
  const userMenu = page.getByRole('button', { name: 'KC' });
  await userMenu.click();

  // Click logout menu item
  const logoutButton = page.getByRole('menuitem', { name: /logout/i });
  await logoutButton.click();

  // Wait for redirect to login page
  await page.waitForURL('/login', { timeout: 5000 });
  await expect(page.getByText('Welcome back')).toBeVisible();
}

/**
 * Check if user is currently authenticated.
 *
 * @param page - Playwright page object
 * @returns true if authenticated, false otherwise
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  // Check if accessToken exists in localStorage
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
  return accessToken !== null;
}

/**
 * Inject authentication tokens directly into localStorage.
 *
 * Useful for bypassing login UI when you just need authenticated state.
 * Note: This requires MSW to be running to provide valid tokens.
 *
 * @param page - Playwright page object
 */
export async function injectAuthTokens(page: Page) {
  await page.goto('/');

  await page.evaluate(() => {
    localStorage.setItem('accessToken', 'mock-access-token');
    localStorage.setItem('refreshToken', 'mock-refresh-token');
    localStorage.setItem('tenantId', '11111111-1111-1111-1111-111111111111');
  });

  // Reload to apply auth state
  await page.reload();
}
