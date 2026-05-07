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
    // Mock claim-code/token validation.
    // Endpoint: GET /api/v1/auth/beta-signup/validate?token=... per src/lib/api/endpoints.ts
    await page.route('**/api/v1/auth/beta-signup/validate*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          valid: true,
          email: 'admin@abc.vn',
          name: 'Nguyễn Quản Lý',
          orgName: 'Trung tâm ABC',
          persona: 'P2_CENTER_OWNER',
        }),
      });
    });

    // Mock complete-signup endpoint
    await page.route('**/api/v1/auth/beta-signup', (route) => {
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

    // Page heading "Hoàn tất đăng ký Beta"
    await expect(
      page.getByRole('heading', { level: 1 }),
    ).toBeVisible({ timeout: 10000 });

    // Pre-filled email visible after token validation succeeds
    await expect(page.getByText(/admin@abc\.vn/i).first()).toBeVisible({
      timeout: 10000,
    });

    // Fill subdomain (3-50 chars, lowercase + numbers + dash)
    await page.getByLabel('Subdomain').fill('abc-test');

    // Fill password (min 8 chars per BetaSignupForm validation)
    await page.getByLabel('Mật khẩu').fill('TestPassword123!');

    // Submit signup
    await page.getByRole('button', { name: /hoàn tất đăng ký/i }).click();

    // Verify success — form replaced by role="status" block "Tạo tài khoản thành công"
    await expect(
      page.getByText(/(tạo tài khoản thành công|thành công)/i).first(),
    ).toBeVisible({ timeout: 10000 });
  });

  test('invalid claim code shows error', async ({ page }) => {
    // Override mock with invalid response — must unroute before re-routing
    await page.unroute('**/api/v1/auth/beta-signup/validate*');
    await page.route('**/api/v1/auth/beta-signup/validate*', (route) => {
      route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ valid: false, errorCode: 'TOKEN_NOT_FOUND' }),
      });
    });

    await page.goto('/beta-signup?token=INVALID-CODE');

    await expect(
      page
        .getByText(/(không hợp lệ|hết hạn|đã được sử dụng)/i)
        .first(),
    ).toBeVisible({ timeout: 10000 });
  });
});
