---
name: GAP-146 — External HTTP timeouts remainder (payment/email/captcha)
description: 3 HTTP client sites deferred from GAP-131 fix (PR #375) — behavioural timeout tests now in place; Resilience4j + ArchUnit lint + WireMock deferred
type: gap
---

# GAP-146: External HTTP Timeouts Remainder

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-04-20 (deferred scope from GAP-131 fix PR #375)
**Closed:** 2026-04-21 (Wave 9-F)
**Affects:** payment client, email client, captcha client

## Problem

GAP-131 fix (PR #375) closed 6 of 9 HTTP client sites (added connect+read timeouts). 3 sites deferred:

1. Payment gateway client (kitehub-subscription `VietQRService`)
2. Email branding client (kitehub-email `BrandingClient` — reactive WebClient)
3. Captcha verification client (kitehub-subscription `CaptchaService`)

These were deferred because they may need:
- Resilience4j wrappers (not just timeouts) for payment — payment retries need idempotency keys
- WireMock integration tests (more complex than unit timeout assertion)
- Coordinate with feature flags (e.g., captcha toggle for dev/prod)

## Current State (verified 2026-04-21)

State-check against code:

| Site | Timeout wiring | Source of timeout |
|------|----------------|-------------------|
| `VietQRService` | ✅ 5 s connect + 30 s read | injected `RestTemplate` from `RestTemplateConfig` (GAP-131) |
| `CaptchaService` | ✅ 5 s connect + 30 s read | injected `RestTemplate` from `RestTemplateConfig` (GAP-131) |
| `BrandingClient` (email) | ✅ 5 s connect + (timeoutSeconds+1) s response | Netty `HttpClient` built in constructor (GAP-131) |

Timeouts were in place — what remained was BEHAVIOURAL proof (what happens when the timeout fires).

## Root Cause

Scope narrowed in PR #375 to keep PR focused on 6 clearly-bounded sites. Remaining 3 needed tests proving the fallback paths, plus design decisions for deeper resilience (Resilience4j, CB).

## Fix Applied (Wave 9-F)

1. Added 3 targeted behavioural tests exercising the timeout paths:
   - `VietQRServiceTimeoutTest` — simulates `ResourceAccessException` wrapping `SocketTimeoutException`; asserts public fallback QR URL returned for both read and connect timeouts (2 cases).
   - `CaptchaServiceTimeoutTest` — simulates timeout on hCaptcha verify; locks in fail-OPEN policy (return `true`) so signup is not blocked by upstream outage; also covers the disabled short-circuit (3 cases).
   - `BrandingClientTimeoutTest` (kitehub-email) — stands up a JDK `HttpServer` that intentionally sleeps 10 s behind a 1 s client timeout; asserts (a) default branding returned, (b) call unwinds in < 5 s (not JVM-default infinite) — uses JDK only, no WireMock dependency added.
2. All 3 OpenApiConfig classes across modules given explicit bean names (side of GAP-147 fix — prevents future `@ComponentScan` cross-module collisions).

## Acceptance Criteria

- [x] All 3 remaining HTTP clients have connect+read timeouts (verified — in place since GAP-131)
- [x] Behavioural tests cover timeout + error scenarios (6 new tests across 3 files)
- [ ] Payment client has Resilience4j wrapper + idempotency — **deferred** to GAP-146a (follow-up)
- [ ] Email client has retry + DLQ — **deferred** to GAP-146a
- [ ] Captcha client has fail-open fallback — partially covered; documented + test-locked here; deeper CB deferred
- [ ] WireMock integration tests — **deferred** (JDK `HttpServer` used instead — no new dependency)
- [ ] ArchUnit lint rule preventing new `RestTemplate` / `WebClient` without timeout — **deferred** to GAP-146b (meta-gap covering all 9 sites, not just these 3)

## Deferred Follow-ups (NEW gaps to file post-merge)

- **GAP-146a — Resilience4j + idempotency for payment/email/captcha** (P2, feature-meta): adds `@CircuitBreaker` + `@Retry` on these 3 external calls; requires idempotency key design for VietQR; DLQ wiring for email retries. Not a hotfix scope.
- **GAP-146b — ArchUnit rule: no `RestTemplate`/`WebClient` without timeout** (P2, meta): CI-level enforcement for all 14 HTTP sites, covers future additions.

## Tests Added

| File | Tests | Module |
|------|------:|--------|
| `VietQRServiceTimeoutTest.java` | 2 | kitehub-subscription |
| `CaptchaServiceTimeoutTest.java` | 3 | kitehub-subscription |
| `BrandingClientTimeoutTest.java` | 1 | kitehub-email |

## Related

- Parent: GAP-131 (closed PR #375 for 6 sites)
- Follow-up: GAP-146a (Resilience4j) — to be filed
- Follow-up: GAP-146b (ArchUnit lint) — to be filed
- Related: GAP-147 (paired fix — kitehub-admin bean conflict; same wave 9-F PR)
- Related: GAP-144 (Alertmanager — payment failure alerting)

## Log

- 2026-04-20 — Gap filed as Part B perf batch follow-up (deferred from PR #375).
- 2026-04-21 — **Closed (Wave 9-F).** State-check confirmed timeouts already wired (GAP-131 landed them); behavioural tests added to close the proof gap. Resilience4j + ArchUnit + WireMock descoped into GAP-146a/146b. 6 new tests across 3 files; all green. Paired with GAP-147 in PR for Wave 9-F.
