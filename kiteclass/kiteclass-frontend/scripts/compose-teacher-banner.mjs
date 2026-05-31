/**
 * Compose hero banner cho giảng viên độc lập (thesis demo) — wave-thesis-4.
 *
 * Mode: SCENE + TEXT OVERLAY. Input = ảnh ChatGPT (scene người + nền + icon môn,
 * KHÔNG có text). Overlay text block trái (slogan + sub + CTA) + scrim gradient
 * trái để chữ dễ đọc. Output 1672×941 (16:9) PNG. Text HTML render dấu sắc nét.
 *
 * Khánh: ChatGPT đã bake full text → dùng thẳng `khanh/banner-quyet-tam.png`,
 * KHÔNG qua script này.
 *
 * Usage (từ kiteclass/kiteclass-frontend/):
 *   node scripts/compose-teacher-banner.mjs <ha|nhi>
 */
import { chromium } from '@playwright/test';
import { readFileSync, writeFileSync } from 'fs';
import { join } from 'path';

const REPO = join(process.cwd(), '..', '..');
const PORTRAIT_DIR = join(REPO, 'documents/08-thesis/portrait');
const OUT_DIR = join(PORTRAIT_DIR, 'banners');
const PUBLIC_DIR = join(process.cwd(), 'public/demo-banners');

const TEACHERS = {
  ha: {
    scene: 'ha/banner-quyet-tam-1.png',          // cô Hà giơ tay, nền xanh, công thức toán, trái trống
    out: 'co-ha-toan.png',
    accent: '#FDE68A',
    badge: '➗ Toán Tiểu học · Học thử miễn phí',
    line1: 'Lớp <span class="accent">Toán</span> Tiểu học',
    line2: 'Cô Nguyễn Thị Hà',
    sub: 'Xây nền tảng tư duy · Học qua trò chơi · Lớp nhỏ kèm sát từng em',
    scrim: 'rgba(30,58,138,.78)',                // navy-blue scrim
  },
  nhi: {
    scene: 'nhi/banner-hoa-1.png',               // thầy Nhì + 3 học sinh lab, nền xanh hóa học
    out: 'thay-nhi-hoa.png',
    accent: '#FDE68A',
    badge: '🧪 Hóa học THCS · Luyện thi vào 10',
    line1: 'Lớp <span class="accent">Hóa học</span>',
    line2: 'Thầy Nguyễn Đình Nhì',
    sub: 'Hiểu bản chất phản ứng · Thí nghiệm trực quan · Sẵn sàng thi vào 10',
    scrim: 'rgba(6,78,59,.74)',                  // green scrim
  },
};

const key = process.argv[2];
const cfg = TEACHERS[key];
if (!cfg) { console.error('teacher-key phải là: ha | nhi'); process.exit(1); }

const sceneData = 'data:image/png;base64,' + readFileSync(join(PORTRAIT_DIR, cfg.scene)).toString('base64');

const html = `<!doctype html><html><head><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap" rel="stylesheet">
<style>
 *{margin:0;padding:0;box-sizing:border-box;font-family:'Be Vietnam Pro',sans-serif}
 .banner{width:1672px;height:941px;position:relative;overflow:hidden}
 .scene{position:absolute;inset:0;width:100%;height:100%;object-fit:cover}
 /* scrim trái → chữ dễ đọc */
 .scrim{position:absolute;inset:0;background:linear-gradient(90deg,${cfg.scrim} 0%,${cfg.scrim.replace(/[\d.]+\)$/,'.55)')} 32%,transparent 58%)}
 .text{position:absolute;left:90px;top:0;bottom:0;width:760px;display:flex;flex-direction:column;justify-content:center;color:#fff;z-index:3}
 .badge{display:inline-block;background:rgba(255,255,255,.18);backdrop-filter:blur(4px);padding:11px 22px;border-radius:999px;font-size:24px;font-weight:600;margin-bottom:28px;width:fit-content}
 h1{font-size:76px;font-weight:800;line-height:1.08;letter-spacing:-1.5px;margin-bottom:24px;text-shadow:0 2px 20px rgba(0,0,0,.3)}
 h1 .accent{color:${cfg.accent}}
 .sub{font-size:30px;font-weight:400;color:rgba(255,255,255,.92);line-height:1.4;margin-bottom:40px;text-shadow:0 1px 10px rgba(0,0,0,.3)}
 .cta{display:inline-block;background:#F97316;color:#fff;font-weight:800;font-size:27px;text-transform:uppercase;letter-spacing:.5px;padding:22px 46px;border-radius:18px;box-shadow:0 12px 36px rgba(249,115,22,.5);width:fit-content}
</style></head><body>
<div class="banner">
 <img class="scene" src="${sceneData}">
 <div class="scrim"></div>
 <div class="text">
   <span class="badge">${cfg.badge}</span>
   <h1>${cfg.line1}<br>${cfg.line2}</h1>
   <div class="sub">${cfg.sub}</div>
   <span class="cta">Học thử miễn phí →</span>
 </div>
</div></body></html>`;

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1672, height: 941 }, deviceScaleFactor: 1 });
await page.setContent(html, { waitUntil: 'networkidle' });
await page.waitForTimeout(800);
const buf = await (await page.$('.banner')).screenshot();
await browser.close();
writeFileSync(join(OUT_DIR, cfg.out), buf);
writeFileSync(join(PUBLIC_DIR, cfg.out), buf);
console.log(`✓ banner ${key} → ${cfg.out} (${buf.length} bytes)`);
