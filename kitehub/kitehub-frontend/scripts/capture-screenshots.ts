/**
 * KiteHub UI Screenshot Capture v1 — Full Coverage Audit
 *
 * Usage:
 *   cd kitehub/kitehub-frontend
 *   npx tsx scripts/capture-screenshots.ts                → latest/
 *   npx tsx scripts/capture-screenshots.ts --label pr-123 → pr-123/ + latest/
 *   BASE_URL=https://... npx tsx scripts/capture-screenshots.ts --label prod
 *
 * Coverage:
 *   - 4 public pages (no backend needed)
 *   - 3 auth pages (client-side forms)
 *   - 7 customer dashboard pages (mock auth injected — OWNER role)
 *   - 5 admin pages (mock auth injected — ADMIN role)
 *   Total: ~152 screenshots + manifest.md
 *
 * Auth injection:
 *   Injects mock Zustand state into localStorage key 'kitehub-auth'.
 *   - Customer pages: OWNER role user
 *   - Admin pages: ADMIN role user (injected per-page)
 *   API calls return errors (no backend) — captures error handling UI.
 *
 * Port: 4701 (different from KiteClass on 4700)
 */

import { chromium, type Page } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { exec, type ChildProcess } from 'child_process';
import http from 'http';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ============================================
// PROJECT CONFIG
// ============================================

const DEFAULT_PORT = 4701;
const DEV_COMMAND = 'npm run dev';
// kitehub-frontend/scripts/ → 3 levels up = project root
const SCREENSHOTS_DIR = path.resolve(__dirname, '../../../documents/screenshots');

// next-themes
const THEME_KEY = 'theme';

// Zustand persist key for auth store
const AUTH_STORAGE_KEY = 'kitehub-auth';

// Mock OWNER auth — for customer dashboard pages
const MOCK_AUTH_OWNER: Record<string, unknown> = {
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
};

// Mock ADMIN auth — for admin pages
const MOCK_AUTH_ADMIN: Record<string, unknown> = {
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
};

// Placeholder ID for parameterized routes
const SAMPLE_ID = 'audit-001';

// ============================================
// PAGE REGISTRY
// ============================================

interface PageConfig {
  name: string;
  path: string;
  authRole?: 'owner' | 'admin'; // inject mock auth before navigate
  note?: string;
}

// === PUBLIC PAGES ===
const PUBLIC_PAGES: PageConfig[] = [
  { name: 'landing',          path: '/' },
  { name: 'pricing',          path: '/pricing' },
  { name: 'blog',             path: '/blog',               note: 'May show empty without backend' },
  { name: 'blog-detail',      path: `/blog/${SAMPLE_ID}`,  note: 'May 404 without backend' },
  { name: 'legal-dmca',       path: '/legal/dmca',         note: 'DMCA intake form (Wave 4 Sub-PR 4.3)' },
];

// === AUTH PAGES (client-side, no backend needed) ===
const AUTH_PAGES: PageConfig[] = [
  { name: 'login',            path: '/login' },
  { name: 'register',         path: '/register' },
  { name: 'verify-email',     path: '/verify-email',       note: 'Shows verify state without token' },
];

// === CUSTOMER DASHBOARD PAGES (OWNER auth) ===
const CUSTOMER_PAGES: PageConfig[] = [
  { name: 'dashboard',        path: '/dashboard',              authRole: 'owner' },
  { name: 'instance-detail',  path: `/instances/${SAMPLE_ID}`, authRole: 'owner', note: 'May show 404' },
  { name: 'settings',         path: '/settings',               authRole: 'owner' },
  { name: 'billing',          path: '/billing',                authRole: 'owner' },
  { name: 'billing-history',  path: '/billing/history',        authRole: 'owner' },
  { name: 'billing-upgrade',  path: '/billing/upgrade',        authRole: 'owner' },
  { name: 'billing-payment',  path: `/billing/payment/${SAMPLE_ID}`, authRole: 'owner', note: 'May show 404' },
  { name: 'branding',         path: '/branding',               authRole: 'owner' },
  { name: 'branding-assets',  path: '/branding/assets',        authRole: 'owner' },
  { name: 'branding-templates', path: '/branding/templates',   authRole: 'owner' },
  { name: 'branding-wizard',  path: '/branding/wizard',        authRole: 'owner' },
];

// === ADMIN PAGES (ADMIN auth) ===
const ADMIN_PAGES: PageConfig[] = [
  { name: 'admin',                 path: '/admin',                         authRole: 'admin' },
  { name: 'admin-instances',       path: '/admin/instances',               authRole: 'admin' },
  { name: 'admin-instance-detail', path: `/admin/instances/${SAMPLE_ID}`,  authRole: 'admin', note: 'May show 404' },
  { name: 'admin-payments',        path: '/admin/payments',                authRole: 'admin' },
  { name: 'admin-revenue',         path: '/admin/revenue',                 authRole: 'admin' },
];

const ALL_PAGES: PageConfig[] = [...PUBLIC_PAGES, ...AUTH_PAGES, ...CUSTOMER_PAGES, ...ADMIN_PAGES];

