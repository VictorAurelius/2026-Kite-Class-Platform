# GAP-393: Wave 34 Performance P1 cluster — quota cache + SSE backpressure + status index + idempotency cache

**Status:** 🟢 DONE 2026-05-07 (Wave 36 Bucket D — 393-A quota cache + 393-B SSE backpressure + 393-D idempotency cache shipped; 393-C status index already DONE Wave 35 Bucket E)
**Priority:** 🟠 P1 cluster (4 sub-issues — performance hardening; ship sau P0 GAP-392)
**Domain:** Backend / Performance
**Found:** 2026-05-07 (Performance /100 audit Wave 34 — agent abeb4c4b)
**Affects:** `kitehub-branding` RegenerateQuotaService + DeployStreamController + BrandingJobRepository

## Problem (4 sub-issues)

### 393-A: RegenerateQuotaService.getQuota() no cache
- `RegenerateQuotaService.java:72` — hits DB `findByUserIdAndWindowStart` mỗi call
- FE wizard fetch quota nhiều lần (Step 5 template select + Step 6 preview)
- Có thể serve từ Caffeine cache với 1min TTL

### 393-B: SSE deploy-stream poller no backpressure
- `DeployStreamController.java:119, 133` — `@Scheduled` poller 2s interval
- `emitter.send()` không có write timeout
- Slow FE network → emitter sends pile up trong heap
- Thread không có concurrency limit

### 393-C: Missing index `BrandingJob.status`
- `BrandingJobRepository.findByStatus()` line 43 — used in deploy-stream poller
- Status enum 6 values (NOT_STARTED/INITIALIZING/GENERATING/DEPLOYED/REGENERATING/FAILED)
- Frequent filter without index → seq scan

### 393-D: Idempotency replay sync 2-query latency
- `RegenerateQuotaService.java:93-98` — 2 DB queries back-to-back trên regenerate path
- Idempotency hash check + job fetch
- Local Caffeine 10min window cache có thể save 1 query

## Proposed Fix

### 393-A
```java
@Cacheable(value="regenerateQuota", key="#userId + '-' + #windowStart", unless="#result == null")
public RegenerateQuota getQuota(UUID userId, OffsetDateTime windowStart) { ... }

@CacheEvict(value="regenerateQuota", key="#usage.userId + '-' + #usage.windowStart")
public void recordUsage(RegenerateUsage usage) { ... }
```

### 393-B
```java
private void emitToSubscriber(SseEmitter emitter, Object event) {
    try {
        emitter.send(SseEmitter.event().data(event).build());
    } catch (IOException ex) {
        log.warn("SSE emitter failed, removing: {}", ex.getMessage());
        removeEmitter(emitter);
    } catch (Exception ex) {
        emitter.completeWithError(ex);
        removeEmitter(emitter);
    }
}
```

Plus add `@Async` với `ThreadPoolTaskExecutor` bounded queue cho poller.

### 393-C
V31 migration (gộp với GAP-392 V31 nếu chưa apply):
```sql
CREATE INDEX idx_branding_job_status ON branding_job(status);
```

### 393-D
```java
private final Cache<String, BrandingRegenerateUsage> idempotencyCache = 
    Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1000).build();

public BrandingJob regenerate(...) {
    BrandingRegenerateUsage cached = idempotencyCache.getIfPresent(idempotencyHash);
    if (cached != null) return jobRepository.findById(cached.getJobId()).orElseThrow();
    // ... existing path
}
```

## Acceptance Criteria

- [x] **393-A**: Quota cache + invalidation shipped via `RegenerateQuotaService.getQuota()` `@Cacheable("regenerateQuota")` keyed by `userId+'_'+tier` + `@CacheEvict` on `regenerate()` path. Cache wired in `CacheConfig.REGENERATE_QUOTA_CACHE` (Caffeine, max-TTL bound by `kitehub.branding.cache.*-ttl-seconds`). Verification: 9 unit tests pass (RegenerateQuotaServiceTest). Spring proxy-based caching exercised by Spring boot context test.
- [x] **393-B**: SSE emitter cleanup + backpressure cap shipped via `DeployStreamController` — IOException **and** IllegalStateException now removeEmitter (poller + heartbeat); per-job subscriber cap `kitehub.branding.deploy-stream.max-emitters-per-job:20` (env override `BRANDING_DEPLOY_STREAM_MAX_EMITTERS`). New `safeComplete()` helper avoids double-completion throws. Verification: 7 unit tests pass (DeployStreamControllerTest) including new `backpressureCap` + `heartbeatIOExceptionEvictsEmitter` regression guards.
- [x] **393-C**: V31 index migration shipped via Wave 35 Bucket E (`V31__index_branding_job_organization_name_and_status.sql` adds `idx_branding_job_status`). `EXPLAIN ANALYZE` verification deferred until GAP-244 dev-stack lands.
- [x] **393-D**: Idempotency local Caffeine cache shipped — `RegenerateQuotaService.idempotencyCache` (10-min TTL, max-size 10000), keyed by `userId+":"+idempotencyKey`. Replay path checks cache first (saves 2 DB queries: usage lookup + job fetch); cache seeded on fresh regenerate. Verification: `idempotencyCacheServesReplay` test asserts only 1 `findByUserIdAndIdempotencyKey` call across 2 regenerates with same key.
- [x] Verification artifact pointer: `cd kitehub && ./mvnw -pl kitehub-branding test` → 229 tests, 0 failures, 0 errors (Wave 36 Bucket D PR). Performance /100 re-audit deferred to Wave 36 closure post-merge audit.

## Related

- Source audit: `documents/04-quality/audits/performance/2026-05-07-wave-33-and-34.md` (Findings #2-5)
- Parent: Wave 34 backend cluster (PRs #905-911)
- Sister gap: GAP-392 (N+1 slug findAll P0 — V31 migration may merge với 393-C)
- Pattern: GAP-219 Wave 5 audit followups perf cluster

## Log

- **2026-05-07** Filed from Performance /100 audit Wave 34. State-check: 0 existing gaps cover quota cache / SSE backpressure / status index / idempotency cache (grep returned 0 matches). Bundled per `audit-to-gap-pipeline.md` §3 P1 cluster pattern.
- **2026-05-07** 393-C status-index portion shipped via Wave 35 Bucket E alongside GAP-392 (`idx_branding_job_status` added in `V31__index_branding_job_organization_name_and_status.sql`). Status flipped 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 — remaining 393-A (quota cache), 393-B (SSE backpressure), 393-D (idempotency cache) scheduled Wave 36 Bucket D per Wave 35 plan footer.
- **2026-05-07** Wave 36 Bucket D shipped: 393-A `@Cacheable`/`@CacheEvict` on `RegenerateQuotaService.getQuota()`/`regenerate()` + 2 new cache names in `CacheConfig` (`REGENERATE_QUOTA_CACHE`, `REGENERATE_IDEMPOTENCY_CACHE`); 393-B `safeComplete()` + `removeEmitter` on IOException/IllegalStateException in poller+heartbeat + per-job subscriber cap `max-emitters-per-job:20`; 393-D local Caffeine 10-min idempotency cache short-circuiting the 2-query replay path. New tests: 4 unit (idempotencyCacheServesReplay, idempotencyCacheSkippedWhenKeyNull, backpressureCap, heartbeatIOExceptionEvictsEmitter). Local verify clean: `mvn -pl kitehub-branding test` → 229 tests pass. Status flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 (all ACs checked + verification artifact cited).
