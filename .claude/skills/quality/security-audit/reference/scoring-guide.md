# Security Audit — Scoring Guide

## Grading Scale

| Score | Grade | Meaning |
|-------|-------|---------|
| 90-100 | A | Production-grade security posture |
| 80-89 | B | Good — minor hardening needed |
| 70-79 | C | Acceptable — known risks documented |
| 60-69 | D | Significant risks — block production deploy |
| <60 | F | Critical vulnerabilities — immediate action |

---

## Category 1: Dependency Vulnerabilities (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | 0 critical/high CVEs, all deps on latest patch |
| 16 | 0 critical, ≤2 high (with mitigation plan) |
| 12 | 0 critical, ≤5 high |
| 8 | 1-2 critical CVEs with no exploit path |
| 4 | Critical CVEs with potential exploit path |
| 0 | Known exploitable CVEs in production deps |

**Check:**
- `npm audit` for both FE apps
- Maven `dependency:tree` for outdated Spring Boot / Jackson / JSoup
- Check Snyk/GitHub Dependabot alerts if enabled

---

## Category 2: Secrets & Credentials (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | No secrets in repo, rotation policy documented, vault integration |
| 16 | No secrets in repo, .env gitignored, documented secret management |
| 12 | No secrets in repo, but no rotation policy |
| 8 | Test secrets acceptable, but prod secrets management unclear |
| 4 | Hardcoded secrets found (even if test-only) |
| 0 | Production secrets in repo history |

**Patterns to scan:**
```
password= | secret= | api_key= | Bearer | AWS_ACCESS | PRIVATE_KEY
jdbc:postgresql://.*:.*@  (inline credentials)
```

---

## Category 3: OWASP Top 10 (20 pts)

| # | OWASP Risk | Check | Points |
|---|-----------|-------|--------|
| A01 | Broken Access Control | Role-based checks on all endpoints | 3 |
| A02 | Cryptographic Failures | JWT signing, password hashing (bcrypt) | 2 |
| A03 | Injection | SQL parameterized (JPA), XSS (SVG sanitizer), SSRF (URL allowlist) | 3 |
| A05 | Security Misconfiguration | CORS restricted, debug off in prod, error messages generic | 3 |
| A06 | Vulnerable Components | (covered in Category 1) | 0 |
| A07 | Auth Failures | Brute force protection (rate limit), session timeout | 3 |
| A08 | Data Integrity | CSRF protection (double-submit token), input validation | 3 |
| A09 | Logging & Monitoring | Security events logged, audit trail | 2 |
| A10 | SSRF | URL allowlist validator active | 1 |

Total: 20 pts across OWASP checks.

---

## Category 4: Auth & Access Control (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | JWT with short expiry + refresh rotation, RBAC enforced, rate limiting per tier |
| 16 | JWT + refresh, roles checked, rate limiting exists |
| 12 | JWT works, roles exist but not enforced on all endpoints |
| 8 | Basic auth, some endpoints unprotected |
| 4 | Auth exists but easily bypassed |
| 0 | No authentication |

**Checks:**
- JWT secret strength (≥256 bit)
- Token expiry configured (access: ≤15min, refresh: ≤7d)
- Rate limit config per tier (FREE/BASIC/PREMIUM/ENTERPRISE)
- Email verification enforced before access
- Password policy (min length, complexity)

---

## Category 5: Infrastructure Security (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | TLS everywhere, CSP headers, Docker non-root, k8s security context, network policies |
| 16 | TLS configured, CORS restricted, Docker security basics |
| 12 | TLS planned, CORS exists, basic Docker config |
| 8 | No TLS enforcement, permissive CORS |
| 4 | Containers run as root, no security context |
| 0 | No infrastructure security considerations |

**Checks:**
- Dockerfile: `USER nonroot` or equivalent
- k8s: `securityContext.runAsNonRoot: true`
- Helm: `ingress.tls` configured
- CORS: not `allowOrigins: *` in production
- CSP headers in gateway config
