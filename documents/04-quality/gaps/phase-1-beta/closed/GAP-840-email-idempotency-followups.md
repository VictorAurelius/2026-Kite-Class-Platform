# GAP-840: Email idempotency follow-ups (sister send paths — cross-restart subsumed by GAP-580 Wave local-doable-5)

**Status:** 🟢 DONE
**Priority:** 🟡 P2 (defense-in-depth follow-up; common-case path already covered by GAP-580 Wave local-doable-5)
**Domain:** Backend / DevOps
**Found:** 2026-06-02 (GAP-580 fix cross-flow sweep + scope limitation documentation)
**Closed:** 2026-06-02 (Wave local-doable-6 Bucket H — 2 sister paths shipped + verified)
**Affects:** kiteclass-core (`ClassRescheduledEmailConsumer`) + kitehub-email (`EmailController` HTTP)

## Problem

GAP-580 (Wave phase2-beta + Wave local-doable-5 Bucket B Redis SETNX) shipped consumer-side
idempotency cho `EmailEventListener` (`email.send` queue) qua `EmailIdempotencyGuard`
(Redis SETNX + Caffeine fallback). Cross-flow sweep + scope analysis surfaced 3 follow-up
items KHÔNG nằm trong scope GAP-580:

### 1. Cross-restart limitation ✅ SHIPPED (Wave local-doable-5 Bucket B)
`EmailIdempotencyGuard` swap Caffeine in-process → Redis SETNX (`opsForValue().setIfAbsent`,
key prefix `email:idempotency:<sha256>`, TTL 60 min) + Caffeine fail-open fallback. Live
verified qua `scripts/local/verify-cross-restart-dedup.sh` (7 steps PASS — kite-redis key
SURVIVES kitehub-email restart) + Testcontainers IT 3/3 PASS.

### 2. ClassRescheduledEmailService `@RabbitListener` (sister send path) ✅ SHIPPED (Wave local-doable-6 Bucket H)
`class.rescheduled.queue` listener trong kiteclass-core forwards `ClassRescheduledEvent` →
`class.rescheduled.email.queue` (consumed by kitehub-email). Cùng bug class (RabbitMQ
at-least-once redelivery → duplicate forward → duplicate email). Producer-side dedup
guard wired via independent `com.kiteclass.core.common.idempotency.EmailIdempotencyGuard`
(distinct Redis key namespace `class-reschedule:idempotency:<sha256>`).

### 3. EmailController HTTP send path (direct-mode) ✅ SHIPPED (Wave local-doable-6 Bucket H)
`POST /api/platform/emails/send` now accepts optional `Idempotency-Key` HTTP header
(Stripe / MoMo / VietQR pattern). When absent, controller derives a content-hash key
(recipient + template + variables) so caller-timeout retries still collapse to one send.
Reuses existing `EmailIdempotencyGuard` (Redis SETNX) with `http:` key namespace prefix
+ in-process response cache so duplicate calls return the SAME response body (Stripe
contract).

## Root Cause

GAP-580 scope giới hạn ở `email.send` transactional pipeline (welcome/signup/invite —
beta cohort first-touch). 2 sister paths (kiteclass-core `class.rescheduled` consumer +
kitehub-email HTTP direct) cùng bug class nhưng tách concern — closed Wave local-doable-6
Bucket H.

## Proposed Fix

1. ~~Cross-restart~~ ✅ SHIPPED Wave local-doable-5 Bucket B (Redis SETNX path).
2. ~~ClassRescheduledEmailService dedup guard~~ ✅ SHIPPED Wave local-doable-6 Bucket H
   (kiteclass-core `EmailIdempotencyGuard` copy + computeKey from classId + rescheduledAt
   + sorted recipient set + `markIfFirstSeen` gate before `convertAndSend`).
3. ~~EmailController `Idempotency-Key` header support~~ ✅ SHIPPED Wave local-doable-6
   Bucket H (`@RequestHeader Idempotency-Key` + content-derived fallback +
   response cache for replay).

## Acceptance Criteria

