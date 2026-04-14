# GAP-043: Performance — Cache Stampede & Thundering Herd Protection

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Performance / Backend
**Detected:** 2026-04-14 (simulation: cross-cutting × C4)

## Problem

Edge performance cases **không có protection**:

- ❌ Cache stampede: khi cache branding package expire, 1000 concurrent requests → all miss → stampede vào DB
- ❌ Thundering herd: 100 tenants cùng regenerate → worker pool overwhelmed
- ❌ CDN invalidation race: update branding → purge CDN → race với in-flight requests
- ❌ Preview generation storm: nhiều tenants trong wizard step 6 cùng lúc
- ❌ Asset URL signing: sign every request = expensive

## Proposed Fix

### 1. Cache Stampede Protection (Request Coalescing)

```java
@Service
public class BrandingPackageService {
  private final Cache<String, CompletableFuture<BrandingPackage>> inFlight = ...;

  public BrandingPackage getPackage(String tenantId) {
    var cached = cache.get(tenantId);
    if (cached != null && !cached.isStale()) return cached;

    // Coalesce concurrent requests for same key
    var future = inFlight.computeIfAbsent(tenantId, id ->
      CompletableFuture.supplyAsync(() -> fetchFromDb(id))
        .whenComplete((pkg, err) -> {
          inFlight.remove(id);
          if (pkg != null) cache.put(id, pkg);
        })
    );

    return future.get();
  }
}
```

### 2. Stale-While-Revalidate

```
Response headers:
Cache-Control: public, max-age=60, stale-while-revalidate=3600
```

- Client serves stale immediately
- Refresh in background
- Smooth transition when update

### 3. Thundering Herd Protection

Queue with max concurrency:
```java
@Configuration
public class QueueConfig {
  @Bean Semaphore workerPool() {
    return new Semaphore(MAX_CONCURRENT_AI_JOBS);  // e.g., 10
  }
}

// Worker:
public void processJob(BrandingJob job) {
  workerPool.acquire();
  try {
    aiClient.generate(job);
  } finally {
    workerPool.release();
  }
}
```

Back-pressure via queue depth (GAP-005 WFQ).

### 4. CDN Invalidation Strategy

```
Update branding (version N → N+1)
  ↓
Upload new assets với new URL pattern: /{tenantId}/v{N+1}/banner.png
  ↓
Update package API response với new URLs
  ↓
(Old URL still accessible for 5 min for in-flight requests)
  ↓
After 5 min: purge old URLs from CDN
```

URL versioning avoids invalidation race.

### 5. Preview Optimistic Rendering

Wizard step 6 preview:
- Render placeholder instantly (template + colors)
- Background fetch AI-generated headline text
- Swap when ready
- Prevent wait cascades

### 6. Asset URL Pre-Signing

Pre-sign URLs at upload time, store signed URL in DB:
```java
@PostPersist
public void signUrls(BrandingResource resource) {
  resource.setSignedUrl(s3.presignUrl(resource.getUrl(), Duration.ofDays(7)));
}
```

7-day expiry, re-sign background job daily. Avoid per-request signing cost.

### 7. Connection Pooling

- PostgreSQL: HikariCP max 50 conn per service instance
- Redis: lettuce connection pool
- HTTP client: OkHttp with pool

### 8. Load Testing

Benchmark scenarios:
- 1000 concurrent package API reads
- 100 concurrent wizard previews
- 500 concurrent rebrand deployments
- Cache flush storm

Tools: k6, Locust.

## Acceptance Criteria

- [ ] Cache stampede protection implemented (request coalescing)
- [ ] Stale-while-revalidate headers
- [ ] Worker pool semaphore với max concurrency
- [ ] CDN URL versioning (no invalidation race)
- [ ] Preview optimistic rendering
- [ ] Asset URL pre-signing
- [ ] Connection pools configured
- [ ] Load test baseline documented
- [ ] Chaos testing: force cache flush → verify no stampede

## Dependencies

- GAP-005 (queue infrastructure)
- GAP-019 (monitoring) — detect issues

## Log

- 2026-04-14 — Performance edge cases missed in GAP-005 + GAP-019
