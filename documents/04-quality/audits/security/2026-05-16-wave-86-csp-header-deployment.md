---
title: Security — CSP Header Deployment (Wave 86 Bucket E Fix 3)
status: complete
created: 2026-05-16
phase: phase-1-beta
wave: 86
gaps: [GAP-NEW-csp-header]
---

# Security Audit — CSP Header Deployment

## Scope

Wave 86 Bucket E Fix 3 — deploy Content-Security-Policy + companion security headers tới cả hai Next.js frontends (`kitehub-frontend` + `kiteclass-frontend`) per OWASP A05 Security Misconfiguration + threat-model `2026-05-16-auth-flow-magic-link.md` §I3 (Referrer-Policy).

**Mode chosen:** `Content-Security-Policy-Report-Only` (NOT enforce).

**Rationale Phase 1 BETA:**
- Beta user cohort small (~5 tenants); breakage = high-touch support
- CSP unknowns: 3rd-party widgets (Vercel Live preview, Vercel Analytics, future Sentry/Tally embed) may need allowlist additions
- Report-Only lets browser report violations via console + future `report-uri` endpoint without blocking
- Flip to enforce mode (`Content-Security-Policy`) after 1 week zero P0 violation collected from beta

## Headers shipped

### kitehub-frontend + kiteclass-frontend (both apps)

Path: applied to `/(.*)` global source — every route.

| Header | Value | Rationale |
|--------|-------|-----------|
| `Content-Security-Policy-Report-Only` | (see directives below) | OWASP A05 + threat-model |
| `X-Frame-Options` | `DENY` | Clickjacking — covers legacy browsers; CSP `frame-ancestors 'none'` covers modern |
| `X-Content-Type-Options` | `nosniff` | MIME-type confusion attacks |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Threat-model `2026-05-16-auth-flow-magic-link.md` §I3 — strip referer cross-origin |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=(), payment=()` | Disable powerful features not used by KiteHub Phase 1 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HSTS 1y; CF Phase 1.5 will add `preload` after stable |

### CSP directives (shared between both apps, domain whitelist differs)

```
default-src 'self';
script-src 'self' 'unsafe-inline' 'unsafe-eval' https://vercel.live https://*.vercel-scripts.com https://va.vercel-scripts.com;
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
img-src 'self' data: https: blob: [https://cdn.kiteclass.com for kc];
font-src 'self' https://fonts.gstatic.com data:;
connect-src 'self' https://<app>.com https://*.<app>.com wss://*.<app>.com https://vercel.live https://vitals.vercel-insights.com;
frame-ancestors 'none';
base-uri 'self';
form-action 'self';
object-src 'none';
[worker-src 'self' blob: for kc PWA SW];
upgrade-insecure-requests;
```

Differences per app:
- `kitehub-frontend`: `connect-src` includes `kitehub.me` domain
- `kiteclass-frontend`: `connect-src` includes `kiteclass.com`; adds `worker-src 'self' blob:` for PWA service-worker (Wave 49 Bucket 0)

## Known caveats — Report-Only mode

- **`'unsafe-inline'` in `style-src`:** Next.js inlines critical CSS for performance. Phase 1.5 path: migrate to nonce-based CSP per Next.js 14+ Server Components.
- **`'unsafe-eval'` in `script-src`:** required by some Next dev tooling + Recharts. Audit Phase 1.5 to remove if not needed in production build.
- **No `report-uri` / `report-to` endpoint:** browser console violations only Phase 1 BETA. Phase 1.5 wire reports to Sentry-style sink.

## Verification

```bash
# After Vercel redeploy, verify headers live:
curl -sI https://kitehub.me/ | grep -iE "content-security|x-frame|referrer|permissions|strict-transport"
curl -sI https://kiteclass.com/ | grep -iE "content-security|x-frame|referrer|permissions|strict-transport"
```

Expected: 5 security headers present including `Content-Security-Policy-Report-Only`.

## Phase 1.5 enforcement path

1. **Week 1 post-deploy:** Monitor browser console (manual sample 3 tenants) for CSP violation reports
2. **Week 2:** If zero P0 violations, file PR flipping header name `Content-Security-Policy-Report-Only` → `Content-Security-Policy`
3. **Week 3:** Add `report-to` directive + Sentry endpoint
4. **Wave 87+:** Migrate to nonce-based CSP, drop `'unsafe-inline'` `'unsafe-eval'`

## Self-test (pre-handoff per pre-handoff-self-test-completeness.md §2.2)

| Check | Verdict |
|---|---|
| (a) Both `next.config.js` files have `headers()` function returning CSP | ✅ |
| (b) Phase 1 mode = Report-Only (no breakage risk) | ✅ |
| (c) CSP directives cover script + style + img + font + connect + frame-ancestors + form-action | ✅ |
| (d) 5 companion headers (XFO, XCTO, Referrer, Permissions, HSTS) present | ✅ |
| (e) Build verify: `pnpm build` next.config valid | ⏳ Deferred to CI (worktree pnpm not available) |
| (f) Documented enforcement flip path | ✅ §Phase 1.5 above |

## References

- OWASP Top 10 A05 — Security Misconfiguration
- OWASP CSP Cheat Sheet — https://cheatsheetseries.owasp.org/cheatsheets/Content_Security_Policy_Cheat_Sheet.html
- Next.js Headers — https://nextjs.org/docs/app/api-reference/config/next-config-js/headers
- Threat-model `documents/02-architecture/threat-models/2026-05-16-auth-flow-magic-link.md` §I3
- `.claude/rules/pre-handoff-self-test-completeness.md` §2.2

## Log

- **2026-05-16:** CSP + 5 security headers deployed Report-Only mode to both Next.js apps. Wave 86 Bucket E Fix 3. Enforcement flip → Phase 1.5 after 1 week clean.
