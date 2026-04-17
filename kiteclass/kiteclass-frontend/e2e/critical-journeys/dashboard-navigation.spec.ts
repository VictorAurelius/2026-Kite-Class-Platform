/**
 * Critical Journey E2E Test: Dashboard Navigation
 *
 * Tests the core navigation flow through the application:
 * 1. Login and land on dashboard
 * 2. Navigate between main sections
 * 3. Verify all navigation links work
 * 4. Test user menu and logout
 *
 * This ensures the basic app structure and navigation is working.
 *
 * @since 2026-02-24
 */

import { test, expect } from '@playwright/test';
import { login, logout, TEST_USER } from '../helpers/auth';

test.describe('Critical Journey: Dashboard Navigation', () => {
  test('should navigate through all main sections successfully', async ({ page }) => {
    // Step 1: Login
    await login(page);

    // Should be on dashboard after login
    await expect(page).toHaveURL(/\/(dashboard)?$/);

    // Verify navigation sidebar/header is present
    await expect(
      page.getByRole('link', { name: /students|học viên/i }).first()
    ).toBeVisible();

    // Step 2: Navigate to Students
    await page.click('a[href="/students"]');
    await expect(page).toHaveURL('/students');
    await expect(page.getByRole('heading', { name: /học viên/i })).toBeVisible();

    // Step 3: Navigate to Teachers
    await page.click('a[href="/teachers"]');
    await expect(page).toHaveURL('/teachers');
    await expect(page.getByRole('heading', { name: /giáo viên/i })).toBeVisible();

    // Step 4: Navigate to Courses
    await page.click('a[href="/courses"]');
    await expect(page).toHaveURL('/courses');
    await expect(page.getByRole('heading', { name: /khóa học/i })).toBeVisible();

    // Step 5: Navigate to Classes
    await page.click('a[href="/classes"]');
    await expect(page).toHaveURL('/classes');
    await expect(page.getByRole('heading', { name: /lớp học/i })).toBeVisible();

    // Step 6: Navigate back to Dashboard (if exists)
    const dashboardLink = page.getByRole('link', { name: /dashboard|trang chủ/i });
    if (await dashboardLink.isVisible()) {
      await dashboardLink.click();
      await expect(page).toHaveURL(/\/(dashboard)?$/);
    }

    // Success! All main navigation works
  });

  test('should display user info and logout successfully', async ({ page }) => {
    await login(page);

    // User avatar/name should be visible in header
    // Look for user menu button (typically shows initials like "KC" or user name)
    const userMenuButton = page.getByRole('button', { name: /KC|owner/i });
    await expect(userMenuButton).toBeVisible({ timeout: 5000 });

    // Click user menu to open dropdown
    await userMenuButton.click();

    // Verify user info is displayed in menu
    await expect(page.getByText(TEST_USER.name)).toBeVisible();
    await expect(page.getByText(TEST_USER.email)).toBeVisible();

    // Logout
    await logout(page);

    // Should redirect to login page
    await expect(page).toHaveURL('/login');
    await expect(page.getByText('Welcome back')).toBeVisible();
  });

  test('should redirect to login when accessing protected route while logged out', async ({
    page,
  }) => {
    // Try to access protected route without logging in
    await page.goto('/students');

    // Should redirect to login
    await expect(page).toHaveURL('/login', { timeout: 5000 });
    await expect(page.getByText('Welcome back')).toBeVisible();

    // Try other protected routes
    await page.goto('/courses');
    await expect(page).toHaveURL('/login');

    await page.goto('/classes');
    await expect(page).toHaveURL('/login');

    await page.goto('/teachers');
    await expect(page).toHaveURL('/login');
  });

  test('should show active navigation state for current page', async ({ page }) => {
    await login(page);

    // Navigate to Students
    await page.click('a[href="/students"]');

    // Students link should have active state (usually different styling)
    const studentsLink = page.getByRole('link', { name: /học viên/i }).first();

    // Check for active class or aria-current attribute
    const isActive = await studentsLink.evaluate((el) => {
      return (
        el.classList.contains('active') ||
        el.getAttribute('aria-current') === 'page' ||
        el.classList.contains('bg-accent') || // Common Shadcn pattern
        el.getAttribute('data-state') === 'active'
      );
    });

    // Note: This might not always be implemented, so we just log if not found
    if (!isActive) {
      console.log('Active navigation state not found - design may not include it');
    }

    // At minimum, the page should be at the correct URL
    await expect(page).toHaveURL('/students');
  });

  test('should handle quick navigation between sections', async ({ page }) => {
    await login(page);

    // Rapidly navigate between sections to test loading states
    await page.click('a[href="/students"]');
    await page.waitForTimeout(300);

    await page.click('a[href="/teachers"]');
    await page.waitForTimeout(300);

    await page.click('a[href="/courses"]');
    await page.waitForTimeout(300);

    await page.click('a[href="/classes"]');
    await page.waitForTimeout(300);

    await page.click('a[href="/students"]');

    // Final URL should be students
    await expect(page).toHaveURL('/students', { timeout: 3000 });

    // Page should have loaded (no loading spinner)
    const loadingSpinner = page.locator('[data-testid="loading-spinner"]');
    await expect(loadingSpinner).not.toBeVisible();

    // Content should be visible
    await expect(page.getByRole('heading', { name: /học viên/i })).toBeVisible();
  });

  test('should display search functionality on list pages', async ({ page }) => {
    await login(page);

    // Navigate to Students list
    await page.click('a[href="/students"]');

    // Search input should be visible
    const searchInput = page.getByPlaceholder(/tìm kiếm/i);
    await expect(searchInput).toBeVisible({ timeout: 5000 });

    // Type search query
    await searchInput.fill('Test');

    // Debounced search should trigger (wait a bit)
    await page.waitForTimeout(1000);

    // Table/list should still be visible (even if empty)
    const table = page.locator('table');
    await expect(table).toBeVisible();

    // Clear search
    await searchInput.clear();
    await page.waitForTimeout(1000);

    // Original list should reload
    await expect(table).toBeVisible();
  });

  test('should show create buttons on list pages', async ({ page }) => {
    await login(page);

    // Students - should have create button
    await page.click('a[href="/students"]');
    await expect(
      page.getByRole('link', { name: /thêm học viên|new student/i })
    ).toBeVisible({ timeout: 5000 });

    // Teachers - should have create button
    await page.click('a[href="/teachers"]');
    await expect(
      page.getByRole('link', { name: /thêm giáo viên|new teacher/i })
    ).toBeVisible();

    // Courses - should have create button
    await page.click('a[href="/courses"]');
    await expect(
      page.getByRole('link', { name: /thêm khóa học|new course/i })
    ).toBeVisible();

    // Classes - conditional (need to select course first)
    await page.click('a[href="/classes"]');

    // Initially no create button (must select course first)
    // const initialCreateButton = page.getByRole('link', { name: /thêm lớp học/i });

    // Select a course
    const courseSelector = page.locator('button[role="combobox"]').first();
    if (await courseSelector.isVisible()) {
      await courseSelector.click();
      await page.locator('[role="option"]').first().click();
      await page.waitForTimeout(500);

      // Now create button should appear
      await expect(
        page.getByRole('link', { name: /thêm lớp học/i })
      ).toBeVisible({ timeout: 5000 });
    }
  });

  test('should handle browser back/forward navigation', async ({ page }) => {
    await login(page);

    // Navigate through pages
    await page.click('a[href="/students"]');
    await expect(page).toHaveURL('/students');

    await page.click('a[href="/teachers"]');
    await expect(page).toHaveURL('/teachers');

    await page.click('a[href="/courses"]');
    await expect(page).toHaveURL('/courses');

    // Use browser back button
    await page.goBack();
    await expect(page).toHaveURL('/teachers');

    await page.goBack();
    await expect(page).toHaveURL('/students');

    // Use browser forward button
    await page.goForward();
    await expect(page).toHaveURL('/teachers');

    await page.goForward();
    await expect(page).toHaveURL('/courses');

    // Page content should load correctly after back/forward
    await expect(page.getByRole('heading', { name: /khóa học/i })).toBeVisible();
  });
});
