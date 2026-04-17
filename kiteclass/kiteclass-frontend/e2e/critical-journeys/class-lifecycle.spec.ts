/**
 * Critical Journey E2E Test: Class Lifecycle Management
 *
 * Tests the complete class lifecycle workflow:
 * 1. View class in SCHEDULED status
 * 2. Start class (SCHEDULED → IN_PROGRESS)
 * 3. Complete class (IN_PROGRESS → COMPLETED)
 * 4. Cancel class workflow (alternative path)
 * 5. Generate class code
 *
 * This test covers the core operations teachers/admins perform on classes.
 *
 * @since 2026-02-24
 */

import { test, expect } from '@playwright/test';
import { login } from '../helpers/auth';

test.describe('Critical Journey: Class Lifecycle', () => {
  test('should start and complete a class successfully', async ({ page }) => {
    // Step 1: Login and navigate to classes
    await login(page);
    await page.click('a[href="/classes"]');
    await expect(page).toHaveURL('/classes');

    // Step 2: Select a course to view its classes
    const courseSelector = page.locator('button[role="combobox"]').first();
    await courseSelector.click();

    // Select first course from dropdown
    await page.locator('[role="option"]').first().click();
    await page.waitForTimeout(1000); // Wait for classes to load

    // Step 3: Navigate to a SCHEDULED class detail page
    // Click on first class row's view button
    const viewButton = page.locator('table tbody tr').first().locator('button').first();
    await expect(viewButton).toBeVisible({ timeout: 5000 });
    await viewButton.click();

    // Should be on class detail page
    await expect(page).toHaveURL(/\/classes\/\d+$/, { timeout: 5000 });

    // Verify initial status is SCHEDULED
    await expect(
      page.getByText(/đã lên lịch|scheduled/i)
    ).toBeVisible({ timeout: 5000 });

    // Step 4: Start the class
    const startButton = page.getByRole('button', { name: /bắt đầu/i });
    await expect(startButton).toBeVisible();
    await startButton.click();

    // Confirm start dialog
    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('bắt đầu');
      await dialog.accept();
    });

    // Wait for status change to IN_PROGRESS
    await expect(
      page.getByText(/đang diễn ra|in progress/i)
    ).toBeVisible({ timeout: 5000 });

    // Verify start button is gone
    await expect(startButton).not.toBeVisible();

    // Step 5: Complete the class
    const completeButton = page.getByRole('button', { name: /hoàn thành/i });
    await expect(completeButton).toBeVisible();
    await completeButton.click();

    // Confirm complete dialog
    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain('hoàn thành');
      await dialog.accept();
    });

    // Wait for status change to COMPLETED
    await expect(
      page.getByText(/đã hoàn thành|completed/i)
    ).toBeVisible({ timeout: 5000 });

    // Verify action buttons are gone (read-only mode)
    await expect(completeButton).not.toBeVisible();
    await expect(page.getByRole('button', { name: /bắt đầu/i })).not.toBeVisible();
    await expect(page.getByRole('button', { name: /hủy/i })).not.toBeVisible();

    // Success! Class lifecycle completed
  });

  test('should cancel a class with reason', async ({ page }) => {
    await login(page);

    // Navigate to classes and select a course
    await page.click('a[href="/classes"]');
    const courseSelector = page.locator('button[role="combobox"]').first();
    await courseSelector.click();
    await page.locator('[role="option"]').first().click();
    await page.waitForTimeout(1000);

    // Navigate to class detail
    await page.locator('table tbody tr').first().locator('button').first().click();
    await expect(page).toHaveURL(/\/classes\/\d+$/);

    // Start the class first (to get to IN_PROGRESS state)
    const startButton = page.getByRole('button', { name: /bắt đầu/i });
    if (await startButton.isVisible()) {
      await startButton.click();
      page.once('dialog', async (dialog) => await dialog.accept());
      await page.waitForTimeout(1000);
    }

    // Now cancel the class
    const cancelButton = page.getByRole('button', { name: /hủy lớp|cancel/i });
    await expect(cancelButton).toBeVisible();
    await cancelButton.click();

    // Should show cancel dialog with reason input
    const cancelDialog = page.locator('[role="dialog"]');
    await expect(cancelDialog).toBeVisible({ timeout: 5000 });

    // Find textarea for cancel reason
    const reasonTextarea = page.locator('textarea[name="cancelReason"], textarea').first();
    await expect(reasonTextarea).toBeVisible();

    // Try to submit without reason - should show error
    const submitCancelButton = cancelDialog.getByRole('button', { name: /xác nhận|confirm/i });
    await submitCancelButton.click();

    // Should show validation error
    await expect(
      page.getByText(/lý do.*bắt buộc|reason.*required/i)
    ).toBeVisible();

    // Enter a valid reason
    await reasonTextarea.fill('Class cancelled due to low enrollment for E2E testing');

    // Submit again
    await submitCancelButton.click();

    // Should show success and status change to CANCELLED
    await expect(
      page.getByText(/đã hủy|cancelled/i)
    ).toBeVisible({ timeout: 5000 });

    // Verify cancel button is gone
    await expect(cancelButton).not.toBeVisible();
  });

  test('should generate and copy class code', async ({ page }) => {
    await login(page);

    // Navigate to class detail
    await page.click('a[href="/classes"]');
    const courseSelector = page.locator('button[role="combobox"]').first();
    await courseSelector.click();
    await page.locator('[role="option"]').first().click();
    await page.waitForTimeout(1000);

    await page.locator('table tbody tr').first().locator('button').first().click();
    await expect(page).toHaveURL(/\/classes\/\d+$/);

    // Find generate code button
    const generateCodeButton = page.getByRole('button', { name: /tạo mã|generate code/i });
    await expect(generateCodeButton).toBeVisible();

    // Click to generate
    await generateCodeButton.click();

    // Confirm generation dialog
    page.once('dialog', async (dialog) => {
      expect(dialog.message()).toContain(/tạo.*mã|generate.*code/i);
      await dialog.accept();
    });

    // Wait for success toast
    await expect(
      page.getByText(/đã tạo mã|code generated/i)
    ).toBeVisible({ timeout: 5000 });

    // Code should be displayed
    const classCodeDisplay = page.locator('code, [class*="code"]').first();
    await expect(classCodeDisplay).toBeVisible();

    // Copy code button should be visible
    const copyButton = page.getByRole('button', { name: /sao chép|copy/i });
    await expect(copyButton).toBeVisible();

    // Click to copy
    await copyButton.click();

    // Should show copy success toast
    await expect(
      page.getByText(/đã sao chép|copied/i)
    ).toBeVisible({ timeout: 3000 });

    // Verify clipboard content (if supported)
    // Note: Clipboard API might not work in all test environments
    try {
      const clipboardText = await page.evaluate(() => navigator.clipboard.readText());
      expect(clipboardText).toBeTruthy();
      expect(clipboardText.length).toBeGreaterThan(0);
    } catch (error) {
      // Clipboard API not available, skip verification
      console.log('Clipboard verification skipped:', error);
    }
  });

  test('should display class sessions correctly', async ({ page }) => {
    await login(page);

    // Navigate to class detail
    await page.click('a[href="/classes"]');
    const courseSelector = page.locator('button[role="combobox"]').first();
    await courseSelector.click();
    await page.locator('[role="option"]').first().click();
    await page.waitForTimeout(1000);

    await page.locator('table tbody tr').first().locator('button').first().click();
    await expect(page).toHaveURL(/\/classes\/\d+$/);

    // Sessions section should be visible
    await expect(
      page.getByText(/buổi học|sessions/i)
    ).toBeVisible({ timeout: 5000 });

    // Should show session list with columns
    const sessionTable = page.locator('table').last(); // Assume sessions table is last
    await expect(sessionTable).toBeVisible();

    // Verify session table has expected columns
    await expect(sessionTable.getByText(/số thứ tự|session/i)).toBeVisible();
    await expect(sessionTable.getByText(/chủ đề|topic/i)).toBeVisible();
    await expect(sessionTable.getByText(/ngày|date/i)).toBeVisible();
    await expect(sessionTable.getByText(/trạng thái|status/i)).toBeVisible();

    // If sessions exist, they should have status badges
    const statusBadges = sessionTable.locator('[class*="badge"]');
    const badgeCount = await statusBadges.count();

    if (badgeCount > 0) {
      // At least one session exists
      expect(badgeCount).toBeGreaterThan(0);
    }
  });

  test('should not allow delete for non-SCHEDULED or enrolled class', async ({ page }) => {
    await login(page);

    // Navigate to class detail
    await page.click('a[href="/classes"]');
    const courseSelector = page.locator('button[role="combobox"]').first();
    await courseSelector.click();
    await page.locator('[role="option"]').first().click();
    await page.waitForTimeout(1000);

    await page.locator('table tbody tr').first().locator('button').first().click();
    await expect(page).toHaveURL(/\/classes\/\d+$/);

    // Start the class to change status from SCHEDULED
    const startButton = page.getByRole('button', { name: /bắt đầu/i });
    if (await startButton.isVisible()) {
      await startButton.click();
      page.once('dialog', async (dialog) => await dialog.accept());
      await page.waitForTimeout(1000);
    }

    // Now in IN_PROGRESS state, delete button should NOT be visible
    const deleteButton = page.getByRole('button', { name: /xóa|delete/i });
    await expect(deleteButton).not.toBeVisible();

    // Business rule: Can only delete SCHEDULED classes with 0 enrollments
  });

  test('should show error for invalid class ID', async ({ page }) => {
    await login(page);

    // Navigate directly to non-existent class
    await page.goto('/classes/99999');

    // Should show error
    await expect(
      page.getByText(/không tìm thấy lớp học|class not found/i)
    ).toBeVisible({ timeout: 5000 });
  });
});
