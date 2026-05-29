/**
 * Thesis demo evidence capture — demo-trio (GAP-804/805/807) Sky Education tenant.
 *
 * Standalone Playwright script (NOT a test-runner spec) so it ignores playwright.config
 * webServer (4700) and drives the LIVE running stack: FE localhost:3000 → gateway 9000.
 *
 * Tenant resolution: the gateway resolves tenant from the `X-Instance-Subdomain` dev
 * header (TenantResolverGatewayFilterFactory). On localhost there is no subdomain, so we
 * inject it via browser-context extraHTTPHeaders → gateway resolves `sky-education`
 * (instance e8ff87e1) → kiteclass-core RLS returns the Sky demo data (orange branding,
 * 78 students, 5 courses, attendance/grade/payment depth).
 *
 * Run from kiteclass-frontend dir:  node e2e/capture-thesis-evidence.mjs
 * Output PNGs: documents/08-thesis/evidence/demo-trio/
 */
import { chromium } from '@playwright/test';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import fs from 'node:fs';

const FE = 'http://localhost:3000';
const OWNER_EMAIL = 'owner.sky@test.vn';
const OWNER_PASS = 'Test@1234';
const SUBDOMAIN = 'sky-education';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(__dirname, '../../../documents/08-thesis/evidence/demo-trio');
fs.mkdirSync(OUT, { recursive: true });

const shot = (page, name) =>
  page.screenshot({ path: path.join(OUT, name), fullPage: true });

(async () => {
  const browser = await chromium.launch({ headless: true });
  const log = (...a) => console.log('[capture]', ...a);
  let failed = false;

  // ---- AFTER state: Sky tenant resolved via X-Instance-Subdomain --------------
  const ctx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    extraHTTPHeaders: { 'X-Instance-Subdomain': SUBDOMAIN },
    locale: 'vi-VN',
  });
  const page = await ctx.newPage();
  page.on('console', (m) => { if (m.type() === 'error') log('PAGE-ERR:', m.text()); });

  try {
    // 1) Login page (auth layout — Sky branding via BrandingProvider)
    await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);
    await shot(page, '01-login-page.png');
    log('login page captured');

    // 2) Login as Sky owner
    await page.locator('input[type="email"]').first().fill(OWNER_EMAIL);
    await page.locator('input[type="password"]').first().fill(OWNER_PASS);
    const loginResp = page.waitForResponse(
      (r) => r.url().includes('/auth/login'), { timeout: 15000 }).catch(() => null);
    await page.locator('button[type="submit"]').first().click();
    const resp = await loginResp;
    log('login status:', resp ? resp.status() : 'no-response');
    await page.waitForTimeout(3000);
    log('post-login url:', page.url());

    // 3) Dashboard overview — KPI real-data (Học viên 78 · Khóa học 5) + orange theme
    await page.goto(`${FE}/overview`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3500); // let useBranding + KPI queries resolve
    const primary = await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue('--primary').trim());
    log('dashboard --primary CSS var:', primary);
    await shot(page, '02-dashboard-overview-kpi-orange.png');
    log('overview captured');

    // 4) Branding settings — orange color + logo render
    await page.goto(`${FE}/branding`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    await shot(page, '03-branding-settings.png');
    log('branding settings captured');

    // 5) Settings page (alt branding surface)
    await page.goto(`${FE}/settings`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2500);
    await shot(page, '04-settings.png');
    log('settings captured');
  } catch (e) {
    failed = true;
    log('ERROR:', e.message);
    await shot(page, 'ERROR-state.png').catch(() => {});
  }
  await ctx.close();

  // ---- BEFORE reference: default theme (no Sky subdomain) ---------------------
  try {
    const ctx2 = await browser.newContext({
      viewport: { width: 1440, height: 900 }, locale: 'vi-VN',
    });
    const p2 = await ctx2.newPage();
    await p2.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' });
    await p2.waitForTimeout(1500);
    await shot(p2, '00-login-default-theme.png');
    log('default-theme reference captured');
    await ctx2.close();
  } catch (e) { log('default-ref skipped:', e.message); }

  await browser.close();
  log('DONE. Output →', OUT);
  process.exit(failed ? 1 : 0);
})();
