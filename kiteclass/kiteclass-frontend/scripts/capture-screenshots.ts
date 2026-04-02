/**
 * KiteClass UI Screenshot Capture — for UI quality audits.
 *
 * Usage:
 *   cd kiteclass/kiteclass-frontend
 *   npx tsx scripts/capture-screenshots.ts                → latest/
 *   npx tsx scripts/capture-screenshots.ts --label pr-123 → pr-123/ + latest/
 *   BASE_URL=https://... npx tsx scripts/capture-screenshots.ts --label prod
 *
 * Features:
 *   - Auto-detects dev server, starts if needed
 *   - Auto-updates latest/ when using --label
 *   - Per-page subfolders: {label}/{page}/{theme}-{viewport}.png
 *   - Captures both light + dark (next-themes)
 *
 * Adapted from: claude-starter-kit scripts/capture-screenshots.ts (v2.1.0)
 */

import { chromium } from 'playwright';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { exec, ChildProcess } from 'child_process';
import http from 'http';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ============================================
// ADAPT THESE FOR YOUR PROJECT
// ============================================

const DEFAULT_PORT = 3000;
const DEV_COMMAND = `npm run dev`;
// From kiteclass/kiteclass-frontend/scripts/ → up 3 levels = project root
const SCREENSHOTS_DIR = path.resolve(__dirname, '../../../documents/screenshots');

// === PUBLIC PAGES (no auth required) ===
// Public routes render fully without backend (SSG/ISR fallback)
// Auth routes (login, register) are 'use client' — render form without API
const PAGES = [
  // Public marketing pages
  { name: 'landing', path: '/' },
  { name: 'about', path: '/about' },
  { name: 'catalog', path: '/catalog' },
  { name: 'contact', path: '/contact' },
  // Auth pages (client-side, no backend needed)
  { name: 'login', path: '/login' },
  { name: 'register', path: '/register' },
  { name: 'register-student', path: '/register/student' },
  { name: 'forgot-password', path: '/forgot-password' },
  { name: 'reset-password', path: '/reset-password' },
  // Dashboard pages — uncomment after setting up auth storageState
  // { name: 'classes', path: '/classes' },
  // { name: 'courses', path: '/courses' },
  // { name: 'students', path: '/students' },
  // { name: 'attendance', path: '/attendance' },
  // { name: 'billing', path: '/billing' },
  // { name: 'settings', path: '/settings' },
  // { name: 'teacher-dashboard', path: '/teacher/dashboard' },
];

// ============================================
// Configuration (usually no changes needed)
// ============================================

const VIEWPORTS = [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'mobile', width: 375, height: 812 },
];

const THEMES = ['light', 'dark'] as const;

// next-themes stores theme as plain string: 'dark' | 'light'
// Key is 'theme' by default for next-themes
const DARK_MODE_KEY = 'theme';
const DARK_MODE_VALUE = (isDark: boolean) => isDark ? 'dark' : 'light';

// ============================================
// Script logic (no changes needed below)
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
    console.log(`✓ Dev server running at ${BASE_URL}\n`);
    return null;
  }

  console.log(`⏳ Starting dev server...`);
  const child = exec(DEV_COMMAND, { cwd: path.resolve(__dirname, '..') });

  for (let i = 0; i < 20; i++) {
    await new Promise(r => setTimeout(r, 1000));
    if (await checkServer(BASE_URL)) {
      console.log(`✓ Dev server started\n`);
      return child;
    }
  }
  console.log(`✗ Failed to start dev server`);
  child.kill();
  return null;
}

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });

  console.log(`📸 Capturing to: ${label}/`);
  console.log(`🔗 Source: ${BASE_URL}`);

  const alsoLatest = label !== 'latest';
  const LATEST_DIR = path.join(SCREENSHOTS_DIR, 'latest');
  if (alsoLatest) {
    fs.mkdirSync(LATEST_DIR, { recursive: true });
    console.log(`📸 Also updating: latest/`);
  }

  const devServer = await startDevServer();
  const browser = await chromium.launch();
  const isProd = !BASE_URL.includes('localhost');

  for (const theme of THEMES) {
    for (const viewport of VIEWPORTS) {
      const context = await browser.newContext({
        viewport: { width: viewport.width, height: viewport.height },
        colorScheme: theme,
      });
      const page = await context.newPage();

      await page.addInitScript(([key, val]: [string, string]) => {
        localStorage.setItem(key, val);
      }, [DARK_MODE_KEY, DARK_MODE_VALUE(theme === 'dark')] as [string, string]);

      for (const p of PAGES) {
        const url = `${BASE_URL}${p.path}`;
        try {
          const waitUntil = isProd ? 'networkidle' : 'domcontentloaded';
          const timeout = isProd ? 30000 : 15000;

          await page.goto(url, { waitUntil, timeout });
          await page.evaluate(([key, val]: [string, string]) => {
            localStorage.setItem(key, val);
          }, [DARK_MODE_KEY, DARK_MODE_VALUE(theme === 'dark')] as [string, string]);
          await page.reload({ waitUntil, timeout });
          await page.waitForTimeout(isProd ? 1500 : 800);

          const filename = `${theme}-${viewport.name}.png`;
          const pageDir = path.join(OUT_DIR, p.name);
          fs.mkdirSync(pageDir, { recursive: true });
          await page.screenshot({ path: path.join(pageDir, filename), fullPage: true });

          if (alsoLatest) {
            const latestPageDir = path.join(LATEST_DIR, p.name);
            fs.mkdirSync(latestPageDir, { recursive: true });
            await page.screenshot({ path: path.join(latestPageDir, filename), fullPage: true });
          }

          console.log(`  ✓ ${p.name}/${filename}`);
        } catch (e) {
          console.log(`  ✗ ${p.name}/${theme}-${viewport.name}: ${(e as Error).message}`);
        }
      }

      await context.close();
    }
  }

  await browser.close();
  if (devServer) devServer.kill();

  console.log(`\n✅ Screenshots saved to documents/screenshots/${label}/`);
  if (alsoLatest) console.log(`✅ Also updated documents/screenshots/latest/`);
}

main().catch(console.error);
