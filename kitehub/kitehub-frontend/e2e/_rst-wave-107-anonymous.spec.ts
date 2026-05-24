/**
 * RST Wave 107 Mảng A — Khách ẩn danh (anonymous prospect)
 *
 * 3 luồng walk-through KH frontend tại localhost:3001:
 *  - A1: Land homepage `/` — verify hero + nav + VN locale
 *  - A2: Pricing page `/pricing` — verify VND format `đ` symbol per VN-localization §2
 *  - A3: Beta status / signup intent `/beta-status` — verify form/CTA visible
 *
 * Pattern theo `e2e/_rst-kc.spec.ts` Wave 105 baseline — walk + screenshots + console.log,
 * không assertions cứng (observation-driven RST).
 *
 * @since Wave 107
 */
import { test } from '@playwright/test';

const KH_BASE = 'http://localhost:3001';
const SCREENS = '/tmp/rst-screenshots/wave-107';

test('RST-A1: KH anonymous land homepage /', async ({ page }) => {
  test.setTimeout(30000);
  await page.goto(`${KH_BASE}/`);
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: `${SCREENS}/a1-homepage.png`, fullPage: true });

  // Capture key surfaces
  const title = await page.title().catch(() => 'no-title');
  const h1 = await page.locator('h1').first().innerText().catch(() => 'no-h1');
  const navLinks = await page.locator('nav a').allInnerTexts().catch(() => []);
  console.log('A1_TITLE:', title);
  console.log('A1_H1:', h1);
  console.log('A1_NAV_LINKS:', JSON.stringify(navLinks.slice(0, 10)));

  // VN locale check
  const html = await page.locator('html').getAttribute('lang').catch(() => null);
  console.log('A1_HTML_LANG:', html);

  // CTA visibility (typical conversion funnel entry)
  const ctaCount = await page.locator('a[href*="beta"], a[href*="signup"], a[href*="pricing"]').count();
  console.log('A1_CTA_COUNT:', ctaCount);
});

test('RST-A2: KH anonymous /pricing — VND format check', async ({ page }) => {
  test.setTimeout(45000);
  await page.goto(`${KH_BASE}/pricing`, { waitUntil: 'domcontentloaded' });
  // /pricing has long-tail network activity (analytics/Vercel beacons); domcontentloaded is enough.
  // networkidle previously caused 30s timeout — captured as Wave 107 RST observation.
  await page.waitForTimeout(2500);
  await page.screenshot({ path: `${SCREENS}/a2-pricing.png`, fullPage: true });

  const bodyText = await page.locator('body').innerText().catch(() => '');

  // VN-localization §2 row "Currency": expect `đ` symbol or `VNĐ`
  const hasDongSymbol = /đ|VNĐ|VND/.test(bodyText);
  const hasUsdSymbol = /\$\d/.test(bodyText);
  console.log('A2_VND_PRESENT:', hasDongSymbol);
  console.log('A2_USD_PRESENT (should be false):', hasUsdSymbol);
  console.log('A2_BODY_SAMPLE:', bodyText.substring(0, 500).replace(/\n+/g, ' | '));

  // Pricing tiers visible
  const tierCount = await page.locator('[class*="tier"], [class*="plan"], [class*="card"]').count();
  console.log('A2_TIER_CARDS:', tierCount);
});

test('RST-A3: KH anonymous /beta-status — signup intent surface', async ({ page }) => {
  test.setTimeout(30000);
  await page.goto(`${KH_BASE}/beta-status`);
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: `${SCREENS}/a3-beta-status.png`, fullPage: true });

  const title = await page.title().catch(() => 'no-title');
  const h1 = await page.locator('h1').first().innerText().catch(() => 'no-h1');
  const bodyText = await page.locator('body').innerText().catch(() => '');

  console.log('A3_TITLE:', title);
  console.log('A3_H1:', h1);
  console.log('A3_BODY_SAMPLE:', bodyText.substring(0, 400).replace(/\n+/g, ' | '));

  // Beta funnel surface: form OR CTA link to request access
  const formCount = await page.locator('form').count();
  const requestCta = await page.locator('a[href*="beta"], button:has-text("Yêu cầu"), button:has-text("Đăng ký")').count();
  console.log('A3_FORM_COUNT:', formCount);
  console.log('A3_REQUEST_CTA_COUNT:', requestCta);
});
