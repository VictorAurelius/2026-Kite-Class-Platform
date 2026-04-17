import { test, expect } from '@playwright/test';

test.describe('Homepage', () => {
  test('should load homepage', async ({ page }) => {
    await page.goto('/');
    // Check page loads and has KiteClass branding in header nav link
    await expect(page.getByRole('link', { name: 'KiteClass' }).first()).toBeVisible();
  });
});
