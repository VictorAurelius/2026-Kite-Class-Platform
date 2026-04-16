/**
 * KiteClass UI Screenshot Capture v2 — Full Coverage Audit
 *
 * Usage:
 *   cd kiteclass/kiteclass-frontend
 *   npx tsx scripts/capture-screenshots.ts                → latest/
 *   npx tsx scripts/capture-screenshots.ts --label pr-123 → pr-123/ + latest/
 *   BASE_URL=https://... npx tsx scripts/capture-screenshots.ts --label prod
 *
 * Coverage:
 *   - 9 public/auth pages (no backend needed)
 *   - 21 dashboard pages (mock auth injected via localStorage)
 *   - 2 themes × 2 viewports = 4 screenshots per page
 *   Total: ~120 screenshots + manifest.md
 *
 * Auth injection:
 *   Injects mock Zustand state into localStorage key 'auth-storage'.
 *   Dashboard pages render full layout with mock API data (via Playwright route interception).
 *   Pages show realistic content — forms, tables, stats with mock data.
 *
 * Adapted from: claude-starter-kit scripts/capture-screenshots.ts (v2.2.0)
 */

import { chromium, type Page } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { exec, type ChildProcess } from 'child_process';
import http from 'http';
import { setupMockApi } from './mock-api-routes';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ============================================
// PROJECT CONFIG
// ============================================

const DEFAULT_PORT = 3000;
const DEV_COMMAND = 'npm run dev';
// kiteclass-frontend/scripts/ → 3 levels up = project root
const SCREENSHOTS_DIR = path.resolve(__dirname, '../../../documents/screenshots');

// next-themes: key = 'theme', value = 'dark' | 'light'
const THEME_KEY = 'theme';
// KiteClass tenant theme key (separate from next-themes)
const KITECLASS_THEME_KEY = 'kiteclass_theme';

// Zustand persist key for auth store
const AUTH_STORAGE_KEY = 'auth-storage';

// Mock auth state — dashboard pages render layout, API calls will fail gracefully
const MOCK_AUTH: Record<string, unknown> = {
  state: {
    user: {
      id: 1,
      email: 'audit@kiteclass.local',
      name: 'Audit Admin',
      userType: 'ADMIN',
    },
    accessToken: 'audit.mock.token',
    refreshToken: 'audit.mock.refresh',
    tenantId: 'audit-tenant-001',
    isAuthenticated: true,
  },
  version: 0,
};

// Numeric ID for parameterized routes — mock API returns data for these IDs
const SAMPLE_ID = '1';

// ============================================
// PAGE REGISTRY
// ============================================

interface PageConfig {
  name: string;
  path: string;
  needsAuth?: boolean;   // inject MOCK_AUTH before navigate
  waitExtra?: number;    // extra ms after load (animations, lazy content)
  note?: string;         // shown in manifest
}

// === PUBLIC PAGES (no auth required) ===
const PUBLIC_PAGES: PageConfig[] = [
  { name: 'landing',           path: '/',                note: 'Marketing landing — API fallback data expected' },
  { name: 'about',             path: '/about' },
  { name: 'catalog',           path: '/catalog',         note: 'Shows loading spinner without backend' },
  { name: 'catalog-detail',    path: `/catalog/${SAMPLE_ID}`, note: 'May 404 without backend' },
  { name: 'contact',           path: '/contact' },
];

// === AUTH PAGES (client-side forms, no backend needed) ===
const AUTH_PAGES: PageConfig[] = [
  { name: 'login',             path: '/login' },
  { name: 'register',          path: '/register' },
  { name: 'register-student',  path: '/register/student' },
  { name: 'forgot-password',   path: '/forgot-password' },
  { name: 'reset-password',    path: '/reset-password',  note: 'Shows invalid-token error state — expected' },
];

