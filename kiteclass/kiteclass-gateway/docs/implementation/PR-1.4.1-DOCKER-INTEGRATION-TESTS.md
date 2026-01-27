# PR 1.4.1: Docker Setup & Integration Tests

**Status:** 📋 PLANNED
**Priority:** ⚠️ HIGH
**Estimated Effort:** 1-2 days
**Depends On:** PR 1.4 (Auth Module)

---

## 🎯 Problem Statement

After completing PR 1.4 (Auth Module), we have:
- ✅ **Unit tests:** 42/42 PASSED (100% coverage on business logic)
- ❌ **Integration tests:** 3/13 PASSED (need Docker/Testcontainers)

**Failed Integration Tests:**
1. **AuthControllerTest** - 0/9 tests passed
   - Issue: Requires full Spring context with database
   - Uses `@WebFluxTest` (lightweight, no DB)
   - Needs refactor to `@SpringBootTest` with Testcontainers

2. **UserRepositoryTest** - 0/1 test passed
   - Issue: Needs Testcontainers for PostgreSQL
   - Docker daemon must be running

**Why This Matters:**
- Integration tests verify Spring wiring and database interactions
- Catches configuration issues that unit tests miss
- Required for CI/CD pipeline
- Ensures production readiness

---

## 📋 Scope

### 1. Add Docker Compose for Local Development

**Files to Create:**
- `docker-compose.yml` (root folder)
- `docker-compose.dev.yml` (development overrides)
- `.env.example` (environment variables template)

**Services:**
```yaml
services:
  postgres:
    image: postgres:15-alpine
    ports: 5432:5432
    environment:
      POSTGRES_DB: kiteclass_dev
      POSTGRES_USER: kiteclass
      POSTGRES_PASSWORD: kiteclass123
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports: 6379:6379

  gateway:
    build: .
    ports: 8080:8080
    depends_on:
      - postgres
      - redis
    environment:
      DB_HOST: postgres
      REDIS_HOST: redis
```

**Benefits:**
- Easy local development setup
- Consistent environment across team
- No need to install PostgreSQL/Redis locally
- Matches production environment

---

### 2. Fix AuthControllerTest (9 tests)

**Current Implementation:**
```java
@WebFluxTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {
    @MockBean private AuthService authService;
    // ...
}
```

**Problem:**
- `@WebFluxTest` doesn't load full Spring context
- SecurityConfig requires database connection
- JwtTokenProvider needs to be initialized

**Solution: Refactor to Use @SpringBootTest**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private WebTestClient webClient;

    @Test
    void shouldLoginSuccessfully() {
        // Test with real database and full Spring context
        webClient.post()
            .uri("/api/v1/auth/login")
            .bodyValue(new LoginRequest("owner@kiteclass.local", "Admin@123"))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.accessToken").exists();
    }
}
```

**Changes Required:**
1. Rename: `AuthControllerTest` → `AuthControllerIntegrationTest`
2. Replace `@WebFluxTest` with `@SpringBootTest`
3. Add `@Testcontainers` annotation
4. Add PostgreSQL container setup
5. Remove `@MockBean` - use real beans
6. Update test assertions for real data

---

### 3. Fix UserRepositoryTest (1 test)

**Current Implementation:**
```java
@DataR2dbcTest
@Testcontainers
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    // Test assumes Flyway migrations run, but they don't in @DataR2dbcTest
}
```

**Problem:**
- `@DataR2dbcTest` doesn't run Flyway migrations
- Test expects tables to exist (they don't)
- Need to either:
  - a) Add Flyway dependency to test
  - b) Use `@SpringBootTest` instead

**Solution: Enable Flyway in Tests**
```java
@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", () ->
            "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/test");
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        // Flyway migrations have run, tables exist
        StepVerifier.create(userRepository.findByEmail("owner@kiteclass.local"))
            .expectNextMatches(user -> user.getEmail().equals("owner@kiteclass.local"))
            .verifyComplete();
    }
}
```

---

### 4. Setup CI/CD with Docker

**GitHub Actions Workflow:**
```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Start Docker containers
        run: docker-compose up -d postgres redis

      - name: Wait for PostgreSQL
        run: |
          until docker exec $(docker ps -q -f name=postgres) pg_isready; do
            sleep 1
          done

      - name: Run tests
        run: ./mvnw clean verify

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml

      - name: Stop Docker containers
        run: docker-compose down
