# Code Review & Fix Plan

**Date:** 2026-01-30
**Version:** 2.0 (Based on Actual Implementation Analysis)
**Scope:** Review & fix 14 implemented PRs (Gateway 6, Core 4, Frontend 4)

**⚠️ IMPORTANT:** This plan reviews EXISTING code only. Does NOT implement new features.

---

## Executive Summary

### Plan Purpose

Create **REVIEW PRs** to fix bugs, add missing tests, and improve quality of implemented code. **NO NEW FEATURES** - only review and fix existing implementations.

### Critical Issues Found

**From Analysis Report (`implemented-code-analysis.md`):**
- ❌ 0 security tests (tenant isolation, OWASP Top 10)
- ❌ 0 multi-tenant tests
- ❌ Test coverage < 50% (target: 80%)
- ⚠️ Deprecated API usage (@MockBean)
- ⚠️ Missing PR 1.8 (cross-service integration)

**Review PRs Needed:** 12 PRs
**Effort:** 10-12 days
**Priority:** URGENT (before continuing with new features)

---

## PART 1: GATEWAY SERVICE REVIEW PRs

### PR-REVIEW-1.1: Gateway Security Tests ⚠️ CRITICAL

**Branch:** `review-gateway-security`
**Priority:** P0 (URGENT)
**Effort:** 3 days
**Depends On:** None

**Scope:**
Add security tests for Auth Module (PR 1.4), User Module (PR 1.3), Rate Limiting (PR 1.6)

**Files to Create/Modify:**
```
kiteclass-gateway/src/test/java/com/kiteclass/gateway/
├── module/auth/
│   ├── JwtSecurityTest.java (NEW - 6 tests)
│   ├── PasswordPolicyTest.java (NEW - 4 tests)
│   └── AccountLockoutTest.java (NEW - 3 tests)
├── module/user/
│   └── UserSecurityTest.java (NEW - 5 tests)
└── filter/
    └── RateLimitSecurityTest.java (NEW - 3 tests)
```

**Tasks:**

1. **JWT Security Tests** (6 tests)
```java
// JwtSecurityTest.java
@SpringBootTest
class JwtSecurityTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldRejectExpiredToken() {
        // Given: Token expired 1 hour ago
        String expiredToken = generateExpiredToken();

        // When/Then: Should throw TokenExpiredException
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
            .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void shouldRejectInvalidSignature() {
        // Given: Token with tampered signature
        String validToken = generateValidToken();
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "HACK";

        // When/Then: Should throw InvalidTokenException
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(tamperedToken))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void logoutShouldBlacklistToken() {
        // Given: Valid access token + refresh token
        String accessToken = generateAccessToken();
        String refreshToken = generateRefreshToken();

        // When: User logs out
        authService.logout(refreshToken).block();

        // Then: Access token should be blacklisted
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(accessToken))
            .isInstanceOf(TokenBlacklistedException.class);
    }

    @Test
    void refreshTokenShouldRotate() {
        // Given: Refresh token
        String refreshToken = generateRefreshToken();

        // When: Use refresh token
        AuthResponse response1 = authService.refreshAccessToken(refreshToken).block();

        // Then: Old refresh token should be invalidated
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
            .isInstanceOf(RefreshTokenUsedException.class);

        // New refresh token should work
        assertThatCode(() -> authService.refreshAccessToken(response1.refreshToken()))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectTokenWithWrongTenant() {
        // Given: Token for tenant1
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();
        String token = generateTokenForTenant(tenant1);

        // When: Validate for tenant2
        // Then: Should throw TenantMismatchException
        assertThatThrownBy(() -> jwtService.validateTokenForTenant(token, tenant2))
            .isInstanceOf(TenantMismatchException.class);
    }

    @Test
    void shouldPreventTokenReuse() {
        // Given: Token used in request
        String token = generateAccessToken();
        jwtTokenProvider.validateToken(token);

        // When: Try to reuse token after logout
        authService.logout(generateRefreshToken()).block();

        // Then: Should be blacklisted
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
            .isInstanceOf(TokenBlacklistedException.class);
    }
}
```

2. **Password Policy Tests** (4 tests)
```java
// PasswordPolicyTest.java
@SpringBootTest
class PasswordPolicyTest {

    @Autowired
    private AuthService authService;

    @Test
    void registerShouldEnforceMinPasswordLength() {
        // When/Then: Password < 8 chars should fail
        RegisterRequest request = new RegisterRequest("user@test.com", "Pass1!", "User");

        assertThatThrownBy(() -> authService.register(request).block())
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("at least 8 characters");
    }

    @Test
    void registerShouldRequireUppercaseLowercaseNumber() {
        // When/Then: Missing uppercase
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@test.com", "password123!", "User")))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("uppercase");

        // When/Then: Missing number
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@test.com", "Password!", "User")))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("number");
    }

    @Test
    void registerShouldRequireSpecialCharacter() {
        // When/Then: Missing special character
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@test.com", "Password123", "User")))
            .isInstanceOf(WeakPasswordException.class)
            .hasMessageContaining("special character");
    }

    @Test
    void registerShouldHashPasswordWithBcrypt() {
        // Given: Valid password
        RegisterRequest request = new RegisterRequest("user@test.com", "Pass123!@#", "User");

        // When: Register user
        AuthResponse response = authService.register(request).block();

        // Then: Password should be hashed
        User user = userRepository.findByEmail("user@test.com").block();
        assertThat(user.getPasswordHash()).startsWith("$2a$"); // BCrypt prefix
        assertThat(user.getPasswordHash()).isNotEqualTo("Pass123!@#");
        assertThat(user.getPasswordHash()).hasSize(60); // BCrypt hash length
    }
}
```

3. **Account Lockout Tests** (3 tests)
```java
// AccountLockoutTest.java
@SpringBootTest
@Transactional
class AccountLockoutTest {

    @Autowired
    private AuthService authService;

    @Test
    void loginShouldLockAccountAfter5FailedAttempts() {
        // Given: User registered
        authService.register(new RegisterRequest("user@test.com", "Pass123!@#", "User")).block();

        // When: 5 failed login attempts
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(new LoginRequest("user@test.com", "wrong-password")).block();
            } catch (InvalidCredentialsException ignored) {}
        }

        // Then: 6th attempt should throw AccountLockedException
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@test.com", "wrong-password")))
            .isInstanceOf(AccountLockedException.class)
            .hasMessageContaining("5 failed attempts")
            .hasMessageContaining("15 minutes");
    }

    @Test
    void loginShouldUnlockAccountAfterLockoutPeriod() throws InterruptedException {
        // Given: Account locked
        // ... same setup as above

        // When: Wait for lockout period (15 minutes)
        Thread.sleep(15 * 60 * 1000 + 1000); // 15min + 1sec

        // Then: Should be able to login with correct password
        assertThatCode(() -> authService.login(
                new LoginRequest("user@test.com", "Pass123!@#")))
            .doesNotThrowAnyException();
    }

    @Test
    void loginShouldResetFailedAttemptsOnSuccess() {
        // Given: 3 failed attempts
        for (int i = 0; i < 3; i++) {
            try {
                authService.login(new LoginRequest("user@test.com", "wrong")).block();
            } catch (InvalidCredentialsException ignored) {}
        }

        // When: Successful login
        authService.login(new LoginRequest("user@test.com", "Pass123!@#")).block();

        // Then: Failed attempt counter should reset
        // (Next 5 failures should lock, not 2)
        for (int i = 0; i < 5; i++) {
            try {
                authService.login(new LoginRequest("user@test.com", "wrong")).block();
            } catch (InvalidCredentialsException ignored) {}
        }

        // This should now lock (because counter was reset)
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("user@test.com", "wrong")))
            .isInstanceOf(AccountLockedException.class);
    }
}
```

