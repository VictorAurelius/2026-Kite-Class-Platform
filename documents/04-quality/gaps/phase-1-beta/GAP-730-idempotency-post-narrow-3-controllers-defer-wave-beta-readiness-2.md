# GAP-730: Idempotency POST narrow (signup + enrollment + beta-request) — defer Wave beta-readiness-2 (agent blocked)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (kiteclass-core + kitehub-platform)
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

- [ ] Migration `V*__shared_idempotency_keys.sql` shipped
- [ ] `IdempotencyService.java` (or extended `PaymentIdempotencyService`) handles 4 scopes
- [ ] 3 controllers (signup + enrollment + beta-request) wrap với idempotency logic
- [ ] 3 IT tests verify duplicate Idempotency-Key → no duplicate DB row
- [ ] `mvnw verify -P strict-warnings` PASS
- [ ] Per-tenant rate-limit on Idempotency-Key abuse — defer Wave beta-readiness-3+ (follow-up gap)

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
