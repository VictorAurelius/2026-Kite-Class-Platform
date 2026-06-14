# GAP-1357: Cache hit-ratio metric không emit qua Micrometer (toàn fleet)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 4.5)
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

- [ ] Caffeine cache builder có `.recordStats()`
- [ ] `/actuator/prometheus` expose `cache_gets_total` cho ≥1 cache mỗi service
- [ ] Dashboard/alert reference cache hit-ratio (hoặc gap follow-up cho dashboard)

## Related

- Discovered in: 2026-06-14 performance audit (F-002)
- `.claude/skills/quality/performance-audit/SKILL.md` sub-check 4.5
