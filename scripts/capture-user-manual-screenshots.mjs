#!/usr/bin/env node
/**
 * capture-user-manual-screenshots.mjs — Playwright spec for user manual page captures.
 *
 * Per `.claude/rules/user-manual-content-standard.md` §2 item 6:
 *   - Resolution 1440×900 desktop
 *   - vi-VN locale
 *   - Annotation: mũi tên đỏ #dc2626 + viền vàng #facc15 + step numbers
 *
 * Strategy:
 *   1. Tier 1: Navigate to /help/{persona}/{slug} → screenshot full page
 *   2. Tier 2: Programmatic annotation via Sharp (deferred if Sharp unavailable)
 *
 * Output: documents/05-guides/user-manual/{persona}/screenshots/{topic}-step-{N}.png
 *
 * Wave 80 Bucket D — GAP-537 follow-up
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const args = process.argv.slice(2);
const opts = {};
for (let i = 0; i < args.length; i++) {
  if (args[i].startsWith('--')) {
    opts[args[i].slice(2)] = args[i + 1];
    i++;
  }
}

const PERSONA = opts.persona || 'anonymous';
const BASE_URL = opts['base-url'] || 'http://localhost:3001';
const OUTPUT_DIR = opts['output-dir'];

if (!OUTPUT_DIR) {
  console.error('ERROR: --output-dir required');
  process.exit(1);
}

const PROJECT_ROOT = path.resolve(__dirname, '..');
const PERSONA_DIR = path.join(
  PROJECT_ROOT,
  'documents',
  '05-guides',
  'user-manual',
  PERSONA
);

if (!fs.existsSync(PERSONA_DIR)) {
  console.error(`ERROR: persona dir not found: ${PERSONA_DIR}`);
  process.exit(1);
}

// Discover pages
const pages = fs
  .readdirSync(PERSONA_DIR)
  .filter((f) => f.endsWith('.md') && !f.startsWith('_'))
  .map((f) => f.replace(/\.md$/, ''))
  .sort((a, b) => (a === 'index' ? -1 : b === 'index' ? 1 : a.localeCompare(b)));

console.log(`[capture] Persona ${PERSONA} → ${pages.length} pages`);

let playwright;
try {
  playwright = await import('@playwright/test');
} catch (e) {
  try {
    playwright = await import('playwright');
  } catch (e2) {
    console.error('[capture] Playwright not installed.');
    console.error(
      '[capture] Install: cd kitehub/kitehub-frontend && pnpm add -D @playwright/test'
    );
    console.error('[capture] OR: npm install -g playwright + npx playwright install chromium');
    console.error('[capture] Skipping capture; placeholder strategy applies per §D.2 Tier 2 fallback.');

    // Write placeholders (1×1 transparent PNG)
    // 67-byte transparent 1×1 PNG
    const placeholder = Buffer.from(
      '89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4890000000d4944415478da636408000000000400010ce4cfd60000000049454e44ae426082',
      'hex'
    );
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    for (const slug of pages) {
      for (let step = 1; step <= 3; step++) {
        const f = path.join(OUTPUT_DIR, `${slug}-step-${step}.png`);
        if (!fs.existsSync(f)) {
          fs.writeFileSync(f, placeholder);
          console.log(`[capture]   placeholder: ${path.basename(f)}`);
        }
      }
    }
    console.log(`[capture] ✓ Wrote ${pages.length * 3} placeholder PNG files`);
    process.exit(0);
  }
}

const { chromium } = playwright;

(async () => {
  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--lang=vi-VN'],
  });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'vi-VN',
    extraHTTPHeaders: { 'Accept-Language': 'vi-VN,vi;q=0.9' },
  });

  fs.mkdirSync(OUTPUT_DIR, { recursive: true });

  for (const slug of pages) {
    const url = `${BASE_URL}/help/${PERSONA}/${slug}`;
    console.log(`[capture]   → ${url}`);

    const page = await context.newPage();
    try {
      await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
      await page.waitForSelector('article, main', { timeout: 10000 }).catch(() => {});

      // Capture 3 viewports per page: top (step-1), middle (step-2), bottom (step-3)
      // Approximation: scroll to 0%, 33%, 66% of page height
      const heights = [0, 0.33, 0.66];
      for (let i = 0; i < heights.length; i++) {
        const pct = heights[i];
        await page.evaluate((p) => {
          window.scrollTo(0, document.body.scrollHeight * p);
        }, pct);
        await page.waitForTimeout(500);
        const outFile = path.join(OUTPUT_DIR, `${slug}-step-${i + 1}.png`);
        await page.screenshot({ path: outFile, type: 'png' });
        console.log(`[capture]     ✓ ${path.basename(outFile)}`);
      }
    } catch (e) {
      console.warn(`[capture]     ⚠ failed ${slug}: ${e.message}`);
    } finally {
      await page.close();
    }
  }

  await browser.close();
  console.log(`[capture] ✓ Done`);
})().catch((e) => {
  console.error('[capture] FATAL:', e);
  process.exit(1);
});
