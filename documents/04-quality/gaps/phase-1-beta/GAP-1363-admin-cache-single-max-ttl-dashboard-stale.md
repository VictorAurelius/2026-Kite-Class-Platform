# GAP-1363: kitehub-admin cache single-max-TTL → dashboard cache stale 1h

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 4.2)
**Affects:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/config/CacheConfig.java:60-90`

## Problem

`AdminCacheConfig` dùng MỘT `CaffeineCacheManager` với `expireAfterWrite(max(dashboardTtl 300s, revenueTtl 3600s)) = 3600s` cho TẤT CẢ cache (dashboard + revenue + subscriptionByInstance + instanceSummary). Comment thừa nhận per-cache TTL "would require a dedicated CaffeineCacheManager subclass... For baseline GAP-132 we accept single-TTL".

Hệ quả: `ADMIN_DASHBOARD_CACHE` (đáng lẽ 300s) thực tế TTL 3600s → dashboard số liệu stale tới 1h thay vì 5min. KHÔNG phải infinite cache (vẫn bounded) → chỉ sub-optimal freshness → P3.

## Proposed Fix

Dùng per-cache TTL: `CaffeineCacheManager` registerCustomCache(name, individually-built Caffeine) cho dashboard (300s) vs revenue (3600s); hoặc tách 2 cache manager.

## Acceptance Criteria

- [ ] Dashboard cache TTL = 300s (không bị nâng theo revenue)
- [ ] Revenue cache TTL = 3600s độc lập

## Related

- Discovered in: 2026-06-14 performance audit (F-012)
- GAP-132 (precedent — baseline single-TTL accepted)
