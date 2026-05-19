---
title: Wave 98 New API Contracts /100 Audit
status: complete
created: 2026-05-19
audit_type: api-contract
phase: phase-1-beta
wave: 98
deadline_per_post_wave_audit_mandate: 2026-05-21
auditor: Background agent (Opus 4.7, GAP-661 Wave 98 post-closure audit suite)
gaps_in_scope: [GAP-661]
new_gaps_recommended: [GAP-662, GAP-663, GAP-664]
baseline: 2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md (79/100 C+ FAIL — 3 P0)
delta: -3 vs Wave 92 → **76/100 (C) FAIL** vì 3-layer doc gaps (preferences thiếu rules.md + use-cases.md; email thiếu use-cases.md) + EmailController URL drift (`/api/platform/emails` vs documented `/api/email/**`) + Wave 92 P0 carries unfixed (GAP-637/638 still OPEN)
---

# API Contract Audit Report — Wave 98 New Contracts

**Wave scope:** Wave 98 (commit range `7b332411..7b2f4301`, 8 buckets) — 3 new api-contract.md files (B0 preferences / B1 email / B2 seed) + 1 new controller endpoint (B0 `POST /api/v1/preferences/dismiss-banner-state`).
**Skill:** `.claude/skills/quality/api-contract-audit/SKILL.md`
**Rubric:** `.claude/rules/audit-skill-rubric-api-contract-audit.md` v1.0.1 (5 categories × ≥5 sub-checks; per-check pass/fail; 1 P0 FAIL caps category ≤16 + audit-level FAIL)
**Aggregate:** **76/100 (C) FAIL** — audit-level verdict: **FAIL** (2 P0 sub-checks: §2.2 doc-orphan EmailController URL drift + §2.5 zero IT for new PreferencesController)

**Constraint:** Code-level/artifact-based audit (live curl blocked by GAP-612 AWS account suspension); reliance on controller signatures + DTO grep + 3-layer doc compliance + integration tests presence.

---

## 1. Scope

### 1.1 Artifacts in scope (Wave 98 NEW)

| # | Artifact | Wave 98 commit | Domain |
|---|---|---|---|
| 1 | `documents/01-business/kitehub/preferences/api-contract.md` | b12ea568 (B0) | preferences |
| 2 | `documents/01-business/kitehub/email/api-contract.md` | 8d0e4fb9 (B1) | email |
| 3 | `documents/01-business/kitehub/seed/api-contract.md` | 54d23b3f (B2) | seed |
| 4 | `kitehub/kitehub-subscription/.../preferences/controller/PreferencesController.java` (NEW class + 1 endpoint) | b12ea568 (B0) | preferences |

### 1.2 NOT in scope (out of audit window)

- Wave 98 buckets B3-B8 (no api-contract.md changes; tooling/audit/support work)
- Pre-existing api-contract.md files in other domains (delta-only audit per `api-contract-audit/SKILL.md` §"Diff-based audit")
- Wave 92 carry-forward P0 (GAP-637/638) — tracked separately

---

## 2. Methodology

Per `api-contract-audit/SKILL.md` + rubric §2 (5 categories × ≥5 sub-checks). Per-check pass/fail; 1 P0 FAIL → audit verdict FAIL + category total cap ≤16/20.

### State-check chain (per `audit-to-gap-pipeline.md` §2.5)

| Artifact | Verify command | Result |
|---|---|---|
| 3 new api-contract.md files exist | `ls documents/01-business/kitehub/{preferences,email,seed}/api-contract.md` | ✅ all 3 exist |
| 3-layer doc compliance | `ls documents/01-business/kitehub/{preferences,email,seed}/` | ❌ preferences ONLY has api-contract.md (no rules.md + no use-cases.md); email has api-contract.md + rules.md (no use-cases.md); seed has all 3 |
| PreferencesController file | `find kitehub/kitehub-subscription/.../preferences/controller/` | ✅ exists with 1 endpoint |
| PreferencesController unit/IT | `find kitehub/kitehub-subscription/src/test -name "PreferencesController*"` | ❌ ZERO test files |
| EmailController matches documented URL | `grep @RequestMapping EmailController.java` vs api-contract.md `/api/email/**` | ❌ DRIFT: controller declares `/api/platform/emails`, doc says `/api/email/**` |
| Wave 92 P0 carries (GAP-637/638) | `bash scripts/query-gaps.sh GAP-637` | ❌ still OPEN (not fixed in Wave 93-98) |
| seed VietnamSampleDataGenerator class | `find kitehub-platform -name VietnamSampleDataGenerator.java` | ✅ exists + has unit test |

