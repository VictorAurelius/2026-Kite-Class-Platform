/**
 * E2E tests for settings page.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';
import {
  clearBrowserStorage,
  registerAndNavigate,
  mockSettingsAPIs,
  mockAuthRegisterAPI
} from './utils/test-helpers';

test.describe('Settings Page', () => {
  test.beforeEach(async ({ page }) => {
    // Set up mocks before navigation
    await mockAuthRegisterAPI(page); // Mock registration API
    await mockSettingsAPIs(page);     // Mock settings APIs
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

  test('should display account tab content with form fields', async ({ page }) => {
    // Account tab is default - verify form content loads
    const emailField = page.locator('input[type="email"], input[disabled]').first();
    const nameField = page.getByPlaceholder(/tên/i).or(page.locator('input[name="name"]')).first();
    // At least some form content should be visible
    await page.waitForTimeout(2000);
    const hasEmail = await emailField.isVisible().catch(() => false);
    const hasName = await nameField.isVisible().catch(() => false);
    const hasContent = page.getByText(/email|tên|thông tin/i);
    expect(hasEmail || hasName || await hasContent.first().isVisible().catch(() => false)).toBeTruthy();
  });

  test('should display instance tab content when clicked', async ({ page }) => {
    const instanceTab = page.getByRole('tab', { name: /instance/i });
    await instanceTab.click();
    await page.waitForTimeout(2000);
    // Instance tab should show subdomain or instance info
    const instanceContent = page.getByText(/subdomain|kiteclass\.com|tên miền/i);
    await expect(instanceContent.first()).toBeVisible({ timeout: 10000 });
  });

  test('should display danger zone content when clicked', async ({ page }) => {
    const dangerTab = page.getByRole('tab', { name: /nguy hiểm/i });
    await dangerTab.click();
    await page.waitForTimeout(2000);
    // Danger zone should show destructive actions
    const dangerContent = page.getByText(/xóa|hủy|ngưng|cảnh báo|instance/i);
    await expect(dangerContent.first()).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Settings - Unauthenticated', () => {
  test('should redirect to login when not authenticated', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/settings');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});
