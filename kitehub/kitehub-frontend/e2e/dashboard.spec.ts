/**
 * E2E tests for authenticated dashboard flows.
 *
 * @since PR 5.10
 */

import { test, expect } from '@playwright/test';
import { createRegistrationData } from './fixtures/test-data';
import { clearBrowserStorage } from './utils/test-helpers';

test.describe('Dashboard', () => {
  // Register and login before each test
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);

    // Register new user
    await page.goto('/register');
    const data = createRegistrationData();

    await page.getByPlaceholder('Trung tâm Anh ngữ ABC').fill(data.organizationName);
    await page.getByPlaceholder('abc-center').fill(data.subdomain);
    await page.getByPlaceholder('email@example.com').fill(data.email);

    const passwordFields = page.locator('input[type="password"]');
    await passwordFields.first().fill(data.password);
    await passwordFields.nth(1).fill(data.password);

    await page.getByRole('button', { name: /tạo tài khoản/i }).click();
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });
  });

  test('should display dashboard after login', async ({ page }) => {
    // Dashboard heading
    const heading = page.getByRole('heading', { name: /dashboard/i });
    await expect(heading).toBeVisible();
  });

  test('should display welcome message with user info', async ({ page }) => {
    // Check for greeting
    const greeting = page.getByText(/xin chào/i);
    await expect(greeting).toBeVisible();
  });

  test('should display instance card', async ({ page }) => {
    // The newly registered user should have one instance
    const instanceCard = page.locator('[class*="rounded-lg"][class*="border"]').first();
    await expect(instanceCard).toBeVisible();

    // Should show organization name in instance card (h3 heading)
    const orgName = page.locator('h3:has-text("Test Organization")');
    await expect(orgName).toBeVisible();
  });

  test('should show trial status', async ({ page }) => {
    // New registration should be on trial
    const trialBadge = page.getByText(/trial|dùng thử/i);
    await expect(trialBadge.first()).toBeVisible();
  });

  test('should navigate to instance detail', async ({ page }) => {
    // Click on instance card
    const instanceCard = page.locator('a[href*="/instances/"]').first();
    await instanceCard.click();

    // Should navigate to instance detail page
    await expect(page).toHaveURL(/\/instances\/[\w-]+/);
  });

  test('should persist session after page refresh', async ({ page }) => {
    // Refresh the page
    await page.reload();

    // Should still be on dashboard
    await expect(page).toHaveURL('/dashboard');

    // Dashboard should still show content
    const heading = page.getByRole('heading', { name: /dashboard/i });
    await expect(heading).toBeVisible();
  });

  test('should have navigation sidebar', async ({ page }) => {
    // Check for navigation links - sidebar uses Vietnamese labels
    const dashboardLink = page.getByRole('link', { name: /tổng quan/i });
    await expect(dashboardLink).toBeVisible();

    // Check for other nav items
    const settingsLink = page.getByRole('link', { name: /cài đặt/i });
    await expect(settingsLink).toBeVisible();
  });

  test('should navigate to settings', async ({ page }) => {
    const settingsLink = page.getByRole('link', { name: /cài đặt|settings/i });
    await settingsLink.click();

    await expect(page).toHaveURL('/settings');
  });
});

test.describe('Dashboard - Unauthenticated', () => {
  test('should redirect to login when not authenticated', async ({ page }) => {
    await clearBrowserStorage(page);

    // Try to access dashboard directly
    await page.goto('/dashboard');

    // Should redirect to login
    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });

  test('should redirect to login for settings page', async ({ page }) => {
    await clearBrowserStorage(page);

    await page.goto('/settings');

    await expect(page).toHaveURL('/login', { timeout: 5000 });
  });
});
