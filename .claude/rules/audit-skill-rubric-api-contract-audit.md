---
paths:
  - "documents/04-quality/audits/api-contract/**"
---

# Audit Skill Rubric — api-contract-audit (5 categories, per-check pass/fail)

**Priority:** 🟠 MANDATORY — audit primacy + per-check rubric for `api-contract-audit` skill
**Version:** 1.0.1
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (5-category per-check rubric + bug-finding-primacy + extends `api-contract-audit/SKILL.md` + worked self-test on current main surfaces ≥1 finding) per §6.5 Enforcement Parity Mandate; no constraint loosening — generalizes Wave 71c security-audit pattern closing GAP-523)
**Applies to:** Every invocation of `.claude/skills/quality/api-contract-audit/SKILL.md` (/100 API endpoint ↔ docs sync — every controller endpoint documented + every documented endpoint exists)

---

## 1. The Rule

> **`api-contract-audit` skill must score every Category by per-check pass/fail. "Endpoint exists" ≠ "schema matches" ≠ "error codes match" — each must be enumerated separately. Any P0/P1 sub-check FAIL caps category total ≤ 16/20 AND audit-level verdict = FAIL. The bug list (every undocumented or drifted endpoint with `Controller.java:N` evidence) is the deliverable.**

Wave 40 baseline `72/100 C+` cited 36 undocumented endpoints — but the score averaged 5 categories' sub-checks. "Endpoint coverage" can be 90% while "schema match" is 50% — averaging hides the schema-drift class. Per-check pass/fail forces every dimension to surface.

---

## 2. Mandatory per-check enumeration (≥5 per category)

### 2.1 Category 1 — Endpoint Coverage (P0 existence, P1 categorization)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 1.1 | Every `@*Mapping` in `*Controller.java` has matching api-contract.md entry | P0 | controller endpoint set − doc endpoint set = 0 |
| 1.2 | Every documented endpoint exists in code (no docs-orphans) | P0 | doc endpoint set − controller endpoint set = 0 |
| 1.3 | Public endpoints (`/public/**`) documented separately from authenticated | P1 | api-contract.md has Public + Auth sections |
| 1.4 | Gateway-proxied routes (`/api/v1/**`) mapped to backend service in docs | P1 | api-contract.md notes the proxy chain |
| 1.5 | Non-REST endpoints (SSE, WebSocket) documented with different schema | P1 | api-contract.md has dedicated section |
| 1.6 | Webhook receivers (`/webhooks/**`) documented with provider | P1 | per-provider section |

### 2.2 Category 2 — Request/Response Schema Match (P0 fields, P0 types)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 2.1 | Request DTO fields match documented schema (no extra/missing fields) | P0 | sample 5 endpoints; field set diff = 0 |
| 2.2 | Response DTO fields match documented schema | P0 | sample 5 endpoints; field set diff = 0 |
| 2.3 | Field types match: docs say `string`, DTO declares `String` not `Long` | P0 | type cross-check |
| 2.4 | Required vs optional fields match (`@NotNull` / `@Nullable` vs docs) | P1 | sample 3 DTOs |
| 2.5 | Nested objects fully documented (no `Object` placeholder for known types) | P1 | doc schema has typed inner refs |
| 2.6 | Enums: documented allowed values match Java enum constants | P1 | sample 2 enum-typed fields |

### 2.3 Category 3 — Error Code Consistency (P0 codes, P1 messages)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 3.1 | HTTP status codes documented match `@ResponseStatus` + `ResponseEntity.status(...)` | P0 | sample 5 endpoints' error paths |
| 3.2 | Application error codes (`SUBSCRIPTION_EXPIRED`, etc.) documented per endpoint | P0 | per-endpoint Error Codes section in api-contract.md |
| 3.3 | Error response body schema documented (problem+json or custom) | P1 | api-contract.md describes error envelope |
| 3.4 | Validation errors (400) include field-level details documented | P1 | sample 2 endpoints |
| 3.5 | Rate-limit (429) response documented per `pre-launch-auth-hardening-checklist.md` rate-limit table | P1 | rate-limited endpoints note 429 in docs |

### 2.4 Category 4 — Versioning & Deprecation (P0 SemVer, P1 lifecycle)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 4.1 | All endpoints under `/api/v1/**` (versioned URL) per `versioning-policy.md` §7.1 | P0 | grep returns 0 endpoints outside `/api/v[0-9]+/` |
| 4.2 | No breaking changes in MINOR releases (backwards-compat verified) | P0 | last MINOR diff in api-contract.md shows no removed fields |
| 4.3 | Deprecated endpoints marked `@Deprecated` + docs flag + removal date | P1 | grep `@Deprecated` in controllers cross-ref docs |
| 4.4 | Deprecation policy ≥6 months notice documented | P1 | api-contract.md preamble OR ADR |
| 4.5 | Breaking-change MAJOR bumps documented with migration guide | P1 | per `release-deploy-standard.md` §3.4 |

### 2.5 Category 5 — Integration Test Coverage (P0 happy-path, P1 error-path)

