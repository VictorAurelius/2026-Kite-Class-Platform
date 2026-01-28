# Code Quality & Testing Best Practices Guide

**Project:** KiteClass Platform
**Last Updated:** 2026-01-28
**Version:** 1.0

> Comprehensive guide for maintaining code quality, fixing warnings, and writing tests.
> All lessons learned from Gateway Service quality improvements (Jan 2026).

---

## Table of Contents

1. [Spring Boot Testing](#spring-boot-testing)
2. [Mockito Best Practices](#mockito-best-practices)
3. [AssertJ Assertions](#assertj-assertions)
4. [Testcontainers](#testcontainers)
5. [Deprecated APIs](#deprecated-apis)
6. [Code Quality Checklist](#code-quality-checklist)

---

## 1. Spring Boot Testing

### 1.1 @MockBean Deprecation (Spring Boot 3.4.0+)

**❌ DEPRECATED:**
```java
@MockBean
private AuthService authService;
```

**✅ CORRECT - Use @TestConfiguration:**
```java
@WebFluxTest(AuthController.class)
@Import({AuthControllerTest.MockConfig.class, TestSecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private AuthService authService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(authService);
    }
}
```

**Key Points:**
- Use `@TestConfiguration` with inner static class
- Add `@Primary` to override existing beans
- Remember `Mockito.reset()` in `@BeforeEach`
- Update `@Import` to include MockConfig

---

### 1.2 Integration Test Best Practices

**Template for Controller Integration Tests:**
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
class IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", postgres::getJdbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }
}
```

---

## 2. Mockito Best Practices

### 2.1 Complete ArgumentMatchers Import List

**✅ ALWAYS import explicitly (NEVER use wildcard):**
```java
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.isNotNull;

// Mockito methods
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
```

**❌ NEVER:**
```java
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
```

---

### 2.2 Custom Argument Matchers with argThat()

**✅ Use argThat() for complex verification:**
```java
verify(repository).save(argThat(user ->
    user.getEmail().equals("test@example.com") &&
    user.getStatus() == UserStatus.ACTIVE &&
    user.getRoles().contains("ADMIN")
));

verify(publisher).publishAll(argThat(events ->
    events.size() == 3 &&
    events.stream().allMatch(e -> e.getType() == EventType.USER_UPDATED)
));
```

---

### 2.3 Lenient Mocking (Deprecated)

**❌ DEPRECATED:**
```java
@Mock(lenient = true)
private EmailProperties emailProperties;
```

**✅ CORRECT - Don't use lenient:**
```java
@Mock
private EmailProperties emailProperties;

// If absolutely needed, use class-level:
@MockitoSettings(strictness = Strictness.LENIENT)
class MyTest {
    // ...
}
```

**Why avoid lenient:**
- Hides unused stubbings (test smells)
- Makes tests less maintainable
- Mockito strict mode catches real issues

---

## 3. AssertJ Assertions

### 3.1 Complete Imports

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

// ❌ NEVER use wildcard
import static org.assertj.core.api.Assertions.*;
```

---

### 3.2 Testing No Exception

**✅ Use assertThatCode() (compatible with older AssertJ):**
```java
assertThatCode(() -> someMethod())
    .doesNotThrowAnyException();
```

**⚠️ assertThatNoException() requires AssertJ 3.23.0+:**
```java
// Only if you have AssertJ 3.23.0+
assertThatNoException()
    .isThrownBy(() -> someMethod());
```

---

### 3.3 Replace Native assert with AssertJ

**❌ BAD - Java assert (disabled by default):**
```java
assert user != null;
assert user.getId() == 1;
```

**✅ GOOD - AssertJ:**
```java
assertThat(user).isNotNull();
assertThat(user.getId()).isEqualTo(1);
```

---

## 4. Testcontainers

### 4.1 Resource Leak Warning

**Problem:** `PostgreSQLContainer` implements `AutoCloseable`, static field triggers warning.

**✅ SOLUTION - @SuppressWarnings("resource"):**
```java
@Container
@SuppressWarnings("resource") // Managed by Testcontainers framework
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);
```

**Why this is correct:**
- Testcontainers manages lifecycle automatically
- Container cleaned up on JVM shutdown
- This is a false positive warning
- Recommended by Testcontainers documentation

---

### 4.2 Container Reuse

**✅ ALWAYS add .withReuse(true):**
```java
.withReuse(true) // Performance optimization
```

**Benefits:**
- Faster test execution
- Reuses container across test runs
- Reduces Docker overhead

---

## 5. Deprecated APIs

### 5.1 Bandwidth.classic() - Bucket4j

**❌ DEPRECATED:**
```java
Bandwidth limit = Bandwidth.classic(
    capacity,
    Refill.intervally(capacity, duration)
);
```

**✅ CORRECT - Use builder:**
```java
Bandwidth limit = Bandwidth.builder()
    .capacity(capacity)
    .refillIntervally(capacity, duration)
    .build();
```

---

### 5.2 Arrays.asList() → List.of()

**❌ OLD (works but not modern):**
```java
List<String> roles = Arrays.asList("OWNER", "ADMIN");
```

**✅ NEW (Java 9+):**
```java
List<String> roles = List.of("OWNER", "ADMIN");
```

**Benefits:**
- More concise
- Immutable by default
- Better performance

---

## 6. Code Quality Checklist

### 6.1 Before Committing Test Code

- [ ] No wildcard imports (`import static org.mockito.Mockito.*;`)
- [ ] All ArgumentMatchers explicitly imported
- [ ] AssertJ assertions instead of `assert` keyword
- [ ] @MockBean replaced with @TestConfiguration (Spring Boot 3.4.0+)
- [ ] Mockito.reset() in @BeforeEach when using @TestConfiguration
- [ ] @SuppressWarnings("resource") on Testcontainers
- [ ] .withReuse(true) on all containers
- [ ] No deprecated APIs without good reason
- [ ] Use List.of() instead of Arrays.asList()
- [ ] Use assertThatCode() instead of assertThatNoException()

---

### 6.2 Common Warning Fixes

| Warning | Fix |
|---------|-----|
| Resource leak (Testcontainers) | `@SuppressWarnings("resource")` |
| @MockBean deprecated | Migrate to @TestConfiguration |
| Unused ObjectMapper | Remove if truly unused |
| Deprecated Bandwidth.classic() | Use Bandwidth.builder() |
| Arrays.asList() | Replace with List.of() |
| argThat() undefined | Add import: ArgumentMatchers.argThat |
| anyList() undefined | Add import: ArgumentMatchers.anyList |
| anyInt() undefined | Add import: ArgumentMatchers.anyInt |

---

### 6.3 Null Safety Patterns

**✅ ALWAYS check null after .block():**
```java
// ❌ BAD
User user = userRepository.findById(1L).block();
user.getName(); // NPE if null!

// ✅ GOOD - With assertion
User user = userRepository.findById(1L).block();
assertThat(user).isNotNull();
assertThat(user.getName()).isEqualTo("John");

// ✅ BETTER - Use StepVerifier for reactive
StepVerifier.create(userRepository.findById(1L))
    .assertNext(user -> assertThat(user.getName()).isEqualTo("John"))
    .verifyComplete();
```

---

## 7. Standard Test File Template

### 7.1 Controller Unit Test (WebFluxTest)

```java
package com.kiteclass.gateway.controller;

import com.kiteclass.gateway.service.YourService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(YourController.class)
@Import({YourControllerTest.MockConfig.class, TestSecurityConfig.class})
@DisplayName("YourController Tests")
class YourControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private YourService yourService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public YourService yourService() {
            return Mockito.mock(YourService.class);
        }
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(yourService);
    }

    @Test
    @DisplayName("Test description")
    void testMethod() {
        // Given
        when(yourService.method(any())).thenReturn(Mono.just(result));

        // When/Then
        webTestClient.get()
                .uri("/api/endpoint")
                .exchange()
                .expectStatus().isOk();
    }
}
```

---

### 7.2 Service Unit Test

```java
package com.kiteclass.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("YourService Tests")
class YourServiceTest {

    @Mock
    private YourRepository repository;

    @InjectMocks
    private YourServiceImpl service;

    @Test
    @DisplayName("Test description")
    void testMethod() {
        // Given
        when(repository.findById(anyLong())).thenReturn(Mono.just(entity));

        // When
        Mono<Entity> result = service.findById(1L);

        // Then
        StepVerifier.create(result)
                .assertNext(e -> assertThat(e.getId()).isEqualTo(1L))
                .verifyComplete();

        verify(repository).findById(1L);
    }
}
```

---

### 7.3 Integration Test (Testcontainers)

```java
package com.kiteclass.gateway.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Integration Tests")
class IntegrationTest {

    @Container
    @SuppressWarnings("resource") // Managed by Testcontainers framework
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", postgres::getJdbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Test description")
    void testMethod() {
        webTestClient.get()
                .uri("/api/endpoint")
                .exchange()
                .expectStatus().isOk();
    }
}
```

---

## 8. Quick Reference

### 8.1 Import Cheat Sheet

```java
// === JUnit 5 ===
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

// === Mockito ===
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// === AssertJ ===
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// === Spring Test ===
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;

// === Reactor Test ===
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

// === Testcontainers ===
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
```

---

## 9. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-28 | Initial guide based on Gateway Service quality improvements |

---

## 10. References

- Spring Boot Testing Documentation
- Mockito Documentation
- AssertJ Documentation
- Testcontainers Documentation
- Gateway Service PRs: 7db6961 → 5b9fa9f

---

**Maintained by:** KiteClass Team
**Questions?** Refer to this guide first, then ask the team.