```

---

### 5. Add More Integration Test Scenarios

**New Test Classes to Create:**

**JwtAuthenticationIntegrationTest:**
- Test full authentication flow with real tokens
- Verify JWT filter extracts correct user info
- Test token expiration handling
- Test refresh token rotation

**AccountLockingIntegrationTest:**
- Test 5 failed login attempts → account locks
- Test account unlocks after 30 minutes
- Test successful login resets failed attempts counter

**RolePermissionIntegrationTest:**
- Test RBAC with real database
- Verify role-permission mappings
- Test access control on endpoints

**RefreshTokenIntegrationTest:**
- Test refresh token stored in database
- Test token rotation on refresh
- Test refresh token expiration
- Test logout invalidates refresh token

---

## 📁 Files to Create/Modify

### New Files:
```
kiteclass-gateway/
├── docker-compose.yml                                         # NEW
├── docker-compose.dev.yml                                     # NEW
├── .env.example                                               # NEW
├── .github/workflows/test.yml                                 # NEW
├── docs/guides/DOCKER-SETUP.md                                # NEW
└── src/test/java/com/kiteclass/gateway/
    ├── integration/                                           # NEW FOLDER
    │   ├── AuthControllerIntegrationTest.java                 # NEW
    │   ├── UserRepositoryIntegrationTest.java                 # NEW
    │   ├── JwtAuthenticationIntegrationTest.java              # NEW
    │   ├── AccountLockingIntegrationTest.java                 # NEW
    │   ├── RolePermissionIntegrationTest.java                 # NEW
    │   └── RefreshTokenIntegrationTest.java                   # NEW
    └── config/
        └── TestcontainersConfiguration.java                   # NEW
```

### Modified Files:
```
├── pom.xml                                                    # Add testcontainers dependency
├── src/test/resources/application-test.yml                    # Update test config
├── README.md                                                  # Add Docker setup section
├── docs/guides/TESTING.md                                     # Update with Docker instructions
└── scripts/test/run-tests.sh                                  # Add Docker check
```

---

## 🔧 Implementation Steps

### Phase 1: Docker Setup (Day 1 Morning)
1. Create `docker-compose.yml` with postgres, redis, gateway
2. Create `.env.example` with all environment variables
3. Test local development with Docker
4. Update README.md with Docker setup instructions
5. Create `docs/guides/DOCKER-SETUP.md`

### Phase 2: Fix Existing Tests (Day 1 Afternoon)
1. Refactor AuthControllerTest → AuthControllerIntegrationTest
2. Update UserRepositoryTest with Flyway
3. Run tests and verify all pass
4. Update test documentation

### Phase 3: Add New Integration Tests (Day 2 Morning)
1. Create integration test folder structure
2. Implement JwtAuthenticationIntegrationTest
3. Implement AccountLockingIntegrationTest
4. Implement RolePermissionIntegrationTest
5. Implement RefreshTokenIntegrationTest

### Phase 4: CI/CD Setup (Day 2 Afternoon)
1. Create GitHub Actions workflow
2. Test CI/CD pipeline
3. Setup code coverage reporting
4. Document CI/CD process

---

## ✅ Success Criteria

- [ ] Docker Compose setup complete and documented
- [ ] All 13 integration tests pass (100%)
  - [ ] AuthController: 9/9 tests passing
  - [ ] UserRepository: 1/1 test passing
  - [ ] New integration tests: 3+ tests passing
- [ ] CI/CD pipeline working with Docker
- [ ] Code coverage report generated
- [ ] Documentation updated (README, TESTING guide, DOCKER-SETUP guide)
- [ ] Local development environment easy to setup (< 5 minutes)

---

## 📊 Expected Results

### Before PR 1.4.1:
- Unit tests: 42/42 PASSED (100%)
- Integration tests: 3/13 PASSED (23%)
- **Total: 45/55 PASSED (82%)**

### After PR 1.4.1:
- Unit tests: 42/42 PASSED (100%)
- Integration tests: 13/13 PASSED (100%)
- New integration tests: 5/5 PASSED (100%)
- **Total: 60/60 PASSED (100%)**

---

## 📚 References

- [Test Results (Current)](../test-reports/TEST-RESULTS-FINAL.md)
- [PR 1.4 Summary](../pr-summaries/PR-1.4-SUMMARY.md)
- [Testing Guide](../guides/TESTING.md)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

---

## 🚀 How to Start This PR

### For Claude:
```
I want to implement PR 1.4.1 (Docker Setup & Integration Tests).

Please read:
1. /path/to/docs/implementation/PR-1.4.1-DOCKER-INTEGRATION-TESTS.md
2. /path/to/docs/test-reports/TEST-RESULTS-FINAL.md

Then follow the implementation steps in Phase 1-4.
```

### For Human Developer:
1. Read this document completely
2. Ensure Docker Desktop is installed and running
3. Checkout new branch: `git checkout -b feature/pr-1.4.1-docker-tests`
4. Follow Phase 1-4 implementation steps
5. Run tests: `./mvnw clean verify`
6. Create PR when all tests pass

---

**Created:** 2026-01-26
**Author:** VictorAurelius + Claude Sonnet 4.5
**Type:** Implementation Plan
**Related:** PR 1.4 (Auth Module)
