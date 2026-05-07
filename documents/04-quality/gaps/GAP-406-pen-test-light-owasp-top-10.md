# GAP-406: Pen-test Light (OWASP Top 10 + Security Headers + CSRF)

**Status:** 🟡 PARTIAL 2026-05-07 (Wave 37 Bucket C — automated baseline + checklist shipped; staging verification deferred to deploy)
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

- [x] `.github/workflows/zap-baseline.yml` shipped — `workflow_dispatch` trigger Phase 1; weekly schedule deferred to Phase 2 staging-stable (commented in YAML)
- [ ] Manual security headers checklist: 5 required headers verified curl — **deferred to staging deploy** per `gap-done-discipline.md` §3 PARTIAL exit ramp; checklist documented §7 of OWASP guide ready to run when staging URL active
- [ ] CSRF: Spring Security CsrfFilter verification per service — **deferred to staging deploy**; checklist §13 of OWASP guide documents required verification commands
- [x] OWASP Top 10 walkthrough document `documents/05-guides/security/owasp-top-10-baseline.md` shipped (13 categories + headers checklist + Phase 1.5 engagement plan)
- [x] External pen-test engagement plan (Phase 1.5 trigger) — §15 of OWASP guide documents trigger condition + scope + vendor selection process

## Log

- **2026-05-07** Wave 37 Bucket C: 3-layer security baseline shipped — (1) ZAP automated workflow `workflow_dispatch` mode, (2) `documents/05-guides/security/owasp-top-10-baseline.md` 13-category walkthrough + 5-header curl checklist + Phase 1.5 engagement plan, (3) external pen-test trigger gate documented. Per `gap-done-discipline.md` §3 PARTIAL exit ramp: 3 of 5 AC immediately verifiable; 2 AC (headers curl + CSRF verify) require staging deploy to actually exercise — tracked here as PARTIAL, not split into new gap because verification is direct continuation of this gap's AC. Verifications to run on staging deploy: §7 curl + §13 POST tests of OWASP guide. v1.0.0 PRODUCTION blocking gate: ZAP scan PASS + 5 headers verified + CSRF verified.

## Related

- `release-deploy-standard.md` §3.4
- OWASP Top 10 (2021)
- PDPL Art 23 security requirements
- Sister: GAP-400 (Trivy image scan)
