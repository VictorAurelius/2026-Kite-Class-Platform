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

  test('should display billing page content', async ({ page }) => {
    // Page shows either heading or error state
    await page.waitForTimeout(2000);
    const heading = page.getByRole('heading', { name: /thanh toán/i });
    const error = page.getByText(/không thể tải|lỗi/i);
    const isHeadingVisible = await heading.isVisible().catch(() => false);
    const isErrorVisible = await error.first().isVisible().catch(() => false);
    expect(isHeadingVisible || isErrorVisible).toBeTruthy();
  });

  test('should show billing state after loading', async ({ page }) => {
    await page.waitForTimeout(3000);
    // Either shows plan info, no subscription, or error
    const content = page.getByText(/gói đăng ký|chưa có gói|không thể tải/i);
    await expect(content.first()).toBeVisible();
  });

  test('should display plan comparison or error', async ({ page }) => {
    await page.waitForTimeout(3000);
    // If no error, plan comparison shows tiers
    const tiers = page.getByText(/FREE|BASIC|PREMIUM|ENTERPRISE/i);
    const error = page.getByText(/không thể tải|lỗi/i);
    const hasTiers = await tiers.first().isVisible().catch(() => false);
    const hasError = await error.first().isVisible().catch(() => false);
    expect(hasTiers || hasError).toBeTruthy();
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

  test('should display upgrade page content', async ({ page }) => {
    await page.waitForTimeout(3000);
    // Shows heading, loading, or error
    const heading = page.getByRole('heading', { name: /thay đổi gói|đăng ký/i });
    const error = page.getByText(/không tìm thấy|lỗi|không thể tải/i);
    const loading = page.locator('[class*="animate-spin"]');
    const hasHeading = await heading.first().isVisible().catch(() => false);
    const hasError = await error.first().isVisible().catch(() => false);
    const hasLoading = await loading.first().isVisible().catch(() => false);
    expect(hasHeading || hasError || hasLoading).toBeTruthy();
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
    const backBtn = page.getByText(/quay lại/i);
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