// ============================================
// VIEWPORTS & THEMES
// ============================================

const VIEWPORTS = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'mobile',  width: 375,  height: 812 },
] as const;

const THEMES = ['light', 'dark'] as const;

// ============================================
// MANIFEST TRACKING
// ============================================

interface CaptureRecord {
  page: string;
  path: string;
  theme: string;
  viewport: string;
  file: string;
  sizeKB: number;
  status: 'ok' | 'error';
  error?: string;
  note?: string;
}

const captureLog: CaptureRecord[] = [];

// ============================================
// SERVER UTILITIES
// ============================================

const labelIdx = process.argv.indexOf('--label');
const label = labelIdx >= 0 ? process.argv[labelIdx + 1] : 'latest';
const BASE_URL = process.env.BASE_URL || `http://localhost:${DEFAULT_PORT}`;
const OUT_DIR = path.join(SCREENSHOTS_DIR, label.includes('kitehub') ? label : `kitehub-${label}`);

function checkServer(url: string): Promise<boolean> {
  return new Promise((resolve) => {
    const req = http.get(url, () => resolve(true));
    req.on('error', () => resolve(false));
    req.setTimeout(2000, () => { req.destroy(); resolve(false); });
  });
}

async function startDevServer(): Promise<ChildProcess | null> {
  if (BASE_URL.includes('vercel') || BASE_URL.includes('github.io')) return null;

  const isUp = await checkServer(BASE_URL);
  if (isUp) {
    console.log(`✓ Dev server at ${BASE_URL}\n`);
    return null;
  }

  console.log('⏳ Starting dev server (port 4701)...');
  const child = exec(DEV_COMMAND, { cwd: path.resolve(__dirname, '..') });

  for (let i = 0; i < 30; i++) {
    await new Promise(r => setTimeout(r, 1000));
    if (await checkServer(BASE_URL)) {
      console.log('✓ Dev server started\n');
      return child;
    }
  }
  console.log('✗ Failed to start dev server');
  child.kill();
  return null;
}

// ============================================
// MANIFEST GENERATION
// ============================================

function writeManifest(outDir: string): void {
  const now = new Date().toISOString();
  const okCount = captureLog.filter(r => r.status === 'ok').length;
  const errCount = captureLog.filter(r => r.status === 'error').length;

  const byPage = new Map<string, CaptureRecord[]>();
  for (const rec of captureLog) {
    if (!byPage.has(rec.page)) byPage.set(rec.page, []);
    byPage.get(rec.page)!.push(rec);
  }

  const lines: string[] = [
    `# Screenshot Manifest — ${label} (KiteHub)`,
    '',
    `**App:** KiteHub Frontend (port ${DEFAULT_PORT})  `,
    `**Generated:** ${now}  `,
    `**Base URL:** ${BASE_URL}  `,
    `**Screenshots:** ${okCount} ok / ${errCount} errors / ${captureLog.length} total  `,
    `**Auth:** Mock-injected (OWNER for customer pages, ADMIN for admin pages)  `,
    '',
    '> Auto-generated. Do NOT edit directly — re-runs overwrite this file.',
    '> Add visual audit notes to `ui-review-latest.md` instead.',
    '',
    '---',
    '',
    '## Quick Index',
    '',
    '| Page | Route | Auth Role | Screenshots | Notes |',
    '|------|-------|-----------|-------------|-------|',
  ];

  for (const [pageName, records] of byPage) {
    const firstRec = records[0];
    const okFiles = records.filter(r => r.status === 'ok').length;
    const errFiles = records.filter(r => r.status === 'error').length;
    const statusIcon = errFiles > 0 ? '⚠️' : '✓';
    const pageConf = ALL_PAGES.find(p => p.name === pageName);
    const authRole = pageConf?.authRole ?? 'none';
    const noteText = firstRec.note ?? '';
    lines.push(
      `| \`${pageName}\` | \`${firstRec.path}\` | ${authRole} | ${statusIcon} ${okFiles}/${records.length} | ${noteText} |`
    );
  }

  lines.push('', '---', '', '## Pages');

  for (const [pageName, records] of byPage) {
    const firstRec = records[0];
    lines.push('', `### \`${pageName}\` → \`${firstRec.path}\``);
    if (firstRec.note) lines.push(`> ${firstRec.note}`);
    lines.push('');
    lines.push('| File | Theme | Viewport | Size | Status |');
    lines.push('|------|-------|----------|------|--------|');
    for (const rec of records) {
      const statusIcon = rec.status === 'ok' ? '✓' : `✗ ${rec.error ?? ''}`;
      lines.push(
        `| \`${pageName}/${rec.file}\` | ${rec.theme} | ${rec.viewport} | ${rec.sizeKB}KB | ${statusIcon} |`
      );
    }
    lines.push('', '**Visual notes:** _(fill during audit)_');
  }

  lines.push('', '---', '', `*Generated by capture-screenshots.ts · ${now}*`);

  const manifestPath = path.join(outDir, 'manifest.md');
  fs.writeFileSync(manifestPath, lines.join('\n') + '\n', 'utf-8');
  console.log(`\n📋 Manifest: ${manifestPath}`);
}

