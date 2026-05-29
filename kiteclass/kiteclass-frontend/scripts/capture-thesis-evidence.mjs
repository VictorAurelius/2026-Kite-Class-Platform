/**
 * Thesis evidence capture — Ch.3 (Khánh) + §4.2 (Hà free / Nhì paid)
 *
 * Viewport-only (đoạn đầu, KHÔNG fullPage scroll) per user direction 2026-05-30.
 * Real seeded tenant data via JWT inject + ?tenant override (landing SSR).
 *
 * Run: cd kiteclass/kiteclass-frontend && node scripts/capture-thesis-evidence.mjs
 * Requires: dev server :4700 + gateway :9000 + seeded 3 GV tenants.
 */
import { chromium } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const GW = 'http://localhost:9000';
const BASE = 'http://localhost:4700';
const OUT = path.resolve(__dirname, '../../../documents/08-thesis/evidence/demo-trio');
fs.mkdirSync(OUT, { recursive: true });

const TENANT = {
  khanh: '126eaa8c-1f63-4c30-81b5-a5921b384b3b',
  ha:    'ad0fa96e-af24-49cb-b3e5-19d44f182d85',
  nhi:   '0abe093c-4c66-4c99-abab-a756582dc60b',
};
const CRED = {
  khanh: ['khanh.do@gmail.com', 'Khanh@2026'],
  ha:    ['ha.nguyen@gmail.com', 'HaToan@2026'],
  nhi:   ['nhi.nguyen@gmail.com', 'Nhi@2026'],
};

async function login(t) {
  const [email, password] = CRED[t];
  const r = await fetch(`${GW}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Id': TENANT[t] },
    body: JSON.stringify({ email, password }),
  });
  if (!r.ok) throw new Error(`login ${t} HTTP ${r.status}`);
  return r.json();
}

// Capture ONLY pages for the tenant the dev server env (NEXT_PUBLIC_TENANT_ID) is
// pinned to — so layout header + page hero both match the tenant (no ?tenant bleed).
// Homepage path = '/' (env-based). Run per-tenant after restarting dev with that env.
const ONLY = process.argv[2]; // 'khanh' | 'ha' | 'nhi'
// file, path, tenant, auth(bool)
const ALL_JOBS = [
  // Ch.3 — Khánh (Pháp luật THPT, navy+gold)
  { f: '12-public-homepage-sky-branded.png',     p: '/',          t: 'khanh', auth: false },
  { f: '01-login-page.png',                       p: '/login',     t: 'khanh', auth: false },
  { f: '02-dashboard-overview-kpi-orange.png',    p: '/dashboard', t: 'khanh', auth: true },
  { f: '03-branding-settings.png',                p: '/branding',  t: 'khanh', auth: true },
  { f: '05-students.png',                         p: '/students',  t: 'khanh', auth: true },
  // §4.2 — Hà (Toán Tiểu học, free, blue) + Nhì (Hóa THCS, paid, green)
  { f: 'ha-homepage-blue-branded.png',            p: '/',          t: 'ha',  auth: false },
  { f: 'ha-dashboard.png',                        p: '/dashboard', t: 'ha',  auth: true },
  { f: 'nhi-homepage-green-branded.png',          p: '/',          t: 'nhi', auth: false },
  { f: 'nhi-dashboard.png',                       p: '/dashboard', t: 'nhi', auth: true },
];
const JOBS = ONLY ? ALL_JOBS.filter((j) => j.t === ONLY) : ALL_JOBS;

const tokens = {};
async function tokenFor(t) {
  if (!tokens[t]) tokens[t] = await login(t);
  return tokens[t];
}

const main = async () => {
  // --disable-web-security: dev FE port :4700 not in gateway CORS allowlist (403);
  // bypass CORS for headless capture only — data is REAL (gateway API verified working).
  const browser = await chromium.launch({
    args: ['--disable-web-security', '--disable-features=IsolateOrigins,site-per-process'],
  });
  const results = [];
  for (const job of JOBS) {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await ctx.newPage();
    try {
      if (job.auth) {
        const a = await tokenFor(job.t);
        const authStorage = JSON.stringify({
          state: { user: a.user, accessToken: a.accessToken, refreshToken: a.refreshToken, tenantId: TENANT[job.t], isAuthenticated: true },
          version: 0,
        });
        await page.addInitScript(([as, tid, at, rt]) => {
          // Zustand UI auth state (renders authenticated layout)
          localStorage.setItem('auth-storage', as);
          // Flat keys read by api-client.ts request interceptor (Authorization + X-Tenant-Id)
          localStorage.setItem('accessToken', at);
          localStorage.setItem('refreshToken', rt);
          localStorage.setItem('tenantId', tid);
        }, [authStorage, TENANT[job.t], a.accessToken, a.refreshToken]);
      }
      await page.goto(`${BASE}${job.p}`, { waitUntil: 'networkidle', timeout: 30000 });
      await page.waitForTimeout(2500); // content + count-up animation
      // dismiss Next dev overlay if present
      try {
        const btn = page.locator('nextjs-portal button[aria-label="Close"]');
        if (await btn.isVisible({ timeout: 300 })) await btn.click();
      } catch {}
      const out = path.join(OUT, job.f);
      await page.screenshot({ path: out, fullPage: false }); // viewport top only
      const kb = Math.round(fs.statSync(out).size / 1024);
      results.push(`  ✓ ${job.f} (${kb}KB) [${job.t}${job.auth ? '/auth' : ''}]`);
    } catch (e) {
      results.push(`  ✗ ${job.f}: ${String(e.message).slice(0, 90)}`);
    } finally {
      await ctx.close();
    }
  }
  await browser.close();
  console.log('\n=== Thesis evidence capture (viewport-only) ===');
  console.log(results.join('\n'));
  console.log(`\n📁 ${OUT}`);
};
main().catch((e) => { console.error(e); process.exit(1); });
