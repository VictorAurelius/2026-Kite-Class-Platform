# Code Review PR Implementation Plan

**Version:** 1.0
**Date:** 2026-01-30
**Purpose:** Chi tiết PRs để review và fix code theo quality standards

**Tham chiếu:**
- `documents/reports/code-review-requirement-report.md`
- `.claude/skills/kiteclass-backend-testing-patterns.md`
- `.claude/skills/security-testing-standards.md`
- `.claude/skills/development-workflow.md`

---

## Executive Summary

**Tổng quan:**
- **Total PRs:** 18 PRs (4 phases)
- **Timeline:** 7 weeks (5-7 weeks với 4-person team)
- **Effort:** 27-35 person-days
- **Team:** 2 backend + 1 frontend + 1 QA

**Critical Path:**
```
Phase 1 (Security) → Phase 2 (Testing) → Phase 3 (E2E + CI/CD) → Phase 4 (Performance)
     Week 1-2            Week 3-4              Week 5-6              Week 7
```

---

## PHASE 1: CRITICAL SECURITY REVIEW (Week 1-2)

**Timeline:** 2 weeks
**Priority:** 🔴 CRITICAL (MUST complete before Phase 2)
**Blocking:** All other phases depend on security fixes

---

### PR 1.1: Multi-Tenant Security Audit - Repositories

**Branch:** `security/multi-tenant-repositories`
**Assignee:** Backend Developer 1
**Effort:** 1.5 days
**Priority:** 🔴 P0

**Scope:**
- Audit ALL repository interfaces/classes trong `kiteclass-backend/src/main/java/**/repository/`
- Add missing `@TenantScoped` annotations
- Add manual `WHERE instance_id = :instanceId` filters
- Remove any hardcoded UUIDs

**Files to Review:**
```
kiteclass-backend/src/main/java/
├── student/repository/StudentRepository.java
├── class/repository/ClassRepository.java
├── attendance/repository/AttendanceRepository.java
├── payment/repository/PaymentOrderRepository.java
├── trial/repository/InstanceConfigRepository.java
└── ... (ước tính ~50 repository files)
```

**Checklist:**
- [ ] All repositories have `@TenantScoped` OR manual tenant filter
- [ ] No `UUID.fromString("...")` in repository code
- [ ] All JPA queries have `WHERE instance_id = :instanceId`
- [ ] Bulk operations respect tenant boundaries
- [ ] Cross-tenant access tests written (≥10 tests)

**Testing Requirements:**
```java
// Example test to add
@Test
void findById_shouldNotReturnCrossTenantData() {
    UUID tenant1 = UUID.randomUUID();
    UUID tenant2 = UUID.randomUUID();

    setTenantContext(tenant1);
    Student student1 = studentRepository.save(createStudent());

    setTenantContext(tenant2);
    Optional<Student> result = studentRepository.findById(student1.getId());

    assertThat(result).isEmpty(); // Cross-tenant access blocked
}
```

**Success Criteria:**
- [ ] 100% repositories audited
- [ ] All cross-tenant access tests pass
- [ ] No hardcoded instance IDs
- [ ] Code review approved by security team

**Merge Criteria:**
- ✅ All checklist items completed
- ✅ Cross-tenant security tests pass (100%)
- ✅ No regression in existing tests
- ✅ Security team approval

---

### PR 1.2: Multi-Tenant Security Audit - Services

**Branch:** `security/multi-tenant-services`
**Assignee:** Backend Developer 2
**Effort:** 1 day
**Priority:** 🔴 P0
**Depends On:** PR 1.1

**Scope:**
- Audit ALL service classes trong `kiteclass-backend/src/main/java/**/service/`
- Verify services use `TenantContext.getCurrentInstanceId()`
- Ensure no hardcoded instance IDs in business logic
- Add tenant validation in service methods

**Files to Review:**
```
kiteclass-backend/src/main/java/
├── student/service/StudentServiceImpl.java
├── class/service/ClassServiceImpl.java
├── payment/service/PaymentServiceImpl.java
└── ... (ước tính ~30 service files)
```

