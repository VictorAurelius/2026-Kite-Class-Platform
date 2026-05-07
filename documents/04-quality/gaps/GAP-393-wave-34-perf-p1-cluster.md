# GAP-393: Wave 34 Performance P1 cluster — quota cache + SSE backpressure + status index + idempotency cache

**Status:** 🔵 OPEN
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

- [ ] **393-A**: Quota cache + invalidation; integration test: 2nd `getQuota()` within TTL → 0 DB queries
- [ ] **393-B**: SSE emitter timeout + IOException cleanup + bounded executor; integration test: slow consumer → no heap leak
- [ ] **393-C**: V31 index migration + `EXPLAIN ANALYZE` confirms index hit on `findByStatus`
- [ ] **393-D**: Idempotency local cache; integration test: replay → 1 DB query thay vì 2
- [ ] Re-run Performance /100 audit delta: 58/100 → ≥70/100

## Related

- Source audit: `documents/04-quality/audits/performance/2026-05-07-wave-33-and-34.md` (Findings #2-5)
- Parent: Wave 34 backend cluster (PRs #905-911)
- Sister gap: GAP-392 (N+1 slug findAll P0 — V31 migration may merge với 393-C)
- Pattern: GAP-219 Wave 5 audit followups perf cluster

## Log

- **2026-05-07** Filed from Performance /100 audit Wave 34. State-check: 0 existing gaps cover quota cache / SSE backpressure / status index / idempotency cache (grep returned 0 matches). Bundled per `audit-to-gap-pipeline.md` §3 P1 cluster pattern.
