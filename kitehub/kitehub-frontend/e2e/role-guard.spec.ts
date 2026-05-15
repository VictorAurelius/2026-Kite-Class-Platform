/**
 * E2E tests for RoleGuard / Sidebar role-based visibility.
 *
 * GAP-562b (Wave 80 Bucket C): STAFF role hitting Owner-only routes
 * (/billing, /branding, /settings) must be redirected to /dashboard.
 * Sidebar customerNav must hide Owner-only entries when STAFF logged in.
 *
 * @since Wave 80 Bucket C
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth } from './utils/test-helpers';

const OWNER_ONLY_PATHS = ['/billing', '/branding', '/settings'] as const;

test.describe('RoleGuard — STAFF hitting Owner-only routes redirects to /dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'STAFF');
  });

  for (const path of OWNER_ONLY_PATHS) {
    test(`STAFF hitting ${path} → redirects to /dashboard`, async ({ page }) => {
      await page.goto(path);
      // RoleGuard schedules router.replace('/dashboard'); allow up to 2s for redirect.
      await expect(page).toHaveURL(/\/dashboard$/, { timeout: 2000 });
    });
  }
});

test.describe('RoleGuard — OWNER retains full access', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'OWNER');
  });

  for (const path of OWNER_ONLY_PATHS) {
    test(`OWNER hitting ${path} → page loads without redirect`, async ({ page }) => {
      await page.goto(path);
      // OWNER should remain on the requested path (or at least not bounce to /dashboard).
      await page.waitForLoadState('networkidle', { timeout: 5000 }).catch(() => undefined);
      const url = new URL(page.url());
      expect(url.pathname).not.toBe('/dashboard');
      expect(url.pathname.startsWith(path)).toBeTruthy();
    });
  }
});

test.describe('Sidebar — Owner-only nav entries hidden for STAFF', () => {
  test('STAFF Sidebar shows Tổng quan + Bắt đầu only', async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'STAFF');
    await page.goto('/dashboard');

    // Wait for sidebar hydration
    await expect(page.getByText('Tổng quan')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('Bắt đầu')).toBeVisible();

    // Owner-only entries MUST NOT be visible.
    await expect(page.getByTestId('customer-nav-billing')).not.toBeVisible();
    await expect(page.getByTestId('customer-nav-branding')).not.toBeVisible();
    await expect(page.getByTestId('customer-nav-settings')).not.toBeVisible();
  });

  test('OWNER Sidebar shows all 5 customer nav entries', async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'OWNER');
    await page.goto('/dashboard');

    await expect(page.getByText('Tổng quan')).toBeVisible({ timeout: 5000 });
    await expect(page.getByText('Bắt đầu')).toBeVisible();
    await expect(page.getByTestId('customer-nav-billing')).toBeVisible();
    await expect(page.getByTestId('customer-nav-branding')).toBeVisible();
    await expect(page.getByTestId('customer-nav-settings')).toBeVisible();
  });
});