**Checklist:**
- [ ] All services use `TenantContext.getCurrentInstanceId()`
- [ ] No hardcoded `UUID.fromString(...)` in services
- [ ] Service methods validate tenant ownership
- [ ] API responses filtered by current tenant
- [ ] Service-level security tests written (≥15 tests)

**Testing Requirements:**
```java
@Test
void createStudent_shouldUseTenantContext() {
    UUID instanceId = UUID.randomUUID();
    setTenantContext(instanceId);

    CreateStudentRequest request = createRequest();
    StudentResponse response = studentService.createStudent(request);

    assertThat(response.instanceId()).isEqualTo(instanceId);
}

@Test
void getStudents_shouldOnlyReturnCurrentTenantStudents() {
    UUID tenant1 = UUID.randomUUID();
    UUID tenant2 = UUID.randomUUID();

    setTenantContext(tenant1);
    studentService.createStudent(createRequest("student1@t1.com"));

    setTenantContext(tenant2);
    studentService.createStudent(createRequest("student2@t2.com"));

    setTenantContext(tenant1);
    Page<StudentResponse> results = studentService.getStudents(Pageable.unpaged());

    assertThat(results.getContent())
        .hasSize(1)
        .allMatch(s -> s.instanceId().equals(tenant1));
}
```

**Success Criteria:**
- [ ] 100% services audited
- [ ] All service security tests pass
- [ ] Tenant context properly used everywhere
- [ ] Code review approved

---

### PR 1.3: JWT Token Validation & Tenant Security

**Branch:** `security/jwt-tenant-validation`
**Assignee:** Backend Developer 1
**Effort:** 0.5 day
**Priority:** 🔴 P0

**Scope:**
- Review `JWTAuthenticationFilter`, `JWTService`
- Add instanceId validation in JWT tokens
- Prevent cross-tenant JWT usage
- Add JWT security tests

**Files to Review:**
```
kiteclass-backend/src/main/java/
├── security/JWTAuthenticationFilter.java
├── security/JWTService.java
└── security/JWTTokenValidator.java
```

**Checklist:**
- [ ] JWT tokens contain `instanceId` claim
- [ ] Token validation checks instanceId matches request
- [ ] Cross-tenant token usage blocked
- [ ] Token expiry enforced (1 hour access, 7 days refresh)
- [ ] JWT security tests written (≥8 tests)

**Testing Requirements:**
```java
@Test
void validateToken_shouldRejectCrossTenantToken() {
    UUID tenant1 = UUID.randomUUID();
    UUID tenant2 = UUID.randomUUID();

    String token = jwtService.generateToken(1L, "user@t1.com", tenant1, List.of("OWNER"));

    setTenantContext(tenant2);

    assertThatThrownBy(() -> jwtService.validateToken(token))
        .isInstanceOf(InvalidTokenException.class)
        .hasMessageContaining("Invalid token for this instance");
}
```

**Success Criteria:**
- [ ] JWT validation includes instanceId check
- [ ] All JWT security tests pass
- [ ] No cross-tenant token usage possible

---

### PR 1.4: Payment Security Audit

**Branch:** `security/payment-validation`
**Assignee:** Backend Developer 2
**Effort:** 1 day
**Priority:** 🔴 P0

**Scope:**
- Review `PaymentService`, `VietQRService`, `PaymentController`
- Add amount validation (499k STANDARD, 999k PREMIUM)
- Add double-payment prevention
- Add order expiry validation
- Add transaction idempotency

**Files to Review:**
```
kiteclass-backend/src/main/java/
├── payment/service/PaymentServiceImpl.java
├── payment/service/VietQRServiceImpl.java
├── payment/controller/PaymentController.java
└── payment/repository/PaymentOrderRepository.java
```

