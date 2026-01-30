# Performance Testing Standards

**Version:** 1.0.0
**Last Updated:** 2026-01-30
**Status:** Production Standard

---

## Table of Contents

1. [Overview](#overview)
2. [Performance Targets](#performance-targets)
3. [k6 Load Testing](#k6-load-testing)
4. [Database Performance Benchmarks](#database-performance-benchmarks)
5. [Cache Hit Rate Monitoring](#cache-hit-rate-monitoring)
6. [API Rate Limit Handling](#api-rate-limit-handling)
7. [Frontend Performance](#frontend-performance)
8. [Performance Regression Detection](#performance-regression-detection)

---

## Overview

### Purpose

This document defines **performance testing standards** for KiteClass Platform, ensuring:

- **API response times** meet targets (P95 < 500ms)
- **Database queries** are optimized (< 100ms for simple queries)
- **Cache hit rates** are high (≥ 80% for instance configs)
- **Load testing** validates scalability (500+ concurrent users)
- **Rate limits** prevent abuse (100 req/min per instance)

### Performance Testing Pyramid

```
         Load Testing (k6)
       /                    \
  JMH Benchmarks      Lighthouse CI
  /                                  \
Database Query Plans    Bundle Size Analysis
```

---

## Performance Targets

### API Response Time Targets

| Endpoint | P50 | P95 | P99 | Max |
|----------|-----|-----|-----|-----|
| **GET /api/students** | 50ms | 200ms | 500ms | 1s |
| **POST /api/students** | 100ms | 300ms | 700ms | 1.5s |
| **GET /api/instance/config** | 20ms | 50ms | 100ms | 200ms |
| **POST /api/payments/orders** | 200ms | 500ms | 1s | 2s |
| **GET /api/payments/orders/:id/status** | 50ms | 150ms | 300ms | 500ms |
| **POST /api/ai-branding/generate** | 500ms | 1s | 2s | 5s |

### Database Query Targets

| Query Type | Target | Max |
|------------|--------|-----|
| **Simple SELECT** (indexed) | 10ms | 50ms |
| **JOIN (2 tables)** | 20ms | 100ms |
| **Aggregation** | 50ms | 200ms |
| **Full-text search** | 100ms | 500ms |

### Cache Performance Targets

| Cache Type | Hit Rate | Latency |
|------------|----------|---------|
| **Instance Config** (Redis) | ≥ 90% | < 5ms |
| **Feature Flags** (Redis) | ≥ 95% | < 5ms |
| **JWT Token Validation** (Redis) | ≥ 80% | < 5ms |

### Frontend Performance Targets

| Metric | Target |
|--------|--------|
| **First Contentful Paint (FCP)** | < 1.5s |
| **Time to Interactive (TTI)** | < 3s |
| **Largest Contentful Paint (LCP)** | < 2.5s |
| **Cumulative Layout Shift (CLS)** | < 0.1 |
| **JavaScript Bundle Size** | < 500KB (gzipped) |

---

## k6 Load Testing

### k6 Installation

```bash
# macOS
brew install k6

# Linux
sudo apt-get install k6

# Windows
choco install k6
```

### Basic Load Test Script

```javascript
// k6/scripts/student-api-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '3m', target: 50 },   // Stay at 50 users
    { duration: '1m', target: 100 },  // Ramp up to 100 users
    { duration: '3m', target: 100 },  // Stay at 100 users
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // P95 < 500ms, P99 < 1s
    http_req_failed: ['rate<0.01'],                  // Error rate < 1%
    errors: ['rate<0.01'],
  },
};

export default function () {
  const BASE_URL = __ENV.BASE_URL || 'https://staging.kiteclass.com';
  const TOKEN = __ENV.AUTH_TOKEN;

  // GET /api/students
  const getResponse = http.get(`${BASE_URL}/api/students`, {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
  });

  check(getResponse, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'has students array': (r) => JSON.parse(r.body).content !== undefined,
  }) || errorRate.add(1);

  sleep(1); // Think time between requests
}
```

**Run:**
```bash
k6 run k6/scripts/student-api-load-test.js \
  --env BASE_URL=https://staging.kiteclass.com \
  --env AUTH_TOKEN=$AUTH_TOKEN
```

**Expected Output:**
```
     ✓ status is 200
     ✓ response time < 500ms
     ✓ has students array

     http_req_duration..............: avg=245ms  p(95)=450ms p(99)=800ms
     http_req_failed................: 0.15%
     iterations.....................: 15000
     vus............................: 100

✅ All thresholds passed
```

### Payment Flow Load Test

```javascript
// k6/scripts/payment-flow-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 20 },  // Ramp up to 20 concurrent payments
    { duration: '5m', target: 20 },  // Sustain load
    { duration: '1m', target: 0 },   // Ramp down
  ],
  thresholds: {
    'http_req_duration{endpoint:create_order}': ['p(95)<500'],
    'http_req_duration{endpoint:poll_status}': ['p(95)<150'],
    http_req_failed: ['rate<0.005'], // Payment error rate < 0.5%
  },
};

export default function () {
  const BASE_URL = __ENV.BASE_URL;
  const TOKEN = __ENV.AUTH_TOKEN;

  // Step 1: Create payment order
  const createOrderResponse = http.post(
    `${BASE_URL}/api/payments/orders`,
    JSON.stringify({ tier: 'STANDARD' }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${TOKEN}`,
      },
      tags: { endpoint: 'create_order' },
    }
  );

  check(createOrderResponse, {
    'order created': (r) => r.status === 201,
    'has QR code URL': (r) => JSON.parse(r.body).qrCodeUrl !== undefined,
  });

  const orderId = JSON.parse(createOrderResponse.body).orderId;

  // Step 2: Poll payment status (simulate user waiting)
  for (let i = 0; i < 10; i++) {
    sleep(5); // Poll every 5 seconds

    const statusResponse = http.get(
      `${BASE_URL}/api/payments/orders/${orderId}/status`,
      {
        headers: { 'Authorization': `Bearer ${TOKEN}` },
        tags: { endpoint: 'poll_status' },
      }
    );

    const status = JSON.parse(statusResponse.body).status;

    if (status === 'PAID' || status === 'FAILED') {
      break;
    }
  }
}
```

### Multi-Tenant Load Test

```javascript
// k6/scripts/multi-tenant-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const instances = [
  { id: 'tenant-1', token: __ENV.TOKEN_1 },
  { id: 'tenant-2', token: __ENV.TOKEN_2 },
  { id: 'tenant-3', token: __ENV.TOKEN_3 },
];

export const options = {
  vus: 30, // 10 VUs per tenant
  duration: '5m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    'checks{tenant:tenant-1}': ['rate>0.99'],
    'checks{tenant:tenant-2}': ['rate>0.99'],
    'checks{tenant:tenant-3}': ['rate>0.99'],
  },
};

export default function () {
  const instance = instances[__VU % instances.length];

  const response = http.get(
    `https://${instance.id}.kiteclass.com/api/students`,
    {
      headers: { 'Authorization': `Bearer ${instance.token}` },
      tags: { tenant: instance.id },
    }
  );

  check(response, {
    'status is 200': (r) => r.status === 200,
    'tenant isolation verified': (r) => {
      const body = JSON.parse(r.body);
      // Verify only current tenant's students returned
      return body.content.every(s => s.instanceId === instance.id);
    },
  }, { tenant: instance.id });

  sleep(1);
}
```

---

## Database Performance Benchmarks

### Query Execution Plan Analysis

```sql
-- Check if indexes are being used
EXPLAIN ANALYZE
SELECT *
FROM students
WHERE instance_id = '123e4567-e89b-12d3-a456-426614174000'
  AND email = 'student@school.com';

-- Expected:
-- Index Scan using idx_students_instance_email on students
-- Planning Time: 0.123 ms
-- Execution Time: 2.456 ms
```

**Good Plan:**
- Uses index scan (not sequential scan)
- Execution time < 10ms for simple queries
- Planning time < 1ms

**Bad Plan (needs optimization):**
```
Seq Scan on students
  Filter: (instance_id = '...' AND email = '...')
Planning Time: 0.500 ms
Execution Time: 245.678 ms  ❌ Too slow!
```

**Fix:** Add composite index
```sql
CREATE INDEX idx_students_instance_email
ON students(instance_id, email);
```

### JMeter Database Benchmark

```xml
<!-- jmeter/database-benchmark.jmx -->
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">50</stringProp>
  <stringProp name="ThreadGroup.ramp_time">10</stringProp>
  <stringProp name="ThreadGroup.duration">60</stringProp>

  <JDBCSampler>
    <stringProp name="query">
      SELECT * FROM students
      WHERE instance_id = ?
      ORDER BY created_at DESC
      LIMIT 20
    </stringProp>
    <stringProp name="queryArguments">${instanceId}</stringProp>
  </JDBCSampler>

  <ResultCollector>
    <stringProp name="assertion">responseTime &lt; 100</stringProp>
  </ResultCollector>
</ThreadGroup>
```

**Run:**
```bash
jmeter -n -t jmeter/database-benchmark.jmx -l results.jtl
```

### Spring Boot JPA Query Performance

```java
// src/test/java/com/kiteclass/performance/StudentRepositoryPerformanceTest.java
@SpringBootTest
@ActiveProfiles("performance")
public class StudentRepositoryPerformanceTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void getStudentsByInstanceId_shouldCompleteIn100ms() {
        UUID instanceId = UUID.randomUUID();

        // Seed 1000 students
        for (int i = 0; i < 1000; i++) {
            studentRepository.save(Student.builder()
                .instanceId(instanceId)
                .name("Student " + i)
                .email("student" + i + "@school.com")
                .build());
        }

        // Benchmark query
        long startTime = System.nanoTime();

        Page<Student> students = studentRepository.findByInstanceId(
            instanceId,
            PageRequest.of(0, 20)
        );

        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        assertThat(students.getContent()).hasSize(20);
        assertThat(durationMs).isLessThan(100L); // < 100ms
    }

    @Test
    void findStudentByEmail_shouldUseIndex() {
        // Enable query logging
        System.setProperty("spring.jpa.show-sql", "true");

        studentRepository.findByEmail("student@school.com");

        // Verify index usage in logs:
        // select ... from students where email=? limit 1
        // Uses: idx_students_email
    }
}
```

---

## Cache Hit Rate Monitoring

### Redis Cache Performance

```java
// src/main/java/com/kiteclass/cache/CacheMetrics.java
@Component
public class CacheMetrics {

    private final MeterRegistry meterRegistry;

    @Autowired
    public CacheMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordCacheHit(String cacheName) {
        meterRegistry.counter("cache.hits", "cache", cacheName).increment();
    }

    public void recordCacheMiss(String cacheName) {
        meterRegistry.counter("cache.misses", "cache", cacheName).increment();
    }

    public double getCacheHitRate(String cacheName) {
        double hits = meterRegistry.counter("cache.hits", "cache", cacheName).count();
        double misses = meterRegistry.counter("cache.misses", "cache", cacheName).count();

        if (hits + misses == 0) return 0.0;

        return hits / (hits + misses);
    }
}
```

**Usage:**
```java
@Service
public class InstanceConfigService {

    @Autowired
    private CacheMetrics cacheMetrics;

    @Cacheable(value = "instanceConfig", key = "#instanceId")
    public InstanceConfig getConfig(UUID instanceId) {
        cacheMetrics.recordCacheMiss("instanceConfig");

        // Fetch from database
        return instanceConfigRepository.findByInstanceId(instanceId)
            .orElseThrow(() -> new InstanceNotFoundException(instanceId));
    }

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object trackCacheHits(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result != null) {
            cacheMetrics.recordCacheHit("instanceConfig");
        }

        return result;
    }
}
```

### Prometheus Cache Metrics

```yaml
# Prometheus query
rate(cache_hits_total{cache="instanceConfig"}[5m]) /
(rate(cache_hits_total{cache="instanceConfig"}[5m]) +
 rate(cache_misses_total{cache="instanceConfig"}[5m]))

