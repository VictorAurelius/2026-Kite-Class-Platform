# Performance Audit — Scoring Guide

## Grading Scale

| Score | Grade | Meaning |
|-------|-------|---------|
| 90-100 | A | Production-optimized |
| 80-89 | B | Good — minor optimizations available |
| 70-79 | C | Acceptable — known bottlenecks documented |
| 60-69 | D | Performance risks — address before scale |
| <60 | F | Critical bottlenecks — will fail under load |

---

## Category 1: DB Query Efficiency (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | No N+1, all list queries paginated, indexes on FK/search columns, `@Query` optimized |
| 16 | No N+1, most queries paginated, basic indexes |
| 12 | 1-2 N+1 patterns in non-critical paths, pagination exists |
| 8 | Multiple N+1 patterns, some unbounded queries |
| 4 | Frequent N+1, no pagination strategy |
| 0 | Raw SQL or unbounded findAll() on large tables |

**N+1 Detection Patterns:**
- `findAll()` followed by loop accessing lazy-loaded collection
- `@OneToMany(fetch = LAZY)` accessed in loop without `@EntityGraph` or `JOIN FETCH`
- Repository methods returning `List<Entity>` without `Pageable`

**Index Check:**
- FK columns should have indexes (Flyway migrations)
- Search columns (email, slug, status) should have indexes
- Composite indexes for common query patterns

---

## Category 2: API Response Time (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | All endpoints <200ms p95, load-tested at 100 concurrent |
| 16 | All endpoints <500ms p95, basic load test done |
| 12 | Most endpoints fast, 1-2 slow (identified) |
| 8 | No measurement, but code patterns suggest reasonable |
| 4 | Known slow endpoints, no optimization |
| 0 | Synchronous heavy operations blocking requests |

**Check without load test:**
- Any `@Transactional` spanning external calls?
- AI calls async via queue (not blocking HTTP)?
- Large file operations streamed (not buffered)?
- Pagination on all list endpoints?

---

## Category 3: Frontend Bundle (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | All routes <100KB gzip, code-split, lazy imports, no unused deps |
| 16 | Most routes <150KB, code-split, minor unused deps |
| 12 | Some routes >200KB, basic code splitting |
| 8 | Large bundles >300KB, minimal splitting |
| 4 | Single bundle >500KB |
| 0 | No build optimization |

**Next.js build output interpretation:**
```
Route (app)                      Size     First Load JS
─ /(public)                      5.2 kB   89 kB     ← OK
─ /(customer)/branding/wizard    12 kB    102 kB    ← OK
─ /(customer)/billing            45 kB    180 kB    ← Warning >150KB
```

---

## Category 4: Caching Strategy (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Redis cache-aside on hot paths, TTL configured, invalidation strategy, ETag for FE |
| 16 | Redis configured, basic caching on key endpoints |
| 12 | Redis available, minimal caching implemented |
| 8 | No application cache, relies on DB query cache |
| 4 | Cache exists but no TTL/invalidation = stale data risk |
| 0 | No caching strategy |

**Check:**
- `@Cacheable` annotations on read-heavy services
- `spring.cache.type: redis` in application.yml
- TTL configured per cache name
- Cache invalidation on write operations (`@CacheEvict`)
- Branding package: ETag + conditional GET (ADR reference)

---

## Category 5: Resource Utilization (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Connection pools sized, thread pools configured, memory limits set, health probes correct |
| 16 | Basic pool config, memory limits in Docker/k8s |
| 12 | Default Spring Boot pools, some Docker limits |
| 8 | All defaults, no explicit configuration |
| 4 | Defaults with known issues (pool exhaustion risk) |
| 0 | No resource consideration |

**Key configs:**
- HikariCP: `maximum-pool-size` (default 10 — adequate for dev, may need tuning for prod)
- Resilience4j bulkhead: `maxConcurrentCalls` configured?
- RabbitMQ: `prefetch-count` set?
- Docker: `deploy.resources.limits.memory` in compose/k8s
- JVM: `-Xmx` configured in Dockerfile CMD