**Checklist:**
- [ ] Amount validation matches tier pricing
- [ ] Double payment prevention (check order status)
- [ ] Order expiry validation (10-minute QR code)
- [ ] Transaction idempotency (VietQR callbacks)
- [ ] Audit logging for all payment state changes
- [ ] No financial data in logs (masked account numbers)
- [ ] Payment security tests written (≥12 tests)

**Testing Requirements:**
```java
@Test
void verifyPayment_shouldPreventDoublePayment() {
    PaymentOrder order = createPaidOrder();

    VerifyPaymentRequest request = new VerifyPaymentRequest(
        order.getOrderId(), "FT999", LocalDateTime.now()
    );

    assertThatThrownBy(() -> paymentService.verifyPayment(request))
        .isInstanceOf(PaymentAlreadyPaidException.class);
}

@Test
void verifyPayment_shouldValidateAmount() {
    PaymentOrder order = createPendingOrder(499000L); // STANDARD

    VerifyPaymentRequest request = new VerifyPaymentRequest(
        order.getOrderId(), "FT123", LocalDateTime.now(), 100L // Wrong amount
    );

    assertThatThrownBy(() -> paymentService.verifyPayment(request))
        .isInstanceOf(AmountMismatchException.class);
}

@Test
void verifyPayment_shouldRejectExpiredOrder() {
    PaymentOrder order = createExpiredOrder();

    assertThatThrownBy(() -> paymentService.verifyPayment(new VerifyPaymentRequest(...)))
        .isInstanceOf(PaymentExpiredException.class);
}
```

**Success Criteria:**
- [ ] Payment validation comprehensive
- [ ] All payment security tests pass
- [ ] Audit logging complete
- [ ] No financial data leakage

---

### PR 1.5: Feature Detection Security

**Branch:** `security/feature-detection`
**Assignee:** Backend Developer 1
**Effort:** 1 day
**Priority:** 🔴 P0

**Scope:**
- Review `FeatureDetectionService`, `InstanceConfigService`
- Add `@RequireFeature` annotations to STANDARD/PREMIUM endpoints
- Enforce tier limits (maxStudents, maxCourses, storageGB)
- Add feature detection tests

**Files to Review:**
```
kiteclass-backend/src/main/java/
├── feature/service/FeatureDetectionService.java
├── instance/service/InstanceConfigService.java
├── engagement/controller/EngagementController.java (STANDARD)
├── media/controller/MediaController.java (STANDARD)
├── aibranding/controller/AIBrandingController.java (PREMIUM)
└── ... (all STANDARD/PREMIUM controllers)
```

**Checklist:**
- [ ] All STANDARD/PREMIUM endpoints have `@RequireFeature`
- [ ] Feature config cached with 1-hour TTL (Redis)
- [ ] Tier limits enforced in services
- [ ] API returns 403 Forbidden for unavailable features
- [ ] Feature detection tests written (≥15 tests)

**Testing Requirements:**
```java
@Test
void trackEngagement_shouldRequireStandardTier() {
    UUID instanceId = createInstance(PricingTier.BASIC);

    assertThatThrownBy(() ->
        engagementController.trackEngagement(instanceId, createRequest())
    )
    .isInstanceOf(FeatureNotAvailableException.class)
    .hasMessageContaining("ENGAGEMENT")
    .hasMessageContaining("STANDARD");
}

@Test
void createStudent_shouldEnforceStudentLimit() {
    UUID instanceId = createInstance(PricingTier.BASIC); // Limit: 50

    // Create 50 students (at limit)
    for (int i = 0; i < 50; i++) {
        studentService.createStudent(instanceId, createRequest());
    }

    // 51st student should fail
    assertThatThrownBy(() ->
        studentService.createStudent(instanceId, createRequest())
    )
    .isInstanceOf(LimitExceededException.class)
    .hasMessageContaining("50 students");
}
```

**Success Criteria:**
- [ ] All premium features gated
- [ ] Tier limits enforced
- [ ] All feature detection tests pass

---