# Expected: ≥ 0.90 (90% hit rate)
```

**Alert:**
```yaml
- alert: CacheHitRateLow
  expr: |
    rate(cache_hits_total[5m]) /
    (rate(cache_hits_total[5m]) + rate(cache_misses_total[5m])) < 0.80
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "Cache hit rate below 80%"
    description: "Cache: {{ $labels.cache }}, Hit rate: {{ $value }}"
```

### Cache Warm-Up Strategy

```java
@Component
public class CacheWarmUpRunner implements ApplicationRunner {

    @Autowired
    private InstanceConfigRepository instanceConfigRepository;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Warming up instance config cache...");

        List<InstanceConfig> configs = instanceConfigRepository.findAll();

        Cache cache = cacheManager.getCache("instanceConfig");

        for (InstanceConfig config : configs) {
            cache.put(config.getInstanceId(), config);
        }

        logger.info("Cache warmed up: {} instances", configs.size());
    }
}
```

---

## API Rate Limit Handling

### Rate Limit Configuration

```java
// src/main/java/com/kiteclass/config/RateLimitConfig.java
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter instanceRateLimiter() {
        return RateLimiter.of("instanceApi", RateLimiterConfig.custom()
            .limitForPeriod(100)              // 100 requests
            .limitRefreshPeriod(Duration.ofMinutes(1)) // per 1 minute
            .timeoutDuration(Duration.ofSeconds(5))    // wait max 5s
            .build());
    }

    @Bean
    public RateLimiter paymentRateLimiter() {
        return RateLimiter.of("paymentApi", RateLimiterConfig.custom()
            .limitForPeriod(10)               // 10 payment requests
            .limitRefreshPeriod(Duration.ofMinutes(1)) // per 1 minute
            .timeoutDuration(Duration.ofSeconds(10))
            .build());
    }
}
```

**Usage:**
```java
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private RateLimiter instanceRateLimiter;

    @GetMapping
    @RateLimiter(name = "instanceApi")
    public ResponseEntity<Page<StudentResponse>> getStudents(Pageable pageable) {
        // Rate limited to 100 req/min per instance
        return ResponseEntity.ok(studentService.getStudents(pageable));
    }
}
```

### Rate Limit Testing

```javascript
// k6/scripts/rate-limit-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  duration: '2m',
};

