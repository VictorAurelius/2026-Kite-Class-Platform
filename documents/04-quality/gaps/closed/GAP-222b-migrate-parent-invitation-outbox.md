## GAP-222b: Migrate ParentInvitationServiceImpl to OutboxEventWriter

**Status:** 🟢 DONE — Exception A migration shipped 2026-04-26 (outbox-first + best-effort fast-path with marker comment); existing happyPath test extended to verify outbox call; 1117/1117 kiteclass-core tests green
**Priority:** 🟠 P1 (reliability — single bypass in tenant-critical onboarding path)
**Domain:** Backend (kiteclass-core)
**Found:** 2026-04-26 (Sub-PR 6.4 scope check)
**Parent gap:** GAP-222
**Effort:** S-M (1-2h)
**NOT blocked by GAP-222a** — kiteclass-core can use its own outbox infra in-place; no shared-lib dependency required.

## Current State (verified 2026-04-26)

| Asset | Status |
|-------|--------|
| Bypass site | `kiteclass/kiteclass-core/.../module/parent/service/impl/ParentInvitationServiceImpl.java` line 284 — single `rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, EMAIL_ROUTING_KEY, event)` |
| Existing test | `kiteclass-core/.../parent/service/ParentInvitationServiceTest.java` — TDD safety net present |
| Outbox infra | ✅ Available in same module (`kiteclass-core/common/outbox/OutboxEventWriter`) |
| Documented exception (per `design-patterns.md` §3.5.1)? | ❌ No — no fast-path comment, no marker → silent bypass |

## Problem

`ParentInvitationServiceImpl` publishes parent-invitation email events directly to RabbitMQ. If broker is down at the moment a parent registers, the DB row commits but the email event is lost — parent never receives the invitation. This is exactly the failure mode `design-patterns.md` §3.5 was written to prevent.

Onboarding is tenant-critical (Wave 8a `tenant-onboarding-runbook` references parent-invite as Day-1 path).

## Proposed Fix

1. Inject `OutboxEventWriter outbox` via constructor (same pattern as `BrandingEventPublisher`)
2. Replace `rabbitTemplate.convertAndSend(EMAIL_EXCHANGE, EMAIL_ROUTING_KEY, event)` with `outbox.enqueue(EMAIL_ROUTING_KEY, "ParentInvitation", invitationId.toString(), payload)` inside the existing `@Transactional` method
3. Drop the `RabbitTemplate` field if no other use remains in the file
4. Add 1 test asserting outbox row written on commit (use existing test class as base)
5. Verify async outbox publisher drains the row → message reaches consumer (existing integration-test pattern)

## Acceptance Criteria

- [x] `rabbitTemplate.convertAndSend` retained as best-effort fast-path under §3.5.1 Exception A (with marker comment "outbox is the reliability net" + log-and-swallow on broker error) — original gap proposed pure-outbox replacement, but Exception A pattern matches the established `BrandingEventPublisher` precedent in the same module and preserves immediate-delivery latency when broker is up
- [x] Existing happyPath test extended to assert `outbox.enqueue` called with `EMAIL_ROUTING_KEY` ("email.send"), aggregate type "ParentInvitation", and invitation id
- [x] All existing tests in `ParentInvitationServiceTest` stay green (13/13)
- [x] Full `kiteclass-core` test suite green (1117/1117, 52 skipped pre-existing)
- [ ] `design-pattern-audit` Cat 5 next run drops by 1 site for this file (deferred — happens at next audit, not part of this gap)
- [x] Manual verification description in PR (broker-down behavior preserved by existing try/catch around fast-path; outbox row guarantees eventual delivery)

## Dependencies

- None — outbox infra already in same module
- Re-uses existing `EMAIL_ROUTING_KEY` constant (no consumer change)

## Risk / Tradeoffs

- **Latency**: +1 DB row + async dispatch latency vs direct publish. Acceptable for parent-invitation (not sub-second SLA).
- **Consumer impact**: `outbox-publisher` already publishes to the same `EMAIL_EXCHANGE` → email-consumer service unchanged.

## Related

- Parent: GAP-222
- Sibling: GAP-222a (shared-lib extraction, not required for this gap), GAP-222c (kitehub migration, blocked by 222a)
- Rule: `.claude/rules/design-patterns.md` §3.5 + §3.5.1 (silent bypass = anti-pattern)
- Reference impl: `BrandingEventPublisher` — same outbox pattern in same module
- Audit: `documents/04-quality/audits/design-patterns/audit-2026-04-26.md` Cat 5

## Log

- **2026-04-26 (later — SHIPPED):** Status 🔵 OPEN → 🟢 DONE. Migration applied as Exception A pattern (matches BrandingEventPublisher precedent in same module): outbox.enqueue first inside existing @Transactional block, then existing fast-path try/catch with new marker comment "outbox is the reliability net". Constructor expanded with OutboxEventWriter + ObjectMapper params; test ObjectMapper uses findAndRegisterModules() to match Spring Boot's default JavaTimeModule registration (initial omission caused Instant serialization failure in test). Original gap proposed pure-outbox replacement; revised to Exception A for module consistency + preserved low-latency happy path. happyPath test extended to verify outbox call with aggregate metadata. Full kiteclass-core suite 1117/1117 green.
- 2026-04-26 — Gap created during Sub-PR 6.4 scope check. State-check confirmed: 1 bypass site, test file exists, outbox infra in same module → low-risk migration. Independent of 222a.
