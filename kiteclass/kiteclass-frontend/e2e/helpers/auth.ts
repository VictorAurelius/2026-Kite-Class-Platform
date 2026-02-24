/**
 * E2E Authentication Helper
 *
 * Provides utilities for logging in and managing authentication
 * in Playwright E2E tests.
 *
 * @since 2026-02-24
 */

import { Page, expect } from '@playwright/test';

/**
 * Test user credentials (must match MSW handlers)
 */
export const TEST_USER = {
  email: 'test@example.com',
  password: 'password123',
  name: 'Test User',
  role: 'OWNER',
};

/**
 * Login to the application using the test user credentials.
 *
 * This function:
 * 1. Navigates to the login page
 * 2. Fills in email and password
 * 3. Submits the form
 * 4. Waits for redirect to dashboard
 *
 * @param page - Playwright page object
 * @param credentials - Optional custom credentials (defaults to TEST_USER)
 */
export async function login(
  page: Page,
  credentials: { email: string; password: string } = TEST_USER
) {
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
  await page.waitForURL(/\/(dashboard)?$/, { timeout: 10000 });

  // Verify we're authenticated by checking for navbar or dashboard content
  // This ensures the auth state is properly set
  await expect(
    page.getByText(/học viên|giảng viên|khóa học/i)
  ).toBeVisible({ timeout: 5000 });
}

/**
 * Logout from the application.
 *
 * @param page - Playwright page object
 */
export async function logout(page: Page) {
  // Click on user menu (usually in top-right corner)
  const userMenu = page.getByRole('button', { name: /user menu|account/i });
  await userMenu.click();

  // Click logout button
  const logoutButton = page.getByRole('menuitem', { name: /log out|đăng xuất/i });
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
