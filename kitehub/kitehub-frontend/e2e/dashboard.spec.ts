/**
 * E2E tests for authenticated dashboard flows.
 *
 * @since PR 5.10
 */

import { test, expect } from '@playwright/test';
import { createRegistrationData } from './fixtures/test-data';
import {
  clearBrowserStorage,
  mockAllAuthAPIs,
  mockInstancesAPI,
  mockInstanceDetailAPI,
} from './utils/test-helpers';

test.describe('Dashboard', () => {
  const instanceId = '00000000-0000-0000-0000-000000000001';

  // Register and login before each test (with mocked APIs)
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await mockAllAuthAPIs(page);
    await mockInstancesAPI(page, instanceId);
    await mockInstanceDetailAPI(page, instanceId, 'TRIAL');

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

    // Mark OnboardingWizard as completed so it doesn't overlay the dashboard
    await page.evaluate(() => {
      localStorage.setItem('kite hub_onboarding_completed', 'true');
    });
    await page.reload();
    await expect(page).toHaveURL('/dashboard');
  });

  test('should display dashboard after login', async ({ page }) => {
    // Dashboard shows greeting heading (e.g. "Chào buổi sáng, ...")
    const heading = page.getByRole('heading', { name: /chào|quản lý trung tâm/i });
    await expect(heading.first()).toBeVisible();
  });

  test('should display welcome message with user info', async ({ page }) => {
    // Greeting contains "Chào" (morning/afternoon/evening greeting)
    const greeting = page.getByText(/chào/i);
    await expect(greeting.first()).toBeVisible();
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

    // Dashboard should still show content (greeting heading)
    const heading = page.getByRole('heading', { name: /chào|quản lý trung tâm/i });
    await expect(heading.first()).toBeVisible();
  });

  test('should have navigation sidebar', async ({ page }) => {
    // Check for navigation links - sidebar uses Vietnamese labels
    const dashboardLink = page.getByRole('link', { name: /tổng quan/i });
    await expect(dashboardLink).toBeVisible();

    // "Cài đặt" may appear in both sidebar and quick-start area — check first match
    const settingsLink = page.getByRole('link', { name: /cài đặt/i });
    await expect(settingsLink.first()).toBeVisible();
  });

  test('should navigate to settings', async ({ page }) => {
    // Use sidebar nav link (first match) to avoid strict mode on duplicate "Cài đặt" links
    const settingsLink = page.getByRole('link', { name: /cài đặt|settings/i });
    await settingsLink.first().click();

    await expect(page).toHaveURL('/settings', { timeout: 15000 });
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
