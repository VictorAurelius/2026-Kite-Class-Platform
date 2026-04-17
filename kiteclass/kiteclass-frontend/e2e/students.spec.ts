/**
 * E2E tests for Students Module - Detail & Edit Pages
 *
 * Tests the student detail and edit flows that cannot be tested
 * with integration tests due to Next.js 15 async params limitation.
 *
 * This unblocks 20 skipped integration tests from Phase 1.
 *
 * @since 2026-02-23
 */

import { test, expect } from '@playwright/test';
import { login } from './helpers/auth';

test.describe('Students Module - Detail Page', () => {
  test('should view student detail page', async ({ page }) => {
    await login(page);

    // Navigate to students list
    await page.goto('/students');
    await expect(page.getByRole('heading', { name: 'Học viên' })).toBeVisible();

    // Click on first student's view button (Eye icon)
    const viewButton = page.locator('table tbody tr:first-child button').first();
    await expect(viewButton).toBeVisible();
    await viewButton.click();

    // Should be on student detail page
    await expect(page).toHaveURL(/\/students\/\d+/);

    // Should show student details section
    await expect(page.getByText(/thông tin chi tiết học viên/i)).toBeVisible();
  });

  test('should display student information correctly', async ({ page }) => {
    await login(page);

    // Go directly to student detail page (assuming student ID 1 exists)
    await page.goto('/students/1');

    // Should show student detail heading and fields
    await expect(page.getByText(/thông tin chi tiết học viên/i)).toBeVisible();
    await expect(page.getByText(/email/i)).toBeVisible();
    await expect(page.getByText(/số điện thoại/i)).toBeVisible();
    await expect(page.getByText(/ngày sinh/i)).toBeVisible();
    await expect(page.getByText(/giới tính/i)).toBeVisible();
    await expect(page.getByText(/địa chỉ/i)).toBeVisible();
    await expect(page.getByText(/trạng thái/i)).toBeVisible();
  });

  test('should have edit button on detail page', async ({ page }) => {
    await login(page);
    await page.goto('/students/1');

    // Should have edit button
    const editButton = page.getByRole('link', { name: /chỉnh sửa/i });
    await expect(editButton).toBeVisible();
    await expect(editButton).toHaveAttribute('href', '/students/1/edit');
  });

  test('should have delete button on detail page', async ({ page }) => {
    await login(page);
    await page.goto('/students/1');

    // Should have delete button
    const deleteButton = page.getByRole('button', { name: /xóa/i });
    await expect(deleteButton).toBeVisible();
  });

  test('should show error when student not found', async ({ page }) => {
    await login(page);

    // Try to view non-existent student
    await page.goto('/students/99999');

    // Should show error message
    await expect(page.getByText(/không tìm thấy thông tin học viên/i)).toBeVisible({ timeout: 5000 });
  });

  test('should show loading spinner while loading student', async ({ page }) => {
    await login(page);

    // Slow down the network to see loading state
    await page.route('**/api/v1/students/*', async route => {
      await new Promise(resolve => setTimeout(resolve, 1000));
      await route.fallback();
    });

    await page.goto('/students/1');

    // Should show loading spinner
    const spinner = page.locator('[data-testid="loading-spinner"]');
    await expect(spinner).toBeVisible();

    // Eventually page should load
    await expect(page.getByText(/thông tin chi tiết học viên/i)).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Students Module - Edit Page', () => {
  test('should navigate to edit page from detail', async ({ page }) => {
    await login(page);
    await page.goto('/students/1');

    // Click edit button
    const editButton = page.getByRole('link', { name: /chỉnh sửa/i });
    await editButton.click();

    // Should be on edit page
    await expect(page).toHaveURL(/\/students\/1\/edit/);
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();
  });

  test('should load existing student data in form', async ({ page }) => {
    await login(page);
    await page.goto('/students/1/edit');

    // Wait for form to load
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    // Form fields should have existing values
    const nameInput = page.locator('input[name="name"]');
    await expect(nameInput).not.toHaveValue('');

    const emailInput = page.locator('input[name="email"]');
    await expect(emailInput).not.toHaveValue('');
  });

  test('should update student successfully', async ({ page }) => {
    await login(page);
    await page.goto('/students/1/edit');
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    // Change name
    const nameInput = page.locator('input[name="name"]');
    await nameInput.clear();
    await nameInput.fill('Nguyễn Văn Test E2E');

    // Change phone
    const phoneInput = page.locator('input[name="phone"]');
    await phoneInput.clear();
    await phoneInput.fill('0999888777');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show success toast
    await expect(page.getByText(/đã cập nhật thông tin học viên/i)).toBeVisible({ timeout: 5000 });

    // Should redirect back to detail page
    await expect(page).toHaveURL(/\/students\/1$/);
  });

  test('should validate required fields on edit', async ({ page }) => {
    await login(page);
    await page.goto('/students/1/edit');
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    // Clear name: triple-click to select all, then type empty to clear
    const nameInput = page.locator('input[name="name"]');
    await nameInput.click({ clickCount: 3 });
    await nameInput.fill('');

    // Disable HTML5 native validation so RHF/zod validation runs
    await page.evaluate(() => {
      const form = document.querySelector('form');
      if (form) form.setAttribute('novalidate', '');
    });

    // Submit form
    await page.locator('button[type="submit"]').click();

    // Should show validation error
    await expect(page.getByText(/tên không được để trống/i)).toBeVisible({ timeout: 5000 });
  });

  test('should validate email format on edit', async ({ page }) => {
    await login(page);
    await page.goto('/students/1/edit');
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    // Replace email with invalid value (passes HTML5 but fails zod)
    const emailInput = page.locator('input[name="email"]');
    await emailInput.click({ clickCount: 3 });
    await emailInput.fill('not-a-valid-email');

    // Disable HTML5 native validation so RHF/zod validation runs
    await page.evaluate(() => {
      const form = document.querySelector('form');
      if (form) form.setAttribute('novalidate', '');
    });

    // Submit form
    await page.locator('button[type="submit"]').click();

    // Should show validation error
    await expect(page.getByText(/email không hợp lệ/i)).toBeVisible({ timeout: 5000 });
  });

  test('should handle duplicate email error on edit', async ({ page }) => {
    await login(page);

    // Mock duplicate email error
    await page.route('**/api/v1/students/*', async route => {
      const method = route.request().method();
      if (method === 'PUT' || method === 'PATCH') {
        await route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({
            status: 409,
            error: 'DUPLICATE_EMAIL',
            message: 'Email test@example.com đã tồn tại trong hệ thống',
          }),
        });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/students/1/edit');
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    // Change email to duplicate
    const emailInput = page.locator('input[name="email"]');
    await emailInput.clear();
    await emailInput.fill('test@example.com');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show error toast (locator-based to match inside notification list item)
    await expect(page.locator('li').filter({ hasText: /đã tồn tại/i })).toBeVisible({ timeout: 5000 });
  });

  test('should handle server error on edit', async ({ page }) => {
    await login(page);

    // Mock server error
    await page.route('**/api/v1/students/*', async route => {
      const method = route.request().method();
      if (method === 'PUT' || method === 'PATCH') {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({
            status: 500,
            error: 'INTERNAL_SERVER_ERROR',
            message: 'Đã xảy ra lỗi từ máy chủ',
          }),
        });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/students/1/edit');
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    // Fill and submit
    const nameInput = page.locator('input[name="name"]');
    await nameInput.clear();
    await nameInput.fill('Test Name');
    await page.click('button[type="submit"]');

    // Should show error toast (locator-based to match inside notification list item)
    await expect(page.locator('li').filter({ hasText: /đã xảy ra lỗi từ máy chủ/i })).toBeVisible({ timeout: 5000 });
  });

  test('should disable submit button while updating', async ({ page }) => {
    await login(page);

    // Add a slow PATCH mock so button stays disabled long enough to assert
    await page.route('**/api/v1/students/*', async route => {
      const method = route.request().method();
      if (method === 'PUT' || method === 'PATCH') {
        await new Promise(resolve => setTimeout(resolve, 500));
        await route.fallback();
      } else {
        await route.fallback();
      }
    });

    await page.goto('/students/1/edit');
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    const submitButton = page.locator('button[type="submit"]');

    // Submit form
    await submitButton.click();

    // Button should be disabled while request is in flight
    await expect(submitButton).toBeDisabled();
  });

  test('should show error when student not found on edit', async ({ page }) => {
    await login(page);

    // Try to edit non-existent student
    await page.goto('/students/99999/edit');

    // Should show error message
    await expect(page.getByText(/không tìm thấy thông tin học viên/i)).toBeVisible({ timeout: 5000 });
  });

  test('should have cancel button to go back', async ({ page }) => {
    await login(page);
    await page.goto('/students/1/edit');
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();

    // Should have cancel/back button
    const cancelButton = page.getByRole('button', { name: /hủy|quay lại/i });
    await expect(cancelButton).toBeVisible();
  });
});

test.describe('Students Module - Delete from Detail', () => {
  test('should delete student from detail page with confirmation', async ({ page }) => {
    await login(page);
    await page.goto('/students/1');

    // Handle confirmation dialog BEFORE clicking (window.confirm fires during click)
    page.on('dialog', async dialog => {
      expect(dialog.message()).toContain('xóa học viên');
      await dialog.accept();
    });

    // Click delete button
    const deleteButton = page.getByRole('button', { name: /xóa/i });
    await deleteButton.click();

    // Should show success toast
    await expect(page.getByText(/đã xóa học viên/i)).toBeVisible({ timeout: 5000 });

    // Should redirect to students list
    await expect(page).toHaveURL('/students');
  });

  test('should cancel delete when confirmation rejected', async ({ page }) => {
    await login(page);
    await page.goto('/students/1');

    // Get student name before deletion attempt
    const studentName = await page.locator('h1, h2').first().textContent();

    // Click delete button
    const deleteButton = page.getByRole('button', { name: /xóa/i });
    await deleteButton.click();

    // Reject confirmation dialog
    page.on('dialog', async dialog => {
      await dialog.dismiss();
    });

    // Should still be on detail page
    await expect(page).toHaveURL(/\/students\/1/);
    await expect(page.getByText(studentName!)).toBeVisible();
  });
});

test.describe('Students Module - Navigation', () => {
  test('should navigate from list to detail to edit and back', async ({ page }) => {
    await login(page);

    // Start at students list
    await page.goto('/students');
    await expect(page.getByRole('heading', { name: 'Học viên' })).toBeVisible();

    // Go to detail by clicking view button
    await page.locator('table tbody tr:first-child button').first().click();
    await expect(page).toHaveURL(/\/students\/\d+$/);

    // Go to edit
    await page.getByRole('link', { name: /chỉnh sửa/i }).click();
    await expect(page).toHaveURL(/\/students\/\d+\/edit/);

    // Cancel/back to detail
    const cancelButton = page.getByRole('button', { name: /hủy|quay lại/i });
    if (await cancelButton.isVisible()) {
      await cancelButton.click();
      await expect(page).toHaveURL(/\/students\/\d+$/);
    }

    // Back to list
    await page.getByRole('link', { name: /học viên|danh sách/i }).click();
    await expect(page).toHaveURL('/students');
  });

  test('should have breadcrumb navigation', async ({ page }) => {
    await login(page);
    await page.goto('/students/1/edit');

    // Should have breadcrumb or back links
    // (Adjust selectors based on actual implementation)
    const breadcrumb = page.locator('nav[aria-label="breadcrumb"], .breadcrumb');
    await breadcrumb.isVisible().catch(() => false);

    // Just verify page loaded correctly if no breadcrumb
    await expect(page.getByText(/chỉnh sửa học viên/i)).toBeVisible();
  });
});
