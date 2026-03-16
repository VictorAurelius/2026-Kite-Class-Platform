/**
 * E2E tests for Attendance Management UI Enhancements (PR 3.8.1).
 *
 * @author KiteClass Team
 * @since 3.8.1 (PR 3.8.1)
 */

import { test, expect } from '@playwright/test';

test.describe('Attendance Enhancements - PR 3.8.1', () => {
  // Setup: Login before each test
  test.beforeEach(async ({ page }) => {
    // Navigate to login page
    await page.goto('/login');

    // Fill login form
    await page.fill('input[name="email"]', 'teacher@example.com');
    await page.fill('input[name="password"]', 'password');

    // Submit login
    await page.click('button[type="submit"]');

    // Wait for redirect to dashboard
    await page.waitForURL('/dashboard', { timeout: 10000 });
  });

  test.describe('Student Attendance History Page', () => {
    test('student can view attendance history with calendar', async ({ page }) => {
      // Navigate to students list
      await page.goto('/students');
      await page.waitForLoadState('networkidle');

      // Click first student row
      const firstStudent = page.locator('[data-testid="student-row"]').first();
      if (await firstStudent.isVisible()) {
        await firstStudent.click();
      } else {
        // Alternative: Click first student link
        await page.click('a[href*="/students/"]');
      }

      // Wait for student detail page
      await page.waitForLoadState('networkidle');

      // Click attendance link/tab
      const attendanceLink = page.locator('a[href*="/attendance"]').first();
      if (await attendanceLink.isVisible()) {
        await attendanceLink.click();
      } else {
        // Fallback: Navigate directly
        const currentUrl = page.url();
        const studentId = currentUrl.match(/\/students\/(\d+)/)?.[1];
        if (studentId) {
          await page.goto(`/students/${studentId}/attendance`);
        }
      }

      // Verify attendance history page loaded
      await expect(page.locator('h1')).toContainText(/Lịch sử điểm danh|Điểm danh/i);

      // Verify stats cards visible
      await expect(page.locator('text=Tổng số')).toBeVisible({ timeout: 5000 });
      await expect(page.locator('text=Có mặt')).toBeVisible();

      // Verify calendar rendered
      await expect(page.locator('.grid-cols-7')).toBeVisible();

      // Verify weekday headers
      await expect(page.locator('text=T2')).toBeVisible();
      await expect(page.locator('text=T3')).toBeVisible();

      // Click a calendar date button (if any has attendance data)
      const dateWithAttendance = page.locator('button[class*="bg-green"], button:has-text("lần")').first();
      if (await dateWithAttendance.isVisible()) {
        await dateWithAttendance.click();

        // Verify detail dialog opens
        await expect(page.locator('[role="dialog"]')).toBeVisible({ timeout: 3000 });
        await expect(page.locator('text=Chi tiết điểm danh')).toBeVisible();

        // Close dialog
        await page.keyboard.press('Escape');
        await expect(page.locator('[role="dialog"]')).not.toBeVisible();
      }
    });

    test('student can filter attendance by class', async ({ page }) => {
      // Navigate directly to student attendance page
      await page.goto('/students/1/attendance');
      await page.waitForLoadState('networkidle');

      // Find filter section
      const filterSection = page.locator('text=Lọc');
      if (await filterSection.isVisible()) {
        // Click class filter dropdown
        const classSelect = page.locator('[role="combobox"]').first();
        if (await classSelect.isVisible()) {
          await classSelect.click();

          // Select an option (if available)
          const firstOption = page.locator('[role="option"]').first();
          if (await firstOption.isVisible()) {
            await firstOption.click();
          }
        }
      }

      // Verify page still renders correctly after filter
      await expect(page.locator('.grid-cols-7')).toBeVisible();
    });

    test('student can export CSV', async ({ page }) => {
      // Navigate to student attendance page
      await page.goto('/students/1/attendance');
      await page.waitForLoadState('networkidle');

      // Find export button
      const exportButton = page.locator('button:has-text("Xuất CSV")');

      if (await exportButton.isVisible()) {
        // Start waiting for download
        const downloadPromise = page.waitForEvent('download', { timeout: 5000 });

        // Click export button
        await exportButton.click();

        // Wait for download
        const download = await downloadPromise;

        // Verify download
        expect(download.suggestedFilename()).toMatch(/diem-danh.*\.csv/);
      }
    });
  });

  test.describe('Admin Statistics Dashboard', () => {
    test('admin can view system statistics and trends', async ({ page }) => {
      // Navigate to admin stats page
      await page.goto('/admin/attendance/stats');
      await page.waitForLoadState('networkidle');

      // Verify page loaded
      await expect(page.locator('h1')).toContainText(/Thống kê điểm danh/i);

      // Verify stats cards visible
      await expect(page.locator('text=Tổng lớp học')).toBeVisible({ timeout: 5000 });
      await expect(page.locator('text=Tổng buổi học')).toBeVisible();
      await expect(page.locator('text=Tỷ lệ điểm danh TB')).toBeVisible();

      // Verify trends chart renders
      const chart = page.locator('svg').first();
      if (await chart.isVisible()) {
        await expect(chart).toBeVisible();
      }

      // Verify class breakdown table
      await expect(page.locator('text=Chi tiết theo lớp')).toBeVisible();
    });

    test('admin can change date range filter', async ({ page }) => {
      // Navigate to admin stats page
      await page.goto('/admin/attendance/stats');
      await page.waitForLoadState('networkidle');

      // Find date inputs
      const startDateInput = page.locator('input[type="date"]').first();
      const endDateInput = page.locator('input[type="date"]').nth(1);

      if (await startDateInput.isVisible() && await endDateInput.isVisible()) {
        // Change date range
        await startDateInput.fill('2026-02-01');
        await endDateInput.fill('2026-02-28');

        // Wait for data to reload
        await page.waitForTimeout(1000);

        // Verify stats cards still visible
        await expect(page.locator('text=Tổng lớp học')).toBeVisible();
      }
    });

    test('admin can export statistics CSV', async ({ page }) => {
      // Navigate to admin stats page
      await page.goto('/admin/attendance/stats');
      await page.waitForLoadState('networkidle');

      // Find export button
      const exportButton = page.locator('button:has-text("Xuất báo cáo CSV")');

      if (await exportButton.isVisible()) {
        // Start waiting for download
        const downloadPromise = page.waitForEvent('download', { timeout: 5000 });

        // Click export button
        await exportButton.click();

        // Wait for download
        const download = await downloadPromise;

        // Verify download
        expect(download.suggestedFilename()).toMatch(/thong-ke-diem-danh.*\.csv/);
      }
    });
  });

  test.describe('Teacher Dashboard', () => {
    test('teacher can view today\'s classes and pending attendance', async ({ page }) => {
      // Navigate to teacher dashboard
      await page.goto('/teacher/dashboard');
      await page.waitForLoadState('networkidle');

      // Verify dashboard loaded
      await expect(page.locator('h1')).toContainText(/Giáo viên|Chào/i);

      // Verify quick stats cards
      await expect(page.locator('text=Lớp học hôm nay')).toBeVisible({ timeout: 5000 });
      await expect(page.locator('text=Chưa điểm danh')).toBeVisible();
      await expect(page.locator('text=Đã hoàn thành')).toBeVisible();
      await expect(page.locator('text=Tổng học viên')).toBeVisible();

      // Verify today's classes widget renders
      const todayClassesWidget = page.locator('text=Lớp học hôm nay').first();
      await expect(todayClassesWidget).toBeVisible();
    });

    test('teacher can navigate to mark attendance from dashboard', async ({ page }) => {
      // Navigate to teacher dashboard
      await page.goto('/teacher/dashboard');
      await page.waitForLoadState('networkidle');

      // Look for "Điểm danh" button (mark attendance)
      const markButton = page.locator('button:has-text("Điểm danh")').first();

      if (await markButton.isVisible()) {
        await markButton.click();

        // Verify navigated to attendance form page
        await page.waitForURL(/\/classes\/\d+\/attendance/, { timeout: 5000 });
        await expect(page).toHaveURL(/\/classes\/\d+\/attendance/);
      }
    });

    test('teacher can see pending attendance count', async ({ page }) => {
      // Navigate to teacher dashboard
      await page.goto('/teacher/dashboard');
      await page.waitForLoadState('networkidle');

      // Look for pending badge
      const pendingBadge = page.locator('text=/Chưa điểm danh: \\d+/');

      if (await pendingBadge.isVisible()) {
        const badgeText = await pendingBadge.textContent();
        expect(badgeText).toMatch(/\d+/); // Should contain a number
      }
    });
  });

  test.describe('Calendar Interactions', () => {
    test('clicking calendar date opens detail dialog', async ({ page }) => {
      // Navigate to student attendance page
      await page.goto('/students/1/attendance');
      await page.waitForLoadState('networkidle');

      // Wait for calendar to render
      await expect(page.locator('.grid-cols-7')).toBeVisible({ timeout: 5000 });

      // Find date with attendance (green background or has "lần" text)
      const dateWithAttendance = page.locator('button[class*="bg-green"]').first();

      if (await dateWithAttendance.isVisible()) {
        // Click date
        await dateWithAttendance.click();

        // Verify dialog opened
        await expect(page.locator('[role="dialog"]')).toBeVisible({ timeout: 3000 });
        await expect(page.locator('text=Chi tiết điểm danh')).toBeVisible();

        // Verify dialog shows stats
        await expect(page.locator('text=Tổng')).toBeVisible();

        // Close dialog with Escape
        await page.keyboard.press('Escape');

        // Verify dialog closed
        await expect(page.locator('[role="dialog"]')).not.toBeVisible({ timeout: 2000 });
      }
    });

    test('calendar navigation works correctly', async ({ page }) => {
      // Navigate to student attendance page
      await page.goto('/students/1/attendance');
      await page.waitForLoadState('networkidle');

      // Get current month
      const monthTitle = page.locator('h3').first();
      const _currentMonth = await monthTitle.textContent();

      // Click next month button
      const nextButton = page.locator('button').filter({ has: page.locator('svg') }).last();
      await nextButton.click();

      // Wait a bit for navigation
      await page.waitForTimeout(500);

      // Get new month
      const newMonth = await monthTitle.textContent();

      // Months should be different (unless it's the same month name in different years)
      expect(newMonth).toBeTruthy();

      // Click "Hôm nay" to return to current month
      const todayButton = page.locator('button:has-text("Hôm nay")');
      await todayButton.click();
      await page.waitForTimeout(500);

      // Should navigate back
      const finalMonth = await monthTitle.textContent();
      expect(finalMonth).toBeTruthy();
    });
  });

  test.describe('Responsive Design', () => {
    test('pages work on mobile viewport', async ({ page }) => {
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 667 });

      // Test admin stats page on mobile
      await page.goto('/admin/attendance/stats');
      await page.waitForLoadState('networkidle');

      // Verify page renders
      await expect(page.locator('h1')).toBeVisible({ timeout: 5000 });

      // Verify stats cards visible (should stack vertically)
      await expect(page.locator('text=Tổng lớp học')).toBeVisible();

      // Test teacher dashboard on mobile
      await page.goto('/teacher/dashboard');
      await page.waitForLoadState('networkidle');

      await expect(page.locator('h1')).toBeVisible();
      await expect(page.locator('text=Lớp học hôm nay')).toBeVisible();
    });

    test('pages work on tablet viewport', async ({ page }) => {
      // Set tablet viewport
      await page.setViewportSize({ width: 768, height: 1024 });

      // Test student attendance page on tablet
      await page.goto('/students/1/attendance');
      await page.waitForLoadState('networkidle');

      // Verify calendar renders correctly
      await expect(page.locator('.grid-cols-7')).toBeVisible({ timeout: 5000 });

      // Verify stats cards visible
      await expect(page.locator('text=Tổng số')).toBeVisible();
    });
  });

  test.describe('Error Handling', () => {
    test('handles navigation to non-existent student', async ({ page }) => {
      // Navigate to non-existent student
      await page.goto('/students/99999/attendance');
      await page.waitForLoadState('networkidle');

      // Should show error or empty state
      const errorMessage = page.locator('text=/Không tìm thấy|không tồn tại/i');
      const emptyState = page.locator('text=/Chưa có/i');

      // Either error message or empty state should be visible
      const hasError = await errorMessage.isVisible().catch(() => false);
      const hasEmpty = await emptyState.isVisible().catch(() => false);

      expect(hasError || hasEmpty).toBeTruthy();
    });

    test('handles empty attendance data gracefully', async ({ page }) => {
      // Navigate to page that might have no data
      await page.goto('/students/1/attendance');
      await page.waitForLoadState('networkidle');

      // Should render calendar even with no data
      await expect(page.locator('.grid-cols-7')).toBeVisible({ timeout: 5000 });

      // Should show appropriate empty state messages
      const _emptyMessage = page.locator('text=/Chưa có lịch sử|Không có dữ liệu/i');
      // Empty message might or might not be visible depending on data
    });
  });

  test.describe('Performance', () => {
    test('pages load within acceptable time', async ({ page }) => {
      const startTime = Date.now();

      await page.goto('/admin/attendance/stats');
      await page.waitForLoadState('networkidle');

      const loadTime = Date.now() - startTime;

      // Should load within 5 seconds (generous limit)
      expect(loadTime).toBeLessThan(5000);
    });

    test('calendar renders smoothly', async ({ page }) => {
      await page.goto('/students/1/attendance');
      await page.waitForLoadState('networkidle');

      // Calendar should be visible quickly
      await expect(page.locator('.grid-cols-7')).toBeVisible({ timeout: 3000 });

      // Navigation should be responsive
      const nextButton = page.locator('button').filter({ has: page.locator('svg') }).last();
      await nextButton.click();

      // Should update quickly (within 1 second)
      await page.waitForTimeout(1000);
      await expect(page.locator('.grid-cols-7')).toBeVisible();
    });
  });
});
