/**
 * E2E tests for instance detail page.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, registerAndNavigate } from './utils/test-helpers';

test.describe('Instance Detail Page', () => {
  test.beforeEach(async ({ page }) => {
    // Register and navigate to dashboard, then click into instance detail
    await registerAndNavigate(page, '/dashboard');

    // Click on instance card to navigate to detail
    const instanceLink = page.locator('a[href*="/instances/"]').first();
    await instanceLink.click();
    await expect(page).toHaveURL(/\/instances\/[\w-]+/, { timeout: 10000 });
  });

  test('should display organization name', async ({ page }) => {
    const orgName = page.getByRole('heading', { name: /test organization/i });
    await expect(orgName).toBeVisible();
  });

  test('should display subdomain info', async ({ page }) => {
    const subdomain = page.getByText(/\.kiteclass\.com/i);
    await expect(subdomain).toBeVisible();
  });

  test('should display status badge', async ({ page }) => {
    const statusBadge = page.getByText(/trial|active|dùng thử/i);
    await expect(statusBadge.first()).toBeVisible();
  });

  test('should display instance info card', async ({ page }) => {
    const infoHeading = page.getByText(/thông tin/i);
    await expect(infoHeading.first()).toBeVisible();
  });

  test('should display tier information', async ({ page }) => {
    const tierInfo = page.getByText(/free|basic|premium|enterprise/i);
    await expect(tierInfo.first()).toBeVisible();
  });

  test('should display trial status for new instance', async ({ page }) => {
    const trialInfo = page.getByText(/trial|dùng thử|ngày/i);
    await expect(trialInfo.first()).toBeVisible();
  });

  test('should have action buttons', async ({ page }) => {
    const accessBtn = page.getByRole('link', { name: /truy cập kiteclass/i });
    await expect(accessBtn).toBeVisible();
  });

  test('should have upgrade button', async ({ page }) => {
    const upgradeBtn = page.getByRole('link', { name: /nâng cấp/i });
    await expect(upgradeBtn).toBeVisible();
  });

  test('should have AI branding button', async ({ page }) => {
    const brandingBtn = page.getByRole('link', { name: /ai branding/i });
    await expect(brandingBtn).toBeVisible();
  });
});

test.describe('Instance Detail - Unauthenticated', () => {
  test('should redirect to login for instance detail', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/instances/00000000-0000-0000-0000-000000000001');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});
