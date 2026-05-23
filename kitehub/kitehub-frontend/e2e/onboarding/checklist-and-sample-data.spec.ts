/**
 * E2E spec — Day-1 Onboarding Checklist + Sample data load (Wave 101 Bucket D GAP-538).
 *
 * <p>Verifies tenant onboarding flow:
 * <ol>
 *   <li>Login as tenant Owner</li>
 *   <li>Navigate to /onboarding (dashboard route)</li>
 *   <li>See 5-step VN checklist (Vietnamese labels per
 *       {@code vn-localization-audit-checklist.md} §2)</li>
 *   <li>Click IMPORT_DATA step → confirmation dialog</li>
 *   <li>Click "Bật dữ liệu mẫu" → step marks completed → percent updates</li>
 *   <li>Verify VN-friendly sample data references appear (no John Doe / Class A1)</li>
 * </ol>
 *
 * <p>Tests run against MSW-stubbed BE for CI determinism; live verify against real BE
 * blocked on GAP-612 (AWS account suspended) — track in GAP-538 PARTIAL exit ramp.</p>
 *
 * @see GAP-538 (onboarding checklist + sample/demo data seed)
 * @see VietnamSampleDataGenerator (kitehub-platform Wave 98 B2 seed worker)
 * @since Wave 101 Bucket D
 */

import { test, expect } from '@playwright/test';

const ONBOARDING_HAPPY = {
  tenantId: '00000000-0000-0000-0000-000000000001',
  completionPercent: 0,
  totalSteps: 5,
  completedSteps: 0,
  lastUpdatedAt: '2026-05-19T10:00:00Z',
  steps: [
    { stepId: 'PROFILE_SETUP', completed: false, completedAt: null },
    { stepId: 'INVITE_TEAM', completed: false, completedAt: null },
    { stepId: 'IMPORT_DATA', completed: false, completedAt: null },
    { stepId: 'CREATE_FIRST_CLASS', completed: false, completedAt: null },
    { stepId: 'EXPLORE_FEATURES', completed: false, completedAt: null },
  ],
};

const ONBOARDING_AFTER_IMPORT = {
  ...ONBOARDING_HAPPY,
  completionPercent: 20,
  completedSteps: 1,
  steps: ONBOARDING_HAPPY.steps.map((s) =>
    s.stepId === 'IMPORT_DATA'
      ? { ...s, completed: true, completedAt: '2026-05-19T10:01:00Z' }
      : s,
  ),
};

test.describe('Onboarding checklist + sample data', () => {
  test.beforeEach(async ({ page }) => {
    // Auth stub: assume tenant Owner already logged in.
    await page.addInitScript(() => {
      sessionStorage.setItem('accessToken', 'test-jwt-owner');
    });

    // Stub GET /api/v1/onboarding-progress.
    await page.route('**/api/v1/onboarding-progress', async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(ONBOARDING_HAPPY),
        });
        return;
      }
      // PUT — return updated state.
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(ONBOARDING_AFTER_IMPORT),
      });
    });
  });

  test('renders 5-step checklist with Vietnamese labels', async ({ page }) => {
    await page.goto('/onboarding');

    // Verify Vietnamese heading per vn-localization-audit-checklist.md §2 Section 2.
    await expect(page.getByRole('heading', { name: /Bắt đầu với KiteHub/i })).toBeVisible();

    // 5 steps render in canonical order.
    const items = page.locator('li[data-step-id]');
    await expect(items).toHaveCount(5);
    await expect(items.nth(0)).toHaveAttribute('data-step-id', 'PROFILE_SETUP');
    await expect(items.nth(2)).toHaveAttribute('data-step-id', 'IMPORT_DATA');

    // VN label present (no English "Setup Profile" / "Import Data").
    // Wave 105 Bucket B: title was reframed to dual-mode for Owner persona —
    // "Hoàn tất hồ sơ trung tâm" (was "tenant") + "Nhập danh sách học viên"
    // (was "Nhập dữ liệu mẫu"; agent walk renames per onboarding.ts:68).
    await expect(page.getByText(/Hoàn tất hồ sơ trung tâm/)).toBeVisible();
    await expect(page.getByText(/Nhập danh sách học viên/)).toBeVisible();
  });

  test('shows confirmation dialog when toggling IMPORT_DATA', async ({ page }) => {
    await page.goto('/onboarding');

    // Click IMPORT_DATA toggle.
    const importDataStep = page.locator('li[data-step-id="IMPORT_DATA"]');
    await importDataStep.getByRole('button').click();

    // Dialog appears with VN-narrative consent text.
    const dialog = page.getByTestId('onboarding-demo-confirm');
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText(/Bật dữ liệu mẫu cho tài khoản/i)).toBeVisible();
    await expect(dialog.getByText(/lớp học, học viên, lịch học/)).toBeVisible();

    // Confirm CTA "Bật dữ liệu mẫu" (Vietnamese).
    await expect(page.getByTestId('onboarding-demo-confirm-cta')).toHaveText(/Bật dữ liệu mẫu/);
  });

  test('completes IMPORT_DATA after opt-in confirmation', async ({ page }) => {
    await page.goto('/onboarding');

    await page.locator('li[data-step-id="IMPORT_DATA"]').getByRole('button').click();
    await page.getByTestId('onboarding-demo-confirm-cta').click();

    // Verify percent updates 0% → 20%.
    await expect(page.getByTestId('onboarding-progress-percent')).toHaveText('20%');
  });

  test('IMPORT_DATA step uses Vietnamese description (no English placeholder)', async ({ page }) => {
    await page.goto('/onboarding');

    const importStep = page.locator('li[data-step-id="IMPORT_DATA"]');
    const text = await importStep.textContent();

    // Per vn-localization-audit-checklist.md §3: no English placeholder data.
    expect(text).not.toMatch(/John Doe|Jane Doe|Example Center|Class A1/i);
    expect(text).toMatch(/dữ liệu mẫu/);
  });
});
