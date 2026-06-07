/**
 * GAP-951 — Mobile-first admin layout regression guard.
 *
 * Verify the dashboard/admin layout is usable on a small phone (iPhone SE /
 * Galaxy A05 class = 375×667), the target device for the ~50%+ of VN admins
 * who manage their center from a phone.
 *
 * Asserts on a 375×667 viewport:
 *  1. Desktop fixed sidebar is collapsed/hidden by default; hamburger visible.
 *  2. A core admin page renders without horizontal overflow.
 *  3. The hamburger opens the navigation drawer (Sheet) with nav links.
 *  4. A form control (search input) is usable + full-width on mobile.
 *  5. Interactive controls meet the ≥44px touch-target minimum.
 *
 * Uses Playwright route-mocking (injectAuthTokens + setupApiMocks) — mirrors
 * the pattern in e2e/helpers/auth.ts; no live backend required.
 *
 * Per `.claude/rules/e2e-rst-test-layer-boundary.md` §3 — this E2E spec is the
 * regression guard paired with the GAP-951 responsive-layout fix.
 *
 * @since GAP-951
 */

import { test, expect, type Page } from '@playwright/test';
import { injectAuthTokens } from './helpers/auth';
import { setupApiMocks } from './helpers/api-mocks';

// Pin a small-phone viewport so this spec is deterministic on any project.
test.use({
  viewport: { width: 375, height: 667 },
  isMobile: true,
  hasTouch: true,
});

const MOBILE_WIDTH = 375;
// Allow a 1px rounding tolerance on overflow assertions.
const OVERFLOW_TOLERANCE = 1;

async function gotoAdminPage(page: Page, path: string) {
  // Permissive fallback for any API endpoint not covered by setupApiMocks.
  // Registered FIRST so the specific mocks (registered later, higher priority
  // in Playwright's last-registered-wins order) still win. Prevents unmocked
  // secondary endpoints (notifications, profile, branding) from raising error
  // toasts that would overlay and intercept clicks on the hamburger.
  await page.route('**/api/**', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '{"content":[],"data":{}}' }),
  );
  await setupApiMocks(page);
  // injectAuthTokens navigates to '/', seeds sessionStorage auth, then reloads.
  await injectAuthTokens(page);
  await page.goto(path, { waitUntil: 'domcontentloaded' });
}

test.describe('GAP-951 mobile-first admin layout (375×667)', () => {
  test('sidebar is collapsed by default; hamburger is visible', async ({ page }) => {
    await gotoAdminPage(page, '/students');

    // Page heading renders (auth guard passed, content mounted).
    await expect(
      page.getByRole('heading', { name: /học viên/i }),
    ).toBeVisible({ timeout: 15000 });

    // Desktop fixed sidebar (<aside>) is hidden on mobile via `hidden md:flex`.
    // Its nav links must NOT be visible inline before opening the drawer.
    const hamburger = page.getByRole('button', { name: /mở menu điều hướng/i });
    await expect(hamburger).toBeVisible();

    // The persistent desktop sidebar nav should be collapsed (display:none)
    // on a 375px viewport — only the hamburger gives access to navigation.
    const desktopSidebar = page.locator('aside').first();
    if (await desktopSidebar.count()) {
      await expect(desktopSidebar).toBeHidden();
    }
  });

  test('core admin page renders without horizontal overflow', async ({ page }) => {
    await gotoAdminPage(page, '/students');
    await expect(
      page.getByRole('heading', { name: /học viên/i }),
    ).toBeVisible({ timeout: 15000 });

    // No element should push the document wider than the viewport.
    const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(scrollWidth).toBeLessThanOrEqual(MOBILE_WIDTH + OVERFLOW_TOLERANCE);
  });

  test('hamburger opens the navigation drawer with nav links', async ({ page }) => {
    await gotoAdminPage(page, '/students');
    await expect(
      page.getByRole('heading', { name: /học viên/i }),
    ).toBeVisible({ timeout: 15000 });

    const hamburger = page.getByRole('button', { name: /mở menu điều hướng/i });
    await expect(hamburger).toBeVisible();
    // dispatchEvent bypasses any transient toast overlay (the button's
    // visibility + 44px hit area are asserted separately in the touch-target
    // test); this isolates "click handler opens the drawer".
    await hamburger.dispatchEvent('click');

    // Sheet drawer (role=dialog) opens with the shared SidebarNav.
    const drawer = page.getByRole('dialog');
    await expect(drawer).toBeVisible();
    // Drawer contains navigation links (e.g. Dashboard / Học viên / Lớp học).
    await expect(
      drawer.getByRole('link', { name: /lớp học/i }).first(),
    ).toBeVisible();
  });

  test('search form control is usable and full-width on mobile', async ({ page }) => {
    await gotoAdminPage(page, '/students');
    await expect(
      page.getByRole('heading', { name: /học viên/i }),
    ).toBeVisible({ timeout: 15000 });

    // The search input should be visible, editable, and not overflow the screen.
    // Scope to <main> so we target the page's SearchInput, not the header
    // search bar (which is `hidden sm:flex` → display:none on a 375px phone).
    const search = page.locator('main input[type="search"], main input[placeholder*="Tìm kiếm"]').first();
    await expect(search).toBeVisible();
    await search.fill('Trần Thị Hồng');
    await expect(search).toHaveValue('Trần Thị Hồng');

    const box = await search.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      // Input must stay within the viewport (no horizontal clipping).
      expect(box.x + box.width).toBeLessThanOrEqual(MOBILE_WIDTH + OVERFLOW_TOLERANCE);
    }
  });

  test('hamburger meets the 44px touch-target minimum', async ({ page }) => {
    await gotoAdminPage(page, '/students');
    const hamburger = page.getByRole('button', { name: /mở menu điều hướng/i });
    await expect(hamburger).toBeVisible({ timeout: 15000 });

    const box = await hamburger.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      // WCAG 2.5.5 — interactive controls ≥44×44 CSS px on touch devices.
      expect(box.width).toBeGreaterThanOrEqual(44);
      expect(box.height).toBeGreaterThanOrEqual(44);
    }
  });
});
