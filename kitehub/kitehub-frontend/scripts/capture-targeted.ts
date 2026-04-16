/**
 * Targeted capture — only specific pages. Designed to run from kitehub-frontend/.
 * Usage: npx tsx scripts/capture-targeted.ts --pages legal-dmca=/legal/dmca,branding-wizard=/branding/wizard --label wave3-4-missed
 */
import { chromium } from '@playwright/test';
import path from 'path';
import fs from 'fs';

const args = process.argv.slice(2);
function arg(name: string): string {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : '';
}

const port = parseInt(arg('port') || '3001', 10);
const label = arg('label') || 'targeted';
const pages = (arg('pages') || '').split(',').map(raw => {
  const idx = raw.indexOf('=');
  if (idx < 0) return { name: raw, route: '/' + raw };
  return { name: raw.slice(0, idx), route: raw.slice(idx + 1) };
}).filter(p => p.name && p.route);

console.log('Pages:', JSON.stringify(pages));

if (!pages.length) { console.error('--pages required'); process.exit(1); }

const BASE_URL = `http://localhost:${port}`;
const OUT_DIR = path.resolve(__dirname, `../../../documents/screenshots/kitehub-${label}`);

const THEMES = ['light', 'dark'] as const;
const VIEWPORTS = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'mobile', width: 375, height: 812 },
];

const MOCK_AUTH = JSON.stringify({
  state: {
    user: { id: 'audit-001', email: 'owner@audit.local', name: 'Audit Owner', role: 'OWNER' },
    accessToken: 'audit.mock.token', refreshToken: 'audit.mock.refresh', isAuthenticated: true,
  },
  version: 0,
});

async function main() {
  console.log(`📸 Targeted: ${pages.map(p => p.name).join(', ')}`);
  console.log(`🔗 ${BASE_URL} → ${OUT_DIR}\n`);

  const browser = await chromium.launch();

  for (const theme of THEMES) {
    for (const vp of VIEWPORTS) {
      console.log(`▸ ${theme} / ${vp.name}`);
      const ctx = await browser.newContext({
        viewport: { width: vp.width, height: vp.height },
        colorScheme: theme,
      });
      const page = await ctx.newPage();
      await page.addInitScript(([k, v]: [string, string]) => { localStorage.setItem(k, v); }, ['theme', theme] as [string, string]);

      for (const p of pages) {
        const dir = path.join(OUT_DIR, p.name);
        fs.mkdirSync(dir, { recursive: true });
        const file = `${theme}-${vp.name}.png`;
        try {
          await page.goto(`${BASE_URL}${p.route}`, { waitUntil: 'domcontentloaded', timeout: 20000 });
          await page.evaluate(([ak, av]: [string, string]) => { localStorage.setItem(ak, av); },
            ['kitehub-auth', MOCK_AUTH] as [string, string]);
          await page.reload({ waitUntil: 'domcontentloaded', timeout: 20000 });
          await page.waitForTimeout(1500);
          const fp = path.join(dir, file);
          await page.screenshot({ path: fp, fullPage: true });
          const kb = Math.round(fs.statSync(fp).size / 1024);
          console.log(`  ✓ ${p.name}/${file} (${kb}KB)`);
        } catch (e) {
          console.log(`  ✗ ${p.name}/${file}: ${(e as Error).message.slice(0, 100)}`);
        }
      }
      await ctx.close();
    }
  }

  await browser.close();
  console.log(`\n✅ Done — ${OUT_DIR}`);
}

main().catch(e => { console.error(e); process.exit(1); });
