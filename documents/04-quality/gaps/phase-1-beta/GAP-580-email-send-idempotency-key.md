# GAP-580: Email send idempotency key (consumer-side dedup, `email.send` pipeline)

**Status:** 🟡 PARTIAL (common-case consumer dedup shipped + MailHog-verified; cross-restart + sister paths → GAP-840)
**Priority:** 🟠 P1 (trust-damage prevention cho beta cohort — duplicate emails = poor UX)
**Domain:** Backend / DevOps
**Found:** 2026-05-15 (Wave 85 Bucket A simulation 3-axis cell 22)
**Affects:** kitehub-email service email send pipeline + RabbitMQ message redelivery semantics

## Current State (verified 2026-06-02 — state-check per audit-to-gap-pipeline.md §2.5)

State-check phát hiện proposed fix gốc (V54 migration + `email_send_audit` UNIQUE) là
**architecturally invalid**: `kitehub-email` deliberately stateless — KHÔNG có JPA datasource,
flyway, hay `email_send_audit` table (xác nhận: no `db/migration` dir, no JPA/postgresql dep
trong pom). Symptom (RabbitMQ at-least-once redelivery → duplicate send tại consumer
`EmailEventListener`) là THẬT và uncovered tại consumer layer. Producer-side
`EmailServiceClient.alreadySentToday` (`email_sent_log` functional unique index trong
kitehub-subscription) chỉ guard PRODUCER publishing 2 lần/ngày — KHÔNG guard consumer
redelivery (event đã ở queue, producer dedup không re-run).

**Scope revised:** consumer-side dedup tại `EmailEventListener` qua `EmailIdempotencyGuard`
(Caffeine TTL — reuse cache backend đã có cho branding lookup, fits stateless design),
KHÔNG phải DB migration. Common-case duplicate (in-flight redelivery + same-process retry)
covered + MailHog-verified. Cross-restart + sister send paths → GAP-840.

## Problem

Wave 85 Bucket A simulation cell 22 surface failure mode:

- Scenario: 100 tenants concurrent; kitehub-email OOM mid-process → message ack lost → RabbitMQ redeliver → **duplicate email sent**.
- Beta cohort impact: Vy (anonymous prospect), Hằng (P2 owner), Tâm (P3 manager) nhận duplicate welcome / signup / invite emails → trust damage cho first-touch experience.
- Email send pipeline hiện tại không có idempotency key — RabbitMQ "at-least-once" delivery semantics + Spring listener exception → duplicate send.

GAP-502 RC2 OOM thrash historical context — even sau Wave 85 Bucket E `MaxRAMPercentage=60` override, edge case OOM vẫn possible khi noisy neighbor + high load. Idempotency = defense-in-depth.

## Root Cause

- Email send pipeline relies on RabbitMQ message ack — nếu service crash giữa "send email" + "publish ack", broker redeliver → duplicate.
- Schema `email_send_audit` table missing UNIQUE constraint trên idempotency_key.
- Spring `@RabbitListener` không wrap send + ack trong same transaction (broker ack ≠ business ack).

## Proposed Fix (SUPERSEDED — see Current State)

~~Wave 86 scope: V54 migration `email_send_audit` UNIQUE + ON CONFLICT~~ — invalid,
kitehub-email has no DB. Replaced by consumer-side Caffeine dedup (see Implemented Fix).

## Implemented Fix (2026-06-02)

1. **`EmailIdempotencyGuard`** (`kitehub-email/.../service/`) — Caffeine TTL dedup cache
   (default 60-min TTL, 50k max-size; reuses Caffeine backend already present for branding
   cache). `markIfFirstSeen(key)` atomic check-and-set via `putIfAbsent` (concurrent-safe);
   `computeKey(...)` derives deterministic SHA-256 key from explicit producer key OR
   recipient+template+type+sorted-variables.
