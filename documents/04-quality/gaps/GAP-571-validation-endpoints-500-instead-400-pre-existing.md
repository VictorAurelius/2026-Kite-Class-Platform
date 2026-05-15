---
title: "GAP-571: 2 validation endpoints return 500 instead of 400/401 on invalid input"
status: OPEN
priority: P1
domain: Backend
phase: phase-1-beta
wave: 82-post-deploy-smoke-finding
created: 2026-05-15
---

# GAP-571: Validation endpoints return 500 instead of 400 on invalid input

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (visible to anonymous users; UX confusion)
**Domain:** Backend / Controller exception handling
**Found:** 2026-05-15 (Wave 82 post-deploy smoke `v0.9.0-beta-staging.16`)
**Affects:** 2 endpoints public-anonymous facing — BetaAccessController + AuthController

## Problem

Wave 82 post-deploy smoke 14-endpoint sweep 2026-05-15 11:00 UTC reveals 2 endpoints return HTTP 500 (Internal Server Error) thay vì 400/401 cho invalid input:

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  "https://api.kitehub.me/api/v1/auth/beta-signup/validate?token=invalid"
# Actual: 500 | Expected: 400 (invalid token format) or 401 (token unauthorized)

curl -s -o /dev/null -X POST \
  -H "Content-Type: application/json" -d '{}' \
  "https://api.kitehub.me/api/auth/verify-email"
# Actual: 500 | Expected: 400 (missing required `token` field, validation fail)
```

**Pre-existing — không phải Wave 82 regression.** Wave 82 Bucket F changes touched gateway routing + Spring web config; these 2 endpoints' controller logic không thay đổi. Smoke surface lần đầu vì pre-Wave-82 không có comprehensive endpoint sweep.

## Root Cause (hypothesis — cần investigate)

1. **BetaAccessController.validate(token):** likely throws `IllegalArgumentException` hoặc `NullPointerException` cho invalid token format (e.g., not base64, malformed JWT structure) → no `@ControllerAdvice` for these exception types → Spring default returns 500.

2. **AuthController.verifyEmail(body):** likely missing `@Valid` annotation on request body OR DTO missing `@NotBlank` constraint → null `token` field → service throws NPE → 500.

Both = controller-side exception handling gap. Standard fix: add `@RestControllerAdvice` with `@ExceptionHandler` cho `IllegalArgumentException` + `MethodArgumentNotValidException` returning `ProblemDetail` 400.

## Proposed Fix

1. **Add `@RestControllerAdvice` GlobalExceptionHandler** trong kitehub-subscription (or kitehub-shared common module nếu existed):
   ```java
   @RestControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler({IllegalArgumentException.class, ValidationException.class})
       public ResponseEntity<ProblemDetail> handle400(Exception ex) {
           var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
           return ResponseEntity.status(400).body(pd);
       }
       // ... similar for MethodArgumentNotValidException, ConstraintViolationException
   }
   ```

2. **Add `@Valid` + `@NotBlank` annotations** cho relevant DTOs:
   - `VerifyEmailRequest.token` → `@NotBlank`
   - `BetaSignupValidateRequest.token` (query param) → `@NotBlank` on `@RequestParam`

3. **Add regression tests:**
   - `AuthControllerTest.verifyEmail_emptyBody_returns400`
   - `BetaAccessControllerTest.validate_invalidToken_returns400`

## Acceptance Criteria

- [ ] `curl GET /api/v1/auth/beta-signup/validate?token=invalid` returns 400 với `application/problem+json` body
- [ ] `curl POST /api/auth/verify-email -d '{}'` returns 400 với field-level error JSON
- [ ] `curl POST /api/auth/verify-email -d '{"token":""}'` returns 400 với validation error message
- [ ] Regression tests added — mvn verify PASS
- [ ] No regression on valid-token success path (200 still works)

## Related

- Wave 82 smoke source: post-deploy 2026-05-15 11:00 UTC curl sweep (in conversation)
- Sister Wave 82 post-deploy finding: GAP-570 (F5 Spring 500→404 incomplete — same class: 500 default error pages)
- Rule: `pre-launch-owasp-rest-hardening-checklist.md` §2.5 (A05 Security Misconfiguration — production stacktrace hidden) — partial mitigation since fix surfaces 500 cleanly but doesn't fix the root cause
- Rule: `pre-handoff-self-test-completeness.md` §2 — endpoint sweep would have caught this before Wave 78/79/80 closure

## Log

- **2026-05-15:** Gap filed post-Wave-82 deploy smoke. 14-endpoint sweep against `v0.9.0-beta-staging.16` LIVE production found 2 endpoints returning 500 on invalid input. Pre-existing bug surfaced by smoke — không phải Wave 82 regression. P1 visible to anonymous users (no auth required). Non-blocking Bucket H dev walk-through (valid-input flows work; only edge cases return 500). Investigate + fix next BE wave.
