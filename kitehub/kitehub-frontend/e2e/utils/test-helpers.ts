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
