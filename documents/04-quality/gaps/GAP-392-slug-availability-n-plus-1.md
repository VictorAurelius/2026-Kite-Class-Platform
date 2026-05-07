# GAP-392: SlugAvailabilityService N+1 — `findAll()` scans entire BrandingJob table mỗi keystroke

**Status:** 🟢 DONE 2026-05-07 (Wave 35 Bucket E)
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

- [x] `BrandingJobRepository.existsByOrganizationNameLowercased(String)` query method (named `existsByOrganizationNameLowercased` instead of `existsBySlug` to reflect actual semantics: lowercase comparison against raw `organization_name`)
- [x] V31 migration: functional index trên `LOWER(organization_name)` (`V31__index_branding_job_organization_name_and_status.sql` — also adds `idx_branding_job_status` per GAP-393-C scope)
- [x] `SlugAvailabilityService.isTaken()` refactored — no `findAll()` (verified by `noFindAllInvocation` regression test)
- [x] Unit tests: 5 cases including `noFindAllInvocation` regression guard (100 successive `check()` calls confirm `findAll()` never invoked) — `SlugAvailabilityServiceTest`

## Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Optional Caffeine LRU cache + `@CacheEvict` on save | Future perf-enhancement gap — SQL pushdown + V31 functional index already removes the N+1; cache is incremental optimisation, not blocking. |
| Integration test: concurrent 50 calls → no thread starvation | Tracked under GAP-244 dev-stack restoration — concurrent integration tests need a live Postgres + Flyway-applied schema to be meaningful. Functional index makes contention unlikely at static review. |
| Re-run Performance /100 audit delta: 58/100 → ≥65/100 | Captured by the next post-Wave-35 audit refresh per `post-wave-audit-mandate.md` §2.1 (within 3 days of wave merge). |
| Hyphen-stripping slug-normalisation (e.g. `"Acme Corp" → "acme-corp"` matching) | Requires a stored `slug_normalised` column or pg_trgm — separate schema-evolution gap once a real slug-collision incident surfaces. Current lowercase comparison is the correct best-effort proxy per §isTaken comment block. |

## Related

- Source audit: `documents/04-quality/audits/performance/2026-05-07-wave-33-and-34.md` (Finding #1 P0)
- Parent gap: GAP-272i (slug-availability endpoint — Wave 34 Bucket A)
- Related index gap (P1): GAP-393 — `BrandingJob.status` index missing for poll loop
- Pattern: GAP-126/130 (n+1 query incidents)
- Memory: `feedback_audit_grep_scope.md` — audit must include all submodules

## Log

- **2026-05-07** Filed from Performance /100 audit Wave 34. State-check: 0 existing gaps cover this finding (grep `slug.*findAll|SlugAvailability.*N\\+1` returned 0 matches). Verified hardcoded `findAll()` at line 85.
- **2026-05-07** Wave 35 Bucket E shipped fix. Replaced `findAll().stream().anyMatch(...)` with `existsByOrganizationNameLowercased(slug)` derived query backed by the V31 functional index `idx_branding_job_org_name_lower`. Removed unused private `normalize()` helper. Added 5 unit tests including a 100-iteration regression guard (`noFindAllInvocation`) confirming `findAll()` is never invoked on the slug-availability hot path. V31 also adds `idx_branding_job_status` covering GAP-393-C status-index portion (GAP-393 stays 🟡 PARTIAL — remaining items in Wave 36 Bucket D per plan footer). Verification: `SlugAvailabilityServiceTest` 5/5 green via `./mvnw -pl kitehub-branding test`. Status flip respects `gap-done-discipline.md` §2: every `[ ]` either resolved as `[x]` or moved to §Out-of-scope with explicit deferral pointer.
