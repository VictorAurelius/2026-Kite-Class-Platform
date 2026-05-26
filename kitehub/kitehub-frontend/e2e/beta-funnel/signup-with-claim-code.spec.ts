/**
 * E2E spec — Beta Funnel: Signup with claim code.
 *
 * Visitor /beta-signup?token=<claim-code> → exchange claim code → fills password +
 * accepts PDPL consent → tenant created → redirected to dashboard.
 *
 * Mock shape audit (GAP-455 Phase 1, 2026-05-09):
 *   - validateToken response shape now matches `BetaTokenValidationResponse`
 *     record per BE source: `{valid, email, name, orgName, persona, errorCode}`.
 *   - completeBetaSignup response now matches `BetaRequestResponse` per BE source
 *     (FE's BetaSignupForm doesn't actually consume the body — it only checks
 *     201 status + sets submitted=true — but matching shape removes silent drift).
 *
 * Error-path coverage (GAP-455 Phase 2):
 *   - errorCode TOKEN_EXPIRED → distinct FE message "Liên kết kích hoạt đã hết hạn"
 *   - errorCode ALREADY_USED → distinct FE message "Liên kết đã được sử dụng"
 *   - Pre-audit only tested generic "invalid" path; FE actually branches on
 *     errorCode (BetaSignupForm.tsx:89-93) so 3 distinct messages exist.
 *
 * PDPL consent gate update (Wave beta-prep-1 Bucket A, 2026-05-26):
 *   - Bucket A added 3 granular consent checkboxes per Decree 13 Art 4
 *     (tosPrivacy required + marketing optional + analytics optional).
 *   - Submit button disabled until `acceptTosPrivacy=true` (BetaSignupForm.tsx:240).
 *   - Happy path test MUST check `#consent-tos-privacy` before clicking submit;
 *     otherwise click is a no-op and the test times out waiting for success UI.
 *   - Optional consent boxes intentionally left unchecked to verify they're truly
 *     optional (form should submit successfully when only tosPrivacy is ticked).
 *
 * @see GAP-404 (Wave 37 Bucket C — beta funnel E2E coverage)
 * @see GAP-455 (Wave 49 — KH coverage extension)
 * @see Wave beta-prep-1 Bucket A — PDPL consent gate (3 granular checkboxes)
 * @since Wave 37 (extended Wave 49, PDPL consent gate Wave beta-prep-1)
 */

import { test, expect } from '@playwright/test';

const CLAIM_CODE = 'CLAIM-TEST-XYZ-12345';

// Real BE response shape per BetaTokenValidationResponse record.
const TOKEN_VALIDATE_HAPPY = {
  valid: true,
  email: 'admin@abc.vn',
  name: 'Nguyễn Quản Lý',
  orgName: 'Trung tâm ABC',
  persona: 'P2_CENTER_OWNER',
  errorCode: null,
};

// Real BE response shape per BetaRequestResponse record (signup completion).
const SIGNUP_COMPLETE_HAPPY = {
  id: 12345,
  email: 'admin@abc.vn',
  name: 'Nguyễn Quản Lý',
  orgName: 'Trung tâm ABC',
  persona: 'P2_CENTER_OWNER',
  referralSource: null,
  status: 'SIGNED_UP',
  createdAt: '2026-05-09T10:00:00Z',
  approvedAt: '2026-05-09T11:00:00Z',
  rejectedAt: null,
  rejectionReason: null,
};

