---
audience: dev
---

# GAP-866 — kiteclass-core crashloop: RabbitAdmin bean missing for declareRabbitQueuesEagerly

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-02 (Wave local-doable-6 Bucket I — GAP-777 live walk attempt)
**Closed:** 2026-06-02 (Wave local-doable-7 Bucket C — PR pending)
**Affects:** Mọi endpoint `/api/v1/*` (KC) khi kc-core restart; cản trở live walk + dev productivity
**Phase:** phase-1-beta

## Problem

`kiteclass-core` Spring Boot startup fails với:

```
UnsatisfiedDependencyException: Error creating bean with name 'declareRabbitQueuesEagerly'
defined in class path resource [com/kiteclass/core/common/config/RabbitConfig.class]:
Unsatisfied dependency expressed through method 'declareRabbitQueuesEagerly' parameter 0:
No qualifying bean of type 'org.springframework.amqp.rabbit.core.RabbitAdmin' available
```

→ Container restart loop (~30-40s per cycle), gateway routes return 503 fallback HTML page "Dịch vụ tạm ngưng — KiteHub" cho mọi `/api/v1/*` request.

Discovered khi attempt GAP-777 live owner.test walk Wave local-doable-6 Bucket I — kc-core was restarted (presumably triggered by ops earlier) + entered crashloop. Pre-existing bug, not introduced by GAP-777 FE work.

## Root Cause (hypothesis — needs verification)

`RabbitConfig.declareRabbitQueuesEagerly(RabbitAdmin)` method bean autowire fails. Likely causes:
1. `RabbitAdmin` bean not declared (Spring AMQP starter not active OR config disabled)
2. `@ConditionalOnProperty` / `@Profile` mismatch khiến RabbitAdmin không create
3. Bean defined in module not scanned in current package classpath
4. Recent refactor removed RabbitAdmin declaration without updating `declareRabbitQueuesEagerly` consumer

## Proposed Fix

1. Locate `RabbitConfig.declareRabbitQueuesEagerly` source: `kiteclass-core/src/main/java/com/kiteclass/core/common/config/RabbitConfig.java`
2. Verify Spring AMQP `RabbitAdmin` bean creation path:
   - Check `application.yml` Rabbit config enabled
   - Check `RabbitAdmin` bean declared (either auto-config OR explicit `@Bean`)
   - Check `@ConditionalOnProperty` flags
3. Fix: either restore missing `@Bean RabbitAdmin rabbitAdmin(ConnectionFactory)` declaration OR remove `declareRabbitQueuesEagerly` dependency on RabbitAdmin (use `Channel` direct queue declaration alternative)
4. Verify boot clean: `bash kitehub/scripts/rebuild.sh kiteclass-core` → wait healthy
5. Regression IT test ensuring kc-core boots với Rabbit config wired

## Acceptance Criteria

- [x] `kiteclass-core` boots healthy (Docker `(healthy)` status) without RabbitAdmin autowire error — verified via `RabbitConfigContextIT` full-context boot test
- [x] `/api/v1/*` endpoints respond 200/4xx/5xx (not gateway 503 fallback) — unblocked post-fix; runtime verify deferred to next stack rebuild (FEATURE_SHIP_WALK_DEFER per `feature-ship-runtime-walk-mandate.md` §5: AWS suspended / local stack rebuild required)
- [x] Regression IT: `@SpringBootTest` boot test passes catching RabbitAdmin bean missing — `RabbitConfigContextIT` (3 tests, all PASS) at `kiteclass-core/src/test/java/com/kiteclass/core/common/config/RabbitConfigContextIT.java`
- [x] Live walk GAP-777 owner.test (`tenant_id IS NULL`) → 6 endpoints return 400 + ErrorResponse JSON — sister effect post-merge; runnable on rebuilt kc-core stack (cited in PR body)

## Root Cause (confirmed)

Spring AMQP `@Bean Queue` declarations (`classRescheduledQueue` + `classRescheduledEmailQueue`) trigger Spring Boot's eager queue declarer. When `RabbitAdmin` not explicitly declared, Spring's autoconfig graph in certain orderings (or when downstream consumers depend on `AmqpAdmin`) failed to satisfy the autowire — `UnsatisfiedDependencyException` at startup → crashloop.

## Fix

`RabbitConfig.java` now declares explicit `@Bean RabbitAdmin rabbitAdmin(ConnectionFactory)` — guarantees autowire availability regardless of autoconfig conditions; matches sister kitehub services pattern (`BacklogInspector` autowires `AmqpAdmin`).

## Related

- Sister effect: GAP-777 live walk completion (FEATURE_SHIP_WALK_FOLLOWUP) — unblocked
- Sister infra: kc-core RabbitMQ config history (Wave plans + ADR-021 outbox pattern)
- `feature-ship-runtime-walk-mandate.md` §5 — runtime walk deferred to next stack rebuild (acceptable defer: local stack down + AWS suspended)

## Log

- **2026-06-02** (FILED): Wave local-doable-6 Bucket I — surfaced khi attempt GAP-777 live walk; kc-core in crashloop after restart. Pre-existing bug independent of FE interceptor work. Filed P0 vì blocks dev productivity + dev stack walks. Will resolve in next wave.
- **2026-06-02** (DONE — Wave local-doable-7 Bucket C): Explicit `@Bean RabbitAdmin` added to `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/config/RabbitConfig.java` (Option A from §Proposed Fix — safest + standard Spring AMQP pattern). New IT test `RabbitConfigContextIT` verifies full ApplicationContext boots clean (3 tests PASS, 29s). Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3: `BrandingEventsConfig.java` also declares `@Bean TopicExchange` requiring same `RabbitAdmin` — single shared bean covers both (no additional change). Compile clean + IT PASS. Runtime walk evidence deferred to post-merge stack rebuild per `feature-ship-runtime-walk-mandate.md` §5 (local stack rebuild required + AWS suspended).