4. **OWASP SQL Injection Tests** (5 tests)
```java
// UserSecurityTest.java
@SpringBootTest
@Transactional
class UserSecurityTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void searchUsersShouldPreventSqlInjection() {
        // Given: Malicious search input
        String maliciousInput = "'; DROP TABLE users; --";

        // When: Search with malicious input
        Flux<UserResponse> results = userService.searchUsers(maliciousInput, Pageable.unpaged());

        // Then: Should return empty (no match), not drop table
        StepVerifier.create(results.collectList())
            .assertNext(list -> assertThat(list).isEmpty())
            .verifyComplete();

        // Verify: Table still exists
        StepVerifier.create(userRepository.count())
            .assertNext(count -> assertThat(count).isGreaterThan(0))
            .verifyComplete();
    }

    @Test
    void createUserShouldEscapeSpecialCharacters() {
        // Given: Input with SQL special characters
        String name = "O'Brien";
        String email = "user'; DELETE FROM users; --@example.com";

        CreateUserRequest request = new CreateUserRequest(name, email, "Pass123!@#");

        // When: Create user
        UserResponse response = userService.createUser(request).block();

        // Then: Should be saved safely (no SQL injection)
        assertThat(response.name()).isEqualTo("O'Brien");
        assertThat(response.email()).contains("DELETE"); // Stored as literal string

        // Verify: No tables dropped
        StepVerifier.create(userRepository.count())
            .assertNext(count -> assertThat(count).isEqualTo(1))
            .verifyComplete();
    }

    // ... 3 more SQL injection tests
}
```

5. **Rate Limiting Security Tests** (3 tests)
```java
// RateLimitSecurityTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class RateLimitSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldEnforceIpRateLimit() {
        // Given: 100 requests per minute limit
        String clientIp = "192.168.1.1";

        // When: Make 101 requests from same IP
        for (int i = 0; i < 100; i++) {
            webTestClient.get()
                .uri("/api/v1/users")
                .header("X-Forwarded-For", clientIp)
                .exchange()
                .expectStatus().isOk();
        }

        // Then: 101st request should be rate limited
        webTestClient.get()
            .uri("/api/v1/users")
            .header("X-Forwarded-For", clientIp)
            .exchange()
            .expectStatus().isEqualTo(429) // Too Many Requests
            .expectHeader().exists("X-RateLimit-Remaining")
            .expectHeader().valueEquals("X-RateLimit-Remaining", "0")
            .expectHeader().exists("Retry-After");
    }

    @Test
    void shouldEnforceUserRateLimit() {
        // Given: Authenticated user, 1000 requests per minute limit
        String accessToken = generateAccessToken();

        // When: Make 1001 requests
        for (int i = 0; i < 1000; i++) {
            webTestClient.get()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk();
        }

        // Then: 1001st should be rate limited
        webTestClient.get()
            .uri("/api/v1/users")
            .header("Authorization", "Bearer " + accessToken)
            .exchange()
            .expectStatus().isEqualTo(429);
    }

    @Test
    void shouldNotRateLimitHealthCheck() {
        // Given: Health check endpoint
        // When: Make 10000 requests to /actuator/health
        for (int i = 0; i < 10000; i++) {
            webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
        }

        // Then: Should never be rate limited
    }
}
```

**Deliverables:**
- ✅ 21 new security tests
- ✅ JWT token blacklist implementation
- ✅ Password policy validation
- ✅ Account lockout mechanism (15min)
- ✅ SQL injection prevention verified
- ✅ Rate limiting security verified

**Verification:**
```bash
cd kiteclass-gateway
./mvnw test -Dtest="*SecurityTest"
# All tests must pass (21/21)
```

**Success Criteria:**
- [ ] All 21 security tests passing
- [ ] No security vulnerabilities found
- [ ] OWASP Top 10 coverage improved

---

### PR-REVIEW-1.2: Gateway Test Coverage Improvement

**Branch:** `review-gateway-test-coverage`
**Priority:** P1 (HIGH)
**Effort:** 2 days
**Depends On:** PR-REVIEW-1.1

**Scope:**
Improve test coverage from ~40% to 80% for Gateway modules

**Current State:**
- 39 tests total (unit + integration)
- Estimated coverage: ~40%
- Missing: Repository tests, integration tests

**Tasks:**

1. **Add Repository Tests** (10 tests)
```java
// UserRepositoryTest.java
@DataR2dbcTest
@Import(R2dbcConfig.class)
@Testcontainers
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> postgres.getJdbcUrl().replace("jdbc:", "r2dbc:"));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailShouldReturnUser_whenExists() {
        // Given: User saved
        User user = User.builder()
            .email("test@example.com")
            .passwordHash("hash")
            .build();
        user = userRepository.save(user).block();

        // When: Find by email
        User found = userRepository.findByEmail("test@example.com").block();

        // Then: Should return user
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(user.getId());
    }

    @Test
    void existsByEmailAndDeletedFalseShouldReturnTrue_whenEmailExists() {
        // Given: User with email
        userRepository.save(User.builder()
            .email("exists@test.com")
            .passwordHash("hash")
            .deleted(false)
            .build()).block();

        // When: Check exists
        Boolean exists = userRepository.existsByEmailAndDeletedFalse("exists@test.com").block();

        // Then: Should return true
        assertThat(exists).isTrue();
    }

    @Test
    void findByIdAndDeletedFalseShouldReturnEmpty_whenDeleted() {
        // Given: Deleted user
        User user = userRepository.save(User.builder()
            .email("deleted@test.com")
            .passwordHash("hash")
            .deleted(true)
            .build()).block();

        // When: Find by ID
        User found = userRepository.findByIdAndDeletedFalse(user.getId()).block();

        // Then: Should return empty (soft delete filter)
        assertThat(found).isNull();
    }

    // ... 7 more repository tests
}
```

2. **Add Integration Tests** (15 tests)
```java
// AuthIntegrationTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void registerShouldCreateUserAndReturnTokens() {
        // When: Register new user
        webTestClient.post()
            .uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "newuser@test.com",
                    "password": "Pass123!@#",
                    "name": "New User"
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.data.accessToken").exists()
            .jsonPath("$.data.refreshToken").exists()
            .jsonPath("$.data.userId").exists();

        // Then: User should exist in database
        // (Verify via GET /api/v1/auth/me)
    }

    @Test
    void loginShouldReturnTokens_withValidCredentials() {
        // Given: User registered
        registerUser("login@test.com", "Pass123!@#");

        // When: Login
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "login@test.com",
                    "password": "Pass123!@#"
                }
                """)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.accessToken").exists()
            .jsonPath("$.data.refreshToken").exists();
    }

    @Test
    void logoutShouldBlacklistTokens() {
        // Given: Logged in user
        AuthResponse auth = registerAndLogin("logout@test.com", "Pass123!@#");

        // When: Logout
        webTestClient.post()
            .uri("/api/v1/auth/logout")
            .header("Authorization", "Bearer " + auth.accessToken())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "refreshToken": "%s"
                }
                """.formatted(auth.refreshToken()))
            .exchange()
            .expectStatus().isOk();

        // Then: Access token should be blacklisted
        webTestClient.get()
            .uri("/api/v1/auth/me")
            .header("Authorization", "Bearer " + auth.accessToken())
            .exchange()
            .expectStatus().isUnauthorized();
    }

    // ... 12 more integration tests
}
```

**Deliverables:**
- ✅ 25 new tests (10 repository + 15 integration)
- ✅ Total Gateway tests: 64 (39 existing + 25 new)
- ✅ Coverage >= 80% (verified with JaCoCo)
- ✅ JaCoCo HTML report

**Verification:**
```bash
cd kiteclass-gateway
./mvnw verify
# Open target/site/jacoco/index.html
# Verify: Line coverage >= 80%, Branch coverage >= 75%
```

---

### PR-REVIEW-1.3: Fix Deprecated APIs

**Branch:** `review-gateway-deprecated-apis`
**Priority:** P2 (MEDIUM)
**Effort:** 1 day
**Depends On:** None

**Scope:**
Fix all deprecated API usage in Gateway tests (Spring Boot 3.4+ compliance)

**Issue:**
```java
// UserControllerTest.java (CURRENT - DEPRECATED)
@WebMvcTest(UserController.class)
class UserControllerTest {
    @MockBean // ❌ DEPRECATED in Spring Boot 3.4+
    private UserService userService;
}
```

**Fix:**
```java
// UserControllerTest.java (FIXED)
@WebMvcTest(UserController.class)
@Import(UserControllerTest.MockConfig.class)
class UserControllerTest {

    @Autowired
    private UserService userService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }
    }
}
```

**Files to Fix:**
- `UserControllerTest.java`
- `AuthControllerTest.java`
- `RefreshTokenRepositoryTest.java`
- `RoleRepositoryTest.java`
- (Est. 10 test files)

**Deliverables:**
- ✅ All @MockBean replaced with @TestConfiguration
- ✅ 0 deprecation warnings in Maven build
- ✅ All tests still passing

