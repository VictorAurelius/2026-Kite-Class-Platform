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
    // Mock the POST endpoint so test does not depend on backend availability.
    // Endpoint: POST /api/v1/auth/request-beta-access (per src/lib/api/endpoints.ts)
    await page.route('**/api/v1/auth/request-beta-access', (route) => {
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

    // Form heading visible (h1 "Đăng ký dùng thử Beta")
    const heading = page.getByRole('heading', { level: 1 });
    await expect(heading).toBeVisible({ timeout: 10000 });

    // Fill required fields per BetaRequestForm component
    await page.getByLabel('Email').fill('owner@test-center.vn');
    await page.getByLabel('Họ và tên').fill('Nguyễn Test');
    await page.getByLabel('Tên tổ chức / trung tâm').fill('Trung tâm Anh ngữ Test');

    // PDPL consent checkbox required to enable submit (Wave 35 GAP-385)
    await page.getByTestId('beta-consent-checkbox').check();

    // Submit
    await page.getByTestId('beta-submit').click();

    // Verify success: form replaced by role="status" with heading "Đã nhận yêu cầu beta"
    await expect(
      page.getByText(/(đã nhận|thành công|submitted|received)/i).first(),
    ).toBeVisible({ timeout: 10000 });
  });

  test('form rejects empty submission', async ({ page }) => {
    await page.goto('/request-beta-access');

    // Submit button must be disabled until PDPL consent checked (Wave 35 GAP-385).
    // Disabled state IS the rejection of empty submission.
    const submit = page.getByTestId('beta-submit');
    await expect(submit).toBeDisabled();
    await expect(page).toHaveURL(/request-beta-access/, { timeout: 5000 });
  });
});
