# GAP-730: Idempotency POST narrow (signup + enrollment + beta-request) — defer Wave beta-readiness-2 (agent blocked)

**Status:** 🟢 DONE (Wave local-doable-10 Bucket A — 2026-06-02)
**Priority:** 🔴 P0
**Domain:** Backend (kiteclass-core + kitehub-subscription)
**Detected:** 2026-05-24 (Wave beta-readiness-1 Bucket C scope — agent blocked, defer)
**Affects:** Signup endpoint + enrollment endpoint + beta-request endpoint — POST mutation safety on retry

## Problem

Wave beta-readiness-1 Bucket C scope = port `PaymentIdempotencyService` pattern (Wave 105 Bucket D) to 3 controllers (signup + enrollment + beta-request). Agent execution blocked do content filter policy mid-implementation (no PR shipped).

Currently 3 POST mutation endpoints accept duplicate requests → potential double-create:
- POST `/api/v1/auth/signup` (or equivalent) → user clicks submit 2x → 2 accounts
- POST `/api/v1/enrollment/{classId}/enroll` → 2x enroll → 2 enrollment rows (may bypass capacity per GAP-727)
- POST `/api/v1/beta-request` → 2x beta request → 2 pending records

PaymentController already has `PaymentIdempotencyService` (Wave 105 Bucket D) — proven pattern reusable.

## V2 Audit Evidence

`documents/04-quality/audits/persona-review/2026-05-24-outside-in-phase-1-closure-failure-mode-matrix-v2-state-checked.md` A3:

> **Idempotency POST mutations** — VERIFIED-PARTIAL: `PaymentIdempotencyService.java` EXISTS for payment; signup + enrollment + beta-request unverified (likely missing)

## Proposed Fix (Wave beta-readiness-2 Bucket — recommend)

1. **Design decision:** SHARED `idempotency_keys` table với `(tenant_id, idempotency_key, scope)` composite PK; `scope` enum (PAYMENT existing, SIGNUP, ENROLLMENT, BETA_REQUEST)
2. **Migration:** `V*__shared_idempotency_keys.sql` (post-V65 from Wave beta-readiness-1 Bucket B)
3. **Service:** `IdempotencyService` (shared `common/idempotency/` package):
   - `Optional<IdempotencyRecord> findExisting(tenantId, key, scope)`
   - `void recordRequest(tenantId, key, scope, userId, requestHash, responseStatus, responseBody)`
   - `DuplicateKeyException` → return cached response
4. **Apply:**
   - Signup controller — wraps registration endpoint
   - EnrollmentController — wraps enroll endpoint (interacts với Wave beta-readiness-1 Bucket B capacity check — order matters)
   - Beta-request controller (kitehub-platform side likely)
5. **Option A** (annotation): `@Idempotent(scope = Scope.SIGNUP)` + Aspect intercept POST controllers reading `Idempotency-Key` header
6. **Option B** (inline): explicit service call trong mỗi controller (less DRY, no AOP)
7. **IT test:** per controller submit POST với same `Idempotency-Key` 2x → 2nd request returns cached response, NOT duplicate DB row

## Acceptance Criteria

- [x] Migration shipped — `idempotency_keys` table (V41 kitehub-subscription + V66 kiteclass-core ENROLLMENT scope already shipped earlier waves; this gap reuses existing schema)
- [x] `IdempotencyService` (kitehub-subscription) handles multiple endpoint ids; ENROLLMENT scope handled by kiteclass-core `IdempotencyService` (Wave beta-readiness-2 Bucket A)
- [x] 3 controllers (signup + enrollment + beta-request) wrap với idempotency logic — signup + beta-request via `IdempotencyHandlerInterceptor` URI-to-endpoint map (this PR); enrollment inline via `EnrollmentController` (Wave beta-readiness-2 Bucket A precedent)
- [x] 3 IT tests verify duplicate Idempotency-Key → no duplicate DB row — 4 new IT (SignupIdempotencyIT 2 + BetaRequestIdempotencyIT 2) + 3 regression (IdempotencyInterceptorIT GAP-536) = 7 PASS total
- [x] `./mvnw compile -P strict-warnings` PASS kitehub-subscription
- [ ] Per-tenant rate-limit on Idempotency-Key abuse — DEFERRED Wave beta-readiness-3+ per gap §Out-of-scope (filed as follow-up)

### Out-of-scope

- Production live verify deduplication — gated GAP-612 AWS restore (follow-up post-restore)
- Per-tenant idempotency rate-limit (DDoS mitigation) — defer Wave beta-readiness-3+
- Migration backfill cho existing payment idempotency rows (if shared schema) — separate gap if needed

## Priority Rationale (P0)

Phase 1 BETA gate — POST mutation safety mandatory. Per `pre-handoff-self-test-completeness.md` §2.6 Payment flow gap (d) "Idempotency key honored — same key replayed → no double-charge; row in payment_attempts table" — proven critical pattern.

## Related

- Wave beta-readiness-1 Bucket C scope (defer to beta-readiness-2 Bucket — agent blocked)
- Wave 105 Bucket D `PaymentIdempotencyService.java` — pattern source
- V2 audit `failure-mode-matrix-v2-state-checked.md` A3
- `pre-handoff-self-test-completeness.md` §2.6 Payment flow

## Log

- **2026-05-24 (Bucket C defer):** Agent execution blocked do content filter policy mid-implementation (1 hour compute lost). User chose option 1 (file follow-up gap + defer fix sang Wave beta-readiness-2) per AskUserQuestion 2026-05-24. Re-spawn agent next session với narrow scope.

- **2026-06-02 (Wave local-doable-10 Bucket A — DONE):** Implemented Idempotency-Key support trên 2 endpoint còn lại (signup + beta-request) via extending existing `IdempotencyHandlerInterceptor` (kitehub-subscription) thay vì spawn shared library — pragmatic vì 2 controllers cùng module với existing interceptor. Approach summary:
  - **Interceptor extension** (`IdempotencyHandlerInterceptor.java`) — added URI→endpoint map (3 entries: `/api/platform/instances` GAP-536 + `/api/auth/register` SIGNUP + `/api/v1/auth/request-beta-access` BETA_REQUEST). `resolveEndpoint(uri)` dispatch.
  - **Filter extension** (`IdempotencyCachingFilter.java`) — extend `shouldNotFilter` từ 1 path sang Set 3 paths.
  - **WebMvcConfig** — extend `addPathPatterns` từ 1 path sang 3 paths.
  - **2 new IT** — `SignupIdempotencyIT` (2 tests: same-key-same-body replay + no-header backward-compat) + `BetaRequestIdempotencyIT` (2 tests: same pattern). All run trên Testcontainers Postgres 16 per `postgres-specific-type-testcontainers.md`.
  - **ENROLLMENT scope** — Wave beta-readiness-2 Bucket A already shipped via inline pattern trên `EnrollmentController` (kiteclass-core); CSV `IdempotencyScope.ENROLLMENT` enum value confirmed. State-check verified per `audit-to-gap-pipeline.md` §2.8.
  - **Cross-flow sweep** (per `cross-flow-bug-class-sweep.md` §3): sweep POST controllers trong kitehub-subscription, 26 POST endpoints found ngoài 3 trong gap scope. Verdict EXEMPT cho phần lớn (auth refresh/login = no duplicate side-effect; admin force-convert/rollback-migration = single tenant action protected by admin auth; payment + subscription đã có separate idempotency layer). Narrow scope gap = correctly bounded.
- **Test results:** Tests run 7 total (4 new + 3 regression). Failures 0. Errors 0. Skipped 0.