**Verification:**
```bash
cd kiteclass-gateway
./mvnw clean compile -Xlint:deprecation
# Should show 0 deprecation warnings
```

---

### PR-REVIEW-1.4: Implement PR 1.8 (Cross-Service Integration) ⚠️ CRITICAL BLOCKER

**Branch:** `review-gateway-cross-service`
**Priority:** P0 (URGENT - BLOCKING ALL USER FLOWS)
**Effort:** 3 days
**Depends On:** PR 2.11 (COMPLETE ✅)

**Scope:**
Implement missing UserType + ReferenceId pattern for linking Gateway User → Core Student/Teacher/Parent

**Critical Impact:**
- ❌ Students CANNOT register (no Student entity created in Core)
- ❌ Teachers CANNOT login (no Teacher profile linked)
- ❌ Parents CANNOT access system

**Tasks:**

1. **Database Migration** (V008)
```sql
-- V008__add_user_type_and_reference.sql
ALTER TABLE users ADD COLUMN user_type VARCHAR(20);
ALTER TABLE users ADD COLUMN reference_id VARCHAR(100);

CREATE INDEX idx_users_reference ON users(user_type, reference_id);

-- Add CHECK constraint
ALTER TABLE users ADD CONSTRAINT chk_user_type
    CHECK (user_type IN ('ADMIN', 'STAFF', 'TEACHER', 'PARENT', 'STUDENT'));

-- Update existing users to ADMIN type
UPDATE users SET user_type = 'ADMIN' WHERE user_type IS NULL;

-- Make NOT NULL after migration
ALTER TABLE users ALTER COLUMN user_type SET NOT NULL;
```

2. **Create UserType Enum**
```java
// UserType.java
package com.kiteclass.gateway.common.constant;

public enum UserType {
    ADMIN("System administrator with full access"),
    STAFF("Administrative staff (non-teaching)"),
    TEACHER("Teacher with classroom management access"),
    PARENT("Parent with student portal access"),
    STUDENT("Student with learning portal access");

    private final String description;

    UserType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canManageStudents() {
        return this == ADMIN || this == STAFF || this == TEACHER;
    }

    public boolean canAccessParentPortal() {
        return this == PARENT;
    }

    public boolean canAccessStudentPortal() {
        return this == STUDENT;
    }
}
```

3. **Update User Entity**
```java
// User.java
@Table("users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "user_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType; // ✅ NEW

    @Column(name = "reference_id")
    private String referenceId; // ✅ NEW - Links to Core entity ID

    // ... existing fields
}
```

4. **Implement Feign Client**
```java
// CoreServiceClient.java
package com.kiteclass.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "kiteclass-core", url = "${core.service.url}")
public interface CoreServiceClient {

    @PostMapping("/internal/students")
    StudentProfileResponse createStudent(
        @RequestHeader("X-Internal-Request") String internalToken,
        @RequestBody CreateStudentRequest request
    );

    @GetMapping("/internal/students/{id}")
    StudentProfileResponse getStudent(
        @RequestHeader("X-Internal-Request") String internalToken,
        @PathVariable("id") Long id
    );

    @DeleteMapping("/internal/students/{id}")
    void deleteStudent(
        @RequestHeader("X-Internal-Request") String internalToken,
        @PathVariable("id") Long id
    );

    // Similar for Teacher, Parent
    @PostMapping("/internal/teachers")
    TeacherProfileResponse createTeacher(...);

    @GetMapping("/internal/teachers/{id}")
    TeacherProfileResponse getTeacher(...);
}
```

5. **Update AuthService with Saga Pattern**
```java
// AuthServiceImpl.java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CoreServiceClient coreServiceClient;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    @Override
    public Mono<AuthResponse> registerStudent(StudentRegistrationRequest request) {
        return Mono.defer(() -> {
            // Step 1: Create User in Gateway
            User user = User.builder()
                .email(request.email())
                .passwordHash(hashPassword(request.password()))
                .userType(UserType.STUDENT)
                .build();

            return userRepository.save(user)
                .flatMap(savedUser -> {
                    // Step 2: Create Student in Core (Feign call)
                    CreateStudentRequest studentRequest = new CreateStudentRequest(
                        request.name(),
                        request.email(),
                        request.phone(),
                        request.dateOfBirth(),
                        request.gender(),
                        request.address()
                    );

                    try {
                        StudentProfileResponse profile = coreServiceClient.createStudent(
                            "true", // X-Internal-Request header
                            studentRequest
                        );

                        // Step 3: Link User to Student
                        savedUser.setReferenceId(profile.id().toString());
                        return userRepository.save(savedUser);

                    } catch (FeignException ex) {
                        // Saga rollback: Delete User if Student creation failed
                        return userRepository.deleteById(savedUser.getId())
                            .then(Mono.error(new RegistrationFailedException(
                                "Failed to create student profile in Core service", ex)));
                    }
                })
                .flatMap(linkedUser -> {
                    // Step 4: Generate JWT tokens
                    String accessToken = jwtTokenProvider.generateAccessToken(linkedUser);
                    String refreshToken = jwtTokenProvider.generateRefreshToken(linkedUser);

                    return Mono.just(new AuthResponse(
                        accessToken,
                        refreshToken,
                        linkedUser.getId(),
                        linkedUser.getUserType(),
                        linkedUser.getReferenceId()
                    ));
                });
        });
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
            .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
            .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
            .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
            .flatMap(user -> {
                // Fetch profile from Core if not ADMIN
                if (user.getUserType() != UserType.ADMIN) {
                    return fetchProfileFromCore(user)
                        .flatMap(profile -> generateAuthResponse(user, profile));
                }
                return generateAuthResponse(user, null);
            });
    }

    private Mono<Object> fetchProfileFromCore(User user) {
        return Mono.fromCallable(() -> {
            Long referenceId = Long.parseLong(user.getReferenceId());
            return switch (user.getUserType()) {
                case STUDENT -> coreServiceClient.getStudent("true", referenceId);
                case TEACHER -> coreServiceClient.getTeacher("true", referenceId);
                case PARENT -> coreServiceClient.getParent("true", referenceId);
                default -> null;
            };
        });
    }

    private Mono<AuthResponse> generateAuthResponse(User user, Object profile) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return Mono.just(new AuthResponse(
            accessToken,
            refreshToken,
            user.getId(),
            user.getUserType(),
            user.getReferenceId(),
            profile
        ));
    }
}
```

6. **Add Integration Tests** (15 tests)
```java
// StudentRegistrationIntegrationTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class StudentRegistrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CoreServiceClient coreServiceClient;

    @Test
    void registerStudentShouldCreateUserAndStudentProfile() {
        // Given: Mock Core response
        StudentProfileResponse mockProfile = new StudentProfileResponse(
            123L, "John Doe", "john@student.com", "0901234567"
        );
        when(coreServiceClient.createStudent(eq("true"), any()))
            .thenReturn(mockProfile);

        // When: Register student
        webTestClient.post()
            .uri("/api/v1/auth/register/student")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "name": "John Doe",
                    "email": "john@student.com",
                    "password": "Pass123!@#",
                    "phone": "0901234567"
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.data.accessToken").exists()
            .jsonPath("$.data.refreshToken").exists()
            .jsonPath("$.data.userType").isEqualTo("STUDENT")
            .jsonPath("$.data.referenceId").isEqualTo("123");

        // Then: User should be created with referenceId
        User user = userRepository.findByEmail("john@student.com").block();
        assertThat(user).isNotNull();
        assertThat(user.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(user.getReferenceId()).isEqualTo("123");

        // Verify: Core API was called
        verify(coreServiceClient).createStudent(eq("true"), any());
    }

    @Test
    void registerStudentShouldRollbackIfCoreFails() {
        // Given: Core throws exception
        when(coreServiceClient.createStudent(eq("true"), any()))
            .thenThrow(FeignException.InternalServerError.class);

        // When: Register student
        webTestClient.post()
            .uri("/api/v1/auth/register/student")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "name": "John Doe",
                    "email": "john@student.com",
                    "password": "Pass123!@#"
                }
                """)
            .exchange()
            .expectStatus().is5xxServerError();

        // Then: User should NOT be created (rollback)
        User user = userRepository.findByEmail("john@student.com").block();
        assertThat(user).isNull();
    }

    @Test
    void loginShouldFetchStudentProfileFromCore() {
        // Given: Student user exists with referenceId
        User student = User.builder()
            .email("student@test.com")
            .passwordHash(hashPassword("Pass123!@#"))
            .userType(UserType.STUDENT)
            .referenceId("123")
            .build();
        userRepository.save(student).block();

        // Mock Core response
        when(coreServiceClient.getStudent(eq("true"), eq(123L)))
            .thenReturn(new StudentProfileResponse(123L, "John Doe", "student@test.com", null));

        // When: Login
        webTestClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                    "email": "student@test.com",
                    "password": "Pass123!@#"
                }
                """)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.profile.name").isEqualTo("John Doe")
            .jsonPath("$.data.profile.id").isEqualTo(123);

        // Verify: Core API was called
        verify(coreServiceClient).getStudent(eq("true"), eq(123L));
    }

    // ... 12 more integration tests
}
```

