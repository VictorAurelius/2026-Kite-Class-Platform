# PR Implementation Quick Reference Guide

**Version:** 1.0
**Date:** 2026-01-30
**Purpose:** Quick reference cho developers khi implement PRs

---

## 📌 TL;DR - Quick Start

1. **Pick a PR** từ `documents/plans/code-review-pr-plan.md`
2. **Create issue** từ GitHub Issue Template (`.github/ISSUE_TEMPLATE/`)
3. **Create branch** theo naming convention
4. **Implement** theo checklist trong issue
5. **Create PR** với template (`.github/PULL_REQUEST_TEMPLATE.md`)
6. **Pass all checks** (tests, coverage, security, review)
7. **Merge** khi tất cả criteria đạt

---

## 🔄 PR Workflow

```
1. PICK PR
   ↓
2. CREATE ISSUE (từ template)
   ↓
3. CREATE BRANCH
   git checkout -b security/multi-tenant-repositories
   ↓
4. IMPLEMENT
   - Code changes
   - Write tests
   - Update docs
   ↓
5. TEST LOCALLY
   ./mvnw test           # Backend
   npm run test          # Frontend
   ↓
6. COMMIT
   git commit -m "security(multi-tenant): add tenant filtering to repositories"
   ↓
7. PUSH & CREATE PR
   git push -u origin security/multi-tenant-repositories
   ↓
8. PASS CI/CD CHECKS
   - Tests passing
   - Coverage ≥80%
   - Security scan clean
   ↓
9. CODE REVIEW
   - ≥1 approval required
   - Security team (for Phase 1)
   ↓
10. MERGE
    Squash merge to develop
```

---

## 📂 Quick Reference by Phase

### PHASE 1: Security Review (Week 1-2)

**Documents to Read:**
- `.claude/skills/security-testing-standards.md`
- `.claude/skills/development-workflow.md` (KiteClass checklist)

**Branch Naming:**
```
security/multi-tenant-repositories
security/multi-tenant-services
security/jwt-validation
security/payment-validation
security/feature-detection
test/security-tests
```

**Must Have:**
- [ ] Security tests (≥5 tests per vulnerability)
- [ ] Cross-tenant access tests
- [ ] OWASP Top 10 tests
- [ ] Security team approval

**Common Patterns:**

```java
// Multi-tenant repository
@TenantScoped
public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query("SELECT s FROM Student s WHERE s.instanceId = :instanceId")
    Page<Student> findByInstanceId(@Param("instanceId") UUID instanceId, Pageable p);
}

// Cross-tenant test
@Test
void shouldPreventCrossTenantAccess() {
    UUID tenant1 = UUID.randomUUID();
    UUID tenant2 = UUID.randomUUID();

    setTenantContext(tenant1);
    Student s1 = repo.save(createStudent());

    setTenantContext(tenant2);
    Optional<Student> result = repo.findById(s1.getId());

    assertThat(result).isEmpty(); // Blocked
}
```

---

### PHASE 2: Testing Implementation (Week 3-4)

**Documents to Read:**
- `.claude/skills/kiteclass-backend-testing-patterns.md`
- `.claude/skills/kiteclass-frontend-testing-patterns.md`
- `documents/testing/integration-testing-strategy.md`

**Branch Naming:**
```
test/backend-unit-core
test/backend-unit-payment
test/backend-integration
test/frontend-components-core
test/frontend-components-payment
test/frontend-integration
```

**Must Have:**
- [ ] Coverage ≥80%
- [ ] Happy path + edge cases + error scenarios
- [ ] Integration tests with Testcontainers

**Common Patterns:**

```java
// Backend unit test
@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {
    @Mock private StudentRepository repo;
    @Mock private StudentMapper mapper;
    @InjectMocks private StudentServiceImpl service;

    @Test
    void createStudent_shouldValidateEmail() {
        when(repo.existsByEmailAndInstanceId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.createStudent(createRequest()))
            .isInstanceOf(DuplicateEmailException.class);
    }
}

// Backend integration test
@SpringBootTest
@Testcontainers
class PaymentFlowIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void paymentFlow_shouldUpgradeTier() {
        // Full flow test
    }
}
```

```typescript
// Frontend component test
import { render, screen } from '@testing-library/react';
import { FeatureGate } from './FeatureGate';

describe('FeatureGate', () => {
  it('should show content for available features', async () => {
    server.use(
      http.get('/api/instance/config', () => {
        return HttpResponse.json({ tier: 'STANDARD', features: { ENGAGEMENT: true } });
      })
    );

    render(<FeatureGate feature="ENGAGEMENT"><div>Content</div></FeatureGate>);

    expect(await screen.findByText('Content')).toBeInTheDocument();
  });
});
```

