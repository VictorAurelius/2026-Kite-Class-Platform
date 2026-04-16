/**
 * Targeted capture — only specific pages for audit.
 * Usage: npx tsx .claude/hooks/targeted-capture.ts --app kitehub --port 3001 --pages legal-dmca=/legal/dmca,branding-wizard=/branding/wizard --label wave3-4-missed
 */
import { chromium } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import http from 'http';
import { exec, type ChildProcess } from 'child_process';

const args = process.argv.slice(2);
function arg(name: string): string {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : '';
}

const app = arg('app') || 'kitehub';
const port = parseInt(arg('port') || '3001', 10);
const label = arg('label') || 'targeted';
const pagesParsed = (arg('pages') || '').split(',').map(p => {
  const [name, route] = p.split('=');
  return { name, path: route };
}).filter(p => p.name && p.path);

if (!pagesParsed.length) {
  console.error('No pages specified. Use --pages name1=/path1,name2=/path2');
  process.exit(1);
}

const BASE_URL = `http://localhost:${port}`;
const SCREENSHOTS_DIR = path.resolve(__dirname, '../../documents/screenshots');
const prefix = app === 'kiteclass' ? '' : `${app}-`;
const OUT_DIR = path.join(SCREENSHOTS_DIR, `${prefix}${label}`);

const THEMES = ['light', 'dark'] as const;
const VIEWPORTS = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'mobile', width: 375, height: 812 },
];

const AUTH_STORAGE_KEY = app === 'kitehub' ? 'kitehub-auth' : 'kiteclass-auth';
const MOCK_AUTH = {
  state: {
    user: { id: 'audit-001', email: 'owner@audit.local', name: 'Audit Owner', role: 'OWNER' },
    accessToken: 'audit.mock.token', refreshToken: 'audit.mock.refresh', isAuthenticated: true,
  },
  version: 0,
};

function checkServer(): Promise<boolean> {
  return new Promise(resolve => {
    const req = http.get(BASE_URL, () => resolve(true));
    req.on('error', () => resolve(false));
    req.setTimeout(3000, () => { req.destroy(); resolve(false); });
  });
}

async function startDev(): Promise<ChildProcess | null> {
  if (await checkServer()) { console.log(`✓ Dev server at ${BASE_URL}`); return null; }
  console.log('⏳ Starting dev server...');
  const cwd = app === 'kitehub'
    ? path.resolve(__dirname, '../../kitehub/kitehub-frontend')
    : path.resolve(__dirname, '../../kiteclass/kiteclass-frontend');
  const child = exec(`PORT=${port} npm run dev`, { cwd });
  for (let i = 0; i < 60; i++) {
    await new Promise(r => setTimeout(r, 1000));
    if (await checkServer()) { console.log('✓ Dev server started'); return child; }
  }
  console.error('✗ Dev server timeout'); child.kill(); process.exit(1);
}

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  const dev = await startDev();
  const browser = await chromium.launch();

  console.log(`\n📸 Targeted capture: ${pagesParsed.map(p => p.name).join(', ')}`);
  console.log(`📁 Output: ${OUT_DIR}\n`);

  for (const theme of THEMES) {
    for (const vp of VIEWPORTS) {
      const ctx = await browser.newContext({
        viewport: { width: vp.width, height: vp.height },
        colorScheme: theme,
      });
      const page = await ctx.newPage();
      await page.addInitScript(([k, v]: [string, string]) => { localStorage.setItem(k, v); }, ['theme', theme] as [string, string]);

      for (const p of pagesParsed) {
        const url = `${BASE_URL}${p.path}`;
        const dir = path.join(OUT_DIR, p.name);
        fs.mkdirSync(dir, { recursive: true });
        const file = `${theme}-${vp.name}.png`;
        try {
          await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 15000 });
          await page.evaluate(([ak, av]: [string, string]) => { if (av) localStorage.setItem(ak, av); },
            [AUTH_STORAGE_KEY, JSON.stringify(MOCK_AUTH)] as [string, string]);
          await page.reload({ waitUntil: 'domcontentloaded', timeout: 15000 });
          await page.waitForTimeout(1200);
          const fp = path.join(dir, file);
          await page.screenshot({ path: fp, fullPage: true });
          const kb = Math.round(fs.statSync(fp).size / 1024);
          console.log(`  ✓ ${p.name}/${file} (${kb}KB)`);
        } catch (e) {
          console.log(`  ✗ ${p.name}/${file}: ${(e as Error).message.slice(0, 80)}`);
        }
      }
      await ctx.close();
    }
  }

  await browser.close();
  if (dev) dev.kill();
  console.log(`\n✅ Done — ${OUT_DIR}`);
}

main().catch(e => { console.error(e); process.exit(1); });