### PR 1.6: Security Testing Suite

**Branch:** `test/security-tests`
**Assignee:** QA Engineer
**Effort:** 1.5 days
**Priority:** 🔴 P0
**Depends On:** PR 1.1, 1.2, 1.3, 1.4, 1.5

**Scope:**
- Consolidate all security tests from PR 1.1-1.5
- Add comprehensive security test suite
- Add OWASP Top 10 tests
- Setup security test automation

**Testing Coverage:**
- [ ] Multi-tenant isolation tests (≥20 tests)
- [ ] Payment security tests (≥12 tests)
- [ ] JWT security tests (≥8 tests)
- [ ] Feature detection security tests (≥15 tests)
- [ ] OWASP Top 10 tests (≥30 tests)
  - SQL Injection
  - XSS
  - CSRF
  - Authentication bypass
  - Authorization bypass

**Files to Create:**
```
kiteclass-backend/src/test/java/
├── security/
│   ├── MultiTenantSecurityTest.java
│   ├── PaymentSecurityTest.java
│   ├── JWTSecurityTest.java
│   ├── FeatureDetectionSecurityTest.java
│   └── OWASP/
│       ├── SQLInjectionTest.java
│       ├── XSSTest.java
│       ├── CSRFTest.java
│       └── AuthorizationTest.java
```

**Success Criteria:**
- [ ] Total ≥85 security tests passing
- [ ] Coverage: Security-critical code ≥95%
- [ ] All OWASP Top 10 covered
- [ ] Automated in CI/CD

---

### PHASE 1 SUMMARY

**Total PRs:** 6
**Total Effort:** 6.5 days (2 developers + 1 QA)
**Timeline:** 2 weeks

**Merge Order:**
1. PR 1.1 (Multi-Tenant Repositories)
2. PR 1.2 (Multi-Tenant Services) - depends on 1.1
3. PR 1.3 (JWT Validation)
4. PR 1.4 (Payment Security)
5. PR 1.5 (Feature Detection)
6. PR 1.6 (Security Test Suite) - depends on all above

**Phase 1 Success Criteria:**
- ✅ All 6 PRs merged
- ✅ All security tests passing (≥85 tests)
- ✅ Security team approval
- ✅ No HIGH/CRITICAL security vulnerabilities
- ✅ Ready to proceed to Phase 2

---

## PHASE 2: TESTING IMPLEMENTATION (Week 3-4)

**Timeline:** 2 weeks
**Priority:** 🔴 CRITICAL
**Depends On:** Phase 1 complete

---

### PR 2.1: Backend Unit Tests - Core Modules

**Branch:** `test/backend-unit-core`
**Assignee:** Backend Developer 1
**Effort:** 2 days
**Priority:** 🔴 P0

**Scope:**
- Write unit tests cho Student, Class, Attendance modules
- Target: 90%+ service coverage
- Use Mockito for dependencies

**Files to Test:**
```
kiteclass-backend/src/main/java/
├── student/service/StudentServiceImpl.java
├── class/service/ClassServiceImpl.java
└── attendance/service/AttendanceServiceImpl.java
```

**Testing Requirements:**
- [ ] StudentService: ≥25 unit tests (CRUD + validation)
- [ ] ClassService: ≥20 unit tests
- [ ] AttendanceService: ≥15 unit tests
- [ ] All edge cases covered
- [ ] All error scenarios tested

**Example Tests:**
```java
@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {
    @Mock private StudentRepository repository;
    @Mock private StudentMapper mapper;
    @InjectMocks private StudentServiceImpl service;

    @Test
    void createStudent_shouldValidateEmailUniqueness() {
        when(repository.existsByEmailAndInstanceId(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.createStudent(createRequest()))
            .isInstanceOf(DuplicateEmailException.class);
    }

    // ... 24 more tests
}
```

**Success Criteria:**
- [ ] ≥60 unit tests written and passing
- [ ] Service coverage ≥90%
- [ ] All edge cases covered

