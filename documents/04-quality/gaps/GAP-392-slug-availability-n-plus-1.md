# GAP-392: SlugAvailabilityService N+1 — `findAll()` scans entire BrandingJob table mỗi keystroke

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING — Phase 1 launch chặn (production-critical scaling issue)
**Domain:** Backend / Performance
**Found:** 2026-05-07 (Performance /100 audit Wave 34 — agent abeb4c4b)
**Affects:** `kitehub-branding` SlugAvailabilityService — invoked on every wizard slug check

## Problem

`kitehub/kitehub-branding/src/main/java/com/kitehub/branding/wizard/service/SlugAvailabilityService.java:85`:

```java
return jobRepository.findAll().stream()
    .map(job -> normalize(job.getOrganizationName()))
    .anyMatch(normalized::equals);
```

**Vấn đề:**
1. `findAll()` KHÔNG pagination → scans entire `branding_job` table mỗi call
2. `BrandingJob.organization_name` KHÔNG có DB index
3. Endpoint gọi từ FE `WelcomeStep.tsx` qua `useSlugAvailability` hook **mỗi keystroke** (debounce 600ms acknowledged)
4. O(n) scale với job count → 1K jobs = 1K rows network + JVM heap mỗi call

**Impact production:**
- 100 concurrent users × 5 keystrokes/sec = 500 full-table scans/sec
- Heap pressure → GC spikes
- DB CPU saturation
- Latency P95 catastrophic ở >1K jobs

## Root Cause

Wave 34 Bucket A (PR #907 SlugAvailabilityService) ship scaffold v0 dùng `findAll()` cho simplicity. State-check Wave 34 plan time có thể đã miss query optimization audit. Pattern recurrent với `audit-grep-scope` memory — N+1 escapes static review.

## Proposed Fix

### Step 1: SQL-pushdown thay vì in-memory scan

```java
// BrandingJobRepository.java
@Query("SELECT COUNT(j) > 0 FROM BrandingJob j WHERE LOWER(j.organizationName) = LOWER(:slug)")
boolean existsBySlug(@Param("slug") String slug);

// SlugAvailabilityService.java
public boolean isTaken(String slug) {
    String normalized = normalize(slug);
    return jobRepository.existsBySlug(normalized);
}
```

### Step 2: DB index migration V31

```sql
-- V31__index_branding_job_organization_name.sql
CREATE INDEX idx_branding_job_org_name_lower 
  ON branding_job (LOWER(organization_name));
```

### Step 3: Optional Caffeine LRU cache layer

Cache 100-entry, 5min TTL — repeated checks during wizard flow miss DB:

```java
@Cacheable(value="slugAvailability", key="#slug", unless="#result==true")
public boolean isTaken(String slug) { ... }
```

Cache invalidate when new BrandingJob persisted (via `@CacheEvict` trên `save`).

## Acceptance Criteria

- [ ] `BrandingJobRepository.existsBySlug(String)` query method
- [ ] V31 migration: functional index trên `LOWER(organization_name)`
- [ ] `SlugAvailabilityService.isTaken()` refactored — no `findAll()`
- [ ] Optional Caffeine cache layer + `@CacheEvict` on save
- [ ] Unit test: 1000 jobs in DB → `isTaken()` <50ms (vs current ~500ms+ static-extrapolated)
- [ ] Integration test: concurrent 50 calls → no thread starvation
- [ ] Re-run Performance /100 audit delta: 58/100 → ≥65/100

## Related

- Source audit: `documents/04-quality/audits/performance/2026-05-07-wave-33-and-34.md` (Finding #1 P0)
- Parent gap: GAP-272i (slug-availability endpoint — Wave 34 Bucket A)
- Related index gap (P1): GAP-393 — `BrandingJob.status` index missing for poll loop
- Pattern: GAP-126/130 (n+1 query incidents)
- Memory: `feedback_audit_grep_scope.md` — audit must include all submodules

## Log

- **2026-05-07** Filed from Performance /100 audit Wave 34. State-check: 0 existing gaps cover this finding (grep `slug.*findAll|SlugAvailability.*N\\+1` returned 0 matches). Verified hardcoded `findAll()` at line 85.
