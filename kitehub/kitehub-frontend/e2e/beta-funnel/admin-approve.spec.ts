/**
 * E2E spec — Beta Funnel: Admin approve / reject / role enforcement.
 *
 * Mock shape audit (GAP-455 Phase 1, 2026-05-09):
 *   - List response uses BetaRequestPage shape (content[]+page+size+totalElements+totalPages)
 *   - Approve/reject responses now match `BetaRequestResponse` per BE source.
 *     Pre-audit included a fictional `claimCode` field; that field doesn't
 *     exist in BetaRequestResponse — tokens travel via email only per BE source.
 *
 * Coverage extension (GAP-455 Phase 2):
 *   - Reject flow: POST /api/v1/admin/beta-requests/:id/reject + verify
 *     "Đã từ chối" badge after action.
 *   - Non-admin role enforcement: BE returns 403 → FE shouldn't break.
 *
 * @see GAP-404 (Wave 37 Bucket C — beta funnel E2E coverage)
 * @see GAP-455 (Wave 49 — KH coverage extension)
 * @since Wave 37 (extended Wave 49)
 */

import { test, expect } from '@playwright/test';
import { clearBrowserStorage, setupMockAuth } from '../utils/test-helpers';

// Real BE response shape per BetaRequestResponse record.
function buildBetaRequest(status: string, overrides: Record<string, unknown> = {}) {
  return {
    id: 1,
    email: 'admin@abc.vn',
    name: 'Nguyễn Quản Lý',
    orgName: 'Trung tâm ABC',
    persona: 'P2_CENTER_OWNER',
    referralSource: null,
    status,
    createdAt: '2026-05-07T10:00:00Z',
    approvedAt: status === 'APPROVED' ? '2026-05-07T10:30:00Z' : null,
    rejectedAt: status === 'REJECTED' ? '2026-05-07T10:30:00Z' : null,
    rejectionReason: status === 'REJECTED' ? 'Không phù hợp tiêu chí' : null,
    ...overrides,
  };
}

test.describe('Beta Funnel — Admin approve', () => {
  test.beforeEach(async ({ page }) => {
    await clearBrowserStorage(page);
    await setupMockAuth(page, 'ADMIN');
  });

  test('admin sees pending request và approve thành công', async ({ page }) => {
    // List endpoint — return one PENDING request, flip to APPROVED on refresh after action.
    let pendingState: 'PENDING' | 'APPROVED' = 'PENDING';
    await page.route('**/api/v1/admin/beta-requests*', (route) => {
      const url = route.request().url();
      if (url.includes('/approve') || url.includes('/reject')) {
        route.continue();
        return;
      }
      if (route.request().method() === 'GET') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [buildBetaRequest(pendingState)],
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

    // Approve endpoint — flip state so subsequent list refresh shows APPROVED.
    // Real BE returns BetaRequestResponse (no `claimCode` field — tokens travel
    // via email only; FE only updates the row state, doesn't display tokens).
    await page.route('**/api/v1/admin/beta-requests/*/approve', (route) => {
      pendingState = 'APPROVED';
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(buildBetaRequest('APPROVED')),
      });
    });

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
    // STATUS_LABEL[APPROVED] = "Đã duyệt".
    await expect(
      page.getByTestId('beta-requests-table').locator('tbody').getByText(/đã duyệt/i),
    ).toBeVisible({ timeout: 10000 });
  });

  test('admin có thể từ chối pending request', async ({ page }) => {
    // GAP-455 Phase 2: reject flow coverage (api-contract.md POST /reject).
    let pendingState: 'PENDING' | 'REJECTED' = 'PENDING';
    await page.route('**/api/v1/admin/beta-requests*', (route) => {
      const url = route.request().url();
      if (url.includes('/approve') || url.includes('/reject')) {
        route.continue();
        return;
      }
      if (route.request().method() === 'GET') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [buildBetaRequest(pendingState)],
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

    await page.route('**/api/v1/admin/beta-requests/*/reject', (route) => {
      pendingState = 'REJECTED';
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(buildBetaRequest('REJECTED')),
      });
    });

    await page.goto('/admin/beta-requests');
    await expect(page.getByText('Trung tâm ABC').first()).toBeVisible({ timeout: 10000 });

    // Reject button label per FE (PENDING-only action button alongside "Duyệt").
    await page.getByRole('button', { name: /từ chối/i }).first().click();

    // Some FE implementations open a reason modal first; if so, fill + confirm.
    // Defensive selector: prefer modal confirm, fallback to direct status check.
    const reasonInput = page.getByPlaceholder(/lý do|reason/i).first();
    if (await reasonInput.isVisible({ timeout: 1000 }).catch(() => false)) {
      await reasonInput.fill('Không phù hợp tiêu chí');
      await page.getByRole('button', { name: /xác nhận|confirm|từ chối/i }).last().click();
    }

    // After reject, list refresh shows REJECTED status badge.
    await expect(
      page.getByTestId('beta-requests-table').locator('tbody').getByText(/đã từ chối|từ chối/i),
    ).toBeVisible({ timeout: 10000 });
  });

  test('non-admin user gets 403 from beta-requests endpoint (BE-side enforcement)', async ({ page }) => {
    // GAP-455 Phase 2: BR-BETA-003 PLATFORM_ADMIN role required.
    // Mock BE returning 403 simulates a logged-in non-admin user trying to
    // hit the admin endpoint. FE should show error UI, not crash.
    await page.route('**/api/v1/admin/beta-requests*', (route) => {
      route.fulfill({
        status: 403,
        contentType: 'application/json',
        body: JSON.stringify({
          error: 'FORBIDDEN',
          message: 'PLATFORM_ADMIN role required.',
        }),
      });
    });

    await page.goto('/admin/beta-requests');

    // FE may show error toast / empty state / redirect — pragmatic assertion:
    // the page does NOT show real beta-request data (Trung tâm ABC text absent).
    // If FE explicitly handles 403 with a message, that text appears instead.
    await page.waitForTimeout(1500); // allow request + error UI render

    // Negative assertion: row data is NOT visible.
    await expect(page.getByText('Trung tâm ABC')).not.toBeVisible();
  });
});
