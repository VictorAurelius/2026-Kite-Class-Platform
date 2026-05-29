/**
 * GAP-758 — KC persona route-restrict regression guard.
 *
 * Verify Owner JWT (KH PlatformRole = OWNER) bị bounce khỏi KC persona portal
 * routes `(teacher)/*` + `(dashboard)/parent/*` + `(dashboard)/student/*`
 * vì userType undefined (KH login response shape không có userType field —
 * sibling architectural GAP-725 Phase 2).
 *
 * Per `.claude/rules/e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion
 * mandate: UI exposure audit 2026-05-27 → fix GAP-758 Option A → pair với
 * E2E spec same PR để prevent regression.
 *
 * Tiền điều kiện: owner.test@test.vn / Test@1234 đã seeded (Wave 105).
 *
 * @since Wave 758 — GAP-758
 */
import { test, expect } from '@playwright/test';

const KC_BASE = 'http://localhost:3000';
const SCREENS = '/tmp/rst-screenshots/gap-758';
const OWNER_EMAIL = 'owner.test@test.vn';
const OWNER_PASS = 'Test@1234';

async function loginAsOwner(page: import('@playwright/test').Page) {
  await page.goto(`${KC_BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1000);
  await page.locator('input[type="email"]').first().fill(OWNER_EMAIL);
  await page.locator('input[type="password"]').first().fill(OWNER_PASS);
  await page.locator('button[type="submit"]').first().click();
  // Wait for redirect away from /login (Owner lands at /dashboard)
  await page.waitForURL(url => !url.toString().endsWith('/login'), { timeout: 10000 });
}

test.describe('GAP-758 KC persona route-restrict', () => {
  test('Owner accessing /teacher → bounce /dashboard', async ({ page }) => {
    test.setTimeout(45000);
    await loginAsOwner(page);

    await page.goto(`${KC_BASE}/teacher`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENS}/teacher-bounce.png`, fullPage: true });

    const finalUrl = page.url();
    console.log('GAP-758 /teacher final URL:', finalUrl);
    // Should bounce to /dashboard (not stay on /teacher rendering mock data)
    expect(finalUrl).not.toContain('/teacher');
    expect(finalUrl).toContain('/dashboard');
  });

  test('Owner accessing /parent → bounce /dashboard', async ({ page }) => {
    test.setTimeout(45000);
    await loginAsOwner(page);

    await page.goto(`${KC_BASE}/parent`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENS}/parent-bounce.png`, fullPage: true });

    const finalUrl = page.url();
    console.log('GAP-758 /parent final URL:', finalUrl);
    expect(finalUrl).not.toContain('/parent');
    expect(finalUrl).toContain('/dashboard');
  });

  test('Owner accessing /parent/billing → bounce /dashboard (sibling route guard)', async ({ page }) => {
    test.setTimeout(45000);
    await loginAsOwner(page);

    await page.goto(`${KC_BASE}/parent/billing`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENS}/parent-billing-bounce.png`, fullPage: true });

    const finalUrl = page.url();
    console.log('GAP-758 /parent/billing final URL:', finalUrl);
    // Sibling parent route — new layout.tsx covers entire /parent/* tree
    expect(finalUrl).not.toContain('/parent');
    expect(finalUrl).toContain('/dashboard');
  });

  test('Owner accessing /student → bounce /dashboard', async ({ page }) => {
    test.setTimeout(45000);
    await loginAsOwner(page);

    await page.goto(`${KC_BASE}/student`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENS}/student-bounce.png`, fullPage: true });

    const finalUrl = page.url();
    console.log('GAP-758 /student final URL:', finalUrl);
    expect(finalUrl).not.toContain('/student');
    expect(finalUrl).toContain('/dashboard');
  });

  test('Owner accessing /dashboard → STAYS (positive control)', async ({ page }) => {
    test.setTimeout(45000);
    await loginAsOwner(page);

    await page.goto(`${KC_BASE}/dashboard`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    const finalUrl = page.url();
    console.log('GAP-758 /dashboard positive control URL:', finalUrl);
    // Owner-side dashboard MUST stay accessible (Owner legitimate persona)
    expect(finalUrl).toContain('/dashboard');
  });
});
