/**
 * E2E tests for settings page.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, registerAndNavigate } from './utils/test-helpers';

test.describe('Settings Page', () => {
  test.beforeEach(async ({ page }) => {
    await registerAndNavigate(page, '/settings');
  });

  test('should display settings heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /cài đặt/i });
    await expect(heading).toBeVisible();
  });

  test('should display settings description', async ({ page }) => {
    const desc = page.getByText(/quản lý tài khoản/i);
    await expect(desc).toBeVisible();
  });

  test('should have account tab', async ({ page }) => {
    const accountTab = page.getByRole('tab', { name: /tài khoản/i });
    await expect(accountTab).toBeVisible();
  });

  test('should have instance tab', async ({ page }) => {
    const instanceTab = page.getByRole('tab', { name: /instance/i });
    await expect(instanceTab).toBeVisible();
  });

  test('should have danger zone tab', async ({ page }) => {
    const dangerTab = page.getByRole('tab', { name: /nguy hiểm/i });
    await expect(dangerTab).toBeVisible();
  });

  test('should show account tab content by default', async ({ page }) => {
    // Account tab should be active by default
    const accountTab = page.getByRole('tab', { name: /tài khoản/i });
    await expect(accountTab).toHaveAttribute('data-state', 'active');
  });

  test('should switch to instance tab', async ({ page }) => {
    const instanceTab = page.getByRole('tab', { name: /instance/i });
    await instanceTab.click();
    await expect(instanceTab).toHaveAttribute('data-state', 'active');
  });

  test('should switch to danger zone tab', async ({ page }) => {
    const dangerTab = page.getByRole('tab', { name: /nguy hiểm/i });
    await dangerTab.click();
    await expect(dangerTab).toHaveAttribute('data-state', 'active');
  });
});

test.describe('Settings - Unauthenticated', () => {
  test('should redirect to login when not authenticated', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/settings');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});
