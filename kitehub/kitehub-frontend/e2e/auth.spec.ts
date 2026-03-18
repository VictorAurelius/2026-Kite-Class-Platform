/**
 * E2E tests for authentication flows.
 *
 * @since PR 5.10
 */

import { test, expect } from '@playwright/test';
import { createRegistrationData, invalidCredentials } from './fixtures/test-data';
import { clearBrowserStorage, isAuthenticated } from './utils/test-helpers';

test.describe('Registration Flow', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/register');
  });

  test('should display registration form', async ({ page }) => {
    // Check form elements using text and placeholder selectors
    await expect(page.getByText('Tên tổ chức')).toBeVisible();
    await expect(page.getByPlaceholder('abc-center')).toBeVisible();
    await expect(page.getByPlaceholder('email@example.com')).toBeVisible();
    await expect(page.getByText('Mật khẩu').first()).toBeVisible();
    await expect(page.getByRole('button', { name: /tạo tài khoản/i })).toBeVisible();
  });

  test('should show validation errors for empty form', async ({ page }) => {
    // Submit empty form
    await page.getByRole('button', { name: /tạo tài khoản/i }).click();

    // Check for validation messages (form should not navigate)
    await expect(page).toHaveURL('/register');
  });

  test('should register successfully with valid data', async ({ page }) => {
    const data = createRegistrationData();

    // Fill form using placeholders and input types
    await page.getByPlaceholder('Trung tâm Anh ngữ ABC').fill(data.organizationName);
    await page.getByPlaceholder('abc-center').fill(data.subdomain);
    await page.getByPlaceholder('email@example.com').fill(data.email);

    // Password fields - use type=password inputs
    const passwordFields = page.locator('input[type="password"]');
    await passwordFields.first().fill(data.password);
    await passwordFields.nth(1).fill(data.password); // Confirm password

    // Submit
    await page.getByRole('button', { name: /tạo tài khoản/i }).click();

    // Should redirect to dashboard
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });

    // Should be authenticated
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(true);
  });

  test('should show error for duplicate subdomain', async ({ page }) => {
    // First registration
    const data = createRegistrationData();

    await page.getByPlaceholder('Trung tâm Anh ngữ ABC').fill(data.organizationName);
    await page.getByPlaceholder('abc-center').fill(data.subdomain);
    await page.getByPlaceholder('email@example.com').fill(data.email);

    const passwordFields = page.locator('input[type="password"]');
    await passwordFields.first().fill(data.password);
    await passwordFields.nth(1).fill(data.password);

    await page.getByRole('button', { name: /tạo tài khoản/i }).click();
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });

    // Clear storage and try to register with same subdomain
    await clearBrowserStorage(page);
    await page.goto('/register');

    await page.getByPlaceholder('Trung tâm Anh ngữ ABC').fill('Another Org');
    await page.getByPlaceholder('abc-center').fill(data.subdomain); // Same subdomain
    await page.getByPlaceholder('email@example.com').fill('another@example.com');

    const passwordFields2 = page.locator('input[type="password"]');
    await passwordFields2.first().fill(data.password);
    await passwordFields2.nth(1).fill(data.password);

    await page.getByRole('button', { name: /tạo tài khoản/i }).click();

    // Should show error
    const errorMessage = page.getByText(/subdomain|đã được sử dụng|thất bại/i);
    await expect(errorMessage).toBeVisible({ timeout: 5000 });
  });

  test('should have link to login page', async ({ page }) => {
    const loginLink = page.getByRole('link', { name: /đăng nhập/i });
    await expect(loginLink).toBeVisible();

    await loginLink.click();
    await expect(page).toHaveURL('/login');
  });
});

test.describe('Login Flow', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await page.goto('/login');
  });

  test('should display login form', async ({ page }) => {
    await expect(page.getByPlaceholder('email@example.com')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.getByRole('button', { name: /đăng nhập/i })).toBeVisible();
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    // First register a user
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

    // Logout
    const logoutButton = page.getByRole('button', { name: /đăng xuất/i });
    if (await logoutButton.isVisible().catch(() => false)) {
      await logoutButton.click();
    }
    await clearBrowserStorage(page);

    // Now login with the same credentials
    await page.goto('/login');
    await page.getByPlaceholder('email@example.com').fill(data.email);
    await page.locator('input[type="password"]').fill(data.password);
    await page.getByRole('button', { name: /đăng nhập/i }).click();

    // Should redirect to dashboard
    await expect(page).toHaveURL('/dashboard', { timeout: 10000 });

    // Should be authenticated
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(true);
  });

  test('should show error for invalid credentials', async ({ page }) => {
    await page.getByPlaceholder('email@example.com').fill(invalidCredentials.email);
    await page.locator('input[type="password"]').fill(invalidCredentials.password);

    await page.getByRole('button', { name: /đăng nhập/i }).click();

    // Should show error message
    const errorMessage = page.getByText(/thất bại|sai|không đúng/i);
    await expect(errorMessage).toBeVisible({ timeout: 5000 });

    // Should stay on login page
    await expect(page).toHaveURL('/login');
  });

  test('should have link to register page', async ({ page }) => {
    const registerLink = page.getByRole('link', { name: /đăng ký/i });
    await expect(registerLink).toBeVisible();

    await registerLink.click();
    await expect(page).toHaveURL('/register');
  });
});

test.describe('Logout Flow', () => {
  test('should logout and redirect to home', async ({ page }) => {
    // First register to get authenticated
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

    // Find and click logout - look for it directly or in menu
    const logoutButton = page.getByRole('button', { name: /đăng xuất|logout/i });

    // Logout might be in a dropdown menu
    if (await logoutButton.isVisible().catch(() => false)) {
      await logoutButton.click();
    } else {
      // Try clicking user menu first
      const userMenu = page.getByRole('button', { name: /menu|user|profile/i });
      if (await userMenu.isVisible().catch(() => false)) {
        await userMenu.click();
        await page.getByText(/đăng xuất|logout/i).click();
      }
    }

    // Should redirect to home or login
    await expect(page).toHaveURL(/\/(login)?$/);

    // Should be logged out
    const authenticated = await isAuthenticated(page);
    expect(authenticated).toBe(false);
  });
});
