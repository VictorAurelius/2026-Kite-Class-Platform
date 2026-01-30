---
name: Phase 4 - Performance PR
about: Template cho Performance Testing & Deployment PRs (Phase 4)
title: '[PERF/DEPLOY] PR 4.X: <Title>'
labels: performance, deployment, phase-4, medium
assignees: ''
---

## PR Information

**PR Number:** PR 4.X (từ code-review-pr-plan.md)
**Phase:** Phase 4 - Performance & Deployment
**Priority:** 🟢 P2 - MEDIUM
**Type:**
- [ ] Performance Testing (k6, Lighthouse)
- [ ] Deployment Documentation

**Estimated Effort:** X days
**Assignee:** @username

---

## Performance Testing Scope (if applicable)

### Backend Performance

- [ ] k6 load tests for API endpoints
- [ ] Database query performance benchmarks
- [ ] Cache hit rate monitoring
- [ ] API rate limiting tests

**Performance Targets:**
- P95 response time: < 500ms
- P99 response time: < 1s
- Error rate: < 1%
- Database queries: < 100ms
- Cache hit rate: ≥ 80%

### Frontend Performance

- [ ] Lighthouse CI setup
- [ ] Bundle size analysis
- [ ] Core Web Vitals optimization
- [ ] Performance regression detection

**Performance Targets:**
- Performance Score: ≥ 80
- FCP (First Contentful Paint): < 1.5s
- LCP (Largest Contentful Paint): < 2.5s
- TTI (Time to Interactive): < 3s
- CLS (Cumulative Layout Shift): < 0.1
- Bundle size: < 500KB (gzipped)

---

## Deployment Documentation Scope (if applicable)

### Documentation to Create

- [ ] Deployment runbook
- [ ] Rollback procedures (< 5 minutes)
- [ ] Database migration checklist
- [ ] Zero-downtime deployment guide
- [ ] Feature flag management guide
- [ ] Monitoring & alerting setup

---

## Implementation Checklist

### k6 Load Testing (Backend)

- [ ] k6 installed locally
- [ ] Load test scripts created
- [ ] Test scenarios defined
- [ ] Baseline performance captured
- [ ] Performance targets validated

### Lighthouse CI (Frontend)

- [ ] Lighthouse CI configured
- [ ] Performance budgets defined
- [ ] CI integration completed
- [ ] Performance baseline established

### Deployment Docs

- [ ] Runbook template created
- [ ] All deployment steps documented
- [ ] Rollback procedures tested
- [ ] Checklists created
- [ ] Team training completed

---

## Performance Test Examples

### k6 Load Test Script

```javascript
// k6/scripts/student-api-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 50 },   // Stay at 50 users
    { duration: '1m', target: 100 },  // Ramp up to 100 users
    { duration: '5m', target: 100 },  // Stay at 100 users
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // P95 < 500ms
    http_req_failed: ['rate<0.01'],                  // Error rate < 1%
  },
};

export default function () {
  const BASE_URL = __ENV.BASE_URL || 'https://staging.kiteclass.com';
  const TOKEN = __ENV.AUTH_TOKEN;

  const res = http.get(`${BASE_URL}/api/students`, {
    headers: { 'Authorization': `Bearer ${TOKEN}` },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
```

**Run:**
```bash
k6 run k6/scripts/student-api-load-test.js \
  --env BASE_URL=https://staging.kiteclass.com \
  --env AUTH_TOKEN=$AUTH_TOKEN
```

### Lighthouse CI Config

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
    }
  }
}
```

---

## Performance Benchmarks

### Backend Performance Results

**Before Optimization:**
```
P50: XXXms
P95: XXXms
P99: XXXms
Error Rate: X.XX%
```

**After Optimization:**
```
P50: XXXms (target: < 200ms) ✅
P95: XXXms (target: < 500ms) ✅
P99: XXXms (target: < 1s) ✅
Error Rate: X.XX% (target: < 1%) ✅
```

### Frontend Performance Results

**Lighthouse Scores:**
```
Performance: XX/100 (target: ≥80) ✅
Accessibility: XX/100 (target: ≥90) ✅
Best Practices: XX/100 (target: ≥90) ✅
SEO: XX/100 (target: ≥90) ✅