**Deliverables:**
- ✅ UserType enum (5 values)
- ✅ Database migration (user_type, reference_id)
- ✅ User entity updated
- ✅ CoreServiceClient (Feign)
- ✅ Registration Saga pattern (Student)
- ✅ Login with profile fetch
- ✅ 15 integration tests
- ✅ Documentation update (`.claude/skills/cross-service-data-strategy.md`)

**Verification:**
```bash
cd kiteclass-gateway
./mvnw test -Dtest="*RegistrationIntegrationTest"
./mvnw test -Dtest="*LoginIntegrationTest"
# All tests must pass (15/15)
```

**Success Criteria:**
- [ ] Students can register successfully
- [ ] User created in Gateway + Student created in Core
- [ ] Saga rollback works if Core fails
- [ ] Login fetches profile from Core
- [ ] All integration tests passing

---

## PART 2: CORE SERVICE REVIEW PRs

### PR-REVIEW-2.1: Core Multi-Tenant Security ⚠️ CRITICAL

**Branch:** `review-core-multi-tenant`
**Priority:** P0 (URGENT - DATA LEAKAGE RISK)
**Effort:** 3 days
**Depends On:** None

**Scope:**
Add multi-tenant support to all Core entities (currently missing!)

**Critical Issue:**
```java
// BaseEntity.java (CURRENT - NO TENANT ISOLATION!)
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    private Long id;
    // ❌ MISSING: private UUID instanceId;
}
```

**Fix:**

1. **Update BaseEntity**
```java
// BaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "instance_id = :tenantId")
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId; // ✅ ADDED - Multi-tenant support

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    public void markAsDeleted() {
        this.deleted = true;
    }

    // Getters/Setters
}
```

2. **Create TenantContext**
```java
// TenantContext.java
package com.kiteclass.core.common.context;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(UUID instanceId) {
        if (instanceId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        currentTenant.set(instanceId);
    }

    public static UUID getCurrentTenant() {
        UUID tenantId = currentTenant.get();
        if (tenantId == null) {
            throw new TenantNotSetException(
                "Tenant context not set for current thread. " +
                "Ensure X-Tenant-Id header is provided in request."
            );
        }
        return tenantId;
    }

    public static void clear() {
        currentTenant.remove();
    }

    public static boolean isSet() {
        return currentTenant.get() != null;
    }
}
```

3. **Create Tenant Filter Interceptor**
```java
// TenantFilterInterceptor.java
package com.kiteclass.core.config;

import com.kiteclass.core.common.context.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    @Override
    public boolean preHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler
    ) {
        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null) {
            UUID tenantId = UUID.fromString(tenantHeader);
            TenantContext.setCurrentTenant(tenantId);

            // Enable Hibernate filter
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
        }
        return true;
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception ex
    ) {
        TenantContext.clear();
    }
}
```

4. **Register Interceptor**
```java
// WebMvcConfig.java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TenantFilterInterceptor tenantFilterInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantFilterInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/v1/auth/**", "/actuator/**");
    }
}
```

5. **Database Migrations**
```sql
-- V009__add_instance_id_to_students.sql
ALTER TABLE students ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
CREATE INDEX idx_students_instance ON students(instance_id);

-- V010__add_instance_id_to_courses.sql
ALTER TABLE courses ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
CREATE INDEX idx_courses_instance ON courses(instance_id);

-- V011__add_instance_id_to_teachers.sql
ALTER TABLE teachers ADD COLUMN instance_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
CREATE INDEX idx_teachers_instance ON teachers(instance_id);

-- V012__add_instance_id_to_classes.sql
-- V013__add_instance_id_to_enrollments.sql
-- V014__add_instance_id_to_attendance.sql
-- V015__add_instance_id_to_invoices.sql
-- V016__add_instance_id_to_payments.sql
-- ... (Apply to ALL entities)
```

6. **Add Multi-Tenant Security Tests** (20 tests)
```java
// StudentServiceMultiTenantTest.java
@SpringBootTest
@Transactional
@Testcontainers
class StudentServiceMultiTenantTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withReuse(true);

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("getStudents should only return current tenant's students")
    void getStudents_shouldOnlyReturnCurrentTenantStudents() {
        // Given: Two tenants with students
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        TenantContext.setCurrentTenant(tenant1);
        studentRepository.save(Student.builder()
            .name("Tenant 1 Student")
            .email("student1@t1.com")
            .instanceId(tenant1)
            .build());

        TenantContext.setCurrentTenant(tenant2);
        studentRepository.save(Student.builder()
            .name("Tenant 2 Student")
            .email("student2@t2.com")
            .instanceId(tenant2)
            .build());

        // When: Get students as tenant1
        TenantContext.setCurrentTenant(tenant1);
        PageResponse<StudentResponse> tenant1Students =
            studentService.getStudents(null, null, Pageable.unpaged());

        // Then: Should only see tenant1's student
        assertThat(tenant1Students.getContent())
            .hasSize(1)
            .extracting(StudentResponse::name)
            .containsExactly("Tenant 1 Student");

        // Should NOT see tenant2's student
        assertThat(tenant1Students.getContent())
            .noneMatch(s -> s.email().equals("student2@t2.com"));

        TenantContext.clear();
    }

    @Test
    @DisplayName("getStudentById should throw 404 for other tenant's student")
    void getStudentById_shouldThrow404_whenAccessingOtherTenantStudent() {
        // Given: Student belongs to tenant1
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        TenantContext.setCurrentTenant(tenant1);
        Student student = studentRepository.save(Student.builder()
            .name("Tenant 1 Student")
            .email("student@t1.com")
            .instanceId(tenant1)
            .build());
        Long studentId = student.getId();
        TenantContext.clear();

        // When: Try to access as tenant2
        TenantContext.setCurrentTenant(tenant2);

        // Then: Should throw EntityNotFoundException (cross-tenant access denied)
        assertThatThrownBy(() -> studentService.getStudentById(studentId))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(studentId.toString());

        TenantContext.clear();
    }

    @Test
    @DisplayName("updateStudent should not allow cross-tenant update")
    void updateStudent_shouldNotAllowCrossTenantUpdate() {
        // Given: Student belongs to tenant1
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        TenantContext.setCurrentTenant(tenant1);
        Student student = studentRepository.save(Student.builder()
            .name("Original Name")
            .email("student@t1.com")
            .instanceId(tenant1)
            .build());
        Long studentId = student.getId();
        TenantContext.clear();

        // When: Tenant2 tries to update tenant1's student
        TenantContext.setCurrentTenant(tenant2);
        UpdateStudentRequest request = new UpdateStudentRequest(
            "Hacked Name", null, null, null, null, null, null
        );

        // Then: Should throw exception (not found in tenant2 context)
        assertThatThrownBy(() -> studentService.updateStudent(studentId, request))
            .isInstanceOf(EntityNotFoundException.class);

        // Verify: Original data unchanged
        TenantContext.clear();
        TenantContext.setCurrentTenant(tenant1);
        Student unchanged = studentRepository.findById(studentId).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Original Name");

        TenantContext.clear();
    }

    @Test
    @DisplayName("deleteStudent should not allow cross-tenant delete")
    void deleteStudent_shouldNotAllowCrossTenantDelete() {
        // Given: Student belongs to tenant1
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        TenantContext.setCurrentTenant(tenant1);
        Student student = studentRepository.save(Student.builder()
            .name("Tenant 1 Student")
            .email("student@t1.com")
            .instanceId(tenant1)
            .build());
        Long studentId = student.getId();
        TenantContext.clear();

        // When: Tenant2 tries to delete tenant1's student
        TenantContext.setCurrentTenant(tenant2);

        // Then: Should throw exception
        assertThatThrownBy(() -> studentService.deleteStudent(studentId))
            .isInstanceOf(EntityNotFoundException.class);

        // Verify: Student still exists
        TenantContext.clear();
        TenantContext.setCurrentTenant(tenant1);
        assertThat(studentRepository.findById(studentId)).isPresent();

        TenantContext.clear();
    }

    @Test
    @DisplayName("should prevent SQL injection via tenant context")
    void shouldPreventSqlInjection_viaTenantContext() {
        // Given: Two tenants with students
        UUID tenant1 = UUID.randomUUID();
        UUID tenant2 = UUID.randomUUID();

        TenantContext.setCurrentTenant(tenant1);
        studentRepository.save(Student.builder()
            .name("Tenant 1 Student")
            .email("student1@t1.com")
            .instanceId(tenant1)
            .build());

        TenantContext.setCurrentTenant(tenant2);
        studentRepository.save(Student.builder()
            .name("Tenant 2 Student")
            .email("student2@t2.com")
            .instanceId(tenant2)
            .build());

        // When: Attacker tries SQL injection via search
        TenantContext.setCurrentTenant(tenant1);
        String maliciousInput = "' OR instance_id IS NOT NULL --";

        PageResponse<StudentResponse> students =
            studentService.getStudents(maliciousInput, null, Pageable.unpaged());

        // Then: Should return empty (no match), not all students
        assertThat(students.getContent()).isEmpty();

        // Verify: Only tenant1's data accessible
        PageResponse<StudentResponse> tenant1Students =
            studentService.getStudents(null, null, Pageable.unpaged());
        assertThat(tenant1Students.getContent()).hasSize(1);

        TenantContext.clear();
    }

    // ... 15 more multi-tenant security tests
    // (UPDATE, DELETE, bulk operations, repository-level tests, etc.)
}
```

