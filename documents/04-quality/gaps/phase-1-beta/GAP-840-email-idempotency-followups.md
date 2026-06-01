# GAP-840: Email idempotency follow-ups (cross-restart + sister send paths)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (defense-in-depth follow-up; GAP-580 đã cover common-case duplicate)
**Domain:** Backend / DevOps
**Found:** 2026-06-02 (GAP-580 fix cross-flow sweep + scope limitation documentation)
**Affects:** kitehub-email — cross-restart idempotency + sister consumer send paths

## Problem

GAP-580 (Wave phase2-beta) shipped consumer-side idempotency cho `EmailEventListener`
(`email.send` queue) qua `EmailIdempotencyGuard` (Caffeine TTL dedup). Fix này cover
in-flight redelivery + same-process Spring-listener retry — duplicate cause phổ biến nhất.
Cross-flow sweep + scope analysis surface 3 follow-up items KHÔNG nằm trong scope GAP-580:

### 1. Cross-restart limitation (Caffeine in-process)
`EmailIdempotencyGuard` dùng Caffeine in-process cache. Sau OOM-crash-restart (đúng
failure mode cell 22 mô tả), cache trống → message redelivered SAU restart có thể re-send.
Full cross-restart idempotency cần shared store (Redis HOẶC DB). kitehub-email hiện
stateless (no JPA/Redis) — thêm shared store là architecture decision (per
`outside-in-coverage-trigger.md` §2.1 keyword "integration/store") cần benchmark
(Redis vs reuse producer-side `email_sent_log` qua HTTP check).

### 2. ClassRescheduledEmailService `@RabbitListener` (sister send path)
`class.rescheduled.email.queue` consumer cũng send email → cùng bug class (queue
redelivery → duplicate). Chưa có dedup guard.

### 3. EmailController HTTP send path (direct-mode)
`POST /api/platform/emails/send` (`EmailController.sendTemplatedEmail`/`sendEmail`)
synchronous HTTP — at-most-once theo HTTP semantics, NHƯNG caller retry (timeout) có thể
gây duplicate. Idempotency cần request-level `Idempotency-Key` header từ caller
(pattern giống `kitehub-subscription` `IdempotencyHandlerInterceptor`).

## Root Cause

GAP-580 scope giới hạn ở `email.send` transactional pipeline (welcome/signup/invite —
beta cohort first-touch). Các path khác cùng bug class nhưng tách concern.

## Proposed Fix

1. **Cross-restart:** benchmark Redis dedup store vs producer-side `email_sent_log` HTTP
   check; pick 1; apply tới `EmailIdempotencyGuard` (replace Caffeine với shared backend
   HOẶC two-tier Caffeine-first + shared-fallback). Outside-in audit trước khi lock.
2. **ClassRescheduledEmailService:** inject `EmailIdempotencyGuard` + dedup check trước send.
3. **EmailController:** add `Idempotency-Key` header support (reuse interceptor pattern).

## Acceptance Criteria

- [ ] Outside-in benchmark (Redis vs email_sent_log HTTP) → ADR/decision doc
- [ ] Cross-restart idempotency: redeliver SAU container restart → single send (Testcontainers Redis OR integration)
- [ ] `ClassRescheduledEmailService` dedup guard wired + test
- [ ] `EmailController` `Idempotency-Key` header support + test
- [ ] MailHog verify each path: identical send twice → 1 message

## Related

- GAP-580 (parent — consumer-side `email.send` dedup shipped; this is the deferred remainder)
- `EmailIdempotencyGuard` (kitehub-email — §coverage/limitation javadoc)
- `kitehub-subscription` `IdempotencyHandlerInterceptor` (HTTP idempotency-key pattern precedent)
- `cross-flow-bug-class-sweep.md` (sweep methodology — this gap = DEFER rows from GAP-580 sweep)

## Log

- **2026-06-02** Filed from GAP-580 fix cross-flow sweep. GAP-580 covered `email.send`
  consumer dedup (common-case). 3 follow-up items deferred: cross-restart (architecture
  decision), ClassRescheduledEmailService sister path, EmailController HTTP path. Status OPEN.
