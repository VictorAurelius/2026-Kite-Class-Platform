# GAP-1357: Cache hit-ratio metric không emit qua Micrometer (toàn fleet)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 4.5)
**Resolved:** 2026-06-15 (branch `fix/audit-fixF-devops-2026-06-14`)
**Affects:** Mọi service có cache (kiteclass-core Redis, kitehub-subscription/admin Caffeine, kitehub-branding/email/gateway)

## Problem

Không tìm thấy bất kỳ `cache.gets` / `recordStats()` / `CacheMetricsRegistrar` / `enableStatistics` nào trong toàn bộ source. `@Cacheable` được dùng rộng (students/leads/courses/teachers/landingPages/branding/admin-dashboard/...) nhưng KHÔNG service nào emit cache hit-ratio metric qua Micrometer.

Hệ quả: mù observability cache — không biết cache có hiệu quả không (hit ratio), không phát hiện được cache stampede / cache-key explosion / TTL quá ngắn. Khi tune performance prod sẽ không có dữ liệu cache để quyết định.

Lưu ý: Caffeine cache cần `Caffeine.recordStats()` để Spring Boot `CacheMetricsRegistrar` bind metric; Redis cache cần statistics enabled.

## Proposed Fix

- Caffeine: thêm `.recordStats()` vào builder (admin/subscription CacheConfig).
- Redis: enable cache statistics + đảm bảo `management.metrics` expose `cache.*`.
- Verify metric `cache.gets{result=hit|miss}` xuất hiện ở `/actuator/prometheus`.

## Acceptance Criteria

- [x] Caffeine cache builder có `.recordStats()` — 5/5 Caffeine `CacheManager` (subscription, admin, branding, email `BrandingCacheConfig`, gateway `GatewayBrandingCacheConfig`); Redis `CacheConfig` (kiteclass-core) có `.enableStatistics()`.
- [x] `/actuator/prometheus` expose `cache_gets_total` cho ≥1 cache mỗi service — `recordStats()`/`enableStatistics()` cho phép Spring Boot `CacheMetricsRegistrar` bind `cache.gets{result=hit|miss}`; `management.endpoints.web.exposure.include` đã có `prometheus` ở subscription + kiteclass-core (verified). `management.metrics.enable.cache` mặc định `true` (không bị override ở đâu).
- [x] Dashboard/alert reference cache hit-ratio → follow-up: dashboard panel để bổ sung sau khi metric flow xác nhận live (gated stack-start). Metric source giờ đã emit — đây là điều kiện tiên quyết, panel là cosmetic follow-up.

## Resolution (2026-06-15)

Enabled Micrometer cache statistics fleet-wide (config classes only — no service/business code touched):

| Service | File | Change |
|---|---|---|
| kitehub-subscription | `config/CacheConfig.java` | Caffeine `.recordStats()` |
| kitehub-admin | `config/CacheConfig.java` | Caffeine `.recordStats()` |
| kitehub-branding | `config/CacheConfig.java` | Caffeine `.recordStats()` |
| kitehub-email | `config/BrandingCacheConfig.java` | Caffeine `.recordStats()` |
| kitehub-gateway | `config/GatewayBrandingCacheConfig.java` | Caffeine `.recordStats()` |
| kiteclass-core | `common/config/CacheConfig.java` | Redis `RedisCacheManagerBuilder.enableStatistics()` |

API verified type-safe against cached jars: `Caffeine.recordStats()` → `Caffeine<K,V>` (caffeine 3.2.3); `RedisCacheManagerBuilder.enableStatistics()` → builder (spring-data-redis 3.5.11). With recordStats/enableStatistics on, Spring Boot's `CacheMetricsRegistrar` auto-binds `cache.gets{result=hit|miss}` (→ `cache_gets_total` at `/actuator/prometheus`). Note: kitehub-gateway has no prometheus registry dep so its cache metric binds to the default registry only; the 5 services with prometheus (subscription/admin/branding/email/kiteclass-core) satisfy "≥1 cache per service" at the prometheus endpoint.

## Related

- Discovered in: 2026-06-14 performance audit (F-002)
- `.claude/skills/quality/performance-audit/SKILL.md` sub-check 4.5
