/**
 * RST Wave 107 Mảng B-onboard — Chủ trung tâm (owner onboard)
 *
 * 4 luồng walk-through KC frontend tại localhost:3000:
 *  - B1: Login từ đầu (smoke; Wave 105 đã sửa 5 bug đăng nhập)
 *  - B2: Branding wizard initial setup (cài đặt thương hiệu)
 *  - B3: Tenant picker (skip nếu chỉ 1 tenant — owner.test = 1 tenant seeded)
 *  - B4: Dashboard nav probe — click qua sidebar items, không 404 / không blank
 *
 * Pattern theo `_rst-kc.spec.ts` Wave 105 baseline.
 *
 * Tiền điều kiện: owner.test@test.vn / Test@1234 đã seeded (Wave 105).
 *
 * @since Wave 107
 */
import { test } from '@playwright/test';

const KC_BASE = 'http://localhost:3000';
const SCREENS = '/tmp/rst-screenshots/wave-107';
const OWNER_EMAIL = 'owner.test@test.vn';
const OWNER_PASS = 'Test@1234';

test('RST-B1: KC owner login smoke', async ({ page }) => {
  test.setTimeout(45000);

  await page.goto(`${KC_BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${SCREENS}/b1-1-login-form.png`, fullPage: true });

  await page.locator('input[type="email"]').first().fill(OWNER_EMAIL);
  await page.locator('input[type="password"]').first().fill(OWNER_PASS);

  const loginRespPromise = page
    .waitForResponse(r => r.url().includes('/auth/login'), { timeout: 10000 })
    .catch(() => null);
  await page.locator('button[type="submit"]').first().click();
  const loginResp = await loginRespPromise;
  console.log('B1_LOGIN_STATUS:', loginResp ? loginResp.status() : 'timeout/no-net');

  await page.waitForTimeout(2500);
  await page.screenshot({ path: `${SCREENS}/b1-2-post-login.png`, fullPage: true });
  console.log('B1_POST_LOGIN_URL:', page.url());
  console.log('B1_VERDICT:', page.url().endsWith('/login') ? 'STILL_ON_LOGIN_PAGE' : 'REDIRECTED_OK');
});

test('RST-B2: KC owner /branding wizard initial setup', async ({ page }) => {
  test.setTimeout(60000);

  // Re-login (each test gets fresh context)
  await page.goto(`${KC_BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[type="email"]').first().fill(OWNER_EMAIL);
  await page.locator('input[type="password"]').first().fill(OWNER_PASS);
  await page.locator('button[type="submit"]').first().click();
  await page.waitForTimeout(2500);

  // Try /branding/wizard (initial setup wizard)
  await page.goto(`${KC_BASE}/branding/wizard`, { waitUntil: 'domcontentloaded' }).catch(() => {});
  await page.waitForTimeout(2000);
  await page.screenshot({ path: `${SCREENS}/b2-1-branding-wizard.png`, fullPage: true });

  const wizardUrl = page.url();
  const h1 = await page.locator('h1').first().innerText().catch(() => 'no-h1');
  const bodyText = await page.locator('body').innerText().catch(() => '');
  console.log('B2_URL:', wizardUrl);
  console.log('B2_H1:', h1);
  console.log('B2_BODY_SAMPLE:', bodyText.substring(0, 400).replace(/\n+/g, ' | '));

  // Wizard step inputs visible? (graceful — branding wizard may render blank do hardcoded
  // tenantId='current-tenant' không match AuthContext real tenant — RST finding Wave 107)
  const inputCount = await page
    .locator('input, textarea, select')
    .count()
    .catch(() => -1);
  console.log('B2_INPUT_COUNT:', inputCount);

  const verdict =
    bodyText.trim().length < 50
      ? 'BLANK_RENDER_BUG (wizard component empty — likely hardcoded tenantId scaffold mismatch)'
      : inputCount > 0
        ? 'WIZARD_RENDERED_WITH_INPUTS'
        : 'WIZARD_RENDERED_NO_INPUTS';
  console.log('B2_VERDICT:', verdict);
});

test('RST-B3: KC owner tenant picker (1-tenant default skip)', async ({ page }) => {
  test.setTimeout(45000);

  // Re-login
  await page.goto(`${KC_BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[type="email"]').first().fill(OWNER_EMAIL);
  await page.locator('input[type="password"]').first().fill(OWNER_PASS);
  await page.locator('button[type="submit"]').first().click();
  await page.waitForTimeout(2500);

  await page.screenshot({ path: `${SCREENS}/b3-1-post-login.png`, fullPage: true });
  const url = page.url();
  console.log('B3_URL:', url);

  // owner.test = 1 tenant → expect direct landing on dashboard, NO picker shown
  const hasPicker = await page.locator('text=/chọn trung tâm|select tenant|switch workspace/i').count();
  console.log('B3_PICKER_VISIBLE:', hasPicker);
  console.log('B3_VERDICT:', hasPicker === 0 ? 'SKIPPED_PICKER_OK_1_TENANT' : 'PICKER_SHOWN');
});

test('RST-B4: KC owner dashboard nav probe', async ({ page }) => {
  test.setTimeout(90000);

  // Re-login
  await page.goto(`${KC_BASE}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[type="email"]').first().fill(OWNER_EMAIL);
  await page.locator('input[type="password"]').first().fill(OWNER_PASS);
  await page.locator('button[type="submit"]').first().click();
  await page.waitForTimeout(2500);

  // Walk through key dashboard routes — verify each renders without 404 / crash
  const routes = ['/overview', '/branding', '/students', '/teachers', '/classes', '/courses', '/attendance', '/billing', '/settings'];
  for (const route of routes) {
    await page.goto(`${KC_BASE}${route}`, { waitUntil: 'domcontentloaded' }).catch(() => {});
    await page.waitForTimeout(1500);
    const slug = route.replace(/^\//, '').replace(/\//g, '-');
    await page.screenshot({ path: `${SCREENS}/b4-${slug}.png`, fullPage: true });

    const h1 = await page.locator('h1').first().innerText({ timeout: 2000 }).catch(() => 'no-h1');
    const bodyText = await page.locator('body').innerText({ timeout: 2000 }).catch(() => '');
    const hasError = /404|not found|something went wrong|error/i.test(bodyText.substring(0, 200));
    const hasBlank = bodyText.trim().length < 50;
    const finalUrl = page.url();

    console.log(`B4_${route}: url=${finalUrl} | h1="${h1.substring(0, 40)}" | error=${hasError} | blank=${hasBlank}`);
  }
});