---

### PHASE 3: E2E & CI/CD (Week 5-6)

**Documents to Read:**
- `.claude/skills/e2e-testing-standards.md`
- `.claude/skills/ci-cd-quality-enforcement.md`

**Branch Naming:**
```
test/e2e-guest-trial
test/e2e-payment-features
test/e2e-multi-tenant
cicd/test-automation
cicd/security-scanning
cicd/quality-gates
```

**Must Have:**
- [ ] ≥50 E2E tests (critical user journeys)
- [ ] CI/CD workflows operational
- [ ] Quality gates enforced

**Common Patterns:**

```typescript
// E2E test (Playwright)
import { test, expect } from '@playwright/test';

test('payment journey', async ({ page }) => {
  const helpers = new KiteClassTestHelpers(page);

  await helpers.setupInstance('BASIC');
  await helpers.login('owner@school.com', 'password');

  await page.goto('/settings/pricing');
  await page.click('button:has-text("Upgrade to Standard")');

  await helpers.waitForPaymentQR();

  const orderId = await page.locator('text=/ORD-/').textContent();
  await helpers.simulatePayment(orderId!);

  await expect(page.locator('text=/payment successful/i')).toBeVisible();
});
```

```yaml
# CI/CD workflow (GitHub Actions)
name: Backend Tests

on: [pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: kiteclass_test

    steps:
      - uses: actions/checkout@v4
      - name: Run tests
        run: ./mvnw test
      - name: Check coverage
        run: ./mvnw jacoco:check
```

---

### PHASE 4: Performance & Deployment (Week 7)

**Documents to Read:**
- `.claude/skills/performance-testing-standards.md`
- `.claude/skills/deployment-quality-standards.md`

**Branch Naming:**
```
perf/k6-load-tests
perf/lighthouse-ci
docs/deployment-procedures
```

**Must Have:**
- [ ] P95 < 500ms
- [ ] Performance Score ≥ 80
- [ ] Deployment runbook complete

**Common Patterns:**

```javascript
// k6 load test
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '5m', target: 50 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const res = http.get('https://staging.kiteclass.com/api/students', {
    headers: { 'Authorization': `Bearer ${__ENV.TOKEN}` },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response < 500ms': (r) => r.timings.duration < 500,
  });
}
```

---

## 🧪 Testing Cheat Sheet

### Backend Testing

**Run tests:**
```bash
./mvnw test                    # All unit tests
./mvnw test -Dtest=StudentServiceImplTest  # Specific test
./mvnw verify -P integration   # Integration tests
./mvnw jacoco:report           # Coverage report
./mvnw jacoco:check            # Check coverage ≥80%
```

**Test naming:**
- Unit test: `*Test.java` (e.g., `StudentServiceImplTest.java`)
- Integration test: `*IntegrationTest.java`

**Test methods:**
```java
@Test
void methodName_shouldDoSomething_whenCondition() {
    // Given (Arrange)
    CreateStudentRequest request = createRequest();
    when(repo.save(any())).thenReturn(entity);

    // When (Act)
    StudentResponse response = service.createStudent(request);

    // Then (Assert)
    assertThat(response.getName()).isEqualTo("John Doe");
    verify(repo).save(any());
}
```

---

### Frontend Testing

**Run tests:**
```bash
npm run test                   # All tests
npm run test:coverage          # With coverage
npm run test:watch             # Watch mode
npm run test -- FeatureGate.test.tsx  # Specific test
```

**Test naming:**
- Component test: `*.test.tsx` (e.g., `FeatureGate.test.tsx`)

**Test methods:**
```typescript
describe('ComponentName', () => {
  it('should do something when condition', async () => {
    // Arrange
    server.use(http.get('/api/...', () => HttpResponse.json({ ... })));

    // Act
    render(<ComponentName />);

    // Assert
    expect(await screen.findByText('Expected')).toBeInTheDocument();
  });
});
```

---

### E2E Testing

**Run tests:**
```bash
npm run test:e2e               # All E2E tests
npm run test:e2e -- --ui       # UI mode
npm run test:e2e -- --debug    # Debug mode
npm run test:e2e -- guest-user-journey.spec.ts  # Specific test
```

**Test naming:**
- E2E test: `*.spec.ts` (e.g., `payment-journey.spec.ts`)

**Test methods:**
```typescript
test('user journey description', async ({ page }) => {
  await page.goto('https://...');
  await page.click('button:has-text("...")');
  await expect(page.locator('text=...')).toBeVisible();
});
```

---

## 🔒 Security Testing Checklist

### Must Test for Multi-Tenant Security

