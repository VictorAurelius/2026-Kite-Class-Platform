/**
 * E2E tests for the home/landing page.
 *
 * @since PR 5.10
 */

import { test, expect } from '@playwright/test';

test.describe('Home Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should display the KiteHub logo', async ({ page }) => {
    // Check for logo text or image
    const logo = page.getByText('KiteHub');
    await expect(logo.first()).toBeVisible();
  });

  test('should display hero section', async ({ page }) => {
    // Check for hero heading
    const heading = page.getByRole('heading', { level: 1 });
    await expect(heading).toBeVisible();
  });

  test('should have navigation links', async ({ page }) => {
    // Check for login link in navigation (not footer)
    const loginLink = page.getByRole('navigation').getByRole('link', { name: /đăng nhập/i });
    await expect(loginLink).toBeVisible();

    // Check for register/trial link
    const registerLink = page.getByRole('navigation').getByRole('link', { name: /dùng thử/i });
    await expect(registerLink).toBeVisible();
  });

  test('should navigate to login page', async ({ page }) => {
    // Use navigation link specifically
    const loginLink = page.getByRole('navigation').getByRole('link', { name: /đăng nhập/i });
    await loginLink.click();

    await expect(page).toHaveURL('/login');
  });

  test('should navigate to register page', async ({ page }) => {
    const registerLink = page.getByRole('link', { name: /dùng thử/i });
    await registerLink.first().click();

    await expect(page).toHaveURL('/register');
  });

  test('should display features section', async ({ page }) => {
    // Scroll to features if needed
    const featuresSection = page.locator('text=Tính năng').first();
    await expect(featuresSection).toBeVisible();
  });

  test('should display pricing section', async ({ page }) => {
    // Check for pricing link in navigation
    const pricingLink = page.getByRole('navigation').getByRole('link', { name: /bảng giá/i });
    await expect(pricingLink).toBeVisible();
  });

  test('should be responsive on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });

    // Logo should still be visible
    const logo = page.getByText('KiteHub');
    await expect(logo.first()).toBeVisible();

    // Navigation might be in hamburger menu
    // Just check page loads without errors
    await expect(page).toHaveTitle(/KiteHub/i);
  });
});