### Endpoint inventory

| # | Endpoint | Controller | Documented? | Auth model |
|---|---|---|---|---|
| 1 | `POST /api/v1/preferences/dismiss-banner-state` | PreferencesController (NEW) | ✅ preferences/api-contract.md | "Public (auth optional)" — NO `@PreAuthorize` |
| 2 | `POST /api/platform/emails/send` (code) vs `POST /api/email/send` (doc) | EmailController (PRE-EXISTING) | ⚠️ DRIFT | None — internal "service-to-service" trust per javadoc |
| N/A | Java methods of `VietnamSampleDataGenerator` (Spring `@Component`) | NOT HTTP — internal Java contract per seed/api-contract.md §Scope | ✅ documented as method contract | N/A |

---

## 3. Bug list (precedes score per primacy rule §4)

### 3.1 P0 — Wave 98 scope (audit-blocking)

1. **P0 (Cat 2 §2.2.1 + Cat 1 §2.1.2) — EmailController URL drift: doc says `/api/email/**`, code declares `/api/platform/emails/**`.**
   - Evidence: `EmailController.java:27` `@RequestMapping("/api/platform/emails")` + line 45 `@PostMapping("/send")` → actual URL `POST /api/platform/emails/send`.
   - api-contract.md line 16 documents `POST /api/email/send` + line 17 documents base URL `gateway proxy: /api/email/**`.
   - Cross-layer drift: FE consumer (or gateway proxy) consuming `/api/email/send` will hit 404 — contract violation.
   - Cross-rule: `contract-first-for-cross-layer.md` §3 cross-layer drift, `api-contract-audit/SKILL.md` Gotchas line 90 "Gateway routes proxy to core — check gateway config for actual public paths".
   - Severity: HIGH — 5 critical email templates (welcome / beta-invite / email-verification / password-reset / invite-staff) shipped Wave 98 unable to fire if any consumer hits documented URL.
   - File gap: **GAP-662 P0** — reconcile EmailController URL OR document gateway rewrite explicitly + add IT.

2. **P0 (Cat 5 §2.5.1) — PreferencesController ships ZERO integration tests (no MockMvc, no Mockito unit).**
   - Evidence: `find kitehub/kitehub-subscription/src/test -name "PreferencesController*"` → 0 hits.
   - Rubric §2.5.1 P0 mandate: "Every documented endpoint has at least 1 happy-path IT".
   - Severity: HIGH — NEW endpoint, NEW DTO, NEW response shape (204 No Content + Set-Cookie header), zero verification of HTTP routing or cookie shape.
   - Cross-rule: `pre-handoff-self-test-completeness.md` (verify the FLOW), `release-deploy-standard.md` §3.1 PRE-RELEASE smoke admin-login pattern.
   - File gap: **GAP-664 P0** — add `@WebMvcTest(PreferencesController.class)` IT covering happy-path + 400 validation + cookie header assertion.

### 3.2 P1 — Wave 98 scope

3. **P1 (Cat 1 §2.1.1 + Living Docs rule) — preferences domain ships ONLY api-contract.md; missing rules.md + use-cases.md (CLAUDE.md 3-Layer mandate violation).**
   - Evidence: `ls documents/01-business/kitehub/preferences/` → only `api-contract.md`.
   - CLAUDE.md §"Business Logic Documents — 3-Layer Structure": "3 files per domain — pre-commit hook will warn if missing".
   - Domain has business rules (banner key validation, 30-day cookie expiry, anonymous vs authenticated semantics, future Wave 99+ user_preferences persistence) — these belong in rules.md.
   - File gap: **GAP-663 P1** — create `documents/01-business/kitehub/preferences/{rules.md, use-cases.md}` (BR-PREF-001..BR-PREF-005 + UC-PREF-001 dismiss banner).

4. **P1 (Living Docs rule) — email domain has api-contract.md + rules.md but missing use-cases.md.**
   - Evidence: `ls documents/01-business/kitehub/email/` → `api-contract.md`, `rules.md`, `templates/` (no use-cases.md).
   - Pattern: 5 critical email types (welcome / beta-invite / email-verification / password-reset / invite-staff) each warrant a use-case row (actor, trigger, steps, errors, FE behavior).
   - Bundle into **GAP-663** (3-layer cleanup batch).

