/**
 * Teacher login → attendance → grade → report E2E flow (GAP-268b).
 *
 * Note: Wave 49 Bucket B uses inline mock data (`teacher-mock-data.ts`)
 * rather than live `/api/teacher/*` endpoints, so this spec exercises the
 * full route shell + Wave 49 mock fixtures. The mock fixture's lead class
 * is "Lớp 10A2" (not "Lớp 6A1" as the gap text suggested) — the spec
 * adapts to actual fixture data per `audit-to-gap-pipeline.md` §2.5
 * state-check.
 *
 * @since Wave 51 Bucket A
 */
import { expect, test } from '@playwright/test';
import { loginAs } from './_helpers';

test.describe('GAP-268b — Teacher attendance → grade → report', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'TEACHER');
  });

  test('happy path: dashboard → attendance → grades → reports', async ({ page }) => {
    // 1. Teacher dashboard renders with greeting + today queue.
    await page.goto('/teacher/dashboard');
    await expect(page.getByText(/Lớp hôm nay/i)).toBeVisible({ timeout: 10000 });

    // 2. Attendance overview — class list visible; pick the first class.
    await page.goto('/teacher/attendance');
    await expect(page.getByRole('heading', { name: 'Điểm danh' })).toBeVisible();
    await expect(page.getByText('Lớp dạy hôm nay')).toBeVisible();
    // Mock fixture's first class is Lớp 10A2 / Toán nâng cao.
    await expect(page.getByText(/Lớp 10A2/).first()).toBeVisible();

    // 3. Drill into the per-class roster.
    const firstClassLink = page.getByText(/Lớp 10A2/).first();
    await firstClassLink.click();
    await expect(page).toHaveURL(/\/teacher\/attendance\/.+/);

    // 4. Navigate to grades index.
    await page.goto('/teacher/grades');
    await expect(page).toHaveURL(/\/teacher\/grades/);

    // 5. Reports — verify report shell renders.
    await page.goto('/teacher/reports');
    await expect(page).toHaveURL(/\/teacher\/reports/);
  });

  test('error branch: navigating to non-existent class report does not crash', async ({
    page,
  }) => {
    // Hit a class id the fixture has no data for — page should render an
    // empty/fallback state rather than a runtime error.
    await page.goto('/teacher/reports/nonexistent-class-xyz');
    // Page should not throw; URL stays on the report route.
    await expect(page).toHaveURL(/\/teacher\/reports\/nonexistent-class-xyz/);
    // Body should still be reachable (no client-side crash).
    await expect(page.locator('body')).toBeVisible();
  });
});