export default function () {
  const responses = [];

  // Send 150 requests in 60 seconds (exceeds 100 req/min limit)
  for (let i = 0; i < 150; i++) {
    const response = http.get('https://staging.kiteclass.com/api/students', {
      headers: { 'Authorization': `Bearer ${__ENV.TOKEN}` },
    });

    responses.push(response.status);

    sleep(0.4); // 150 requests in 60 seconds
  }

  // Count how many were rate limited
  const rateLimited = responses.filter(status => status === 429).length;

  check({ rateLimited }, {
    'rate limiter activated': (data) => data.rateLimited >= 50, // ~50 requests blocked
  });
}
```

**Expected:**
- First 100 requests: 200 OK
- Remaining 50 requests: 429 Too Many Requests

**Response Headers:**
```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1706605200
Retry-After: 45
```

---

## Frontend Performance

### Bundle Size Analysis

```bash
# Analyze bundle size
npm run build -- --analyze

# Expected output:
# ✓ Compiled in 12.5s
#
# Route (app)                Size     First Load JS
# ┌ ○ /                     145 KB        320 KB
# ├ ○ /dashboard            85 KB         260 KB
# ├ ○ /students             92 KB         267 KB
# └ ○ /settings/pricing     78 KB         253 KB
#
# ○ (Static) prerendered as static HTML
```

**Target:** < 500 KB per route

### Lighthouse CI Performance

```json
// .lighthouserc.json
{
  "ci": {
    "collect": {
      "numberOfRuns": 3,
      "url": [
        "http://localhost:3000",
        "http://localhost:3000/dashboard",
        "http://localhost:3000/students"
      ]
    },
    "assert": {
      "assertions": {
        "categories:performance": ["error", { "minScore": 0.8 }],
        "first-contentful-paint": ["error", { "maxNumericValue": 1500 }],
        "interactive": ["error", { "maxNumericValue": 3000 }],
        "largest-contentful-paint": ["error", { "maxNumericValue": 2500 }],
        "cumulative-layout-shift": ["error", { "maxNumericValue": 0.1 }]
      }
    },
    "upload": {
      "target": "temporary-public-storage"
    }
  }
}
```

**Run:**
```bash
npm run lighthouse
```

**Expected:**
```
✅ Performance Score: 92/100
✅ First Contentful Paint: 1.2s (target: < 1.5s)
✅ Time to Interactive: 2.7s (target: < 3s)
✅ Largest Contentful Paint: 2.1s (target: < 2.5s)
✅ Cumulative Layout Shift: 0.05 (target: < 0.1)
```

### React Component Profiling

```typescript
// components/students/StudentList.tsx
import { Profiler, ProfilerOnRenderCallback } from 'react';