// === DASHBOARD PAGES (mock auth injected) ===
const DASHBOARD_PAGES: PageConfig[] = [
  // Overview
  { name: 'dashboard-teacher', path: '/dashboard',                         needsAuth: true },
  // Classes
  { name: 'classes',           path: '/classes',                          needsAuth: true },
  { name: 'class-detail',      path: `/classes/${SAMPLE_ID}`,             needsAuth: true, note: 'May show 404 — captures error UI' },
  { name: 'class-edit',        path: `/classes/${SAMPLE_ID}/edit`,        needsAuth: true },
  { name: 'class-attendance',  path: `/classes/${SAMPLE_ID}/attendance`,  needsAuth: true },
  // Courses
  { name: 'courses',           path: '/courses',                          needsAuth: true },
  { name: 'course-new',        path: '/courses/new',                      needsAuth: true },
  { name: 'course-detail',     path: `/courses/${SAMPLE_ID}`,             needsAuth: true },
  { name: 'course-edit',       path: `/courses/${SAMPLE_ID}/edit`,        needsAuth: true },
  { name: 'course-class-new',  path: `/courses/${SAMPLE_ID}/classes/new`, needsAuth: true },
  // Students
  { name: 'students',          path: '/students',                         needsAuth: true },
  { name: 'student-new',       path: '/students/new',                     needsAuth: true },
  { name: 'student-detail',    path: `/students/${SAMPLE_ID}`,            needsAuth: true },
  { name: 'student-edit',      path: `/students/${SAMPLE_ID}/edit`,       needsAuth: true },
  { name: 'student-attendance',path: `/students/${SAMPLE_ID}/attendance`, needsAuth: true },
  // Teachers
  { name: 'teachers',          path: '/teachers',                         needsAuth: true },
  { name: 'teacher-new',       path: '/teachers/new',                     needsAuth: true },
  { name: 'teacher-detail',    path: `/teachers/${SAMPLE_ID}`,            needsAuth: true },
  { name: 'teacher-edit',      path: `/teachers/${SAMPLE_ID}/edit`,       needsAuth: true },
  // Attendance
  { name: 'attendance',        path: '/attendance',                       needsAuth: true },
  { name: 'attendance-reports',path: '/attendance/reports',               needsAuth: true },
  { name: 'attendance-stats',  path: '/admin/attendance/stats',           needsAuth: true },
  // Billing
  { name: 'billing',           path: '/billing',                          needsAuth: true },
  { name: 'billing-detail',    path: `/billing/${SAMPLE_ID}`,             needsAuth: true },
  { name: 'billing-pay',       path: `/billing/${SAMPLE_ID}/pay`,         needsAuth: true },
  // Settings
  { name: 'settings',          path: '/settings',                         needsAuth: true },
  // Branding (Wave 3 Sub-PR 3.7)
  { name: 'branding-wizard',   path: '/branding/wizard',                  needsAuth: true, note: 'AI branding wizard scaffold' },
];

const ALL_PAGES: PageConfig[] = [...PUBLIC_PAGES, ...AUTH_PAGES, ...DASHBOARD_PAGES];

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
const OUT_DIR = path.join(SCREENSHOTS_DIR, label);

function checkServer(url: string): Promise<boolean> {
  return new Promise((resolve) => {
    const req = http.get(url, () => resolve(true));
    req.on('error', () => resolve(false));
    req.setTimeout(2000, () => { req.destroy(); resolve(false); });
  });
}

