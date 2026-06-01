---
audience: dev
---

# GAP-785 — RabbitMQ queue 'class.rescheduled.queue' không auto-declared on kiteclass-core startup

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-05-28 (Wave meta-6 human walk RST cycle)
**Phase:** phase-1-beta

## Problem

`kiteclass-core` startup fails to consume queue `class.rescheduled.queue` → `/actuator/health` returns 503 SERVICE_UNAVAILABLE → service stays "starting" indefinitely → kite-gateway circuit breaker stuck open → all `/api/v1/**` requests fail "Dịch vụ tạm ngưng".

```
2026-05-27 18:23:54 — Caused by: com.rabbitmq.client.ShutdownSignalException: 
  channel error; protocol method: #method<channel.close>(
  reply-code=404, reply-text=NOT_FOUND - no queue 'class.rescheduled.queue' in vhost '/'
```

## Root Cause

kiteclass-core declares queue via Spring AMQP `@RabbitListener` annotation. Expected behavior: Spring AMQP auto-declares queue at startup. Actual: queue declaration fails — possibly:
- RabbitMQ user `kitehub` doesn't have `configure` permission on vhost `/` to declare new queues (only `read+write` granted)
- OR `@RabbitListener` annotation missing `declare = true` flag (RabbitListener default may not auto-declare)
- OR Spring AMQP startup ordering — queue listener bean starts before admin/template can declare

Manual workaround verified: 
```bash
docker exec kite-rabbitmq rabbitmqadmin -u kitehub -p <PASS> declare queue name=class.rescheduled.queue durable=true
```
→ kiteclass-core restart → healthy in 42s.

## Affected scope

Any developer freshly cloning + running `kitehub/scripts/up.sh` will hit this. Ops-readiness audit (PR #1910 Wave 92 76/100) didn't catch.

## Proposed Fix

### Bucket A — kiteclass-core inline fix

Audit `@RabbitListener` declarations trong `kiteclass-core/module/**/*.java`. Add explicit `Queue` `@Bean` declarations OR `@RabbitListener(queuesToDeclare = @Queue("class.rescheduled.queue"))` to ensure auto-declare at startup.

Investigation needed: greps `@RabbitListener` + count queue-declaration completeness.

### Bucket B — RabbitMQ user permission grant

Grant kitehub user `configure` perm on vhost `/`:
```bash
rabbitmqctl set_permissions -p / kitehub ".*" ".*" ".*"
```

Update `kitehub/scripts/up.sh` or `kite-rabbitmq` Docker image init script to grant on first startup.

### Bucket C — Smoke test extension

ops-readiness post-up.sh smoke: `curl -sf http://localhost:8088/actuator/health || echo "FAIL"` — catches early at ops level instead of "starting" forever.

## Acceptance Criteria

- [ ] Fresh `down.sh + up.sh` → kiteclass-core healthy without manual queue declare
- [ ] RabbitMQ queue list includes `class.rescheduled.queue` after up.sh
- [ ] Bucket A investigation findings logged: which @RabbitListener annotations need explicit declare
- [ ] Bucket B: permission grant on user setup OR Bucket A inline fix
- [ ] ops-readiness audit skill rubric Cat 2 Observability extension: post-startup smoke check broker queue ↔ service consumers

## Related

- RST artifact: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-human-walk-rst.md` Finding #6 + Class C
- Wave 92 ops-readiness audit PR #1910 (76/100 — missed broker queue audit class)
- Existing Spring AMQP doc: https://docs.spring.io/spring-amqp/docs/current/reference/html/#queue-amqp-declared

## Log

- **2026-05-28** — Found qua Wave meta-6 RST walk. Blocked walk completion until manual workaround applied. P1 — affects all dev/staging environments. Per `meta-gap-priority.md` §3 META P1 force-multiplier (Bucket A inline + Bucket B perm + Bucket C audit skill ext = eliminate class permanently).
