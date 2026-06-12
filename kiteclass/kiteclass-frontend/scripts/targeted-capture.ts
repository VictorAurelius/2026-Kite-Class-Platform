/**
 * Targeted screenshot capture for specific pages after fix.
 * Usage: npx tsx scripts/targeted-capture.ts
 */

import { chromium } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const BASE_URL = 'http://localhost:3100';
const OUTPUT_DIR = path.resolve(__dirname, '../../../documents/screenshots/after-pr-264');

// Only pages affected by PR #264 fixes
const PAGES = [
  { name: 'billing', path: '/billing' },
  { name: 'billing-detail', path: '/billing/1' },
  { name: 'billing-pay', path: '/billing/1/pay' },
  { name: 'settings', path: '/settings' },
  { name: 'dashboard', path: '/dashboard' },
  { name: 'attendance-stats', path: '/admin/attendance/stats' },
  { name: 'students', path: '/students' },
  { name: 'teachers', path: '/teachers' },
];

const VIEWPORTS = [
  { name: 'desktop', width: 1280, height: 800 },
  { name: 'mobile', width: 375, height: 812 },
];

const THEMES = ['light', 'dark'];

async function setTheme(page: any, theme: string) {
  await page.evaluate((t: string) => {
    document.documentElement.classList.remove('light', 'dark');
    document.documentElement.classList.add(t);
    localStorage.setItem('theme', t);
  }, theme);
  await page.waitForTimeout(300);
}

async function fakeAuth(page: any) {
  await page.evaluate(() => {
    localStorage.setItem('accessToken', 'fake-token-for-screenshots');
    localStorage.setItem('refreshToken', 'fake-refresh-token');
    localStorage.setItem('tenantId', '1');
    const authState = {
      state: {
        isAuthenticated: true,
        accessToken: 'fake-token-for-screenshots',
        refreshToken: 'fake-refresh-token',
        user: {
          id: 1,
          email: 'admin@kitehub.me',
          firstName: 'Admin',
          lastName: 'User',
          role: 'ADMIN',
        },
      },
      version: 0,
    };
    localStorage.setItem('auth-storage', JSON.stringify(authState));
  });
}

async function main() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });

  const browser = await chromium.launch();
  let count = 0;

  for (const pageInfo of PAGES) {
    for (const theme of THEMES) {
      for (const viewport of VIEWPORTS) {
        const context = await browser.newContext({
          viewport: { width: viewport.width, height: viewport.height },
        });
        const page = await context.newPage();

        // Set auth for dashboard pages
        await page.goto(BASE_URL);
        await fakeAuth(page);
        await setTheme(page, theme);

        try {
          await page.goto(`${BASE_URL}${pageInfo.path}`, {
            waitUntil: 'networkidle',
            timeout: 15000,
          });
        } catch {
          // networkidle timeout is OK — page may have pending requests
        }

        await page.waitForTimeout(1500);

        const filename = `${pageInfo.name}_${theme}_${viewport.name}.png`;
        await page.screenshot({
          path: path.join(OUTPUT_DIR, filename),
          fullPage: true,
        });

        count++;
        console.log(`[${count}/${PAGES.length * THEMES.length * VIEWPORTS.length}] ${filename}`);

        await context.close();
      }
    }
  }

  await browser.close();

  // Write manifest
  const files = fs.readdirSync(OUTPUT_DIR).filter(f => f.endsWith('.png'));
  const manifest = `# Targeted Capture — after PR #264\n\n` +
    `**Date:** ${new Date().toISOString().split('T')[0]}\n` +
    `**Pages:** ${PAGES.length} (affected by fix only)\n` +
    `**Total:** ${files.length} screenshots\n\n` +
    `## Files\n\n` +
    files.map(f => `- ${f} (${(fs.statSync(path.join(OUTPUT_DIR, f)).size / 1024).toFixed(0)}KB)`).join('\n') + '\n';

  fs.writeFileSync(path.join(OUTPUT_DIR, 'manifest.md'), manifest);
  console.log(`\nDone: ${count} screenshots in ${OUTPUT_DIR}`);
}

main().catch(console.error);