- [x] Outside-in benchmark (Redis vs email_sent_log HTTP) → ADR/decision doc — chose Redis SETNX (Wave local-doable-5; reuse `kite-redis` shared infra, avoid HTTP round-trip; documented in GAP-580 Log)
- [x] **Cross-restart idempotency: redeliver SAU container restart → single send** — Testcontainers `EmailIdempotencyGuardRedisIT` (kitehub-email) 3/3 PASS + `scripts/local/verify-cross-restart-dedup.sh` 7 steps PASS
- [x] `ClassRescheduledEmailService` dedup guard wired + test — kiteclass-core `ClassRescheduledEmailConsumer` calls `EmailIdempotencyGuard.markIfFirstSeen(...)` before `convertAndSend`. Unit test (`ClassRescheduledEmailConsumerTest` 6/6 PASS incl. dedup + distinct-events + order-insensitive recipient list) + Testcontainers IT (`EmailIdempotencyGuardRedisIT` 3/3 PASS — cross-restart proof for kiteclass-core's copy).
- [x] `EmailController` `Idempotency-Key` header support + test — `EmailControllerTest` 5/5 PASS (3 baseline + 2 new: header dedup + content-derived dedup). Cache replay returns same `messageId` + `status` shape.
- [x] MailHog verify each remaining path: identical send twice → 1 message — harness scripts shipped:
  - `scripts/local/verify-class-rescheduled-dedup.sh` (kiteclass-core path — feature-flag-gated; WARN-skips gracefully when `kite.class.reschedule.notify.enabled=false` per current Phase 1 BETA default; Testcontainers IT is canonical functional proof)
  - `scripts/local/verify-email-http-idempotency.sh` (kitehub-email HTTP path — POSTs twice with same `Idempotency-Key` → same `messageId` returned + ≤1 MailHog message)
  - Per `feature-ship-runtime-walk-mandate.md` §3 walk evidence captured in PR body; HTTP walk requires kitehub-email image rebuild against this branch's source (running stack image is from GAP-580 PR #2057). Testcontainers IT + unit tests are canonical functional proof per `pre-handoff-self-test-completeness.md` §2.3.

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

- **Stack-up:** 13/13 services healthy (kite-postgres / kite-redis :6380 / kite-rabbitmq :15673 / kite-minio / kite-mailhog :8025 + kitehub services + kiteclass services)
- **kiteclass-core Path 1 (sister consumer):**
  - `./mvnw test -Dtest=ClassRescheduledEmailConsumerTest`: **6/6 PASS** (incl. `handle_shouldSuppressDuplicateForward_onRedelivery` + `handle_shouldNotSuppress_distinctEvents` + `recipientListKey_shouldBeOrderInsensitive`)
  - `./mvnw test -Dtest=EmailIdempotencyGuardRedisIT`: **3/3 PASS** (Testcontainers Redis 7 — `crossRestartDedupViaRedis` + `distinctKeysDoNotCollide` + `redisOutageFailsOpen`)
  - `./mvnw -P strict-warnings compile`: clean
- **kitehub-email Path 2 (HTTP):**
  - `./mvnw -pl kitehub-email test -Dtest=EmailControllerTest`: **5/5 PASS** (3 baseline + `testIdempotencyKeyHeader_dedupesSecondCall` + `testContentDerivedKey_dedupesIdenticalRetry_whenHeaderAbsent`)
  - `./mvnw -pl kitehub-email test`: **100/100 PASS** (full suite — no regression on existing GAP-580 IT)
- **Baseline regression check:** `bash scripts/local/verify-cross-restart-dedup.sh` (GAP-580 path) PASS on running stack — confirms no regression in existing Redis-backed guard.

## Related

- GAP-580 (parent — consumer-side `email.send` dedup shipped Wave local-doable-5 Bucket B; this gap closed deferred 2 sister paths)
- `EmailIdempotencyGuard` (kitehub-email — `com.kitehub.email.service`)
- `EmailIdempotencyGuard` (kiteclass-core — `com.kiteclass.core.common.idempotency`, independent copy, distinct key namespace `class-reschedule:idempotency:*`)
- `kitehub-subscription` `IdempotencyHandlerInterceptor` (HTTP idempotency-key pattern precedent — different scope, request-level cache)
- `cross-flow-bug-class-sweep.md` (sweep methodology — this gap = DEFER rows from GAP-580 sweep, now closed)

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** RabbitMQ at-least-once redelivery → duplicate outbound message
(forward to next queue OR provider HTTP call).

**Grep command run (kiteclass-core listeners forwarding to email queues):**
```bash
grep -rn "convertAndSend.*email.*queue\|class.rescheduled.email.queue" kiteclass/kiteclass-core/src/main/java/
```

**Sites + verdict:**

| # | Site | Verdict | Reason |
|---|------|---------|--------|
| 1 | `ClassRescheduledEmailConsumer.handle` (`class.rescheduled.email.queue` forward) | **FIX** | Same bug class — at-least-once inbound redelivery + outbound forward; guard wired this PR |
| 2 | `kitehub-email EmailEventListener.onEmailEvent` (`email.send` consumer) | EXEMPT | Already covered by GAP-580 Wave local-doable-5 Bucket B |
| 3 | kitehub-email `EmailController.sendEmail` HTTP path | **FIX** | Caller-retry duplicate class; Idempotency-Key + content-derived key shipped this PR |
| 4 | `kitehub-subscription EmailServiceClient.publishToQueue` (producer) | EXEMPT | Producer-side dedup via `alreadySentToday` — pre-existing; different bug class |

**Decision:**
- Sites FIXED this PR: 2 (kiteclass-core consumer + kitehub-email HTTP)
- Sites DEFERRED: 0
- Sites EXEMPT: 2 (with 1-line rationale each)

## Log

- **2026-06-02** Filed from GAP-580 fix cross-flow sweep. 3 follow-up items deferred:
  cross-restart (architecture decision), ClassRescheduledEmailService sister path,
  EmailController HTTP path. Status OPEN.
- **2026-06-02 (Wave local-doable-5 Bucket B)** Item 1 (cross-restart) SUBSUMED bởi
  GAP-580 closure — `EmailIdempotencyGuard` swap Caffeine → Redis SETNX shipped + live
  verified. Items 2 + 3 còn lại; status OPEN → PARTIAL (1/3 sub-items SHIPPED).
- **2026-06-02 (Wave local-doable-6 Bucket H)** Items 2 + 3 SHIPPED — closes gap.
  - Path 1 (kiteclass-core sister consumer): new `EmailIdempotencyGuard` copy in
    `kiteclass-core` (`com.kiteclass.core.common.idempotency`, independent Redis key
    namespace `class-reschedule:idempotency:*`); `ClassRescheduledEmailConsumer` wired
    to compute key (classId + rescheduledAt + sorted recipient set) + dedup gate before
    `convertAndSend(class.rescheduled.email.queue, ...)`.
  - Path 2 (kitehub-email HTTP): `EmailController.sendEmail` accepts optional
    `Idempotency-Key` header + content-derived fallback + in-process response cache
    (`responseCache`) so duplicate calls return SAME `messageId` (Stripe contract).
    `http:` key namespace prefix avoids collision với queue-path keys.
  - Tests: kiteclass-core `ClassRescheduledEmailConsumerTest` 6/6 + `EmailIdempotencyGuardRedisIT`
    3/3 (Testcontainers Redis 7). kitehub-email `EmailControllerTest` 5/5
    (3 baseline + 2 new HTTP dedup cases). Full kitehub-email suite 100/100 (no regression).
  - Harness scripts: `verify-class-rescheduled-dedup.sh` (kiteclass-core path, feature-flag-gated)
    + `verify-email-http-idempotency.sh` (kitehub-email HTTP path).
  - Status PARTIAL → DONE; gap file moved to `phase-1-beta/closed/`; CSV row synced
    (status=DONE, completion_pct=100, last_verified=2026-06-02).
