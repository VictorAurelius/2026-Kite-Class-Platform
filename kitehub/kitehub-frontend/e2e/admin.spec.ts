/**
 * E2E tests for admin pages.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth } from './utils/test-helpers';

test.describe('Admin Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'ADMIN');
    await page.goto('/admin');
  });

  test('should display admin dashboard heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /dashboard/i });
    await expect(heading).toBeVisible();
  });

  test('should display stats cards', async ({ page }) => {
    await page.waitForTimeout(2000);
    // Stats cards should be visible (even if loading)
    const totalInstances = page.getByText(/tổng instance/i);
    await expect(totalInstances).toBeVisible();
  });

  test('should display active instances stat', async ({ page }) => {
    await page.waitForTimeout(2000);
    const activeStat = page.getByText(/đang hoạt động/i);
    await expect(activeStat).toBeVisible();
  });

  test('should display trial instances stat', async ({ page }) => {
    await page.waitForTimeout(2000);
    const trialStat = page.getByText(/đang dùng thử/i);
    await expect(trialStat).toBeVisible();
  });

  test('should display suspended stat', async ({ page }) => {
    await page.waitForTimeout(2000);
    const suspendedStat = page.getByText(/tạm ngưng/i);
    await expect(suspendedStat).toBeVisible();
  });

  test('should display revenue section', async ({ page }) => {
    await page.waitForTimeout(2000);
    const revenueStat = page.getByText(/doanh thu/i);
    await expect(revenueStat.first()).toBeVisible();
  });

  test('should have quick action links', async ({ page }) => {
    await page.waitForTimeout(2000);
    const manageLink = page.getByRole('link', { name: /quản lý instance/i });
    await expect(manageLink).toBeVisible();
  });

  test('should have admin sidebar navigation', async ({ page }) => {
    const dashboardLink = page.getByRole('link', { name: /dashboard/i });
    await expect(dashboardLink).toBeVisible();

    const instancesLink = page.getByRole('link', { name: /instances/i });
    await expect(instancesLink).toBeVisible();
  });

  test('should navigate to instances from quick action link', async ({ page }) => {
    await page.waitForTimeout(2000);
    const manageLink = page.getByRole('link', { name: /quản lý instance/i });
    await manageLink.click();
    await expect(page).toHaveURL('/admin/instances');
  });

  test('should navigate to payments from sidebar', async ({ page }) => {
    const paymentsLink = page.getByRole('link', { name: /thanh toán/i });
    await paymentsLink.click();
    await expect(page).toHaveURL('/admin/payments');
  });
});

test.describe('Admin Instances Page', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'ADMIN');
    await page.goto('/admin/instances');
  });

  test('should display instances management heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /quản lý instances/i });
    await expect(heading).toBeVisible();
  });

  test('should have refresh button', async ({ page }) => {
    const refreshBtn = page.getByRole('button', { name: /làm mới/i });
    await expect(refreshBtn).toBeVisible();
  });

  test('should show instances table or loading state', async ({ page }) => {
    // Page should display content area with heading already verified
    const subheading = page.getByText(/xem và quản lý/i);
    await expect(subheading).toBeVisible();
  });
});

test.describe('Admin Payments Page', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'ADMIN');
    await page.goto('/admin/payments');
  });

  test('should display payments heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /xác nhận thanh toán/i });
    await expect(heading).toBeVisible();
  });

  test('should have refresh button', async ({ page }) => {
    const refreshBtn = page.getByRole('button', { name: /làm mới/i });
    await expect(refreshBtn).toBeVisible();
  });

  test('should show auto-refresh note', async ({ page }) => {
    const note = page.getByText(/tự động làm mới/i);
    await expect(note).toBeVisible();
  });
});

test.describe('Admin - Unauthenticated', () => {
  test('should redirect to login for admin dashboard', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/admin');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });

  test('should redirect to login for admin instances', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/admin/instances');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });

  test('should redirect to login for admin payments', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/admin/payments');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});

test.describe('Admin - Non-Admin User', () => {
  test('should redirect non-admin user from admin pages', async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'OWNER');
    await page.goto('/admin');
    // Should redirect away from admin (either to login or dashboard)
    await page.waitForTimeout(3000);
    const url = page.url();
    expect(url).not.toContain('/admin');
  });
});