---

### PR 2.2: Backend Unit Tests - Payment & Trial

**Branch:** `test/backend-unit-payment`
**Assignee:** Backend Developer 2
**Effort:** 1.5 days
**Priority:** 🔴 P0

**Scope:**
- Write unit tests cho Payment, Trial, FeatureDetection modules
- Target: 90%+ service coverage

**Files to Test:**
```
kiteclass-backend/src/main/java/
├── payment/service/PaymentServiceImpl.java
├── trial/service/TrialServiceImpl.java
└── feature/service/FeatureDetectionServiceImpl.java
```

**Testing Requirements:**
- [ ] PaymentService: ≥30 unit tests (create order, verify, handle callbacks)
- [ ] TrialService: ≥20 unit tests (lifecycle, expiration)
- [ ] FeatureDetectionService: ≥15 unit tests

**Success Criteria:**
- [ ] ≥65 unit tests written and passing
- [ ] Service coverage ≥90%

---

### PR 2.3: Backend Integration Tests

**Branch:** `test/backend-integration`
**Assignee:** Backend Developer 1 + 2
**Effort:** 2 days
**Priority:** 🔴 P0
**Depends On:** PR 2.1, 2.2

**Scope:**
- Write integration tests cho full flows
- Use Testcontainers (PostgreSQL + Redis)
- Test API endpoints

**Testing Requirements:**
- [ ] Multi-tenant isolation integration tests (≥10 tests)
- [ ] Feature detection integration tests (≥10 tests)
- [ ] Payment flow integration tests (≥8 tests)
- [ ] Trial lifecycle integration tests (≥8 tests)
- [ ] API endpoint tests (≥20 tests)

**Example Tests:**
```java
@SpringBootTest
@Testcontainers
class PaymentFlowIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void paymentFlow_createToVerify() {
        // Create order
        PaymentOrderResponse order = paymentService.createSubscriptionOrder(
            user, PricingTier.STANDARD
        );

        assertThat(order.getQrImageUrl()).contains("vietqr.io");
        assertThat(order.getAmount()).isEqualTo(499000L);

        // Verify payment
        paymentService.verifyPayment(new VerifyPaymentRequest(
            order.getOrderId(), "FT123", LocalDateTime.now()
        ));

        // Check tier upgraded
        InstanceConfig config = configService.getConfig(user.getInstanceId());
        assertThat(config.getTier()).isEqualTo(PricingTier.STANDARD);
    }
}
```

**Success Criteria:**
- [ ] ≥56 integration tests passing
- [ ] All critical flows tested
- [ ] Testcontainers setup works

---

### PR 2.4: Frontend Component Tests - Core

**Branch:** `test/frontend-components-core`
**Assignee:** Frontend Developer
**Effort:** 2 days
**Priority:** 🟡 HIGH

**Scope:**
- Setup Vitest + React Testing Library
- Write component tests cho FeatureGate, UpgradeModal
- Write component tests cho common UI

**Setup:**
```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      statements: 80,
      branches: 75,
      functions: 80,
      lines: 80,
    },
  },
});
```

**Files to Test:**
```
kiteclass-frontend/src/components/
├── features/FeatureGate.tsx
├── features/UpgradeModal.tsx
├── layout/Sidebar.tsx
└── ui/* (button, dialog, etc.)
```

**Testing Requirements:**
- [ ] FeatureGate: ≥8 tests (tier-based rendering)
- [ ] UpgradeModal: ≥6 tests (display, navigation)
- [ ] Sidebar: ≥5 tests (menu filtering)
- [ ] UI components: ≥10 tests

**Example Tests:**
```typescript
// FeatureGate.test.tsx
import { render, screen } from '@testing-library/react';
import { FeatureGate } from './FeatureGate';

describe('FeatureGate', () => {
  it('should show content for available features', async () => {
    server.use(
      http.get('/api/instance/config', () => {
        return HttpResponse.json({
          tier: 'STANDARD',
          features: { ENGAGEMENT: true },
        });
      })
    );

    render(
      <FeatureGate feature="ENGAGEMENT">
        <div>Engagement Dashboard</div>
      </FeatureGate>
    );

    expect(await screen.findByText('Engagement Dashboard')).toBeInTheDocument();
  });

  // ... 7 more tests
});
```

