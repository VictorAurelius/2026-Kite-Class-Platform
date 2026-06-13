/**
 * render-docx-capture.cjs — render thesis-v1.docx → PNG per page via docx-preview + Playwright.
 * Run: NODE_PATH=<kiteclass-frontend/node_modules> node render-docx-capture.cjs
 * No LibreOffice needed — docx-preview renders docx XML in headless chromium.
 */
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
  await page.setContent('<!doctype html><html><body style="margin:0;background:#888"><div id="c"></div></body></html>');
  await page.addScriptTag({ url: 'https://unpkg.com/jszip@3.10.1/dist/jszip.min.js' });
  await page.addScriptTag({ url: 'https://unpkg.com/docx-preview@0.3.5/dist/docx-preview.min.js' });
  await page.evaluate(async (b64) => {
    const bin = atob(b64); const arr = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
    const blob = new Blob([arr]);
    await window.docx.renderAsync(blob, document.getElementById('c'), null, {
      className: 'docx', inWrapper: true, breakPages: true,
      ignoreWidth: false, ignoreHeight: false, experimental: true,
    });
  }, b64);
  await page.waitForTimeout(2500);

  // docx-preview emits <section class="docx"> per page inside .docx-wrapper
  const sections = await page.$$('.docx-wrapper > section');
  console.log('PAGES:', sections.length);

  // Page 1 = cover (#1)
  if (sections[0]) { await sections[0].screenshot({ path: path.join(OUT, 'wave4-cover.png') }); console.log('saved wave4-cover.png'); }
  if (sections[1]) { await sections[1].screenshot({ path: path.join(OUT, 'wave4-cover-phu.png') }); console.log('saved wave4-cover-phu.png'); }

  // Find page containing "Bảng 2.8" (#5)
  let found = -1;
  for (let i = 0; i < sections.length; i++) {
    const txt = await sections[i].innerText();
    if (txt.includes('Bảng 2.8')) { found = i; break; }
  }
  if (found >= 0) {
    await sections[found].screenshot({ path: path.join(OUT, 'wave4-bang-2-8.png') });
    console.log('saved wave4-bang-2-8.png (page', found + 1, ')');
  } else { console.log('Bảng 2.8 page not found'); }

  await browser.close();
})().catch(e => { console.error('ERR', e.message); process.exit(1); });