**Deliverables:**
- ✅ instanceId added to ALL entities (via BaseEntity)
- ✅ TenantContext ThreadLocal
- ✅ JPA @Filter for tenant isolation
- ✅ TenantFilterInterceptor (Spring MVC)
- ✅ 8 database migrations (all entities)
- ✅ 20 multi-tenant security tests
- ✅ Documentation update

**Verification:**
```bash
cd kiteclass-core
./mvnw test -Dtest="*MultiTenantTest"
# All tests must pass (20/20)
```

**Success Criteria:**
- [ ] All entities have instanceId
- [ ] Tenant filter works correctly
- [ ] Cross-tenant access prevented
- [ ] All security tests passing

---

### PR-REVIEW-2.2: Core OWASP Security Tests

**Branch:** `review-core-owasp-security`
**Priority:** P0 (CRITICAL)
**Effort:** 2 days
**Depends On:** PR-REVIEW-2.1

**Scope:**
Add OWASP Top 10 security tests for Core modules

**Tasks:**

1. **SQL Injection Tests** (5 tests per module)
```java
// StudentSecurityTest.java
@SpringBootTest
@Transactional
class StudentSecurityTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("should prevent SQL injection via search parameter")
    void shouldPreventSqlInjection_viaSearch() {
        // Given: Malicious search input
        String maliciousInput = "'; DROP TABLE students; --";

        // When: Search with malicious input
        PageResponse<StudentResponse> result =
            studentService.getStudents(maliciousInput, null, Pageable.unpaged());

        // Then: Should return empty (no match), not drop table
        assertThat(result.getContent()).isEmpty();

        // Verify: Table still exists
        assertThat(studentRepository.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("should use parameterized queries for create")
    void shouldUseParameterizedQueries_create() {
        // Given: Input with SQL special characters
        String name = "O'Brien";
        String email = "user'; DELETE FROM students; --@example.com";

        CreateStudentRequest request = new CreateStudentRequest(
            name, email, "0901234567", null, null, null
        );

        // When: Create student
        StudentResponse response = studentService.createStudent(request);

        // Then: Should be saved safely (no SQL injection)
        assertThat(response.name()).isEqualTo("O'Brien");
        assertThat(response.email()).contains("DELETE"); // Stored as literal

        // Verify: No tables dropped
        assertThat(studentRepository.count()).isEqualTo(1);
    }

    // ... 3 more SQL injection tests
}
```

2. **XSS Prevention Tests** (3 tests per module)
```java
@Test
@DisplayName("should sanitize HTML in user input")
void shouldSanitizeHtml() {
    // Given: Input with XSS payload
    String xssPayload = "<script>alert('XSS')</script>";

    CreateStudentRequest request = new CreateStudentRequest(
        xssPayload, "user@example.com", "0901234567", null, null, null
    );

    // When: Create student
    StudentResponse response = studentService.createStudent(request);

    // Then: HTML should be escaped or sanitized
    assertThat(response.name())
        .doesNotContain("<script>")
        .satisfiesAnyOf(
            name -> assertThat(name).isEqualTo("alert('XSS')"), // Script removed
            name -> assertThat(name).contains("&lt;script&gt;") // HTML encoded
        );
}
```

3. **Authorization Tests** (Apply to all modules)
```java
// CourseAuthorizationTest.java
@Test
@DisplayName("should prevent unauthorized course creation")
void shouldPreventUnauthorizedCourseCreation() {
    // Given: User without TEACHER role
    // (Mock security context with STUDENT role)

    CreateCourseRequest request = new CreateCourseRequest(...);

    // When/Then: Should throw ForbiddenException
    assertThatThrownBy(() -> courseService.createCourse(request))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("ROLE_TEACHER required");
}
```

**Deliverables:**
- ✅ 15 OWASP security tests (Student module)
- ✅ 15 OWASP security tests (Course module)
- ✅ 15 OWASP security tests (Teacher module)
- ✅ Total: 45 security tests
- ✅ Input sanitization for XSS
- ✅ Authorization enforcement

**Verification:**
```bash
cd kiteclass-core
./mvnw test -Dtest="*SecurityTest"
# All tests must pass (45/45)
```

---

### PR-REVIEW-2.3: Core Test Coverage Improvement

**Branch:** `review-core-test-coverage`
**Priority:** P1 (HIGH)
**Effort:** 2 days
**Depends On:** PR-REVIEW-2.1, PR-REVIEW-2.2

**Scope:**
Improve test coverage from ~60% to 80%

**Current State:**
- 20 tests total
- Estimated coverage: ~60%
- Missing: Integration tests, repository tests

**Tasks:**

1. **Add Integration Tests** (15 tests per module)
```java
// StudentIntegrationTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class StudentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateStudentViaApi() throws Exception {
        UUID tenantId = UUID.randomUUID();

        CreateStudentRequest request = new CreateStudentRequest(
            "John Doe", "john@student.com", "0901234567",
            LocalDate.of(2005, 1, 15), Gender.MALE, "123 Main St"
        );

        mockMvc.perform(post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", tenantId.toString())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("John Doe"))
            .andExpect(jsonPath("$.data.email").value("john@student.com"));
    }

    // ... 14 more integration tests
    // (GET, UPDATE, DELETE, search, pagination, validation errors, etc.)
}
```

2. **Add Repository Integration Tests** (10 tests per module)
```java
// StudentRepositoryTest.java
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class StudentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void findBySearchCriteria_shouldSearchByName() {
        // Given: Students with different names
        studentRepository.save(Student.builder()
            .name("John Doe")
            .email("john@test.com")
            .instanceId(UUID.randomUUID())
            .build());

        studentRepository.save(Student.builder()
            .name("Jane Smith")
            .email("jane@test.com")
            .instanceId(UUID.randomUUID())
            .build());

        // When: Search by name
        Page<Student> results = studentRepository.findBySearchCriteria(
            "John", null, Pageable.unpaged()
        );

        // Then: Should find only John
        assertThat(results.getContent())
            .hasSize(1)
            .extracting(Student::getName)
            .containsExactly("John Doe");
    }

    // ... 9 more repository tests
    // (Search by email, phone, filter by status, soft delete, etc.)
}
```

**Deliverables:**
- ✅ 45 integration tests (Student: 15, Course: 15, Teacher: 15)
- ✅ 30 repository tests (Student: 10, Course: 10, Teacher: 10)
- ✅ Total new tests: 75
- ✅ Coverage >= 80%
- ✅ JaCoCo report