**Success Criteria:**
- [ ] ≥29 component tests passing
- [ ] Component coverage ≥80%
- [ ] MSW setup for API mocking

---

### PR 2.5: Frontend Component Tests - Payment & Trial

**Branch:** `test/frontend-components-payment`
**Assignee:** Frontend Developer
**Effort:** 2 days
**Priority:** 🟡 HIGH

**Scope:**
- Write component tests cho Payment UI
- Write component tests cho Trial Banners
- Write component tests cho AI Branding UI

**Files to Test:**
```
kiteclass-frontend/src/components/
├── payment/PaymentQRCode.tsx
├── payment/PaymentCountdown.tsx
├── payment/PaymentStatusPoller.tsx
├── trial/TrialBanner.tsx
└── aibranding/AIBrandingForm.tsx
```

**Testing Requirements:**
- [ ] PaymentQRCode: ≥6 tests
- [ ] PaymentCountdown: ≥8 tests (timer logic)
- [ ] PaymentStatusPoller: ≥7 tests (polling logic)
- [ ] TrialBanner: ≥6 tests
- [ ] AIBrandingForm: ≥5 tests

**Success Criteria:**
- [ ] ≥32 component tests passing
- [ ] Timer/polling logic tested with fake timers

---

### PR 2.6: Frontend Integration Tests

**Branch:** `test/frontend-integration`
**Assignee:** Frontend Developer
**Effort:** 1.5 days
**Priority:** 🟡 HIGH
**Depends On:** PR 2.4, 2.5

**Scope:**
- Write integration tests cho API calls
- Test React Query caching
- Test Zustand state management

**Testing Requirements:**
- [ ] API integration tests (≥15 tests)
- [ ] React Query caching tests (≥8 tests)
- [ ] Zustand store tests (≥7 tests)

**Success Criteria:**
- [ ] ≥30 integration tests passing
- [ ] API mocking with MSW works
- [ ] State management tested

---

### PHASE 2 SUMMARY

**Total PRs:** 6
**Total Effort:** 11 days (2 backend + 1 frontend)
**Timeline:** 2 weeks

**Merge Order:**
1. PR 2.1 (Backend Unit - Core)
2. PR 2.2 (Backend Unit - Payment)
3. PR 2.3 (Backend Integration) - depends on 2.1, 2.2
4. PR 2.4 (Frontend Component - Core)
5. PR 2.5 (Frontend Component - Payment)
6. PR 2.6 (Frontend Integration) - depends on 2.4, 2.5

**Phase 2 Success Criteria:**
- ✅ Backend test coverage ≥80%
- ✅ Frontend test coverage ≥80%
- ✅ Total ≥250 tests passing
- ✅ All critical flows tested
- ✅ Ready for Phase 3

---

## PHASE 3: E2E & CI/CD (Week 5-6)

**Timeline:** 2 weeks
**Priority:** 🔴 CRITICAL
**Depends On:** Phase 2 complete

---

### PR 3.1: E2E Tests - Guest & Trial Journeys

**Branch:** `test/e2e-guest-trial`
**Assignee:** QA Engineer
**Effort:** 2 days
**Priority:** 🔴 P0

**Scope:**
- Setup Playwright
- Write guest user journey tests
- Write trial user journey tests

**Setup:**
```typescript
// playwright.config.ts
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: 2,
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
  ],
});
```

**Testing Requirements:**
- [ ] Guest landing page journey (≥5 tests)
- [ ] Trial signup journey (≥8 tests)
- [ ] Trial countdown journey (≥7 tests)
- [ ] Grace period journey (≥5 tests)

