const fs = require('fs');
const path = require('path');
const { chromium } = require('/home/kitedev/projects/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend/node_modules/@playwright/test');
const THESIS = '/home/kitedev/projects/2026-Kite-Class-Platform/documents/08-thesis';
const DOCX = path.join(THESIS, 'thesis-v1.docx');
const OUT = path.join(THESIS, 'screenshots-render');

(async () => {
  const b64 = fs.readFileSync(DOCX).toString('base64');
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 920, height: 1300 }, deviceScaleFactor: 2 });
  await page.setContent('<!doctype html><html><body style="margin:0;background:#fff"><div id="c"></div></body></html>');
  await page.addScriptTag({ url: 'https://unpkg.com/jszip@3.10.1/dist/jszip.min.js' });
  await page.addScriptTag({ url: 'https://unpkg.com/docx-preview@0.3.5/dist/docx-preview.min.js' });
  await page.evaluate(async (b64) => {
    const bin = atob(b64); const arr = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
    await window.docx.renderAsync(new Blob([arr]), document.getElementById('c'), null,
      { className: 'docx', inWrapper: true, breakPages: true, experimental: true });
  }, b64);
  await page.waitForTimeout(2500);

  // find the <p> caption containing "Bảng 2.8" then clip caption + following table
  const box = await page.evaluate(() => {
    const els = [...document.querySelectorAll('p,span,div')];
    const cap = els.find(e => e.textContent.trim().startsWith('Bảng 2.8'));
    if (!cap) return null;
    cap.scrollIntoView();
    const r = cap.getBoundingClientRect();
    return { x: Math.max(0, r.left - 20), y: Math.max(0, r.top - 40), w: 760, h: 460 };
  });
  if (!box) { console.log('caption not found'); await browser.close(); return; }
  await page.screenshot({ path: path.join(OUT, 'wave4-bang-2-8-zoom.png'),
    clip: { x: box.x, y: box.y, width: box.w, height: box.h } });
  console.log('saved wave4-bang-2-8-zoom.png');
  await browser.close();
})().catch(e => { console.error('ERR', e.message); process.exit(1); });
