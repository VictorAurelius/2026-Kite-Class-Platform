# GAP-1076: kitehub-admin unhealthy — SB3 redis config thiếu (RedisHealthIndicator → localhost DOWN)

**Status:** 🟢 DONE 2026-06-08 — fix applied + verified (admin redis UP, overall healthy)
**Priority:** 🟡 P2
**Domain:** DevOps (config) / Backend
**Found:** 2026-06-08 (verify local stack sau GAP-1075 — user "check kitehub-admin unhealthy luôn")
**Affects:** `kitehub-admin` actuator health → DOWN (chặn ops monitoring + compose `depends_on` skip)

## Problem

Sau khi rebuild + `up --force-recreate` toàn stack (GAP-1075), `kitehub-admin` báo **unhealthy**. Health probe: `overall DOWN` — `db/diskSpace/ping/rabbit/ssl` đều UP, chỉ **`redis = DOWN`** (`RedisConnectionFailureException: Unable to connect to Redis`).

Cùng class bug GAP-1075 (found qua cross-flow sweep direction):
- kitehub-admin = Spring Boot 3 → đọc `spring.data.redis.*`.
- `docker-compose.kitehub.yml` chỉ set `SPRING_REDIS_HOST=kite-redis` (tên prop **SB2** `spring.redis.host`, SB3 ignore).
- `application.yml` admin **KHÔNG có** block `spring.data.redis` → fallback default `localhost:6379` → trong container không có redis → health DOWN.
- spring-data-redis nằm trên classpath **transitively** (không có direct dep trong pom) → `RedisHealthIndicator` auto-register + cố connect.
- Admin thực tế dùng **Caffeine** cho `@Cacheable` (`cache.type: caffeine`), KHÔNG có `RedisTemplate` → redis là phantom dependency, health check spurious.

## Proposed Fix

**APPLIED** (option A — consistent với platform + compose intent + GAP-1075 subscription fix): thêm block `spring.data.redis` vào `admin/application.yml` với env fallback chain `${SPRING_DATA_REDIS_HOST:${SPRING_REDIS_HOST:localhost}}` → đọc `SPRING_REDIS_HOST=kite-redis` compose đã set → health connect kite-redis thật → UP.

(Alternative option B — `management.health.redis.enabled: false` để tắt health check spurious — không chọn vì compose đã set intent admin reach redis + giữ consistent mọi service point kite-redis.)

## Acceptance Criteria

- [x] Root cause = SB3 redis config thiếu (cùng class GAP-1075), không phải redis chết
- [x] `spring.data.redis` block thêm vào admin application.yml (fallback chain)
- [x] Rebuild admin + verify `/actuator/health` → `redis = UP`, overall UP — **DONE** (rebuild.sh kitehub-admin → healthy 51s; `curl :8085/actuator/health` overall UP + redis UP)

## Related

- Discovered in: verify local stack post-GAP-1075 rebuild 2026-06-08
- Bug class: GAP-1075 (subscription SB3 redis config) — same class, cross-flow sweep direction
- `cross-flow-bug-class-sweep` (SB3 `spring.data.redis` config missing across services)
- Follow-up candidate: sweep các SB3 service khác có cùng pattern (compose set SPRING_REDIS_HOST nhưng yml thiếu spring.data.redis block)