| # | Check | Severity | Pass criterion |
|---|---|---|---|
| 5.1 | Every documented endpoint has at least 1 happy-path IT | P0 | every `@*Mapping` has matching `*IT.java` test |
| 5.2 | Error paths covered (401, 403, 404, 422, 429 where applicable) | P0 | sample 5 endpoints' IT cover ≥3 error codes |
| 5.3 | Consumer-driven contract tests (Pact or equivalent) | P1 | `pact-jvm` OR similar present |
| 5.4 | Backwards-compat test on MINOR releases (old client → new server) | P1 | oasdiff or equivalent in CI |
| 5.5 | Schema validation runtime check (e.g., Spring Cloud Contract) | P2 | optional but recommended |

---

## 3. Banned shortcuts

| ❌ Banned | ✅ Required |
|---|---|
| "Cat 1 score 14/20 — most endpoints documented" | If 5 endpoints undocumented → P0 FAIL → cap 16/20 |
| "Schema mostly matches" without sampling | Sample ≥5 endpoints' DTOs vs schema field-by-field |
| Skip Cat 5 because "happy path tested" without enumerating error codes | 5.2 P0 separate from 5.1 P0 |
| "72/100 baseline, 36 undocumented" without per-endpoint FAIL list | Bug list = every undocumented endpoint with `Controller.java:line` evidence |
| Aggregate Cat 3 as "errors documented" without 3.2 application error codes check | 3.2 distinct from 3.1 |

---

## 4. Bug-finding > scoring primacy (BLOCKING)

> **An `api-contract-audit` run's purpose is to surface API-contract drift BEFORE consumers hit it (mobile app, third-party integrations, partners). A score of `72/100` listing only "36 undocumented" is less useful than listing each of 36 with `Controller.java:line` evidence + severity.** Per Wave 71c primacy pattern, bug list IS the deliverable.

Rules for every `api-contract-audit` run:

1. Enumerate ALL §2 sub-checks across 5 categories. NEVER skip.
2. Each sub-check returns `PASS` / `FAIL` / `N/A-with-reason` / `❓ UNCHECKED`. No partial credit.
3. Final output starts with bug list (every undocumented/drifted endpoint with file:line + severity) BEFORE score table.
4. Score descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. If time-budget runs out, mark `❓ UNCHECKED` — NEVER default to PASS.

---

## 5. Worked self-test — apply rubric to current main HEAD (2026-05-14)

| Sub-check | Verification | Verdict |
|---|---|---|
| 1.1 Every `@*Mapping` documented | Wave 40 baseline noted 36 undocumented endpoints | ❌ FAIL (P0) — still present per Wave 40 baseline |
| 1.2 Doc-orphans | grep doc endpoints not in any `@*Mapping` | ⚠️ UNCHECKED in scope |
| 4.1 All endpoints under `/api/v[0-9]+/` | `grep '@RequestMapping(\"/api/[^v]' --include='*Controller.java'` | ⚠️ Likely some `/api/auth/**` outside v[0-9]+ per `pre-launch-auth-hardening-checklist.md` §2.1 listing |
| 5.1 Every endpoint has IT | rough count `*IT.java` vs `*Controller.java` endpoint count | ⚠️ Likely partial — Wave 40 noted "consumer-driven contract tests still missing" |
| 5.3 CDC tests | grep `pact` in `pom.xml` | ❌ FAIL (P1) — Wave 40 explicit "consumer-driven contract tests still missing → ⚠️ PARTIAL on tests dimension" |

**Verdict:** ≥2 confirmed FAILs (1.1, 5.3) per Wave 40 baseline evidence. Per-check rubric forces per-endpoint enumeration — Wave 40 `72/100` reflected "36 undocumented" but didn't list which 36. New rubric makes each endpoint a FAIL row. Self-test PASS ✅.

---

## 6. Enforcement (per `rule-change-process.md` §6.5)

### 6.1 api-contract-audit/SKILL.md rubric extension (paired same PR)

Skill body extended with §"Per-check scoring" subsection citing this rule.

### 6.2 Pre-promotion gate

Before any release tag `v1.0.0-rc.*` or `v1.0.0`, `api-contract-audit` run MUST report ZERO P0 FAILs across §2.1-§2.5.

### 6.3 Reviewer checklist

- [ ] Bug list precedes score?
- [ ] Each Category lists ≥5 per-check verdicts?
- [ ] Every undocumented endpoint listed with file:line evidence?

### 6.4 Override mechanism

```
git commit -m "...
API_CONTRACT_DEFER: <check ID + reason — e.g., 5.3 CDC tests Wave 70+>
API_CONTRACT_FOLLOWUP: <gap link + completion date>"
```

### 6.5 Detector (deferred)

Future `scripts/check-api-contract-rubric.sh` parsing controller AST + api-contract.md cross-ref — defer until 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.

---

## 7. Log

- **2026-05-14 (v1.0.1):** PATCH — added `paths:` frontmatter per Wave 73 Bucket A1 path-scope. No constraint change; rule auto-loads only when matching files in context.
- **2026-05-14 (v1.0.0):** Rule created closing GAP-523 META P0 (Wave 72b Bucket E). Generalizes Wave 71c security-audit per-check pattern to api-contract-audit's 5 categories. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (GAP-523 Wave 71c retro) → Classify ✓ (no rule enforces per-endpoint pass/fail for api contract Cat 1-5) → Rule+Enforce ✓ (this file + api-contract-audit/SKILL.md §"Per-check scoring" extension paired same PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§5 worked example on current main — 2 confirmed FAILs: 1.1 36 undocumented endpoints + 5.3 CDC tests missing) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — no constraint loosening). Detector deferred per premature-rule guard ≥7 days.
