---
name: Phase 2 - Testing PR
about: Template cho Testing Implementation PRs (Phase 2)
title: '[TEST] PR 2.X: <Title>'
labels: testing, phase-2, critical
assignees: ''
---

## PR Information

**PR Number:** PR 2.X (từ code-review-pr-plan.md)
**Phase:** Phase 2 - Testing Implementation
**Priority:** 🔴 P0 - CRITICAL
**Test Type:**
- [ ] Backend Unit Tests
- [ ] Backend Integration Tests
- [ ] Frontend Component Tests
- [ ] Frontend Integration Tests

**Estimated Effort:** X days
**Assignee:** @username

---

## Testing Scope

### Modules to Test

**Backend:**
- [ ] StudentService
- [ ] ClassService
- [ ] PaymentService
- [ ] TrialService
- [ ] FeatureDetectionService
- [ ] Other: ___________

**Frontend:**
- [ ] FeatureGate component
- [ ] PaymentQRCode component
- [ ] TrialBanner component
- [ ] Other: ___________

---

## Test Coverage Goals

### Current Coverage
- Backend: XX%
- Frontend: YY%

### Target Coverage
- Backend: ≥80% (target: 90% for services)
- Frontend: ≥80%

### Expected New Tests
- Unit Tests: XX tests
- Integration Tests: YY tests
- Component Tests: ZZ tests

---

## Implementation Checklist

### Setup

**Backend (if applicable):**
- [ ] Mockito dependencies added
- [ ] Testcontainers setup (PostgreSQL + Redis)
- [ ] Test utilities created
- [ ] MockMvc configured

**Frontend (if applicable):**
- [ ] Vitest + RTL setup
- [ ] MSW (Mock Service Worker) configured
- [ ] Test utilities created
- [ ] Testing Library queries configured

### Test Writing

- [ ] Happy path tests written
- [ ] Edge case tests written
- [ ] Error scenario tests written
- [ ] Boundary condition tests written
- [ ] All tests passing locally

### Test Organization

- [ ] Tests follow naming convention (`*Test.java`, `*.test.tsx`)
- [ ] Tests organized by module
- [ ] Test fixtures/factories created
- [ ] Test utilities extracted and reusable

---

## Test Examples

### Backend Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {
    @Mock private StudentRepository repository;
    @Mock private StudentMapper mapper;
    @InjectMocks private StudentServiceImpl service;

    @Test
    void createStudent_shouldValidateEmailUniqueness() {
        // Given
        CreateStudentRequest request = createRequest();
        when(repository.existsByEmailAndInstanceId(any(), any())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.createStudent(request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("Email already exists");

        verify(repository, never()).save(any());
    }
}
```

### Backend Integration Test Example

```java
@SpringBootTest
@Testcontainers
class PaymentFlowIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private PaymentService paymentService;

    @Test
    void paymentFlow_shouldUpgradeTier() {
        // Create order
        PaymentOrderResponse order = paymentService.createOrder(
            instanceId, PricingTier.STANDARD
        );

        // Verify payment
        paymentService.verifyPayment(new VerifyPaymentRequest(
            order.getOrderId(), "FT123", LocalDateTime.now()
        ));

        // Check tier upgraded
        InstanceConfig config = configService.getConfig(instanceId);
        assertThat(config.getTier()).isEqualTo(PricingTier.STANDARD);
    }
}
```

### Frontend Component Test Example

```typescript
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
});
```

---

## Testing Patterns Reference

### Backend Testing
- **Unit Tests:** `.claude/skills/kiteclass-backend-testing-patterns.md`
- **Integration Tests:** `documents/testing/integration-testing-strategy.md`

### Frontend Testing
- **Component Tests:** `.claude/skills/kiteclass-frontend-testing-patterns.md`

---

## Coverage Report

### Before

```
Backend Coverage: XX%
├── Service Layer: XX%
├── Controller Layer: XX%
└── Repository Layer: XX%

Frontend Coverage: YY%
├── Components: YY%
├── Hooks: YY%
└── Utils: YY%
```

### After (Target)

```
Backend Coverage: ≥80%
├── Service Layer: ≥90%
├── Controller Layer: ≥75%
└── Repository Layer: ≥70%

Frontend Coverage: ≥80%
├── Components: ≥85%
├── Hooks: ≥80%
└── Utils: ≥80%
```

---

## Test Execution

### Run Tests Locally

**Backend:**
```bash
./mvnw test                    # Unit tests
./mvnw verify -P integration   # Integration tests
./mvnw jacoco:report           # Coverage report
```

**Frontend:**
```bash
npm run test                   # All tests
npm run test:coverage          # With coverage
npm run test:watch             # Watch mode
```

### Expected Results

```
✅ All tests passing
Coverage: ≥80%
No test warnings
No flaky tests
```

---

## Review Criteria

### Test Quality

- [ ] Tests are clear and readable
- [ ] Tests use descriptive names
- [ ] Tests follow AAA pattern (Arrange-Act-Assert)
- [ ] Tests are isolated (no dependencies between tests)
- [ ] Tests use proper assertions
- [ ] No commented-out tests

### Test Coverage

- [ ] Coverage meets target (≥80%)
- [ ] Critical paths covered (100%)
- [ ] Edge cases covered
- [ ] Error scenarios covered
- [ ] No untested code in critical modules

### Performance

- [ ] Tests run quickly (< 5 minutes total)
- [ ] No slow tests (> 10 seconds each)
- [ ] Integration tests use Testcontainers efficiently

---

## Merge Criteria

- [ ] All tests passing (100%)
- [ ] Coverage ≥80%
- [ ] Code review approved
- [ ] No test warnings
- [ ] CI/CD checks passing

---

## Post-Merge Actions

- [ ] Update coverage badge
- [ ] Update testing documentation
- [ ] Notify team of new tests

---

**Reference Documents:**
- `.claude/skills/kiteclass-backend-testing-patterns.md`
- `.claude/skills/kiteclass-frontend-testing-patterns.md`
- `documents/testing/integration-testing-strategy.md`
- `documents/plans/code-review-pr-plan.md` (Phase 2)
