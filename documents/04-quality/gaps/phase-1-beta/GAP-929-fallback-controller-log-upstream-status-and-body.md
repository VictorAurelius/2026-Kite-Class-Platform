# GAP-929: FallbackController logs upstream status + 200-char body fingerprint when fallback fires

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-04 (Wave flow-kh1, filed as Phase 3 follow-up to GAP-928 — Phase 1 + Phase 2 closed gateway false 503 by tuning + carving the auth circuit breaker; Phase 3 closes the observability gap that made the original incident expensive to diagnose)
**Affects:**
- `kitehub/kitehub-gateway/src/main/java/.../controller/FallbackController.java`
- Indirect: every operator who needs to tell a *real upstream failure* (503/500/timeout from kitehub-subscription) apart from a *slow but successful* upstream call (circuit-breaker fallback when the upstream actually committed)

## Problem

When the gateway circuit-breaker fires and routes a request to `FallbackController.fallbackAuth`, the only signal currently surfaced is `WARN c.k.g.controller.FallbackController - Circuit breaker triggered for auth service`. That message is identical whether:

- the upstream returned a real `5xx` (real failure — fallback is correct),
- the upstream timed out (real failure — fallback is correct),
- the upstream completed successfully but slower than the breaker's `slowCallDurationThreshold` (false fallback — upstream committed, client sees 503, retry will fail because state is half-applied — the exact pattern that produced GAP-928, GAP-927).

Without per-fallback structured logs, distinguishing "real failure" from "false 503" requires DB forensics (cross-check `users` / `instances` tables for the relevant tenant against the request timestamp). That is too expensive to do at scale during a real incident.

## Proposed Fix

Extend `FallbackController.fallbackAuth` (and sibling fallbacks for instances/subscription/payment/etc. if cheap) to log:

- Upstream HTTP status (from the inner exception if accessible — `WebClientResponseException.getStatusCode()`, or the `null` marker when the call timed out before a response)
- Request method + path
- A 200-char fingerprint of the upstream response body when one exists (truncated, PII-scrubbed per `logs-format-standard.md` §3.1 — strip `token` / `email` / `password` / `secret` fields)
- Correlation ID / trace ID propagated from the inbound `X-Request-Id` or Sleuth/OTel context
- Circuit-breaker name (`authCircuitBreaker` / `authWriteCircuitBreaker` / etc.) and the breaker state at fallback time (`OPEN` vs `HALF_OPEN`)

## Acceptance Criteria

- [ ] `FallbackController.fallbackAuth` emits a single structured log line per fallback invocation containing: status, method, path, body fingerprint (≤200 chars, PII-scrubbed), correlation ID, breaker name, breaker state
- [ ] Body fingerprint follows `logs-format-standard.md` §3.1 — no raw token / email / password / API-key values in output
- [ ] Smoke test: stop kitehub-subscription, POST `/api/v1/auth/beta-signup` from the gateway, verify the fallback log contains `breaker=authWriteCircuitBreaker state=OPEN status=null` (real failure shape) and is distinguishable from a slow-but-successful call shape

## Related

- Parent: [GAP-928](closed/GAP-928-gateway-false-503-on-successful-upstream.md) Phase 3 (Phase 1 + Phase 2 closed; this gap captures the deferred observability work)
- Sister: GAP-925 (FE generic catch — pair this with structured server-side log to triage faster on user-reported 503), GAP-927 (BE rollback rotates token — combined with this log line, operators can confirm "false 503" without DB forensics)
- Per `logs-format-standard.md` §3.1 — PII-safe shape for structured logs (no raw token / email)
- Per `design-patterns.md` §3.6 — circuit breaker observability is part of operational config; not "set once and forget"
