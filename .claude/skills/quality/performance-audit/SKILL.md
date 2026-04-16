---
name: performance-audit
description: "Dùng khi user nói 'perf audit', 'performance check', 'load test', 'kiểm tra hiệu năng', 'N+1', 'bundle size', hoặc trước production deploy. Baseline performance metrics /100."
user-invocable: true
---

# /performance-audit — Performance Baseline Assessment

Score /100. Identify bottlenecks across DB, API, frontend, caching, and resources.

## Process

### 1. Automated Checks

```bash
# DB: N+1 query detection
grep -rn "findAll\|findBy" --include="*.java" kiteclass/kiteclass-core/src/main/ | grep -v test | head -30
grep -rn "@Query" --include="*.java" kiteclass/kiteclass-core/src/main/ | head -20

# FE bundle: Next.js build analysis
cd kitehub/kitehub-frontend && npm run build 2>&1 | tail -30
cd kiteclass/kiteclass-frontend && npm run build 2>&1 | tail -30

# Redis caching
grep -rn "RedisTemplate\|@Cacheable\|cache" --include="*.java" kiteclass/ kitehub/ | grep -v test | head -20

# Resource config
grep -rn "pool-size\|max-connections\|timeout\|thread" --include="*.yml" kiteclass/ kitehub/ | head -20
```

### 2. Score 5 Categories

| # | Category (20pts) | Key Checks |
|---|-----------------|------------|
| 1 | **DB Query Efficiency** | N+1 patterns, missing indexes, @Query optimization |
| 2 | **API Response Time** | E2E timing, slow endpoints, pagination |
| 3 | **Frontend Bundle** | Build size, tree-shaking, lazy loading |
| 4 | **Caching Strategy** | Redis usage, TTL config, cache-aside pattern |
| 5 | **Resource Utilization** | Connection pools, thread pools, memory limits |

Scoring details: `reference/scoring-guide.md`

### 3. Output

Save to `documents/04-quality/audits/performance/performance-audit-[date].md`

## Context Management

Token budget ~15-25K (nhỏ nhất trong audit suite). Chú ý:

1. **Build output** — `npm run build` output có thể 100+ lines. LUÔN `| tail -30` (chỉ cần route sizes table).
2. **Grep N+1** — `| head -30` per module. Đếm occurrences, không list tất cả.
3. **Không chạy load test trong audit** — chỉ static analysis + config review. Load test = separate task.

## Gotchas

- `findAll()` on JPA repo without `@Query` or `Pageable` = potential N+1
- Next.js `next build` output shows route sizes — flag anything >250KB
- Redis config is in `application.yml` under `spring.data.redis`
- KiteClass has Resilience4j bulkhead — check thread pool sizes match
- Docker `deploy.resources.limits` may not be set in dev compose — check k8s/Helm for prod
- Build output có thể rất dài — chỉ lấy summary table cuối cùng

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
