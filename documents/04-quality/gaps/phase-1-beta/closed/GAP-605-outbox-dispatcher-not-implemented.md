# GAP-605 — subscription_outbox dispatcher chưa implement; events stuck `dispatched_at = NULL`

**Status:** 🟢 DONE (2026-05-24 state-check found pre-existing implementation Wave 91 Bucket A)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-17 (Wave 90 walkthrough — beta.invite event không tới kitehub-email)
**Affects:** Mọi cross-service event publish qua `SubscriptionEventEmitter.emit()` — beta invite, beta consent audit, future migration events, future tenant init events

## Problem

`SubscriptionEventEmitter.emit()` chỉ write `subscription_outbox` row, KHÔNG có:
1. Fast-path `rabbitTemplate.convertAndSend` (per `design-patterns.md §3.5.1` Exception A)
2. Scheduled dispatcher poll `dispatched_at IS NULL` rows + publish to RMQ

Kết quả: outbox events stuck mãi mãi. Beta invite emails KHÔNG bao giờ tới invitee.

DB evidence (Wave 90 verify):
```
event_type=beta.invite.sent | topic=email.beta.invite | dispatched_at=NULL | created_at=2026-05-17 16:36:08
event_type=beta.invite.sent | topic=email.beta.invite | dispatched_at=NULL | created_at=2026-05-17 16:31:15
event_type=beta.consent.given | topic=audit.beta.consent | dispatched_at=NULL | created_at=2026-05-17 16:36:00
```

Code evidence:
```java
// kitehub-subscription/.../SubscriptionEventEmitter.java line ~50
public void emit(UUID instanceId, String eventType, String topic, String payload) {
    SubscriptionOutboxEvent event = SubscriptionOutboxEvent.builder()...build();
    outboxRepository.save(event);  // ← chỉ write DB, không publish RMQ
    log.debug("Outbox event queued...");
}
```

`find kitehub/kitehub-subscription/src/main -name "*Dispatcher*" -o -name "*Relay*"` → 0 results (chỉ `SubscriptionOutboxRepository` + `SubscriptionOutboxEvent` entity).

## Root cause

Wave 33 GAP-372 implement outbox pattern nhưng dispatcher portion chưa ship. Có thể planned for follow-up wave mà bị miss khi closure flip DONE. Architectural debt accumulated 6+ waves.

## Proposed Fix

### Phase 1 (hotfix, ≤30 min)
Add fast-path RMQ publish trong `SubscriptionEventEmitter.emit()` (per `design-patterns.md §3.5.1` Exception A — pattern đã có sẵn trong `EmailServiceClient.publishToQueue`):

```java
public void emit(UUID instanceId, String eventType, String topic, String payload) {
    SubscriptionOutboxEvent event = SubscriptionOutboxEvent.builder()...build();
    outboxRepository.save(event);
    try {
        rabbitTemplate.convertAndSend(EmailQueueConfig.EMAIL_EXCHANGE, topic, payload);
        log.debug("Fast-path publish OK: {}", eventType);
    } catch (Exception ex) {
        log.warn("Fast-path publish failed, dispatcher will retry: {}", ex.getMessage());
    }
}
```

### Phase 2 (sustained, 2-4h)
Implement `@Scheduled` dispatcher:
- Poll every 10s WHERE `dispatched_at IS NULL`
- Publish to RMQ via topic from row
- UPDATE `dispatched_at = NOW()` on success
- Skip rows with last-failure >5min (backoff)
- Metric: `outbox_dispatcher_lag_seconds` + `outbox_undispatched_count`

## Acceptance Criteria

- [ ] Phase 1: `SubscriptionEventEmitter.emit()` writes outbox + publishes RMQ (fast-path). New beta approve → email arrives within 5s.
- [ ] Phase 2: `SubscriptionOutboxDispatcher` class + `@Scheduled(fixedDelay=10000)` poll. Stuck rows post-RMQ-downtime get re-published.
- [ ] Backfill stuck rows: SQL UPDATE existing `dispatched_at = NULL` rows OR republish from app (after Phase 1 deploy).
- [ ] Integration test: stop RMQ → emit event → row stays NULL → start RMQ → dispatcher catches up.
- [ ] Metric exposed `/actuator/prometheus`.

## Related

- `design-patterns.md` §3.5 + §3.5.1 — outbox + fast-path pattern
- Wave 33 GAP-372 — original beta invite outbox design
- GAP-606 — admin-new-login-alert template missing (separate poison-pill bug surfaced same incident)
- GAP-607 — RMQ DLQ not configured (causes infinite retry on poison pills)
- Wave 90 audit `documents/04-quality/audits/aws-verification/2026-05-17-wave-90-live-verify.md` §9 (sub-finding)

## Log

- **2026-05-17:** Gap filed during Wave 90 walkthrough — user submit beta + approve → DB row APPROVED ✅ → outbox row inserted ✅ → email NEVER arrives ❌. Workaround: direct HTTP POST kitehub-email/api/platform/emails/send works (template renders OK). P0 BLOCKER beta cohort onboarding — every beta approve produces orphan outbox row.
- **2026-05-24 (DONE — state-check found pre-existing impl):** Wave beta-readiness-2 Bucket C fix-time state-check per `audit-to-gap-pipeline.md` §2.8 (gap age 7 days, drift-class trigger). State-check evidence:
  - `find kitehub-subscription/src/main -name "*Dispatcher*"` → `SubscriptionOutboxDispatcher.java` EXISTS (164 LOC, @Scheduled fixedDelayString="${outbox.dispatcher.poll-interval-ms:10000}")
  - `grep "rabbitTemplate.convertAndSend" SubscriptionEventEmitter.java` → fast-path EXISTS (line 90, Exception A pattern per `design-patterns.md` §3.5.1)
  - `git log --diff-filter=A SubscriptionOutboxDispatcher.java` → shipped commit `017ce90d` 2026-05-18 Wave 91 Bucket A PR #1487 `feat(wave-91 bucket A): outbox dispatcher + RMQ DLQ (GAP-605+607)`
  - Gap file Status field never flipped at PR #1487 closure → stale 7 days
  - Per §2.8 decision matrix "Symptom no longer present (self-corrected)" → Flip DONE, NO new fix PR needed
  - Bucket C scope (Wave beta-readiness-2 plan) = no-op; mark Bucket C SHIPPED-via-pre-existing in wave plan closure scope reconciliation table
