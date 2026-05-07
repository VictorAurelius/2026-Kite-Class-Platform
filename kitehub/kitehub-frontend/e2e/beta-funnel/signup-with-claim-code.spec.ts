/**
 * E2E spec — Beta Funnel: Signup with claim code.
 *
 * Visitor /beta-signup?token=<claim-code> → exchange claim code → fills password +
 * accepts PDPL consent → tenant created → redirected to dashboard.
 *
 * @see GAP-404 (Wave 37 Bucket C — beta funnel E2E coverage)
 * @since Wave 37
 */

import { test, expect } from '@playwright/test';

const CLAIM_CODE = 'CLAIM-TEST-XYZ-12345';

test.describe('Beta Funnel — Signup with claim code', () => {
  test.beforeEach(async ({ page }) => {
    // Mock claim code validation
    await page.route('**/api/v1/beta-access/claim/validate*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          valid: true,
          email: 'admin@abc.vn',
          organizationName: 'Trung tâm ABC',
        }),
      });
    });

    // Mock signup endpoint
    await page.route('**/api/v1/beta-access/signup', (route) => {
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          tenantId: 'tenant-test-001',
          accessToken: 'mock-jwt-token',
          subdomain: 'abc-test',
        }),
      });
    });
  });

  test('visitor với valid claim code có thể signup', async ({ page }) => {
    await page.goto(`/beta-signup?token=${CLAIM_CODE}`);

    // Page heading
    await expect(
      page.getByRole('heading').first(),
    ).toBeVisible({ timeout: 10000 });

    // Pre-filled email visible (from claim code validation)
    await expect(page.getByText(/admin@abc\.vn/i).first()).toBeVisible({
      timeout: 10000,
    });

    // Fill password
    const passwordInputs = page.locator('input[type="password"]');
    const count = await passwordInputs.count();
    if (count >= 1) {
      await passwordInputs.first().fill('TestPassword123!');
    }
    if (count >= 2) {
      await passwordInputs.nth(1).fill('TestPassword123!');
    }

    // Accept PDPL consent (Wave 35)
    const consents = page.getByRole('checkbox');
    const consentCount = await consents.count();
    for (let i = 0; i < consentCount; i++) {
      await consents.nth(i).check();
    }

    // Submit signup
    const submit = page
      .getByRole('button', { name: /(đăng ký|signup|create|tạo)/i })
      .first();
    await submit.click();

    // Verify redirect to dashboard OR success state
    await page.waitForURL(/(dashboard|signup-success|abc-test)/, {
      timeout: 10000,
    }).catch(() => {
      // Fallback — accept success message in-page
    });

    await expect(
      page
        .getByText(/(dashboard|chào mừng|welcome|signup.*success)/i)
        .first(),
    ).toBeVisible({ timeout: 10000 });
  });

  test('invalid claim code shows error', async ({ page }) => {
    // Override mock with invalid response
    await page.route('**/api/v1/beta-access/claim/validate*', (route) => {
      route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ valid: false, error: 'INVALID_CLAIM_CODE' }),
      });
    });

    await page.goto('/beta-signup?token=INVALID-CODE');

    await expect(
      page
        .getByText(/(invalid|không hợp lệ|hết hạn|expired|error)/i)
        .first(),
    ).toBeVisible({ timeout: 10000 });
  });
});
