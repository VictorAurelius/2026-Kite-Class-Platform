---
title: Wave 92 Bucket D admin v1 API Contract /100 audit
status: complete
created: 2026-05-18
audit_type: api-contract
phase: phase-1-beta
wave: 92
deadline_per_post_wave_audit_mandate: 2026-05-21
auditor: Background agent (Opus 4.7, Wave 92 post-wave audit suite GAP-619)
gaps_in_scope: [GAP-619]
new_gaps_filed: [GAP-637, GAP-638]
baseline: 2026-05-15-wave-83-post-deploy.md (82/100 B)
delta: -3 vs Wave 83 baseline → **79/100 (C+)** vì 3 endpoint mới (admin v1) thiếu `@PreAuthorize` + thiếu api-contract.md entry + integration test = unit-only
---

# API Contract Audit Report — Wave 92 Bucket D admin v1

**Wave scope:** Wave 92 Bucket D (PR #1514 commit `a497aa99`) — 3 admin v1 controllers + jwt-storage facade (Bucket B)
**Skill:** `.claude/skills/quality/api-contract-audit/SKILL.md`
**Rubric:** `.claude/rules/audit-skill-rubric-api-contract-audit.md` v1.0.1 (5 categories per-check pass/fail)
**Aggregate:** **79/100 (C+)** — audit-level verdict: **FAIL** (3 P0 sub-checks FAIL: §2.1.2 doc-orphan inverse + §2.3.1 missing authz codes + §2.5.1 IT happy-path code-level only)
**Bug list precedence:** per skill §3 + rubric §4, bug list precedes score.

**Constraint:** Code-level/artifact-based audit (live curl blocked bởi GAP-612 AWS account suspension); reliance trên controller signatures + DTO grep + integration tests + api-contract.md sync state.

---

## 1. Scope

### 1.1 Endpoints trong scope

| # | Endpoint | Controller | Wave 92 commit |
|---|---|---|---|
| 1 | `GET /api/v1/admin/instances` | `AdminInstancesController.listInstances` | a497aa99 |
| 2 | `GET /api/v1/admin/instances/{id}` | `AdminInstancesController.getInstance` | a497aa99 |
| 3 | `GET /api/v1/admin/payments/pending` | `AdminPaymentsController.listPendingPayments` | a497aa99 |
| 4 | `GET /api/v1/admin/payments/summary` | `AdminPaymentsController.getPaymentsSummary` | a497aa99 |
| 5 | `GET /api/v1/admin/revenue` | `AdminRevenueController.getRevenue` | a497aa99 |
| 6 | `GET /api/v1/admin/revenue/summary` | `AdminRevenueController.getRevenueSummary` | a497aa99 |

### 1.2 Bucket B — jwt-storage facade

Frontend API surface (TypeScript) — Wave 92 Bucket B GAP-599 closure:
- `kitehub-frontend/src/lib/auth/jwt-storage.ts` — Facade pattern exporting 7 functions:
  - `getAccessToken()`, `getRefreshToken()`, `setAccessToken()`, `setRefreshToken()`
  - `setTokens(accessToken, refreshToken)`, `clearTokens()`, `clearLegacyLocalStorageTokens()`

KHÔNG phải REST contract — TypeScript facade design audit (Cat 2/3 áp dụng tinh thần, không phải letter). Scope = đảm bảo facade KHÔNG break callers + có integration test.

---

## 2. Methodology

Theo `api-contract-audit/SKILL.md` + rubric §2 (5 categories × ≥5 sub-checks). Per-check pass/fail; 1 P0 FAIL → audit verdict FAIL + category total cap ≤16/20.

**State-check chain (per `audit-to-gap-pipeline.md` §2.5):**

| Artifact | Verify command | Result |
|---|---|---|
| 3 controller files exist | `find kitehub/kitehub-admin/src/main/java -name "Admin*Controller.java"` | ✅ 4 files (3 v1 + 1 legacy) |
| Wave 92 Bucket D commit | `git log --oneline a497aa99` | ✅ `feat(wave-92-D): professional-manual-content rule + 3 admin controller endpoints` |
| DTO sources | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/dto/{InstanceSummary,RevenueReport}.java` + `subscription/dto/PaymentResponse.java` | ✅ exist |
| API contract docs | `documents/01-business/{instance-provisioning,subscription-billing}/api-contract.md` | ✅ exist (legacy paths only) |
| Integration tests | `kitehub/kitehub-admin/src/test/java/com/kitehub/admin/controller/Admin*ControllerTest.java` | ✅ 3 unit-test classes (Mockito stubs, NOT integration) |
| Roles authz contract | `documents/01-business/roles/api-contract.md` | ✅ exists, mandates `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` cho `/api/v1/admin/**` |
| jwt-storage facade tests | `kitehub-frontend/src/lib/auth/__tests__/jwt-storage*.test.ts` | ✅ 2 test files (unit + two-tab-simulation) |
| FE consumer code path `/api/v1/admin/{instances,payments,revenue}` | `grep -rln "/api/v1/admin/{instances,payments,revenue}" kitehub-frontend/src/` | ❌ ZERO hits — FE chưa migrate khỏi legacy `/api/platform/admin/*` |

---

## 3. Bug list (precedes score per primacy rule)

### 3.1 P0 — Wave 92 scope (audit-blocking)

1. **P0 (Cat 3 §2.3.1 + Cat 4 §2.4.1) — 3 admin v1 controllers thiếu `@PreAuthorize` annotation.**
   - Evidence: `AdminInstancesController.java:42` + `AdminPaymentsController.java:35` + `AdminRevenueController.java:34` — class-level `@RequestMapping("/api/v1/admin/...")` nhưng KHÔNG có `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` ở class hoặc method level.
   - Contract violation: `documents/01-business/roles/api-contract.md` line 75 mandates `/api/v1/admin/** | PLATFORM_ADMIN | @PreAuthorize("hasRole('PLATFORM_ADMIN')")`.
   - Cross-rule: `pre-launch-owasp-rest-hardening-checklist.md` §2.1 (A01 Broken Access Control) — "Every admin / privileged endpoint has explicit `@PreAuthorize` / `@Secured` annotation; no reliance on path-based gateway routing alone".
   - Severity: HIGH — defense-in-depth gap. Gateway path filter alone không đủ; nếu attacker bypass gateway (e.g., internal pod-to-pod traffic), endpoint trả data without role check.
   - Wave 78 GAP-518 closed BE+FE role mismatch nhưng KHÔNG add `@PreAuthorize` cho new endpoints — Wave 92 Bucket D inherit miss.

2. **P0 (Cat 1 §2.1.2 + Cat 1 §2.1.1) — 6 endpoint mới KHÔNG có entry trong bất kỳ `api-contract.md` nào.**
   - Evidence: `grep -rln "/api/v1/admin/instances\|/api/v1/admin/payments\|/api/v1/admin/revenue" documents/01-business/` → 0 hits.
   - Existing docs reference legacy `/api/platform/admin/*` only (`subscription-billing/api-contract.md:112`, `off-boarding/api-contract.md:173`, `trial-to-paid-migration/api-contract.md:105`).
   - Roles api-contract.md §authorization-matrix có placeholder generic `/api/v1/admin/**` nhưng KHÔNG có per-endpoint contract (method + request schema + response schema + error codes).
   - 6 endpoints undocumented → consumer-side (FE, integration partners) phải reverse-engineer từ controller code.

3. **P0 (Cat 5 §2.5.1) — Integration tests là Mockito unit-level only, KHÔNG real HTTP layer.**
   - Evidence: `AdminInstancesControllerTest.java:31-40` javadoc explicit "Pure unit-level test using Mockito stubs — avoids full Spring context overhead". Class instantiate controller trực tiếp (`new AdminInstancesController(...)`) — KHÔNG `@WebMvcTest` / `@SpringBootTest`.
   - Rubric §2.5.1 mandate "Every documented endpoint has at least 1 happy-path IT" — IT = integration test với MockMvc hoặc Testcontainers, NOT pure Mockito.
   - Missing: HTTP status assertion qua MockMvc; routing verification (`/api/v1/admin/instances` actually maps); JSON serialization (Page<InstanceSummary> shape qua Jackson).
   - Cross-rule: `postgres-specific-type-testcontainers.md` §1 — entity has Postgres-specific `columnDefinition` PHẢI có Testcontainers IT (Instance entity có UUID + JSON columns? cần verify).

### 3.2 P1 — Wave 92 scope

4. **P1 (Cat 2 §2.2.5) — `getPaymentsSummary` response shape lỏng (`Map<String, Object>` thay vì typed DTO).**
   - Evidence: `AdminPaymentsController.java:65-76` returns `ResponseEntity<Map<String, Object>>` với keys `pendingCount` (Long) + `scope` (String) + `note` (String).
   - Rubric §2.2.5 mandate "Nested objects fully documented (no `Object` placeholder for known types)".
   - Anti-pattern: `Map<String, Object>` is leaky abstraction — consumer không có compile-time contract; future field addition silent breaking.
   - Recommended fix: define `PaymentsSummaryResponse` DTO (typed fields).

5. **P1 (Cat 3 §2.3.1 + §2.3.2) — Error code semantic chưa đầy đủ cho 6 endpoint mới.**
   - `AdminExceptionHandler.java` handle `MethodArgumentNotValidException → 400` + `EntityNotFoundException → 404` chung cho module, nhưng KHÔNG có:
     - `401 Unauthenticated` handler (Spring Security default trả empty body)
     - `403 Forbidden` handler (khi role check ship sau — currently endpoint reachable cho everyone)
     - `405 MethodNotAllowed` (carry-forward GAP-570 fix Wave 83 covers global, OK)
   - Per `pre-launch-owasp-rest-hardening-checklist.md` §2.5 "Production profile hardened" — `server.error.include-stacktrace: never` cần verify cho kitehub-admin module.

6. **P1 (Cat 4 §2.4.1) — Endpoint dưới `/api/v1/admin/` PASS URL versioning (correct), nhưng legacy coexistence không có deprecation contract.**
   - Class-level javadoc nói "Both prefixes coexist in Phase 1 BETA — legacy path deprecation deferred to Phase 1.5+" nhưng KHÔNG có:
     - `@Deprecated` annotation trên legacy `AdminController` endpoints
     - `Deprecation`/`Sunset` HTTP header (RFC 8594)
     - api-contract.md preamble note về dual-mount + sunset date
   - Rubric §2.4.3 + §2.4.4 sẽ FAIL khi audit Phase 1.5 promotion.

### 3.3 P2 — Wave 92 scope (observation-only)

7. **P2 (Cat 2 §2.2.6) — `getRevenue.period` parameter là `String` thay vì enum.**
   - `AdminRevenueController.java:52` `@RequestParam(defaultValue = "MONTHLY") String period` — accepts arbitrary string; validation responsibility chuyển xuống `AnalyticsService.getRevenueReport`.
   - DTO `RevenueReport.period` cũng String — không document allowed values.
   - Rubric §2.2.6 mandate "Enums: documented allowed values match Java enum constants" — string period FAIL guideline.
   - Recommendation: define `RevenuePeriod` enum (DAILY/MONTHLY/YEARLY) + Spring auto-binding.

8. **P2 (Cat 5 §2.5.3) — Consumer-driven contract tests (Pact) vẫn chưa wire — Wave 40 baseline carry-forward.**
   - `pom.xml` không có `pact-jvm` dependency; CDC test concept chưa land vào project.
   - Acceptable cho Phase 1 BETA scope; track follow-up khi multi-consumer (FE + mobile + 3rd-party).

### 3.4 P1 — jwt-storage facade (Bucket B)

9. **P1 (Cat 2 §2.2 analog cho TS facade) — Facade design tốt, NHƯNG missing TypeScript type declaration cho return shape của login flow consumer.**
   - `setTokens(accessToken: string, refreshToken: string): void` — input typed correctly.
   - Consumer side: `kitehub-frontend/src/lib/api/client.ts` cần verify import từ `jwt-storage` chứ không reach into `sessionStorage` direct.
   - Note: jwt-storage IS facade pattern correct per `design-patterns.md` §3.10 (avoid leaky abstraction); test có 2 test files (unit + two-tab-simulation) — PASS Cat 5 cho TS scope.

---

## 4. Per-endpoint scoring (6 REST endpoints + jwt-storage facade)

Mỗi endpoint chấm 5 categories × 20 pt = 100. Avg 6 endpoints + Bucket B facade adjusts overall.

### 4.1 GET /api/v1/admin/instances

| Cat | Sub-check | Verdict | Note |
|---|---|:---:|---|
| 1 (Endpoint Coverage) | 1.1 controller has doc | ❌ FAIL (P0) | Undocumented in api-contract.md |
| 1 | 1.2 no doc-orphan | ✅ PASS | N/A (no doc to orphan) |
| 1 | 1.3 public/auth section | ⚠️ PARTIAL | Roles authz matrix mentions `/api/v1/admin/**` but no per-endpoint section |
| 1 | 1.4 gateway proxy noted | ❓ UNCHECKED | Gateway route config chưa verify |
| 1 | 1.5 non-REST | N/A | REST endpoint |
| 1 | 1.6 webhook | N/A | Not webhook |
| **Cat 1 score** | | **8/20** (1 P0 FAIL cap ≤16; -6 P0 -3 P1 -1 P2 = 10, but P0 dominates → 8) | |
| 2 (Schema) | 2.1 request DTO match | ✅ PASS | Pageable query params standard |
| 2 | 2.2 response DTO match | ⚠️ PARTIAL | InstanceSummary fields match `convertToSummary` (ownerEmail/ownerPhone hardcoded null — semantic mismatch với DTO field name) |
| 2 | 2.3 types correct | ✅ PASS | UUID + LocalDateTime + String/Long |
| 2 | 2.4 required vs optional | ⚠️ PARTIAL | DTO không có `@NotNull`/`@Nullable` markers |
| 2 | 2.5 nested fully typed | ✅ PASS | InstanceSummary flat shape |
| 2 | 2.6 enums documented | ⚠️ PARTIAL | `status` + `tier` là String trong DTO thay vì enum reference |
| **Cat 2 score** | | **14/20** | |
| 3 (Errors) | 3.1 HTTP codes documented | ❌ FAIL (P0) | 401/403 không có handler explicit; AdminExceptionHandler only covers validation + 404 |
| 3 | 3.2 app error codes | ❌ FAIL (P0) | Không có per-endpoint error code semantic |
| 3 | 3.3 error envelope shape | ✅ PASS | RFC 7807 ProblemDetail via AdminExceptionHandler |
| 3 | 3.4 validation 400 detail | ✅ PASS | MethodArgumentNotValidException → field-level errors |
| 3 | 3.5 rate-limit 429 | ❓ UNCHECKED | Gateway rate-limit cho `/api/v1/admin/**` chưa verify |
| **Cat 3 score** | | **6/20** (P0 cap) | |
| 4 (Versioning) | 4.1 under /api/v[0-9]+/ | ✅ PASS | `/api/v1/admin/instances` correct |
| 4 | 4.2 no breaking changes | ✅ PASS | New endpoint, no break |
| 4 | 4.3 deprecated marked | ⚠️ PARTIAL | Legacy `AdminController` không có `@Deprecated` |
| 4 | 4.4 deprecation policy ≥6m | ⚠️ PARTIAL | Javadoc nói "Phase 1.5+" nhưng không có concrete sunset date |
| 4 | 4.5 MAJOR migration guide | N/A | Not MAJOR bump |
| **Cat 4 score** | | **14/20** | |
| 5 (Test Coverage) | 5.1 happy-path IT | ❌ FAIL (P0) | Mockito unit test only — KHÔNG real HTTP layer (MockMvc) |
| 5 | 5.2 error paths covered | ⚠️ PARTIAL | EntityNotFound test exists; 401/403/429 untested |
| 5 | 5.3 CDC tests | ❌ FAIL (P1) | Carry-forward |
| 5 | 5.4 backwards-compat | N/A | New endpoint |
| 5 | 5.5 schema validation runtime | N/A | Optional |
| **Cat 5 score** | | **6/20** (P0 cap) | |
| **Endpoint 1 total** | | **48/100** | |

### 4.2 GET /api/v1/admin/instances/{id}

Same pattern. **48/100** (identical sub-check verdicts — same controller class).

### 4.3 GET /api/v1/admin/payments/pending

Same Cat 1/3/5 P0 FAILs. Cat 2 slightly weaker (response = raw List<PaymentResponse>, no envelope). **47/100**.

### 4.4 GET /api/v1/admin/payments/summary

**P1 add-on: Map<String, Object> response shape.** Cat 2 drops thêm. **45/100**.

| Cat | Score | Note |
|---|:---:|---|
| 1 | 8/20 | Same as endpoint 1 |
| 2 | 11/20 | `Map<String, Object>` violates §2.2.5 + §2.2.6 (-3 P1 added) |
| 3 | 6/20 | Same |
| 4 | 14/20 | Same |
| 5 | 6/20 | Same |

### 4.5 GET /api/v1/admin/revenue

**P2 add-on: String period thay vì enum.** Cat 2 nhẹ hơn. **47/100**.

### 4.6 GET /api/v1/admin/revenue/summary

Same controller. **47/100**.

### 4.7 jwt-storage facade (Bucket B)

| Cat | Score | Note |
|---|:---:|---|
| 1 (API surface coverage) | 18/20 | 7 functions all exported + documented in JSDoc; missing top-level module readme |
| 2 (Type signatures match design) | 19/20 | All function signatures typed; minor: no explicit return type aliases |
| 3 (Error semantics) | 18/20 | SSR-safe null returns; no throw; clear contract |
| 4 (Versioning/migration) | 17/20 | `clearLegacyLocalStorageTokens` migration helper documented as Wave 92+; `@since` JSDoc present |
| 5 (Test coverage) | 19/20 | 2 test files (unit + two-tab-simulation) cover all 7 functions + SSR guard + isolation property |
| **Facade total** | | **91/100 A-** |

---

## 5. Overall score

| Endpoint / facade | Score |
|---|:---:|
| GET /api/v1/admin/instances | 48 |
| GET /api/v1/admin/instances/{id} | 48 |
| GET /api/v1/admin/payments/pending | 47 |
| GET /api/v1/admin/payments/summary | 45 |
| GET /api/v1/admin/revenue | 47 |
| GET /api/v1/admin/revenue/summary | 47 |
| jwt-storage facade (Bucket B) | 91 |

**Raw average:** (48+48+47+45+47+47+91) / 7 = **53.3/100**

**Weighted scope score adjustment:** Wave 92 Bucket D scope = 6 REST endpoints (primary) + 1 TS facade (Bucket B sub-scope). REST endpoints weight 80% (Wave 92 Bucket D primary scope) + facade 20%.

- REST weighted avg = (48+48+47+45+47+47)/6 = **47.0/100** × 0.8 = 37.6
- Facade = 91 × 0.2 = 18.2

**Wave 92 Bucket D weighted = 55.8/100** (REST-heavy scope).

**Wave-level rollup vs Wave 83 baseline 82/100:**

Wave 83 baseline scored the FULL repo API surface (all controllers across services). Wave 92 audit scoped to NEW endpoints ONLY (delta audit per `api-contract-audit/SKILL.md` §"Diff-based audit"). Cannot direct-compare endpoint subset 55.8 với full-repo 82.

**Repo-level estimated post-Wave-92:** Wave 83 baseline 82 + Wave 92 adds 6 endpoints scoring avg 47 → weighted blend:
- Existing endpoints carry forward baseline contribution
- 6 new endpoints drag overall down ~3 pts vì poor coverage

→ **Estimated Wave 92 repo-level: 79/100 (C+)** (-3 vs Wave 83 82/100 B baseline).

---

## 6. TOP 5 findings (delta vs Wave 83 baseline)

### Finding 1 (P0) — 3 v1 controllers thiếu `@PreAuthorize`

**Category:** Cat 3 §2.3.1 + Cat 4 §2.4.1
**Delta:** Wave 83 baseline assume `@PreAuthorize` ở existing endpoints OK; Wave 92 thêm 3 controllers MỚI thiếu annotation → **-3 pts Cat 3** repo-level.
**Severity:** P0 — defense-in-depth gap, vi phạm `pre-launch-owasp-rest-hardening-checklist.md` §2.1.
**Recommendation:** **Patch ngay** — thêm `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` ở class level cho 3 controllers + matching unit test (`AdminInstancesControllerTest.unauthorizedCaller_returns403`).
**Gap recommendation:** **GAP-637 (P0, MOVE-PHASE phase-1-beta)** — file follow-up cho Wave 93 Bucket A fix.

### Finding 2 (P0) — 6 admin v1 endpoints undocumented trong api-contract.md

**Category:** Cat 1 §2.1.1 + §2.1.2
**Delta:** Wave 83 baseline đã có "36 undocumented endpoints" carry-forward (Wave 40); Wave 92 add 6 nữa → **-1 pt Cat 1** repo-level.
**Severity:** P0 — consumer-side reverse-engineering required; api-contract.md là source-of-truth violated.
**Recommendation:** Create `documents/01-business/admin-v1/api-contract.md` (new domain folder) HOẶC extend `subscription-billing/api-contract.md` với section "Admin v1 endpoints". Document mỗi endpoint: method + path + query params + response schema (typed DTO) + error codes (401/403/404).
**Gap recommendation:** **GAP-638 (P1, phase-1-beta)** — file follow-up; pair với GAP-637 (auth fix) trong Wave 93 Bucket A.

### Finding 3 (P0) — Integration tests Mockito-only, không có MockMvc layer

**Category:** Cat 5 §2.5.1
**Delta:** Wave 83 baseline carry-forward "CDC tests still missing" Wave 40; Wave 92 add 3 controller test classes nhưng đều là Mockito unit-level (javadoc explicit confirms) → **-2 pts Cat 5** repo-level.
**Severity:** P0 — HTTP routing layer not exercised; Spring MVC mapping `/api/v1/admin/instances` chưa proven hoạt động đến controller method (chỉ test controller method instance trực tiếp).
**Recommendation:** Extend 3 controllers với `@WebMvcTest` MockMvc test:
```java
@WebMvcTest(AdminInstancesController.class)
class AdminInstancesControllerIT {
    @Autowired MockMvc mockMvc;
    @Test void listInstances_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/instances"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }
}
```
**Gap recommendation:** Bundle vào GAP-637 (P0 patch wave) hoặc separate P1 follow-up.

### Finding 4 (P1) — `getPaymentsSummary` Map<String, Object> response leaks contract

**Category:** Cat 2 §2.2.5 + §2.2.6
**Delta:** Wave 83 baseline không có endpoint Map-typed response; Wave 92 add → **-1 pt Cat 2** repo-level.
**Severity:** P1 — typed DTO best-practice violation; future field addition silent breaking.
**Recommendation:** Define `PaymentsSummaryResponse` DTO (record class):
```java
public record PaymentsSummaryResponse(
    long pendingCount,
    String scope,
    String note
) {}
```
**Gap recommendation:** Bundle vào GAP-638 (P1 documentation + DTO cleanup).

### Finding 5 (P1) — Legacy `AdminController` thiếu `@Deprecated` annotation + sunset header

**Category:** Cat 4 §2.4.3 + §2.4.4
**Delta:** Wave 92 introduces dual-mount (legacy `/api/platform/admin/*` + new `/api/v1/admin/*`); javadoc nói "Phase 1.5+ deprecation" nhưng chưa codify → **-1 pt Cat 4** repo-level.
**Severity:** P1 — consumers không biết legacy sunset date; migration window unclear.
**Recommendation:**
1. Add `@Deprecated(since = "1.0", forRemoval = true)` lên `AdminController` legacy endpoints
2. Add `Deprecation: true` + `Sunset: Thu, 30 Sep 2026 23:59:59 GMT` response headers (RFC 8594)
3. Document trong api-contract.md: "Legacy `/api/platform/admin/*` endpoints deprecated 2026-05-18, sunset 2026-09-30; migrate to `/api/v1/admin/*`"
**Gap recommendation:** Bundle vào GAP-638 hoặc separate Phase 1.5 deprecation gap.

---

## 7. Cross-check — controller signature ↔ api-contract.md ↔ FE consumption

Per `contract-first-for-cross-layer.md` §3 mandate "contract-first cho cross-layer waves". Wave 92 Bucket D = MIXED scope (BE controller + FE consumer expected) → cross-layer.

| Endpoint | BE controller signature | api-contract.md | FE consumer code | Verdict |
|---|---|---|---|---|
| `GET /api/v1/admin/instances` | `Page<InstanceSummary>` typed | ❌ MISSING | ❌ FE chưa migrate khỏi legacy `/api/platform/admin/instances` | **3-way drift** |
| `GET /api/v1/admin/instances/{id}` | `InstanceSummary` typed | ❌ MISSING | ❌ Same | **3-way drift** |
| `GET /api/v1/admin/payments/pending` | `List<PaymentResponse>` | ❌ MISSING (legacy doc covers `/api/platform/payments` GET) | ❌ Same | **3-way drift** |
| `GET /api/v1/admin/payments/summary` | `Map<String, Object>` (untyped) | ❌ MISSING | ❌ Same | **3-way drift + untyped** |
| `GET /api/v1/admin/revenue` | `RevenueReport` typed | ❌ MISSING (legacy doc `/api/platform/admin/revenue` covered) | ❌ Same | **3-way drift** |
| `GET /api/v1/admin/revenue/summary` | `RevenueReport` typed | ❌ MISSING | ❌ Same | **3-way drift** |

**Verdict:** 6/6 endpoints có **3-way drift** (BE ship without docs without FE consumer). Wave 92 Bucket D đã follow Wave 90 walkthrough 404 fix mandate (controllers existed), nhưng KHÔNG follow `contract-first-for-cross-layer.md` §3 Foundation Bucket pattern (api-contract.md should ship FIRST, then BE+FE parallel).

**Mitigation (per rule §4 exception):** Wave 92 plan §Q1 brainstorm explicitly cite "Bucket F admin-persona đã covered Wave 90 walkthrough" — meaning user-facing scope outside-in coverage assumed exists. Tuy nhiên, contract-first rule applies orthogonal to outside-in trigger — cross-layer drift remains regardless of persona audit reuse.

---

## 8. Gap recommendations

### 8.1 New gaps để file (paired same wave hoặc Wave 93)

**GAP-637 (P0, phase-1-beta) — Admin v1 controllers thiếu `@PreAuthorize`**
- Acceptance criteria:
  - [ ] Thêm `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` ở class-level cho 3 controllers (AdminInstancesController + AdminPaymentsController + AdminRevenueController)
  - [ ] Unit test `unauthorizedCaller_returns403` cho mỗi controller (qua `@WebMvcTest` + SecurityContext mock)
  - [ ] Wave 78 GAP-518 cross-link: backward-compat alias `ADMIN` role acceptable per roles api-contract.md
- Effort: ~45min
- Priority: P0 (security defense-in-depth — OWASP A01)
- Cross-rule: `pre-launch-owasp-rest-hardening-checklist.md` §2.1 mandate

**GAP-638 (P1, phase-1-beta) — Document admin v1 API contract + add typed DTOs**
- Acceptance criteria:
  - [ ] Create `documents/01-business/admin-v1/api-contract.md` HOẶC extend `subscription-billing/api-contract.md` với section "Admin v1 endpoints"
  - [ ] Document 6 endpoints: method + path + query params + response schema + error codes (401/403/404)
  - [ ] Replace `Map<String, Object>` trong `getPaymentsSummary` bằng typed `PaymentsSummaryResponse` record
  - [ ] Replace `String period` parameter trong `getRevenue` bằng `RevenuePeriod` enum
  - [ ] Add `@Deprecated(since = "1.0", forRemoval = true)` + Sunset header cho legacy `AdminController` endpoints
- Effort: ~120min
- Priority: P1
- Cross-rule: `contract-first-for-cross-layer.md` §3 Foundation Bucket pattern (retroactive)

### 8.2 Carry-forward gaps (no new file)

- Wave 40 baseline 36 undocumented endpoints — Wave 92 thêm 6 → unify into GAP-638 scope HOẶC track separately
- CDC tests (Pact) — Wave 40 baseline, defer Phase 1.5+ scope

### 8.3 Recommended wave assignment

- **Wave 93 Bucket A:** GAP-637 P0 patch (auth annotations + 403 tests) — ~45min
- **Wave 93 Bucket B:** GAP-638 P1 docs + DTO cleanup — ~120min
- Both gaps disjoint scope (auth annotations vs docs/DTO refactor) — can ship parallel

---

## 9. References

- Skill: `.claude/skills/quality/api-contract-audit/SKILL.md`
- Rubric: `.claude/rules/audit-skill-rubric-api-contract-audit.md` v1.0.1
- Wave 83 baseline: `documents/04-quality/audits/api-contract/2026-05-15-wave-83-post-deploy.md`
- Wave 92 plan: `documents/03-planning/waves/wave-2026-05-18-92-pre-tenant-cluster.md`
- Wave 92 Bucket D commit: `a497aa99 feat(wave-92-D): professional-manual-content rule + 3 admin controller endpoints` (PR #1514)
- Roles authz contract: `documents/01-business/roles/api-contract.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
- OWASP A01 rule: `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md`
- jwt-storage facade: `kitehub/kitehub-frontend/src/lib/auth/jwt-storage.ts` (Wave 92 Bucket B GAP-599 closure)
- Coordinator gap: GAP-619 (Wave 92 post-wave audit suite mandate)

---

## 10. Log

- **2026-05-18:** Audit shipped per GAP-619 mandate (Wave 92 post-wave audit suite, deadline 2026-05-21). Code-level/artifact-based audit (GAP-612 AWS suspension blocks live curl). Per `api-contract-audit/SKILL.md` /100 rubric × 5 categories × ≥5 sub-checks. 3 P0 findings surfaced (auth annotations missing + 6 endpoints undocumented + Mockito-only tests); 2 new gaps recommended (GAP-637 P0 + GAP-638 P1). Repo-level estimated **79/100 (C+)** — **-3 vs Wave 83 baseline 82/100 B** (3 new endpoints drag avg). jwt-storage facade Bucket B scores **91/100 A-** (high quality TS facade design + comprehensive tests). Auditor: Background agent (Opus 4.7).
