/** Render 1 trang PDF → PNG qua pdf.js + Playwright (không cần poppler/LibreOffice). */
const fs = require('fs');
const path = require('path');
const { chromium } = require('/home/kitedev/projects/2026-Kite-Class-Platform/kiteclass/kiteclass-frontend/node_modules/@playwright/test');

const PDF = process.argv[2];
const OUT = process.argv[3];
const PAGE = parseInt(process.argv[4] || '1', 10);

(async () => {
  const b64 = fs.readFileSync(PDF).toString('base64');
  const browser = await chromium.launch();
  const page = await browser.newPage({ deviceScaleFactor: 2 });
  await page.setContent('<!doctype html><body style="margin:0;background:#fff"><canvas id="cv"></canvas></body>');
  await page.addScriptTag({ url: 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js' });
  await page.evaluate(async ({ b64, pageNo }) => {
    window.pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
    const bin = atob(b64); const arr = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i);
    const pdf = await window.pdfjsLib.getDocument({ data: arr }).promise;
    const pg = await pdf.getPage(pageNo);
    const vp = pg.getViewport({ scale: 2.0 });
    const cv = document.getElementById('cv');
    cv.width = vp.width; cv.height = vp.height;
    await pg.render({ canvasContext: cv.getContext('2d'), viewport: vp }).promise;
  }, { b64, pageNo: PAGE });
  await page.waitForTimeout(800);
  const cv = await page.$('#cv');
  await cv.screenshot({ path: OUT });
  console.log('saved', OUT);
  await browser.close();
})().catch(e => { console.error('ERR', e.message); process.exit(1); });
