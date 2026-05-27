# Testing Standards

**Version:** 2.0 (Consolidated)
**Gộp từ:** testing-guide, spring-boot-testing-quality, kiteclass-backend-testing-patterns,
kiteclass-frontend-testing-patterns, frontend-testing-requirements, e2e-testing-standards,
performance-testing-standards, security-testing-standards, ide-testcontainers-warnings

---

## Mục lục nhanh

| Cần gì | Xem section |
|--------|-------------|
| Testing stack tổng quan | [1. Stack](#1-testing-stack) |
| Spring Boot test templates | [2. Backend Tests](#2-backend-unit--integration-tests) |
| Testcontainers / IDE warnings | [2.4 Testcontainers](#24-testcontainers-best-practices) |
| **GAP-746 flush trap (mandatory đọc)** | [2.8 Flush discipline](#28-hibernate-persistence-context-flush-discipline-trong-it-tests-gap-746-trap) |
| **AFTER_COMMIT listener test pattern (mandatory)** | [2.9 AFTER_COMMIT pattern](#29-testing-after_commit-listener-side-effects) |
| Frontend component tests | [3. Frontend Tests](#3-frontend-tests) |
| E2E với Playwright | [4. E2E Tests](#4-e2e-tests) |
| Performance targets | [5. Performance](#5-performance-targets) |
| Security testing | [6. Security](#6-security-testing) |
| Checklist trước commit | [7. Checklist](#7-checklist-trước-commit) |

---

## 1. Testing Stack

### Backend (Java/Spring Boot)

| Library | Purpose | Version |
|---------|---------|---------|
| JUnit 5 | Test framework | 5.10+ |
| Mockito | Mocking | 5.x |
| AssertJ | Fluent assertions | 3.24+ |
| Testcontainers | Database testing | 1.19+ |
| Spring Boot Test | Integration testing | 3.4+ |
| JaCoCo | Coverage reporting | 0.8.11+ |

### Frontend (TypeScript/React)

| Library | Purpose |
|---------|---------|
| Vitest | Test runner |
| React Testing Library | Component testing |
| MSW (Mock Service Worker) | API mocking |
| Playwright | E2E testing |

### Coverage Requirements

| Layer | Minimum |
|-------|---------|
| Backend (line coverage) | >= 80% |
| Frontend (lines/functions/statements) | >= 80% |
| Frontend (branches) | >= 75% |

---

## 2. Backend Unit & Integration Tests

### 2.1 Controller Unit Test (Spring MVC)

```java
@WebMvcTest(StudentController.class)
@AutoConfigureMockMvc
@Import({StudentControllerTest.TestSecurityConfig.class, StudentControllerTest.MockConfig.class})
class StudentControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public StudentService studentService() {
            return Mockito.mock(StudentService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private StudentService studentService;

    @BeforeEach
    void resetMocks() { Mockito.reset(studentService); }

    @Test
    void createStudent_shouldReturn201() throws Exception {
        when(studentService.createStudent(any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

### 2.2 Controller Unit Test (WebFlux - Reactive)

```java
@WebFluxTest(YourController.class)
@Import({YourControllerTest.MockConfig.class, TestSecurityConfig.class})
class YourControllerTest {

    @Autowired private WebTestClient webTestClient;
    @Autowired private YourService yourService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public YourService yourService() { return Mockito.mock(YourService.class); }
    }

    @BeforeEach
    void resetMocks() { Mockito.reset(yourService); }
}
```

**Key Points:**
- Spring Boot 3.4.0+: Dung `@TestConfiguration` thay `@MockBean` (deprecated)
- Luon add `@Primary` tren mock beans
- `Mockito.reset()` trong `@BeforeEach`

### 2.3 Service Unit Test

```java
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private StudentRepository repository;
    @InjectMocks private StudentServiceImpl service;

    @Test
    void findById_shouldReturnStudent() {
        when(repository.findById(anyLong())).thenReturn(Optional.of(entity));

        Student result = service.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(repository).findById(1L);
    }
}
```

### 2.4 Testcontainers Best Practices

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers framework
    protected static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("kiteclass_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true); // Performance: reuse across runs

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
}
```

**IDE Warning Fix (VSCode Warning Code 1102):**
Dung `@Container` annotation de IDE nhan ra lifecycle management:
```java
@Container  // Signals Testcontainers manages lifecycle - no @SuppressWarnings needed
static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
```

### 2.5 Mockito Imports (KHONG dung wildcard)

```java
// ALWAYS explicit imports
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

// NEVER wildcard - import static org.mockito.Mockito.*;
```

### 2.6 AssertJ Patterns

```java
// Use assertThatCode (compatible with AssertJ < 3.23)
assertThatCode(() -> someMethod()).doesNotThrowAnyException();

// AssertJ thay Java assert
assertThat(user).isNotNull();
assertThat(user.getId()).isEqualTo(1L);
assertThatThrownBy(() -> service.delete(999L))
    .isInstanceOf(NotFoundException.class);
```

### 2.7 Multi-Tenant Testing Pattern

```java
@SpringBootTest
@Transactional
class StudentServiceMultiTenantTest extends MultiTenantTestBase {

    @Test
    @DisplayName("getStudents should only return current tenant's students")
    void getStudents_shouldOnlyReturnCurrentTenantStudents() {
        UUID tenant1 = createInstanceId("tenant1");
        UUID tenant2 = createInstanceId("tenant2");

        createStudent(tenant1, "Student A");
        createStudent(tenant2, "Student B");

        TenantContext.setCurrentTenant(tenant1);

        List<Student> results = studentService.findAll();

        assertThat(results)
            .hasSize(1)
            .allMatch(s -> s.getInstanceId().equals(tenant1));

        TenantContext.clear();
    }
}
```

### 2.8 Hibernate persistence-context flush discipline trong IT tests (GAP-746 trap)

**Trap (verified Wave rst-cleanup 2026-05-27 — 2 tests caught):** `TestTenantContextFilter` áp dụng GAP-746 cross-tenant defend fix — khi `PREVIOUS_TENANT != current tenantUuid`, gọi `entityManager.clear()` **WITHOUT** prior `flush()`. Pending UPDATE/INSERT trong persistence context bị **discard silent** trước khi `mockMvc.perform` execute → controller đọc stale DB row.

**Pattern bị ảnh hưởng:** Test class với `@Transactional` + setUp() saves entities (testAssignment / testCourse / testStudent) + test method modifies entity in-memory + calls `mockMvc.perform` để gọi controller validate hành vi update.

**Symptoms:**
- HTTP 400 unexpected (vd `ASSIGNMENT_NOT_ACCEPTING_SUBMISSIONS` despite `setStatus(PUBLISHED)`)
- Assertion fail trên derived field (vd `adjustedScore = 100` despite `setDueDate(past)` triggering penalty)
- Test passes locally individually nhưng fails khi run với class — depending on JUnit method order + previous test's tenantId

**Required pattern (mandatory):**

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssignmentIntegrationTest {
    @PersistenceContext  // jakarta.persistence
    private EntityManager entityManager;

    // ... repository autowires ...

    @Test
    void submitAssignment_shouldSucceed_whenPublished() throws Exception {
        // Modify entity in test body
        testAssignment.setStatus(AssignmentStatus.PUBLISHED);
        assignmentRepository.save(testAssignment);

        // ✅ MANDATORY: flush UPDATE before mockMvc clear()
        // Without this, TestTenantContextFilter cross-tenant clear discards the UPDATE
        entityManager.flush();

        mockMvc.perform(post("/api/v1/assignments/submit")
                .header("X-Tenant-Id", tenantId.toString())
                .header("X-User-Id", testStudent.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
}
```

**When to flush explicitly:**

| Scenario | Flush required? |
|---|---|
| setUp() saves new entities (IDENTITY ID) — Hibernate auto-flushes for ID generation | ❌ NO — auto-flushed |
| Test body modifies managed entity (`entity.setX(...)` + `repo.save()`) + calls `mockMvc.perform` | ✅ **YES — mandatory** |
| Test body creates new entity (`repo.save(new X())`) without intervening modification | ❌ NO — INSERT auto-flushes IDENTITY |
| Test body modifies entity then creates ANOTHER new entity then `mockMvc.perform` | ⚠️ **YES** — be explicit; don't rely on cross-call flush ordering |
| Test body modifies entity then queries via repo directly (no mockMvc) | ❌ NO — Hibernate auto-flush before query |

**Reference:**
- `kiteclass-core/src/main/java/com/kiteclass/core/config/TestTenantContextFilter.java` lines 88-94 — GAP-746 fix
- `.claude/rules/audit-to-gap-pipeline.md` §2.6 state-check at fix-time
- Wave gap-746 closure 2026-05-25 + Wave rst-cleanup 2026-05-27 (`AssignmentIntegrationTest` 2 sites)

### 2.9 Testing AFTER_COMMIT listener side-effects

**Trap (verified Wave rst-cleanup 2026-05-27):** `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` chỉ fire **AFTER the parent transaction commits**. Trong test class với `@Transactional @Rollback(true)` (Spring TX rolls back at end), parent TX **never commits** → AFTER_COMMIT listener **never fires** → downstream side-effect (invoice auto-creation, audit log write, notification dispatch) **never happens**.

**Listeners trong codebase áp dụng pattern này (per `audit-service-isolation.md` + capacity-race fix Wave beta-readiness-1):**
- `EnrollmentEventListener` → creates Invoice on enrollment
- `GradeEventListener` → publishes Grade event
- `AdminLoginAlertEventListener` → sends alert email
- `BrandingEventEmitter` → publishes branding update
- (general: any `@TransactionalEventListener(phase = AFTER_COMMIT)` annotation)

**Symptoms:**
- Test creates entity A via API → expects downstream entity B to exist
- Query for B returns empty content
- Other tests defensively early-return on empty list, so failure surfaces only in tests asserting B directly

**Required pattern — Option A (preferred, simplest):** Autowire the target service + manually trigger after parent action.

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback(true)
class InvoiceFlowIntegrationTest {
    @Autowired
    private InvoiceService invoiceService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void testMultiTenantIsolation_InvoiceFilters() throws Exception {
        // Enroll via API (parent TX never commits in @Transactional test)
        Long enrollment1Id = enrollStudent(student1Id, class1Id, tuition);

        // ✅ MANDATORY: AFTER_COMMIT listener won't fire in rollback TX;
        // manually invoke target service to seed dependent entity
        invoiceService.createInvoiceForEnrollment(enrollment1Id);
        entityManager.flush();

        // Now assertion can proceed
        mockMvc.perform(get("/api/v1/invoices/student/" + student1Id + "/unpaid")
                .header("X-Tenant-Id", tenant1.toString()))
            .andExpect(jsonPath("$.data.content[*].studentId").value(student1Id.intValue()));
    }
}
```

**Required pattern — Option B (when downstream chain is multi-step):** Use Spring's `TestTransaction` to commit between steps.

```java
// In test method, after enrollment:
TestTransaction.flagForCommit();
TestTransaction.end();
TestTransaction.start();  // new TX for assertion
// AFTER_COMMIT fired in between because TX committed
```

**Banned shortcut:** Removing `@TransactionalEventListener(phase = AFTER_COMMIT)` from production listener to "make test work" — production needs AFTER_COMMIT for capacity-race safety per `audit-service-isolation.md`. Fix the test, not the production code.

**Reference:**
- `kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/listener/EnrollmentEventListener.java` — Wave beta-readiness-1 Bucket B capacity-race fix mandating AFTER_COMMIT
- `.claude/rules/audit-service-isolation.md` v1.0.0 — `Propagation.REQUIRES_NEW` companion mandate
- Wave rst-cleanup 2026-05-27 (`InvoiceFlowIntegrationTest.testMultiTenantIsolation_InvoiceFilters`)

---

## 3. Frontend Tests

### 3.1 Test Setup (MSW + QueryClient)

```typescript
// src/test/setup.ts
import { beforeAll, afterEach, afterAll } from 'vitest';
import { server } from '../mocks/server';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

```typescript
// src/test/utils.tsx - Wrapper with QueryClient
export function renderWithProviders(ui: ReactElement) {
  const testQueryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(ui, { wrapper: ({ children }) => (
    <QueryClientProvider client={testQueryClient}>{children}</QueryClientProvider>
  )});
}
```

### 3.2 Component Test Pattern

```typescript
describe('StudentForm', () => {
  it('should render all fields', () => {
    render(<StudentForm onSubmit={vi.fn()} />);
    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
  });

  it('should validate required fields', async () => {
    const user = userEvent.setup();
    render(<StudentForm onSubmit={vi.fn()} />);
    await user.click(screen.getByRole('button', { name: /submit/i }));
    expect(screen.getByText(/name.*required/i)).toBeInTheDocument();
  });

  it('should submit valid data', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<StudentForm onSubmit={onSubmit} />);
    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.click(screen.getByRole('button', { name: /submit/i }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
  });

  it('should display API errors', async () => {
    server.use(
      http.post('/api/v1/students', () =>
        HttpResponse.json({ message: 'Email already exists' }, { status: 400 })
      )
    );
    // Submit and check error message displayed
  });
});
```

### 3.3 Coverage Thresholds

```typescript
// vitest.config.ts
coverage: {
  thresholds: { lines: 80, functions: 80, branches: 75, statements: 80 }
}
```

Run coverage: `pnpm test:coverage`

**Phai test tat ca states:** loading + error + empty + success

---

## 4. E2E Tests

### 4.1 Test Pyramid cho KiteClass

```
       E2E Tests (10%)      <- Playwright, critical business flows only
     /                  \
Integration Tests (30%)     <- Testcontainers, API flows
/                          \
Unit/Component Tests (60%)  <- Fast, isolated
```

### 4.2 Critical Flows phai co E2E

- Trial registration: 14-day trial → 3-day grace → expiry
- Payment flow: QR display → payment → verification → tier upgrade
- Multi-tenant isolation: data leak prevention giua instances
- Feature access control: tier bypass prevention

### 4.3 Playwright Setup

```typescript
// playwright.config.ts
export default defineConfig({
  testDir: './tests/e2e',
  use: {
    baseURL: 'http://localhost:4700',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
  ],
});
```

---

## 5. Performance Targets

| Endpoint | P95 | Max |
|----------|-----|-----|
| GET /api/students | 200ms | 1s |
| POST /api/students | 300ms | 1.5s |
| GET /api/instance/config | 50ms | 200ms |
| POST /api/payments/orders | 500ms | 2s |
| POST /api/ai-branding/generate | 1s | 5s |

| Cache | Hit Rate |
|-------|----------|
| Instance Config (Redis) | >= 90% |
| Feature Flags (Redis) | >= 95% |

| Frontend Metric | Target |
|-----------------|--------|
| First Contentful Paint | < 1.5s |
| Largest Contentful Paint | < 2.5s |
| Total Bundle Size | < 500KB gzipped |

---

## 6. Security Testing

### 6.1 Cross-Tenant Access Prevention

```java
@Test
void shouldPreventCrossTenantDataAccess() {
    UUID tenant1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID tenant2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    createStudentForTenant(tenant2, "Tenant2 Student");

    TenantContext.setCurrentTenant(tenant1);
    List<Student> students = studentRepository.findAll();

    // Must NOT see tenant2's data
    assertThat(students).noneMatch(s -> s.getInstanceId().equals(tenant2));
    TenantContext.clear();
}
```

### 6.2 JWT Security Tests

```java
@Test
void shouldRejectExpiredToken() {
    String expiredToken = generateExpiredToken();
    webTestClient.get().uri("/api/v1/students")
        .header("Authorization", "Bearer " + expiredToken)
        .exchange()
        .expectStatus().isUnauthorized();
}
```

---

## 7. Checklist Truoc Commit

### Backend

- [ ] Khong co wildcard imports
- [ ] `@MockBean` replaced voi `@TestConfiguration` (Spring Boot 3.4.0+)
- [ ] `Mockito.reset()` trong `@BeforeEach`
- [ ] `@SuppressWarnings("resource")` hoac `@Container` tren Testcontainers
- [ ] `.withReuse(true)` tren tat ca containers
- [ ] Dung `assertThatCode()` thay `assertThatNoException()`
- [ ] MapStruct: `@Mapping(target = "...", ignore = true)` cho unmapped fields
- [ ] Dung `List.of()` thay `Arrays.asList()`

### Frontend

- [ ] Tat ca components moi co tests
- [ ] Coverage >= 80% (`pnpm test:coverage`)
- [ ] MSW mock handlers cho API calls
- [ ] Test loading, error, empty, success states
- [ ] Khong co `.skip` hoac `.only`
- [ ] `pnpm test` pass voi 0 failures

### Common Fixes

| Warning | Fix |
|---------|-----|
| Resource leak (Testcontainers) | `@SuppressWarnings("resource")` hoac `@Container` |
| `@MockBean` deprecated | Migrate to `@TestConfiguration` |
| `argThat()` undefined | Add `import static org.mockito.ArgumentMatchers.argThat` |
| `assertThatNoException()` undefined | Dung `assertThatCode().doesNotThrowAnyException()` |
| MapStruct unmapped properties | Add `@Mapping(target = "...", ignore = true)` |
| Wildcard import `lombok.*` | Use explicit imports |
