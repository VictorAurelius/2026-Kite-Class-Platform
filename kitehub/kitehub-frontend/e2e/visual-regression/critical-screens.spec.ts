/**
 * Visual regression spec — pixel-diff baseline cho 8 critical screens.
 *
 * Threshold: maxDiffPixelRatio 0.02 (2%) — accommodate font rendering variance.
 * Baseline PNGs sinh tự động ở first run với --update-snapshots; commit dưới
 * `e2e/visual-regression/critical-screens.spec.ts-snapshots/<browser>/<screen>.png`.
 *
 * Update workflow: when intentional UI change ships, run
 *   pnpm -F kitehub-frontend exec playwright test visual-regression --update-snapshots
 * then review the regenerated PNGs in PR.
 *
 * @see GAP-405 (Wave 37 Bucket C — visual regression baseline)
 * @since Wave 37
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth } from '../utils/test-helpers';

// Stable viewport for consistent snapshots
test.use({ viewport: { width: 1280, height: 720 } });

const SNAPSHOT_OPTIONS = {
  maxDiffPixelRatio: 0.02,
  fullPage: false,
  animations: 'disabled' as const,
};

test.describe('Visual regression — critical screens (Wave 37 baseline)', () => {
  test('home page baseline', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot('home.png', SNAPSHOT_OPTIONS);
  });

  test('pricing page baseline', async ({ page }) => {
    await page.goto('/pricing');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot('pricing.png', SNAPSHOT_OPTIONS);
  });

  test('request beta access baseline', async ({ page }) => {
    await page.goto('/request-beta-access');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot(
      'request-beta-access.png',
      SNAPSHOT_OPTIONS,
    );
  });

  test('beta signup baseline', async ({ page }) => {
    await page.route('**/api/v1/beta-access/claim/validate*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          valid: true,
          email: 'preview@example.com',
          organizationName: 'Preview Center',
        }),
      }),
    );
    await page.goto('/beta-signup?token=PREVIEW-CLAIM-CODE');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot('beta-signup.png', SNAPSHOT_OPTIONS);
  });

  test('admin beta-requests baseline', async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'ADMIN');
    await page.route('**/api/v1/admin/beta-requests*', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [],
          totalElements: 0,
          totalPages: 0,
        }),
      }),
    );
    await page.goto('/admin/beta-requests');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot(
      'admin-beta-requests.png',
      SNAPSHOT_OPTIONS,
    );
  });

  test('login page baseline', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot('login.png', SNAPSHOT_OPTIONS);
  });

  test('signup page baseline', async ({ page }) => {
    await page.goto('/signup');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot('signup.png', SNAPSHOT_OPTIONS);
  });

  test('dashboard baseline (mocked owner)', async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'OWNER');
    await page.goto('/dashboard');
    await page.waitForLoadState('networkidle').catch(() => {});
    await expect(page).toHaveScreenshot('dashboard.png', SNAPSHOT_OPTIONS);
  });
});
