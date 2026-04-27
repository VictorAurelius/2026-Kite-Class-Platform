/**
 * Captures screenshots of the AI Branding wizard demo flow walking through
 * the FrontendInstance lifecycle states. Designed to run against a local
 * stack with kiteclass-core on dev profile (BrandingDataSeeder seeded) and
 * the FE wizard wired to the MSW handlers (or the real BE — both honor the
 * same endpoints).
 *
 * <p>Manual run only (excluded from default CI):
 *   pnpm exec playwright test e2e/ai-branding-demo.spec.ts
 *
 * <p>Tracking: GAP-235 Sub-PR G.
 */

import { expect, test } from '@playwright/test';
import path from 'path';

const SHOT_DIR = path.join(__dirname, 'screenshots', 'ai-branding');
const FAST_MODE = process.env.NEXT_PUBLIC_MOCK_DELAY_MS === '0';

test.describe('AI Branding lifecycle demo capture', () => {
  test.skip(
    !process.env.AI_BRANDING_DEMO_RUN,
    'Set AI_BRANDING_DEMO_RUN=1 to run; manual capture, not part of default CI run',
  );

  test.use({
    viewport: { width: 1440, height: 900 },
  });

  test('captures NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED → REGENERATING → FAILED', async ({ page }) => {
    // 1. NOT_STARTED — wizard landing, freshly-created instance
    await page.goto('/branding/wizard?demoStartFresh=1');
    await expect(page.getByText(/Tạo thương hiệu|Create branding/i)).toBeVisible();
    await page.screenshot({ path: path.join(SHOT_DIR, '01-not-started.png'), fullPage: true });

    // 2. INITIALIZING — kicked by infrastructure-ready POST
    await page.getByRole('button', { name: /Deploy|Triển khai/i }).click();
    await expect(page.getByText(/INITIALIZING|Đang khởi tạo/i)).toBeVisible({ timeout: 5_000 });
    await page.screenshot({ path: path.join(SHOT_DIR, '02-initializing.png'), fullPage: true });

    // 3. GENERATING — after the 1.5s delayed transition (or immediate if FAST_MODE=1)
    await expect(page.getByText(/GENERATING|Đang tạo/i)).toBeVisible({
      timeout: FAST_MODE ? 1_000 : 5_000,
    });
    await page.screenshot({ path: path.join(SHOT_DIR, '03-generating.png'), fullPage: true });

    // 4. DEPLOYED — branding-completed gate passes at score 85
    await expect(page.getByText(/DEPLOYED|Đã triển khai/i)).toBeVisible({ timeout: 10_000 });
    await page.screenshot({ path: path.join(SHOT_DIR, '04-deployed.png'), fullPage: true });

    // 5. REGENERATING — rebrand kicks the loop
    await page.getByRole('button', { name: /Regenerate|Tạo lại/i }).click();
    await expect(page.getByText(/REGENERATING|Đang tạo lại/i)).toBeVisible({ timeout: 5_000 });
    await page.screenshot({ path: path.join(SHOT_DIR, '05-regenerating.png'), fullPage: true });

    // 6. FAILED — explicit failure path; useful for retry UX
    await page.evaluate(async () => {
      await fetch('/api/v1/instances/2/failed', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: 'Demo capture: forced failure for screenshot' }),
      });
    });
    await page.reload();
    await expect(page.getByText(/FAILED|Thất bại/i)).toBeVisible({ timeout: 5_000 });
    await page.screenshot({ path: path.join(SHOT_DIR, '06-failed.png'), fullPage: true });
  });
});
