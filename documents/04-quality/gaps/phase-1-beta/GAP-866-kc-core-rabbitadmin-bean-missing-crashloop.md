---
audience: dev
---

# GAP-866 — kiteclass-core crashloop: RabbitAdmin bean missing for declareRabbitQueuesEagerly

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-02 (Wave local-doable-6 Bucket I — GAP-777 live walk attempt)
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

- [ ] `kiteclass-core` boots healthy (Docker `(healthy)` status) without RabbitAdmin autowire error
- [ ] `/api/v1/*` endpoints respond 200/4xx/5xx (not gateway 503 fallback)
- [ ] Regression IT: `@SpringBootTest` boot test passes catching RabbitAdmin bean missing
- [ ] Live walk GAP-777 owner.test (`tenant_id IS NULL`) → 6 endpoints return 400 + ErrorResponse JSON (unblocks GAP-777 walk evidence completion)

## Related

- Blocks: GAP-777 live walk completion (FEATURE_SHIP_WALK_FOLLOWUP)
- Sister infra: kc-core RabbitMQ config history (search Wave plans + ADR-021 outbox pattern)

## Log

- **2026-06-02** (FILED): Wave local-doable-6 Bucket I — surfaced khi attempt GAP-777 live walk; kc-core in crashloop after restart. Pre-existing bug independent of FE interceptor work. Filed P0 vì blocks dev productivity + dev stack walks. Will resolve in next wave.
