// kitehub-banner-renderer — HTML → WebP rasterise sidecar (GAP-1135).
//
// Single purpose: receive composed banner HTML from kitehub-branding
// (BannerHtmlComposer output), render it in headless Chromium at the requested
// viewport, and return a WebP image. This keeps the JVM branding service image
// free of Node/Chromium (the deferred deployment decision in BannerRenderer.java).
//
// Contract:
//   POST /render   { html: string, width?: number, height?: number } -> image/webp
//   GET  /health   -> 200 "ok"
//
// The Chromium browser is launched once and reused across requests (one page per
// request, closed after). deviceScaleFactor:2 + sharp webp(quality:88) mirror the
// reference rasteriser kiteclass-frontend/scripts/compose-sky-demo-banner.mjs.

import http from 'node:http';
import { chromium } from 'playwright';
import sharp from 'sharp';

const PORT = Number(process.env.PORT || 3000);
const MAX_BODY_BYTES = 4 * 1024 * 1024; // 4 MB composed-HTML ceiling
const DEFAULT_WIDTH = 1200;
const DEFAULT_HEIGHT = 630;
const NAV_TIMEOUT_MS = Number(process.env.RENDER_TIMEOUT_MS || 15000);

let browserPromise = null;

/** Launch Chromium once; relaunch transparently if it died. */
async function getBrowser() {
  if (browserPromise) {
    const b = await browserPromise.catch(() => null);
    if (b && b.isConnected()) return b;
    browserPromise = null;
  }
  browserPromise = chromium.launch({
    args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
  });
  return browserPromise;
}

async function renderWebp(html, width, height) {
  const browser = await getBrowser();
  const page = await browser.newPage({
    viewport: { width, height },
    deviceScaleFactor: 2,
  });
  try {
    page.setDefaultTimeout(NAV_TIMEOUT_MS);
    await page.setContent(html, { waitUntil: 'networkidle' });
    // Wait for web fonts so Vietnamese diacritics render crisp, not fallback.
    await page.evaluate(() => document.fonts && document.fonts.ready).catch(() => {});
    await page.waitForTimeout(300);
    const png = await page.screenshot({ clip: { x: 0, y: 0, width, height } });
    return await sharp(png).resize(width, height).webp({ quality: 88 }).toBuffer();
  } finally {
    await page.close().catch(() => {});
  }
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY_BYTES) {
        reject(new Error('payload too large'));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'GET' && req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('ok');
    return;
  }

  if (req.method === 'POST' && req.url === '/render') {
    const started = Date.now();
    try {
      const raw = await readBody(req);
      const { html, width, height } = JSON.parse(raw || '{}');
      if (typeof html !== 'string' || html.trim() === '') {
        res.writeHead(400, { 'Content-Type': 'text/plain' });
        res.end('field "html" (non-empty string) is required');
        return;
      }
      const w = Number.isFinite(width) && width > 0 ? Math.min(width, 4096) : DEFAULT_WIDTH;
      const h = Number.isFinite(height) && height > 0 ? Math.min(height, 4096) : DEFAULT_HEIGHT;
      const webp = await renderWebp(html, w, h);
      res.writeHead(200, {
        'Content-Type': 'image/webp',
        'Content-Length': webp.length,
      });
      res.end(webp);
      console.log(`[render] ${w}x${h} -> ${webp.length}B in ${Date.now() - started}ms`);
    } catch (err) {
      console.error('[render] failed:', err?.message || err);
      res.writeHead(500, { 'Content-Type': 'text/plain' });
      res.end(`render failed: ${err?.message || 'unknown'}`);
    }
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('not found');
});

server.listen(PORT, () => console.log(`kitehub-banner-renderer listening on :${PORT}`));

// Graceful shutdown — close Chromium so the container exits clean.
for (const sig of ['SIGTERM', 'SIGINT']) {
  process.on(sig, async () => {
    server.close();
    try {
      const b = await browserPromise;
      if (b) await b.close();
    } catch { /* ignore */ }
    process.exit(0);
  });
}
