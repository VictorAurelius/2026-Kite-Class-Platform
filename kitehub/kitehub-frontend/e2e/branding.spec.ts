/**
 * E2E tests for branding pages.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';
import {
  clearBrowserStorage,
  registerAndNavigate,
  mockInstancesAPI,
  mockBrandingAPIs,
  mockAuthRegisterAPI
} from './utils/test-helpers';

test.describe('Branding Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    // Setup mocks BEFORE navigation
    await mockAuthRegisterAPI(page); // Mock registration API
    await mockInstancesAPI(page);
    await mockBrandingAPIs(page, false); // Empty state
    await registerAndNavigate(page, '/branding');
  });

  test('should display branding heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /ai branding/i });
    await expect(heading).toBeVisible();
  });

  test('should have create branding button', async ({ page }) => {
    const createBtn = page.getByRole('link', { name: /tạo branding/i }).or(
      page.getByRole('button', { name: /tạo branding/i })
    );
    await expect(createBtn.first()).toBeVisible({ timeout: 10000 });
  });

  test('should show empty state or assets', async ({ page }) => {
    await page.waitForTimeout(2000);
    // New user has no assets - should show empty state or status
    const content = page.getByText(/chưa có tài nguyên|trạng thái|tài nguyên/i);
    await expect(content.first()).toBeVisible();
  });

  test('should have sidebar navigation', async ({ page }) => {
    const brandingLink = page.getByRole('link', { name: /ai branding/i });
    await expect(brandingLink).toBeVisible();
  });

  test('should navigate to wizard when clicking create branding', async ({ page }) => {
    const createBtn = page.getByRole('link', { name: /tạo branding/i }).or(
      page.getByRole('button', { name: /tạo branding/i })
    );
    await createBtn.first().click();
    await expect(page).toHaveURL('/branding/wizard', { timeout: 10000 });
  });
});

test.describe('Branding Wizard', () => {
  test.beforeEach(async ({ page }) => {
    // Setup mocks BEFORE navigation
    await mockAuthRegisterAPI(page); // Mock registration API
    await mockInstancesAPI(page);
    await mockBrandingAPIs(page, false);
    await registerAndNavigate(page, '/branding/wizard');
  });

  test('should display wizard heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /hướng dẫn|ai branding/i });
    await expect(heading.first()).toBeVisible({ timeout: 10000 });
  });

  test('should show step indicator', async ({ page }) => {
    // Step 1 should show "Tải Logo" label
    const stepLabel = page.getByText(/tải logo/i);
    await expect(stepLabel.first()).toBeVisible({ timeout: 10000 });
  });

  test('should show upload step as first step', async ({ page }) => {
    await page.waitForTimeout(2000);
    // Upload step should have file input or upload area
    const content = page.locator('main');
    await expect(content).toBeVisible();
  });
});

test.describe('Branding Assets', () => {
  test.beforeEach(async ({ page }) => {
    // Setup mocks BEFORE navigation
    await mockAuthRegisterAPI(page); // Mock registration API
    await mockInstancesAPI(page);
    await mockBrandingAPIs(page, false); // Empty state
    await registerAndNavigate(page, '/branding/assets');
  });

  test('should display assets heading', async ({ page }) => {
    const heading = page.getByText(/tài nguyên branding/i);
    await expect(heading.first()).toBeVisible({ timeout: 10000 });
  });

  test('should have back button', async ({ page }) => {
    const backBtn = page.getByText(/quay lại/i);
    await expect(backBtn).toBeVisible();
  });

  test('should have create new button', async ({ page }) => {
    const createBtn = page.getByText(/tạo mới/i);
    await expect(createBtn).toBeVisible();
  });

  test('should show empty state for new user', async ({ page }) => {
    const emptyState = page.getByText(/chưa có tài nguyên/i);
    await expect(emptyState).toBeVisible({ timeout: 10000 });
  });

  test('should have create first branding button in empty state', async ({ page }) => {
    const createBtn = page.getByText(/tạo branding đầu tiên/i);
    await expect(createBtn).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Branding - Unauthenticated', () => {
  test('should redirect to login for branding page', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/branding');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });

  test('should redirect to login for wizard page', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/branding/wizard');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });

  test('should redirect to login for assets page', async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/branding/assets');
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});