**Success Criteria:**
- [ ] ≥25 E2E tests passing
- [ ] Playwright setup complete

---

### PR 3.2: E2E Tests - Payment & Feature Upgrade

**Branch:** `test/e2e-payment-features`
**Assignee:** QA Engineer
**Effort:** 2 days
**Priority:** 🔴 P0

**Scope:**
- Write payment journey tests
- Write feature upgrade journey tests
- Write AI branding journey tests

**Testing Requirements:**
- [ ] Payment journey (create → QR → verify) (≥8 tests)
- [ ] Feature upgrade journey (≥7 tests)
- [ ] AI branding journey (≥5 tests)

**Success Criteria:**
- [ ] ≥20 E2E tests passing
- [ ] All payment flows tested

---

### PR 3.3: E2E Tests - Multi-Tenant Isolation

**Branch:** `test/e2e-multi-tenant`
**Assignee:** QA Engineer
**Effort:** 1 day
**Priority:** 🔴 P0

**Scope:**
- Write multi-tenant isolation E2E tests
- Verify data separation in UI

**Testing Requirements:**
- [ ] Multi-tenant isolation tests (≥5 tests)

**Success Criteria:**
- [ ] Multi-tenant E2E tests passing
- [ ] Total E2E tests ≥50

---

### PR 3.4: CI/CD Pipeline - Test Automation

**Branch:** `cicd/test-automation`
**Assignee:** Backend Developer 1 + QA
**Effort:** 1.5 days
**Priority:** 🔴 P0

**Scope:**
- Setup GitHub Actions workflows
- Configure test automation
- Setup coverage reporting

**Files to Create:**
```yaml
.github/workflows/
├── backend-tests.yml     (unit + integration)
├── frontend-tests.yml    (component + integration)
├── e2e-tests.yml         (Playwright)
└── coverage.yml          (Codecov)
```

**Success Criteria:**
- [ ] All tests run on PR
- [ ] Coverage reports generated
- [ ] Tests block merge if failing

---

### PR 3.5: CI/CD Pipeline - Security Scanning

**Branch:** `cicd/security-scanning`
**Assignee:** Backend Developer 2
**Effort:** 1 day
**Priority:** 🟡 HIGH

**Scope:**
- Setup OWASP Dependency Check
- Setup Snyk scanning
- Setup CodeQL analysis

**Files to Create:**
```yaml
.github/workflows/
├── owasp-dependency-check.yml
├── snyk-security.yml
└── codeql-analysis.yml
```

**Success Criteria:**
- [ ] Security scans run on PR
- [ ] HIGH/CRITICAL vulnerabilities block merge

---

### PR 3.6: CI/CD Pipeline - Quality Gates

**Branch:** `cicd/quality-gates`
**Assignee:** Backend Developer 1
**Effort:** 1 day
**Priority:** 🟡 HIGH
**Depends On:** PR 3.4, 3.5

**Scope:**
- Configure coverage gates (≥80%)
- Configure branch protection rules
- Setup merge criteria

**Success Criteria:**
- [ ] Coverage gate enforced
- [ ] Branch protection active
- [ ] CI/CD fully operational

---

### PHASE 3 SUMMARY

**Total PRs:** 6
**Total Effort:** 8.5 days (1 QA + 2 backend)
**Timeline:** 2 weeks

**Phase 3 Success Criteria:**
- ✅ ≥50 E2E tests passing
- ✅ CI/CD pipeline operational
- ✅ Quality gates enforced
- ✅ Security scanning automated

---

## PHASE 4: PERFORMANCE & DEPLOYMENT (Week 7)

**Timeline:** 1 week
**Priority:** 🟢 MEDIUM

---

### PR 4.1: Performance Testing - k6 Load Tests

**Branch:** `perf/k6-load-tests`
**Assignee:** Backend Developer 2
**Effort:** 1.5 days
**Priority:** 🟢 MEDIUM

**Scope:**
- Write k6 load test scripts
- Test API endpoints under load
- Test payment flows under load

