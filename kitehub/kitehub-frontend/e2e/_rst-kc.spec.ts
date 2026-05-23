import { test } from '@playwright/test';

const KC_BASE = 'http://localhost:3000';
const SCREENS = '/tmp/rst-screenshots';

test('RST KC Owner login + dashboard probe', async ({ page }) => {
  test.setTimeout(45000);

  // Login
  await page.goto(`${KC_BASE}/login`);
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: `${SCREENS}/kc-fix-1-login.png`, fullPage: true });

  await page.locator('input[type="email"]').first().fill('owner.test@test.vn');
  await page.locator('input[type="password"]').first().fill('Test@1234');
  await page.screenshot({ path: `${SCREENS}/kc-fix-2-filled.png`, fullPage: true });

  // Capture network response
  const loginRespPromise = page.waitForResponse(r => r.url().includes('/auth/login'), { timeout: 10000 }).catch(() => null);
  await page.locator('button[type="submit"]').first().click();
  const loginResp = await loginRespPromise;
  if (loginResp) {
    console.log('LOGIN_RESPONSE:', loginResp.status(), loginResp.url());
  } else {
    console.log('LOGIN_RESPONSE: timeout/no-network');
  }
  await page.waitForTimeout(2500);
  await page.screenshot({ path: `${SCREENS}/kc-fix-3-after-login.png`, fullPage: true });
  console.log('POST_LOGIN_URL:', page.url());

  // Try parent route if logged in
  if (!page.url().endsWith('/login')) {
    await page.goto(`${KC_BASE}/parent`, { waitUntil: 'networkidle', timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(2000);
    await page.screenshot({ path: `${SCREENS}/kc-fix-4-parent.png`, fullPage: true });
    const text = await page.locator('body').innerText().catch(() => '');
    console.log('PARENT_TEXT_SAMPLE:', text.substring(0, 300));
  }
});
