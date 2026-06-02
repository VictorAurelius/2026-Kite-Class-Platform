---
audience: dev
---

# GAP-866 — kiteclass-core RabbitMQ queue auto-declare broken (cold rebuild fail)

**Status:** 🟡 PARTIAL (proposed fix shipping Wave local-doable-5 Bucket E; cold-broker verify deferred next session)
**Priority:** 🟠 P1 (cold rebuild blocker — manual workaround restore-able)
**Domain:** Backend (kiteclass-core RabbitMQ topology)
**Found:** 2026-06-02 (Wave local-doable-5 Bucket A first cold rebuild on fresh WSL)
**Affects:** kiteclass-core startup khi RabbitMQ broker fresh (no queues persisted)
**Phase:** phase-1-beta

## Problem

Fresh `docker-compose up` với volume-less hoặc fresh RabbitMQ instance → `kiteclass-core` startup FAIL:

```
ERROR o.s.boot.SpringApplication - Application run failed
Caused by: org.springframework.amqp.rabbit.listener.BlockingQueueConsumer$DeclarationException:
  Failed to declare queue(s):[class.rescheduled.queue]
Caused by: com.rabbitmq.client.ShutdownSignalException: channel error;
  protocol method: #method<channel.close>(reply-code=404, reply-text=NOT_FOUND
  - no queue 'class.rescheduled.queue' in vhost '/', class-id=50, method-id=10)
```

Spring context refresh cancelled → ProtocolHandler stopped → HikariCP shutdown → container restart loop (`health: starting` indefinitely). Container never reach healthy state.

## Root Cause (verified 2026-06-02 — fix-time state-check per audit-to-gap-pipeline.md §2.8)

State-check empirical:
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/config/RabbitConfig.java` declares 2 Queue `@Bean`s (`classRescheduledQueue` line 103, `classRescheduledEmailQueue` line 119)
- Spring Boot auto-configures `RabbitAdmin` (no explicit bean override)
- `@RabbitListener(queues = "class.rescheduled.queue")` trên `ClassRescheduledNoOpConsumer` + `ClassRescheduledEmailConsumer` (mutually exclusive via `@ConditionalOnProperty kite.class.reschedule.notify.enabled`)
- Same `@RabbitListener(queues = "...")` pattern dùng thành công trong kitehub-branding (queues declared correctly trên fresh broker)

**Likely root cause:** Race between RabbitAdmin's connection-listener triggered declaration cycle và `@RabbitListener` container `queueDeclarePassive` call. Khi RabbitConfig CHỈ có Queue beans (KHÔNG có Exchange + Binding tạo topology completeness signal — đối chiếu kitehub-branding có cả Queue + Exchange + Binding triggering full topology declare), eager declaration không guaranteed trước listener start trên fresh broker.

KhÔng phải bug logic — đây là Spring AMQP lifecycle ordering edge case khi topology incomplete.

## Workaround (2026-06-02 — applied manually)

```bash
docker exec kite-rabbitmq rabbitmqadmin -u kitehub -p <password> \
  declare queue name=class.rescheduled.queue durable=true
docker exec kite-rabbitmq rabbitmqadmin -u kitehub -p <password> \
  declare queue name=class.rescheduled.email.queue durable=true
docker restart kiteclass-core
```

Queue persist trong RabbitMQ volume mount → workaround chỉ cần áp dụng 1 lần per fresh broker. Subsequent restarts hoạt động bình thường.

## Proposed Fix (Wave local-doable-5 Bucket E — this PR)

Thêm `ApplicationRunner` bean vào `RabbitConfig.java` declare queues eagerly via `RabbitAdmin` ngay khi context ready (BEFORE listener container start):

```java
@Bean
public ApplicationRunner declareRabbitQueuesEagerly(
        RabbitAdmin rabbitAdmin,
        Queue classRescheduledQueue,
        Queue classRescheduledEmailQueue) {
    return args -> {
        rabbitAdmin.declareQueue(classRescheduledQueue);
        rabbitAdmin.declareQueue(classRescheduledEmailQueue);
        log.info("Declared RabbitMQ queues eagerly: {}, {}",
                classRescheduledQueue.getName(),
                classRescheduledEmailQueue.getName());
    };
}
```

Defensive vì:
- `RabbitAdmin.declareQueue()` idempotent (existing queue → no-op)
- Chạy SAU context ready → guarantees RabbitAdmin initialized
- Chạy TRƯỚC `@RabbitListener` actual message consumption start
- Log line "Declared RabbitMQ queues eagerly" cho phép verify trong logs

## Acceptance Criteria

- [x] Root cause documented (Spring AMQP lifecycle ordering, fresh broker race)
- [x] Manual workaround documented (rabbitmqadmin declare commands)
- [x] Proposed fix shipped (`ApplicationRunner` in RabbitConfig.java) — Wave local-doable-5 Bucket E
- [x] Compile + image rebuild PASS local
- [x] kiteclass-core healthy after rebuild với fix applied (existing broker, queues persisted)
- [ ] **Cold-broker verify**: delete queue from broker → restart kc-core → verify queue auto-declared without manual intervention — DEFERRED next session (would break parallel Bucket B/C agents working against current healthy stack)
- [ ] Long-term: consider declare Exchange + Binding cho topology completeness (parity với kitehub-branding pattern) — DEFERRED follow-up nếu fix này không cover all cases

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 — code-level + warm-broker verify)

- Stack-up: `bash kitehub/scripts/up.sh --profile full --rebuild` (12 min cold cache fresh WSL)
- Empirical observation: kiteclass-core startup FAIL với 404 NOT_FOUND on fresh broker; workaround applied; rebuild với fix verified application log shows "Declared RabbitMQ queues eagerly" trên warm broker
- Cold-broker verify: DEFER next session per AC above

## Related

- Wave local-doable-5 Bucket A — cold rebuild incident (this gap surfaced)
- Bucket E proposed fix — RabbitConfig.java ApplicationRunner
- Sister patterns: kitehub-branding RabbitMQConfig + AIQueueConfig (Queue + Exchange + Binding all together — declare correctly)

## Log

- **2026-06-02** (PARTIAL): Bucket E (Wave local-doable-5) — file gap + ship proposed fix in single PR. Fix-time state-check per `audit-to-gap-pipeline.md` §2.8 confirmed symptom present at fresh broker rebuild moment. Compile + warm-broker verify PASS; cold-broker verify deferred next session để không break parallel Bucket B+C agents (their worktrees branched off `wave/local-doable-5` BEFORE this fix; broker state must remain healthy until they merge). Per `gap-done-discipline.md` §1 — keeping PARTIAL vì AC cold-broker verify unchecked; mark DONE post next session cold-rebuild that confirms fix sufficient.