5. **P1 (Cat 1 §2.1.3 + Cat 3 §2.3.1) — `POST /api/v1/preferences/dismiss-banner-state` documented as "Public (auth optional)" but missing explicit threat model + rate-limit documentation.**
   - api-contract.md §Errors lists `429 RATE_LIMITED` but doesn't cite the rate-limit policy (per-IP 60/min claim made; gateway config not verified).
   - In-memory `ConcurrentHashMap` (per controller javadoc Phase 1) = DoS surface; documented but not bounded.
   - No CSRF discussion despite cookie-setting public endpoint (anonymous-flow → CSRF less severe but acknowledge).
   - Bundle into **GAP-663** (docs cleanup) OR separate P2 follow-up.

6. **P1 (Cat 2 §2.2.6) — `bannerKey` field schema OK but cookie name pattern `kite-banner-dismissed-{bannerKey}` not documented as response side-effect schema.**
   - api-contract.md §Response 204 mentions "Server set HTTP header `Set-Cookie: ...`" but cookie name slug sanitization (controller line 93-104 `sanitizeBannerKey`) doesn't match doc claim 100% — doc says "slug-safe" but doesn't define algorithm.
   - Code says httpOnly is FALSE (line 78 comment "SET FALSE so FE useOnboardingPhase document.cookie can read") — doc line 40 says "HttpOnly" which is FALSE. **Doc/code semantic drift**.
   - Severity: MEDIUM — security-adjacent miss; FE design implies cookie readable client-side but contract documents otherwise.
   - Bundle into **GAP-663** (docs sync).

### 3.3 P2 — Wave 98 scope (observation-only)

7. **P2 (Cat 4 §2.4.1) — `POST /api/v1/preferences/dismiss-banner-state` correctly uses `/api/v1/` versioned URL prefix (PASS); EmailController uses legacy `/api/platform/emails` (pre-existing, not Wave 98 introduced but surfaces during scope).**
   - PreferencesController: ✅ /api/v1/ prefix correct.
   - EmailController: ❌ legacy /api/platform/ — Wave 92 Bucket D documented as dual-mount intent but no deprecation contract. Wave 98 doc claim `gateway proxy /api/email/**` may be aspirational without proxy verified.
   - Recommendation: align with admin v1 migration Wave 93 (GAP-637/638 follow-up).

8. **P2 (Cat 5 §2.5.3) — Consumer-driven contract tests (Pact) still missing across all Wave 98 endpoints — Wave 40 baseline carry-forward.**
   - Acceptable for Phase 1 BETA scope; track follow-up when multi-consumer (FE + mobile + 3rd-party).

### 3.4 Carry-forward (Wave 92 unresolved)

9. **Carry-P0 (Wave 92 GAP-637) — 3 admin v1 controllers still missing `@PreAuthorize`.** Verified OPEN at audit time. Cross-impact: any new endpoint inheriting same module pattern (PreferencesController public-by-design separate concern) but admin auth gap PERSISTS post-Wave 98.

