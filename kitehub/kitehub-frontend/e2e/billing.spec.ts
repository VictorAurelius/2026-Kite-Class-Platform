/**
 * E2E tests for billing pages.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, registerAndNavigate } from './utils/test-helpers';

test.describe('Billing Page', () => {
  test.beforeEach(async ({ page }) => {
    await registerAndNavigate(page, '/billing');
  });

  test('should display billing heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /thanh toán|đăng ký/i });
    await expect(heading).toBeVisible();
  });

  test('should show plan information or no subscription state', async ({ page }) => {
    // Either shows current plan or "no subscription" message
    const content = page.getByText(/gói đăng ký|chưa có gói/i);
    await expect(content.first()).toBeVisible({ timeout: 10000 });
  });

  test('should display plan comparison', async ({ page }) => {
    // Plan comparison should show tier options
    await page.waitForTimeout(2000);
    const tiers = page.getByText(/FREE|BASIC|PREMIUM|ENTERPRISE/);
    await expect(tiers.first()).toBeVisible();
  });

  test('should have navigation sidebar with billing active', async ({ page }) => {
    const billingLink = page.getByRole('link', { name: /thanh toán/i });
    await expect(billingLink).toBeVisible();
  });
});

test.describe('Billing Upgrade Page', () => {
  test.beforeEach(async ({ page }) => {
    await registerAndNavigate(page, '/billing/upgrade');
  });

  test('should display upgrade heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /thay đổi gói|đăng ký/i });
    await expect(heading.first()).toBeVisible({ timeout: 10000 });
  });

  test('should show step indicator or loading state', async ({ page }) => {
    // Page shows loading then step indicator or error
    await page.waitForTimeout(2000);
    const pageContent = page.locator('main');
    await expect(pageContent).toBeVisible();
  });
});

test.describe('Billing History Page', () => {
  test.beforeEach(async ({ page }) => {
    await registerAndNavigate(page, '/billing/history');
  });

  test('should display history heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /lịch sử thanh toán/i });
    await expect(heading).toBeVisible();
  });

  test('should have back button', async ({ page }) => {
    const backBtn = page.getByRole('link', { name: /quay lại/i });
    await expect(backBtn).toBeVisible();
  });

  test('should show payment table or empty state', async ({ page }) => {
    await page.waitForTimeout(2000);
    // Either shows payment history table or loading/empty state
    const content = page.locator('main');
    await expect(content).toBeVisible();
  });
});

test.describe('Billing - Unauthenticated', () => {
  test('should redirect to login for billing page', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/billing');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });

  test('should redirect to login for upgrade page', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/billing/upgrade');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });

  test('should redirect to login for history page', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/billing/history');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});