const onRender: ProfilerOnRenderCallback = (
  id,
  phase,
  actualDuration,
  baseDuration,
  startTime,
  commitTime
) => {
  if (actualDuration > 100) {
    console.warn(`Slow render: ${id} took ${actualDuration}ms`);
  }
};

export function StudentList() {
  return (
    <Profiler id="StudentList" onRender={onRender}>
      {/* Component content */}
    </Profiler>
  );
}
```

---

## Performance Regression Detection

### Continuous Benchmarking

```yaml
# .github/workflows/performance-benchmark.yml
name: Performance Benchmark

on:
  pull_request:
  push:
    branches: [main]

jobs:
  backend-benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run JMH Benchmarks
        run: |
          cd kiteclass/kiteclass-backend
          mvn jmh:benchmark

      - name: Compare with Baseline
        run: |
          python scripts/compare-benchmarks.py \
            --current target/jmh-result.json \
            --baseline benchmarks/baseline.json \
            --threshold 10

  frontend-benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run Lighthouse CI
        uses: treosh/lighthouse-ci-action@v10
        with:
          urls: http://localhost:3000
          configPath: .lighthouserc.json

  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run k6 Load Test
        run: |
          k6 run k6/scripts/student-api-load-test.js \
            --env BASE_URL=https://staging.kiteclass.com \
            --out json=k6-results.json

      - name: Check Thresholds
        run: |
          if grep -q "✗" k6-results.json; then
            echo "❌ Performance regression detected"
            exit 1
          fi
```

---

## Best Practices Summary

### DO ✅

1. **Set performance targets** for all critical endpoints
2. **Run load tests** before deploying to production
3. **Monitor cache hit rates** (target: ≥ 80%)
4. **Use database indexes** for all query filters
5. **Analyze query execution plans** for slow queries
6. **Implement rate limiting** to prevent abuse
7. **Track bundle sizes** (target: < 500KB)
8. **Use Lighthouse CI** to catch regressions
9. **Benchmark database queries** with JMeter/k6
10. **Profile React components** to find slow renders

### DON'T ❌

1. **Don't skip load testing** for payment flows
2. **Don't ignore slow queries** (> 100ms)
3. **Don't fetch unbounded data** (always use pagination)
4. **Don't skip cache warming** after deployment
5. **Don't disable rate limiting** in production
6. **Don't ignore bundle size growth** (track over time)
7. **Don't skip performance tests** in CI/CD
8. **Don't optimize prematurely** (measure first)

---

**Document Version:** 1.0.0
**Last Updated:** 2026-01-30
**Next Review:** 2026-02-28
