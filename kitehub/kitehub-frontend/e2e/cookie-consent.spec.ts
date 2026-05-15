/**
 * E2E tests for PDPL cookie consent flow (GAP-558 Wave 83 Bucket E).
 *
 * Validates the end-to-end flow that closes GAP-558:
 *   1. Anonymous visit → banner shows (essential cookies only loaded)
 *   2. Click "Từ chối tất cả" → banner dismisses, no GA script tag in DOM
 *   3. Re-visit + click "Đồng ý tất cả" → banner dismisses, persistence in
 *      localStorage matches { essential: true, analytics: true, marketing: true }
 *   4. Footer exposes "Chính sách Cookie" link → navigates to /legal/cookies
 *
 * GA gating verification: in dev/test mode `NEXT_PUBLIC_GA_ID` is usually
 * unset, so we focus on the consent persistence + UI flow. When GA_ID IS set,
 * the script tag is gated behind `useConsent().analytics === true` per
 * `ConsentGatedAnalytics.tsx`.
 *
 * @since Wave 83 — GAP-558
 */

import { test, expect } from '@playwright/test';

const CONSENT_STORAGE_KEY = 'kite.consent.v1';

test.describe('Cookie consent banner — PDPL Art 11 (GAP-558)', () => {
  test.beforeEach(async ({ context }) => {
    // Wipe consent state so every test starts as a fresh anonymous visitor.
    await context.clearCookies();
    await context.addInitScript((key) => {
      try {
        window.localStorage.removeItem(key);
      } catch {
        /* ignore storage-disabled errors */
      }
    }, CONSENT_STORAGE_KEY);
  });

  test('shows the consent banner on first visit to a public page', async ({ page }) => {
    await page.goto('/');
    const banner = page.getByTestId('consent-banner');
    await expect(banner).toBeVisible();

    // 3 equally-weighted CTAs per BR-PDPL-CONSENT-002 dark-pattern guard.
    await expect(page.getByTestId('consent-reject-btn')).toBeVisible();
    await expect(page.getByTestId('consent-accept-btn')).toBeVisible();
    await expect(page.getByTestId('consent-customize-btn')).toBeVisible();
  });

  test('rejecting all stores essential-only consent + dismisses banner', async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('consent-reject-btn').click();

    // Banner dismisses
    await expect(page.getByTestId('consent-banner')).toBeHidden();

    // Persistence: only essential = true
    const stored = await page.evaluate((key) => {
      return window.localStorage.getItem(key);
    }, CONSENT_STORAGE_KEY);
    expect(stored).not.toBeNull();
    const parsed = JSON.parse(stored ?? '{}');
    expect(parsed.categories).toEqual({
      essential: true,
      analytics: false,
      marketing: false,
    });

    // GA script tag should NOT be present in the DOM (essential-only post-consent).
    // Note: in dev mode NEXT_PUBLIC_GA_ID is usually unset; this guards against
    // a future regression that mounts GA unconditionally.
    const gaScript = page.locator('script[src*="googletagmanager.com/gtag/js"]');
    await expect(gaScript).toHaveCount(0);
  });

  test('accepting all stores full consent + dismisses banner', async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('consent-accept-btn').click();

    await expect(page.getByTestId('consent-banner')).toBeHidden();

    const stored = await page.evaluate((key) => {
      return window.localStorage.getItem(key);
    }, CONSENT_STORAGE_KEY);
    expect(stored).not.toBeNull();
    const parsed = JSON.parse(stored ?? '{}');
    expect(parsed.categories).toEqual({
      essential: true,
      analytics: true,
      marketing: true,
    });
  });

  test('Footer exposes "Chính sách Cookie" link → /legal/cookies', async ({ page }) => {
    await page.goto('/');
    // Dismiss banner first so the footer is in view without consent overlay.
    await page.getByTestId('consent-reject-btn').click();
    await expect(page.getByTestId('consent-banner')).toBeHidden();

    const cookieLink = page.getByTestId('footer-cookie-policy-link');
    await expect(cookieLink).toBeVisible();
    await expect(cookieLink).toHaveAttribute('href', '/legal/cookies');

    await cookieLink.click();
    await expect(page).toHaveURL(/\/legal\/cookies$/);
    // Sanity: the destination page renders the policy heading.
    await expect(page.getByRole('heading', { name: /Chính sách Cookie/i })).toBeVisible();
  });
});