// ============================================
// CAPTURE LOGIC
// ============================================

async function capturePage(
  page: Page,
  p: PageConfig,
  theme: string,
  viewport: typeof VIEWPORTS[number],
  outDir: string,
  isProd: boolean
): Promise<void> {
  const url = `${BASE_URL}${p.path}`;
  const filename = `${theme}-${viewport.name}.png`;
  const pageDir = path.join(outDir, p.name);
  fs.mkdirSync(pageDir, { recursive: true });

  try {
    const waitUntil = 'networkidle' as const;
    const timeout = isProd ? 30000 : 20000;

    await page.goto(url, { waitUntil, timeout });

    // Re-apply theme + auth after navigation, then reload for hydration
    const mockAuth = p.authRole === 'admin' ? MOCK_AUTH_ADMIN : (p.authRole === 'owner' ? MOCK_AUTH_OWNER : null);
    await page.evaluate(
      ([themeKey, themeVal, authKey, authVal]: string[]) => {
        localStorage.setItem(themeKey, themeVal);
        if (authVal) localStorage.setItem(authKey, authVal);
        // Apply dark class directly
        if (themeVal === 'dark') {
          document.documentElement.classList.add('dark');
          document.documentElement.style.colorScheme = 'dark';
        } else {
          document.documentElement.classList.remove('dark');
          document.documentElement.style.colorScheme = 'light';
        }
      },
      [THEME_KEY, theme, AUTH_STORAGE_KEY, mockAuth ? JSON.stringify(mockAuth) : '']
    );
    await page.reload({ waitUntil: 'networkidle', timeout });
    await page.waitForTimeout(isProd ? 1500 : 1000);

    const screenshotPath = path.join(pageDir, filename);
    await page.screenshot({ path: screenshotPath, fullPage: true });
    const sizeKB = Math.round(fs.statSync(screenshotPath).size / 1024);

    captureLog.push({
      page: p.name, path: p.path, theme, viewport: viewport.name,
      file: filename, sizeKB, status: 'ok', note: p.note,
    });
    console.log(`  ✓ ${p.name}/${filename} (${sizeKB}KB)`);
  } catch (e) {
    const errMsg = (e as Error).message.slice(0, 80);
    captureLog.push({
      page: p.name, path: p.path, theme, viewport: viewport.name,
      file: filename, sizeKB: 0, status: 'error', error: errMsg, note: p.note,
    });
    console.log(`  ✗ ${p.name}/${theme}-${viewport.name}: ${errMsg}`);
  }
}

// ============================================
// MAIN
// ============================================

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });

  console.log(`📸 Capturing to: ${path.basename(OUT_DIR)}/`);
  console.log(`🔗 Base URL: ${BASE_URL}`);
  console.log(`📄 Pages: ${PUBLIC_PAGES.length} public + ${AUTH_PAGES.length} auth + ${CUSTOMER_PAGES.length} customer + ${ADMIN_PAGES.length} admin = ${ALL_PAGES.length} total`);
  console.log(`📐 Matrix: ${THEMES.length} themes × ${VIEWPORTS.length} viewports = ${THEMES.length * VIEWPORTS.length} screenshots/page`);
  console.log(`📊 Total: ~${ALL_PAGES.length * THEMES.length * VIEWPORTS.length} screenshots\n`);

  const devServer = await startDevServer();
  const browser = await chromium.launch();
  const isProd = !BASE_URL.includes('localhost');

  for (const theme of THEMES) {
    for (const viewport of VIEWPORTS) {
      console.log(`\n▸ ${theme} / ${viewport.name}`);

      const context = await browser.newContext({
        viewport: { width: viewport.width, height: viewport.height },
        colorScheme: theme,
      });
      const page = await context.newPage();

      // Pre-inject theme into all pages + apply dark class immediately
      await page.addInitScript(([key, val]: [string, string]) => {
        localStorage.setItem(key, val);
        // Apply dark class before hydration to prevent FOUC
        if (val === 'dark') {
          document.documentElement.classList.add('dark');
          document.documentElement.style.colorScheme = 'dark';
        } else {
          document.documentElement.classList.remove('dark');
          document.documentElement.style.colorScheme = 'light';
        }
      }, [THEME_KEY, theme] as [string, string]);

      for (const p of ALL_PAGES) {
        await capturePage(page, p, theme, viewport, OUT_DIR, isProd);
      }

      await context.close();
    }
  }

  await browser.close();
  if (devServer) devServer.kill();

  writeManifest(OUT_DIR);

  const okCount = captureLog.filter(r => r.status === 'ok').length;
  const errCount = captureLog.filter(r => r.status === 'error').length;
  console.log(`\n✅ Done: ${okCount} ok, ${errCount} errors`);
  console.log(`📁 Screenshots: documents/screenshots/${path.basename(OUT_DIR)}/`);
  console.log(`📋 Manifest:    documents/screenshots/${path.basename(OUT_DIR)}/manifest.md`);
}

main().catch(console.error);
