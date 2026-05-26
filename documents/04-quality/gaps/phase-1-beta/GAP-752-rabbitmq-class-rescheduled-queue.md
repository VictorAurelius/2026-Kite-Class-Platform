---
status: OPEN
priority: P1
phase: phase-1-beta
domain: Backend
found: 2026-05-26
wave-source: rst-cascade-1-cluster-4
related: [GAP-291]
---

# RabbitMQ class.rescheduled.queue declaration missing (Wave br-4 GAP-291 incomplete)

## Problem

`kiteclass-core` 24 restarts during stack startup (Wave rst-cascade-1 Phase 0 Preflight 2026-05-26). Spring AMQP log:
```
Caused by: com.rabbitmq.client.ShutdownSignalException: channel error;
protocol method: #method<channel.close>(reply-code=404, reply-text=NOT_FOUND -
no queue 'class.rescheduled.queue' in vhost '/', class-id=50, method-id=10)
```

Actuator `/actuator/health` returns 503 SERVICE_UNAVAILABLE → docker healthcheck fail → restart loop.

## Root Cause

- `kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/event/consumer/ClassRescheduledEmailConsumer.java:45` declares `@RabbitListener(queues = "class.rescheduled.queue")` — Spring AMQP defaults to `queueDeclarePassive` (assume queue exists)
- `kiteclass-core/src/main/java/com/kiteclass/core/common/config/RabbitConfig.java:85-86` says "Exchanges, queues, and bindings are defined per-module" — BUT no per-module config file exists for class/reschedule module
- Wave br-4 Bucket D (PR #1781 reschedule feature) shipped consumer code WITHOUT matching `@Bean Queue` declaration

Same issue applies to `ClassRescheduledNoOpConsumer.java` — both consumers use passive queue.

## Workaround Applied (Phase 0 cascade fix)

```bash
docker exec kite-rabbitmq rabbitmqadmin declare queue name=class.rescheduled.queue durable=true -u kitehub -p $RABBITMQ_PASS
# → kiteclass-core restart → healthy in ~30s
```

Workaround works locally but does NOT persist across RabbitMQ data-volume recreate (production cutover OR docker volume prune scenarios).

## Proposed Fix

Add module config `kiteclass-core/src/main/java/com/kiteclass/core/module/clazz/config/ClassRabbitConfig.java`:

```java
@Configuration
public class ClassRabbitConfig {
    public static final String QUEUE_NAME = "class.rescheduled.queue";
    public static final String EXCHANGE_NAME = "class.events.exchange";
    public static final String ROUTING_KEY = "class.rescheduled";

    @Bean
    public Queue classRescheduledQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public TopicExchange classEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Binding classRescheduledBinding(Queue classRescheduledQueue, TopicExchange classEventsExchange) {
        return BindingBuilder.bind(classRescheduledQueue).to(classEventsExchange).with(ROUTING_KEY);
    }
}
```

Cross-reference Wave br-4 GAP-291 implementation để ensure exchange + routing key match producer side.

## Acceptance Criteria

- [ ] `ClassRabbitConfig.java` created với `@Bean Queue` + `@Bean Exchange` + `@Bean Binding` per `RabbitConfig.java` line 85-86 module-config pattern
- [ ] `kiteclass-core` starts clean on fresh RabbitMQ vhost (data volume prune simulation) — no `NOT_FOUND` errors
- [ ] Integration test `ClassRescheduledQueueDeclarationIT` covers passive declare survives container restart
- [ ] Audit: `documents/04-quality/audits/quality/2026-05-26-wave-rst-cascade-1-cluster-4-infra-ui.md` §3 cascade finding #1 referenced

## Related

- GAP-291 Wave br-4 Bucket D (PR #1781) — reschedule feature shipped consumer without queue config
- Wave rst-cascade-1 Phase 0 Preflight discovery
- `kiteclass-core/.../ClassRescheduledEmailConsumer.java:45` (consumer site)
- `kiteclass-core/.../ClassRescheduledNoOpConsumer.java` (sister consumer same passive declare)
- `kiteclass-core/.../RabbitConfig.java:85-86` (module-config pattern reference)

## Effort estimate

~30 min coding + ~30 min IT test. Bundle với Wave rst-cascade-2 (~1-2 wave bucket).

## Log

- **2026-05-26 (OPEN):** Filed per Wave rst-cascade-1 closure audit §3 cascade finding #1. Phase 0 Preflight discovered + workaround applied. Wave rst-cascade-2 candidate. Per `audit-to-gap-pipeline.md` §3.