**Verification:**
```bash
cd kiteclass-core
./mvnw verify
# Open target/site/jacoco/index.html
# Verify coverage >= 80%
```

---

### PR-REVIEW-2.4: Internal API Security Hardening

**Branch:** `review-core-internal-api-security`
**Priority:** P1 (HIGH)
**Effort:** 1 day
**Depends On:** None

**Scope:**
Improve internal API security (currently only header-based)

**Current Implementation:**
```java
// InternalRequestFilter.java (WEAK)
if (request.getRequestURI().startsWith("/internal/")) {
    String internalHeader = request.getHeader("X-Internal-Request");
    if (!"true".equals(internalHeader)) {
        throw new ForbiddenException("INTERNAL_API_ACCESS_DENIED");
    }
}
```

**Issue:** Header can be spoofed by attacker

**Fix: Implement HMAC Signature**

```java
// InternalRequestFilter.java (IMPROVED)
@Component
public class InternalRequestFilter extends OncePerRequestFilter {

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/internal/")) {
            // Verify HMAC signature
            String signature = request.getHeader("X-Internal-Signature");
            String timestamp = request.getHeader("X-Internal-Timestamp");

            if (!isValidSignature(signature, timestamp)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"INVALID_INTERNAL_SIGNATURE\"}");
                return;
            }

            // Verify timestamp (prevent replay attacks)
            if (!isWithinTimeWindow(timestamp, 300)) { // 5 minutes
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"REQUEST_EXPIRED\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidSignature(String signature, String timestamp) {
        if (signature == null || timestamp == null) {
            return false;
        }

        // Calculate expected signature: HMAC-SHA256(timestamp, secret)
        String expectedSignature = HmacUtils.hmacSha256Hex(internalApiSecret, timestamp);

        // Constant-time comparison (prevent timing attacks)
        return MessageDigest.isEqual(
            signature.getBytes(StandardCharsets.UTF_8),
            expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isWithinTimeWindow(String timestamp, int windowSeconds) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            return Math.abs(currentTime - requestTime) <= windowSeconds;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
```

**Gateway Client Update:**
```java
// CoreServiceClient.java (Gateway)
@FeignClient(
    name = "kiteclass-core",
    url = "${core.service.url}",
    configuration = InternalApiConfig.class
)
public interface CoreServiceClient {
    // ... methods
}

// InternalApiConfig.java
@Configuration
public class InternalApiConfig {

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    @Bean
    public RequestInterceptor internalApiInterceptor() {
        return template -> {
            long timestamp = System.currentTimeMillis() / 1000;
            String signature = HmacUtils.hmacSha256Hex(
                internalApiSecret,
                String.valueOf(timestamp)
            );

            template.header("X-Internal-Signature", signature);
            template.header("X-Internal-Timestamp", String.valueOf(timestamp));
        };
    }
}
```

**Security Tests:**
```java
// InternalApiSecurityTest.java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class InternalApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${internal.api.secret}")
    private String internalApiSecret;

    @Test
    void shouldRejectRequestWithoutSignature() throws Exception {
        mockMvc.perform(get("/internal/students/1"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("INVALID_INTERNAL_SIGNATURE"));
    }

    @Test
    void shouldRejectRequestWithInvalidSignature() throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String invalidSignature = "invalid";

        mockMvc.perform(get("/internal/students/1")
                .header("X-Internal-Signature", invalidSignature)
                .header("X-Internal-Timestamp", timestamp))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptRequestWithValidSignature() throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String validSignature = HmacUtils.hmacSha256Hex(
            internalApiSecret,
            String.valueOf(timestamp)
        );

        mockMvc.perform(get("/internal/students/1")
                .header("X-Internal-Signature", validSignature)
                .header("X-Internal-Timestamp", timestamp))
            .andExpect(status().isOk()); // or 404 if student not found
    }

    @Test
    void shouldRejectReplayAttack() throws Exception {
        // Old timestamp (10 minutes ago)
        long oldTimestamp = (System.currentTimeMillis() / 1000) - 600;
        String signature = HmacUtils.hmacSha256Hex(
            internalApiSecret,
            String.valueOf(oldTimestamp)
        );

        mockMvc.perform(get("/internal/students/1")
                .header("X-Internal-Signature", signature)
                .header("X-Internal-Timestamp", oldTimestamp))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("REQUEST_EXPIRED"));
    }

    @Test
    void shouldPreventTimingAttacks() throws Exception {
        // Test that signature comparison is constant-time
        // (Same execution time for valid/invalid signatures)

        long timestamp = System.currentTimeMillis() / 1000;
        String correctSignature = HmacUtils.hmacSha256Hex(
            internalApiSecret,
            String.valueOf(timestamp)
        );
        String wrongSignature = "0000000000000000000000000000000000000000000000000000000000000000";

        // Measure time for correct signature
        long start1 = System.nanoTime();
        mockMvc.perform(get("/internal/students/1")
            .header("X-Internal-Signature", correctSignature)
            .header("X-Internal-Timestamp", timestamp));
        long time1 = System.nanoTime() - start1;

        // Measure time for wrong signature
        long start2 = System.nanoTime();
        mockMvc.perform(get("/internal/students/1")
            .header("X-Internal-Signature", wrongSignature)
            .header("X-Internal-Timestamp", timestamp));
        long time2 = System.nanoTime() - start2;

        // Time difference should be negligible (< 1ms)
        assertThat(Math.abs(time1 - time2)).isLessThan(1_000_000); // 1ms in nanoseconds
    }
}
```

**Deliverables:**
- ✅ HMAC-SHA256 signature-based authentication
- ✅ Replay attack prevention (5min window)
- ✅ Constant-time comparison (timing attack prevention)
- ✅ 5 security tests
- ✅ Gateway client updated with request interceptor
- ✅ Documentation (`.claude/skills/cross-service-data-strategy.md`)

**Verification:**
```bash
cd kiteclass-core
./mvnw test -Dtest="InternalApiSecurityTest"
# All tests must pass (5/5)
```

---

## PART 3: FRONTEND REVIEW PRs

### PR-REVIEW-3.1: Frontend Types & API Client

**Branch:** `review-frontend-types-api`
**Priority:** P0 (CRITICAL - BLOCKING FRONTEND DEVELOPMENT)
**Effort:** 2 days
**Depends On:** None

**Scope:**
Add missing TypeScript types and API client configuration

**Current State:**
```bash
$ ls -la src/types/
total 0  # ❌ EMPTY!
```

**Tasks:**

1. **Create Type Definitions**
```typescript
// src/types/index.ts
export * from './api';
export * from './feature-detection';
export * from './auth';
export * from './student';
export * from './course';
export * from './teacher';
```

2. **Feature Detection Types**
```typescript
// src/types/feature-detection.ts
export enum PricingTier {
  BASIC = 'BASIC',
  STANDARD = 'STANDARD',
  PREMIUM = 'PREMIUM',
}

export enum FeatureName {
  STUDENTS = 'STUDENTS',
  CLASSES = 'CLASSES',
  ATTENDANCE = 'ATTENDANCE',
  ENGAGEMENT = 'ENGAGEMENT',
  AI_BRANDING = 'AI_BRANDING',
  MEDIA = 'MEDIA',
  CUSTOM_DOMAIN = 'CUSTOM_DOMAIN',
}

export interface InstanceConfig {
  instanceId: string;
  tier: PricingTier;
  features: Record<FeatureName, boolean>;
  limitations: {
    maxStudents?: number;
    maxCourses?: number;
    maxStorage?: number; // in MB
  };
  status: InstanceStatus;
  trialDaysRemaining?: number;
  trialExpiresAt?: string;
  suspendedAt?: string;
}

export enum InstanceStatus {
  TRIAL = 'TRIAL',
  ACTIVE = 'ACTIVE',
  GRACE_PERIOD = 'GRACE_PERIOD',
  SUSPENDED = 'SUSPENDED',
}

export interface FeatureRequirement {
  feature: FeatureName;
  requiredTier: PricingTier;
  description: string;
}

// Tier definitions
export const TIER_FEATURES: Record<PricingTier, FeatureName[]> = {
  [PricingTier.BASIC]: [
    FeatureName.STUDENTS,
    FeatureName.CLASSES,
    FeatureName.ATTENDANCE,
  ],
  [PricingTier.STANDARD]: [
    FeatureName.STUDENTS,
    FeatureName.CLASSES,
    FeatureName.ATTENDANCE,
    FeatureName.ENGAGEMENT,
  ],
  [PricingTier.PREMIUM]: [
    FeatureName.STUDENTS,
    FeatureName.CLASSES,
    FeatureName.ATTENDANCE,
    FeatureName.ENGAGEMENT,
    FeatureName.AI_BRANDING,
    FeatureName.MEDIA,
    FeatureName.CUSTOM_DOMAIN,
  ],
};

export const TIER_LIMITS: Record<PricingTier, { maxStudents: number; maxCourses: number }> = {
  [PricingTier.BASIC]: { maxStudents: 50, maxCourses: 10 },
  [PricingTier.STANDARD]: { maxStudents: 200, maxCourses: 50 },
  [PricingTier.PREMIUM]: { maxStudents: Number.POSITIVE_INFINITY, maxCourses: Number.POSITIVE_INFINITY },
};
```

