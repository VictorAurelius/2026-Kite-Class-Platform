/**
 * Compose hero banner cho giảng viên độc lập (thesis demo) — wave-thesis-4.
 *
 * Banner = 3 lớp (per memory feedback_thesis_banner_html_compose):
 *   (1) text slogan/CTA (HTML render — dấu tiếng Việt sắc nét)
 *   (2) ảnh portrait (chroma-key nền xanh studio → trong suốt)
 *   (3) icon chủ đề theo môn (emoji, opacity thấp)
 *
 * Pipeline: PIL chroma-key (python) → HTML setContent → Playwright screenshot
 *   2400×1260 (deviceScaleFactor 2) → PNG. KHÔNG dùng AI image-gen (garble dấu).
 *
 * CHỈ áp dụng cho portrait nền đơn sắc xanh studio (thầy Nhì, cô Hà).
 * Cô Khánh nền poster phức tạp → cần ảnh ChatGPT-gen (Prompt A/B per
 * banner-prompts-and-design-spec.md), KHÔNG chroma-key được.
 *
 * Usage (từ kiteclass/kiteclass-frontend/, cần @playwright/test):
 *   node ../../documents/08-thesis/scripts/compose-teacher-banner.mjs <teacher-key>
 *   teacher-key ∈ { nhi, ha }
 */
import { chromium } from '@playwright/test';
import { readFileSync, writeFileSync, mkdtempSync } from 'fs';
import { execFileSync } from 'child_process';
import { tmpdir } from 'os';
import { join } from 'path';

const REPO = join(process.cwd(), '..', '..');
const PORTRAIT_DIR = join(REPO, 'documents/08-thesis/portrait');
const OUT_DIR = join(PORTRAIT_DIR, 'banners');
const PUBLIC_DIR = join(process.cwd(), 'public/demo-banners');

const TEACHERS = {
  nhi: {
    portrait: 'Nguyễn Đình Nhì - THCS - Hóa Học.png',
    out: 'thay-nhi-hoa.png',
    grad: ['#16A34A', '#0f7a37', '#0a5527'],
    badge: '🧪 Luyện thi vào 10 · Hóa học THCS',
    line1: 'Lớp <span class="accent">Hóa học</span>',
    line2: 'Thầy Nguyễn Đình Nhì',
    sub: 'Hiểu bản chất phản ứng · Phương pháp hệ thống · Sẵn sàng thi vào lớp 10',
    icons: [['⚗️', 'i1'], ['🧪', 'i2'], ['⚛️', 'i3'], ['🔬', 'i4']],
  },
  ha: {
    portrait: 'Nguyễn Thị Hà - Tiểu Học - Toán Học.png',
    out: 'co-ha-toan.png',
    grad: ['#2563EB', '#1d4ed8', '#1e3a8a'],
    badge: '➗ Toán Tiểu học · Học thử miễn phí',
    line1: 'Lớp <span class="accent">Toán</span> Tiểu học',
    line2: 'Cô Nguyễn Thị Hà',
    sub: 'Xây nền tảng tư duy · Học qua trò chơi · Lớp nhỏ kèm sát từng em',
    icons: [['➗', 'i1'], ['📐', 'i2'], ['🔢', 'i3'], ['✏️', 'i4']],
  },
};

const key = process.argv[2];
const cfg = TEACHERS[key];
if (!cfg) { console.error('teacher-key phải là: nhi | ha'); process.exit(1); }

// --- Lớp 2: chroma-key nền xanh studio → PNG trong suốt ---
const tmp = mkdtempSync(join(tmpdir(), 'banner-'));
const cutout = join(tmp, 'cutout.png');
const py = `
from PIL import Image
im = Image.open(${JSON.stringify(join(PORTRAIT_DIR, cfg.portrait))}).convert("RGBA")
px = im.load(); W,H = im.size
for y in range(H):
  for x in range(W):
    r,g,b,a = px[x,y]
    if b>120 and (b-r)>40 and (b-g)>25:
      px[x,y]=(r,g,b,0)
im.save(${JSON.stringify(cutout)})
`;
execFileSync('python3', ['-c', py]);
const portraitData = 'data:image/png;base64,' + readFileSync(cutout).toString('base64');

const iconCss = {
  i1: 'font-size:220px;left:-30px;top:-40px', i2: 'font-size:150px;right:380px;top:30px',
  i3: 'font-size:120px;left:120px;bottom:-20px', i4: 'font-size:90px;right:60px;bottom:40px',
};
const iconsHtml = cfg.icons.map(([e, c]) => `<span class="icon" style="${iconCss[c]}">${e}</span>`).join('');

const html = `<!doctype html><html><head><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;600;700;800&display=swap" rel="stylesheet">
<style>
 *{margin:0;padding:0;box-sizing:border-box;font-family:'Be Vietnam Pro',sans-serif}
 .banner{width:1200px;height:630px;position:relative;overflow:hidden;
   background:linear-gradient(125deg,${cfg.grad[0]} 0%,${cfg.grad[1]} 55%,${cfg.grad[2]} 100%)}
 .icon{position:absolute;opacity:.09;color:#fff}
 .glow{position:absolute;right:-120px;bottom:-160px;width:560px;height:560px;border-radius:50%;
   background:radial-gradient(circle,rgba(255,255,255,.35) 0%,transparent 70%);filter:blur(40px)}
 .text{position:absolute;left:70px;top:0;bottom:0;width:620px;display:flex;flex-direction:column;justify-content:center;color:#fff;z-index:3}
 .badge{display:inline-block;background:rgba(255,255,255,.16);padding:9px 18px;border-radius:999px;font-size:18px;font-weight:600;margin-bottom:22px;width:fit-content}
 h1{font-size:56px;font-weight:800;line-height:1.08;letter-spacing:-1px;margin-bottom:18px}
 h1 .accent{color:#FDE68A}
 .sub{font-size:22px;font-weight:400;color:rgba(255,255,255,.9);line-height:1.4;margin-bottom:28px}
 .cta{display:inline-block;background:#F97316;color:#fff;font-weight:800;font-size:20px;text-transform:uppercase;letter-spacing:.5px;padding:16px 34px;border-radius:14px;box-shadow:0 10px 30px rgba(249,115,22,.45);width:fit-content}
 .portrait{position:absolute;right:30px;bottom:0;height:600px;z-index:2;filter:drop-shadow(0 18px 40px rgba(0,0,0,.35))}
</style></head><body>
<div class="banner">
 ${iconsHtml}
 <div class="glow"></div>
 <img class="portrait" src="${portraitData}">
 <div class="text">
   <span class="badge">${cfg.badge}</span>
   <h1>${cfg.line1}<br>${cfg.line2}</h1>
   <div class="sub">${cfg.sub}</div>
   <span class="cta">Học thử miễn phí →</span>
 </div>
</div></body></html>`;

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1200, height: 630 }, deviceScaleFactor: 2 });
await page.setContent(html, { waitUntil: 'networkidle' });
await page.waitForTimeout(800);
const buf = await (await page.$('.banner')).screenshot();
await browser.close();
writeFileSync(join(OUT_DIR, cfg.out), buf);
writeFileSync(join(PUBLIC_DIR, cfg.out), buf);
console.log(`✓ banner ${key} → ${cfg.out} (${buf.length} bytes)`);
