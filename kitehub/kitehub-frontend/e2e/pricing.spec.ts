/**
 * E2E tests for public pricing page.
 *
 * @since PR 5.12
 */

import { test, expect } from '@playwright/test';

test.describe('Pricing Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/pricing');
  });

  test('should display pricing heading', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /bảng giá/i });
    await expect(heading).toBeVisible();
  });

  test('should display all 4 pricing tiers', async ({ page }) => {
    await expect(page.getByText('FREE')).toBeVisible();
    await expect(page.getByText('BASIC')).toBeVisible();
    await expect(page.getByText('PREMIUM')).toBeVisible();
    await expect(page.getByText('ENTERPRISE')).toBeVisible();
  });

  test('should show free tier as Miễn phí', async ({ page }) => {
    await expect(page.getByText(/miễn phí/i).first()).toBeVisible();
  });

  test('should have billing toggle for monthly/yearly', async ({ page }) => {
    const monthlyText = page.getByText(/tháng/i).first();
    await expect(monthlyText).toBeVisible();

    const yearlyText = page.getByText(/năm/i).first();
    await expect(yearlyText).toBeVisible();
  });

  test('should highlight PREMIUM as most popular', async ({ page }) => {
    const popularBadge = page.getByText(/phổ biến nhất/i);
    await expect(popularBadge).toBeVisible();
  });

  test('should have CTA buttons for each tier', async ({ page }) => {
    const startFreeBtn = page.getByRole('link', { name: /bắt đầu miễn phí/i });
    await expect(startFreeBtn).toBeVisible();

    const contactBtn = page.getByRole('link', { name: /liên hệ/i });
    await expect(contactBtn).toBeVisible();
  });

  test('should display FAQ section', async ({ page }) => {
    const faqHeading = page.getByRole('heading', { name: /câu hỏi thường gặp/i });
    await expect(faqHeading).toBeVisible();
  });

  test('should have FAQ items', async ({ page }) => {
    await expect(page.getByText(/trial 14 ngày/i)).toBeVisible();
    await expect(page.getByText(/thanh toán bằng hình thức/i)).toBeVisible();
  });

  test('should navigate to register from CTA', async ({ page }) => {
    const startFreeBtn = page.getByRole('link', { name: /bắt đầu miễn phí/i });
    await startFreeBtn.click();
    await expect(page).toHaveURL('/register');
  });

  test('should be accessible from homepage navigation', async ({ page }) => {
    await page.goto('/');
    const pricingLink = page.getByRole('navigation').getByRole('link', { name: /bảng giá|pricing/i });
    await expect(pricingLink).toBeVisible();
  });
});
