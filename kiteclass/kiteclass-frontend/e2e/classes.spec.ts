/**
 * E2E tests for Classes Module
 *
 * Tests the full class lifecycle including:
 * - Creating classes within course context
 * - Viewing class details
 * - Editing class information
 * - Changing location types
 * - Deleting classes
 *
 * These tests cover functionality that cannot be tested with
 * integration tests due to Next.js 15 async params limitation.
 *
 * @since 2026-02-23
 */

import { test, expect } from '@playwright/test';

// Mock authentication - adjust based on your auth setup
test.beforeEach(async ({ page }) => {
  // TODO: Replace with actual login flow
  // For now, assume user is authenticated
  await page.goto('/');
});

test.describe('Classes Module - Create Flow', () => {
  test('should navigate to create class page from course detail', async ({ page }) => {
    // Navigate to courses list
    await page.goto('/courses');
    await expect(page.getByText('Khóa học')).toBeVisible();

    // Click on first course to go to detail
    await page.click('table tbody tr:first-child');

    // Should be on course detail page
    await expect(page).toHaveURL(/\/courses\/\d+/);

    // Click "Add Class" button
    const addClassButton = page.getByRole('link', { name: /thêm lớp học/i });
    await expect(addClassButton).toBeVisible();
    await addClassButton.click();

    // Should be on create class page
    await expect(page).toHaveURL(/\/courses\/\d+\/classes\/new/);
    await expect(page.getByText('Thêm lớp học')).toBeVisible();
  });

  test('should create new class successfully', async ({ page }) => {
    // Go directly to create class page (assuming course ID 1 exists)
    await page.goto('/courses/1/classes/new');

    // Wait for page to load and show course context
    await expect(page.getByText('Thêm lớp học')).toBeVisible();
    await expect(page.getByText(/tạo lớp học mới cho khóa học:/i)).toBeVisible();

    // Fill in required fields
    await page.fill('input[name="name"]', 'Lớp Test E2E');

    // Select location type (should default to IN_PERSON)
    const locationSelect = page.locator('select[name="locationType"]');
    await expect(locationSelect).toHaveValue('IN_PERSON');

    // Fill optional fields
    await page.fill('textarea[name="description"]', 'Đây là lớp học test E2E');
    await page.fill('input[name="schedule"]', 'Thứ 2, 4, 6: 08:00-10:00');
    await page.fill('input[name="locationDetail"]', 'Phòng A101');

    // Fill dates
    await page.fill('input[name="startDate"]', '2026-03-01');
    await page.fill('input[name="endDate"]', '2026-06-30');

    // Max students should have default value 30
    const maxStudentsInput = page.locator('input[name="maxStudents"]');
    await expect(maxStudentsInput).toHaveValue('30');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show success toast
    await expect(page.getByText(/đã tạo lớp học mới/i)).toBeVisible({ timeout: 5000 });

    // Should redirect to course page (not classes list)
    await expect(page).toHaveURL(/\/courses\/1$/, { timeout: 5000 });
  });

  test('should show validation error for empty class name', async ({ page }) => {
    await page.goto('/courses/1/classes/new');
    await expect(page.getByText('Thêm lớp học')).toBeVisible();

    // Submit without filling name
    await page.click('button[type="submit"]');

    // Should show validation error
    await expect(page.getByText(/tên lớp học không được để trống/i)).toBeVisible();
  });

  test('should validate maxStudents is positive', async ({ page }) => {
    await page.goto('/courses/1/classes/new');
    await expect(page.getByText('Thêm lớp học')).toBeVisible();

    // Fill name
    await page.fill('input[name="name"]', 'Test Class');

    // Set maxStudents to 0
    const maxStudentsInput = page.locator('input[name="maxStudents"]');
    await maxStudentsInput.clear();
    await maxStudentsInput.fill('0');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show validation error
    await expect(page.getByText(/sĩ số tối đa phải >= 1/i)).toBeVisible();
  });

  test('should validate end date is after start date', async ({ page }) => {
    await page.goto('/courses/1/classes/new');
    await expect(page.getByText('Thêm lớp học')).toBeVisible();

    // Fill name
    await page.fill('input[name="name"]', 'Test Class');

    // Fill dates with end before start
    await page.fill('input[name="startDate"]', '2026-03-01');
    await page.fill('input[name="endDate"]', '2026-02-01');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show validation error
    await expect(page.getByText(/ngày kết thúc phải sau hoặc bằng ngày bắt đầu/i)).toBeVisible();
  });

  test('should change location type to ONLINE', async ({ page }) => {
    await page.goto('/courses/1/classes/new');
    await expect(page.getByText('Thêm lớp học')).toBeVisible();

    // Fill name
    await page.fill('input[name="name"]', 'Online Class');

    // Change location type to ONLINE
    const locationSelect = page.locator('select[name="locationType"]');
    await locationSelect.selectOption('ONLINE');
    await expect(locationSelect).toHaveValue('ONLINE');

    // Fill location detail for online
    await page.fill('input[name="locationDetail"]', 'Zoom Meeting Room');

    // Submit form
    await page.click('button[type="submit"]');

    // Should create successfully
    await expect(page.getByText(/đã tạo lớp học mới/i)).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Classes Module - View & Edit Flow', () => {
  test('should view class detail page', async ({ page }) => {
    // Navigate to course detail
    await page.goto('/courses/1');

    // Find and click on a class in the list
    const firstClass = page.locator('table tbody tr:first-child');
    await expect(firstClass).toBeVisible();
    await firstClass.click();

    // Should be on class detail page
    await expect(page).toHaveURL(/\/courses\/\d+\/classes\/\d+/);

    // Should show class details
    await expect(page.getByText(/thông tin lớp học/i)).toBeVisible();
  });

  test('should edit class information', async ({ page }) => {
    // Go to class detail page (assuming class ID 1 exists)
    await page.goto('/courses/1/classes/1');

    // Click edit button
    const editButton = page.getByRole('link', { name: /chỉnh sửa/i });
    await expect(editButton).toBeVisible();
    await editButton.click();

    // Should be on edit page
    await expect(page).toHaveURL(/\/courses\/\d+\/classes\/\d+\/edit/);

    // Change class name
    const nameInput = page.locator('input[name="name"]');
    await nameInput.clear();
    await nameInput.fill('Lớp Đã Chỉnh Sửa');

    // Change description
    const descriptionInput = page.locator('textarea[name="description"]');
    await descriptionInput.clear();
    await descriptionInput.fill('Mô tả đã được cập nhật');

    // Submit form
    await page.click('button[type="submit"]');

    // Should show success toast
    await expect(page.getByText(/đã cập nhật lớp học/i)).toBeVisible({ timeout: 5000 });

    // Should redirect back to detail page
    await expect(page).toHaveURL(/\/courses\/\d+\/classes\/\d+$/);
  });

  test('should show course context on edit page', async ({ page }) => {
    await page.goto('/courses/1/classes/1/edit');

    // Should show course name in context
    await expect(page.getByText(/chỉnh sửa lớp học/i)).toBeVisible();
    // Course name should be displayed somewhere on the page
  });
});

test.describe('Classes Module - Delete Flow', () => {
  test('should delete class with confirmation', async ({ page }) => {
    // Go to course detail with classes
    await page.goto('/courses/1');

    // Find delete button for first class
    const deleteButton = page.locator('table tbody tr:first-child button[title*="Xóa"], table tbody tr:first-child button:has-text("Xóa")').first();

    // Click delete button
    await deleteButton.click();

    // Handle confirmation dialog
    page.on('dialog', async dialog => {
      expect(dialog.message()).toContain('xóa lớp học');
      await dialog.accept();
    });

    // Should show success toast after deletion
    await expect(page.getByText(/đã xóa lớp học/i)).toBeVisible({ timeout: 5000 });
  });

  test('should cancel delete when confirmation rejected', async ({ page }) => {
    await page.goto('/courses/1');

    // Find delete button for first class
    const deleteButton = page.locator('table tbody tr:first-child button[title*="Xóa"]').first();

    // Get class name before deletion attempt
    const className = await page.locator('table tbody tr:first-child td:nth-child(2)').textContent();

    // Click delete button
    await deleteButton.click();

    // Reject confirmation dialog
    page.on('dialog', async dialog => {
      await dialog.dismiss();
    });

    // Class should still be in the list
    await expect(page.getByText(className!)).toBeVisible();
  });
});

test.describe('Classes Module - Error Handling', () => {
  test('should show error when course not found', async ({ page }) => {
    // Try to create class for non-existent course
    await page.goto('/courses/99999/classes/new');

    // Should show error message
    await expect(page.getByText(/không tìm thấy khóa học/i)).toBeVisible({ timeout: 5000 });
  });

  test('should handle API errors gracefully', async ({ page }) => {
    // Mock API error by intercepting request
    await page.route('**/api/v1/courses/*/classes', route => {
      route.fulfill({
        status: 500,
        body: JSON.stringify({
          status: 500,
          error: 'INTERNAL_SERVER_ERROR',
          message: 'Đã xảy ra lỗi từ máy chủ',
        }),
      });
    });

    await page.goto('/courses/1/classes/new');
    await page.fill('input[name="name"]', 'Test Class');
    await page.click('button[type="submit"]');

    // Should show error toast
    await expect(page.getByText(/đã xảy ra lỗi từ máy chủ/i)).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Classes Module - Loading States', () => {
  test('should show loading spinner while loading course', async ({ page }) => {
    // Slow down the network to see loading state
    await page.route('**/api/v1/courses/*', async route => {
      await new Promise(resolve => setTimeout(resolve, 1000));
      await route.continue();
    });

    await page.goto('/courses/1/classes/new');

    // Should show loading spinner
    const spinner = page.locator('[data-testid="loading-spinner"]');
    await expect(spinner).toBeVisible();

    // Eventually page should load
    await expect(page.getByText('Thêm lớp học')).toBeVisible({ timeout: 5000 });
  });

  test('should disable submit button while submitting', async ({ page }) => {
    await page.goto('/courses/1/classes/new');
    await page.fill('input[name="name"]', 'Test Class');

    const submitButton = page.locator('button[type="submit"]');

    // Click submit
    await submitButton.click();

    // Button should be disabled immediately
    await expect(submitButton).toBeDisabled();
  });
});
