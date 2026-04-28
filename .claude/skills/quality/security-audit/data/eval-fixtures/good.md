# Eval Fixture — good.md

# Expected: PASS — clean baseline; no secrets, no critical CVEs, auth on every endpoint

**Skill:** `quality/security-audit`
**Scenario:** Synthetic codebase that passes all 5 security audit categories.
**Which check fires:** none (clean baseline).
**Filed:** 2026-04-28 (GAP-253 pilot)

---

## Setup (synthetic)

### Cat 1 — Dependency Vulnerabilities

```bash
$ npm audit --json | jq '.metadata.vulnerabilities'
{ "critical": 0, "high": 0, "moderate": 0, "low": 0, "info": 0 }

$ ./mvnw dependency:tree | grep -i CVE
# (no output)
```

### Cat 2 — Secrets & Credentials

```bash
$ grep -rn "password=\|api_key=\|Bearer " --include="*.java" \
    --include="*.yml" kiteclass/*/src/main/ kitehub/*/src/main/ \
  | grep -v test | grep -v ".example"
# (no output)
```

`application.yml`:

```yaml
spring:
  datasource:
    password: ${POSTGRES_PASSWORD}   # ← env var, not hardcoded
ai:
  api-key: ${AI_API_KEY:#{null}}     # ← env var with null default
```

### Cat 3 — OWASP Top 10

- XSS guard: `JsoupSanitizer` runs on every user-generated HTML field
- SQL injection: all repository methods use `@Query` parameterized binding
- CSRF: enabled in `WebSecurityConfig` for state-mutating endpoints
- SSRF: URL allowlist `kiteclass.security.allowed-host-patterns` configured

### Cat 4 — Auth & Access Control

- Every controller method: `@PreAuthorize` annotation present
- Rate limiting: `@RateLimit(key=…)` on auth + AI endpoints
- JWT validation: `JwtAuthenticationFilter` registered on gateway

### Cat 5 — Infrastructure Security

- Docker images run as non-root user (`USER 1000` in Dockerfile)
- TLS 1.3 only in production (`server.ssl.protocols`)
- Helm charts have NetworkPolicy resource limiting east-west traffic

---

## Expected audit-report excerpt

```
## Cat 1 — Dependency Vulnerabilities  20/20
## Cat 2 — Secrets & Credentials       20/20
## Cat 3 — OWASP Top 10                20/20
## Cat 4 — Auth & Access Control       20/20
## Cat 5 — Infrastructure Security     20/20
Total: 100/100  Grade: A
```

No gaps filed. Codebase ready for production deploy.

---

## How to use this fixture

When extending the security-audit skill, run logic against this fixture to
confirm a passing scenario stays passing. If your change breaks this fixture,
you've introduced a false positive — investigate before merging.