```java
// 1. Cross-tenant access prevention
@Test
void shouldNotAccessCrossTenantData() {
    setTenantContext(tenant1);
    Entity e1 = repo.save(createEntity());

    setTenantContext(tenant2);
    Optional<Entity> result = repo.findById(e1.getId());

    assertThat(result).isEmpty();
}

// 2. JWT cross-tenant validation
@Test
void shouldRejectCrossTenantJWT() {
    String token = jwtService.generateToken(1L, "user@t1.com", tenant1, roles);
    setTenantContext(tenant2);

    assertThatThrownBy(() -> jwtService.validateToken(token))
        .isInstanceOf(InvalidTokenException.class);
}

// 3. Payment double-payment prevention
@Test
void shouldPreventDoublePayment() {
    PaymentOrder order = createPaidOrder();

    assertThatThrownBy(() -> paymentService.verifyPayment(order.getOrderId()))
        .isInstanceOf(PaymentAlreadyPaidException.class);
}
```

---

## 📊 Coverage Requirements

| Layer | Target | Command |
|-------|--------|---------|
| **Backend Service** | ≥90% | `./mvnw test` |
| **Backend Controller** | ≥75% | `./mvnw test` |
| **Backend Repository** | ≥70% | `./mvnw verify -P integration` |
| **Frontend Components** | ≥85% | `npm run test:coverage` |
| **Frontend Hooks** | ≥80% | `npm run test:coverage` |
| **Overall** | **≥80%** | Both backend + frontend |

---

## ✅ Merge Criteria Checklist

**Before Creating PR:**
- [ ] All tests passing locally
- [ ] Coverage ≥80%
- [ ] No compilation warnings
- [ ] Security checklist completed (Phase 1)
- [ ] Code self-reviewed

**During Code Review:**
- [ ] CI/CD checks passing
- [ ] Code review approved (≥1 reviewer)
- [ ] Security team approval (Phase 1 PRs)
- [ ] All conversations resolved

**Before Merging:**
- [ ] No merge conflicts
- [ ] Branch up-to-date with base
- [ ] All checklist items completed

---

## 🚨 Common Issues & Solutions

### Issue: Tests failing in CI but passing locally

**Solution:**
```bash
# Check database state
docker-compose -f docker-compose.test.yml down -v
docker-compose -f docker-compose.test.yml up -d

# Re-run tests
./mvnw clean test
```

### Issue: Coverage below 80%

**Solution:**
```bash
# Generate coverage report
./mvnw jacoco:report

# Open report
open target/site/jacoco/index.html

# Add tests for uncovered lines
```

### Issue: Security scan failing

**Solution:**
```bash
# Run dependency check
./mvnw dependency-check:check

# Update vulnerable dependencies
./mvnw versions:use-latest-versions

# Or suppress false positives
# Add to dependency-check-suppressions.xml
```

---

## 📁 Key Files Reference

**Planning:**
- `documents/plans/code-review-pr-plan.md` - All 18 PRs breakdown

**Templates:**
- `.github/PULL_REQUEST_TEMPLATE.md` - PR template
- `.github/ISSUE_TEMPLATE/phase1-security-pr.md` - Security PR template
- `.github/ISSUE_TEMPLATE/phase2-testing-pr.md` - Testing PR template
- `.github/ISSUE_TEMPLATE/phase3-e2e-cicd-pr.md` - E2E/CI/CD PR template
- `.github/ISSUE_TEMPLATE/phase4-performance-pr.md` - Performance PR template

**Standards:**
- `.claude/skills/security-testing-standards.md`
- `.claude/skills/kiteclass-backend-testing-patterns.md`
- `.claude/skills/kiteclass-frontend-testing-patterns.md`
- `.claude/skills/e2e-testing-standards.md`
- `.claude/skills/ci-cd-quality-enforcement.md`
- `.claude/skills/deployment-quality-standards.md`
- `.claude/skills/performance-testing-standards.md`

**Code Review:**
- `.claude/skills/development-workflow.md` (KiteClass checklist)
- `documents/reports/code-review-requirement-report.md` (Final verdict)

---

## 🎯 Quick Commands

**Create PR:**
```bash
# 1. Create branch
git checkout -b security/multi-tenant-repositories

# 2. Make changes + tests

# 3. Commit
git add -A
git commit -m "security(multi-tenant): add tenant filtering"

# 4. Push
git push -u origin security/multi-tenant-repositories

# 5. Create PR on GitHub (use template)
```

**Run all checks locally (before PR):**
```bash
# Backend
./mvnw clean test jacoco:check

# Frontend
npm run test:coverage

# E2E
npm run test:e2e

# Linting
./mvnw checkstyle:check  # Backend
npm run lint              # Frontend
```

---

**Last Updated:** 2026-01-30
**Version:** 1.0
