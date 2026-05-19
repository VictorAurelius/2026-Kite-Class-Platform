/**
 * E2E Tests for GAP-287 — Branding Wizard "Sử dụng mặc định" escape ramp.
 *
 * Per AC-ONBOARD-002 (P1 Solo Teacher persona review):
 *   solo teacher PHẢI có khả năng skip wizard và vào dashboard với theme mặc định.
 *
 * Tests defensively skip nếu wizard không visible (tier-gated / feature flag) — pattern
 * inherited từ `branding.spec.ts`.
 *
 * @since GAP-287 / Wave 101 Bucket C
 */

import { test, expect } from '@playwright/test';
import { login } from './helpers/auth';

test.describe('Branding Wizard — Sử dụng mặc định (GAP-287)', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await page.goto('/branding/wizard');
    await page.waitForLoadState('networkidle');
  });

  test('escape button visible từ step Logo (step 2)', async ({ page }) => {
    // Step 1 (welcome) — chọn segment để unlock NEXT
    const segmentK12 = page.getByRole('radio', { name: /k-?12/i });
    const hasWizard = await segmentK12.isVisible().catch(() => false);

    if (!hasWizard) {
      // Wizard không khả dụng (tier gated / feature flag off) — skip test
      test.skip();
      return;
    }

    await segmentK12.click();
    await page.getByRole('button', { name: /tiếp tục/i }).first().click();

    // Step 2 (logo) — "Sử dụng mặc định" button PHẢI visible
    const skipButton = page.getByTestId('use-defaults-button');
    await expect(skipButton).toBeVisible();
    await expect(skipButton).toContainText(/Sử dụng mặc định/i);
  });

  test('click "Sử dụng mặc định" → wizard advances tới submitting', async ({
    page,
  }) => {
    const segmentK12 = page.getByRole('radio', { name: /k-?12/i });
    const hasWizard = await segmentK12.isVisible().catch(() => false);

    if (!hasWizard) {
      test.skip();
      return;
    }

    // Welcome → Logo
    await segmentK12.click();
    await page.getByRole('button', { name: /tiếp tục/i }).first().click();

    // Logo → click "Sử dụng mặc định" thay vì điền step 2-5
    const skipButton = page.getByTestId('use-defaults-button');
    await expect(skipButton).toBeVisible();
    await skipButton.click();

    // Verify wizard transitions to submitting state (loading indicator)
    const submittingMarker = page.getByText(/Đang gửi tới pipeline AI/i);
    await expect(submittingMarker).toBeVisible({ timeout: 5000 });
  });

  test('escape button visible ở mọi step 2-5 (Logo / Audience / Tone / Template)', async ({
    page,
  }) => {
    const segmentCenter = page.getByRole('radio', {
      name: /trung tâm giáo dục/i,
    });
    const hasWizard = await segmentCenter.isVisible().catch(() => false);

    if (!hasWizard) {
      test.skip();
      return;
    }

    // Sample: Trung tâm Anh ngữ Sky Education — VN sample data
    await segmentCenter.click();
    await page.getByRole('button', { name: /tiếp tục/i }).first().click();

    // Step 2 (logo) — escape visible
    await expect(page.getByTestId('use-defaults-button')).toBeVisible();
    await page.getByRole('button', { name: /tiếp tục/i }).first().click();

    // Step 3 (audience) — escape vẫn visible
    await expect(page.getByTestId('use-defaults-button')).toBeVisible();
    // Pick audience để unlock NEXT
    await page.getByText(/phụ huynh/i).first().click();
    await page.getByRole('button', { name: /tiếp tục/i }).first().click();

    // Step 4 (tone) — escape vẫn visible
    await expect(page.getByTestId('use-defaults-button')).toBeVisible();
    // Pick tone để unlock NEXT
    await page.getByText(/chuyên nghiệp/i).first().click();
    await page.getByRole('button', { name: /tiếp tục/i }).first().click();

    // Step 5 (template) — escape vẫn visible
    await expect(page.getByTestId('use-defaults-button')).toBeVisible();
  });
});
