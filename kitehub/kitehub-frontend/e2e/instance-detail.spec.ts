/**
 * E2E tests for instance detail page.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth, mockInstanceDetailAPI, mockInstancesAPI } from './utils/test-helpers';

test.describe('Instance Detail Page', () => {
  const instanceId = '00000000-0000-0000-0000-000000000001';

  test.beforeEach(async ({ page }) => {
    // Setup mock auth and APIs
    await setupMockAuth(page, 'OWNER');

    // Mock instance owner list API (for dashboard) - use same instance ID
    await mockInstancesAPI(page, instanceId);

    // Mock instance detail API
    await mockInstanceDetailAPI(page, instanceId, 'TRIAL');

    // Navigate directly to instance detail page
    await page.goto(`/instances/${instanceId}`);
    await expect(page).toHaveURL(`/instances/${instanceId}`, { timeout: 10000 });
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
    // Scope to main content — sidebar also has an "AI Branding" link (same href)
    const brandingBtn = page.locator('main').getByRole('link', { name: 'AI Branding', exact: true });
    await expect(brandingBtn).toBeVisible();
  });

  test('should navigate to upgrade when clicking upgrade button', async ({ page }) => {
    const upgradeBtn = page.getByRole('link', { name: /nâng cấp/i });
    await upgradeBtn.click();
    await expect(page).toHaveURL(/\/billing\/upgrade|\/billing/);
  });

  test('should navigate to branding when clicking AI Branding button', async ({ page }) => {
    // Scope to main content — sidebar also has an "AI Branding" link (same href)
    const brandingBtn = page.locator('main').getByRole('link', { name: 'AI Branding', exact: true });
    await brandingBtn.click();
    await expect(page).toHaveURL('/branding', { timeout: 15000 });
  });
});

test.describe('Instance Detail - Unauthenticated', () => {
  test('should redirect to login for instance detail', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/instances/00000000-0000-0000-0000-000000000001');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});
