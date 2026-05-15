# GAP-580: Email send idempotency key (UNIQUE constraint on `email_send_audit`)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (trust-damage prevention cho beta cohort — duplicate emails = poor UX)
**Domain:** Backend / Database / DevOps
**Found:** 2026-05-15 (Wave 85 Bucket A simulation 3-axis cell 22)
**Affects:** kitehub-email service email send pipeline + RabbitMQ message redelivery semantics

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

## Proposed Fix

Wave 86 scope (3 sub-tasks, paired với Wave 85 Bucket G G-AC4 smoke test):

1. **Schema migration V54** — add `idempotency_key VARCHAR(255) UNIQUE` cho `email_send_audit` table; backfill existing rows với UUID generation.
2. **Pipeline logic** — `EmailService.send()` accept `idempotency_key` param (default = hash of recipient + template + params); INSERT INTO `email_send_audit (idempotency_key, ...) ON CONFLICT (idempotency_key) DO NOTHING RETURNING id` — nếu RETURNING id → send via Resend; nếu no row returned (duplicate key) → skip send, log "idempotent skip".
3. **Smoke test trong Wave 85 Bucket G G-AC4** — RabbitMQ force redeliver scenario (kill listener mid-process) → assert single email delivered to Mailtrap inbox.

## Acceptance Criteria

- [ ] V54 migration adds `idempotency_key VARCHAR(255) UNIQUE` cho `email_send_audit`
- [ ] `EmailService.send()` accepts idempotency_key param (auto-generate default hash)
- [ ] ON CONFLICT DO NOTHING RETURNING id flow tested
- [ ] Spring `@RabbitListener` idempotency wrap shipped
- [ ] Wave 85 Bucket G smoke test `smoke-email-idempotency.sh` PASS (force redeliver → single send)
- [ ] Integration test: trigger duplicate via parallel @Async → 1 email + 1 idempotent-skip log
- [ ] Resend API call count metric — assert single call per idempotency_key
- [ ] Pre-handoff verify per `pre-handoff-self-test-completeness.md` §2.9 (background job idempotency)

## Related

- Wave 85 Bucket A simulation 3-axis: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-85-simulation-3axis.md` cell 22
- Wave 85 Bucket G G-AC4 (smoke test, paired)
- Wave 86 scope (planned for code implementation)
- GAP-502 (parent OOM thrash — idempotency = defense-in-depth)
- `pre-handoff-self-test-completeness.md` §2.9 background job class

## Log

- **2026-05-15** Filed via Wave 85 Bucket A simulation 3-axis audit cell 22. Bucket G G-AC4 smoke test ships Wave 85; schema + pipeline implementation defer Wave 86. Status OPEN.
