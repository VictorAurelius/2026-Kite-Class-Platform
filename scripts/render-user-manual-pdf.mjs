#!/usr/bin/env node
/**
 * render-user-manual-pdf.mjs — Puppeteer renderer for user manual PDF per persona
 *
 * Per `.claude/rules/user-manual-content-standard.md` §2 item 15.
 *
 * Strategy:
 *   1. For each Markdown source in documents/05-guides/user-manual/{persona}/*.md,
 *      navigate Puppeteer to the matching Next.js route /help/{persona}/{slug}
 *   2. Call page.pdf() with A4 portrait format + header (logo + persona) + footer (page N/M + URL)
 *   3. Concatenate per-route PDFs via pdf-lib into a single per-persona PDF
 *
 * Usage:
 *   node render-user-manual-pdf.mjs --persona anonymous \
 *     --base-url http://localhost:3001 \
 *     --output documents/05-guides/user-manual/anonymous-manual.pdf
 *
 * Wave 80 Bucket D — GAP-537 follow-up
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ============================================
// Argument parsing
// ============================================

const args = process.argv.slice(2);
const opts = {};
for (let i = 0; i < args.length; i++) {
  const arg = args[i];
  if (arg.startsWith('--')) {
    opts[arg.slice(2)] = args[i + 1];
    i++;
  }
}

const PERSONA = opts.persona || 'anonymous';
const BASE_URL = opts['base-url'] || 'http://localhost:3001';
const PROJECT_ROOT = path.resolve(__dirname, '..');
const MANUAL_DIR = path.join(PROJECT_ROOT, 'documents', '05-guides', 'user-manual');
const OUTPUT = opts.output || path.join(MANUAL_DIR, `${PERSONA}-manual.pdf`);
const PERSONA_DIR = path.join(MANUAL_DIR, PERSONA);

if (!fs.existsSync(PERSONA_DIR)) {
  console.error(`ERROR: persona dir not found: ${PERSONA_DIR}`);
  process.exit(1);
}

// ============================================
// Discover persona pages
// ============================================

function getOrderedPages() {
  const files = fs
    .readdirSync(PERSONA_DIR)
    .filter((f) => f.endsWith('.md') && !f.startsWith('_'))
    .map((f) => f.replace(/\.md$/, ''));

  // index first, rest alphabetical
  files.sort((a, b) => {
    if (a === 'index') return -1;
    if (b === 'index') return 1;
    return a.localeCompare(b);
  });

  return files;
}

const pages = getOrderedPages();
console.log(`[render-pdf] Persona ${PERSONA} → ${pages.length} pages: ${pages.join(', ')}`);

// ============================================
// Try Puppeteer
// ============================================

let puppeteer, PDFDocument;
try {
  puppeteer = (await import('puppeteer')).default;
} catch (e) {
  console.warn(
    '[render-pdf] puppeteer not installed. Install via: npm install -g puppeteer'
  );
  console.warn(
    '[render-pdf] Falling back to single-page-per-route + manual concat'
  );
}

try {
  const pdfLib = await import('pdf-lib');
  PDFDocument = pdfLib.PDFDocument;
} catch (e) {
  console.warn(
    '[render-pdf] pdf-lib not installed. Install via: npm install -g pdf-lib'
  );
}

if (!puppeteer) {
  console.error('[render-pdf] Cannot proceed without puppeteer.');
  console.error(
    '[render-pdf] Install: cd kitehub/kitehub-frontend && pnpm add -D puppeteer pdf-lib'
  );
  console.error(
    '[render-pdf] OR install globally: npm install -g puppeteer pdf-lib'
  );
  console.error(
    '[render-pdf] Bash wrapper will fall back to pandoc + wkhtmltopdf if available.'
  );
  process.exit(2);
}

// ============================================
// Render each route → individual PDF buffer
// ============================================

async function renderRoute(browser, slug) {
  const url = `${BASE_URL}/help/${PERSONA}/${slug}`;
  console.log(`[render-pdf]   → ${url}`);

  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900 });

  // Locale vi-VN per user-manual-content-standard.md §2 row 6
  await page.setExtraHTTPHeaders({ 'Accept-Language': 'vi-VN,vi;q=0.9' });

  try {
    await page.goto(url, { waitUntil: 'networkidle0', timeout: 30000 });
  } catch (e) {
    console.warn(`[render-pdf]   ⚠ navigation timeout for ${url}; falling back to landing /help/${PERSONA}`);
    if (slug === 'index') {
      await page.goto(`${BASE_URL}/help/${PERSONA}`, { waitUntil: 'networkidle0' });
    } else {
      throw e;
    }
  }

  // Wait for content
  await page.waitForSelector('article, main', { timeout: 10000 }).catch(() => {});

  const pdfBuffer = await page.pdf({
    format: 'A4',
    printBackground: true,
    margin: { top: '20mm', bottom: '25mm', left: '15mm', right: '15mm' },
    displayHeaderFooter: true,
    headerTemplate: `
      <div style="font-size: 9px; padding: 0 15mm; width: 100%; color: #475569;">
        <span style="float: left;">KiteHub — Hướng dẫn ${PERSONA}</span>
        <span style="float: right;">${slug}</span>
      </div>
    `,
    footerTemplate: `
      <div style="font-size: 9px; padding: 0 15mm; width: 100%; color: #475569;">
        <span style="float: left;">${url}</span>
        <span style="float: right;">Trang <span class="pageNumber"></span>/<span class="totalPages"></span></span>
      </div>
    `,
  });

  await page.close();
  return pdfBuffer;
}

// ============================================
// Main
// ============================================

(async () => {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--lang=vi-VN'],
  });

  try {
    const pdfBuffers = [];
    for (const slug of pages) {
      try {
        const buf = await renderRoute(browser, slug);
        pdfBuffers.push({ slug, buf });
      } catch (e) {
        console.warn(`[render-pdf] ⚠ skipped ${slug}: ${e.message}`);
      }
    }

    if (pdfBuffers.length === 0) {
      console.error('[render-pdf] No pages rendered. Check dev server.');
      process.exit(3);
    }

    if (PDFDocument && pdfBuffers.length > 1) {
      // Concat via pdf-lib
      const merged = await PDFDocument.create();
      for (const { buf } of pdfBuffers) {
        const doc = await PDFDocument.load(buf);
        const copied = await merged.copyPages(doc, doc.getPageIndices());
        copied.forEach((p) => merged.addPage(p));
      }
      const mergedBytes = await merged.save();
      fs.mkdirSync(path.dirname(OUTPUT), { recursive: true });
      fs.writeFileSync(OUTPUT, mergedBytes);
      console.log(`[render-pdf] ✓ Merged ${pdfBuffers.length} pages → ${OUTPUT}`);
    } else {
      // Single-page fallback or pdf-lib missing
      fs.mkdirSync(path.dirname(OUTPUT), { recursive: true });
      fs.writeFileSync(OUTPUT, pdfBuffers[0].buf);
      if (pdfBuffers.length > 1) {
        console.warn(
          `[render-pdf] ⚠ pdf-lib missing — wrote first page only. Install pdf-lib to concat.`
        );
      } else {
        console.log(`[render-pdf] ✓ Single page → ${OUTPUT}`);
      }
    }
  } finally {
    await browser.close();
  }
})().catch((e) => {
  console.error('[render-pdf] FATAL:', e);
  process.exit(1);
});
