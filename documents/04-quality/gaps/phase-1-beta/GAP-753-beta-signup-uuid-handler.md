---
status: OPEN
priority: P1
phase: phase-1-beta
domain: Backend
found: 2026-05-26
wave-source: rst-cascade-1-cluster-3-phase-beta
related: [GAP-610]
---

# beta-signup validate invalid UUID format → HTTP 500 instead of 400 (GAP-610 cascade)

## Problem

`GET /api/v1/auth/beta-signup/validate?token=<invalid-uuid-format>` returns HTTP 500 instead of HTTP 400 Bad Request. Confirmed Wave rst-cascade-1 Phase β AWS production smoke 2026-05-26:

```bash
curl https://api.kitehub.me/api/v1/auth/beta-signup/validate?token=invalid-uuid
# → HTTP 500
```

For valid UUID format (token doesn't exist), endpoint correctly returns HTTP 404. The 500 only fires when UUID parser itself fails.

## Root Cause

Wave beta-readiness-5 Bucket C fix (PR #1828) for GAP-610 only handled VALID UUID format inputs. Spring `MethodArgumentTypeMismatchException` for malformed UUID propagates as 500 because no `@ExceptionHandler` defined.

Controller method signature uses `@RequestParam UUID token` — Spring binding fails before controller logic executes.

## Proposed Fix

Option A — `@ExceptionHandler` cho `MethodArgumentTypeMismatchException` in `BetaAccessController` hoặc `@ControllerAdvice`:

```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<BetaSignupErrorResponse> handleInvalidUuid(MethodArgumentTypeMismatchException ex) {
    if ("token".equals(ex.getName()) && UUID.class.equals(ex.getRequiredType())) {
        return ResponseEntity.badRequest()
            .body(new BetaSignupErrorResponse("INVALID_TOKEN_FORMAT", "Token format invalid; must be UUID v4"));
    }
    return ResponseEntity.badRequest().body(new BetaSignupErrorResponse("INVALID_REQUEST", ex.getMessage()));
}
```

Option B — Change param type to `String` + manual `UUID.fromString` với try-catch + return 400 BadRequest.

Option A preferred (less invasive, centralized error handling).

## Acceptance Criteria (paired with E2E spec per `e2e-rst-test-layer-boundary.md` §3)

- [ ] `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` in `BetaAccessController` OR `@ControllerAdvice` returns HTTP 400 + JSON body `{errorCode: "INVALID_TOKEN_FORMAT", ...}`
- [ ] **NEW E2E spec** `kitehub-frontend/e2e/auth-beta-signup-invalid-uuid.spec.ts` covers test matrix:
  - Valid UUID + token exists → 200 + valid response
  - Valid UUID + token not found → 404
  - Invalid UUID format → **400** (not 500)
  - Missing token param → 400
  - Expired token (UUID exists in DB but past TTL) → 410 Gone
- [ ] `BetaAccessControllerTest` unit test cases per matrix
- [ ] Production smoke verify Phase β post-fix: `curl <invalid-uuid>` → 400 (not 500)

## Related

- GAP-610 Wave br-5 Bucket C original fix (PR #1828) — only valid UUID covered
- Wave rst-cascade-1 Phase α Cluster 3 (Onboarding agent) flagged + Phase β AWS smoke confirmed cascade
- `kitehub-platform/.../BetaAccessController.java` (controller site)
- `documents/04-quality/audits/quality/2026-05-26-wave-rst-cascade-1-closure.md` §Cascade findings #2
- Rule `.claude/rules/e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate

## Effort estimate

~30 min coding (Option A) + ~45 min E2E spec + IT test. Bundle với Wave rst-cascade-2 (~1 wave bucket).

## Log

- **2026-05-26 (OPEN):** Filed per Wave rst-cascade-1 closure audit §3 cascade finding #2 + Phase β production confirmed. Production smoke `curl invalid-uuid → 500`. Per `audit-to-gap-pipeline.md` §3 + `e2e-rst-test-layer-boundary.md` §3 RST→E2E promotion mandate. Wave rst-cascade-2 candidate.
