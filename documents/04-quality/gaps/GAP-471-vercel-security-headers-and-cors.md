# GAP-471: Vercel Production Frontend missing 4/5 Security Headers + CORS Wildcard

**Status:** 🟡 PARTIAL 2026-05-11 (Wave 61 Bucket E — `vercel.json` `headers` shipped cho cả 2 FE + CORS scoped về static assets; live `curl -sI` verify deferred to Bucket A cutover)
**Priority:** 🟠 P1 → promoted 🔴 P0 (Wave 61 Bucket E pre-cutover guard)
**Domain:** Frontend / Security
**Found:** 2026-05-11 (Wave 60 Bucket A pen-test self-audit — live probe `curl -sI https://kitehub.me/`)
**Affects:** Phase 1 BETA cohort frontend kitehub.me + future PAID tenants

## Problem

Live probe production frontend xác nhận chỉ 1/5 security headers PASS (HSTS only). CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy đều missing. Thêm vào đó `access-control-allow-origin: *` (CORS wildcard) trên public asset routes.

## Evidence

```bash
$ curl -sI https://kitehub.me/ | head -25
HTTP/2 200
access-control-allow-origin: *                        # ⚠️ CORS WILDCARD
content-type: text/html; charset=utf-8
strict-transport-security: max-age=63072000           # ✅ HSTS 2 năm
server: Vercel
# ❌ MISSING: Content-Security-Policy
# ❌ MISSING: X-Frame-Options
# ❌ MISSING: X-Content-Type-Options
# ❌ MISSING: Referrer-Policy
```

`/login` cũng cùng pattern thiếu headers.

## Root Cause

- Vercel default chỉ inject HSTS cho HTTPS domains; phần lại phụ thuộc app config
- `vercel.json` chưa cấu hình `headers` array
- `next.config.js` chưa cấu hình `async headers()`
- CORS wildcard ổn cho static asset (favicon, CSS, JS public) NHƯNG dangerous khi cutover BE qua subdomain api.kitehub.me

## Proposed Fix

Add `vercel.json` (hoặc `next.config.js` headers) cho cả 2 FE deploys (kitehub-frontend + kiteclass-frontend):

```json
{
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        {
          "key": "Content-Security-Policy",
          "value": "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://api.kitehub.me https://*.vercel.app; frame-ancestors 'none';"
        },
        { "key": "X-Frame-Options", "value": "DENY" },
        { "key": "X-Content-Type-Options", "value": "nosniff" },
        { "key": "Referrer-Policy", "value": "strict-origin-when-cross-origin" },
        { "key": "Permissions-Policy", "value": "camera=(), microphone=(), geolocation=()" }
      ]
    }
  ]
}
```

CSP cần iterate trên staging trước (Next.js inline scripts có thể bị block); start với `Content-Security-Policy-Report-Only` mode.

CORS wildcard: scope giới hạn về `/_next/static/*` only; non-asset routes default deny.

## Acceptance Criteria

- [x] `vercel.json` thêm 6 headers (CSP enforce + HSTS + X-Frame-Options DENY + X-Content-Type-Options nosniff + Referrer-Policy + Permissions-Policy) cho kitehub-frontend + kiteclass-frontend (Wave 61 Bucket E)
- [x] CORS scope chỉ `*` cho `/_next/static/*` + static assets matched bằng extension regex (PNG/JPG/SVG/font…); root `/(.*)` không set CORS wildcard → mặc định same-origin
- [ ] CSP deploy report-only mode đầu tiên, monitor 48h, sau đó enforce — **không áp dụng:** ship CSP enforce ngay vì script-src đã whitelist `'unsafe-inline' 'unsafe-eval'` (Next.js inline hydration). Nếu phát hiện block sau cutover → rollback bằng PR riêng đổi `Content-Security-Policy` → `Content-Security-Policy-Report-Only`.
- [ ] Re-probe `curl -sI https://kitehub.me/` xác nhận 6/6 headers PASS — **deferred:** phụ thuộc Bucket A DNS cutover; sẽ verify trong Wave 62 closure
- [ ] Update audit `documents/04-quality/audits/security/2026-XX-headers-reverify.md` post-fix — **deferred:** post-cutover

## Related

- 2026-05-11 pen-test audit P1-A
- `documents/05-guides/security/owasp-top-10-baseline.md` §7 headers checklist
- `release-deploy-standard.md` §3.4 cổng MAJOR pen-test light gate
- OWASP Secure Headers Project

## Log

- **2026-05-11** Filed by Wave 60 Bucket A pen-test self-audit (GAP-406 follow-up). Phase 1 BETA mitigation: invite-only cohort + Vercel HSTS protect downgrade attacks. Promote P0 khi v1.0.0 PRODUCTION cutover.
- **2026-05-11** Wave 61 Bucket E — `vercel.json` shipped cho `kitehub-frontend` + `kiteclass-frontend` với 6-header bundle (CSP enforce + HSTS preload 2-year + X-Frame-Options DENY + nosniff + Referrer-Policy + Permissions-Policy). CSP `connect-src` whitelist gồm `https://api.kitehub.me` + `https://*.kitehub.me` + `https://*.vercel.app` cho preview deploys. CORS wildcard chỉ apply cho `/_next/static/*` + static asset extensions; root `/(.*)` mặc định same-origin. Status PARTIAL vì live `curl -sI` verify cần Bucket A DNS cutover trước (`api.kitehub.me` chưa resolve).
