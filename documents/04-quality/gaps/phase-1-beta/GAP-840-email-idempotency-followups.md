# GAP-840: Email idempotency follow-ups (sister send paths — cross-restart subsumed by GAP-580 Wave local-doable-5)

**Status:** 🟡 PARTIAL (cross-restart shipped via GAP-580 Redis swap; 2 sister send paths remain)
**Priority:** 🟡 P2 (defense-in-depth follow-up; GAP-580 đã cover common-case duplicate + cross-restart)
**Domain:** Backend / DevOps
**Found:** 2026-06-02 (GAP-580 fix cross-flow sweep + scope limitation documentation)
**Affects:** kitehub-email — sister consumer send paths (`ClassRescheduledEmailService` + `EmailController` HTTP)

## Problem

GAP-580 (Wave phase2-beta) shipped consumer-side idempotency cho `EmailEventListener`
(`email.send` queue) qua `EmailIdempotencyGuard` (Caffeine TTL dedup). Fix này cover
in-flight redelivery + same-process Spring-listener retry — duplicate cause phổ biến nhất.
Cross-flow sweep + scope analysis surface 3 follow-up items KHÔNG nằm trong scope GAP-580:

### 1. Cross-restart limitation ✅ SHIPPED (Wave local-doable-5 Bucket B)
~~`EmailIdempotencyGuard` dùng Caffeine in-process cache.~~ GAP-580 Wave local-doable-5
Bucket B đã swap `EmailIdempotencyGuard` từ Caffeine in-process → Redis SETNX
(`opsForValue().setIfAbsent`, key prefix `email:idempotency:<sha256>`, TTL 60 min) +
Caffeine fail-open fallback. Live verified qua `scripts/local/verify-cross-restart-dedup.sh`
(7 steps PASS — kite-redis key SURVIVES kitehub-email restart) + Testcontainers IT 3/3 PASS.
Architecture decision: chọn Redis (reuse `kite-redis` shared infra, avoid HTTP round-trip
to producer-side `email_sent_log`).

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

1. ~~Cross-restart~~ ✅ SHIPPED Wave local-doable-5 Bucket B (Redis SETNX path).
2. **ClassRescheduledEmailService:** inject `EmailIdempotencyGuard` + dedup check trước send.
3. **EmailController:** add `Idempotency-Key` header support (reuse interceptor pattern).

## Acceptance Criteria

- [x] ~~Outside-in benchmark (Redis vs email_sent_log HTTP) → ADR/decision doc~~ — chose Redis SETNX (Wave local-doable-5; reuse `kite-redis` shared infra, avoid HTTP round-trip; documented in GAP-580 Log)
- [x] **Cross-restart idempotency: redeliver SAU container restart → single send** — Testcontainers `EmailIdempotencyGuardRedisIT` 3/3 PASS + `scripts/local/verify-cross-restart-dedup.sh` 7 steps PASS
- [ ] `ClassRescheduledEmailService` dedup guard wired + test
- [ ] `EmailController` `Idempotency-Key` header support + test
- [ ] MailHog verify each remaining path: identical send twice → 1 message

## Related

- GAP-580 (parent — consumer-side `email.send` dedup shipped; this is the deferred remainder)
- `EmailIdempotencyGuard` (kitehub-email — §coverage/limitation javadoc)
- `kitehub-subscription` `IdempotencyHandlerInterceptor` (HTTP idempotency-key pattern precedent)
- `cross-flow-bug-class-sweep.md` (sweep methodology — this gap = DEFER rows from GAP-580 sweep)

## Log

- **2026-06-02** Filed from GAP-580 fix cross-flow sweep. GAP-580 covered `email.send`
  consumer dedup (common-case). 3 follow-up items deferred: cross-restart (architecture
  decision), ClassRescheduledEmailService sister path, EmailController HTTP path. Status OPEN.
- **2026-06-02 (Wave local-doable-5 Bucket B)** Item 1 (cross-restart) SUBSUMED bởi
  GAP-580 closure — `EmailIdempotencyGuard` swap Caffeine → Redis SETNX shipped + live
  verified. Items 2 (ClassRescheduledEmailService) + 3 (EmailController HTTP) còn lại;
  status OPEN → PARTIAL (1/3 sub-items SHIPPED). Priority P2 giữ nguyên — defense-in-depth,
  không blocking Phase 1 BETA (welcome/signup/invite path đã đủ coverage qua GAP-580).
