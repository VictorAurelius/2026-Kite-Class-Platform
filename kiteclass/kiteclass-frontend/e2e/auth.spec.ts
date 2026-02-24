/**
 * E2E tests for Authentication Flow
 *
 * Tests login, logout, and authentication state management.
 *
 * @since 2026-02-24
 */

import { test, expect } from '@playwright/test';
import { login, logout, TEST_USER, isAuthenticated } from './helpers/auth';

test.describe('Authentication Flow', () => {
  test('should display login page', async ({ page }) => {
    await page.goto('/login');

    // Check page elements
    await expect(page.getByText('Welcome back')).toBeVisible();
    await expect(page.getByText('Sign in to your account to continue')).toBeVisible();
    await expect(page.getByPlaceholder('you@example.com')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    await page.goto('/login');

    // Fill in credentials
    await page.fill('input[name="email"]', TEST_USER.email);
    await page.fill('input[name="password"]', TEST_USER.password);

    // Submit form
    await page.click('button[type="submit"]');

    // Should redirect to dashboard
    await page.waitForURL(/\/(dashboard)?$/, { timeout: 10000 });

    // Should show success toast
    await expect(page.getByText(/login successful|welcome back/i)).toBeVisible({
      timeout: 5000,
    });

    // Verify tokens are stored
    const hasToken = await isAuthenticated(page);
    expect(hasToken).toBe(true);
  });

  test('should show error with invalid credentials', async ({ page }) => {
    await page.goto('/login');

    // Fill in invalid credentials
    await page.fill('input[name="email"]', 'wrong@example.com');
    await page.fill('input[name="password"]', 'wrongpassword');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show error message
    await expect(
      page.getByText(/login failed|invalid email or password/i)
    ).toBeVisible({ timeout: 5000 });

    // Should stay on login page
    await expect(page).toHaveURL('/login');
  });

  test('should validate email format', async ({ page }) => {
    await page.goto('/login');

    // Fill in invalid email
    await page.fill('input[name="email"]', 'not-an-email');
    await page.fill('input[name="password"]', 'password123');

    // Try to submit
    await page.click('button[type="submit"]');

    // Should show validation error
    await expect(page.getByText(/invalid email/i)).toBeVisible();
  });

  test('should validate password length', async ({ page }) => {
    await page.goto('/login');

    // Fill in short password
    await page.fill('input[name="email"]', TEST_USER.email);
    await page.fill('input[name="password"]', '12345'); // Less than 6 chars

    // Try to submit
    await page.click('button[type="submit"]');

    // Should show validation error
    await expect(
      page.getByText(/password must be at least 6 characters/i)
    ).toBeVisible();
  });

  test('should redirect to login if accessing protected route while not authenticated', async ({
    page,
  }) => {
    // Try to access students page without login
    await page.goto('/students');

    // Should redirect to login
    await page.waitForURL('/login', { timeout: 5000 });
    await expect(page.getByText('Welcome back')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    // First login
    await login(page);

    // Verify we're on dashboard
    await expect(page).toHaveURL(/\/(dashboard)?$/);

    // Logout
    await logout(page);

    // Should redirect to login page
    await expect(page).toHaveURL('/login');

    // Tokens should be cleared
    const hasToken = await isAuthenticated(page);
    expect(hasToken).toBe(false);
  });

  test('should persist authentication across page refreshes', async ({ page }) => {
    // Login
    await login(page);

    // Navigate to students page
    await page.goto('/students');
    await expect(page.getByText('Học viên')).toBeVisible();

    // Refresh page
    await page.reload();

    // Should still be on students page (not redirected to login)
    await expect(page.getByText('Học viên')).toBeVisible();

    // Tokens should still exist
    const hasToken = await isAuthenticated(page);
    expect(hasToken).toBe(true);
  });

  test('should handle remember me checkbox', async ({ page }) => {
    await page.goto('/login');

    // Check remember me
    const rememberCheckbox = page.locator('input[id="remember"]');
    await rememberCheckbox.check();
    await expect(rememberCheckbox).toBeChecked();

    // Uncheck
    await rememberCheckbox.uncheck();
    await expect(rememberCheckbox).not.toBeChecked();
  });

  test('should have forgot password link', async ({ page }) => {
    await page.goto('/login');

    const forgotLink = page.getByRole('link', { name: /forgot password/i });
    await expect(forgotLink).toBeVisible();
    await expect(forgotLink).toHaveAttribute('href', '/forgot-password');
  });

  test('should have sign up link', async ({ page }) => {
    await page.goto('/login');

    const signupLink = page.getByRole('link', { name: /sign up/i });
    await expect(signupLink).toBeVisible();
    await expect(signupLink).toHaveAttribute('href', '/register');
  });
});
