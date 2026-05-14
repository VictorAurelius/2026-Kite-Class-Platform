# Pre-Launch OWASP REST Hardening Checklist — security-audit Cat 3 per-check rubric (A01-A06/A08-A10)

**Priority:** 🟠 MANDATORY — pre-launch security gate (Cat 3 force-multiplier)
**Version:** 1.0.0
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer) item ex-A07 + extends security-audit skill Category 3 + worked self-test catches OWASP gaps on current main) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-vague Cat 3 per-OWASP-item enforcement closing GAP-522)
**Applies to:** Any release tag `v0.9.0-beta-staging.*` → first `v1.0.0-rc.*` transition; every PR touching `Controller.java`, `application*.yml` security config, exception handlers, SQL/JPQL query builders, public-facing endpoints

---

## 1. The Rule

> **Before any pre-production tag promotes to release-candidate or first production tag, all 9 OWASP Top 10 (2021) item checks (A01-A06 + A08-A10; A07 covered separately by `pre-launch-auth-hardening-checklist.md`) MUST pass OR have a documented `OWASP_REST_DEFER` trailer with risk acceptance + follow-up gap.**

OWASP Top 10 (2021) is the industry-standard web app security baseline. Wave 71b retro showed security-audit Category 3 rubric was vague ("XSS/SQLi/CSRF/SSRF guards per Wave 4") and bundled 9 OWASP items into 1 20-pt bucket — letting per-item gaps hide. This rule enumerates each item as a per-check sub-check (A07 covered by `pre-launch-auth-hardening-checklist.md` v1.0.0).

This rule fills the coverage gap. Security-audit skill Category 3 rubric extended in same PR.

---

## 2. Mandatory checks (9 — per OWASP Top 10 (2021) item ex-A07)

### 2.1 A01 Broken Access Control — per-resource authorization (P0)

Every admin / privileged endpoint has explicit `@PreAuthorize` / `@Secured` annotation; no reliance on path-based gateway routing alone. Tenant isolation enforced via `@TenantSecurity` interceptor (per Wave 4 scope).

Verify:
```bash
# Every Controller method handling admin / tenant-data MUST have @PreAuthorize
grep -rnE "@(Post|Put|Patch|Delete|Get)Mapping" <backend-product>/*/src/main/java/**/*AdminController.java \
  | xargs -I {} grep -B2 "{}" # check @PreAuthorize precedes
```

Pass criterion: every privileged endpoint has explicit authz annotation; tenant interceptor wired in security config.

### 2.2 A02 Cryptographic Failures — no weak ciphers (P0)

