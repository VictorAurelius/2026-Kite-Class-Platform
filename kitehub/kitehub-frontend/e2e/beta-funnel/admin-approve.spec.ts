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

    // Mock list endpoint — return one PENDING request.
    // FE shape per src/app/(admin)/admin/beta-requests/page.tsx BetaRequest interface:
    // id (number), email, name, orgName, persona, referralSource, status, createdAt, ...
    let pendingState: 'PENDING' | 'APPROVED' = 'PENDING';
    await page.route('**/api/v1/admin/beta-requests*', (route) => {
      const url = route.request().url();
      // Approve action handled by separate route below; skip here
      if (url.includes('/approve') || url.includes('/reject')) {
        route.continue();
        return;
      }
      if (route.request().method() === 'GET') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 1,
                email: 'admin@abc.vn',
                name: 'Nguyễn Quản Lý',
                orgName: 'Trung tâm ABC',
                persona: 'P2_CENTER_OWNER',
                referralSource: null,
                status: pendingState,
                createdAt: '2026-05-07T10:00:00Z',
                approvedAt: pendingState === 'APPROVED' ? '2026-05-07T10:30:00Z' : null,
                rejectedAt: null,
                rejectionReason: null,
              },
            ],
            page: 0,
            size: 50,
            totalElements: 1,
            totalPages: 1,
          }),
        });
      } else {
        route.continue();
      }
    });

    // Mock approve endpoint — flip state so subsequent list refresh shows APPROVED
    await page.route('**/api/v1/admin/beta-requests/*/approve', (route) => {
      pendingState = 'APPROVED';
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: 1,
          status: 'APPROVED',
          claimCode: 'CLAIM-TEST-XYZ-12345',
          approvedAt: new Date().toISOString(),
        }),
      });
    });
  });

  test('admin sees pending request và approve thành công', async ({ page }) => {
    await page.goto('/admin/beta-requests');

    // Page heading "Yêu cầu Beta"
    await expect(
      page.getByRole('heading', { name: /yêu cầu beta/i }).first(),
    ).toBeVisible({ timeout: 10000 });

    // Pending request row visible (orgName cell)
    await expect(page.getByText('Trung tâm ABC').first()).toBeVisible({
      timeout: 10000,
    });

    // Click "Duyệt" (approve) button — only visible when status PENDING
    await page.getByRole('button', { name: 'Duyệt' }).first().click();

    // After approve, fetchData() refreshes list → row's action cell renders
    // STATUS_LABEL[APPROVED] = "Đã duyệt". Scope to table tbody to avoid the
    // status-filter <option> in dropdown (text matches but element is hidden).
    await expect(
      page.getByTestId('beta-requests-table').locator('tbody').getByText(/đã duyệt/i),
    ).toBeVisible({ timeout: 10000 });
  });
});
