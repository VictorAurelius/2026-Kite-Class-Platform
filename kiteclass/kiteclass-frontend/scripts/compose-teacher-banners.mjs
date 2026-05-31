// Solo-teacher demo banner composer — HTML-compose → headless Chromium → PNG.
// Renders 3 promo hero banners (slogan + portrait + CTA) for the 3 independent
// teachers used in thesis evidence (Ch.3 + §4.2). HTML-compose (not AI image-gen)
// keeps Vietnamese diacritics crisp + deterministic + $0 (per GAP-810 direction).
//
// Input  : documents/08-thesis/portrait/<name>.png  (committed)
// Output : documents/08-thesis/portrait/banners/<slug>.png  (1200x630, OG-ready)
// Run    : node scripts/compose-teacher-banners.mjs   (from kiteclass-frontend/)

import { chromium } from '@playwright/test';
import { writeFileSync, readFileSync, existsSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const PORTRAIT_DIR = resolve(here, '../../../documents/08-thesis/portrait');
const OUT_DIR = resolve(PORTRAIT_DIR, 'banners');
mkdirSync(OUT_DIR, { recursive: true });

// 3 giảng viên độc lập — thông tin từ tên file ảnh (authoritative).
const TEACHERS = [
  {
    slug: 'co-khanh-phapluat',
    portrait: 'Đỗ Lan Khánh - THPT - Pháp Luật và Đời Sống.png',
    brand: 'Lớp Cô Khánh',
    name: 'Cô Đỗ Lan Khánh',
    role: 'Giáo viên Pháp luật & Đời sống · THPT',
    s1: 'Pháp luật & Đời sống',
    s2: 'Hiểu luật, vững bước vào đời',
    tagline: 'Kiến thức pháp luật thực tiễn cho học sinh THPT',
    cta: 'Đăng ký học thử',
    // navy + gold (uy tín, pháp lý)
    bg: 'linear-gradient(125deg,#13293d 0%,#1B4965 52%,#235a7d 100%)',
    glow: '#FFB703', accent: '#E8590C', hl: 'linear-gradient(90deg,#FFB703,#FF7A2E)',
    ctaBg: 'linear-gradient(135deg,#FF7A2E,#E8590C)', markBg: 'linear-gradient(135deg,#FF7A2E,#E8590C)',
    badgeAccent: '#E8590C', objPos: 'center top',
  },
  {
    slug: 'co-ha-toan',
    portrait: 'Nguyễn Thị Hà - Tiểu Học - Toán Học.png',
    brand: 'Toán Cô Hà',
    name: 'Cô Nguyễn Thị Hà',
    role: 'Giáo viên Toán · Tiểu học',
    s1: 'Yêu Toán',
    s2: 'từ những bước đầu tiên',
    tagline: 'Toán tư duy nhẹ nhàng cho học sinh tiểu học',
    cta: 'Đăng ký học thử',
    // blue + cyan (thân thiện, trẻ em)
    bg: 'linear-gradient(125deg,#0B3C5D 0%,#1769AA 52%,#2A9DC9 100%)',
    glow: '#36D1DC', accent: '#1769AA', hl: 'linear-gradient(90deg,#7DE3F0,#36D1DC)',
    ctaBg: 'linear-gradient(135deg,#2AA8D8,#1769AA)', markBg: 'linear-gradient(135deg,#36D1DC,#1769AA)',
    badgeAccent: '#1769AA', objPos: 'center top',
  },
  {
    slug: 'thay-nhi-hoa',
    portrait: 'Nguyễn Đình Nhì - THCS - Hóa Học.png',
    brand: 'Hóa Thầy Nhì',
    name: 'Thầy Nguyễn Đình Nhì',
    role: 'Giáo viên Hóa học · THCS',
    s1: 'Hóa học',
    s2: 'thật gần gũi & dễ hiểu',
    tagline: 'Chinh phục Hóa học bậc THCS qua thí nghiệm trực quan',
    cta: 'Đăng ký học thử',
    // green + lime (hóa học)
    bg: 'linear-gradient(125deg,#0F3D2E 0%,#15803D 52%,#22A35A 100%)',
    glow: '#A3E635', accent: '#15803D', hl: 'linear-gradient(90deg,#BEF264,#A3E635)',
    ctaBg: 'linear-gradient(135deg,#4ADE80,#15803D)', markBg: 'linear-gradient(135deg,#A3E635,#15803D)',
    badgeAccent: '#15803D', objPos: 'center top',
  },
];

function buildHtml(t, dataUri) {
  const initial = t.brand.trim().charAt(0);
  return `<!DOCTYPE html><html lang="vi"><head><meta charset="utf-8">
<link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap" rel="stylesheet">
<style>
*{margin:0;padding:0;box-sizing:border-box}html,body{width:1200px;height:630px}
.banner{width:1200px;height:630px;position:relative;overflow:hidden;font-family:'Be Vietnam Pro',sans-serif;background:${t.bg}}
.glow{position:absolute;right:-120px;bottom:-160px;width:560px;height:560px;background:radial-gradient(circle,${t.glow} 0%,rgba(255,255,255,0) 70%);opacity:.42}
.accent{position:absolute;top:-80px;right:280px;width:420px;height:420px;background:radial-gradient(circle,${t.accent} 0%,rgba(255,255,255,0) 68%);opacity:.32}
.left{position:absolute;left:72px;top:0;width:660px;height:100%;display:flex;flex-direction:column;justify-content:center}
.brand{display:flex;align-items:center;gap:12px;margin-bottom:28px}
.brand .mark{width:34px;height:34px;border-radius:9px;background:${t.markBg};display:flex;align-items:center;justify-content:center;box-shadow:0 4px 14px rgba(0,0,0,.3)}
.brand .mark span{color:#fff;font-weight:800;font-size:20px}
.brand .name{color:#fff;font-weight:700;font-size:21px;letter-spacing:.3px}
h1{color:#fff;font-weight:800;font-size:56px;line-height:1.12;letter-spacing:-.5px}
h1 .hl{background:${t.hl};-webkit-background-clip:text;background-clip:text;color:transparent}
.sub{color:#dbe7ef;font-weight:400;font-size:21px;margin-top:20px;max-width:600px}
.cta{margin-top:36px;display:inline-flex;align-items:center;gap:12px;align-self:flex-start;background:${t.ctaBg};color:#fff;font-weight:700;font-size:21px;padding:17px 34px;border-radius:999px;box-shadow:0 10px 26px rgba(0,0,0,.35)}
.photo-wrap{position:absolute;right:70px;top:50%;transform:translateY(-50%);width:392px;height:392px}
.ring{position:absolute;inset:-10px;border-radius:50%;background:conic-gradient(from 200deg,${t.glow},${t.accent},${t.glow});filter:blur(2px);opacity:.9}
.photo{position:absolute;inset:0;border-radius:50%;overflow:hidden;border:6px solid rgba(255,255,255,.92);box-shadow:0 20px 50px rgba(0,0,0,.4)}
.photo img{width:100%;height:100%;object-fit:cover;object-position:${t.objPos}}
.badge{position:absolute;bottom:6px;left:50%;transform:translateX(-50%);background:#fff;border-radius:14px;padding:10px 20px;text-align:center;box-shadow:0 8px 22px rgba(0,0,0,.25);white-space:nowrap}
.badge .t{color:#13293d;font-weight:800;font-size:18px}.badge .s{color:${t.badgeAccent};font-weight:600;font-size:13px;margin-top:2px}
</style></head><body><div class="banner">
<div class="glow"></div><div class="accent"></div>
<div class="left">
 <div class="brand"><div class="mark"><span>${initial}</span></div><div class="name">${t.brand}</div></div>
 <h1>${t.s1}<br><span class="hl">${t.s2}</span></h1>
 <div class="sub">${t.tagline}</div>
 <div class="cta">${t.cta} <span>→</span></div>
</div>
<div class="photo-wrap"><div class="ring"></div>
 <div class="photo"><img src="${dataUri}" alt="${t.name}"></div>
 <div class="badge"><div class="t">${t.name}</div><div class="s">${t.role}</div></div>
</div></div></body></html>`;
}

const browser = await chromium.launch();
for (const t of TEACHERS) {
  const pPath = resolve(PORTRAIT_DIR, t.portrait);
  if (!existsSync(pPath)) { console.error(`MISSING portrait: ${pPath}`); continue; }
  const ext = t.portrait.toLowerCase().endsWith('.png') ? 'png' : 'jpeg';
  const dataUri = `data:image/${ext};base64,${readFileSync(pPath).toString('base64')}`;
  const page = await browser.newPage({ viewport: { width: 1200, height: 630 }, deviceScaleFactor: 2 });
  await page.setContent(buildHtml(t, dataUri), { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(400);
  const png = await page.screenshot({ clip: { x: 0, y: 0, width: 1200, height: 630 } });
  await page.close();
  const out = resolve(OUT_DIR, `${t.slug}.png`);
  writeFileSync(out, png);
  console.log(`✓ ${t.name} → ${out}`);
}
await browser.close();
console.log('Done — 3 banners in documents/08-thesis/portrait/banners/');
