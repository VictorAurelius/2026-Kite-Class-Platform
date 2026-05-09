/**
 * E2E spec — Beta Funnel: Request flow.
 *
 * Visitor lands on /request-beta-access → fills form (org name + email + consent) →
 * submits → success page confirms request submitted.
 *
 * Mock shape audit (GAP-455 Phase 1, 2026-05-09):
 *   - Happy path response now matches `BetaRequestResponse` record (Long id, full
 *     fields) per `kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/BetaRequestResponse.java`.
 *   - Pre-audit shape used `id: 'req-test-123'` (String) which conflicted with
 *     `admin-approve.spec.ts` (Long) and didn't match BE source.
 *
 * Error-branch coverage (GAP-455 Phase 2):
 *   - 409 BETA_DUPLICATE_EMAIL — verify FE handles graceful (generic error message)
 *   - 429 RATE_LIMITED — verify FE handles graceful
 *   - 400 BETA_HONEYPOT_FILLED — verify FE handles graceful (would silently
 *     fail in real prod; surfaced here for test signal value)
 *
 * @see GAP-404 (Wave 37 Bucket C — beta funnel E2E coverage)
 * @see GAP-455 (Wave 49 — KH coverage extension)
 * @since Wave 37 (extended Wave 49)
 */

import { test, expect } from '@playwright/test';

// Real BE response shape per BetaRequestResponse record.
const BETA_REQUEST_RESPONSE_HAPPY = {
  id: 12345,
  email: 'owner@test-center.vn',
  name: 'Nguyễn Test',
  orgName: 'Trung tâm Anh ngữ Test',
  persona: 'P2_CENTER_OWNER',
  referralSource: null,
  status: 'PENDING',
  createdAt: '2026-05-09T10:00:00Z',
  approvedAt: null,
  rejectedAt: null,
  rejectionReason: null,
};

test.describe('Beta Funnel — Request flow', () => {
  test('visitor có thể submit request beta access form (happy path 201)', async ({ page }) => {
    await page.route('**/api/v1/auth/request-beta-access', (route) => {
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(BETA_REQUEST_RESPONSE_HAPPY),
      });
    });

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

  test('form rejects empty submission (BR-BETA-001 FE consent gate)', async ({ page }) => {
    await page.goto('/request-beta-access');

    // Submit button must be disabled until PDPL consent checked (Wave 35 GAP-385).
    // Disabled state IS the rejection of empty submission.
    const submit = page.getByTestId('beta-submit');
    await expect(submit).toBeDisabled();
    await expect(page).toHaveURL(/request-beta-access/, { timeout: 5000 });
  });

  test('FE handles BE 409 BETA_DUPLICATE_EMAIL gracefully', async ({ page }) => {
    // GAP-455 Phase 2: BR-BETA-002 BE-side rejection coverage.
    await page.route('**/api/v1/auth/request-beta-access', (route) => {
      route.fulfill({
        status: 409,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'BETA_DUPLICATE_EMAIL',
          message: 'An active beta request already exists for this email.',
        }),
      });
    });

    await page.goto('/request-beta-access');
    await page.getByLabel('Email').fill('duplicate@example.edu.vn');
    await page.getByLabel('Họ và tên').fill('Duplicate User');
    await page.getByLabel('Tên tổ chức / trung tâm').fill('Trung tâm Test');
    await page.getByTestId('beta-consent-checkbox').check();
    await page.getByTestId('beta-submit').click();

    // FE shows generic graceful error per BetaRequestForm.tsx catch handler.
    // Note: FE doesn't differentiate BE error codes (out of scope GAP-456 if needed).
    await expect(
      page.getByText(/(thất bại|gửi yêu cầu|thử lại)/i).first(),
    ).toBeVisible({ timeout: 10000 });

    // Verify NOT navigated to success state.
    await expect(page).toHaveURL(/request-beta-access/, { timeout: 5000 });
  });

  test('FE handles BE 429 RATE_LIMITED gracefully', async ({ page }) => {
    // GAP-455 Phase 2: per-IP rate limit coverage (api-contract.md 429).
    await page.route('**/api/v1/auth/request-beta-access', (route) => {
      route.fulfill({
        status: 429,
        contentType: 'application/json',
        headers: { 'Retry-After': '60' },
        body: JSON.stringify({
          error: 'RATE_LIMITED',
          message: 'Too many requests. Please retry after 60 seconds.',
        }),
      });
    });

    await page.goto('/request-beta-access');
    await page.getByLabel('Email').fill('ratelimit@example.com');
    await page.getByLabel('Họ và tên').fill('Rate Limited');
    await page.getByLabel('Tên tổ chức / trung tâm').fill('Trung tâm Test');
    await page.getByTestId('beta-consent-checkbox').check();
    await page.getByTestId('beta-submit').click();

    await expect(
      page.getByText(/(thất bại|gửi yêu cầu|thử lại)/i).first(),
    ).toBeVisible({ timeout: 10000 });
    await expect(page).toHaveURL(/request-beta-access/, { timeout: 5000 });
  });

  test('FE handles BE 400 BETA_HONEYPOT_FILLED gracefully', async ({ page }) => {
    // GAP-455 Phase 2: bot honeypot coverage (rules.md BR-BETA mentions silent
    // reject; api-contract surfaces it for tests). Real bots fill the hidden
    // field; legitimate users never trigger this. FE handler treats all 400/409
    // identically → generic error.
    await page.route('**/api/v1/auth/request-beta-access', (route) => {
      route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'BETA_HONEYPOT_FILLED',
          message: 'Honeypot must be empty.',
        }),
      });
    });

    await page.goto('/request-beta-access');
    await page.getByLabel('Email').fill('bot@example.com');
    await page.getByLabel('Họ và tên').fill('Bot User');
    await page.getByLabel('Tên tổ chức / trung tâm').fill('Bot Corp');
    await page.getByTestId('beta-consent-checkbox').check();
    await page.getByTestId('beta-submit').click();

    await expect(
      page.getByText(/(thất bại|gửi yêu cầu|thử lại)/i).first(),
    ).toBeVisible({ timeout: 10000 });
    await expect(page).toHaveURL(/request-beta-access/, { timeout: 5000 });
  });
});
