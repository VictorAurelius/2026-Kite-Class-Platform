import { chromium } from '@playwright/test';
const tenants = {
  'co-khanh': '126eaa8c-1f63-4c30-81b5-a5921b384b3b',
  'co-ha':    'ad0fa96e-af24-49cb-b3e5-19d44f182d85',
  'thay-nhi': '0abe093c-4c66-4c99-abab-a756582dc60b',
};
const outdir = '../../documents/08-thesis/evidence/landing-verify';
const browser = await chromium.launch();
for (const [name, id] of Object.entries(tenants)) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await ctx.newPage();
  const errs = [];
  page.on('console', m => { if (m.type() === 'error') errs.push(m.text().slice(0,140)); });
  page.on('pageerror', e => errs.push('PAGEERROR: ' + e.message.slice(0,140)));
  const resp = await page.goto(`http://localhost:4700/?tenant=${id}`, { waitUntil: 'networkidle', timeout: 45000 });
  await page.waitForTimeout(1500);
  await page.screenshot({ path: `${outdir}/${name}.png`, fullPage: true });
  console.log(`${name}: HTTP ${resp.status()} | console-errors=${errs.length}`);
  errs.slice(0,4).forEach(e => console.log('   ⚠ ' + e));
  await ctx.close();
}
await browser.close();