Core Web Vitals:
- FCP: X.Xs (target: < 1.5s) ✅
- LCP: X.Xs (target: < 2.5s) ✅
- TTI: X.Xs (target: < 3s) ✅
- CLS: X.XX (target: < 0.1) ✅
```

### Database Performance

```sql
-- Query: Get students by instance
EXPLAIN ANALYZE SELECT * FROM students
WHERE instance_id = '...' AND deleted_at IS NULL;

-- Results:
Planning Time: 0.123 ms ✅
Execution Time: 45.678 ms ✅ (target: < 100ms)
Index Used: idx_students_instance_id ✅
```

---

## Deployment Documentation Template

### Deployment Runbook

```markdown
# Deployment Runbook

## Pre-Deployment Checklist
- [ ] All CI/CD checks passing
- [ ] Database migrations tested
- [ ] Environment variables updated
- [ ] Feature flags configured
- [ ] Rollback plan documented

## Deployment Steps

### 1. Database Migration
```bash
flyway migrate -configFiles=flyway.conf
```

### 2. Backend Deployment
```bash
kubectl apply -f k8s/backend-deployment.yml
kubectl rollout status deployment/kiteclass-backend
```

### 3. Frontend Deployment
```bash
kubectl apply -f k8s/frontend-deployment.yml
kubectl rollout status deployment/kiteclass-frontend
```

### 4. Smoke Tests
```bash
npm run test:smoke -- --env=production
```

## Rollback Procedures (< 5 minutes)

### Option 1: Kubernetes Rollback
```bash
kubectl rollout undo deployment/kiteclass-backend
kubectl rollout undo deployment/kiteclass-frontend
```

### Option 2: Feature Flag Disable
```bash
launchdarkly update-flag new-feature --enabled false
```
```

---

## Performance Testing Reference

### Backend Performance
- **Standards:** `.claude/skills/performance-testing-standards.md`
- **Tools:** k6, JMeter, JMH
- **Targets:** P95 < 500ms, Error rate < 1%

### Frontend Performance
- **Standards:** `.claude/skills/performance-testing-standards.md`
- **Tools:** Lighthouse CI, WebPageTest
- **Targets:** Performance Score ≥ 80, Bundle < 500KB

### Deployment
- **Standards:** `.claude/skills/deployment-quality-standards.md`
- **Strategies:** Blue-Green, Rolling Updates
- **Rollback:** < 5 minutes

---

## Test Execution

### Run Performance Tests

**Backend (k6):**
```bash
# Local load test
k6 run k6/scripts/student-api-load-test.js

# Load test with monitoring
k6 run k6/scripts/student-api-load-test.js \
  --out influxdb=http://localhost:8086/k6
```

**Frontend (Lighthouse):**
```bash
# Run Lighthouse CI
npm run lighthouse

# Run specific URL
lighthouse http://localhost:3000 \
  --output=html \
  --output-path=./lighthouse-report.html
```

**Database (EXPLAIN ANALYZE):**
```bash
# Run query performance tests
psql -U kiteclass -d kiteclass_db \
  -f scripts/performance-tests.sql
```

---

## Review Criteria

### Performance Tests

- [ ] All performance targets met
- [ ] Load tests stable (no flakiness)
- [ ] Performance regression detected
- [ ] Optimization recommendations documented
- [ ] Monitoring alerts configured

### Deployment Docs

- [ ] Runbook complete and tested
- [ ] Rollback procedures validated
- [ ] All checklists comprehensive
- [ ] Team trained on procedures
- [ ] Documentation up-to-date

---

## Merge Criteria

### Performance PRs
- [ ] Performance targets achieved
- [ ] Load tests passing
- [ ] No performance regressions
- [ ] Monitoring configured
- [ ] Code review approved

### Deployment PRs
- [ ] Documentation complete
- [ ] Procedures tested
- [ ] Team sign-off
- [ ] Rollback tested

---

## Post-Merge Actions

- [ ] Update performance dashboard
- [ ] Configure performance alerts
- [ ] Share deployment runbook with team
- [ ] Schedule deployment training

---

**Reference Documents:**
- `.claude/skills/performance-testing-standards.md`
- `.claude/skills/deployment-quality-standards.md`
- `documents/plans/code-review-pr-plan.md` (Phase 4)
