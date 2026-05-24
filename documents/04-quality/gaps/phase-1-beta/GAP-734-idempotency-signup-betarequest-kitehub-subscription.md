# GAP-734: Signup + BetaRequest controller idempotency wrap — kitehub-subscription scope (Wave beta-readiness-2 Bucket A scope reconciliation)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-subscription idempotency POST narrow)
**Detected:** 2026-05-24 (Wave beta-readiness-2 Bucket A Agent A state-check finding — Signup + BetaRequest controllers live trong kitehub-subscription, NOT kiteclass-core as plan assumed)
**Affects:** `POST /api/auth/register` (signup) + `POST /api/v1/auth/request-beta-access` (beta request) endpoints — double-submit safety

## Problem

Wave beta-readiness-2 Bucket A (GAP-730) intended to ship idempotency POST narrow cho 3 controllers — signup + enrollment + beta-request. Agent A Opus state-check phase discovered:

- **EnrollmentController** → `kiteclass-core` ✅ wrapped trong Bucket A PR #1769
- **AuthController** (signup `POST /api/auth/register`) → **`kitehub-subscription`** module (different codebase)
- **BetaAccessController** (beta-request `POST /api/v1/auth/request-beta-access`) → **`kitehub-subscription`** module

PR #1769 ship shared infrastructure (`common/idempotency/` package + V66 migration + IdempotencyService + IdempotencyScope enum reserving SIGNUP + BETA_REQUEST values) trong kiteclass-core. Apply pattern sang kitehub-subscription controllers cần follow-up wave.

## Root Cause

Wave plan assumed all 3 controllers cùng module (kiteclass-core). Reality: KiteHub authentication + beta access flows live trong kitehub-subscription (per Wave 35-ish architecture decision separating customer-facing auth from tenant-internal logic). Agent A state-check at impl time caught the misassumption.

## Proposed Fix (Wave beta-readiness-3+ candidate)

### Step 1: Port idempotency infrastructure to kitehub-subscription

Reuse pattern from kiteclass-core PR #1769:
- Either:
  - **Option A:** Move `common/idempotency/` package vào kitehub-platform shared lib JAR → both kiteclass-core + kitehub-subscription import (cleaner long-term)
  - **Option B:** Duplicate package vào kitehub-subscription (faster, ~10 LOC duplication)
- Reuse `idempotency_keys` table — kitehub-subscription connects to same DB instance; query `WHERE scope IN ('SIGNUP', 'BETA_REQUEST')`

### Step 2: Wrap AuthController.register

```java
@PostMapping("/api/auth/register")
public ResponseEntity<RegisterResponse> register(
        @Valid @RequestBody RegisterRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
) {
    UUID tenantId = TenantContext.getCurrentTenant();
    if (idempotencyKey != null) {
        Optional<IdempotencyService.CachedResponse> cached =
            idempotencyService.findExisting(tenantId, idempotencyKey, IdempotencyScope.SIGNUP);
        if (cached.isPresent()) {
            return ResponseEntity.status(cached.get().status())
                    .header("X-Idempotent-Replay", "true")
                    .body(/* deserialize cached.body() */);
        }
    }
    // ... existing registration logic ...
    if (idempotencyKey != null) {
        idempotencyService.recordRequest(tenantId, idempotencyKey, IdempotencyScope.SIGNUP,
                userId, hashRequest(request), 201, responseJson);
    }
    return ResponseEntity.status(201).body(response);
}
```

### Step 3: Wrap BetaAccessController.requestBetaAccess (same pattern as Step 2)

### Step 4: IT tests

- `kitehub-subscription/src/test/.../auth/SignupIdempotencyIT.java` — submit POST same Idempotency-Key 2x → cached response, NOT duplicate `User` row
- `kitehub-subscription/src/test/.../beta/BetaAccessIdempotencyIT.java` — same pattern cho beta-request

## Acceptance Criteria

- [ ] Idempotency infrastructure available trong kitehub-subscription (Option A shared lib OR Option B duplicate)
- [ ] AuthController.register wraps với inline idempotency logic
- [ ] BetaAccessController.requestBetaAccess wraps với inline idempotency logic
- [ ] 2 IT tests verify duplicate Idempotency-Key → cached response, no duplicate DB row
- [ ] `mvn verify -P strict-warnings` PASS

## Out-of-scope

- Per-tenant rate-limit on Idempotency-Key abuse — tracked Wave beta-readiness-3+ per GAP-730 §Out-of-scope
- Production live verify (gated GAP-612 AWS restore)

## Priority Rationale (P1)

Phase 1 BETA gate — POST mutation safety mandatory cho signup + beta-request flows. EnrollmentController already wrapped (PR #1769); these 2 sister flows complete the trio. P1 vs P0 because production traffic là zero pre-AWS-restore — không gây actual double-submit damage hiện tại.

## Related

- Wave beta-readiness-2 Bucket A PR #1769 — parent (shared infrastructure shipped)
- GAP-730 — parent gap (Bucket A scope)
- GAP-732 + GAP-733 — sibling Wave beta-readiness-2 follow-ups
- `PaymentIdempotencyService.java` (Wave 105 Bucket D) — pattern precedent

## Log

- **2026-05-24 (filed):** Wave beta-readiness-2 Bucket A Agent A scope reconciliation — state-check at impl time discovered Signup + BetaRequest controllers live trong kitehub-subscription (not kiteclass-core). Shared infrastructure shipped Bucket A; 2 controllers wrap defer follow-up.
