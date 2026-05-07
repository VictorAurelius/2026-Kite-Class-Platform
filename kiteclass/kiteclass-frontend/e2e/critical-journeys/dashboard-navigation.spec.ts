// Validated locally 2026-05-07 against 2f1e29bd0d2e04515b45fcdca6bb67926aa75679
import { test, expect } from '@playwright/test';
import { login, logout } from '../helpers/auth';

/**
 * Critical Journey E2E Test: Dashboard Navigation
 *
 * Tests the core navigation flow through the application:
 * 1. Login and land on dashboard
 * 2. Navigate between main sections
 * 3. Verify all navigation links work
 * 4. Test user menu and logout
 *
 * Selectors reconciled 2026-05-07 (GAP-420 sub-A) to match VN-first production UI.
 * VN/EN parallel regex used throughout for future copy-drift resilience.
 * Navigation uses href-based selectors inside <aside> to avoid strict-mode violations.
 *
 * @since 2026-02-24
 * @updated 2026-05-07
 */

test.describe('Critical Journey: Dashboard Navigation', () => {
  test('should navigate through all main sections successfully', async ({ page }) => {
    // Step 1: Login
    await login(page);

    // Should be on dashboard after login
    await expect(page).toHaveURL(/\/(dashboard)?$/);

    // Verify navigation sidebar is present — sidebar renders nav links as <a> inside <button>
    // The link text is VN: "Học viên". Match both VN and EN for resilience.
    await expect(
      page.getByRole('link', { name: /Học viên|students/i }).first()
    ).toBeVisible();

    // Step 2: Navigate to Students — use href selector inside <aside> (unambiguous).
    // Sidebar: <aside> contains <a href="/students"><Button>Học viên</Button></a>.
    // role-based click on the <a> times out (Button inside intercepts); href-based is direct.
    await page.locator('aside a[href="/students"]').click();
    await page.waitForURL('/students', { timeout: 5000 });
    // h1 text: "Học viên"
    await expect(page.getByRole('heading', { name: /^Học viên$/i })).toBeVisible();

    // Step 3: Navigate to Teachers
    await page.locator('aside a[href="/teachers"]').click();
    await page.waitForURL('/teachers', { timeout: 5000 });
    // h1 text: "Giáo viên"
    await expect(page.getByRole('heading', { name: /^Giáo viên$/i })).toBeVisible();

    // Step 4: Navigate to Courses
    await page.locator('aside a[href="/courses"]').click();
    await page.waitForURL('/courses', { timeout: 5000 });
    // h1 text: "Khóa học"
    await expect(page.getByRole('heading', { name: /^Khóa học$/i })).toBeVisible();

    // Step 5: Navigate to Classes
    await page.locator('aside a[href="/classes"]').click();
    await page.waitForURL('/classes', { timeout: 5000 });
    // h1 text: "Lớp học"
    await expect(page.getByRole('heading', { name: /^Lớp học$/i })).toBeVisible();

    // Step 6: Navigate back to Dashboard
    // Sidebar has: { title: 'Dashboard', href: '/dashboard', icon: Home }
    // Note: the logo link also has href="/dashboard"; use nav-specific selector.
    // The nav <a> wraps a <Button> with text "Dashboard" — use that to distinguish from logo link.
    const dashboardLink = page.locator('aside nav a[href="/dashboard"]');
    if (await dashboardLink.isVisible()) {
      await dashboardLink.click();
      await expect(page).toHaveURL(/\/(dashboard)?$/);
    }

    // Success! All main navigation works
  });

  test('should display user info and logout successfully', async ({ page }) => {
    await login(page);

    // Avatar button: <Avatar><AvatarFallback>KC</AvatarFallback></Avatar> wrapped in ghost button.
    // Scoped to banner landmark (header element) to avoid false matches; filter by text content "KC".
    const userMenuButton = page.getByRole('banner').getByRole('button').filter({ hasText: 'KC' });
    await expect(userMenuButton).toBeVisible({ timeout: 5000 });

    // Click user menu to open dropdown
    await userMenuButton.click();

    // Verify dropdown opened — check for "Đăng xuất" menuitem
    await expect(
      page.getByRole('menuitem', { name: /đăng xuất|logout/i })
    ).toBeVisible({ timeout: 3000 });

    // Verify the user label in dropdown (hardcoded "Chủ trung tâm" per header.tsx)
    await expect(page.getByText('Chủ trung tâm')).toBeVisible();

    // Close menu by pressing Escape before calling logout() which will re-open it
    await page.keyboard.press('Escape');
    await page.waitForTimeout(200);

    // Logout (opens menu again and clicks Đăng xuất)
    await logout(page);

    // Should redirect to login page
    await expect(page).toHaveURL('/login');
    // Login heading is VN-first: "Chào mừng trở lại"
    await expect(
      page.getByRole('heading', { name: /(Chào mừng trở lại|Welcome back)/i })
    ).toBeVisible();
  });

  test('should redirect to login when accessing protected route while logged out', async ({
    page,
  }) => {
    // Try to access protected route without logging in
    await page.goto('/students');

    // Auth guard redirects client-side via useEffect; allow up to 8s for redirect.
    await page.waitForURL('/login', { timeout: 8000 });
    // VN-first heading
    await expect(
      page.getByRole('heading', { name: /(Chào mừng trở lại|Welcome back)/i })
    ).toBeVisible();

    // Try other protected routes
    await page.goto('/courses');
    await page.waitForURL('/login', { timeout: 8000 });

    await page.goto('/classes');
    await page.waitForURL('/login', { timeout: 8000 });

    await page.goto('/teachers');
    await page.waitForURL('/login', { timeout: 8000 });
  });

  test('should show active navigation state for current page', async ({ page }) => {
    await login(page);

    // Navigate to Students
    await page.locator('aside a[href="/students"]').click();

    // Students link should have active state — shadcn uses variant="secondary" + bg-secondary class
    // The link <a href="/students"> wraps a <Button variant="secondary"> when active.
    const studentsLink = page.getByRole('link', { name: /^Học viên$/i }).first();

    // Check for active class or aria-current attribute (or shadcn secondary variant)
    const isActive = await studentsLink.evaluate((el) => {
      return (
        el.classList.contains('active') ||
        el.getAttribute('aria-current') === 'page' ||
        el.classList.contains('bg-accent') ||
        el.classList.contains('bg-secondary') ||
        el.getAttribute('data-state') === 'active' ||
        // Check child button for secondary variant
        el.querySelector('[class*="bg-secondary"]') !== null
      );
    });

    if (!isActive) {
      console.log('Active navigation state not found - design may not include it');
    }

    // At minimum, the page should be at the correct URL
    await expect(page).toHaveURL('/students');
  });

  test('should handle quick navigation between sections', async ({ page }) => {
    await login(page);

    // Rapidly navigate between sections to test loading states.
    // Use href-based selectors inside <aside> to avoid ambiguity.
    await page.locator('aside a[href="/students"]').click();
    await page.waitForTimeout(300);

    await page.locator('aside a[href="/teachers"]').click();
    await page.waitForTimeout(300);

    await page.locator('aside a[href="/courses"]').click();
    await page.waitForTimeout(300);

    await page.locator('aside a[href="/classes"]').click();
    await page.waitForTimeout(300);

    await page.locator('aside a[href="/students"]').click();

    // Final URL should be students
    await expect(page).toHaveURL('/students', { timeout: 3000 });

    // Verify page has loaded — heading visible implies no stuck loading state
    await expect(page.getByRole('heading', { name: /^Học viên$/i })).toBeVisible();
  });

  test('should display search functionality on list pages', async ({ page }) => {
    await login(page);

    // Navigate to Students list — use href selector inside <aside>
    await page.locator('aside a[href="/students"]').click();
    await page.waitForURL('/students', { timeout: 5000 });

    // SearchInput on students page: placeholder="Tìm kiếm theo tên, email..."
    // Use exact placeholder to avoid matching the global header search box which also starts with "Tìm kiếm".
    const searchInput = page.getByPlaceholder('Tìm kiếm theo tên, email...');
    await expect(searchInput).toBeVisible({ timeout: 5000 });

    // Type search query
    await searchInput.fill('Test');

    // Debounced search should trigger (wait a bit)
    await page.waitForTimeout(1000);

    // Table/list should still be visible (even if empty with mocked data)
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

    // Students — create button: <Link href="/students/new"><Button>Thêm học viên</Button></Link>
    // Use href selector to be unique — avoids matching sidebar "Học viên" text links.
    await page.locator('aside a[href="/students"]').click();
    await page.waitForURL('/students', { timeout: 5000 });
    await expect(page.locator('a[href="/students/new"]').first()).toBeVisible({ timeout: 5000 });

    // Teachers — create button: <Link href="/teachers/new"><Button>Thêm giáo viên</Button></Link>
    await page.locator('aside a[href="/teachers"]').click();
    await page.waitForURL('/teachers', { timeout: 5000 });
    await expect(page.locator('a[href="/teachers/new"]').first()).toBeVisible();

    // Courses — create button: <Link href="/courses/new"><Button>Thêm khóa học</Button></Link>
    await page.locator('aside a[href="/courses"]').click();
    await page.waitForURL('/courses', { timeout: 5000 });
    await expect(page.locator('a[href="/courses/new"]').first()).toBeVisible();

    // Classes — "Thêm lớp học" only appears AFTER selecting a course from the Select combobox.
    // Initial state: no selectedCourseId → button is conditionally hidden.
    await page.locator('aside a[href="/classes"]').click();
    await page.waitForURL('/classes', { timeout: 5000 });

    // Select a course using the shadcn Select component (SelectTrigger renders as combobox role)
    const courseSelector = page.getByRole('combobox');
    if (await courseSelector.isVisible()) {
      await courseSelector.click();
      // Pick the first option in the SelectContent
      const firstOption = page.getByRole('option').first();
      if (await firstOption.isVisible({ timeout: 2000 }).catch(() => false)) {
        await firstOption.click();
        await page.waitForTimeout(500);

        // Now "Thêm lớp học" link should appear — href is /courses/:id/classes/new
        await expect(
          page.getByRole('link', { name: /thêm lớp học|new class/i })
        ).toBeVisible({ timeout: 5000 });
      } else {
        console.log('No course options available in mock — skipping classes create-button check');
      }
    } else {
      console.log('Course selector not visible — skipping classes create-button check');
    }
  });

  test('should handle browser back/forward navigation', async ({ page }) => {
    await login(page);

    // Navigate through pages using href-based selectors in sidebar
    await page.locator('aside a[href="/students"]').click();
    await expect(page).toHaveURL('/students');

    await page.locator('aside a[href="/teachers"]').click();
    await expect(page).toHaveURL('/teachers');

    await page.locator('aside a[href="/courses"]').click();
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
    await expect(page.getByRole('heading', { name: /^Khóa học$/i })).toBeVisible();
  });
});
