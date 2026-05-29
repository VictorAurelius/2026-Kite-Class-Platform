/**
 * RST Wave 106 Mảng A — Khách ẩn danh (anonymous prospect)
 *
 * 3 luồng walk-through KH frontend tại localhost:3001 — mirror cấu trúc
 * `_rst-wave-107-anonymous.spec.ts` Wave 107 baseline, but output to
 * `/tmp/rst-screenshots/wave-106-mang-a/` cho Wave meta-6 Bucket C
 * (RST HTML dashboard MVP — `documents/04-quality/audits/rst-html/wave-106-mang-a/`).
 *
 *  - A1: Trang chủ ẩn danh `/` — verify hero + nav + VN locale
 *  - A2: Biểu mẫu yêu cầu beta `/beta-status` — verify form/CTA visible
 *  - A3: Trang chính sách / pricing `/pricing` — verify VND format `đ` symbol
 *    (Note: Mảng A3 trong Wave 106 plan = "Trang chính sách"; ở UI hiện tại,
 *    `/terms` + `/privacy` chưa fully wired up tách riêng — sample dùng `/pricing`
 *    làm proxy cho VN-localization audit per `vn-localization-audit-checklist.md` §2)
 *
 * Pattern theo Wave 105/107 baseline — walk + screenshots + console.log,
 * không assertions cứng (observation-driven RST per `e2e-rst-test-layer-boundary.md`).
 *
 * @since Wave meta-6
 */
import { test } from '@playwright/test';

const KH_BASE = 'http://localhost:3001';
const SCREENS = '/tmp/rst-screenshots/wave-106-mang-a';

test('RST-A1: Trang chủ ẩn danh / — hero + nav + VN locale', async ({ page }) => {
  test.setTimeout(30000);
  await page.goto(`${KH_BASE}/`);
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: `${SCREENS}/a1-trang-chu.png`, fullPage: true });

  const title = await page.title().catch(() => 'no-title');
  const h1 = await page.locator('h1').first().innerText().catch(() => 'no-h1');
  const navLinks = await page.locator('nav a').allInnerTexts().catch(() => []);
  console.log('A1_TITLE:', title);
  console.log('A1_H1:', h1);
  console.log('A1_NAV_LINKS:', JSON.stringify(navLinks.slice(0, 10)));

  const htmlLang = await page.locator('html').getAttribute('lang').catch(() => null);
  console.log('A1_HTML_LANG:', htmlLang);

  const ctaCount = await page.locator('a[href*="beta"], a[href*="signup"], a[href*="pricing"]').count();
  console.log('A1_CTA_COUNT:', ctaCount);
});

test('RST-A2: Biểu mẫu yêu cầu beta /beta-status — form/CTA visible', async ({ page }) => {
  test.setTimeout(30000);
  await page.goto(`${KH_BASE}/beta-status`);
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: `${SCREENS}/a2-bieu-mau-beta.png`, fullPage: true });

  const title = await page.title().catch(() => 'no-title');
  const h1 = await page.locator('h1').first().innerText().catch(() => 'no-h1');
  const bodyText = await page.locator('body').innerText().catch(() => '');

  console.log('A2_TITLE:', title);
  console.log('A2_H1:', h1);
  console.log('A2_BODY_SAMPLE:', bodyText.substring(0, 400).replace(/\n+/g, ' | '));

  const formCount = await page.locator('form').count();
  const requestCta = await page.locator(
    'a[href*="beta"], button:has-text("Yêu cầu"), button:has-text("Đăng ký"), button:has-text("Gửi")'
  ).count();
  console.log('A2_FORM_COUNT:', formCount);
  console.log('A2_REQUEST_CTA_COUNT:', requestCta);
});

test('RST-A3: Trang pricing /pricing — VND format check (proxy cho chính sách)', async ({ page }) => {
  test.setTimeout(45000);
  await page.goto(`${KH_BASE}/pricing`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(2500);
  await page.screenshot({ path: `${SCREENS}/a3-trang-chinh-sach.png`, fullPage: true });

  const bodyText = await page.locator('body').innerText().catch(() => '');

  const hasDongSymbol = /đ|VNĐ|VND/.test(bodyText);
  const hasUsdSymbol = /\$\d/.test(bodyText);
  console.log('A3_VND_PRESENT:', hasDongSymbol);
  console.log('A3_USD_PRESENT (should be false):', hasUsdSymbol);
  console.log('A3_BODY_SAMPLE:', bodyText.substring(0, 500).replace(/\n+/g, ' | '));

  const tierCount = await page.locator('[class*="tier"], [class*="plan"], [class*="card"]').count();
  console.log('A3_TIER_CARDS:', tierCount);
});