test.describe('Beta Funnel — Signup with claim code', () => {
  test.beforeEach(async ({ page }) => {
    // Default happy path: token validates + signup completes.
    // Endpoint: GET /api/v1/auth/beta-signup/validate?token=...
    await page.route('**/api/v1/auth/beta-signup/validate*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(TOKEN_VALIDATE_HAPPY),
      });
    });

    // Endpoint: POST /api/v1/auth/beta-signup
    await page.route('**/api/v1/auth/beta-signup', (route) => {
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(SIGNUP_COMPLETE_HAPPY),
      });
    });
  });

  test('visitor với valid claim code có thể signup (happy path)', async ({ page }) => {
    await page.goto(`/beta-signup?token=${CLAIM_CODE}`);

    // Page heading "Hoàn tất đăng ký Beta"
    await expect(
      page.getByRole('heading', { level: 1 }),
    ).toBeVisible({ timeout: 10000 });

    // Pre-filled email visible after token validation succeeds
    await expect(page.getByText(/admin@abc\.vn/i).first()).toBeVisible({
      timeout: 10000,
    });

    // Fill subdomain (3-50 chars, lowercase + numbers + dash per BetaSignupForm regex)
    await page.getByLabel('Subdomain').fill('abc-test');

    // Fill password (min 8 chars per BetaSignupForm validation)
    await page.getByLabel('Mật khẩu').fill('TestPassword123!');

    // PDPL consent gate (Wave beta-prep-1 Bucket A): tick required ToS+Privacy
    // checkbox to enable submit button. Marketing + analytics intentionally
    // left unchecked to verify they're truly optional per Decree 13 Art 4.
    await page.locator('#consent-tos-privacy').check();

    // Submit signup
    await page.getByRole('button', { name: /hoàn tất đăng ký/i }).click();

    // Verify success — form replaced by role="status" block "Tạo tài khoản thành công"
    await expect(
      page.getByText(/(tạo tài khoản thành công|thành công)/i).first(),
    ).toBeVisible({ timeout: 10000 });
  });

  test('TOKEN_NOT_FOUND shows generic "không hợp lệ" error', async ({ page }) => {
    // Override default validate mock with 404 + errorCode TOKEN_NOT_FOUND.
    await page.unroute('**/api/v1/auth/beta-signup/validate*');
    await page.route('**/api/v1/auth/beta-signup/validate*', (route) => {
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          valid: false,
          email: null,
          name: null,
          orgName: null,
          persona: null,
          errorCode: 'TOKEN_NOT_FOUND',
        }),
      });
    });

    await page.goto('/beta-signup?token=NEVER-ISSUED');

    // FE BetaSignupForm.tsx fallback case (errorCode != EXPIRED && != USED):
    // "Liên kết không hợp lệ."
    await expect(
      page.getByText(/liên kết không hợp lệ/i).first(),
    ).toBeVisible({ timeout: 10000 });
  });

  test('TOKEN_EXPIRED shows distinct "đã hết hạn" message', async ({ page }) => {
    // GAP-455 Phase 2: distinct error path per BetaSignupForm.tsx:89-93.
    await page.unroute('**/api/v1/auth/beta-signup/validate*');
    await page.route('**/api/v1/auth/beta-signup/validate*', (route) => {
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          valid: false,
          email: null,
          name: null,
          orgName: null,
          persona: null,
          errorCode: 'TOKEN_EXPIRED',
        }),
      });
    });

    await page.goto('/beta-signup?token=EXPIRED-TOKEN');

    // FE-specific message for TOKEN_EXPIRED case.
    await expect(
      page.getByText(/đã hết hạn|liên hệ đội ngũ/i).first(),
    ).toBeVisible({ timeout: 10000 });
  });

  test('ALREADY_USED shows distinct "đã được sử dụng" message', async ({ page }) => {
    // GAP-455 Phase 2: distinct error path per BetaSignupForm.tsx:89-93.
    await page.unroute('**/api/v1/auth/beta-signup/validate*');
    await page.route('**/api/v1/auth/beta-signup/validate*', (route) => {
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({
          valid: false,
          email: null,
          name: null,
          orgName: null,
          persona: null,
          errorCode: 'ALREADY_USED',
        }),
      });
    });

    await page.goto('/beta-signup?token=USED-TOKEN');

    // FE-specific message for ALREADY_USED case.
    await expect(
      page.getByText(/đã được sử dụng|đăng nhập trực tiếp/i).first(),
    ).toBeVisible({ timeout: 10000 });
  });
});