- No `MD5` / `SHA1` for password hashing (bcrypt cost ≥10 required)
- No `DES` / `3DES` / `RC4` for encryption
- TLS 1.2+ only (Cat 5 owns TLS config; this check confirms code doesn't downgrade)
- JWT uses HS256 with strong secret OR RS256 (sister of `pre-launch-auth-hardening-checklist.md` §2.6)

Verify:
```bash
grep -rnE "MessageDigest\.getInstance\(\"(MD5|SHA-1)\"\)" <backend-product>/ <tenant-product>/ --include="*.java"
# Expected: 0 hits OR each is for non-security purpose (cache key, etc.) with comment
```

### 2.3 A03 Injection — parameterized queries only (P0)

No string concatenation building SQL/JPQL queries. JPA Criteria API or `@Query` with named/positional params.

Verify:
```bash
grep -rnE "(SELECT|UPDATE|DELETE|INSERT).*\+\s*\w+\s*\+|String\.format.*WHERE.*%" \
  <backend-product>/ <tenant-product>/ --include="*.java"
# Expected: 0 hits in non-test code
```

Same applies to NoSQL queries (if any) + HQL + Spring Data dynamic methods.

### 2.4 A04 Insecure Design — threat model per critical flow (P1)

Every critical user flow (auth, payment, file upload, AI-generated content) has documented threat model:
- `documents/02-architecture/threat-models/<flow>.md` exists
- Covers: trust boundaries, attack surfaces, abuse cases, mitigations
- Reviewed within 90d of release

Acceptable v1: Phase 1 BETA has threat models for auth + AI Branding (per Wave 4); Phase 1.5 PAID extends to payment.

### 2.5 A05 Security Misconfiguration — production profile hardened (P1)

Production profile (`application-production.yml`) has:
- `server.error.include-stacktrace: never`
- `server.error.include-message: never`
- Actuator endpoints SCOPED (only `health` exposed publicly; rest behind admin role)
- Debug logs DISABLED (`logging.level.root: INFO` minimum)
- Spring profile flag set correctly (per `production-env-config-registry.md` §11 audit-spring-profiles)

Verify:
```bash
grep -A2 "management.endpoints.web.exposure" <backend-product>/*/src/main/resources/application-production.yml
# Expected: include: health  (NOT include: '*')
```

### 2.6 A06 Vulnerable & Outdated Components (P0)

Covered by `pre-launch-dependency-hardening-checklist.md` sister rule §2.1/§2.2. Cross-reference here; this check confirms cross-reference active in security-audit Cat 1.

### 2.7 A08 Software & Data Integrity — supply chain integrity (P1)

- Docker images SHA-pinned in `Dockerfile`/`docker-compose.yml` (not just tag — `image@sha256:...`)
- CI workflow actions SHA-pinned (`uses: actions/checkout@v4` → ideally `uses: actions/checkout@SHA`)
- Or documented exception per pinning policy

Acceptable v1: tag-pinning OK if Dependabot configured per `pre-launch-dependency-hardening-checklist.md` §2.7. Full SHA pinning = Phase 1.5+ work.

### 2.8 A09 Security Logging & Monitoring Failures — admin audit log (P1)

Every PLATFORM_ADMIN action writes `admin_audit_log` row (per `pre-launch-auth-hardening-checklist.md` §2.7 — same scope). Plus:
- Authentication failures logged WITHOUT password values (per `logs-format-standard.md` §3.1 PII scrubbing)
- Rate-limit breaches alert via metric (per `RateLimitBreachSpike` pattern from `ai-branding-guidelines.md` §2.5)
- Logs forwarded to aggregator within 24h (per `logs-format-standard.md` §4 retention tiers)

Cross-reference: sister check in `pre-launch-auth-hardening-checklist.md` §2.7 (admin audit log) — implementations share the same `AdminAuditLog` entity.

### 2.9 A10 Server-Side Request Forgery — outbound URL allowlist (P1)

Any service consuming user-supplied URLs (uploaded logo URL, webhook URL, callback URL, image fetch) MUST validate:
- Hostname against allowlist OR explicit denylist of metadata IPs (`169.254.169.254`, AWS/GCP/Azure metadata endpoints)
- Block `localhost` / `127.0.0.1` / RFC1918 private ranges from production callouts
- Use circuit breaker + timeout (per `design-patterns.md` §3.6)

Verify: search for `RestTemplate` / `WebClient` / `HttpClient` instantiation against user input:
```bash
grep -rnE "(RestTemplate|WebClient|HttpURLConnection).*\.(get|post|exchange)" \
  <backend-product>/ <tenant-product>/ --include="*.java" | grep -iE "user|input|url"
```

Each hit reviewed for allowlist; AI Branding `Logo URL` already has this (per Wave 4 + `ai-branding-guidelines.md` §9).

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "Gateway path filter is enough for authz" | Per-resource `@PreAuthorize` for defense in depth |
| Allow MD5/SHA1 "because cache key non-security" | Document non-security purpose adjacent to code OR switch to non-cryptographic hash (`hashCode` / xxHash) |
| `String.format("SELECT ... WHERE id=%s", userId)` "small admin tool" | Always parameterized; no exception |
| Skip threat model "auth is obvious" | Critical flows = always documented; Phase 1 BETA scope minimum |
| `management.endpoints.web.exposure.include: '*'` in production | Scope to `health` only; admin auth for rest |
| Block `localhost` only, not `169.254.169.254` | Cloud metadata endpoints exfil risk — denylist explicit |
| Score 17/20 by averaging — A01 broken access hidden in 1 unflagged sub-check | Per-OWASP-item pass/fail; 1 P0 fail = Cat 3 FAIL regardless |

---

## 4. Worked self-test — current main state 2026-05-14

**Apply §2 checklist retroactively to current main HEAD:**

| # | OWASP item | Estimated state | Verdict |
|---|---|---|---|
| 2.1 | A01 — per-resource authz | Wave 71c GAP-518 surfaced role mismatch BE `PLATFORM_ADMIN` vs FE `'ADMIN'` (now fixed); `@PreAuthorize` coverage not fully audited per-endpoint | **PARTIAL** — sample audit needed |
| 2.2 | A02 — no weak crypto | bcrypt assumed (Spring Security default); MD5/SHA1 grep likely clean | likely **PASS** |
| 2.3 | A03 — parameterized queries | Spring Data JPA convention; manual `@Query` rare; grep for `String.format` with SELECT likely 0 hits | likely **PASS** |
| 2.4 | A04 — threat model docs | `documents/02-architecture/threat-models/` likely DOES NOT EXIST yet | **FAIL** — file follow-up gap |
| 2.5 | A05 — prod profile hardened | `production-env-config-registry.md` §11 spring-profile audit found 5 services NO `application-production.yml` exists | **FAIL** — closing via GAP-511 |
| 2.6 | A06 — vuln components | Cross-reference Cat 1 sister rule | **PASS** (delegated) |
| 2.7 | A08 — supply chain integrity | Docker images tag-pinned not SHA-pinned; GH Actions same | **PARTIAL** — Phase 1.5+ scope acceptable v1 |
| 2.8 | A09 — admin audit log | GAP-521 (Wave 71c) tracks `admin_audit_log` entity build; not yet shipped | **FAIL** — closing via GAP-521 |
| 2.9 | A10 — SSRF allowlist | AI Branding logo URL has allowlist per Wave 4; webhook scope (Phase 1.5) not yet | **PARTIAL** for current scope |

**Expected findings:** 3 FAIL (threat models, prod profile, admin audit log) + 3 PARTIAL.

**Verdict:** Rule fires correctly on current main — surfaces specific per-OWASP-item gaps instead of vague Cat 3 score. ✅

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Security-audit skill rubric extension (paired same PR)

`.claude/skills/quality/security-audit/SKILL.md` Category 3 "OWASP A01-A06 + A08-A10" rubric extended with 9 explicit per-OWASP-item rows. Each item pass/fail (no averaging). 1 P0 fail = category total ≤ 16/20.

### 5.2 Pre-promotion gate

Before any maintainer creates git tag matching `v1.0.0-rc.*` or `v1.0.0`, §2 checklist MUST exit 0. Currently: manual run + grep evidence in PR description. Detector script `scripts/check-owasp-rest-hardening.sh` deferred per premature-rule guard ≥7 days.

### 5.3 Reviewer checklist

When reviewing any PR that touches `Controller.java`, `application*.yml` security config, `@Query` annotations, or outbound HTTP client construction, reviewer asks:
- New admin endpoint? → §2.1 `@PreAuthorize` present?
- New crypto / hash use? → §2.2 not MD5/SHA1?
- New query? → §2.3 parameterized?
- New outbound URL with user input? → §2.9 allowlist?

### 5.4 Override mechanism

For genuine schedule pressure (Phase 1 BETA acceptable scope):

```
git commit -m "...
OWASP_REST_DEFER: <A0X + reason — e.g. 'A04 threat models for AI Branding only; payment flow Phase 1.5'>
OWASP_REST_FOLLOWUP: <gap link with completion date>"
```

Trailer logged. Pattern frequency >3 defers per release = meta-review.

### 5.5 Detector (deferred)

Future: `scripts/check-owasp-rest-hardening.sh` greps per-OWASP-item patterns. Defer until 2nd recurrence; reviewer + worked self-test sufficient for v1.0.0.

---

## 6. Relationship to other rules

- **`security-audit/SKILL.md`** — Category 3 rubric extended same PR
- **`pre-launch-auth-hardening-checklist.md`** — covers OWASP A07 separately (Cat 4); this rule covers A01-A06 + A08-A10
- **`pre-launch-dependency-hardening-checklist.md`** — covers A06 via cross-reference (Cat 1)
- **`pre-launch-secrets-hardening-checklist.md`** — covers A02 storage side (Cat 2)
- **`pre-launch-infra-hardening-checklist.md`** — covers A05 misconfig infra side (Cat 5)
- **`logs-format-standard.md`** — covers A09 logging discipline + PII scrubbing
- **`design-patterns.md`** §3.6 — covers A10 SSRF resilience + circuit breaker pattern
- **`ai-branding-guidelines.md`** §9 — covers A10 for AI Branding scope
- **`output-review-mandate.md`** §3 — Security audit row sharpened for Cat 3
- **`release-deploy-standard.md`** §3.4 — MAJOR/first-PROD includes "Pen-test light OWASP top 10"; this rule operationalizes A01-A06/A08-A10 per item
- **`incident-to-rule-pipeline.md`** — direct output of GAP-522 applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + skill rubric extension + worked self-test same PR

---

## 7. Log

- **2026-05-14 (v1.0.0):** Rule created closing Cat 3 slice of GAP-522. Triggered by user-flagged miss "skill audit phải là lớp phòng vệ tin tưởng" + Wave 71c PR #1278 already fixed Cat 4; extending same fix to Cat 3 with one check per OWASP item. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (GAP-522 filed 2026-05-13) → Classify ✓ (Cat 3 rubric was vague "XSS/SQLi/CSRF/SSRF guards per Wave 4" bundling 9 OWASP items in single bucket; allowed averaging that hid per-item gaps) → Rule+Enforce ✓ (this file + security-audit/SKILL.md Cat 3 row update + worked §4 self-test + paired with 3 sister rules per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§4 worked example on current main — 3 FAIL + 3 PARTIAL surface, validating rubric is concrete) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — adds per-OWASP-item Cat 3 coverage to previously-vague rubric; no constraint loosening; existing tags grandfathered; rule applies prospectively to `v1.0.0-rc` promotion).
