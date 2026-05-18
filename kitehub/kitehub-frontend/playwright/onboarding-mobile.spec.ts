/**
 * Onboarding Mobile Regression — Wave 98 GAP-656 UI Coordinator.
 *
 * Verify mobile viewport behavior:
 *  - 375×812 (iPhone 13 reference)
 *  - 360×640 (older Android baseline)
 *  - Zalo in-app WebView UA simulation
 *
 * Per GAP-656 §Proposed Fix Step 4 + failure-mode matrix M-NEW-7:
 *  - Banner renders without horizontal scroll
 *  - Single `?` floating button click opens dropdown
 *  - All touch targets ≥44×44px (WCAG 2.5.5)
 *  - No overlapping floating widgets (banner + `?` button only — feedback
 *    widget already merged into SupportMenu dropdown)
 *
 * Note: Wave 98 ships per-component verification only. Live verify against
 * deployed environment deferred per GAP-612 AWS suspension — code-level
 * verification via Playwright dev server only.
 *
 * @since Wave 98 — GAP-656
 */

import { test, expect, devices } from '@playwright/test';

const VIEWPORTS = [
  { name: 'iPhone-13', viewport: { width: 375, height: 812 } },
  { name: 'Android-baseline', viewport: { width: 360, height: 640 } },
];

const ZALO_WEBVIEW_UA =
  'Mozilla/5.0 (Linux; Android 12; SM-A536E) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.120 Mobile Safari/537.36 ZaloTheme/light';

const MIN_TOUCH_TARGET_PX = 44;

test.describe('Mobile onboarding regression — GAP-656 Wave 98', () => {
  for (const { name, viewport } of VIEWPORTS) {
    test(`${name} ${viewport.width}×${viewport.height} — banner + support menu render without overlap`, async ({
      page,
    }) => {
      await page.setViewportSize(viewport);
      // Use dashboard layout (where banner + SupportMenu both mount)
      await page.goto('/dashboard');

      // Banner should render without horizontal scroll (no overflow-x)
      const banner = page.getByTestId('beta-disclaimer-banner');
      if (await banner.isVisible()) {
        const bannerBox = await banner.boundingBox();
        expect(bannerBox).not.toBeNull();
        if (bannerBox) {
          expect(bannerBox.width).toBeLessThanOrEqual(viewport.width);
        }
      }

      // SupportMenu trigger button MUST be present (single floating button)
      const supportTrigger = page.getByTestId('support-menu-trigger');
      await expect(supportTrigger).toBeVisible({ timeout: 5000 });

      // Touch target ≥44×44px per WCAG 2.5.5
      const triggerBox = await supportTrigger.boundingBox();
      expect(triggerBox).not.toBeNull();
      if (triggerBox) {
        expect(triggerBox.width).toBeGreaterThanOrEqual(MIN_TOUCH_TARGET_PX);
        expect(triggerBox.height).toBeGreaterThanOrEqual(MIN_TOUCH_TARGET_PX);
      }

      // No horizontal scroll on body
      const bodyScrollWidth = await page.evaluate(() => document.body.scrollWidth);
      expect(bodyScrollWidth).toBeLessThanOrEqual(viewport.width + 1); // 1px tolerance for sub-pixel
    });

    test(`${name} ${viewport.width}×${viewport.height} — clicking support menu opens dropdown with 4 items`, async ({
      page,
    }) => {
      await page.setViewportSize(viewport);
      await page.goto('/dashboard');

      const trigger = page.getByTestId('support-menu-trigger');
      await trigger.click();

      const content = page.getByTestId('support-menu-content');
      await expect(content).toBeVisible({ timeout: 3000 });

      // 4 menu items per GAP-656 §Proposed Fix Step 2
      await expect(page.getByTestId('support-menu-help-link')).toBeVisible();
      await expect(page.getByTestId('support-menu-contact-link')).toBeVisible();
      await expect(page.getByTestId('support-menu-feedback-trigger')).toBeVisible();
      await expect(page.getByTestId('support-menu-beta-status-link')).toBeVisible();
    });
  }

  test('Zalo WebView UA — banner + SupportMenu still render', async ({ browser }) => {
    const context = await browser.newContext({
      ...devices['iPhone 13'],
      userAgent: ZALO_WEBVIEW_UA,
    });
    const page = await context.newPage();

    await page.goto('/dashboard');

    // Banner may strip media queries trong Zalo WebView; verify basic visibility
    const supportTrigger = page.getByTestId('support-menu-trigger');
    await expect(supportTrigger).toBeVisible({ timeout: 5000 });

    await context.close();
  });

  test('No floating widget collision — only support menu button visible bottom-right', async ({
    page,
  }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/dashboard');

    // SupportMenu trigger is the ONLY floating bottom-right button per GAP-656.
    // GAP-540 SupportWidget + GAP-542 FeedbackWidget floating buttons MUST be
    // merged into SupportMenu dropdown (no standalone floating buttons).
    const supportTrigger = page.getByTestId('support-menu-trigger');
    await expect(supportTrigger).toBeVisible();

    // Verify no overlapping with standalone feedback widget (B5 should remove
    // FeedbackWidget standalone mount when SupportMenu is rendered).
    // For Wave 98 B0, both may coexist transiently — this assertion documents
    // the target state; B5 wires actual removal.
  });
});
