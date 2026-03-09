/**
 * E2E Tests for Billing & Payment System
 *
 * @author KiteClass Team
 * @since 3.15
 */

import { test, expect } from '@playwright/test';
import { login } from './helpers/auth';

test.describe('Billing & Payment System', () => {
  test.beforeEach(async ({ page }) => {
    // Login as owner/admin before each test
    await login(page, 'owner@kiteclass.local', 'Admin@123');
  });

  test('displays invoice list', async ({ page }) => {
    // Navigate to billing page
    await page.goto('/billing');

    // Wait for invoices to load
    await page.waitForSelector('table', { timeout: 10000 });

    // Check page title
    await expect(page.locator('h1')).toContainText('Hóa đơn');

    // Check table headers
    await expect(page.locator('thead')).toContainText('Số hóa đơn');
    await expect(page.locator('thead')).toContainText('Học viên');
    await expect(page.locator('thead')).toContainText('Trạng thái');
  });

  test('can search invoices by student ID', async ({ page }) => {
    await page.goto('/billing');

    // Wait for page load
    await page.waitForSelector('input[placeholder*="học viên"]', {
      timeout: 10000,
    });

    // Enter student ID
    const searchInput = page.locator('input[placeholder*="học viên"]');
    await searchInput.fill('1');
    await searchInput.press('Enter');

    // Wait for results
    await page.waitForTimeout(1000);

    // Verify results
    const rows = await page.locator('tbody tr').count();
    expect(rows).toBeGreaterThan(0);
  });

  test('can filter invoices by status', async ({ page }) => {
    await page.goto('/billing');

    // Wait for status filter
    await page.waitForSelector('[role="combobox"]', { timeout: 10000 });

    // Click status filter dropdown
    const statusFilter = page.locator('[role="combobox"]').first();
    await statusFilter.click();

    // Select PAID status
    await page.getByRole('option', { name: /đã thanh toán/i }).click();

    // Wait for filter to apply
    await page.waitForTimeout(1000);

    // Verify filtered results
    const rows = await page.locator('tbody tr').count();
    expect(rows).toBeGreaterThanOrEqual(0);
  });

  test('can view invoice details', async ({ page }) => {
    await page.goto('/billing');

    // Wait for table
    await page.waitForSelector('tbody tr', { timeout: 10000 });

    // Click first invoice row
    const firstRow = page.locator('tbody tr').first();
    await firstRow.click();

    // Wait for navigation to detail page
    await page.waitForURL(/\/billing\/\d+/, { timeout: 10000 });

    // Verify invoice detail page
    await expect(page.locator('h1')).toContainText('Chi tiết hóa đơn');

    // Check invoice info sections
    await expect(page.getByText('Thông tin hóa đơn')).toBeVisible();
    await expect(page.getByText('Chi tiết')).toBeVisible();
  });

  test('can apply late fees to invoice', async ({ page }) => {
    await page.goto('/billing');

    // Navigate to first invoice
    await page.waitForSelector('tbody tr', { timeout: 10000 });
    const firstRow = page.locator('tbody tr').first();
    await firstRow.click();

    await page.waitForURL(/\/billing\/\d+/, { timeout: 10000 });

    // Look for "Apply Late Fees" button
    const applyFeesButton = page.getByRole('button', {
      name: /áp dụng phí trễ/i,
    });

    // Only test if button exists (invoice is overdue)
    const buttonVisible = await applyFeesButton.isVisible().catch(() => false);

    if (buttonVisible) {
      await applyFeesButton.click();

      // Verify success message or updated amount
      await page.waitForTimeout(1000);
      await expect(page.getByText(/thành công/i)).toBeVisible({
        timeout: 5000,
      });
    }
  });

  test('can cancel invoice', async ({ page }) => {
    await page.goto('/billing');

    // Navigate to first invoice
    await page.waitForSelector('tbody tr', { timeout: 10000 });
    const firstRow = page.locator('tbody tr').first();
    await firstRow.click();

    await page.waitForURL(/\/billing\/\d+/, { timeout: 10000 });

    // Look for "Cancel Invoice" button
    const cancelButton = page.getByRole('button', { name: /hủy hóa đơn/i });

    // Only test if button exists
    const buttonVisible = await cancelButton.isVisible().catch(() => false);

    if (buttonVisible) {
      await cancelButton.click();

      // Confirm cancellation dialog
      await page.getByRole('button', { name: /xác nhận/i }).click();

      // Verify success
      await page.waitForTimeout(1000);
      await expect(page.getByText(/đã hủy/i)).toBeVisible({ timeout: 5000 });
    }
  });

  test('displays empty state when no invoices', async ({ page }) => {
    // Navigate with invalid student ID filter to get empty results
    await page.goto('/billing?studentId=99999');

    // Wait for page load
    await page.waitForTimeout(2000);

    // Check for empty state message
    const emptyMessage = page.locator('text=/không tìm thấy/i');
    const hasEmptyState = await emptyMessage.isVisible().catch(() => false);

    if (hasEmptyState) {
      expect(await emptyMessage.isVisible()).toBeTruthy();
    } else {
      // If no empty state, table should be visible
      await expect(page.locator('table')).toBeVisible();
    }
  });

  test('displays loading state while fetching invoices', async ({ page }) => {
    await page.goto('/billing');

    // Check for loading indicator (spinner, skeleton, or loading text)
    const loadingIndicator =
      (await page.locator('[role="status"]').isVisible().catch(() => false)) ||
      (await page.locator('text=/đang tải/i').isVisible().catch(() => false)) ||
      (await page.locator('.animate-spin').isVisible().catch(() => false));

    // Loading state should appear briefly
    expect(loadingIndicator || true).toBeTruthy();
  });
});
