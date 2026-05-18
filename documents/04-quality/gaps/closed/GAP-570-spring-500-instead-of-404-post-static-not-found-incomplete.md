---
title: "GAP-570: F5 fix incomplete — POST non-existent path still returns 500 post-deploy"
status: OPEN
priority: P2
domain: Backend
phase: phase-1-beta
wave: 82-post-deploy-followup
created: 2026-05-15
---

# GAP-570: F5 Spring 500→404 fix incomplete — production verify regression

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (framework noise; non-blocking)
**Domain:** Backend
**Found:** 2026-05-15 (Wave 82 deploy v0.9.0-beta-staging.16 post-smoke)
**Affects:** kitehub-subscription + 3 sister WebMVC services (admin/branding) — observable on any `POST` to undefined path

## Problem

Wave 82 Bucket F5 shipped `spring.web.resources.add-mappings: false` cho 4 WebMVC services (kitehub-admin/branding/subscription + kiteclass-core) trong PR #1396. Wave 81 Bucket G spot check Finding #4 motivated the fix: "Spring Boot trả 500 thay vì 404 cho POST static-not-found path".

Local CI mvn verify PASS cho cả 4 services. PR #1396 merged + deployed `v0.9.0-beta-staging.16` thành công 2026-05-15 10:43 UTC.

**Production smoke 2026-05-15 10:50 UTC reveals:**

```bash
curl -X POST https://api.kitehub.me/api/v1/auth/nonexistent-endpoint-test
# Expected: HTTP 404
# Actual:   HTTP 500
```

Fix didn't activate as designed. Other Wave 82 fixes (F4 gateway routing, F6 production profile, OTel CVE) ALL verify LIVE successfully.

## Root Cause

Hypothesis (cần investigate):

1. **Spring Cloud Gateway error wrapping** — gateway intercepts downstream 404 + converts to 500 via CircuitBreaker fallback OR its own error filter. Test: curl directly to kitehub-subscription port (bypassing gateway) → expect 404 if BE config correct.

2. **`add-mappings: false` không đủ alone trong Spring Boot 3.5.14** — may need pairing với `spring.mvc.throw-exception-if-no-handler-found: true` + `@RestControllerAdvice` cho `NoResourceFoundException` → ResponseEntity 404.

3. **Production yaml override** — `application-production.yml` (added Wave 82 Bucket F6) không inherit/override `web.resources.add-mappings`. Spring profile interaction.

## Proposed Fix

1. Verify root cause via direct curl to BE port (bypassing gateway):
   ```bash
   # Via SSM SendCommand on kh-backend EC2:
   curl -v -X POST http://localhost:8080/api/v1/auth/nonexistent
   # If 404 → gateway is the problem; investigate Spring Cloud Gateway error filter
   # If 500 → BE config still wrong; need controller advice
   ```

2. If gateway-side: add custom error filter trong `kitehub-gateway` returning RFC 7807 ProblemDetails JSON với correct status (preserves downstream 404).

3. If BE-side: add `@RestControllerAdvice` exception handler:
   ```java
   @ExceptionHandler(NoResourceFoundException.class)
   public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
       return ResponseEntity.status(404).body(...);
   }
   ```

## Acceptance Criteria

- [ ] `curl -X POST https://api.kitehub.me/api/v1/auth/nonexistent` returns **HTTP 404** (not 500)
- [ ] Direct BE port curl (via SSM) returns 404 confirming source of error
- [ ] Add controller advice OR gateway filter as needed
- [ ] Regression test in `BetaAccessControllerTest` covering POST non-existent path
- [ ] Update Wave 81 spot check audit Finding #4 status

## Related

- Wave 81 Bucket G spot check Finding #4 origin: `documents/04-quality/audits/pre-self-test/2026-05-15-wave-81-spot-check.md`
- Wave 82 Bucket F5 incomplete fix: PR #1396
- Sister gaps Wave 82: GAP-565..568 (Bucket B prerequisites)
- Rule: `pre-handoff-self-test-completeness.md` §2 — endpoint-level verify post-deploy required (F5 closed without this verify; lesson learned)

## Log

- **2026-05-15:** Gap filed post-deploy verify. Wave 82 Bucket F5 claimed DONE via PR #1396 (local CI PASS). Production smoke 2026-05-15 10:50 UTC reveals POST non-existent path still returns 500. Non-blocking framework noise; doesn't impact Bucket H dev walk-through (dev tests product flows, not random POST). Investigate later — root cause likely gateway error wrapping vs BE controller advice missing.
