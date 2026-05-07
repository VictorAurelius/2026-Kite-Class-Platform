# GAP-406: Pen-test Light (OWASP Top 10 + Security Headers + CSRF)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (P0 for v1.0.0 PRODUCTION)
**Domain:** Security / Compliance
**Found:** 2026-05-07 (Wave 37 — `release-deploy-standard.md` §3.4 MAJOR checklist line)
**Affects:** v1.0.0 PRODUCTION launch — minimum security baseline

## Problem

`release-deploy-standard.md` §3.4 MAJOR yêu cầu "Pen-test light (OWASP top 10 + security headers + CSRF)" nhưng KHÔNG có gap track việc thực thi. Phase 1 BETA invite-only chấp nhận được không pen-test, nhưng v1.0.0 paid customers REQUIRED.

## Proposed Fix

3 phases:
1. **Automated baseline** — OWASP ZAP baseline scan trong CI workflow (low-cost, catches common issues)
2. **Manual checklist** — security headers verify (CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy), CSRF token presence on state-mutating endpoints, JWT validation
3. **External pen-test** — Phase 1.5 PAID trigger gate (≥30 paying tenants) → engage external firm 1-2 ngày scope

## Acceptance Criteria

- [ ] `.github/workflows/zap-baseline.yml` runs ZAP scan against staging weekly
- [ ] Manual security headers checklist: 5 required headers verified curl
- [ ] CSRF: Spring Security CsrfFilter enabled hoặc explicit token validation per endpoint
- [ ] OWASP Top 10 walkthrough document `documents/05-guides/security/owasp-top-10-baseline.md`
- [ ] External pen-test engagement plan (Phase 1.5 trigger)

## Related

- `release-deploy-standard.md` §3.4
- OWASP Top 10 (2021)
- PDPL Art 23 security requirements
- Sister: GAP-400 (Trivy image scan)