2. **`EmailEventListener`** — checks guard before `emailSender.sendTemplatedEmail(...)`;
   duplicate within TTL → log "idempotent skip" + return (no re-send). Parses optional
   `idempotencyKey` field from `EmailEvent` for deterministic dedup.
3. **Config:** `kitehub.email.idempotency.ttl-minutes` (60) + `.max-size` (50000).

## Acceptance Criteria

- [x] Consumer-side dedup guard shipped (`EmailIdempotencyGuard` — Caffeine TTL, stateless-fit)
- [x] `EmailEventListener` checks guard before dispatch (idempotent skip on duplicate)
- [x] Deterministic key: explicit producer key OR derived SHA-256 hash (map-order-stable)
- [x] Unit tests: 7 guard tests (first-seen/duplicate/null-fail-open/explicit-precedence/deterministic/concurrent-50-thread-exactly-1) + 3 listener idempotency tests (explicit-key redelivery / derived-key redelivery / distinct-recipients no-false-dedup) — `mvnw verify -P strict-warnings` BUILD SUCCESS, 15/15 pass
- [x] **MailHog live verify** — publish identical `EmailEvent` twice to `email.send`: publish #1 → MailHog 1 message; publish #2 (redelivery) → MailHog STILL 1 (no 2nd Dispatching log) ✓ dedup fired
- [ ] Cross-restart idempotency (shared store survives OOM-restart) → **GAP-840** (architecture decision: Redis vs email_sent_log HTTP)
- [ ] `ClassRescheduledEmailService` sister send path dedup → **GAP-840**
- [ ] `EmailController` HTTP send-path idempotency-key → **GAP-840**
- [ ] (optional) producer `EmailEvent.idempotencyKey` explicit field in kitehub-subscription — deferred (consumer derives reliably without it)

## Pre-handoff verify per pre-handoff-self-test-completeness.md §2.9 (background job idempotency)

- [x] Worker picks up job (verify via container log "Dispatching queued email")
- [x] Duplicate handling: identical event redelivered → single send (MailHog matches=1 after 2 publishes)
- [x] Stack-up: kitehub-email (:8084 healthy) + kite-mailhog (:8025) + kite-rabbitmq (:15673) — rebuilt image with dedup code, recreated container, verified live
- ⚠️ DLQ + cross-restart redelivery not exercised live (GAP-840 scope)

## Related

- Wave 85 Bucket A simulation 3-axis: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-85-simulation-3axis.md` cell 22
- Wave 85 Bucket G G-AC4 (smoke test, paired)
- Wave 86 scope (planned for code implementation)
- GAP-502 (parent OOM thrash — idempotency = defense-in-depth)
- `pre-handoff-self-test-completeness.md` §2.9 background job class

## Log

- **2026-05-15** Filed via Wave 85 Bucket A simulation 3-axis audit cell 22. Bucket G G-AC4 smoke test ships Wave 85; schema + pipeline implementation defer Wave 86. Status OPEN.
- **2026-06-02** Fix shipped (autonomous local-doable gap campaign). State-check (audit-to-gap-pipeline.md §2.5) found original proposed fix (V54 + `email_send_audit` UNIQUE) architecturally invalid — kitehub-email is stateless (no DB). Symptom (consumer redelivery → duplicate) real + uncovered. Scope revised to consumer-side Caffeine TTL dedup at `EmailEventListener` (fits stateless design, reuses existing Caffeine cache backend). 7 guard + 3 listener unit tests, `mvnw verify -P strict-warnings` 15/15 PASS. MailHog live verify: identical event published twice → exactly 1 email delivered (publish #1 → 1 msg; publish #2 redelivery → still 1). Cross-flow sweep: 3 sister sites — `ClassRescheduledEmailService` (DEFER), `BrandingUpdatedListener` (EXEMPT — no send), `EmailController` HTTP (DEFER) → follow-up GAP-840. Status → PARTIAL (common-case shipped+verified; cross-restart + sister paths deferred to GAP-840 per gap-done-discipline.md §3 PARTIAL exit ramp).
