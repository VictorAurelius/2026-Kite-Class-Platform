---
name: GAP-146 — External HTTP timeouts remainder (payment/email/captcha)
description: 3 HTTP client sites deferred from GAP-131 fix (PR #375) — need timeouts + Resilience4j + WireMock integration tests
type: gap
---

# GAP-146: External HTTP Timeouts Remainder

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-04-20 (deferred scope from GAP-131 fix PR #375)
**Affects:** payment client, email client, captcha client

## Problem

GAP-131 fix (PR #375) closed 6 of 9 HTTP client sites (added connect+read timeouts). 3 sites deferred:

1. Payment gateway client (kitehub-subscription payment integration)
2. Email SMTP/SendGrid client (if exists beyond BrandingClient)
3. Captcha verification client (register/login anti-bot)

These were deferred because they may need:
- Resilience4j wrappers (not just timeouts) for payment — payment retries need idempotency keys
- WireMock integration tests (more complex than unit timeout assertion)
- Coordinate with feature flags (e.g., captcha toggle for dev/prod)

## Root Cause

Scope narrowed in PR #375 to keep PR focused on 6 clearly-bounded sites. Remaining 3 need design decisions.

## Proposed Fix

1. Inventory 3 remaining HTTP clients
2. For payment: add Resilience4j retry + circuit breaker + idempotency key pattern
3. For email: add timeout + retry with dead-letter queue
4. For captcha: add timeout + fail-open fallback (don't block login if captcha service down)
5. WireMock integration tests for each
6. ArchUnit lint rule preventing new `RestTemplate` / `WebClient` without timeout config

## Acceptance Criteria

- [ ] All 3 remaining HTTP clients have connect+read timeouts
- [ ] Payment client has Resilience4j wrapper + idempotency
- [ ] Email client has retry + DLQ
- [ ] Captcha client has fail-open fallback
- [ ] WireMock integration tests cover timeout + error scenarios
- [ ] ArchUnit rule added + CI enforced

## Related

- Parent: GAP-131 (closed PR #375 for 6 sites)
- Related: GAP-144 (Alertmanager — payment failure alerting)
