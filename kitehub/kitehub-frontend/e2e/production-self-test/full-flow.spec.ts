/**
 * E2E spec — Production self-test full flow (7 bước).
 *
 * Status: Scaffold (Wave 69). All tests `.skip`-ed by default until selectors
 *   calibrated post user manual-run per `documents/03-planning/end-user/plan-1-self-test-e2e.md`.
 *
 * Target: REAL production (`E2E_PROD_BASE_URL` + `E2E_PROD_API_BASE`).
 *   Không MSW mocks. Tạo real DB rows. Cleanup theo README §Cleanup.
 *
 * Run: pnpm exec playwright test e2e/production-self-test/ --headed
 * Skip from CI by default per `playwright.config.ts` testIgnore (Wave 69+ TODO).
 *
 * @see ../README.md
 * @see ../../../documents/03-planning/end-user/plan-1-self-test-e2e.md
 * @since Wave 69
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env['E2E_PROD_BASE_URL'] ?? 'https://kitehub.me';
const API_BASE = process.env['E2E_PROD_API_BASE'] ?? 'https://api.kitehub.me';
const ADMIN_EMAIL = process.env['E2E_PROD_ADMIN_EMAIL'] ?? 'admin@kitehub.me';
const ADMIN_PASSWORD = process.env['E2E_PROD_ADMIN_PASSWORD'] ?? '';
const TEST_RECIPIENT = process.env['E2E_PROD_TEST_RECIPIENT'] ?? '';

// Generated per-run identifier để cleanup dễ
const RUN_ID = `self-test-${Date.now()}`;
const TEST_TENANT_NAME = `test-tenant-${RUN_ID}`;

test.describe('Production self-test full flow (7 bước)', () => {
  test.beforeAll(() => {
    if (!ADMIN_PASSWORD || !TEST_RECIPIENT) {
      console.warn('[self-test] Missing env vars — ADMIN_PASSWORD or TEST_RECIPIENT');
      console.warn('[self-test] Run: pnpm exec playwright test --grep production-self-test với env set');
    }
  });

  test.skip('Bước 1 — Landing page kitehub.me load + CTA visible', async ({ page }) => {
    await page.goto(BASE_URL);
    await expect(page).toHaveTitle(/KiteHub/i);

    // TODO calibrate selector after manual-run
    const ctaButton = page.getByRole('link', { name: /request beta access|đăng ký beta|yêu cầu/i });
    await expect(ctaButton).toBeVisible();

    // Visual snapshot — opt-in later
    // await expect(page).toHaveScreenshot('landing.png');
  });

  test.skip('Bước 2 — Submit beta access request form', async ({ page }) => {
    await page.goto(`${BASE_URL}/request-beta-access`);

    // TODO calibrate form field names/selectors post manual-run
    await page.getByLabel(/email/i).fill(TEST_RECIPIENT);
    await page.getByLabel(/họ tên|name/i).fill(`Self Test ${RUN_ID}`);
    await page.getByLabel(/tổ chức|organization/i).fill(TEST_TENANT_NAME);
    // persona dropdown — TODO selector
    // source field — TODO selector

    await page.getByRole('button', { name: /submit|gửi|đăng ký/i }).click();

    // Success state
    await expect(page.getByText(/đã được ghi nhận|received|thành công/i)).toBeVisible({ timeout: 10000 });

    // Verify backend persisted — direct API call (read-only)
    const response = await page.request.get(`${API_BASE}/api/v1/admin/beta-requests?email=${encodeURIComponent(TEST_RECIPIENT)}`, {
      headers: { Authorization: `Bearer ${await getAdminToken(page)}` },
    });
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body.content?.[0]?.email).toBe(TEST_RECIPIENT);
  });

  test.skip('Bước 3 — Admin login + approve request trong dashboard', async ({ page }) => {
    // Login admin
    await page.goto(`${BASE_URL}/login`);
    await page.getByLabel(/email/i).fill(ADMIN_EMAIL);
    await page.getByLabel(/password|mật khẩu/i).fill(ADMIN_PASSWORD);
    await page.getByRole('button', { name: /login|đăng nhập/i }).click();
    await expect(page).toHaveURL(/dashboard|admin/, { timeout: 10000 });

    // Navigate admin beta-requests
    await page.goto(`${BASE_URL}/admin/beta-requests`);
    const requestRow = page.getByText(TEST_RECIPIENT);
    await expect(requestRow).toBeVisible();

    // Approve action
    await requestRow.locator('..').getByRole('button', { name: /approve|duyệt/i }).click();
    await expect(page.getByText(/đã duyệt|approved/i)).toBeVisible({ timeout: 10000 });
  });

  test.skip('Bước 4 — Invite email delivery (manual verify hoặc bypass via API)', async () => {
    // Option A: check SES send-statistics moved 1
    // Option B: fetch invite_token directly from DB qua API
    // Option C: skip (require human inbox check) — Plan 1 path C2

    test.fail(true, 'Bước 4 requires human inbox check OR DB-direct token fetch — not auto-testable end-to-end');
  });

  test.skip('Bước 5 — Signup với token → tenant provision', async ({ page }) => {
    // Token expected from Bước 4. For automation: fetch directly from DB via admin API.
    const tokenResp = await page.request.get(
      `${API_BASE}/api/v1/admin/beta-requests/by-email?email=${encodeURIComponent(TEST_RECIPIENT)}`,
      { headers: { Authorization: `Bearer ${await getAdminToken(page)}` } },
    );
    const { inviteToken } = await tokenResp.json();
    expect(inviteToken).toBeTruthy();

    await page.goto(`${BASE_URL}/beta-signup?token=${inviteToken}`);
    await page.getByLabel(/password|mật khẩu/i).fill('SelfTest@2026Strong!');
    await page.getByLabel(/confirm|xác nhận/i).fill('SelfTest@2026Strong!');
    // PDPL consent gate (Wave beta-prep-1 Bucket A): tick required ToS+Privacy
    // checkbox to enable submit button. Marketing + analytics left unchecked.
    await page.locator('#consent-tos-privacy').check();
    await page.getByRole('button', { name: /signup|đăng ký|hoàn tất/i }).click();

    // Verify auto-login or redirect to login
    await expect(page).toHaveURL(/dashboard|onboarding/, { timeout: 15000 });
  });

  test.skip('Bước 6 — Tạo lớp đầu tiên (core flow KiteClass)', async ({ page }) => {
    // Assume logged in from Bước 5 (test.describe.serial future)
    await page.goto(`${BASE_URL}/dashboard`);

    await page.getByRole('button', { name: /create class|tạo lớp/i }).click();
    await page.getByLabel(/tên lớp|class name/i).fill(`Test Class ${RUN_ID}`);
    await page.getByLabel(/môn học|subject/i).fill('Toán 10');
    await page.getByRole('button', { name: /save|lưu|tạo/i }).click();

    await expect(page.getByText(`Test Class ${RUN_ID}`)).toBeVisible();

    // Add 1 student
    await page.getByRole('button', { name: /add student|thêm học viên/i }).click();
    await page.getByLabel(/họ tên/i).fill(`Test Student ${RUN_ID}`);
    await page.getByRole('button', { name: /save|lưu/i }).click();

    // Add 1 schedule entry
    // TODO selector + date picker logic
  });

  test.skip('Bước 7 — Logout + re-login + verify data persisted', async ({ page }) => {
    await page.getByRole('button', { name: /logout|đăng xuất/i }).click();
    await expect(page).toHaveURL(/login|auth/, { timeout: 10000 });

    // Re-login với credentials từ Bước 5
    await page.getByLabel(/email/i).fill(TEST_RECIPIENT);
    await page.getByLabel(/password|mật khẩu/i).fill('SelfTest@2026Strong!');
    await page.getByRole('button', { name: /login|đăng nhập/i }).click();

    // Verify dashboard hiển thị data từ Bước 6
    await expect(page).toHaveURL(/dashboard/, { timeout: 10000 });
    await expect(page.getByText(`Test Class ${RUN_ID}`)).toBeVisible();
  });
});

/**
 * Helper — fetch admin JWT for API-direct verification calls.
 *
 * TODO: extract to fixture file when tests opt-in.
 */
async function getAdminToken(page: any): Promise<string> {
  const resp = await page.request.post(`${API_BASE}/api/v1/login`, {
    data: { email: ADMIN_EMAIL, password: ADMIN_PASSWORD },
  });
  expect(resp.ok()).toBeTruthy();
  const { token } = await resp.json();
  return token;
}
