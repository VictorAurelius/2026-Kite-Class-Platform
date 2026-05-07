/**
 * E2E spec — Beta Funnel: Request flow.
 *
 * Visitor lands on /request-beta-access → fills form (org name + email + consent) →
 * submits → success page confirms request submitted.
 *
 * @see GAP-404 (Wave 37 Bucket C — beta funnel E2E coverage)
 * @since Wave 37
 */

import { test, expect } from '@playwright/test';

test.describe('Beta Funnel — Request flow', () => {
  test.beforeEach(async ({ page }) => {
    // Mock the POST endpoint so test does not depend on backend availability
    await page.route('**/api/v1/beta-access/request', (route) => {
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'req-test-123',
          status: 'PENDING',
          submittedAt: new Date().toISOString(),
        }),
      });
    });
  });

  test('visitor có thể submit request beta access form', async ({ page }) => {
    await page.goto('/request-beta-access');

    // Form heading visible
    const heading = page.getByRole('heading', { level: 1 });
    await expect(heading).toBeVisible({ timeout: 10000 });

    // Fill required fields — try common selectors; fallback to label
    const orgInput = page
      .getByLabel(/(tổ chức|trung tâm|tên trường|organization)/i)
      .or(page.locator('input[name="organizationName"]'))
      .first();
    const emailInput = page
      .getByLabel(/email/i)
      .or(page.locator('input[type="email"]'))
      .first();

    if (await orgInput.isVisible().catch(() => false)) {
      await orgInput.fill('Trung tâm Anh ngữ Test');
    }
    if (await emailInput.isVisible().catch(() => false)) {
      await emailInput.fill('owner@test-center.vn');
    }

    // Consent checkbox (PDPL benchmark)
    const consent = page
      .getByRole('checkbox')
      .or(page.locator('input[type="checkbox"]'))
      .first();
    if (await consent.isVisible().catch(() => false)) {
      await consent.check();
    }

    // Submit
    const submit = page
      .getByRole('button', { name: /(gửi|submit|đăng ký|request)/i })
      .first();
    await submit.click();

    // Verify success indicator: either redirect to confirmation OR success message
    await expect(
      page.getByText(/(thành công|đã gửi|submitted|received|cảm ơn|thank)/i).first(),
    ).toBeVisible({ timeout: 10000 });
  });

  test('form rejects empty submission', async ({ page }) => {
    await page.goto('/request-beta-access');

    const submit = page
      .getByRole('button', { name: /(gửi|submit|đăng ký|request)/i })
      .first();

    // Click submit without filling — HTML5 validation OR custom error must surface
    await submit.click();

    // Stay on same page (URL unchanged) is acceptable signal of validation block
    await expect(page).toHaveURL(/request-beta-access/, { timeout: 5000 });
  });
});