async function startDevServer(): Promise<ChildProcess | null> {
  if (BASE_URL.includes('github.io') || BASE_URL.includes('vercel')) return null;

  const isUp = await checkServer(BASE_URL);
  if (isUp) {
    console.log(`✓ Dev server at ${BASE_URL}\n`);
    return null;
  }

  console.log('⏳ Starting dev server...');
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
// AUTH INJECTION
// ============================================

async function injectAuth(page: Page): Promise<void> {
  await page.addInitScript(([key, val]: [string, string]) => {
    localStorage.setItem(key, val);
  }, [AUTH_STORAGE_KEY, JSON.stringify(MOCK_AUTH)] as [string, string]);
}

// ============================================
// THEME INJECTION
// ============================================

async function setTheme(page: Page, isDark: boolean): Promise<void> {
  const val = isDark ? 'dark' : 'light';
  await page.addInitScript(([themeKey, themeVal, kiteclassKey, kiteclassVal]: string[]) => {
    localStorage.setItem(themeKey, themeVal);
    localStorage.setItem(kiteclassKey, kiteclassVal);
  }, [THEME_KEY, val, KITECLASS_THEME_KEY, val]);
}

// ============================================
// MANIFEST GENERATION
// ============================================

function writeManifest(outDir: string, appName: string, totalPages: number): void {
  const now = new Date().toISOString();
  const okCount = captureLog.filter(r => r.status === 'ok').length;
  const errCount = captureLog.filter(r => r.status === 'error').length;

  // Group by page
  const byPage = new Map<string, CaptureRecord[]>();
  for (const rec of captureLog) {
    if (!byPage.has(rec.page)) byPage.set(rec.page, []);
    byPage.get(rec.page)!.push(rec);
  }

  const lines: string[] = [
    `# Screenshot Manifest — ${label}`,
    '',
    `**App:** ${appName}  `,
    `**Generated:** ${now}  `,
    `**Base URL:** ${BASE_URL}  `,
    `**Screenshots:** ${okCount} ok / ${errCount} errors / ${captureLog.length} total  `,
    `**Auth:** Mock-injected for dashboard pages (API calls may show error states)  `,
    '',
    '> Auto-generated. Do NOT edit directly — re-runs overwrite this file.',
    '> Add visual audit notes to `ui-review-latest.md` instead.',
    '',
    '---',
    '',
    '## Quick Index',
    '',
    '| Page | Route | Auth | Screenshots | Notes |',
    '|------|-------|------|-------------|-------|',
  ];

  for (const [pageName, records] of byPage) {
    const firstRec = records[0];
    const okFiles = records.filter(r => r.status === 'ok').length;
    const errFiles = records.filter(r => r.status === 'error').length;
    const statusIcon = errFiles > 0 ? '⚠️' : '✓';
    const auth = firstRec.note?.includes('Mock') || ALL_PAGES.find(p => p.name === pageName)?.needsAuth ? 'mock' : 'none';
    const noteText = firstRec.note ?? '';
    lines.push(
      `| \`${pageName}\` | \`${firstRec.path}\` | ${auth} | ${statusIcon} ${okFiles}/${records.length} | ${noteText} |`
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
// MAIN
// ============================================

async function capturePage(
  page: Page,
  p: PageConfig,
  theme: string,
  viewport: typeof VIEWPORTS[number],
  outDir: string,
  latestDir: string | null,
  isProd: boolean
): Promise<void> {
  const url = `${BASE_URL}${p.path}`;
  const filename = `${theme}-${viewport.name}.png`;
  const pageDir = path.join(outDir, p.name);
  fs.mkdirSync(pageDir, { recursive: true });

  try {
    const waitUntil = isProd ? 'networkidle' : 'domcontentloaded';
    const timeout = isProd ? 30000 : 15000;

    // Single navigation — addInitScript already set auth + theme before page load
    await page.goto(url, { waitUntil, timeout });
    await page.waitForTimeout(isProd ? 1500 : (p.waitExtra ?? 1200));

    // Dismiss Next.js dev error overlay if present (dev-only, not visible in production)
    try {
      const dismissBtn = page.locator('nextjs-portal button[aria-label="Close"]');
      if (await dismissBtn.isVisible({ timeout: 300 })) {
        await dismissBtn.click();
        await page.waitForTimeout(200);
      }
    } catch {
      // Overlay not present — expected in production
    }

    const screenshotPath = path.join(pageDir, filename);
    await page.screenshot({ path: screenshotPath, fullPage: true });

    const sizeKB = Math.round(fs.statSync(screenshotPath).size / 1024);

    if (latestDir) {
      const latestPageDir = path.join(latestDir, p.name);
      fs.mkdirSync(latestPageDir, { recursive: true });
      await page.screenshot({ path: path.join(latestPageDir, filename), fullPage: true });
    }

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

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });

  const alsoLatest = label !== 'latest';
  const LATEST_DIR = alsoLatest ? path.join(SCREENSHOTS_DIR, 'latest') : null;
  if (LATEST_DIR) {
    fs.mkdirSync(LATEST_DIR, { recursive: true });
    console.log(`📸 Capturing to: ${label}/  (also updating latest/)`);
  } else {
    console.log(`📸 Capturing to: latest/`);
  }
  console.log(`🔗 Base URL: ${BASE_URL}`);
  console.log(`📄 Pages: ${PUBLIC_PAGES.length} public + ${AUTH_PAGES.length} auth + ${DASHBOARD_PAGES.length} dashboard = ${ALL_PAGES.length} total`);
  console.log(`📐 Matrix: ${THEMES.length} themes × ${VIEWPORTS.length} viewports = ${THEMES.length * VIEWPORTS.length} screenshots/page`);
  console.log(`📊 Total: ~${ALL_PAGES.length * THEMES.length * VIEWPORTS.length} screenshots\n`);

  const devServer = await startDevServer();
  const browser = await chromium.launch();
  const isProd = !BASE_URL.includes('localhost');

  for (const theme of THEMES) {
    for (const viewport of VIEWPORTS) {
      console.log(`\n▸ ${theme} / ${viewport.name} (${viewport.width}×${viewport.height})`);

      const context = await browser.newContext({
        viewport: { width: viewport.width, height: viewport.height },
        colorScheme: theme,
      });
      const page = await context.newPage();

      // Intercept all API calls with mock data
      await setupMockApi(page);

      // Pre-inject theme + auth into all pages via initScript
      await setTheme(page, theme === 'dark');
      await injectAuth(page);

      for (const p of ALL_PAGES) {
        await capturePage(page, p, theme, viewport, OUT_DIR, LATEST_DIR, isProd);
      }

      await context.close();
    }
  }

  await browser.close();
  if (devServer) devServer.kill();

  // Write manifests
  writeManifest(OUT_DIR, 'KiteClass Frontend (port 3000)', ALL_PAGES.length);
  if (LATEST_DIR) {
    // Copy manifest to latest too
    fs.copyFileSync(
      path.join(OUT_DIR, 'manifest.md'),
      path.join(LATEST_DIR, 'manifest.md')
    );
  }

  const okCount = captureLog.filter(r => r.status === 'ok').length;
  const errCount = captureLog.filter(r => r.status === 'error').length;
  console.log(`\n✅ Done: ${okCount} ok, ${errCount} errors`);
  console.log(`📁 Screenshots: documents/screenshots/${label}/`);
  console.log(`📋 Manifest:    documents/screenshots/${label}/manifest.md`);
}

main().catch(console.error);
