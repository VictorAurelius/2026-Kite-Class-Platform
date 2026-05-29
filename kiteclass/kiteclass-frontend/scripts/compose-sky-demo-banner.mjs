// Demo banner composer — "code tạo ảnh riêng" (GAP-810).
//
// Renders the Sky Education promo hero banner (slogan + teacher portrait + CTA)
// from an HTML template via headless Chromium, then writes an optimized WebP.
// This is the *generation* code (committed, reproducible). The produced binary
// (public/demo/sky/hero-banner.webp) is gitignored/local-only per GAP-810 — the
// landing seed (BrandingDataSeeder.seedSkyLanding) references only the URL string.
//
// Why HTML-compose instead of AI image-gen: Vietnamese diacritics render crisp +
// deterministic + $0; AI text-in-image garbles dấu. AI is only good for textless
// backgrounds. This proves the Phase 2 "template-composer" direction for AI Branding.
//
// Inputs  (local, gitignored): public/demo/sky/teacher-do-lan-khanh.webp
// Output  (local, gitignored): public/demo/sky/hero-banner.webp (1200x630, OG-ready)
// Run:    node scripts/compose-sky-demo-banner.mjs   (from kiteclass-frontend/)

import { chromium } from 'playwright';
import sharp from 'sharp';
import { writeFileSync, readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const ASSET_DIR = resolve(here, '../public/demo/sky');
const teacherPath = resolve(ASSET_DIR, 'teacher-do-lan-khanh.webp');
const outPath = resolve(ASSET_DIR, 'hero-banner.webp');

if (!existsSync(teacherPath)) {
  console.error(`Missing teacher portrait: ${teacherPath}\n(local-only asset per GAP-810 — restore it before composing).`);
  process.exit(2);
}

// Embed the portrait as a data URI so the render is self-contained (no file:// race).
const teacherDataUri = `data:image/webp;base64,${readFileSync(teacherPath).toString('base64')}`;

const SLOGAN_LINE_1 = 'Mất gốc tiếng Anh?';
const SLOGAN_LINE_2 = 'Đã có cô Khánh';
const TAGLINE = 'Chắp cánh tương lai Anh ngữ — lộ trình cho người mới bắt đầu';
const CTA = 'Đăng ký học thử';
const TEACHER_NAME = 'Cô Đỗ Lan Khánh';
const TEACHER_ROLE = 'Giáo viên Tiếng Anh';

const html = `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap" rel="stylesheet">
<style>
*{margin:0;padding:0;box-sizing:border-box}html,body{width:1200px;height:630px}
.banner{width:1200px;height:630px;position:relative;overflow:hidden;font-family:'Be Vietnam Pro',sans-serif;
 background:linear-gradient(125deg,#13293d 0%,#1B4965 52%,#235a7d 100%)}
.glow{position:absolute;right:-120px;bottom:-160px;width:560px;height:560px;
 background:radial-gradient(circle,#FFB703 0%,rgba(255,183,3,0) 70%);opacity:.45}
.accent{position:absolute;top:-80px;right:280px;width:420px;height:420px;
 background:radial-gradient(circle,#E8590C 0%,rgba(232,89,12,0) 68%);opacity:.35}
.left{position:absolute;left:72px;top:0;width:660px;height:100%;display:flex;flex-direction:column;justify-content:center}
.brand{display:flex;align-items:center;gap:12px;margin-bottom:30px}
.brand .mark{width:34px;height:34px;border-radius:9px;background:linear-gradient(135deg,#FF7A2E,#E8590C);
 display:flex;align-items:center;justify-content:center;box-shadow:0 4px 14px rgba(232,89,12,.4)}
.brand .mark span{color:#fff;font-weight:800;font-size:20px}
.brand .name{color:#fff;font-weight:700;font-size:21px;letter-spacing:.3px}.brand .name b{color:#FFB703}
h1{color:#fff;font-weight:800;font-size:58px;line-height:1.12;letter-spacing:-.5px}
h1 .hl{background:linear-gradient(90deg,#FFB703,#FF7A2E);-webkit-background-clip:text;background-clip:text;color:transparent}
.sub{color:#cfe0ea;font-weight:400;font-size:22px;margin-top:20px}
.cta{margin-top:38px;display:inline-flex;align-items:center;gap:12px;align-self:flex-start;
 background:linear-gradient(135deg,#FF7A2E,#E8590C);color:#fff;font-weight:700;font-size:21px;
 padding:17px 34px;border-radius:999px;box-shadow:0 10px 26px rgba(232,89,12,.45)}
.photo-wrap{position:absolute;right:70px;top:50%;transform:translateY(-50%);width:392px;height:392px}
.ring{position:absolute;inset:-10px;border-radius:50%;
 background:conic-gradient(from 200deg,#FFB703,#E8590C,#FFB703);filter:blur(2px);opacity:.9}
.photo{position:absolute;inset:0;border-radius:50%;overflow:hidden;border:6px solid rgba(255,255,255,.9);box-shadow:0 20px 50px rgba(0,0,0,.4)}
.photo img{width:100%;height:100%;object-fit:cover;object-position:center top}
.badge{position:absolute;bottom:6px;left:50%;transform:translateX(-50%);background:#fff;border-radius:14px;
 padding:10px 20px;text-align:center;box-shadow:0 8px 22px rgba(0,0,0,.25);white-space:nowrap}
.badge .t{color:#13293d;font-weight:800;font-size:18px}.badge .s{color:#E8590C;font-weight:600;font-size:13px;margin-top:2px}
</style></head><body><div class="banner">
<div class="glow"></div><div class="accent"></div>
<div class="left">
 <div class="brand"><div class="mark"><span>S</span></div><div class="name">Sky <b>Education</b></div></div>
 <h1>${SLOGAN_LINE_1}<br><span class="hl">${SLOGAN_LINE_2}</span></h1>
 <div class="sub">${TAGLINE}</div>
 <div class="cta">${CTA} <span>→</span></div>
</div>
<div class="photo-wrap"><div class="ring"></div>
 <div class="photo"><img src="${teacherDataUri}" alt="${TEACHER_NAME}"></div>
 <div class="badge"><div class="t">${TEACHER_NAME}</div><div class="s">${TEACHER_ROLE}</div></div>
</div></div></body></html>`;

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1200, height: 630 }, deviceScaleFactor: 2 });
await page.setContent(html, { waitUntil: 'networkidle' });
await page.evaluate(() => document.fonts.ready);
await page.waitForTimeout(400);
const png = await page.screenshot({ clip: { x: 0, y: 0, width: 1200, height: 630 } });
await browser.close();

await sharp(png).resize(1200, 630).webp({ quality: 88 }).toFile(outPath);
console.log(`Composed banner → ${outPath}`);
