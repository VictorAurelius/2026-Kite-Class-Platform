# OWASP Top 10 (2021) Baseline — Pen-test Light Walkthrough

**Status:** Phase 1 BETA baseline (v0.9.0-beta) — manual checklist + ZAP automated scan
**Target:** Phase 1.5 PAID + v1.0.0 PRODUCTION promotion
**Scope:** kitehub-frontend + kitehub-* backend services + kiteclass-frontend + kiteclass-core
**Source:** [OWASP Top 10 (2021)](https://owasp.org/Top10/) + [`release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) §3.4
**Closes:** GAP-406 baseline (Wave 37 Bucket C)

---

## 1. Mục đích

Tài liệu này codify minimum security baseline cho Phase 1 BETA → v1.0.0 PRODUCTION. KHÔNG thay thế external pen-test (Phase 1.5 trigger ≥30 paying tenants), nhưng phải PASS trước khi tag `v1.0.0`.

3 layers:
1. **Automated** — `.github/workflows/zap-baseline.yml` runs ZAP baseline scan against staging
2. **Manual checklist** — security headers + CSRF + JWT verification (this document §3-§13)
3. **External pen-test** — Phase 1.5 paid trigger gate (out of scope this gap)

---

## 2. ZAP Baseline Scan — Setup & Run

```bash
# Manual trigger via workflow_dispatch
gh workflow run zap-baseline.yml \
  --field target_url=https://staging.kite.vn

# Review report artifact post-run
gh run view --log
gh run download <run-id> -n zap-baseline-report
```

**Pass criteria:** 0 HIGH alerts. MEDIUM alerts triaged + ticketed.

---

## 3. A01:2021 — Broken Access Control

| Check | How to verify | Expected | Status |
|---|---|---|---|
| Tenant isolation | Owner A token cannot read tenant B data | 403 Forbidden | ⏳ verify post-staging |
| Admin role check | Non-admin token on `/admin/*` | 403 Forbidden | ⏳ |
| Direct object reference | Modify `id` param to other tenant's resource | 404 Not Found | ⏳ |
| JWT signature validation | Tampered JWT rejected | 401 Unauthorized | ⏳ |

**Manual test:**
```bash
TOKEN_A=<owner-tenant-A>
curl -H "Authorization: Bearer $TOKEN_A" https://staging.kite.vn/api/v1/tenants/<tenant-B-id>/users
# Expected: 403
```

---

## 4. A02:2021 — Cryptographic Failures

- [ ] HTTPS only (HTTP 301 redirects to HTTPS) — verify staging
- [ ] HSTS header present + `max-age >= 31536000`
- [ ] Passwords hashed với BCrypt (verify `kitehub-subscription` config)
- [ ] JWT signed với HS256+ hoặc RS256 (no `none` alg)
- [ ] Database backups encrypted at rest (RDS encryption flag)
- [ ] Session tokens không transmit in URL query strings

---

## 5. A03:2021 — Injection

- [ ] SQL injection: JPA parameterized queries enforced (no string concat) — ArchUnit test
- [ ] OS command injection: no `Runtime.exec()` with user input
- [ ] LDAP injection: N/A (no LDAP integration Phase 1)
- [ ] XSS: React escapes by default; verify `dangerouslySetInnerHTML` usage audit

**ZAP automated:** ZAP baseline catches reflected XSS + basic SQLi patterns.

---

## 6. A04:2021 — Insecure Design

- [ ] Rate limit per tier enforced (FREE 3/PRO 10/PREMIUM 30 per `ai-branding-guidelines.md`)
- [ ] Beta access: claim code expires (Wave 36 388-B 2FA)
- [ ] Password reset: token expires <15 min
- [ ] Email verification mandatory before login

---

## 7. A05:2021 — Security Misconfiguration

### Required Security Headers (verify all 5)

| Header | Required value | Verify |
|---|---|---|
| `Content-Security-Policy` | `default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; ...` | `curl -I` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` | `curl -I` |
| `X-Frame-Options` | `DENY` or `SAMEORIGIN` | `curl -I` |
| `X-Content-Type-Options` | `nosniff` | `curl -I` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | `curl -I` |

```bash
curl -sI https://staging.kite.vn | grep -iE 'content-security-policy|strict-transport|x-frame|x-content-type|referrer-policy'
```

Pass: all 5 headers present with required values.

### Other config

- [ ] No default credentials (verify dev-only seed disabled in prod profile)
- [ ] Error pages don't leak stack traces (Spring profile = `prod` disables)
- [ ] `actuator/*` endpoints require admin auth except `/health`
- [ ] CORS allowlist explicit (no `*` wildcard)

---

## 8. A06:2021 — Vulnerable & Outdated Components

- [ ] Dependabot enabled + GAP-219 weekly run
- [ ] `pnpm audit --audit-level=high` clean
- [ ] `mvn org.owasp:dependency-check-maven:check` passes
- [ ] Trivy image scan (GAP-400) — 0 HIGH/CRITICAL CVEs

---

## 9. A07:2021 — Identification and Authentication Failures

- [ ] Password complexity: ≥8 chars, mix upper+lower+digit (BR-AUTH-XXX)
- [ ] Brute force: rate limit failed login attempts (5/15min lockout)
- [ ] Session: invalidate on logout
- [ ] MFA available for admin accounts (Phase 1.5 — track follow-up)

---

## 10. A08:2021 — Software and Data Integrity Failures

- [ ] Docker images signed (Cosign — GAP-402)
- [ ] SBOM generated per image (Syft CycloneDX — GAP-402)
- [ ] CI artifacts pinned by SHA, not floating tags
- [ ] Outbox pattern enforces event integrity (per `design-patterns.md` §3.5.1)

---

## 11. A09:2021 — Security Logging and Monitoring Failures

- [ ] Auth events logged (success + failure) per `logs-format-standard.md`
- [ ] Admin actions logged with `userId` + `tenantId`
- [ ] PII scrubbed at logger level (per `logs-format-standard.md` §3)
- [ ] 7-year retention security/audit logs (PDPL + ND-13/2023)
- [ ] Alerts wired for spike of `auth.failure` (Grafana — GAP-115)

---

## 12. A10:2021 — Server-Side Request Forgery (SSRF)

- [ ] AI provider calls (Ollama/Bedrock) use allowlisted endpoints — config-driven
- [ ] Image upload: URL validation rejects internal IPs (RFC 1918 + 169.254.x.x)
- [ ] No user-controllable URL fetching beyond AI providers

---

## 13. CSRF Protection

| Check | Spring Security config | Status |
|---|---|---|
| State-mutating endpoints (POST/PUT/DELETE) protected | `CsrfFilter` enabled OR explicit token validation | ⏳ verify per service |
| API token auth (JWT) excluded from CSRF (stateless) | `csrf.disable()` paired with `SessionCreationPolicy.STATELESS` | ⏳ |
| Browser-based session endpoints have CSRF token | Cookie + header double-submit pattern | ⏳ |

**Verify:**
```bash
curl -X POST https://staging.kite.vn/api/v1/beta-access/request \
  -H "Content-Type: application/json" \
  -d '{"organizationName":"test"}'
# Expected: 401 (no token) OR 403 (CSRF rejected) — never 201
```

---

## 14. Pre-release Gate Checklist

Before tagging `v1.0.0-rc.X`:

- [ ] ZAP baseline scan PASS (0 HIGH alerts)
- [ ] §7 Security headers — all 5 verified via curl
- [ ] §8 Dependency scans clean (Dependabot + Trivy)
- [ ] §11 Auth event logging verified in staging
- [ ] §13 CSRF protection verified per state-mutating endpoint
- [ ] Manual penetration test scope drafted for Phase 1.5

---

## 15. Phase 1.5 External Pen-test Engagement Plan

**Trigger:** ≥30 paying tenants (post-Phase 1 BETA → PAID).

**Scope (1-2 days):**
1. Authenticated tenant isolation test (multi-tenant boundary)
2. AI prompt injection (kitehub-branding wizard inputs)
3. PDPL consent flow + data export/deletion endpoints
4. Admin escalation paths
5. Payment integration (when added Phase 2+)

**Vendor candidates (TBD):** Vietnamese cybersec firms — request quotes Q3 2026.

**Deliverable:** report + remediation tickets filed as gaps within 14 days.

---

## 16. Related

- `release-deploy-standard.md` §3.4 — MAJOR release pen-test requirement
- GAP-400 (Trivy image scan) — sister automated baseline
- GAP-406 (this gap)
- PDPL Art 23 — security requirements
- `logs-format-standard.md` §3 — PII scrubbing
- OWASP Top 10 (2021): https://owasp.org/Top10/

---

## 17. Log

- **2026-05-07** (v1.0.0): Initial baseline created (Wave 37 Bucket C, GAP-406). 13 OWASP categories + headers checklist + Phase 1.5 engagement plan. Reviewer: @nguyenvankiet (solo-dev). Manual verification deferred to Phase 1.5 staging deploy.