3. **Auth Types**
```typescript
// src/types/auth.ts
export interface User {
  id: number;
  email: string;
  name: string;
  userType: UserType;
  referenceId?: string;
}

export enum UserType {
  ADMIN = 'ADMIN',
  STAFF = 'STAFF',
  TEACHER = 'TEACHER',
  PARENT = 'PARENT',
  STUDENT = 'STUDENT',
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  userType: UserType;
  profile?: StudentProfile | TeacherProfile | ParentProfile;
}

export interface StudentProfile {
  id: number;
  name: string;
  email: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
}

export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER',
}
```

4. **API Client**
```typescript
// src/lib/api-client.ts
import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';

const apiClient: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor (add auth token + tenant ID)
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Add access token
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Add tenant ID
    const tenantId = localStorage.getItem('tenantId');
    if (tenantId && config.headers) {
      config.headers['X-Tenant-Id'] = tenantId;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor (handle token refresh)
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Attempt token refresh
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await axios.post(
          `${process.env.NEXT_PUBLIC_API_URL}/api/v1/auth/refresh`,
          { refreshToken }
        );

        const { accessToken: newAccessToken } = response.data.data;
        localStorage.setItem('accessToken', newAccessToken);

        // Retry original request with new token
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        return apiClient(originalRequest);

      } catch (refreshError) {
        // Refresh failed - logout user
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

5. **React Query Provider**
```typescript
// src/providers/ReactQueryProvider.tsx
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState, type ReactNode } from 'react';

export function ReactQueryProvider({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000, // 1 minute
            gcTime: 5 * 60 * 1000, // 5 minutes (renamed from cacheTime)
            retry: 1,
            refetchOnWindowFocus: false,
          },
          mutations: {
            retry: 0,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} position="bottom-right" />
    </QueryClientProvider>
  );
}
```

6. **Feature Detection Hook**
```typescript
// src/hooks/useFeatureDetection.ts
import { useQuery } from '@tanstack/react-query';
import apiClient from '@/lib/api-client';
import type { InstanceConfig, FeatureName } from '@/types/feature-detection';

export function useFeatureDetection() {
  const { data: config, isLoading, error } = useQuery<InstanceConfig>({
    queryKey: ['instance', 'config'],
    queryFn: async () => {
      const response = await apiClient.get('/api/v1/instance/config');
      return response.data.data;
    },
    staleTime: 60 * 60 * 1000, // 1 hour
    retry: 2,
  });

  const hasFeature = (feature: FeatureName): boolean => {
    if (!config) return false;
    return config.features[feature] === true;
  };

  const requireFeature = (feature: FeatureName): void => {
    if (!hasFeature(feature)) {
      throw new Error(
        `Feature "${feature}" is not available on your current plan. ` +
        `Please upgrade to access this feature.`
      );
    }
  };

  const getRequiredTier = (feature: FeatureName): string => {
    // Determine which tier provides this feature
    if (!config) return 'UNKNOWN';

    // Logic to determine required tier based on TIER_FEATURES
    // (Implementation based on TIER_FEATURES constant)
    return 'STANDARD'; // Simplified
  };

  return {
    config,
    isLoading,
    error,
    hasFeature,
    requireFeature,
    getRequiredTier,
    tier: config?.tier,
    status: config?.status,
    trialDaysRemaining: config?.trialDaysRemaining,
  };
}
```

7. **FeatureGate Component**
```typescript
// src/components/features/FeatureGate.tsx
import { type ReactNode } from 'react';
import { useFeatureDetection } from '@/hooks/useFeatureDetection';
import type { FeatureName } from '@/types/feature-detection';
import { UpgradePrompt } from './UpgradePrompt';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Skeleton } from '@/components/ui/skeleton';

interface FeatureGateProps {
  feature: FeatureName;
  children: ReactNode;
  fallback?: ReactNode;
  showUpgradePrompt?: boolean;
}

export function FeatureGate({
  feature,
  children,
  fallback,
  showUpgradePrompt = true,
}: FeatureGateProps) {
  const { hasFeature, isLoading, error, config, getRequiredTier } = useFeatureDetection();

  if (isLoading) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-3/4" />
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription>
          Failed to load feature configuration. Please refresh the page.
        </AlertDescription>
      </Alert>
    );
  }

  if (!hasFeature(feature)) {
    if (fallback) {
      return <>{fallback}</>;
    }

    if (showUpgradePrompt) {
      return (
        <UpgradePrompt
          feature={feature}
          currentTier={config?.tier}
          requiredTier={getRequiredTier(feature)}
        />
      );
    }

    return null;
  }

  return <>{children}</>;
}
```

**Deliverables:**
- ✅ Complete TypeScript types (10+ type files)
- ✅ API client with interceptors
- ✅ React Query provider
- ✅ Feature detection hook
- ✅ FeatureGate component
- ✅ 10 unit tests for hooks/components

**Verification:**
```bash
cd kiteclass-frontend
npm run type-check
# Should pass with 0 errors

npm run test
# All tests should pass
```

---

### PR-REVIEW-3.2: Frontend Component Tests

**Branch:** `review-frontend-component-tests`
**Priority:** P1 (HIGH)
**Effort:** 2 days
**Depends On:** PR-REVIEW-3.1

**Scope:**
Add tests for all implemented frontend components

**Tasks:**

1. **FeatureGate Tests** (6 tests)
```typescript
// src/components/features/__tests__/FeatureGate.test.tsx
import { render, screen } from '@testing-library/react';
import { FeatureGate } from '../FeatureGate';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { FeatureName } from '@/types/feature-detection';

describe('FeatureGate', () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient.clear();
  });

  it('should show content for available features', async () => {
    server.use(
      http.get('/api/v1/instance/config', () => {
        return HttpResponse.json({
          data: {
            tier: 'BASIC',
            features: {
              STUDENTS: true,
              ENGAGEMENT: false,
            },
          },
        });
      })
    );

    render(
      <FeatureGate feature={FeatureName.STUDENTS}>
        <div>Student Management</div>
      </FeatureGate>,
      { wrapper }
    );

    expect(await screen.findByText('Student Management')).toBeInTheDocument();
  });

  it('should show upgrade prompt for unavailable features', async () => {
    server.use(
      http.get('/api/v1/instance/config', () => {
        return HttpResponse.json({
          data: {
            tier: 'BASIC',
            features: {
              ENGAGEMENT: false,
            },
          },
        });
      })
    );

    render(
      <FeatureGate feature={FeatureName.ENGAGEMENT}>
        <div>Engagement Tracking</div>
      </FeatureGate>,
      { wrapper }
    );

    expect(await screen.findByText(/upgrade to/i)).toBeInTheDocument();
    expect(screen.queryByText('Engagement Tracking')).not.toBeInTheDocument();
  });

  it('should render custom fallback', async () => {
    server.use(
      http.get('/api/v1/instance/config', () => {
        return HttpResponse.json({
          data: {
            tier: 'BASIC',
            features: { AI_BRANDING: false },
          },
        });
      })
    );

    render(
      <FeatureGate
        feature={FeatureName.AI_BRANDING}
        fallback={<div>AI Branding requires PREMIUM</div>}
      >
        <div>AI Branding Settings</div>
      </FeatureGate>,
      { wrapper }
    );

    expect(await screen.findByText('AI Branding requires PREMIUM')).toBeInTheDocument();
  });

  it('should show loading state', () => {
    render(
      <FeatureGate feature={FeatureName.STUDENTS}>
        <div>Content</div>
      </FeatureGate>,
      { wrapper }
    );

    // Should show skeleton loader
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('should handle API errors', async () => {
    server.use(
      http.get('/api/v1/instance/config', () => {
        return HttpResponse.json(
          { error: 'Server error' },
          { status: 500 }
        );
      })
    );

    render(
      <FeatureGate feature={FeatureName.STUDENTS}>
        <div>Content</div>
      </FeatureGate>,
      { wrapper }
    );

    expect(await screen.findByText(/failed to load/i)).toBeInTheDocument();
  });

  it('should cache config to prevent duplicate API calls', async () => {
    let apiCallCount = 0;

    server.use(
      http.get('/api/v1/instance/config', () => {
        apiCallCount++;
        return HttpResponse.json({
          data: {
            tier: 'BASIC',
            features: { STUDENTS: true },
          },
        });
      })
    );

    const { rerender } = render(
      <FeatureGate feature={FeatureName.STUDENTS}>
        <div>Content 1</div>
      </FeatureGate>,
      { wrapper }
    );

    await screen.findByText('Content 1');

    // Rerender should use cached data
    rerender(
      <FeatureGate feature={FeatureName.STUDENTS}>
        <div>Content 2</div>
      </FeatureGate>
    );

    await screen.findByText('Content 2');

    expect(apiCallCount).toBe(1); // Only 1 API call
  });
});
```

2. **Hook Tests** (5 tests)
```typescript
// src/hooks/__tests__/useFeatureDetection.test.ts
import { renderHook, waitFor } from '@testing-library/react';
import { useFeatureDetection } from '../useFeatureDetection';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { FeatureName } from '@/types/feature-detection';

