/**
 * E2E test helper utilities.
 *
 * @since PR 5.10
 */

import { Page } from '@playwright/test';

/**
 * Generate a unique subdomain for testing.
 */
export function generateTestSubdomain(): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 6);
  return `test-${timestamp}-${random}`;
}

/**
 * Generate a unique email for testing.
 */
export function generateTestEmail(): string {
  const timestamp = Date.now();
  return `test-${timestamp}@example.com`;
}

/**
 * Wait for navigation to complete.
 */
export async function waitForNavigation(page: Page, url: string): Promise<void> {
  await page.waitForURL(url, { timeout: 10000 });
}

/**
 * Clear localStorage and sessionStorage.
 * Must navigate to a page first to access storage.
 */
export async function clearBrowserStorage(page: Page): Promise<void> {
  // Navigate to base URL first if not already on a page
  const currentUrl = page.url();
  if (currentUrl === 'about:blank' || !currentUrl.startsWith('http')) {
    await page.goto('/');
  }

  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
}

/**
 * Get auth token from localStorage.
 */
export async function getAuthToken(page: Page): Promise<string | null> {
  return page.evaluate(() => localStorage.getItem('accessToken'));
}

/**
 * Check if user is authenticated.
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  const token = await getAuthToken(page);
  return token !== null && token.length > 0;
}

/**
 * Set up mock auth state in localStorage (skips registration flow).
 * Useful for testing pages that need auth but don't need real API data.
 */
export async function setupMockAuth(page: Page, role: 'OWNER' | 'ADMIN' = 'OWNER'): Promise<void> {
  const currentUrl = page.url();
  if (currentUrl === 'about:blank' || !currentUrl.startsWith('http')) {
    await page.goto('/');
  }

  const authState = {
    state: {
      user: {
        id: '00000000-0000-0000-0000-000000000099',
        email: role === 'ADMIN' ? 'admin@kitehub.com' : 'mock@kitehub.com',
        name: role === 'ADMIN' ? 'Admin User' : 'Mock User',
        role,
      },
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      isAuthenticated: true,
    },
    version: 0,
  };

  await page.evaluate((state) => {
    localStorage.setItem('kitehub-auth', JSON.stringify(state));
  }, authState);
}

/**
 * Register a new user and return to specified page.
 * Returns the instance ID from the redirect URL.
 */
export async function registerAndNavigate(page: Page, targetUrl: string): Promise<void> {
  await clearBrowserStorage(page);
  await page.goto('/register');

  const data = {
    organizationName: 'Test Organization',
    subdomain: generateTestSubdomain(),
    email: generateTestEmail(),
    password: 'TestPassword123!',
  };

  await page.getByPlaceholder('Trung tâm Anh ngữ ABC').fill(data.organizationName);
  await page.getByPlaceholder('abc-center').fill(data.subdomain);
  await page.getByPlaceholder('email@example.com').fill(data.email);

  const passwordFields = page.locator('input[type="password"]');
  await passwordFields.first().fill(data.password);
  await passwordFields.nth(1).fill(data.password);

  await page.getByRole('button', { name: /tạo tài khoản/i }).click();
  await expect(page).toHaveURL('/dashboard', { timeout: 10000 });

  if (targetUrl !== '/dashboard') {
    await page.goto(targetUrl);
  }
}

/**
 * Mock admin dashboard API with sample data.
 */
export async function mockAdminDashboardAPI(page: Page): Promise<void> {
  await page.route('**/api/platform/admin/dashboard', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        totalInstances: 25,
        activeInstances: 18,
        trialInstances: 5,
        suspendedInstances: 2,
        totalRevenue: 25000000,
        monthlyRevenue: 5000000,
        pendingPayments: 3,
        newInstancesThisMonth: 4,
      }),
    });
  });
}

/**
 * Mock admin instances list API.
 */
export async function mockAdminInstancesAPI(page: Page): Promise<void> {
  await page.route('**/api/platform/admin/instances', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: '1',
          organizationName: 'Test Org 1',
          subdomain: 'test1',
          status: 'ACTIVE',
          tier: 'BASIC',
          createdAt: new Date().toISOString(),
        },
        {
          id: '2',
          organizationName: 'Test Org 2',
          subdomain: 'test2',
          status: 'TRIAL',
          tier: 'BASIC',
          createdAt: new Date().toISOString(),
        },
      ]),
    });
  });
}

/**
 * Mock admin payments API.
 */
export async function mockAdminPaymentsAPI(page: Page): Promise<void> {
  await page.route('**/api/platform/admin/payments/pending', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([]),
    });
  });
}

/**
 * Mock all admin APIs for testing.
 */
export async function mockAllAdminAPIs(page: Page): Promise<void> {
  await mockAdminDashboardAPI(page);
  await mockAdminInstancesAPI(page);
  await mockAdminPaymentsAPI(page);
}

// Re-export expect for use in helpers
import { expect } from '@playwright/test';
