# GAP-371: CDN Setup — Cloudflare Proxy + DDoS Protection

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA — không block launch nhưng risk visible)
**Domain:** Infrastructure / Performance / Security
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Public-facing performance, DDoS protection, geographic latency

## Problem

KHÔNG có CDN cho public marketing pages + tenant-facing dashboards. Risks:
- Slow load times cho user xa Oracle Cloud region
- DDoS exposure (no upstream filter)
- No WAF (Web Application Firewall)
- No edge caching cho static assets
- No SSL/TLS termination at edge (every request hits origin)

## Proposed Fix

**Cloudflare Free tier setup:**

Pros:
- Free tier có DDoS basic + CDN + SSL/TLS + DNS
- Vietnam edge nodes (Hanoi/HCM)
- Auto-minify HTML/CSS/JS
- Browser cache rules
- Security: bot detection, WAF basic

Cons:
- Free tier limits (100 page rules, basic bot management)
- Vendor lock-in concerns (mitigated bằng portable DNS)

**Setup steps:**
1. Cloudflare account creation
2. Add domain kitehub.vn + kiteclass.vn (per GAP-369)
3. Update domain registrar nameservers → Cloudflare
4. Configure DNS records via Cloudflare (proxy enabled = orange cloud)
5. SSL/TLS: Full (strict) mode với origin cert
6. Page rules: cache static assets aggressively, bypass cache for /api/*
7. Security: enable Bot Fight Mode + Always Use HTTPS
8. Speed: enable Auto Minify (HTML/CSS/JS) + Brotli compression
9. Caching: respect origin headers + custom rules

## Acceptance Criteria

- [ ] Cloudflare account configured + domain added
- [ ] Nameservers updated at registrar
- [ ] DNS records proxied (orange cloud)
- [ ] SSL/TLS Full (strict) mode active
- [ ] Page rules: static assets cached, /api/* bypass
- [ ] Bot Fight Mode enabled
- [ ] WAF managed rules enabled (free tier)
- [ ] HTTPS redirect via Always Use HTTPS
- [ ] Smoke test: kitehub.vn loads với Cloudflare headers (CF-Ray, CF-Cache-Status)
- [ ] Performance baseline: P95 TTFB < 200ms cho cached pages
- [ ] DDoS protection verified (Cloudflare attack analytics dashboard)

## Open decisions

- Cloudflare Free vs Pro ($20/mo)? Free đủ cho beta + early production
- Custom rules priority list

## Effort estimate

~1 ngày setup + verification. Domain nameserver propagation 2-24h.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Sister: GAP-369 (DNS setup)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. STRONGLY recommend cho Phase 1 BETA — DDoS exposure + slow load times nếu skip.