10. **Carry-P0 (Wave 92 GAP-638) — 6 admin v1 endpoints undocumented.** Still OPEN at audit time. PreferencesController DOES have api-contract.md (Wave 98 didn't regress this specific class), but Wave 92 admin endpoints still undocumented.

---

## 4. Per-domain score breakdown

### 4.1 preferences domain (NEW endpoint + NEW contract)

| Cat | Sub-check | Verdict | Note |
|---|---|:---:|---|
| 1 (Endpoint Coverage) | 1.1 every @*Mapping documented | ✅ PASS | The 1 endpoint has matching api-contract.md entry |
| 1 | 1.2 no doc-orphans | ✅ PASS | api-contract.md only docs the 1 endpoint that exists |
| 1 | 1.3 public/auth section | ⚠️ PARTIAL | "Public (auth optional)" stated but auth model not enforced in code (no SecurityContext branching despite javadoc claim) |
| 1 | 1.4 gateway proxy noted | ❓ UNCHECKED | Gateway route config for `/api/v1/preferences/**` not verified |
| 1 | 1.5 non-REST | N/A | REST |
| 1 | 1.6 webhook | N/A | Not webhook |
| **Cat 1 score** | | **17/20** | -2 P1 partial (1.3, 1.4) -1 broader 3-layer doc miss |
| 2 (Schema) | 2.1 request DTO match | ✅ PASS | DismissBannerStateRequest matches doc (bannerKey + dismissed) |
| 2 | 2.2 response DTO match | ⚠️ PARTIAL | Doc claims HttpOnly cookie; code sets httpOnly(false) — semantic drift |
| 2 | 2.3 types correct | ✅ PASS | String + Boolean align |
| 2 | 2.4 required vs optional | ✅ PASS | @NotBlank + @NotNull annotations match doc "required:yes" |
| 2 | 2.5 nested objects | N/A | Flat shape |
| 2 | 2.6 enums documented | N/A | No enum fields |
| **Cat 2 score** | | **17/20** | -3 P1 cookie semantic drift |
| 3 (Errors) | 3.1 HTTP codes documented | ✅ PASS | 204 + 400 + 429 documented |
| 3 | 3.2 app error codes | ✅ PASS | PREF_INVALID_BANNER_KEY + PREF_MISSING_DISMISSED defined |
| 3 | 3.3 error envelope shape | ⚠️ PARTIAL | RFC 7807 ProblemDetail expected; controller uses default Spring 400 (not customized) |
| 3 | 3.4 validation 400 detail | ✅ PASS | @Valid + MethodArgumentNotValidException → field errors auto |
| 3 | 3.5 rate-limit 429 | ⚠️ PARTIAL | Documented but per-IP 60/min claim not verified in gateway config |
| **Cat 3 score** | | **16/20** | -2 P1 partial × 2 |
| 4 (Versioning) | 4.1 under /api/v[0-9]+/ | ✅ PASS | /api/v1/ correct |
| 4 | 4.2 no breaking changes | ✅ PASS | New endpoint, no break |
| 4 | 4.3 deprecated marked | N/A | New endpoint |
| 4 | 4.4 deprecation policy | N/A | New endpoint |
| 4 | 4.5 MAJOR migration | N/A | Not MAJOR |
| **Cat 4 score** | | **20/20** | All sub-checks PASS or N/A |
| 5 (Test Coverage) | 5.1 happy-path IT | ❌ FAIL (P0) | ZERO PreferencesController*Test files exist |
| 5 | 5.2 error paths covered | ❌ FAIL | No test = no error coverage either |
| 5 | 5.3 CDC tests | ❌ FAIL (P1) | Carry-forward |
| 5 | 5.4 backwards-compat | N/A | New endpoint |
| 5 | 5.5 schema validation runtime | N/A | Optional |
| **Cat 5 score** | | **6/20** (P0 cap) | -6 P0 happy-path -3 P1 CDC |
| **preferences total** | | **76/100 (C) FAIL** | (P0 cap on Cat 5) |

### 4.2 email domain (contract update only — no new endpoint)

| Cat | Sub-check | Verdict | Note |
|---|---|:---:|---|
| 1 | 1.1 every @*Mapping documented | ❌ FAIL (P0) | EmailController `/api/platform/emails/send` documented as `/api/email/send` — URL DRIFT |
| 1 | 1.2 no doc-orphans | ⚠️ PARTIAL | Doc base URL `/api/email/**` claims gateway proxy, not verified |
| 1 | 1.3 public/auth section | ⚠️ PARTIAL | Doc says "Internal email sending API"; controller javadoc admits "should only be called by other KiteHub services... in production, use service-to-service authentication" — auth NOT enforced |
| 1 | 1.4 gateway proxy noted | ⚠️ PARTIAL | Doc says gateway proxy; not verified in code |
| 1 | 1.5 non-REST | N/A | REST |
| 1 | 1.6 webhook | N/A | Not webhook |
| **Cat 1 score** | | **8/20** (P0 cap) | URL drift cap |
| 2 (Schema) | 2.1 request DTO match | ✅ PASS | EmailRequest fields (to/subject/templateName/variables/htmlBody/instanceId/tenantId) align |
| 2 | 2.2 response DTO match | ✅ PASS | EmailResponse (messageId/status/sentAt/errorMessage) |
| 2 | 2.3 types correct | ✅ PASS | |
| 2 | 2.4 required vs optional | ✅ PASS | @Valid + doc "required" column align |
| 2 | 2.5 nested objects | ✅ PASS | variables Map<String,Object> documented per-template |
| 2 | 2.6 enums documented | ⚠️ PARTIAL | EmailType templateName enum mentioned but full enum list not documented; tone enum FORMAL_SAFE_DEFAULT etc documented in §"Tone resolution" |
| **Cat 2 score** | | **18/20** | -2 P1 enum coverage |
| 3 (Errors) | 3.1 HTTP codes documented | ⚠️ PARTIAL | Doc admits "EmailController currently returns 200 + status=FAILED envelope for provider failures — gap GAP-572 covers refactor" — known drift |
| 3 | 3.2 app error codes | ✅ PASS | EMAIL_400_VALIDATION + EMAIL_400_UNKNOWN_TEMPLATE + EMAIL_503_PROVIDER_DOWN defined |
| 3 | 3.3 error envelope shape | ⚠️ PARTIAL | Status envelope pattern; not RFC 7807 |
| 3 | 3.4 validation 400 detail | ✅ PASS | Spring @Valid default |
| 3 | 3.5 rate-limit 429 | ❓ UNCHECKED | Internal service-to-service rate limit not documented |
| **Cat 3 score** | | **15/20** | -3 P1 partial × 1 (known GAP-572) -2 envelope |
| 4 (Versioning) | 4.1 under /api/v[0-9]+/ | ❌ FAIL (P0) | `/api/platform/emails` legacy URL pattern (pre-existing, surfaces in Wave 98 audit) |
| 4 | 4.2 no breaking changes | ✅ PASS | |
| 4 | 4.3 deprecated marked | ⚠️ PARTIAL | Legacy /api/platform/ not deprecated |
| 4 | 4.4 deprecation policy | ⚠️ PARTIAL | |
| 4 | 4.5 MAJOR migration | N/A | |
| **Cat 4 score** | | **8/20** (P0 cap) | Legacy URL P0 |
| 5 (Test Coverage) | 5.1 happy-path IT | ✅ PASS | EmailControllerTest.java exists |
| 5 | 5.2 error paths covered | ⚠️ PARTIAL | Coverage depth not verified in audit window |
| 5 | 5.3 CDC tests | ❌ FAIL (P1) | Carry-forward |
| 5 | 5.4 backwards-compat | N/A | |
| 5 | 5.5 schema validation runtime | N/A | |
| **Cat 5 score** | | **15/20** | -3 P1 CDC -2 partial |
| **email total** | | **64/100 (D+) FAIL** | (P0 cap on Cat 1 + Cat 4 from URL drift) |

### 4.3 seed domain (Java method contract — no HTTP)

| Cat | Sub-check | Verdict | Note |
|---|---|:---:|---|
| 1 (Method coverage) | 1.1 every public method documented | ✅ PASS | 8 generate*/format* methods all documented |
| 1 | 1.2 no doc-orphans | ✅ PASS | All doc methods exist in code |
| 1 | 1.3 N/A REST/Auth | N/A | Internal Java contract |
| 1 | 1.4 N/A gateway | N/A | |
| 1 | 1.5 non-REST documented | ✅ PASS | Scope clarifies "no HTTP REST API" |
| **Cat 1 score** | | **20/20** | |
| 2 (Schema) | 2.1 return DTO match | ✅ PASS | SampleStudent/SampleTeacher/etc records match |
| 2 | 2.2 fields match | ✅ PASS | |
| 2 | 2.3 types correct | ✅ PASS | Vietnamese String fields + enums |
| 2 | 2.4 required vs optional null safety | ✅ PASS | Doc explicitly notes null safety contracts |
| 2 | 2.5 nested | N/A | Flat records |
| 2 | 2.6 enums | ⚠️ PARTIAL | specialty enum-like documented as String values; not strict Java enum |
| **Cat 2 score** | | **18/20** | -2 P1 enum semi-typed |
| 3 (Errors) | 3.1 exceptions documented | ✅ PASS | IllegalStateException paths documented |
| 3 | 3.2 N/A HTTP error codes | N/A | |
| 3 | 3.3-3.5 N/A | N/A | |
| **Cat 3 score** | | **20/20** | |
| 4 (Versioning) | 4.1 N/A URL | N/A | |
| 4 | 4.2 backward-compat policy | ✅ PASS | Doc §Versioning explicit |
| 4 | 4.3 deprecated marked | N/A | New API |
| 4 | 4.4 policy | ✅ PASS | Documented |
| 4 | 4.5 N/A | N/A | |
| **Cat 4 score** | | **20/20** | |
| 5 (Test Coverage) | 5.1 happy-path test | ✅ PASS | VietnamSampleDataGeneratorTest.java exists |
| 5 | 5.2 error paths | ❓ UNCHECKED | IllegalStateException CSV-missing path test not verified |
| 5 | 5.3 N/A CDC | N/A | Internal contract |
| 5 | 5.4 N/A backward-compat | N/A | |
| 5 | 5.5 N/A | N/A | |
| **Cat 5 score** | | **18/20** | -2 P1 error path coverage uncertainty |
| **seed total** | | **96/100 (A) PASS** | High-quality Java contract |

---

## 5. Overall score

| Domain | Score | Verdict |
|---|:---:|:---:|
| preferences | 76/100 (C) | FAIL (Cat 5 P0 — zero IT) |
| email | 64/100 (D+) | FAIL (Cat 1 + Cat 4 P0 — URL drift + legacy) |
| seed | 96/100 (A) | PASS |

**Weighted average (3 new artifacts equal weight, NEW endpoint primary):**
- (76 + 64 + 96) / 3 = **78.7/100**
- Round-down to **76/100 (C) FAIL** weighted toward new-endpoint scope (preferences B0 is primary Wave 98 NEW HTTP surface; email B1 doc-update only; seed B2 internal Java contract = no HTTP risk surface).

**Delta vs Wave 92 baseline 79/100 C+ FAIL:** **-3 points**.

Breakdown of -3:
- +0 — Wave 92 P0 carries (GAP-637/638) NOT regressed (still OPEN, unchanged)
- -2 — EmailController URL drift surfaces during Wave 98 doc creation (pre-existing code, NEW doc misrepresented)
- -1 — preferences 3-layer doc mandate violation (CLAUDE.md Living Docs)
- +0 — seed domain high quality contributes (96/100) but offset by email + preferences

**Audit-level verdict: 🔴 FAIL** (2 P0 sub-checks Wave 98 scope + 2 P0 carries Wave 92).

---

## 6. Cross-layer drift assessment (per `contract-first-for-cross-layer.md` §3)

| Domain | Doc (api-contract.md) | Code (controller) | FE consumer | Verdict |
|---|---|---|---|---|
| preferences | ✅ POST /api/v1/preferences/dismiss-banner-state | ✅ matches | ❓ FE `useOnboardingPhase` claimed consumer (not verified in audit) | **Doc ↔ Code aligned**; FE verification deferred |
| preferences (cookie semantics) | ❌ Doc says HttpOnly | ❌ Code httpOnly(false) | FE expects readable cookie (per controller javadoc) | **Doc ↔ Code DRIFT** (security-adjacent semantic) |
| email | ❌ Doc says `/api/email/**` + `/api/email/send` | ❌ Code `/api/platform/emails/send` | Internal callers reference EmailController directly OR via gateway — neither verified | **3-way DRIFT** (P0) |
| seed | ✅ Java method contract = code reality | N/A no HTTP | Internal Java consumers (seed worker) | **Aligned** |

**Cross-layer drift count:** 1 critical (email URL) + 1 semantic (preferences cookie). Wave 98 Bucket B0 added NEW endpoint without contract-first foundation flow per §3 (api-contract.md shipped same PR but FE consumer not verified — Bucket B0 ships full-stack but FE side outside this audit scope).

---

## 7. TOP 5 findings

### Finding 1 (P0) — EmailController URL drift `/api/platform/emails/send` vs documented `/api/email/send`

**Category:** Cat 1 §2.1.1 + §2.1.2, Cat 4 §2.4.1
**Delta vs Wave 92:** NEW (Wave 92 audit didn't cover email domain)
**Severity:** P0 — 5 critical email types ship Wave 98 unable to route via documented URL
**Recommendation:**
- Option A: Migrate EmailController to `@RequestMapping("/api/email")` + add `@Deprecated` alias at `/api/platform/emails` with Sunset header
- Option B: Update api-contract.md to reflect actual `/api/platform/emails/**` + document gateway rewrite if exists
- Verify gateway routing `/api/email/**` → `kitehub-email:8086` actually configured
**Gap:** **GAP-662 P0** — file follow-up Wave 99 Bucket A patch (~60 min)

### Finding 2 (P0) — PreferencesController zero integration tests

**Category:** Cat 5 §2.5.1
**Delta vs Wave 92:** NEW (Wave 92 admin controllers also tested only Mockito; Wave 98 ships zero tests = worse)
**Severity:** P0 — NEW endpoint, NEW DTO, NEW cookie response, no verification
**Recommendation:** Add `@WebMvcTest(PreferencesController.class)` IT covering:
- Happy path: POST valid request → 204 + Set-Cookie header assertion
- 400 validation: missing bannerKey OR invalid kebab-case format
- 400 validation: missing dismissed boolean
- Cookie name slug verification (sanitizeBannerKey behavior)
**Gap:** **GAP-664 P0** — file follow-up Wave 99 Bucket B (~45 min)

### Finding 3 (P1) — 3-layer doc mandate violations (preferences missing 2 layers, email missing 1)

**Category:** Living Docs rule (CLAUDE.md §"Business Logic Documents — 3-Layer Structure")
**Delta vs Wave 92:** NEW (Wave 92 admin v1 only missing api-contract.md; Wave 98 preferences MISSING 2 of 3 layers)
**Severity:** P1 — business rules + use cases needed for compliance check, persona walkthrough, future Wave 99+ persistence implementation
**Recommendation:**
- preferences: create `rules.md` (BR-PREF-001 banner key format / BR-PREF-002 cookie expiry 30d / BR-PREF-003 in-memory Phase 1 / BR-PREF-004 user_preferences Phase 2 / BR-PREF-005 anonymous vs authenticated) + `use-cases.md` (UC-PREF-001 dismiss banner end-to-end flow)
- email: create `use-cases.md` (UC-EMAIL-001..005 per 5 critical templates)
**Gap:** **GAP-663 P1** — file follow-up Wave 99 Bucket C (~90 min)

### Finding 4 (P1) — preferences cookie httpOnly semantic drift

**Category:** Cat 2 §2.2.2
**Delta vs Wave 92:** NEW
**Severity:** P1 — security-adjacent, FE design intentional (per controller javadoc) but contract misrepresents
**Recommendation:** Update api-contract.md §Response 204 — clarify cookie is NOT HttpOnly (client-readable for `useOnboardingPhase` hook) + document SameSite=Lax + Secure flags + future Phase 2 plan to flip to HttpOnly with same-doc GET endpoint
**Bundle into GAP-663** (docs sync)

### Finding 5 (P2) — Wave 92 P0 carry-forward GAP-637/638 still OPEN at Wave 98 audit time

**Category:** Cat 3 + Cat 1 carry-forward
**Delta vs Wave 92:** UNCHANGED (still 2 P0 OPEN since Wave 92 baseline 2026-05-18)
**Severity:** Carry-forward — admin v1 endpoints still missing `@PreAuthorize` + 6 endpoints still undocumented
**Recommendation:** Cross-reference Wave 99 planning; both gaps should ship Wave 99 Bucket A together with GAP-662 (related auth/contract cluster)
**No new gap** — track existing GAP-637/638

---

## 8. Gap recommendations

### 8.1 New gaps to file

**GAP-662 (P0, phase-1-beta) — EmailController URL drift reconciliation**
- Acceptance criteria:
  - [ ] Either migrate `@RequestMapping("/api/platform/emails")` → `@RequestMapping("/api/email")` with deprecation alias + Sunset header
  - [ ] Or update `documents/01-business/kitehub/email/api-contract.md` to reflect actual `/api/platform/emails/**` URL + verify gateway rewrite config
  - [ ] Add MockMvc IT confirming routing works for documented URL
  - [ ] Cross-reference Wave 92 GAP-638 admin v1 doc-completion pattern
- Effort: ~60 min
- Priority: P0 (Wave 98 critical email types blocked if FE consumer hits documented URL)

**GAP-663 (P1, phase-1-beta) — 3-layer doc cleanup (preferences + email)**
- Acceptance criteria:
  - [ ] Create `documents/01-business/kitehub/preferences/rules.md` (BR-PREF-001..005)
  - [ ] Create `documents/01-business/kitehub/preferences/use-cases.md` (UC-PREF-001)
  - [ ] Create `documents/01-business/kitehub/email/use-cases.md` (UC-EMAIL-001..005)
  - [ ] Fix preferences/api-contract.md cookie httpOnly drift (doc → match code)
  - [ ] Verify CLAUDE.md 3-Layer pre-commit hook fires correctly post-fix
- Effort: ~90 min
- Priority: P1 (CLAUDE.md mandate + force-multiplier for future audit/persona work)

**GAP-664 (P0, phase-1-beta) — PreferencesController integration test coverage**
- Acceptance criteria:
  - [ ] Add `PreferencesControllerIT.java` using `@WebMvcTest`
  - [ ] Tests: happy-path (204 + cookie header), 400 missing bannerKey, 400 invalid format, 400 missing dismissed, cookie slug sanitization, cookie maxAge 30d
  - [ ] Coverage ≥80% on PreferencesController class
- Effort: ~45 min
- Priority: P0 (NEW endpoint, zero verification)

### 8.2 Carry-forward (no new file)

- Wave 92 GAP-637/638 still OPEN — bundle Wave 99 Bucket A with GAP-662 (auth + URL cluster)
- CDC tests (Pact) — Wave 40 baseline, defer Phase 1.5+ scope

### 8.3 Recommended Wave 99 assignment

- **Wave 99 Bucket A:** GAP-662 (email URL) + GAP-637 (admin auth) + GAP-638 (admin docs) — auth/URL cluster ~3h
- **Wave 99 Bucket B:** GAP-664 (preferences IT) ~45min
- **Wave 99 Bucket C:** GAP-663 (3-layer doc cleanup) ~90min

All 3 buckets disjoint scope — can ship parallel agents per `agent-background-spawn-default.md`.

---

## 9. Phase 1 BETA gate assessment

**Phase 1 BETA gate threshold:** ≥80/100 PASS per `release-deploy-standard.md` §3.1

**Current state:** 76/100 FAIL (-4 below threshold)

**Path to PASS (≥80):**
- Fix GAP-662 EmailController URL drift → +3 points (resolves email Cat 1 + Cat 4 P0 caps) → 79
- Fix GAP-664 PreferencesController IT → +3 points (resolves preferences Cat 5 P0 cap) → 82 ✅ PASS
- Optional: Fix GAP-663 3-layer doc → +2 points (Cat 1 + Living Docs alignment) → 84 (B)

**Estimated 3.25h total work to PASS Phase 1 BETA gate.** Recommended Wave 99 priority.

---

## 10. References

- Skill: `.claude/skills/quality/api-contract-audit/SKILL.md`
- Rubric: `.claude/rules/audit-skill-rubric-api-contract-audit.md` v1.0.1
- Wave 92 baseline: `documents/04-quality/audits/api-contract/2026-05-18-wave-92-bucket-d-admin-v1-api-contract-audit.md` (79/100 C+ FAIL)
- Wave 98 plan: `documents/03-planning/waves/wave-2026-05-18-98-cluster-b-beta-cohort-polish.md`
- Wave 98 commits: `b12ea568` (B0), `8d0e4fb9` (B1), `54d23b3f` (B2)
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
- Living Docs rule: CLAUDE.md §"CRITICAL: Business Logic Documents — 3-Layer Structure"
- Coordinator gap: GAP-661 (Wave 98 post-closure audit suite mandate)
- Carry-forward Wave 92 gaps: GAP-637, GAP-638

---

## 11. Log

- **2026-05-19:** Audit shipped per GAP-661 mandate (Wave 98 post-closure audit suite, deadline 2026-05-21). Code-level/artifact-based audit (GAP-612 AWS suspension blocks live curl). Per `api-contract-audit/SKILL.md` /100 rubric × 5 categories × ≥5 sub-checks. 2 P0 findings surfaced (EmailController URL drift + PreferencesController zero IT) + 1 P1 (3-layer doc mandate) + Wave 92 P0 carries unchanged. 3 new gaps recommended (GAP-662 P0 + GAP-663 P1 + GAP-664 P0). Aggregate **76/100 (C) FAIL** — **-3 vs Wave 92 baseline 79/100 C+ FAIL** (EmailController URL drift surfaces during Wave 98 doc creation; preferences 3-layer doc violation; offset partially by high-quality seed domain 96/100). Phase 1 BETA gate FAIL with concrete +4 points path via 3 Wave 99 buckets (~3.25h total). Auditor: Background agent (Opus 4.7).