describe('useFeatureDetection', () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  beforeEach(() => {
    queryClient.clear();
  });

  it('should fetch instance config', async () => {
    server.use(
      http.get('/api/v1/instance/config', () => {
        return HttpResponse.json({
          data: {
            tier: 'BASIC',
            features: { STUDENTS: true },
          },
        });
      })
    );

    const { result } = renderHook(() => useFeatureDetection(), { wrapper });

    await waitFor(() => {
      expect(result.current.config).toBeDefined();
      expect(result.current.tier).toBe('BASIC');
    });
  });

  it('should check feature availability', async () => {
    server.use(
      http.get('/api/v1/instance/config', () => {
        return HttpResponse.json({
          data: {
            tier: 'BASIC',
            features: {
              STUDENTS: true,
              ENGAGEMENT: false,
            },
          },
        });
      })
    );

    const { result } = renderHook(() => useFeatureDetection(), { wrapper });

    await waitFor(() => {
      expect(result.current.hasFeature(FeatureName.STUDENTS)).toBe(true);
      expect(result.current.hasFeature(FeatureName.ENGAGEMENT)).toBe(false);
    });
  });

  it('should throw error when requiring unavailable feature', async () => {
    server.use(
      http.get('/api/v1/instance/config', () => {
        return HttpResponse.json({
          data: {
            tier: 'BASIC',
            features: { ENGAGEMENT: false },
          },
        });
      })
    );

    const { result } = renderHook(() => useFeatureDetection(), { wrapper });

    await waitFor(() => {
      expect(() => result.current.requireFeature(FeatureName.ENGAGEMENT))
        .toThrow(/not available/i);
    });
  });

  // ... 2 more tests
});
```

3. **MSW Mocks Setup**
```typescript
// src/mocks/server.ts
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

export const handlers = [
  // Default instance config handler
  http.get('/api/v1/instance/config', () => {
    return HttpResponse.json({
      data: {
        instanceId: '123e4567-e89b-12d3-a456-426614174000',
        tier: 'BASIC',
        features: {
          STUDENTS: true,
          CLASSES: true,
          ATTENDANCE: true,
          ENGAGEMENT: false,
          AI_BRANDING: false,
          MEDIA: false,
          CUSTOM_DOMAIN: false,
        },
        limitations: {
          maxStudents: 50,
          maxCourses: 10,
        },
        status: 'ACTIVE',
      },
    });
  }),

  // Auth handlers
  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = await request.json();
    // Mock login logic
    return HttpResponse.json({
      data: {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        userId: 1,
      },
    });
  }),
];

export const server = setupServer(...handlers);

// Start server before all tests
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));

// Reset handlers after each test
afterEach(() => server.resetHandlers());

// Clean up after all tests
afterAll(() => server.close());
```

**Deliverables:**
- ✅ 11 component tests (FeatureGate: 6, Hook: 5)
- ✅ MSW mocks for API
- ✅ 80%+ test coverage for frontend
- ✅ Vitest configuration

**Verification:**
```bash
cd kiteclass-frontend
npm run test:coverage
# Coverage >= 80%
```

---

### PR-REVIEW-3.3: Frontend Accessibility Tests

**Branch:** `review-frontend-a11y`
**Priority:** P2 (MEDIUM)
**Effort:** 1 day
**Depends On:** PR-REVIEW-3.2

**Scope:**
Add accessibility tests for all components

**Tasks:**

1. **Keyboard Navigation Tests** (5 tests)
2. **Screen Reader Tests** (5 tests)
3. **Color Contrast Tests** (3 tests)

**Deliverables:**
- ✅ 13 accessibility tests
- ✅ WCAG AA compliance verified

---

## PART 4: EXECUTION PLAN

### Week 1: Critical Security & Blockers

**Day 1-2:** Gateway Security Tests (PR-REVIEW-1.1)
- JWT, password, account lockout, OWASP
- **Output:** 21 security tests passing

**Day 3-5:** Implement PR 1.8 Cross-Service (PR-REVIEW-1.4)
- UserType, Feign, Saga, integration tests
- **Output:** Student registration flow working

### Week 2: Multi-Tenant & Coverage

**Day 6-8:** Core Multi-Tenant Security (PR-REVIEW-2.1)
- instanceId, TenantContext, JPA Filter, 20 tests
- **Output:** Tenant isolation enforced

**Day 9-10:** Test Coverage Improvement
- Gateway: PR-REVIEW-1.2 (25 tests → 80%+)
- Core: PR-REVIEW-2.3 (75 tests → 80%+)
- **Output:** Coverage >= 80%

### Week 3: Frontend & Cleanup

**Day 11-12:** Frontend Types & Tests
- PR-REVIEW-3.1: Types + API client
- PR-REVIEW-3.2: Component tests
- **Output:** Frontend testable, 80%+ coverage

**Day 13-14:** Code Quality & Documentation
- PR-REVIEW-1.3: Fix deprecated APIs
- PR-REVIEW-2.4: Internal API security
- PR-REVIEW-2.2: OWASP tests
- Update documentation
- **Output:** Clean, secure codebase

---

## PART 5: SUCCESS CRITERIA

### Gateway Service
- [ ] 80%+ test coverage
- [ ] 21 security tests passing
- [ ] PR 1.8 implemented (cross-service)
- [ ] 0 deprecated API warnings
- [ ] 0 security vulnerabilities

### Core Service
- [ ] 80%+ test coverage
- [ ] Multi-tenant security enforced
- [ ] 65 security tests passing (20 multi-tenant + 45 OWASP)
- [ ] Internal API HMAC-secured
- [ ] 0 cross-tenant data leaks

### Frontend
- [ ] Types complete
- [ ] API client configured
- [ ] 11+ component tests
- [ ] Accessibility tests passing
- [ ] 80%+ test coverage

---

## PART 6: SUMMARY

**Total Review PRs:** 12
**Total Effort:** 10-12 days (2-3 weeks)
**Priority:** URGENT (before new features)

**New Tests Added:**
- Gateway: 46 tests (21 security + 25 coverage)
- Core: 140 tests (20 multi-tenant + 45 OWASP + 75 coverage)
- Frontend: 11 tests (6 component + 5 hook)
- **TOTAL:** 197 new tests

**Impact:**
- ✅ Security hardened (OWASP + multi-tenant)
- ✅ Test coverage 80%+
- ✅ Cross-service integration working
- ✅ Frontend foundation solid
- ✅ Code quality improved
- ✅ Ready for new feature development

**Next Steps After Review:**
1. Continue with remaining Core PRs (2.4-2.10)
2. Continue with remaining Frontend PRs (3.4-3.13)
3. Implement Payment + Trial system
4. Deploy to staging

---

**End of Code Review Plan**
**Related Document:** `implemented-code-analysis.md` (detailed analysis)
**Version:** 2.0
**Date:** 2026-01-30
