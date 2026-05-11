# GAP-406: Pen-test Light (OWASP Top 10 + Security Headers + CSRF)

**Status:** 🟢 DONE 2026-05-11 (Wave 60 Bucket A — full OWASP Top 10 self-audit shipped + live header probe + 3 follow-up gaps filed)
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
- [x] Manual security headers checklist: 5 required headers verified curl trên production frontend `https://kitehub.me/` 2026-05-11 — kết quả 1/5 PASS (HSTS only); 4 missing tracked GAP-471
- [x] CSRF: Spring Security CsrfFilter verification — kiteclass-gateway `SecurityConfig.java` `csrf.disable()` xác nhận, acceptable cho stateless JWT API (per OWASP CSRF prevention cheatsheet); kitehub-gateway parity tracked GAP-472
- [x] OWASP Top 10 walkthrough document `documents/05-guides/security/owasp-top-10-baseline.md` shipped (13 categories + headers checklist + Phase 1.5 engagement plan)
- [x] External pen-test engagement plan (Phase 1.5 trigger) — §15 of OWASP guide documents trigger condition + scope + vendor selection process

## Log

- **2026-05-11** Wave 60 Bucket A: full OWASP Top 10 (2021) pen-test light self-audit shipped (`documents/04-quality/audits/security/2026-05-11-pentest-light-owasp.md` — 76/100 C+ coverage); live header probe `curl -sI https://kitehub.me/` verified 1/5 PASS (HSTS only); 0 P0 / 3 P1 / 2 P2 / 1 P3 findings; 3 follow-up gaps filed (GAP-470 K8s runAsNonRoot, GAP-471 Vercel headers + CORS, GAP-472 gateway filter parity); CSRF verified disabled stateless OK. AC #2 (headers checklist) + AC #3 (CSRF verify) flipped từ PARTIAL deferred → DONE với evidence trên production frontend (gaps tracked separately cho remediation per `audit-to-gap-pipeline.md`). Status flip PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2. v1.0.0 PRODUCTION promotion path: 3 GAP-470/471/472 phải close trước cutover gate.
- **2026-05-07** Wave 37 Bucket C: 3-layer security baseline shipped — (1) ZAP automated workflow `workflow_dispatch` mode, (2) `documents/05-guides/security/owasp-top-10-baseline.md` 13-category walkthrough + 5-header curl checklist + Phase 1.5 engagement plan, (3) external pen-test trigger gate documented. Per `gap-done-discipline.md` §3 PARTIAL exit ramp: 3 of 5 AC immediately verifiable; 2 AC (headers curl + CSRF verify) require staging deploy to actually exercise — tracked here as PARTIAL, not split into new gap because verification is direct continuation of this gap's AC. Verifications to run on staging deploy: §7 curl + §13 POST tests of OWASP guide. v1.0.0 PRODUCTION blocking gate: ZAP scan PASS + 5 headers verified + CSRF verified.

## Related

- `release-deploy-standard.md` §3.4
- OWASP Top 10 (2021)
- PDPL Art 23 security requirements
- Sister: GAP-400 (Trivy image scan)