**Success Criteria:**
- [ ] P95 response time < 500ms
- [ ] Error rate < 1%

---

### PR 4.2: Performance Testing - Frontend

**Branch:** `perf/lighthouse-ci`
**Assignee:** Frontend Developer
**Effort:** 1 day
**Priority:** 🟢 MEDIUM

**Scope:**
- Setup Lighthouse CI
- Optimize bundle size
- Test performance benchmarks

**Success Criteria:**
- [ ] Performance Score ≥80
- [ ] Bundle size < 500KB

---

### PR 4.3: Deployment Documentation

**Branch:** `docs/deployment-procedures`
**Assignee:** Backend Developer 1
**Effort:** 1 day
**Priority:** 🟢 MEDIUM

**Scope:**
- Document deployment procedures
- Document rollback procedures
- Create deployment checklist

**Success Criteria:**
- [ ] Deployment runbook complete
- [ ] Rollback procedures documented

---

### PHASE 4 SUMMARY

**Total PRs:** 3
**Total Effort:** 3.5 days
**Timeline:** 1 week

---

## COMPLETE PR TIMELINE

```
Week 1-2: PHASE 1 - SECURITY (6 PRs)
├── PR 1.1: Multi-Tenant Repositories
├── PR 1.2: Multi-Tenant Services
├── PR 1.3: JWT Validation
├── PR 1.4: Payment Security
├── PR 1.5: Feature Detection
└── PR 1.6: Security Test Suite

Week 3-4: PHASE 2 - TESTING (6 PRs)
├── PR 2.1: Backend Unit - Core
├── PR 2.2: Backend Unit - Payment
├── PR 2.3: Backend Integration
├── PR 2.4: Frontend Component - Core
├── PR 2.5: Frontend Component - Payment
└── PR 2.6: Frontend Integration

Week 5-6: PHASE 3 - E2E & CI/CD (6 PRs)
├── PR 3.1: E2E - Guest & Trial
├── PR 3.2: E2E - Payment & Features
├── PR 3.3: E2E - Multi-Tenant
├── PR 3.4: CI/CD - Test Automation
├── PR 3.5: CI/CD - Security Scanning
└── PR 3.6: CI/CD - Quality Gates

Week 7: PHASE 4 - PERFORMANCE (3 PRs)
├── PR 4.1: k6 Load Tests
├── PR 4.2: Lighthouse CI
└── PR 4.3: Deployment Docs

TOTAL: 18 PRs, 7 weeks
```

---

## FINAL SUCCESS CRITERIA

### Production-Ready Checklist

- [ ] **Phase 1 Complete:** All security PRs merged
  - [ ] Multi-tenant security tests passing (100%)
  - [ ] Payment security validated
  - [ ] JWT security enforced
  - [ ] Feature detection secure

- [ ] **Phase 2 Complete:** All testing PRs merged
  - [ ] Backend coverage ≥80%
  - [ ] Frontend coverage ≥80%
  - [ ] Total ≥250 tests passing

- [ ] **Phase 3 Complete:** E2E & CI/CD PRs merged
  - [ ] ≥50 E2E tests passing
  - [ ] CI/CD pipeline operational
  - [ ] Quality gates enforced

- [ ] **Phase 4 Complete:** Performance PRs merged
  - [ ] Performance benchmarks meet targets
  - [ ] Deployment docs complete

### Code Review & Approval

- [ ] All PRs reviewed by 2+ team members
- [ ] Security PRs approved by security team
- [ ] No HIGH/CRITICAL vulnerabilities
- [ ] All automated tests passing

### Deployment

- [ ] Staging deployment successful
- [ ] Smoke tests passing
- [ ] Performance tests passing
- [ ] Security scan clean
- [ ] Production deployment approved

---

**Plan Version:** 1.0
**Created:** 2026-01-30
**Next Review:** After Phase 1 complete
**Total Effort:** 27-35 person-days (7 weeks)
