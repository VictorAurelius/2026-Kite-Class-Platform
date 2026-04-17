/**
 * Targeted capture — only specific pages. Designed to run from kitehub-frontend/.
 * Usage: npx tsx scripts/capture-targeted.ts --pages legal-dmca=/legal/dmca,branding-wizard=/branding/wizard --label wave3-4-missed
 *
 * Auth: Pre-injects mock Zustand state via addInitScript (no redirect flash).
 * Mock API: Intercepts backend calls via Playwright route() (GAP-076).
 */
import { chromium } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { setupMockApi } from './mock-api-routes.js';

const __dirname = path.dirname(new URL(import.meta.url).pathname);

const cliArgs = process.argv.slice(2);
function arg(name: string): string {
  const i = cliArgs.indexOf(`--${name}`);
  return i >= 0 ? cliArgs[i + 1] : '';
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

const AUTH_STORAGE_KEY = 'kitehub-auth';

const MOCK_AUTH_OWNER = JSON.stringify({
  state: {
    user: {
      id: 'audit-00000000-0000-0000-0000-000000000001',
      email: 'owner@kitehub.local',
      name: 'Audit Owner',
      role: 'OWNER',
    },
    accessToken: 'audit.mock.token',
    refreshToken: 'audit.mock.refresh',
    isAuthenticated: true,
  },
  version: 0,
});

const MOCK_AUTH_ADMIN = JSON.stringify({
  state: {
    user: {
      id: 'audit-00000000-0000-0000-0000-000000000002',
      email: 'admin@kitehub.local',
      name: 'Audit Admin',
      role: 'ADMIN',
    },
    accessToken: 'audit.mock.admin.token',
    refreshToken: 'audit.mock.admin.refresh',
    isAuthenticated: true,
  },
  version: 0,
});

/** Detect auth role from route path */
function detectAuthRole(route: string): 'admin' | 'owner' | null {
  if (route.startsWith('/admin')) return 'admin';
  if (route.startsWith('/dashboard') || route.startsWith('/instances')
    || route.startsWith('/settings') || route.startsWith('/billing')
    || route.startsWith('/branding')) return 'owner';
  return null;
}

async function main() {
  console.log(`📸 Targeted: ${pages.map(p => p.name).join(', ')}`);
  console.log(`🔗 ${BASE_URL} → ${OUT_DIR}\n`);

  const browser = await chromium.launch();

  for (const theme of THEMES) {
    for (const vp of VIEWPORTS) {
      console.log(`▸ ${theme} / ${vp.name}`);

      // Group pages by detected auth role for efficient context reuse
      const grouped = new Map<string, typeof pages>();
      for (const p of pages) {
        const role = detectAuthRole(p.route) ?? 'none';
        if (!grouped.has(role)) grouped.set(role, []);
        grouped.get(role)!.push(p);
      }

      for (const [role, rolePages] of grouped) {
        const authVal = role === 'admin' ? MOCK_AUTH_ADMIN
          : role === 'owner' ? MOCK_AUTH_OWNER
          : '';

        const ctx = await browser.newContext({
          viewport: { width: vp.width, height: vp.height },
          colorScheme: theme,
        });

        // Pre-inject theme + auth via addInitScript (runs BEFORE page JS)
        const initArgs = { themeKey: 'theme', themeVal: theme, authKey: AUTH_STORAGE_KEY, authVal };
        await ctx.addInitScript((a: typeof initArgs) => {
          localStorage.setItem(a.themeKey, a.themeVal);
          if (a.themeVal === 'dark') {
            document.documentElement.classList.add('dark');
            document.documentElement.style.colorScheme = 'dark';
          } else {
            document.documentElement.classList.remove('dark');
            document.documentElement.style.colorScheme = 'light';
          }
          if (a.authVal) localStorage.setItem(a.authKey, a.authVal);
        }, initArgs);

        const page = await ctx.newPage();
        await setupMockApi(page);

        for (const p of rolePages) {
          const dir = path.join(OUT_DIR, p.name);
          fs.mkdirSync(dir, { recursive: true });
          const file = `${theme}-${vp.name}.png`;
          try {
            await page.goto(`${BASE_URL}${p.route}`, { waitUntil: 'networkidle', timeout: 20000 });
            await page.waitForTimeout(1000);
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
  }

  await browser.close();
  console.log(`\n✅ Done — ${OUT_DIR}`);
}

main().catch(e => { console.error(e); process.exit(1); });
