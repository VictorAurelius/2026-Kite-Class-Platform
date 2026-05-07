/**
 * E2E spec — Beta Funnel: Admin approve.
 *
 * Admin logs in → /admin/beta-requests → reviews pending request → approves →
 * claim code emitted (mocked email service).
 *
 * @see GAP-404 (Wave 37 Bucket C — beta funnel E2E coverage)
 * @since Wave 37
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth } from '../utils/test-helpers';

test.describe('Beta Funnel — Admin approve', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'ADMIN');

    // Mock list endpoint — return one PENDING request
    await page.route('**/api/v1/admin/beta-requests*', (route) => {
      if (route.request().method() === 'GET') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 'req-test-001',
                organizationName: 'Trung tâm ABC',
                email: 'admin@abc.vn',
                status: 'PENDING',
                submittedAt: '2026-05-07T10:00:00Z',
              },
            ],
            totalElements: 1,
            totalPages: 1,
          }),
        });
      } else {
        route.continue();
      }
    });

    // Mock approve endpoint — return claim code
    await page.route('**/api/v1/admin/beta-requests/*/approve', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 'req-test-001',
          status: 'APPROVED',
          claimCode: 'CLAIM-TEST-XYZ-12345',
          approvedAt: new Date().toISOString(),
        }),
      });
    });
  });

  test('admin sees pending request và approve thành công', async ({ page }) => {
    await page.goto('/admin/beta-requests');

    // Page heading visible
    await expect(
      page.getByRole('heading').filter({ hasText: /(beta|request)/i }).first(),
    ).toBeVisible({ timeout: 10000 });

    // Pending request row visible
    await expect(page.getByText('Trung tâm ABC').first()).toBeVisible({
      timeout: 10000,
    });

    // Approve button — auto-confirm window.confirm/prompt if any
    page.on('dialog', (dialog) => {
      void dialog.accept('Approved by E2E test');
    });

    const approveBtn = page
      .getByRole('button', { name: /(approve|duyệt|chấp thuận)/i })
      .first();
    await approveBtn.click();

    // Verify success — claim code visible OR success toast
    await expect(
      page
        .getByText(/(approved|đã duyệt|thành công|claim code|CLAIM-)/i)
        .first(),
    ).toBeVisible({ timeout: 10000 });
  });
});
